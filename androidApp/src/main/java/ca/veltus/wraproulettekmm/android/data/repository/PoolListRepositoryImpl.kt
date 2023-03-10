package ca.veltus.wraproulettekmm.android.data.repository

import ca.veltus.wraproulettekmm.android.R
import ca.veltus.wraproulettekmm.android.data.objects.Member
import ca.veltus.wraproulettekmm.android.data.objects.Pool
import ca.veltus.wraproulettekmm.android.data.objects.User
import ca.veltus.wraproulettekmm.android.utils.StringResourcesProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.snapshots
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class PoolListRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val stringResourcesProvider: StringResourcesProvider
) : PoolListRepository {

    companion object {
        private const val TAG = "PoolListRepository"
    }

    private val firestoreInstance: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val currentUserDocReference: DocumentReference
        get() = firestoreInstance.document(
            "users/${
                FirebaseAuth.getInstance().uid ?: throw NullPointerException(
                    "UID is null."
                )
            }"
        )
    private val poolsCollectionReference = firestoreInstance.collection("pools")
    private val usersCollectionReference = firestoreInstance.collection("users")

    override val currentUser: FirebaseUser?
        get() = firebaseAuth.currentUser

    override suspend fun getCurrentUserProfile(): Flow<User?> {
        return firestore.collection("users").document(currentUser!!.uid).snapshots()
            .map<DocumentSnapshot, User?> { it.toObject() }.onCompletion {
//                Log.i(TAG, "getCurrentUserProfile: $it")
            }.catch {
//                Log.e(TAG, "getCurrentUserProfile: $it")
                Firebase.crashlytics.recordException(it)
            }
    }

    override suspend fun getPoolsList(userUid: String): Flow<List<Pool>> {
        return poolsCollectionReference.whereEqualTo(
            "users.$userUid", true
        ).snapshots().map<QuerySnapshot, List<Pool>> { querySnapshot -> querySnapshot.toObjects() }
            .onCompletion {
//                Log.i(TAG, "getPoolsList: $it")
            }.catch {
//                Log.e(TAG, "getPoolsList: $it")
                Firebase.crashlytics.recordException(it)
            }
    }

    /**
     * Check that the supplied values do not match any existing pool. If the list returns empty, create
     * a new pool document in Firestore. If matching pool is found return an error.
     */
    override suspend fun createPool(pool: Pool, onComplete: (String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val docId: String = poolsCollectionReference.document().id
            val currentUser = usersCollectionReference.document(currentUser!!.uid).get().await()
                .toObject(User::class.java)
            pool.adminUid = currentUser!!.uid
            pool.adminName = currentUser.displayName
            pool.users = mutableMapOf(currentUser.uid to true)
            pool.docId = docId
            // Check if pool with supplied parameters already exists.
            val poolIsEmpty = poolsCollectionReference.whereEqualTo("production", pool.production)
                .whereEqualTo("password", pool.password).whereEqualTo("date", pool.date).get()
                .await().isEmpty
            if (poolIsEmpty) {
                try {
                    // If pool doesn't exist create a new pool document using the docId.
                    poolsCollectionReference.document(docId).set(pool).await()
                    // Create a new map under the pools field for the current user and add the newly created pool id and set to true.
                    val newPoolMap = mutableMapOf<String, Any>("pools.$docId" to true)
                    currentUserDocReference.update(newPoolMap).await()
                    // Set the newly created pool document id to the users active pool field.
                    val activePoolMap = mutableMapOf<String, Any>("activePool" to docId)
                    currentUserDocReference.set(activePoolMap, SetOptions.merge()).await()
                    // Create a new member item using the current users account data and save it to the new pools members collection.
                    val member = Member(
                        currentUser.uid,
                        null,
                        docId,
                        currentUser.displayName,
                        currentUser.email,
                        currentUser.department,
                        null,
                        currentUser.profilePicturePath,
                        null,
                        true
                    )
                    poolsCollectionReference.document(docId).collection("members")
                        .document(currentUser.uid).set(member).await()
                    onComplete(null)

                } catch (e: FirebaseFirestoreException) {
                    onComplete(e.message)
                    Firebase.crashlytics.recordException(e)
                }
            } else {
                onComplete(stringResourcesProvider.getString(R.string.poolExistsErrorMessage))
            }
        }
    }

    /**
     * Check that the updated information does not match any existing pool before updating.
     * If matching pool is found return an error.
     */
    override suspend fun updatePool(pool: Pool, onComplete: (String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            // Check if pool with supplied parameters already exists.
            val poolIsEmpty = poolsCollectionReference.whereEqualTo("production", pool.production)
                .whereEqualTo("password", pool.password).whereEqualTo("date", pool.date)
                .whereNotEqualTo("docId", pool.docId).get().await().isEmpty
            if (poolIsEmpty) {
                try {
                    poolsCollectionReference.document(pool.docId).set(pool, SetOptions.merge())
                        .await()
                    onComplete(null)
                } catch (e: FirebaseFirestoreException) {
                    onComplete(e.message)
                    Firebase.crashlytics.recordException(e)
                }
            } else {
                onComplete(stringResourcesProvider.getString(R.string.poolExistsErrorMessage))
            }
        }
    }

    /**
     * Check to see if the supplied values match any existing pool. If pool does not exist, return an error.
     * If the current user is already a member of the pool they are trying to join, return an error.
     * Once the user joins the pool, create and add a member object using their account data to the pool.
     */
    override suspend fun joinPool(
        production: String, password: String, date: String, onComplete: (String?) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            // Retrieve the current users account as a User object.
            val account = currentUserDocReference.get().await().toObject(User::class.java)
            // Check all pools to see if any share the same name, password and date.
            val queryResult = poolsCollectionReference.whereEqualTo("production", production)
                .whereEqualTo("password", password).whereEqualTo("date", date).get().await()
            // Return a message if no pools match those credentials.
            if (queryResult.isEmpty) onComplete(stringResourcesProvider.getString(R.string.joinPoolErrorNoResults))
            else if (queryResult.size() > 1) onComplete(stringResourcesProvider.getString(R.string.joinPoolErrorMultiplePools))
            else {
                queryResult.documents.forEach {
                    val pool = it.toObject(Pool::class.java)
                    // If user is already a member of the pool return a message.
                    if (account!!.pools.any { entry -> entry.key == pool!!.docId && entry.value == true }) {
                        onComplete(stringResourcesProvider.getString(R.string.alreadyPoolMemberErrorMessage))
                        // If the user had previously joined the pool but left, rejoin the pool.
                    } else if (account.pools.any { entry -> entry.key == pool!!.docId && entry.value == false }) {
                        try {
                            // Update the pools users map field and set the current users uid from false to true.
                            poolsCollectionReference.document(pool!!.docId)
                                .update("users.${account.uid}", true).await()
                            // Update the current users member item and set the activeMember field to true.
                            poolsCollectionReference.document(pool.docId).collection("members")
                                .document(account.uid).update("activeMember", true).await()
                            // Create a new map under the pools field for the current user and add the newly created pool id and set to true.
                            val newPoolMap =
                                mutableMapOf<String, Any>("pools.${pool.docId}" to true)
                            currentUserDocReference.update(newPoolMap).await()
                            // Set the newly created pool document id to the users active pool field.
                            val activePoolMap =
                                mutableMapOf<String, Any>("activePool" to pool.docId)
                            currentUserDocReference.set(activePoolMap, SetOptions.merge()).await()
                            onComplete(null)
                        } catch (e: FirebaseFirestoreException) {
                            onComplete(e.message)
                            Firebase.crashlytics.recordException(e)
                        }
                    } else {
                        try {
                            // Create a new map under the pools field for the current user and add the newly created pool id and set to true.
                            val newPoolMap =
                                mutableMapOf<String, Any>("pools.${pool!!.docId}" to true)
                            currentUserDocReference.update(newPoolMap).await()
                            // Set the newly created pool document id to the users active pool field.
                            val activePoolMap =
                                mutableMapOf<String, Any>("activePool" to pool.docId)
                            currentUserDocReference.set(activePoolMap, SetOptions.merge()).await()
                            // Update the pools users map field and set the current users uid from false to true.
                            poolsCollectionReference.document(pool.docId)
                                .update("users.${account.uid}", true).await()
                            // Create a new member item using the current users account data and save it to the new pools members collection.
                            val member = Member(
                                account.uid,
                                null,
                                pool.docId,
                                account.displayName,
                                account.email,
                                account.department,
                                null,
                                account.profilePicturePath,
                                null,
                                true
                            )
                            poolsCollectionReference.document(pool.docId).collection("members")
                                .document(account.uid).set(member).await()
                            onComplete(null)
                        } catch (e: FirebaseFirestoreException) {
                            onComplete(e.message)
                            Firebase.crashlytics.recordException(e)
                        }
                    }

                }
            }
        }

    }

    override suspend fun getEditPool(poolId: String, onComplete: (Pool?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val pool = poolsCollectionReference.document(poolId).get().await()
                    .toObject(Pool::class.java)
                onComplete(pool)
            } catch (e: FirebaseFirestoreException) {
//                Log.e(TAG, "getEditPool: ${e.message}")
                onComplete(null)
                Firebase.crashlytics.recordException(e)
            }
        }
    }

    /**
     * Called when pool admin deletes a pool they created. Once deleted, set the users active pool
     * value to null.
     */
    override suspend fun deletePool(poolUid: String, onComplete: (String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Delete pool document matching the poolUid.
                poolsCollectionReference.document(poolUid).delete().await()
                // Set current users active pool to null.
                currentUserDocReference.update("activePool", null).await()
                // Update current users pools map field to poolUid is false.
                currentUserDocReference.update("pools.$poolUid", false).await()
                onComplete(null)
            } catch (e: FirebaseFirestoreException) {
                onComplete(e.message)
                Firebase.crashlytics.recordException(e)
            }
        }
    }

    /**
     * Called when a user joins, creates or selects a different pool from the PoolsFragment.
     */
    override suspend fun setActivePool(poolId: String, onComplete: (String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val newMap = mutableMapOf<String, Any>("activePool" to poolId)
                currentUserDocReference.set(newMap, SetOptions.merge()).await()
                onComplete(null)

            } catch (e: FirebaseFirestoreException) {
                onComplete(e.message)
                Firebase.crashlytics.recordException(e)
            }
        }
    }
}
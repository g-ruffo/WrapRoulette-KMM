package ca.veltus.wraproulettekmm.android.di

import ca.veltus.wraproulettekmm.android.data.repository.*
import ca.veltus.wraproulettekmm.android.utils.StringResourcesProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class AppModule {

    @Provides
    @Singleton
    fun provideAuthenticationRepository(
        firebaseAuth: FirebaseAuth, firestore: FirebaseFirestore
    ): AuthenticationRepository {
        return AuthenticationRepositoryImpl(firebaseAuth, firestore)
    }

    @Provides
    @Singleton
    fun providePoolListRepository(
        firebaseAuth: FirebaseAuth,
        firestore: FirebaseFirestore,
        stringResourcesProvider: StringResourcesProvider
    ): PoolListRepository {
        return PoolListRepositoryImpl(firebaseAuth, firestore, stringResourcesProvider)
    }

    @Provides
    @Singleton
    fun provideHomeRepository(
        firebaseAuth: FirebaseAuth, firestore: FirebaseFirestore,
        stringResourcesProvider: StringResourcesProvider
    ): HomeRepository {
        return HomeRepositoryImpl(firebaseAuth, firestore, stringResourcesProvider)
    }
}
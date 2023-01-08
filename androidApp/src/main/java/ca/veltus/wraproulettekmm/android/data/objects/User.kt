package ca.veltus.wraproulettekmm.android.data.objects

import androidx.annotation.Keep

@Keep
data class User(
    val uid: String,
    val displayName: String,
    val email: String,
    val department: String?,
    val profilePicturePath: String?,
    val activePool: String?,
    val pools: MutableMap<String, Any> = mutableMapOf()
) {
    constructor() : this("", "", "", null, null, null, mutableMapOf())
}
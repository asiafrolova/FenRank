package com.example.fencing_project.utils


import android.net.Uri

interface AvatarStorageManager {
    suspend fun uploadOpponentAvatar(
        userId: String,
        opponentId: Long,
        imageUri: Uri
    ): String?

    suspend fun uploadUserAvatar(
        userId: String,
        imageUri: Uri
    ): String?

    suspend fun deleteOpponentAvatar(
        userId: String,
        opponentId: Long
    ): Boolean

    suspend fun deleteUserAvatar(userId: String): Boolean

    fun getAvatarUrl(userId: String, opponentId: Long?): String

    fun hasAvatar(userId: String, opponentId: Long): Boolean
    suspend fun compressImage(imageUri: Uri): ByteArray
}
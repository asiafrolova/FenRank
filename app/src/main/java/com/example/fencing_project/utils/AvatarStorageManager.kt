package com.example.fencing_project.utils


import android.net.Uri

interface AvatarStorageManager {
    // Общие методы
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

    // Методы для получения URL/пути
    fun getAvatarUrl(userId: String, opponentId: Long?): String

    // Для проверки существования аватарки
    fun hasAvatar(userId: String, opponentId: Long): Boolean

    // Для компрессии изображения
    suspend fun compressImage(imageUri: Uri): ByteArray
}
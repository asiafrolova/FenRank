package com.example.fencing_project.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.fencing_project.utils.AvatarStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class LocalStorageManager(private val context: Context) : AvatarStorageManager {

    companion object {
        private const val AVATARS_DIR = "avatars"
        private const val USER_AVATARS_DIR = "user_avatars"
        private const val OPPONENT_AVATARS_DIR = "opponent_avatars"
        private const val AVATAR_SIZE = 400
        private const val MAX_IMAGE_SIZE = 1024 * 1024
        private const val USER_AVATAR_FILENAME = "avatar.jpg"
        private const val OPPONENT_AVATAR_PREFIX = "opponent_"
    }
    fun generateUserAvatarUrl(userId: String): String {
        val userDir = File(userAvatarsDir, userId)
        val file = File(userDir, USER_AVATAR_FILENAME)
        return if (file.exists()) {
            file.absolutePath
        } else {
            ""
        }
    }

    private val avatarsDir: File by lazy {
        File(context.filesDir, AVATARS_DIR).apply {
            if (!exists()) mkdirs()
        }
    }

    private val userAvatarsDir: File by lazy {
        File(avatarsDir, USER_AVATARS_DIR).apply {
            if (!exists()) mkdirs()
        }
    }

    private val opponentAvatarsDir: File by lazy {
        File(avatarsDir, OPPONENT_AVATARS_DIR).apply {
            if (!exists()) mkdirs()
        }
    }


    override suspend fun uploadOpponentAvatar(
        userId: String,
        opponentId: Long,
        imageUri: Uri
    ): String? {
        return withContext(Dispatchers.IO) {
            try {
                val compressedBytes = compressImage(imageUri)
                val fileName = "$opponentId.jpg"
                val userDir = File(opponentAvatarsDir, userId).apply {
                    if (!exists()) mkdirs()
                }
                val file = File(userDir, fileName)
                FileOutputStream(file).use { outputStream ->
                    outputStream.write(compressedBytes)
                }
                val filePath = file.absolutePath
                filePath

            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    override suspend fun uploadUserAvatar(userId: String, imageUri: Uri): String? {
        return withContext(Dispatchers.IO) {
            try {

                val compressedBytes = compressImage(imageUri)
                val fileName = "avatar.jpg"
                val userDir = File(userAvatarsDir, userId).apply {
                    if (!exists()) mkdirs()
                }
                val file = File(userDir, fileName)
                FileOutputStream(file).use { outputStream ->
                    outputStream.write(compressedBytes)
                }

                val filePath = file.absolutePath
                filePath

            } catch (e: Exception) {
                null
            }
        }
    }

    override suspend fun deleteOpponentAvatar(userId: String, opponentId: Long): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val fileName = "$opponentId.jpg"
                val userDir = File(opponentAvatarsDir, userId)
                val file = File(userDir, fileName)
                val success = if (file.exists()) {
                    file.delete()
                } else {
                    true
                }
                success

            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    override suspend fun deleteUserAvatar(userId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val fileName = "avatar.jpg"
                val userDir = File(userAvatarsDir, userId)
                val file = File(userDir, fileName)
                val success = if (file.exists()) {
                    file.delete()
                } else {
                    true
                }
                success

            } catch (e: Exception) {
                false
            }
        }
    }
    fun getAvatarPathStr(userId: String, opponentId: Long?): String {
        if (opponentId != null) {
            val fileName = "$opponentId.jpg"
            return "${opponentAvatarsDir}/${userId}/${fileName}"
        } else {
            val fileName = "avatar.jpg"
            return "${userAvatarsDir}/${userId}/${fileName}"

        }
    }
    override fun getAvatarUrl(userId: String, opponentId: Long?): String {
        return if (opponentId != null) {
            val fileName = "$opponentId.jpg"
            val userDir = File(opponentAvatarsDir, userId)
            val file = File(userDir, fileName)
            if (file.exists()) {
                file.absolutePath
            } else {
                ""
            }
        } else {
            val fileName = "avatar.jpg"
            val userDir = File(userAvatarsDir, userId)
            val file = File(userDir, fileName)
            if (file.exists()) {
                file.absolutePath
            } else {
                ""
            }
        }
    }

    override fun hasAvatar(userId: String, opponentId: Long): Boolean {
        val path = getAvatarUrl(userId, opponentId)
        return path.isNotEmpty() && File(path).exists()
    }

    override suspend fun compressImage(imageUri: Uri): ByteArray {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(imageUri)
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (originalBitmap == null) {
                    throw Exception("Не удалось загрузить изображение")
                }
                val maxWidth = 800
                val maxHeight = 800

                val scaledBitmap = if (originalBitmap.width > maxWidth || originalBitmap.height > maxHeight) {
                    val scale = minOf(
                        maxWidth.toFloat() / originalBitmap.width,
                        maxHeight.toFloat() / originalBitmap.height
                    )
                    val newWidth = (originalBitmap.width * scale).toInt()
                    val newHeight = (originalBitmap.height * scale).toInt()
                    Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
                } else {
                    originalBitmap
                }
                val outputStream = ByteArrayOutputStream()
                var quality = 85

                do {
                    outputStream.reset()
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                    quality -= 5
                } while (outputStream.size() > MAX_IMAGE_SIZE && quality > 30)

                val compressedBytes = outputStream.toByteArray()
                outputStream.close()
                compressedBytes

            } catch (e: Exception) {
                throw e
            }
        }
    }

    suspend fun saveAvatar(imageUri: Uri, userId: String, opponentId: Long): String {
        return if (opponentId != null) {
            uploadOpponentAvatar(userId, opponentId, imageUri) ?: ""
        } else {
            uploadUserAvatar(userId, imageUri) ?: ""
        }
    }

    fun getAvatarPath(userId: String, opponentId: Long): String? {
        val path = getAvatarUrl(userId, opponentId)
        return if (path.isNotEmpty()) path else null
    }

    suspend fun deleteAllUserAvatars(userId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                deleteUserAvatar(userId)
                val opponentUserDir = File(opponentAvatarsDir, userId)
                if (opponentUserDir.exists() && opponentUserDir.isDirectory) {
                    opponentUserDir.deleteRecursively()
                }

                val userDir = File(userAvatarsDir, userId)
                if (userDir.exists() && userDir.isDirectory) {
                    userDir.deleteRecursively()
                }

                true
            } catch (e: Exception) {
                false
            }
        }
    }
}
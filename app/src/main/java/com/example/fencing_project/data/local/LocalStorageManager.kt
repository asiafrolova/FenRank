// utils/LocalStorageManager.kt
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
        private const val AVATAR_SIZE = 400 // размер в пикселях
        private const val MAX_IMAGE_SIZE = 1024 * 1024
        private const val USER_AVATAR_FILENAME = "avatar.jpg"
        private const val OPPONENT_AVATAR_PREFIX = "opponent_"// 1MB
    }
    fun generateUserAvatarUrl(userId: String): String {
        // Для локального хранилища возвращаем путь к файлу
        val userDir = File(userAvatarsDir, userId)
        val file = File(userDir, USER_AVATAR_FILENAME)
        return if (file.exists()) {
            file.absolutePath
        } else {
            "" // Пустая строка если файла нет
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

    // === РЕАЛИЗАЦИЯ ИНТЕРФЕЙСА ===

    override suspend fun uploadOpponentAvatar(
        userId: String,
        opponentId: Long,
        imageUri: Uri
    ): String? {
        return withContext(Dispatchers.IO) {
            try {
                println("DEBUG: Локальное сохранение аватарки для opponentId: $opponentId")

                // Сжимаем изображение
                val compressedBytes = compressImage(imageUri)

                // Создаем путь для хранения
                val fileName = "$opponentId.jpg"
                val userDir = File(opponentAvatarsDir, userId).apply {
                    if (!exists()) mkdirs()
                }
                val file = File(userDir, fileName)

                // Сохраняем локально
                FileOutputStream(file).use { outputStream ->
                    outputStream.write(compressedBytes)
                }

                val filePath = file.absolutePath
                println("DEBUG: Аватарка сохранена локально: $filePath, размер: ${compressedBytes.size} байт")

                filePath

            } catch (e: Exception) {
                println("DEBUG: Ошибка локального сохранения аватарки: ${e.message}")
                e.printStackTrace()
                null
            }
        }
    }

    override suspend fun uploadUserAvatar(userId: String, imageUri: Uri): String? {
        return withContext(Dispatchers.IO) {
            try {
                println("DEBUG: Локальное сохранение аватарки пользователя $userId")

                // Сжимаем изображение
                val compressedBytes = compressImage(imageUri)

                // Создаем путь для хранения
                val fileName = "avatar.jpg"
                val userDir = File(userAvatarsDir, userId).apply {
                    if (!exists()) mkdirs()
                }
                val file = File(userDir, fileName)

                // Сохраняем локально
                FileOutputStream(file).use { outputStream ->
                    outputStream.write(compressedBytes)
                }

                val filePath = file.absolutePath
                println("DEBUG: Аватарка пользователя сохранена локально: $filePath")

                filePath

            } catch (e: Exception) {
                println("DEBUG: Ошибка локального сохранения аватарки пользователя: ${e.message}")
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

                println("DEBUG: Локальное удаление аватарки: ${file.absolutePath}")

                val success = if (file.exists()) {
                    file.delete()
                } else {
                    true // Файл не существует, считаем успехом
                }

                println("DEBUG: Аватарка успешно удалена локально: $success")
                success

            } catch (e: Exception) {
                println("DEBUG: Ошибка локального удаления аватарки: ${e.message}")
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

                println("DEBUG: Локальное удаление аватарки пользователя")

                val success = if (file.exists()) {
                    file.delete()
                } else {
                    true
                }

                println("DEBUG: Аватарка пользователя удалена локально: $success")
                success

            } catch (e: Exception) {
                println("DEBUG: Ошибка локального удаления аватарки пользователя: ${e.message}")
                false
            }
        }
    }
    fun getAvatarPathStr(userId: String, opponentId: Long?): String {
        if (opponentId != null) {
            val fileName = "$opponentId.jpg"
            return "${opponentAvatarsDir}/${userId}/${fileName}"
        } else {
            // Для пользователя
            val fileName = "avatar.jpg"
            return "${userAvatarsDir}/${userId}/${fileName}"

        }
    }
    override fun getAvatarUrl(userId: String, opponentId: Long?): String {
        return if (opponentId != null) {
            // Для соперника
            val fileName = "$opponentId.jpg"
            val userDir = File(opponentAvatarsDir, userId)
            val file = File(userDir, fileName)
            if (file.exists()) {
                file.absolutePath
            } else {
                "" // Пустая строка если файла нет
            }
        } else {
            // Для пользователя
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
                // Читаем изображение
                val inputStream = context.contentResolver.openInputStream(imageUri)
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (originalBitmap == null) {
                    throw Exception("Не удалось загрузить изображение")
                }

                // Сжимаем до разумных размеров
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

                // Сжимаем в ByteArray
                val outputStream = ByteArrayOutputStream()
                var quality = 85

                do {
                    outputStream.reset()
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                    quality -= 5
                } while (outputStream.size() > MAX_IMAGE_SIZE && quality > 30)

                val compressedBytes = outputStream.toByteArray()
                outputStream.close()

                println("DEBUG: Изображение сжато, размер: ${compressedBytes.size} байт, качество: ${quality + 5}%")
                compressedBytes

            } catch (e: Exception) {
                println("DEBUG: Ошибка сжатия изображения: ${e.message}")
                throw e
            }
        }
    }

    // === ДОПОЛНИТЕЛЬНЫЕ МЕТОДЫ ===

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

    // Удалить все аватарки пользователя (при удалении аккаунта)
    suspend fun deleteAllUserAvatars(userId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Удаляем аватарку пользователя
                deleteUserAvatar(userId)

                // Удаляем папку с аватарками соперников
                val opponentUserDir = File(opponentAvatarsDir, userId)
                if (opponentUserDir.exists() && opponentUserDir.isDirectory) {
                    opponentUserDir.deleteRecursively()
                }

                // Удаляем папку пользователя
                val userDir = File(userAvatarsDir, userId)
                if (userDir.exists() && userDir.isDirectory) {
                    userDir.deleteRecursively()
                }

                true
            } catch (e: Exception) {
                println("DEBUG: Ошибка удаления всех аватарок пользователя: ${e.message}")
                false
            }
        }
    }
}
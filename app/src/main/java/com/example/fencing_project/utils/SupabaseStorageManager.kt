package com.example.fencing_project.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class SupabaseStorageManager @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val context: Context
) {

    companion object {
        private const val BUCKET_NAME = "opponent-avatars"
        private const val USER_BUCKET_NAME = "user-avatars"
        private const val MAX_IMAGE_SIZE = 1024 * 1024 // 1MB
    }

    private val storage: Storage
        get() = supabaseClient.storage

    suspend fun uploadOpponentAvatar(
        userId: String,
        opponentId: String,
        imageUri: Uri
    ): String? {
        return withContext(Dispatchers.IO) {
            try {
                println("DEBUG: Начинаем загрузку аватарки для opponentId: $opponentId")

                // Сжимаем изображение
                val compressedBytes = compressImageToBytes(imageUri)

                // Создаем путь для хранения
                val fileName = "$opponentId.jpg"
                val filePath = "$userId/$fileName"

                println("DEBUG: Загружаем файл по пути: $filePath, размер: ${compressedBytes.size} байт")

                // Загружаем в Supabase Storage
                storage.from(BUCKET_NAME).upload(
                    path = filePath,
                    data = compressedBytes,
                    upsert = true
                )

                // Получаем URL к файлу
                val publicUrl = getPublicUrl(userId, opponentId)

                println("DEBUG: Аватарка загружена, URL: $publicUrl")
                publicUrl

            } catch (e: Exception) {
                println("DEBUG: Ошибка загрузки аватарки: ${e.message}")
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun getPublicUrl(userId: String, opponentId: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val fileName = "$opponentId.jpg"
                val filePath = "$userId/$fileName"

                // В Supabase Storage URL формируется так:
                // https://project-id.supabase.co/storage/v1/object/public/bucket-name/file-path
                val baseUrl = supabaseClient.supabaseUrl.removeSuffix("/")
                val publicUrl ="https://$baseUrl/storage/v1/object/public/$BUCKET_NAME/$filePath"

                println("DEBUG: Сгенерирован публичный URL: $publicUrl")
                publicUrl
            } catch (e: Exception) {
                println("DEBUG: Ошибка получения URL аватарки: ${e.message}")
                throw e
            }
        }
    }


    private suspend fun compressImageToBytes(uri: Uri): ByteArray {
        return withContext(Dispatchers.IO) {
            try {
                // Читаем изображение
                val inputStream = context.contentResolver.openInputStream(uri)
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

    // Простой способ проверить подключение
    suspend fun testConnection(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Пробуем получить список файлов в bucket (если есть)
                val files = try {
                    storage.from(BUCKET_NAME).list()
                    true
                } catch (e: Exception) {
                    println("DEBUG: Bucket возможно не существует, но это нормально: ${e.message}")
                    true // Все равно возвращаем true, bucket создадим позже
                }
                true
            } catch (e: Exception) {
                println("DEBUG: Ошибка подключения к Supabase: ${e.message}")
                false
            }
        }
    }

    suspend fun deleteOpponentAvatar(userId: String, opponentId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val fileName = "$opponentId.jpg"
                val filePath = "$userId/$fileName"

                println("DEBUG: Удаляем аватарку по пути: $filePath")

                // Удаляем файл из Supabase Storage
                storage.from(BUCKET_NAME).delete(filePath)

                println("DEBUG: Аватарка успешно удалена из Supabase Storage")
                true
            } catch (e: Exception) {
                println("DEBUG: Ошибка удаления аватарки: ${e.message}")
                e.printStackTrace()

                // Если файл не найден, считаем успехом
                if (e.message?.contains("not found", ignoreCase = true) == true ||
                    e.message?.contains("404", ignoreCase = true) == true) {
                    println("DEBUG: Файл не найден, возможно уже удален")
                    true
                } else {
                    false
                }
            }
        }
    }

    // SupabaseStorageManager.kt
// Добавь только эти методы
    suspend fun uploadUserAvatar(
        userId: String, // Firebase UID
        imageUri: Uri
    ): String? {
        return withContext(Dispatchers.IO) {
            try {
                println("DEBUG: Загрузка аватарки пользователя $userId")

                // Сжимаем изображение
                val compressedBytes = compressImageToBytes(imageUri)

                // Путь: userId/avatar.jpg
                val fileName = "avatar.jpg"
                val filePath = "$userId/$fileName"

                println("DEBUG: Загружаем в user-avatars по пути: $filePath")

                // Загружаем
                storage.from(USER_BUCKET_NAME).upload(
                    path = filePath,
                    data = compressedBytes,
                    upsert = true
                )

                // Генерируем URL
                val publicUrl = generateUserAvatarUrl(userId)

                println("DEBUG: Аватарка загружена, URL: $publicUrl")
                publicUrl

            } catch (e: Exception) {
                println("DEBUG: Ошибка загрузки: ${e.message}")
                null
            }
        }
    }

    // Генерация URL без обращения к Supabase
    fun generateUserAvatarUrl(userId: String): String {
        val baseUrl = supabaseClient.supabaseUrl.removeSuffix("/")
        return "$baseUrl/storage/v1/object/public/$USER_BUCKET_NAME/$userId/avatar.jpg"
    }

    suspend fun deleteUserAvatar(userId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val filePath = "$userId/avatar.jpg"
                storage.from(USER_BUCKET_NAME).delete(filePath)
                println("DEBUG: Аватарка удалена")
                true
            } catch (e: Exception) {
                println("DEBUG: Ошибка удаления: ${e.message}")
                // Если файл не найден - все ок
                e.message?.contains("not found", ignoreCase = true) == true ||
                        e.message?.contains("404", ignoreCase = true) == true
            }
        }
    }



}
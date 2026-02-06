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
        private const val MAX_IMAGE_SIZE = 1024 * 1024
    }

    private val storage: Storage
        get() = supabaseClient.storage

    suspend fun uploadOpponentAvatar(
        userId: String,
        opponentId: Long,
        imageUri: Uri
    ): String? {
        return withContext(Dispatchers.IO) {
            try {
                val compressedBytes = compressImageToBytes(imageUri)
                val fileName = "$opponentId.jpg"
                val filePath = "$userId/$fileName"
                storage.from(BUCKET_NAME).upload(
                    path = filePath,
                    data = compressedBytes,
                    upsert = true
                )

                val publicUrl = getPublicUrl(userId, opponentId)

                publicUrl

            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun getPublicUrl(userId: String, opponentId: Long): String {
        return withContext(Dispatchers.IO) {
            try {
                val fileName = "$opponentId.jpg"
                val filePath = "$userId/$fileName"

                val baseUrl = supabaseClient.supabaseUrl.removeSuffix("/")
                val publicUrl ="https://$baseUrl/storage/v1/object/public/$BUCKET_NAME/$filePath"

                publicUrl
            } catch (e: Exception) {
                throw e
            }
        }
    }


    private suspend fun compressImageToBytes(uri: Uri): ByteArray {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
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

    suspend fun testConnection(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val files = try {
                    storage.from(BUCKET_NAME).list()
                    true
                } catch (e: Exception) {

                    true
                }
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    suspend fun deleteOpponentAvatar(userId: String, opponentId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val fileName = "$opponentId.jpg"
                val filePath = "$userId/$fileName"
                storage.from(BUCKET_NAME).delete(filePath)
                true
            } catch (e: Exception) {

                e.printStackTrace()

                if (e.message?.contains("not found", ignoreCase = true) == true ||
                    e.message?.contains("404", ignoreCase = true) == true) {
                    true
                } else {
                    false
                }
            }
        }
    }

    suspend fun uploadUserAvatar(
        userId: String,
        imageUri: Uri
    ): String? {
        return withContext(Dispatchers.IO) {
            try {
                val compressedBytes = compressImageToBytes(imageUri)
                val fileName = "avatar.jpg"
                val filePath = "$userId/$fileName"
                storage.from(USER_BUCKET_NAME).upload(
                    path = filePath,
                    data = compressedBytes,
                    upsert = true
                )

                val publicUrl = generateUserAvatarUrl(userId)
                publicUrl

            } catch (e: Exception) {
                null
            }
        }
    }


    fun generateUserAvatarUrl(userId: String): String {
        val baseUrl = supabaseClient.supabaseUrl.removeSuffix("/")
        return "$baseUrl/storage/v1/object/public/$USER_BUCKET_NAME/$userId/avatar.jpg"
    }

    suspend fun deleteUserAvatar(userId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val filePath = "$userId/avatar.jpg"
                storage.from(USER_BUCKET_NAME).delete(filePath)
                true
            } catch (e: Exception) {
                e.message?.contains("not found", ignoreCase = true) == true ||
                        e.message?.contains("404", ignoreCase = true) == true
            }
        }
    }

    suspend fun downloadOpponentAvatar(
        userId: String,
        opponentId: String,
        saveToLocalPath: String,

    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val fileName = "$opponentId.jpg"
                val filePath = "$userId/$fileName"

                val bytes = storage.from(BUCKET_NAME).downloadPublic(path=filePath)
                val file = File(saveToLocalPath)
                file.parentFile?.mkdirs()

                FileOutputStream(file).use { outputStream ->
                    outputStream.write(bytes)
                }
                true

            } catch (e: Exception) {

                if (e.message?.contains("not found", ignoreCase = true) == true ||
                    e.message?.contains("404", ignoreCase = true) == true) {
                    false
                } else {
                    e.printStackTrace()
                    false
                }
            }
        }
    }

    suspend fun downloadUserAvatar(
        userId: String,
        saveToLocalPath: String
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val filePath = "$userId/avatar.jpg"

                val bytes = storage.from(USER_BUCKET_NAME).downloadPublic(filePath)
                val file = File(saveToLocalPath)
                file.parentFile?.mkdirs()

                FileOutputStream(file).use { outputStream ->
                    outputStream.write(bytes)
                }

                true

            } catch (e: Exception) {
                if (e.message?.contains("not found", ignoreCase = true) == true ||
                    e.message?.contains("404", ignoreCase = true) == true) {
                    false
                } else {
                    false
                }
            }
        }
    }
}
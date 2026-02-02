package com.example.fencing_project.data.repository

import android.content.Context
import com.example.fencing_project.data.model.Opponent
import com.example.fencing_project.data.model.Sync
import com.example.fencing_project.utils.SharedPrefsManager
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val context: Context,
) {
    val pref =SharedPrefsManager(context)
    val currentUser = MutableStateFlow<FirebaseUser?>(firebaseAuth.currentUser)
    init {
        // Слушаем изменения аутентификации
        firebaseAuth.addAuthStateListener { auth ->
            currentUser.value = auth.currentUser
        }
    }


    suspend fun register(email: String, password: String): Result<Unit> {
        return try {
            firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateEmail(
        currentEmail: String,
        currentPassword: String,
        newEmail: String
    ): Result<Unit> {
        return try {
            val user = firebaseAuth.currentUser
                ?: return Result.failure(Exception("Пользователь не авторизован"))

            // Реаутентификация
            val reauthResult = firebaseAuth.signInWithEmailAndPassword(
                currentEmail,
                currentPassword
            ).await()

            if (reauthResult.user != null) {
                // Используем verifyBeforeUpdateEmail
                //user.verifyBeforeUpdateEmail(newEmail).await()
                user.updateEmail(newEmail).await()

                Result.success(Unit)
            } else {
                Result.failure(Exception("Ошибка повторной аутентификации"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Получаем текущего пользователя с обновленными данными
    fun getCurrentUser(): FirebaseUser?{
        return currentUser.value
    }
    fun getUserId():String?{
        return pref.getUserId()
    }

    suspend fun updatePassword(
        currentEmail: String,
        currentPassword: String,
        newPassword: String
    ): Result<Unit> {
        return try {
            val user = firebaseAuth.currentUser

            if (user == null) {
                return Result.failure(Exception("Пользователь не авторизован. Войдите заново."))
            }

            // СНАЧАЛА пробуем аутентифицироваться с текущими данными
            try {
                val signInResult = firebaseAuth.signInWithEmailAndPassword(
                    currentEmail,
                    currentPassword
                ).await()

                if (signInResult.user == null) {
                    return Result.failure(Exception("Неверный текущий пароль"))
                }
            } catch (e: Exception) {
                return Result.failure(Exception("Неверный текущий пароль: ${e.message}"))
            }

            // Теперь пробуем ре-аутентификацию через credential
            val credential = EmailAuthProvider.getCredential(currentEmail, currentPassword)

            try {
                user.reauthenticate(credential).await()
            } catch (e: Exception) {
                println("DEBUG: Ошибка reauthenticate: ${e.message}")

                // Альтернатива: просто обновляем пароль после успешного signIn
                val currentUser = firebaseAuth.currentUser
                currentUser?.updatePassword(newPassword)?.await()
                return Result.success(Unit)
            }

            // Обновление пароля
            user.updatePassword(newPassword).await()
            Result.success(Unit)

        } catch (e: Exception) {
            println("DEBUG: Общая ошибка updatePassword: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    // Альтернативный способ через sendPasswordResetEmail
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

suspend fun login(email: String, password: String): Result<FirebaseUser> {
    return try {
        val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
        Result.success(result.user!!)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

    fun logout() {
        firebaseAuth.signOut()
    }

    suspend fun deleteAccount(currentPassword: String): Result<Unit> {
        return try {
            val user = firebaseAuth.currentUser
                ?: return Result.failure(Exception("Пользователь не авторизован"))

            println("DEBUG: удаляем firebase account")
            // Реаутентификация (требуется для удаления)
            val credential = EmailAuthProvider.getCredential(user.email ?: "", currentPassword)
            user.reauthenticate(credential).await()

            // Удаляем аккаунт
            user.delete().await()
            println("DEBUG: удалили успешно")
            Result.success(Unit)

        } catch (e: Exception) {
            println("DEBUG: удалили с ошибкой $e")
            Result.failure(e)
        }
    }

    suspend fun getUserOpponents(userId: String): List<String> {
        return try {
            val opponentsRef = FirebaseDatabase.getInstance().getReference("opponents")
            val query = opponentsRef.orderByChild("createdBy").equalTo(userId)
            val snapshot = query.get().await()

            val opponentIds = mutableListOf<String>()
            for (snapshot in snapshot.children) {
                snapshot.key?.let { opponentIds.add(it) }
            }

            opponentIds
        } catch (e: Exception) {
            emptyList()
        }
    }



}

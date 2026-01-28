package com.example.fencing_project.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) {
    suspend fun register(email: String, password: String): Result<Unit> {
        return try {
            firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
//    suspend fun login(email: String, password: String):Result<Unit> {
//        return try {
//            firebaseAuth.signInWithEmailAndPassword(email, password).await()
//            Result.success(Unit)
//        } catch (e: Exception) {
//            Result.failure(e)
//        }
//    }
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

    fun currentUser() = firebaseAuth.currentUser
}

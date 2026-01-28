package com.example.fencing_project.data.repository

import com.example.fencing_project.data.model.Bout
import com.example.fencing_project.data.model.Opponent
import com.google.firebase.database.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class BoutRepository @Inject constructor(
    private val database: FirebaseDatabase
) {

    private val boutsRef: DatabaseReference
        get() = database.getReference("bouts")

    private val opponentsRef: DatabaseReference
        get() = database.getReference("opponents")

    // Добавить соперника (Realtime Database)
    suspend fun addOpponent(opponent: Opponent): String {
        return try {
            // Создаем новый ключ
            val opponentId = opponentsRef.push().key ?: throw Exception("Не удалось создать ID")

            // Обновляем объект с ID
            val opponentWithId = opponent.copy(id = opponentId)

            // Сохраняем в Firebase
            opponentsRef.child(opponentId).setValue(opponentWithId).await()

            // Возвращаем ID
            opponentId
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun updateOpponentAvatar(opponentId: String, avatarUrl: String) {
        try {
            val ref = database.getReference("opponents").child(opponentId)
            ref.child("avatarUrl").setValue(avatarUrl).await()
            println("DEBUG: Аватарка обновлена для opponentId: $opponentId")
        } catch (e: Exception) {
            println("DEBUG: Ошибка обновления аватарки: ${e.message}")
            throw e
        }
    }

    // Добавить бой (Realtime Database)
    suspend fun addBout(bout: Bout): String {
        return try {
            val boutId = boutsRef.push().key ?: throw Exception("Не удалось создать ID")
            val boutWithId = bout.copy(id = boutId)

            boutsRef.child(boutId).setValue(boutWithId).await()
            boutId
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun getHomeScreenData(userId: String): Pair<List<Bout>, List<Opponent>> {
        return try {
            // Получаем бои пользователя
            val boutsSnapshot = boutsRef
                .orderByChild("authorId")
                .equalTo(userId)
                .get()
                .await()

            val bouts = mutableListOf<Bout>()
            for (snapshot in boutsSnapshot.children) {
                val bout = snapshot.getValue(Bout::class.java)
                bout?.let { bouts.add(it) }
            }

            // ИСПРАВЛЕНО: Ищем соперников по полю "createdBy"
            val opponentsSnapshot = opponentsRef
                .orderByChild("createdBy") // ← ИЗМЕНИТЕ ЗДЕСЬ!
                .equalTo(userId)
                .get()
                .await()

            val opponents = mutableListOf<Opponent>()
            for (snapshot in opponentsSnapshot.children) {
                val opponent = snapshot.getValue(Opponent::class.java)
                opponent?.let { opponents.add(it) }
            }

            println("DEBUG: Найдено ${opponents.size} соперников для пользователя $userId")
            opponents.forEachIndexed { i, opp ->
                println("DEBUG: Соперник $i: ${opp.name}, createdBy: ${opp.createdBy}")
            }

            Pair(bouts, opponents)
        } catch (e: Exception) {
            println("DEBUG: Ошибка в getHomeScreenData: ${e.message}")
            throw e
        }
    }
}
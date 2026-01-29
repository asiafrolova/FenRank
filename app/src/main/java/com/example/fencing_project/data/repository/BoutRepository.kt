package com.example.fencing_project.data.repository

import com.example.fencing_project.data.model.Bout
import com.example.fencing_project.data.model.Opponent
import com.example.fencing_project.utils.SupabaseStorageManager
import com.google.firebase.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

class BoutRepository @Inject constructor(
    private val database: FirebaseDatabase,
    private val storageManager: SupabaseStorageManager,
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
            // 1. Создаем ID для боя
            val boutId = boutsRef.push().key ?: throw Exception("Не удалось создать ID")
            val boutWithId = bout.copy(id = boutId)

            // 2. Сохраняем бой
            boutsRef.child(boutId).setValue(boutWithId).await()

            // 3. Обновляем статистику соперника
            updateOpponentStats(bout.opponentId, bout.authorId, bout)

            boutId
        } catch (e: Exception) {
            throw e
        }
    }

    private suspend fun updateOpponentStats(opponentId: String, userId: String, bout: Bout) {
        try {
            // Получаем текущие данные соперника
            val opponentRef = opponentsRef.child(opponentId)
            val opponentSnapshot = opponentRef.get().await()

            val currentOpponent = opponentSnapshot.getValue(Opponent::class.java)

            if (currentOpponent != null && currentOpponent.createdBy == userId) {
                // Считаем результат боя
                val userWon = bout.userScore > bout.opponentScore
                val opponentWon = bout.userScore < bout.opponentScore
                val isDraw = bout.userScore == bout.opponentScore

                // Обновляем статистику
                val updatedStats = mapOf<String, Any>(
                    "totalBouts" to (currentOpponent.totalBouts + 1),
                    "userWins" to (currentOpponent.userWins + if (userWon) 1 else 0),
                    "opponentWins" to (currentOpponent.opponentWins + if (opponentWon) 1 else 0),
                    "draws" to (currentOpponent.draws + if (isDraw) 1 else 0),
                    "totalUserScore" to (currentOpponent.totalUserScore + bout.userScore),
                    "totalOpponentScore" to (currentOpponent.totalOpponentScore + bout.opponentScore),
                    "lastBoutDate" to bout.date
                )

                // Обновляем в Firebase
                opponentRef.updateChildren(updatedStats).await()

                println("DEBUG: Статистика соперника $opponentId обновлена")
            } else {
                println("DEBUG: Соперник не найден или не принадлежит пользователю")
            }
        } catch (e: Exception) {
            println("DEBUG: Ошибка обновления статистики: ${e.message}")
            // Не бросаем исключение, чтобы не ломать добавление боя
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

    // BoutRepository.kt
    suspend fun updateBout(bout: Bout) {
        try {
            if (bout.id.isBlank()) {
                throw Exception("ID боя не может быть пустым")
            }

            // Получаем старый бой, чтобы обновить статистику соперника
            val oldBout = getBout(bout.id)

            // Обновляем бой
            boutsRef.child(bout.id).setValue(bout).await()

            // Если изменился счет или соперник, обновляем статистику
            if (oldBout != null) {
                // Отменяем старую статистику
                updateOpponentStatsAfterEdit(oldBout, isDelete = true)
                // Добавляем новую статистику
                updateOpponentStatsAfterEdit(bout, isDelete = false)
            }

            println("DEBUG: Бой обновлен: ${bout.id}")
        } catch (e: Exception) {
            println("DEBUG: Ошибка обновления боя: ${e.message}")
            throw e
        }
    }

    suspend fun deleteBout(boutId: String) {
        try {
            // Получаем бой перед удалением
            val bout = getBout(boutId)

            // Удаляем бой
            boutsRef.child(boutId).removeValue().await()

            // Обновляем статистику соперника
            if (bout != null) {
                updateOpponentStatsAfterEdit(bout, isDelete = true)
            }

            println("DEBUG: Бой удален: $boutId")
        } catch (e: Exception) {
            println("DEBUG: Ошибка удаления боя: ${e.message}")
            throw e
        }
    }

    suspend fun getBout(boutId: String): Bout? {
        return try {
            val snapshot = boutsRef.child(boutId).get().await()
            snapshot.getValue(Bout::class.java)
        } catch (e: Exception) {
            println("DEBUG: Ошибка получения боя: ${e.message}")
            null
        }
    }

    private suspend fun updateOpponentStatsAfterEdit(bout: Bout, isDelete: Boolean) {
        try {
            val opponentRef = opponentsRef.child(bout.opponentId)
            val opponentSnapshot = opponentRef.get().await()

            val currentOpponent = opponentSnapshot.getValue(Opponent::class.java)

            if (currentOpponent != null && currentOpponent.createdBy == bout.authorId) {
                val multiplier = if (isDelete) -1 else 1

                val userWon = bout.userScore > bout.opponentScore
                val opponentWon = bout.userScore < bout.opponentScore
                val isDraw = bout.userScore == bout.opponentScore

                val updatedStats = mapOf<String, Any>(
                    "totalBouts" to (currentOpponent.totalBouts + multiplier),
                    "userWins" to (currentOpponent.userWins + (if (userWon) multiplier else 0)),
                    "opponentWins" to (currentOpponent.opponentWins + (if (opponentWon) multiplier else 0)),
                    "draws" to (currentOpponent.draws + (if (isDraw) multiplier else 0)),
                    "totalUserScore" to (currentOpponent.totalUserScore + bout.userScore * multiplier),
                    "totalOpponentScore" to (currentOpponent.totalOpponentScore + bout.opponentScore * multiplier)
                )

                opponentRef.updateChildren(updatedStats).await()
            }
        } catch (e: Exception) {
            println("DEBUG: Ошибка обновления статистики после редактирования: ${e.message}")
        }
    }
    suspend fun updateOpponent(
        opponentId: String,
        name: String,
        weaponHand: String,
        weaponType: String,
        comment: String,
        avatarUrl: String
    ) {
        try {
            val updates = mapOf<String, Any>(
                "name" to name,
                "weaponHand" to weaponHand,
                "weaponType" to weaponType,
                "comment" to comment,
                "avatarUrl" to avatarUrl
            )

            opponentsRef.child(opponentId).updateChildren(updates).await()
            println("DEBUG: Соперник обновлен: $opponentId")
        } catch (e: Exception) {
            println("DEBUG: Ошибка обновления соперника: ${e.message}")
            throw e
        }
    }

    suspend fun deleteOpponent(opponentId: String) {
        try {
            // Удаляем все бои с этим соперником
            val boutsSnapshot = boutsRef
                .orderByChild("opponentId")
                .equalTo(opponentId)
                .get()
                .await()

            for (snapshot in boutsSnapshot.children) {
                snapshot.ref.removeValue()
            }

            // Удаляем самого соперника
            opponentsRef.child(opponentId).removeValue().await()

            println("DEBUG: Соперник удален: $opponentId")
        } catch (e: Exception) {
            println("DEBUG: Ошибка удаления соперника: ${e.message}")
            throw e
        }
    }

    suspend fun getOpponent(opponentId: String): Opponent? {
        return try {
            val snapshot = opponentsRef.child(opponentId).get().await()
            snapshot.getValue(Opponent::class.java)
        } catch (e: Exception) {
            println("DEBUG: Ошибка получения соперника: ${e.message}")
            null
        }
    }
    suspend fun deleteOpponentWithAvatar(opponentId: String, userId: String, opponentAvatarUrl: String? = null): Boolean {
        return try {
            println("DEBUG: Начинаем удаление соперника $opponentId с аватаркой")

            // 1. Получаем данные соперника перед удалением (чтобы знать userId)
            val opponent = getOpponent(opponentId)
            val actualUserId = opponent?.createdBy ?: userId

            // 2. Удаляем аватарку из Supabase если есть
            if (!opponentAvatarUrl.isNullOrBlank()) {
                println("DEBUG: Удаляем аватарку из Supabase")
                val avatarDeleted = storageManager.deleteOpponentAvatar(actualUserId, opponentId)
                if (!avatarDeleted) {
                    println("DEBUG: Предупреждение: не удалось удалить аватарку из Supabase")
                }
            }

            // 3. Удаляем соперника и его бои из Firebase
            deleteOpponent(opponentId)

            println("DEBUG: Соперник успешно удален")
            true

        } catch (e: Exception) {
            println("DEBUG: Ошибка удаления соперника с аватаркой: ${e.message}")
            e.printStackTrace()
            false
        }
    }

}
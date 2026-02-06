package com.example.fencing_project.data.repository

import com.example.fencing_project.data.model.Bout
import com.example.fencing_project.data.model.Opponent
import com.example.fencing_project.utils.SupabaseStorageManager
import com.google.firebase.database.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class BoutRepository @Inject constructor(
    private val database: FirebaseDatabase,
    private val storageManager: SupabaseStorageManager,

    ) {


    private val boutsRef: DatabaseReference
        get() = database.getReference("bouts")

    private val opponentsRef: DatabaseReference
        get() = database.getReference("opponents")

    suspend fun addOpponent(opponent: Opponent): String {
        return try {
            val opponentId = opponentsRef.push().key ?: throw Exception("Не удалось создать ID")
            val opponentWithId = opponent.copy(id = opponentId)
            opponentsRef.child(opponentId).setValue(opponentWithId).await()
            opponentId
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun updateOpponentAvatar(opponentId: String, avatarUrl: String) {
        try {
            val ref = database.getReference("opponents").child(opponentId)
            ref.child("avatarUrl").setValue(avatarUrl).await()
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun addBout(bout: Bout): String {
        return try {
            val boutId = boutsRef.push().key ?: throw Exception("Не удалось создать ID")
            val boutWithId = bout.copy(id = boutId)
            boutsRef.child(boutId).setValue(boutWithId).await()
            updateOpponentStats(bout.opponentId, bout.authorId, bout)

            boutId
        } catch (e: Exception) {
            throw e
        }
    }

    private suspend fun updateOpponentStats(opponentId: String, userId: String, bout: Bout) {
        try {
            val opponentRef = opponentsRef.child(opponentId)
            val opponentSnapshot = opponentRef.get().await()

            val currentOpponent = opponentSnapshot.getValue(Opponent::class.java)

            if (currentOpponent != null && currentOpponent.createdBy == userId) {
                val userWon = bout.userScore > bout.opponentScore
                val opponentWon = bout.userScore < bout.opponentScore
                val isDraw = bout.userScore == bout.opponentScore

                val updatedStats = mapOf<String, Any>(
                    "totalBouts" to (currentOpponent.totalBouts + 1),
                    "userWins" to (currentOpponent.userWins + if (userWon) 1 else 0),
                    "opponentWins" to (currentOpponent.opponentWins + if (opponentWon) 1 else 0),
                    "draws" to (currentOpponent.draws + if (isDraw) 1 else 0),
                    "totalUserScore" to (currentOpponent.totalUserScore + bout.userScore),
                    "totalOpponentScore" to (currentOpponent.totalOpponentScore + bout.opponentScore),
                    "lastBoutDate" to bout.date
                )

                opponentRef.updateChildren(updatedStats).await()

            }
        } catch (e: Exception) {
           throw e
        }
    }

    suspend fun getHomeScreenData(userId: String): Pair<List<Bout>, List<Opponent>> {
        return try {
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

            val opponentsSnapshot = opponentsRef
                .orderByChild("createdBy")
                .equalTo(userId)
                .get()
                .await()

            val opponents = mutableListOf<Opponent>()
            for (snapshot in opponentsSnapshot.children) {
                val opponent = snapshot.getValue(Opponent::class.java)
                opponent?.let { opponents.add(it) }
            }

            Pair(bouts, opponents)
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun updateBout(bout: Bout) {
        try {
            if (bout.id.isBlank()) {
                throw Exception("ID боя не может быть пустым")
            }

            val oldBout = getBout(bout.id)

            boutsRef.child(bout.id).setValue(bout).await()
            if (oldBout != null) {
                updateOpponentStatsAfterEdit(oldBout, isDelete = true)
                updateOpponentStatsAfterEdit(bout, isDelete = false)
            }

        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun deleteBout(boutId: String) {
        try {
            val bout = getBout(boutId)
            boutsRef.child(boutId).removeValue().await()
            if (bout != null) {
                updateOpponentStatsAfterEdit(bout, isDelete = true)
            }

        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun getBout(boutId: String): Bout? {
        return try {
            val snapshot = boutsRef.child(boutId).get().await()
            snapshot.getValue(Bout::class.java)
        } catch (e: Exception) {
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
            throw e
        }
    }
    suspend fun updateOpponent(
        opponent: Opponent
    ) {
        try {
            val updates = mapOf<String, Any>(
                "roomId" to opponent.roomId,
                "name" to opponent.name,
                "weaponHand" to opponent.weaponHand,
                "weaponType" to opponent.weaponType,
                "comment" to opponent.comment,
                "avatarUrl" to opponent.avatarUrl
            )

            opponentsRef.child(opponent.id).updateChildren(updates).await()
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun deleteOpponent(opponentId: String) {
        try {
            val boutsSnapshot = boutsRef
                .orderByChild("opponentId")
                .equalTo(opponentId)
                .get()
                .await()

            for (snapshot in boutsSnapshot.children) {
                snapshot.ref.removeValue()
            }

            opponentsRef.child(opponentId).removeValue().await()
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun getOpponent(opponentId: String): Opponent? {
        return try {
            val snapshot = opponentsRef.child(opponentId).get().await()
            snapshot.getValue(Opponent::class.java)
        } catch (e: Exception) {
            null
        }
    }
    suspend fun getOpponentByRoomId(roomId: Long, userId: String): Opponent? {
        return try {
            val opponentsSnapshot = opponentsRef
                .orderByChild("createdBy")
                .equalTo(userId)
                .get()
                .await()

            for (snapshot in opponentsSnapshot.children) {
                val opponent = snapshot.getValue(Opponent::class.java)
                if (opponent != null && opponent.roomId == roomId) {
                    return opponent
                }
            }

            null

        } catch (e: Exception) {
            null
        }
    }
    suspend fun deleteOpponentWithAvatar(opponentId: String, userId: String, opponentAvatarUrl: String? = null): Boolean {
        return try {

            val opponent = getOpponent(opponentId)
            val actualUserId = opponent?.createdBy ?: userId
            if (!opponentAvatarUrl.isNullOrBlank()) {
                val avatarDeleted = storageManager.deleteOpponentAvatar(actualUserId, opponentId)

            }

            deleteOpponent(opponentId)
            true

        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }





}
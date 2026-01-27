package com.example.fencing_project.data.repository

import com.example.fencing_project.data.model.Bout
import com.example.fencing_project.data.model.Opponent
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject

class BoutRepository @Inject constructor(
    private val db: FirebaseFirestore
) {

    // --- Opponent Methods ---
    suspend fun addOpponent(opponent: Opponent, userId: String): String {
        val opponentWithUser = opponent.copy(createdBy = userId)
        val docRef = db.collection("opponents").add(opponentWithUser).await()
        return docRef.id
    }

    suspend fun getOpponentsForUser(userId: String): List<Opponent> {
        val snapshot = db.collection("opponents")
            .whereEqualTo("createdBy", userId)
            .get()
            .await()

        return snapshot.documents.map { document ->
            Opponent(
                id = document.id,
                name = document.getString("name") ?: "",
                weaponHand = document.getString("weaponHand") ?: "",
                weaponType = document.getString("weaponType") ?: "",
                comment = document.getString("comment") ?: "",
                photoUrl = document.getString("photoUrl") ?: "",
                createdBy = document.getString("createdBy") ?: ""
            )
        }
    }

    // --- Bout Methods ---
    suspend fun addBout(bout: Bout): String {
        val boutData = hashMapOf(
            "opponentId" to bout.opponentId,
            "authorId" to bout.authorId,
            "userScore" to bout.userScore,
            "opponentScore" to bout.opponentScore,
            "date" to bout.date,
            "comment" to bout.comment
        )

        val docRef = db.collection("bouts").add(boutData).await()
        return docRef.id
    }

    suspend fun getBoutsForUser(userId: String): List<Bout> {
        val snapshot = db.collection("bouts")
            .whereEqualTo("authorId", userId)
            .get()
            .await()

        return snapshot.documents.map { document ->
            Bout(
                id = document.id,
                opponentId = document.getString("opponentId") ?: "",
                authorId = document.getString("authorId") ?: "",
                userScore = (document.getLong("userScore") ?: 0).toInt(),
                opponentScore = (document.getLong("opponentScore") ?: 0).toInt(),
                date = document.getDate("date") ?: Date(),
                comment = document.getString("comment") ?: ""
            )
        }
    }

    // Метод для получения всех данных сразу (для главного экрана)
    suspend fun getHomeScreenData(userId: String): Pair<List<Bout>, List<Opponent>> {
        val bouts = getBoutsForUser(userId)
        val opponents = getOpponentsForUser(userId)
        return Pair(bouts, opponents)
    }
}
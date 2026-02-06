package com.example.fencing_project.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "opponents")
data class LocalOpponent(
    @PrimaryKey(autoGenerate = true)
    val id: Long=0,
    val name: String,
    val weaponHand: String,
    val weaponType: String,
    val comment: String = "",
    val avatarPath: String = "",
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val totalBouts: Int = 0,
    val userWins: Int = 0,
    val opponentWins: Int = 0,
    val draws: Int = 0,
    val totalUserScore: Int = 0,
    val totalOpponentScore: Int = 0,
    val lastBoutDate: Long? = null
) {
    companion object {
        fun generateId(): String {
            return "opp_${System.currentTimeMillis()}_${(0..999).random()}"
        }
    }
}
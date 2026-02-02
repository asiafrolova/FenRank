package com.example.fencing_project.data.local


import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "bouts")
data class LocalBout(
    @PrimaryKey(autoGenerate = true)
    val id: Long=0,
    val opponentId: Long=0,
    val authorId: String,
    val userScore: Int,
    val opponentScore: Int,
    val date: Long,
    val comment: String = ""
) {
    companion object {
        fun generateId(): Long {
            return (0..999).random().toLong()
        }
    }
}
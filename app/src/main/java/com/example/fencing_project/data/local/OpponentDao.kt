package com.example.fencing_project.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface OpponentDao {
    @Query("SELECT * FROM opponents WHERE createdBy = :userId OR createdBy = 'offline' ORDER BY name")
    fun getOpponentsByUser(userId: String): Flow<List<LocalOpponent>>

    @Query("SELECT * FROM opponents WHERE id = :opponentId")
    suspend fun getOpponentById(opponentId: Long): LocalOpponent?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOpponent(opponent: LocalOpponent):Long

    @Update
    suspend fun updateOpponent(opponent: LocalOpponent)

    @Delete
    suspend fun deleteOpponent(opponent: LocalOpponent)

    @Query("DELETE FROM opponents WHERE id = :opponentId")
    suspend fun deleteOpponentById(opponentId: Long)
}
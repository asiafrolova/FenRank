package com.example.fencing_project.data.local


import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BoutDao {
    @Query("SELECT * FROM bouts WHERE authorId = :userId OR authorId = 'offline' ORDER BY date DESC")
    fun getBoutsByUser(userId: String): Flow<List<LocalBout>>

    @Query("SELECT * FROM bouts WHERE id = :boutId ")
    suspend fun getBoutById(boutId: Long): LocalBout?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBout(bout: LocalBout):Long

    @Update
    suspend fun updateBout(bout: LocalBout)

    @Delete
    suspend fun deleteBout(bout: LocalBout)

    @Query("DELETE FROM bouts WHERE id = :boutId")
    suspend fun deleteBoutById(boutId: Long)

    @Query("SELECT * FROM bouts WHERE opponentId = :opponentId ORDER BY date DESC")
    fun getBoutsByOpponent(opponentId: Long): Flow<List<LocalBout>>

    @Query("DELETE FROM bouts WHERE opponentId = :opponentId")
    suspend fun deleteBoutsByOpponent(opponentId: Long)
}
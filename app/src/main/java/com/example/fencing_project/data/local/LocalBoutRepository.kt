package com.example.fencing_project.data.local


import android.net.Uri
import com.example.fencing_project.data.model.Bout
import com.example.fencing_project.data.model.Opponent
import com.example.fencing_project.utils.AvatarStorageManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LocalBoutRepository @Inject constructor(
    private val boutDao: BoutDao,
    private val opponentDao: OpponentDao,
    private val avatarStorageManager: AvatarStorageManager
) {

    suspend fun addBout(bout: LocalBout): Long {
        val id = boutDao.insertBout(bout)
        updateOpponentStats(bout.opponentId, bout)
        return id
    }

    fun getBoutsByUser(userId: String): Flow<List<LocalBout>> {
        return boutDao.getBoutsByUser(userId)
            .map { localBouts ->
                localBouts.map { it }
            }
    }

    suspend fun getBout(boutId: Long): LocalBout? {
        return boutDao.getBoutById(boutId)
    }

    suspend fun updateBout(bout: LocalBout) {
        val oldBout = boutDao.getBoutById(bout.id)
        boutDao.updateBout(bout)
        if (oldBout != null) {
            updateOpponentStatsAfterEdit(oldBout, isDelete = true)
            updateOpponentStatsAfterEdit(bout, isDelete = false)
        }
    }

    suspend fun deleteBout(boutId: Long) {
        val bout = boutDao.getBoutById(boutId)
        bout?.let {
            boutDao.deleteBoutById(boutId)
            updateOpponentStatsAfterEdit(it, isDelete = true)
        }
    }


    suspend fun addOpponent(opponent: LocalOpponent): Long {
        return opponentDao.insertOpponent(opponent)

    }

    fun getOpponentsByUser(userId: String): Flow<List<LocalOpponent>> {
        return opponentDao.getOpponentsByUser(userId)
            .map { localOpponents ->
                localOpponents.map { it }
            }
    }

    suspend fun getOpponent(opponentId: Long): LocalOpponent? {
        return opponentDao.getOpponentById(opponentId)
    }

    suspend fun updateOpponent(opponent: LocalOpponent) {
        opponentDao.updateOpponent(opponent)
    }

    suspend fun deleteOpponent(opponentId: Long) {
        boutDao.deleteBoutsByOpponent(opponentId)
        opponentDao.deleteOpponentById(opponentId)
    }

    suspend fun getHomeScreenData(userId: String): Pair<List<LocalBout>, List<LocalOpponent>> {
        return try {
            val bouts = getBoutsByUser(userId).first()
            val opponents = getOpponentsByUser(userId).first()
            Pair(bouts, opponents)
        } catch (e: Exception) {
            Pair(emptyList(), emptyList())
        }
    }


    private suspend fun updateOpponentStats(opponentId: Long, bout: LocalBout) {
        val opponent = opponentDao.getOpponentById(opponentId) ?: return
        val userWon = bout.userScore > bout.opponentScore
        val opponentWon = bout.userScore < bout.opponentScore
        val isDraw = bout.userScore == bout.opponentScore
        val updatedOpponent = opponent.copy(
            totalBouts = opponent.totalBouts + 1,
            userWins = opponent.userWins + if (userWon) 1 else 0,
            opponentWins = opponent.opponentWins + if (opponentWon) 1 else 0,
            draws = opponent.draws + if (isDraw) 1 else 0,
            totalUserScore = opponent.totalUserScore + bout.userScore,
            totalOpponentScore = opponent.totalOpponentScore + bout.opponentScore,
            lastBoutDate = bout.date
        )
        opponentDao.updateOpponent(updatedOpponent)
    }

    private suspend fun updateOpponentStatsAfterEdit(bout: LocalBout, isDelete: Boolean) {
        val opponent = opponentDao.getOpponentById(bout.opponentId) ?: return
        val multiplier = if (isDelete) -1 else 1
        val userWon = bout.userScore > bout.opponentScore
        val opponentWon = bout.userScore < bout.opponentScore
        val isDraw = bout.userScore == bout.opponentScore
        val updatedOpponent = opponent.copy(
            totalBouts = opponent.totalBouts + multiplier,
            userWins = opponent.userWins + (if (userWon) multiplier else 0),
            opponentWins = opponent.opponentWins + (if (opponentWon) multiplier else 0),
            draws = opponent.draws + (if (isDraw) multiplier else 0),
            totalUserScore = opponent.totalUserScore + bout.userScore * multiplier,
            totalOpponentScore = opponent.totalOpponentScore + bout.opponentScore * multiplier
        )
        opponentDao.updateOpponent(updatedOpponent)
    }
    suspend fun deleteOpponentWithAvatar(
        opponentId: Long,
        userId: String,
        opponentAvatarUrl: String? = null
    ): Boolean {
        return try {
            val opponent = getOpponent(opponentId)
            val actualUserId = opponent?.createdBy ?: userId
            if (!opponentAvatarUrl.isNullOrBlank() || opponent?.avatarPath?.isNotBlank() == true) {
                val avatarDeleted = avatarStorageManager.deleteOpponentAvatar(actualUserId, opponentId)
            }
            deleteOpponent(opponentId)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    suspend fun deleteOpponentAvatar(
        opponentId: Long,
        userId: String,
        opponentAvatarUrl: String? = null
    ):Boolean{
        return try {
            val opponent = getOpponent(opponentId)
            val actualUserId = opponent?.createdBy ?: userId
            if (!opponentAvatarUrl.isNullOrBlank() || opponent?.avatarPath?.isNotBlank() == true) {
                val avatarDeleted = avatarStorageManager.deleteOpponentAvatar(actualUserId, opponentId)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun updateOpponentAvatar(opponentId: Long, avatarUrl: String) {
        try {
            val opponent = getOpponent(opponentId)
            if (opponent != null) {
                val updatedOpponent = opponent.copy(avatarPath = avatarUrl)
                opponentDao.updateOpponent(updatedOpponent)
            }
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun updateOpponentWithAvatar(
        opponentId: Long,
        name: String,
        weaponHand: String,
        weaponType: String,
        comment: String,
        avatarUri: Uri? = null,
        userId: String
    ): Boolean {
        return try {
            val oldOpponent = getOpponent(opponentId)
            val oldAvatarUrl = oldOpponent?.avatarPath
            var newAvatarUrl = oldAvatarUrl
            if (avatarUri != null) {
                val uploadedUrl = avatarStorageManager.uploadOpponentAvatar(
                    userId = userId,
                    opponentId = opponentId,
                    imageUri = avatarUri
                )

                if (uploadedUrl != null) {
                    newAvatarUrl = uploadedUrl
                    if (!oldAvatarUrl.isNullOrBlank()) {
                        avatarStorageManager.deleteOpponentAvatar(userId, opponentId)
                    }
                }
            }
            val updatedOpponent = LocalOpponent(
                id = opponentId,
                name = name,
                weaponHand = weaponHand,
                weaponType = weaponType,
                comment = comment,
                avatarPath = newAvatarUrl ?: "",
                createdBy = userId,
                createdAt = oldOpponent?.createdAt ?: System.currentTimeMillis(),
                totalBouts = oldOpponent?.totalBouts ?: 0,
                userWins = oldOpponent?.userWins ?: 0,
                opponentWins = oldOpponent?.opponentWins ?: 0,
                draws = oldOpponent?.draws ?: 0,
                totalUserScore = oldOpponent?.totalUserScore ?: 0,
                totalOpponentScore = oldOpponent?.totalOpponentScore ?: 0,
                lastBoutDate = oldOpponent?.lastBoutDate
            )
            updateOpponent(updatedOpponent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    suspend fun updateOpponent(
        opponentId: Long,
        name: String,
        weaponHand: String,
        weaponType: String,
        comment: String,
        avatarUrl: String
    ) {
        try {
            val opponent = getOpponent(opponentId)
            if (opponent != null) {
                val updatedOpponent = opponent.copy(
                    name = name,
                    weaponHand = weaponHand,
                    weaponType = weaponType,
                    comment = comment,
                    avatarPath = avatarUrl
                )
                updateOpponent(updatedOpponent)
            }
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun getOpponentsWithAvatars(userId: String): List<LocalOpponent> {
        return opponentDao.getOpponentsByUser(userId)
            .first()
            .map { it }
            .filter { it.avatarPath.isNotBlank() }
    }

    suspend fun backupAvatarsToCloud(userId: String): Map<Long, Boolean> {
        val results = mutableMapOf<Long, Boolean>()
        val opponents = getOpponentsWithAvatars(userId)
        opponents.forEach { opponent ->
            try {
                results[opponent.id] = true
            } catch (e: Exception) {
                results[opponent.id] = false
            }
        }
        return results
    }
}



fun Bout.toLocalBout(): LocalBout {
    return LocalBout(
        authorId = this.authorId,
        userScore = this.userScore,
        opponentScore = this.opponentScore,
        date = this.date,
        comment = this.comment
    )
}

fun LocalBout.toBout(): Bout {
    return Bout(
        id = (this.id).toString(),
        opponentId = (this.opponentId).toString(),
        authorId = this.authorId,
        userScore = this.userScore,
        opponentScore = this.opponentScore,
        date = this.date,
        comment = this.comment
    )
}

fun Opponent.toLocalOpponent(): LocalOpponent {
    return LocalOpponent(
        name = this.name,
        weaponHand = this.weaponHand,
        weaponType = this.weaponType,
        comment = this.comment,
        avatarPath = this.avatarUrl,
        createdBy = this.createdBy,
        createdAt = this.createdAt,
        totalBouts = this.totalBouts,
        userWins = this.userWins,
        opponentWins = this.opponentWins,
        draws = this.draws,
        totalUserScore = this.totalUserScore,
        totalOpponentScore = this.totalOpponentScore,
        lastBoutDate = this.lastBoutDate
    )
}

fun LocalOpponent.toOpponent(): Opponent {
    return Opponent(
        id = (this.id).toString(),
        name = this.name,
        weaponHand = this.weaponHand,
        weaponType = this.weaponType,
        comment = this.comment,
        avatarUrl = this.avatarPath,
        createdBy = this.createdBy,
        createdAt = this.createdAt,
        totalBouts = this.totalBouts,
        userWins = this.userWins,
        opponentWins = this.opponentWins,
        draws = this.draws,
        totalUserScore = this.totalUserScore,
        totalOpponentScore = this.totalOpponentScore,
        lastBoutDate = this.lastBoutDate
    )
}
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

    // === БОИ ===

    suspend fun addBout(bout: LocalBout): Long {
        //val localBout = bout.toLocalBout()
        val id = boutDao.insertBout(bout)

        // Обновляем статистику соперника
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

        // Обновляем статистику если изменился счет или соперник
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

    // === СОПЕРНИКИ ===

    suspend fun addOpponent(opponent: LocalOpponent): Long {
        //val localOpponent = opponent.toLocalOpponent()
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
        // Сначала удаляем все бои этого соперника
        boutDao.deleteBoutsByOpponent(opponentId)
        // Затем удаляем самого соперника
        opponentDao.deleteOpponentById(opponentId)
    }

    suspend fun getHomeScreenData(userId: String): Pair<List<LocalBout>, List<LocalOpponent>> {
        return try {
            // Получаем данные из Flow, используя first()
            val bouts = getBoutsByUser(userId).first()
            val opponents = getOpponentsByUser(userId).first()

            println("DEBUG: Экспорт данных - бои: ${bouts.size}, соперники: ${opponents.size}")

            Pair(bouts, opponents)
        } catch (e: Exception) {
            println("DEBUG: Ошибка получения данных для экспорта: ${e.message}")
            Pair(emptyList(), emptyList())
        }
    }

    // === СТАТИСТИКА ===

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
            println("DEBUG: Начинаем удаление соперника $opponentId с аватаркой")

            // 1. Получаем данные соперника перед удалением
            val opponent = getOpponent(opponentId)
            val actualUserId = opponent?.createdBy ?: userId

            // 2. Удаляем аватарку если есть
            if (!opponentAvatarUrl.isNullOrBlank() || opponent?.avatarPath?.isNotBlank() == true) {
                println("DEBUG: Удаляем аватарку из локального хранилища")
                val avatarDeleted = avatarStorageManager.deleteOpponentAvatar(actualUserId, opponentId)
                if (!avatarDeleted) {
                    println("DEBUG: Предупреждение: не удалось удалить аватарку")
                }
            }

            // 3. Удаляем соперника и его бои из локальной БД
            deleteOpponent(opponentId)

            println("DEBUG: Соперник успешно удален локально")
            true

        } catch (e: Exception) {
            println("DEBUG: Ошибка удаления соперника с аватаркой: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    suspend fun updateOpponentAvatar(opponentId: Long, avatarUrl: String) {
        try {
            val opponent = getOpponent(opponentId)
            if (opponent != null) {
                // Обновляем аватарку в локальной БД
                val updatedOpponent = opponent.copy(avatarPath = avatarUrl)
                opponentDao.updateOpponent(updatedOpponent)
                println("DEBUG: Аватарка обновлена для opponentId: $opponentId, ${updatedOpponent.avatarPath}")
            }
        } catch (e: Exception) {
            println("DEBUG: Ошибка обновления аватарки: ${e.message}")
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
            println("DEBUG: Обновление соперника $opponentId с аватаркой")

            // 1. Получаем старого соперника
            val oldOpponent = getOpponent(opponentId)
            val oldAvatarUrl = oldOpponent?.avatarPath
            println("DEBUG: Старая аватарка: $oldAvatarUrl")

            var newAvatarUrl = oldAvatarUrl

            // 2. Обрабатываем аватарку (если есть новая)
            if (avatarUri != null) {
                println("DEBUG: Загружаем новую аватарку...")

                // Сначала загружаем новую
                val uploadedUrl = avatarStorageManager.uploadOpponentAvatar(
                    userId = userId,
                    opponentId = opponentId,
                    imageUri = avatarUri
                )

                if (uploadedUrl != null) {
                    newAvatarUrl = uploadedUrl
                    println("DEBUG: Новая аватарка загружена: $newAvatarUrl")

                    // Удаляем старую (после успешной загрузки новой)
                    if (!oldAvatarUrl.isNullOrBlank()) {
                        println("DEBUG: Удаляем старую аватарку: $oldAvatarUrl")
                        avatarStorageManager.deleteOpponentAvatar(userId, opponentId)
                    }
                } else {
                    println("DEBUG: Не удалось загрузить новую аватарку")
                    // Оставляем старую
                }
            }

            // 3. Обновляем в локальной БД
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

            println("DEBUG: Соперник успешно обновлен с аватаркой")
            true

        } catch (e: Exception) {
            println("DEBUG: Ошибка обновления соперника с аватаркой: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    // Метод для обновления данных соперника без аватарки
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
                println("DEBUG: Соперник обновлен: $opponentId")
            }
        } catch (e: Exception) {
            println("DEBUG: Ошибка обновления соперника: ${e.message}")
            throw e
        }
    }

    // === ДОПОЛНИТЕЛЬНЫЕ МЕТОДЫ ДЛЯ СИНХРОНИЗАЦИИ ===

    suspend fun getOpponentsWithAvatars(userId: String): List<LocalOpponent> {
        return opponentDao.getOpponentsByUser(userId)
            .first() // Получаем первый элемент Flow
            .map { it }
            .filter { it.avatarPath.isNotBlank() }
    }

    suspend fun backupAvatarsToCloud(userId: String): Map<Long, Boolean> {
        val results = mutableMapOf<Long, Boolean>()
        val opponents = getOpponentsWithAvatars(userId)

        opponents.forEach { opponent ->
            try {
                // Здесь будет логика загрузки в облако
                // Пока просто помечаем как успех
                results[opponent.id] = true
            } catch (e: Exception) {
                results[opponent.id] = false
            }
        }

        return results
    }
}

// === EXTENSION ФУНКЦИИ ДЛЯ КОНВЕРТАЦИИ ===

fun Bout.toLocalBout(): LocalBout {
    return LocalBout(
        //id =  this.id,
        //opponentId = this.opponentId,
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
        //id = this.id,
        name = this.name,
        weaponHand = this.weaponHand,
        weaponType = this.weaponType,
        comment = this.comment,
        avatarPath = this.avatarUrl, // Сохраняем путь к локальной аватарке
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
        avatarUrl = this.avatarPath, // Используем локальный путь
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
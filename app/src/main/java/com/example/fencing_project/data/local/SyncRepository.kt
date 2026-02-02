package com.example.fencing_project.data.repository

import android.content.Context
import android.net.Uri
import com.example.fencing_project.data.local.LocalBout
import com.example.fencing_project.data.local.LocalBoutRepository
import com.example.fencing_project.data.local.LocalOpponent
import com.example.fencing_project.data.local.LocalStorageManager
import com.example.fencing_project.data.model.Opponent
import com.example.fencing_project.data.model.Sync
import com.example.fencing_project.utils.NetworkUtils
import com.example.fencing_project.utils.NotificationHelper
import com.example.fencing_project.utils.SupabaseStorageManager
import com.example.fencing_project.utils.UIState
import com.example.fencing_project.work.AlarmSyncScheduler

import com.example.fencing_project.work.SyncServiceManager
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepository @Inject constructor(
    private val localRepository: LocalBoutRepository,
    private val firebaseRepository: BoutRepository,
    private val authRepository: AuthRepository,
    private val storageManager: SupabaseStorageManager,
    private val localStorageManager: LocalStorageManager,
    private val database: FirebaseDatabase,
    private val syncServiceManager: SyncServiceManager,
    private val alarmSyncScheduler: AlarmSyncScheduler,
    private val networkUtils: NetworkUtils
    ) {
    private val syncRef: DatabaseReference
        get() = database.getReference("syncs")



    private val SYNC_TIMEOUT_MS = 60000L

    suspend fun getSyncByUserId(userId: String): Sync? {
        return try {
            println("DEBUG: Ищем сохранение пользователя $userId")
            val syncsSnapshot = syncRef
                .orderByChild("createdBy")
                .equalTo(userId)
                .get()
                .await()

            // Вручную ищем по roomId
            for (snapshot in syncsSnapshot.children) {
                val sync = snapshot.getValue(Sync::class.java)
                if (sync != null) {
                    println("DEBUG: Найдено сохранение: ${sync.createdAt}")
                    return sync
                }
            }

            println("DEBUG: Сохранения $userId не найдено")
            null

        } catch (e: Exception) {
            println("DEBUG: Ошибка поиска сохранений $userId: ${e.message}")
            null
        }
    }

    suspend fun deleteSync(syncId: String){
        try {
            syncRef.child(syncId).removeValue().await()
            println("DEBUG: Сохранение удалено: $syncId")
        } catch (e: Exception) {
            println("DEBUG: Ошибка удаления сохранения: ${e.message}")
            throw e
        }
    }

    suspend fun addSync(userId: String):String{
        return try {
            val sync = Sync(id="", createdBy = userId, createdAt = System.currentTimeMillis())
            val syncId = syncRef.push().key ?: throw Exception("Не удалось создать ID")
            val syncWithId = sync.copy(id = syncId)
            syncRef.child(syncId).setValue(syncWithId).await()
            syncId
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun updateSync(sync: Sync) {
        try {
            if (sync.id.isBlank()) {
                throw Exception("ID сохранения не может быть пустым")
            }
            syncRef.child(sync.id).setValue(sync).await()
            println("DEBUG: Сохранение обновлено: ${sync.createdBy}")
        } catch (e: Exception) {
            println("DEBUG: Ошибка обновления боя: ${e.message}")
            throw e
        }
    }

    private val _syncState = MutableStateFlow<UIState<String>>(UIState.Idle)
    val syncState: StateFlow<UIState<String>> = _syncState.asStateFlow()

    suspend fun syncWithFirebase(userId: String, showNotification: Boolean = true, context: Context? = null) {
        _syncState.value = UIState.Loading
        if (!networkUtils.isInternetAvailable()) {
            if (context != null && showNotification) {
                NotificationHelper.showSyncErrorNotification(context, "Ошибка синхронизации: нет подключения к интеренету")
            }
            _syncState.value = UIState.Error("Нет подключения к интернету")

            return
        }


        println("DEBUG: Начинаем синхронизацию для пользователя: $userId")

        // 1. Получаем данные с обеих сторон
        val localOpponents = localRepository.getOpponentsByUser(userId).first()
        val localBouts = localRepository.getBoutsByUser(userId).first()

        val (firebaseBouts, firebaseOpponents) = firebaseRepository.getHomeScreenData(userId)

        println("DEBUG: Локально: ${localOpponents.size} соперников, ${localBouts.size} боев")
        println("DEBUG: В Firebase: ${firebaseOpponents.size} соперников, ${firebaseBouts.size} боев")

        // 2. СИНХРОНИЗАЦИЯ СОПЕРНИКОВ
        // 2a. Загружаем локальных соперников в Firebase (если их там нет)
        var uploadedOpponents = 0
        for (localOpponent in localOpponents) {
            // Проверяем, есть ли соперник с таким именем в Firebase
            val existingInFirebase = firebaseOpponents.find {
                it.roomId == localOpponent.id && it.createdBy == userId
            }
            var createdBy= localOpponent.createdBy
            if (createdBy=="offline"){
                createdBy=authRepository.getCurrentUser()?.uid!!
            }
            if (existingInFirebase == null) {
                try {
                    // Конвертируем LocalOpponent в Opponent для Firebase
                    val firebaseOpponent = com.example.fencing_project.data.model.Opponent(
                        roomId = localOpponent.id,
                        id = "", // Firebase сам создаст
                        name = localOpponent.name,
                        weaponHand = localOpponent.weaponHand,
                        weaponType = localOpponent.weaponType,
                        comment = localOpponent.comment,
                        //avatarUrl = localOpponent.avatarPath,
                        createdBy = createdBy,
                        createdAt = localOpponent.createdAt,
                        totalBouts = localOpponent.totalBouts,
                        userWins = localOpponent.userWins,
                        opponentWins = localOpponent.opponentWins,
                        draws = localOpponent.draws,
                        totalUserScore = localOpponent.totalUserScore,
                        totalOpponentScore = localOpponent.totalOpponentScore,
                        lastBoutDate = localOpponent.lastBoutDate
                    )
                    val firebaseId = firebaseRepository.addOpponent(firebaseOpponent)
                    var avatarUrl = ""
                    println("DEBUG: OpponentId = ${firebaseId}")
                    // Если есть аватарка, загружаем ее
                    if (!localOpponent.avatarPath.isNullOrBlank()) {
                        try {
                            // Просто создаем Uri из строки пути
                            val file = File(localOpponent.avatarPath)
                            if (file.exists()) {
                                val uri = Uri.fromFile(file)
                                println("DEBUG: Загружаем аватарку из файла: ${file.absolutePath}, размер: ${file.length()} байт")

                                val uploadedUrl = storageManager.uploadOpponentAvatar(
                                    userId = localOpponent.createdBy,
                                    opponentId = localOpponent.id, // используем локальный ID для имени файла
                                    imageUri = uri
                                )

                                println("DEBUG: storage вернул ${uploadedUrl}")
                                avatarUrl = uploadedUrl ?: ""

                                if (avatarUrl.isNotBlank()) {
                                    firebaseRepository.updateOpponentAvatar(
                                        firebaseId,
                                        avatarUrl
                                    )
                                }
                            } else {
                                println("DEBUG: Файл аватарки не существует: ${localOpponent.avatarPath}")
                            }

                            uploadedOpponents++
                            println("DEBUG: Соперник '${localOpponent.name}' добавлен в Firebase с ID: $firebaseId")
                        } catch (e: Exception) {
                            println("DEBUG: Ошибка добавления соперника '${localOpponent.name}': ${e.message}")
                        }
                    }
                } catch (e: Exception) {
                    println("DEBUG: Ошибка синхронизации: ${e.message}")
                    _syncState.value = UIState.Error("Ошибка: ${e.message}")
                }
            }
        }

        // 2b. Удаляем из Firebase соперников, которых нет локально
        var deletedFromFirebase = 0
        for (firebaseOpponent in firebaseOpponents) {
            val existingLocal = localOpponents.find {
                it.id == firebaseOpponent.roomId && it.createdBy == userId
            }


            if (existingLocal == null) {
                try {
                    firebaseRepository.deleteOpponent(firebaseOpponent.id)
                    if(firebaseOpponent.avatarUrl!=""){
                        storageManager.deleteOpponentAvatar(userId,firebaseOpponent.avatarUrl.substringAfterLast('/').substringBeforeLast('.'))
                    }
                    deletedFromFirebase++
                    println("DEBUG: Соперник '${firebaseOpponent.name}' удален из Firebase (нет локально)")
                } catch (e: Exception) {
                    println("DEBUG: Ошибка удаления соперника из Firebase: ${e.message}")
                }
            }
        }

        // 3a.
        var uploadedBouts = 0
        for (localBout in localBouts) {
            val existingInFirebase = firebaseBouts.find {
                it.roomId == localBout.id && it.authorId == userId
            }
            if (existingInFirebase == null) {
                val opponent =
                    firebaseRepository.getOpponentByRoomId(localBout.opponentId, userId)
                try {
                    var createdBy= localBout.authorId
                    if (createdBy=="offline"){
                        createdBy=authRepository.getCurrentUser()?.uid!!
                    }
                    // Конвертируем LocalOpponent в Opponent для Firebase
                    val firebaseBout = com.example.fencing_project.data.model.Bout(
                        roomId = localBout.id,
                        id = "",
                        opponentId = (opponent ?: Opponent()).id,
                        roomOpponentId = localBout.opponentId,
                        authorId = createdBy,
                        userScore = localBout.userScore,
                        opponentScore = localBout.opponentScore,
                        date = localBout.date,
                        comment = localBout.comment
                    )

                    val firebaseId = firebaseRepository.addBout(firebaseBout)
                    uploadedBouts++
                    println("DEBUG: Соперник '${localBout.id}' добавлен в Firebase с ID: $firebaseId")
                } catch (e: Exception) {
                    println("DEBUG: Ошибка добавления соперника '${localBout.id}': ${e.message}")
                }
            }

        }
        var deletedBoutsFromFirebase = 0
        for (firebaseBout in firebaseBouts) {
            val existingLocal = localBouts.find {
                it.id == firebaseBout.roomId && it.authorId == userId
            }


            if (existingLocal == null) {
                try {
                    firebaseRepository.deleteBout(firebaseBout.id)
                    deletedBoutsFromFirebase++
                    println("DEBUG: Бой '${firebaseBout.id}' удален из Firebase (нет локально)")
                } catch (e: Exception) {
                    println("DEBUG: Ошибка удаления боя из Firebase: ${e.message}")
                }
            }
        }
        val userAvatar = localStorageManager.getAvatarUrl(userId,null)
        if (userAvatar!="") {
            try {
                // Просто создаем Uri из строки пути
                val file = File(userAvatar)
                if (file.exists()) {
                    val uri = Uri.fromFile(file)
                    val uploadedUrl = storageManager.uploadUserAvatar(
                        userId=userId,
                        imageUri = uri
                    )
                } else {
                    println("DEBUG: Файл аватарки не существует: ${userAvatar}")
                }

            } catch (e: Exception) {
                println("DEBUG: Ошибка добавления аватарки '${userId}': ${e.message}")
            }
        }



        // ИТОГ
        val message = buildString {
            append("Синхронизация завершена.\n")
            if (uploadedOpponents > 0) append("Добавлено в Firebase: $uploadedOpponents соперников\n")
            if (deletedFromFirebase > 0) append("Удалено из Firebase: $deletedFromFirebase соперников\n")
            if (uploadedBouts > 0) append("Добавлено в Firebase: $uploadedBouts боев\n")
            if (deletedBoutsFromFirebase > 0) append("Удалено из Firebase: $uploadedBouts боев\n")

        }

        _syncState.value = UIState.Success(message)
        println("DEBUG: Синхронизация завершена: $message")
        val sync = getSyncByUserId(userId)
        if(sync==null){
            addSync(userId)
        }else{
            updateSync(sync.copy(createdAt = System.currentTimeMillis()))
        }
        if (context != null && showNotification) {
            NotificationHelper.showSyncSuccessNotification(context, message)
        }

    }
    suspend fun updateLocalData(userId: String, showNotification: Boolean = true, context: Context? = null){
        _syncState.value = UIState.Loading

        if (!networkUtils.isInternetAvailable()) {
            if (context != null && showNotification) {
                NotificationHelper.showSyncErrorNotification(context, "Ошибка синхронизации: нет подключения к интеренету")
            }
            _syncState.value = UIState.Error("Нет подключения к интернету")

            return
        }
        val sync = getSyncByUserId(userId)
        if(sync==null){
            _syncState.value = UIState.Error("У пользователя нет сохранений")
            return
        }
        println("DEBUG: Начинаем загрузку данных из облака: $userId")
        val localOpponents = localRepository.getOpponentsByUser(userId).first()
        val localBouts = localRepository.getBoutsByUser(userId).first()
        val (firebaseBouts, firebaseOpponents) = firebaseRepository.getHomeScreenData(userId)
        println("DEBUG: Локально: ${localOpponents.size} соперников, ${localBouts.size} боев")
        println("DEBUG: В Firebase: ${firebaseOpponents.size} соперников, ${firebaseBouts.size} боев")

        // 2. СИНХРОНИЗАЦИЯ СОПЕРНИКОВ
        // 2a. Загружаем облачный соперников в локально (если их там нет)
        var uploadedOpponents = 0
        for (firebaseOpponent in firebaseOpponents) {
            // Проверяем, есть ли соперник с таким именем локально
            val existingInRoom = localOpponents.find {
                it.id == firebaseOpponent.roomId && it.createdBy == userId
            }

            if (existingInRoom == null) {
                try {
                    // Конвертируем Opponent в LocalOpponent
                    val localOpponent = LocalOpponent(
                        name = firebaseOpponent.name,
                        weaponHand = firebaseOpponent.weaponHand,
                        weaponType = firebaseOpponent.weaponType,
                        comment = firebaseOpponent.comment,
                        //avatarUrl = localOpponent.avatarPath,
                        createdBy = firebaseOpponent.createdBy,
                        createdAt = firebaseOpponent.createdAt,
                        //totalBouts = firebaseOpponent.totalBouts,
                        //userWins = firebaseOpponent.userWins,
                        //opponentWins = firebaseOpponent.opponentWins,
                        //draws = firebaseOpponent.draws,
                        //totalUserScore = firebaseOpponent.totalUserScore,
                        //totalOpponentScore = firebaseOpponent.totalOpponentScore,
                        //lastBoutDate = firebaseOpponent.lastBoutDate
                    )
                    val roomId = localRepository.addOpponent(localOpponent)
                    println("DEBUG: обновляемый соперник = ${firebaseOpponent.copy(roomId=roomId)}")
                    firebaseRepository.updateOpponent(
                        firebaseOpponent.copy(roomId=roomId)
                    )
                    uploadedOpponents++
                    var avatarUrl = ""
                    println("DEBUG: OpponentId = ${roomId}")
                    println("DEBUG: firebaseOpponent.avatarUrl= '${firebaseOpponent.avatarUrl}'")
                    // Если есть аватарка, загружаем ее
                    if (firebaseOpponent.avatarUrl!="") {
                        try {
                            // Генерируем локальный путь для сохранения
                            val localAvatarPath = localStorageManager.getAvatarPathStr(userId, roomId)
                            println("DEBUG: localAvatarPath= '${localAvatarPath}'")
                            if (localAvatarPath.isNotEmpty()) {
                                // Пробуем загрузить аватарку из Supabase
                                val success = storageManager.downloadOpponentAvatar(
                                    userId = userId,
                                    opponentId =firebaseOpponent.avatarUrl.substringAfterLast('/').substringBeforeLast('.'),
                                    saveToLocalPath = localAvatarPath
                                )

                                if (success) {
                                    // Обновляем путь в локальной базе
                                    localRepository.updateOpponentAvatar(roomId,localAvatarPath)
                                    println("DEBUG: Аватарка загружена для '${firebaseOpponent.name}'")
                                }else{

                                }
                            }
                        } catch (e: Exception) {
                            println("DEBUG: Ошибка загрузки аватарки: ${e.message}")
                        }
                    }
                } catch (e: Exception) {
                    println("DEBUG: Ошибка синхронизации: ${e.message}")
                    _syncState.value = UIState.Error("Ошибка: ${e.message}")
                }
            }
        }
        // 2b. Удаляем локально соперников, которых в Firebase
        var deletedFromFirebase = 0
        for (localOpponent in localOpponents) {
            val existingFirebase = firebaseOpponents.find {
                it.roomId == localOpponent.id && it.createdBy == userId
            }
            if (existingFirebase == null) {
                try {
                    localRepository.deleteOpponent(localOpponent.id)
                    if(localOpponent.avatarPath!=""){
                        localStorageManager.deleteOpponentAvatar(userId, localOpponent.id)
                    }
                    deletedFromFirebase++
                    println("DEBUG: Соперник '${localOpponent.name}' удален локально)")
                } catch (e: Exception) {
                    println("DEBUG: Ошибка удаления соперника локально: ${e.message}")
                }
            }
        }
        //3a загружаем локально бои из firebase
        var uploadedBouts = 0
        for (firebaseBout in firebaseBouts) {
            val existingInRoom = localBouts.find {
                it.id == firebaseBout.roomId && it.authorId == userId
            }
            if (existingInRoom == null) {
                val opponent = firebaseRepository.getOpponent(firebaseBout.opponentId)

                try {
                    // Конвертируем Opponent в LocalOpponent для Room
                    val localBout = LocalBout(
                        opponentId =(opponent?:Opponent()).roomId,
                        authorId = firebaseBout.authorId,
                        userScore = firebaseBout.userScore,
                        opponentScore = firebaseBout.opponentScore,
                        date = firebaseBout.date,
                        comment = firebaseBout.comment
                    )

                    val roomId = localRepository.addBout(localBout)
                    uploadedBouts++
                    println("DEBUG: Бой '${firebaseBout.id}' добавлен в Room с ID: $roomId")
                } catch (e: Exception) {
                    println("DEBUG: Ошибка добавления боя '${firebaseBout.id}': ${e.message}")
                }
            }

        }
        //3b удаление боев локально
        var deletedBoutsFromRoom = 0
        for (localBout in localBouts) {
            val existingFirebase = firebaseBouts.find {
                it.roomId == localBout.id && it.authorId == userId
            }
            if (existingFirebase == null) {
                try {
                    localRepository.deleteBout(localBout.id)
                    deletedBoutsFromRoom++
                    println("DEBUG: Бой '${localBout.id}' удален из локально")
                } catch (e: Exception) {
                    println("DEBUG: Ошибка удаления боя локально: ${e.message}")
                }
            }
        }

        var userAvatarDownloaded = false
        try {
            val localUserAvatarPath = localStorageManager.getAvatarPathStr(userId, null)
            if (localUserAvatarPath.isNotEmpty()) {
                userAvatarDownloaded = storageManager.downloadUserAvatar(
                    userId = userId,
                    saveToLocalPath = localUserAvatarPath
                )

                if (userAvatarDownloaded) {
                    println("DEBUG: Аватарка пользователя загружена")
                } else {
                    println("DEBUG: Аватарка пользователя не найдена в Supabase")
                }
            }
        } catch (e: Exception) {
            println("DEBUG: Ошибка загрузки аватарки пользователя: ${e.message}")
        }

        // ИТОГ
        val message = buildString {
            append("Загрукза данных завершена.\n")
            if (uploadedOpponents > 0) append("Добавлено в Room: $uploadedOpponents соперников\n")
            if (deletedFromFirebase > 0) append("Удалено из Room: $deletedFromFirebase соперников\n")
            if (uploadedBouts > 0) append("Добавлено в Room: $uploadedBouts боев\n")
            if (deletedBoutsFromRoom > 0) append("Удалено из Room: $uploadedBouts боев\n")

        }
        _syncState.value = UIState.Success(message)
        println("DEBUG: Загрузка завершена: $message")
        /////
    }
    fun startBackgroundSync(userId: String) {
        syncServiceManager.startBackgroundSync(userId) // Теперь используем service
    }

    fun scheduleSync(userId: String, frequency: AlarmSyncScheduler.Frequency, hour: Int, minute: Int) {
        alarmSyncScheduler.scheduleSync(userId, frequency, hour, minute)
    }

    fun cancelScheduledSync() {
        alarmSyncScheduler.cancelSchedule()
    }

    fun getCurrentSchedule() = alarmSyncScheduler.getCurrentSchedule()

    fun hasActiveSchedule() = alarmSyncScheduler.hasActiveSchedule()

    fun startBackgroundRestore(userId: String) {
        syncServiceManager.startBackgroundRestore(userId)
    }

//            suspend fun uploadToFirebaseOnly(userId: String) {
//                _syncState.value = UIState.Loading
//
//                try {
//                    println("DEBUG: Только загрузка в Firebase для: $userId")
//
//                    val localOpponents = localRepository.getOpponentsByUser(userId).first()
//                    val localBouts = localRepository.getBoutsByUser(userId).first()
//
//                    // Просто загружаем все локальные данные в Firebase
//                    // (Firebase автоматически перезапишет дубликаты по уникальному имени)
//                    var uploadedOpponents = 0
//                    var uploadedBouts = 0
//
//                    for (localOpponent in localOpponents) {
//                        try {
//                            val firebaseOpponent = com.example.fencing_project.data.model.Opponent(
//                                id = localOpponent.id.toString(), // Используем локальный ID как ключ
//                                name = localOpponent.name,
//                                weaponHand = localOpponent.weaponHand,
//                                weaponType = localOpponent.weaponType,
//                                comment = localOpponent.comment,
//                                avatarUrl = localOpponent.avatarPath,
//                                createdBy = localOpponent.createdBy,
//                                createdAt = localOpponent.createdAt,
//                                totalBouts = localOpponent.totalBouts,
//                                userWins = localOpponent.userWins,
//                                opponentWins = localOpponent.opponentWins,
//                                draws = localOpponent.draws,
//                                totalUserScore = localOpponent.totalUserScore,
//                                totalOpponentScore = localOpponent.totalOpponentScore,
//                                lastBoutDate = localOpponent.lastBoutDate
//                            )
//
//                            // Используем локальный ID как ключ в Firebase
//                            firebaseRepository.updateOpponent(
//                                firebaseOpponent.id,
//                                firebaseOpponent.name,
//                                firebaseOpponent.weaponHand,
//                                firebaseOpponent.weaponType,
//                                firebaseOpponent.comment,
//                                firebaseOpponent.avatarUrl
//                            )
//                            uploadedOpponents++
//                        } catch (e: Exception) {
//                            println("DEBUG: Ошибка загрузки соперника: ${e.message}")
//                        }
//                    }
//
//                    _syncState.value =
//                        UIState.Success("Загружено в Firebase: $uploadedOpponents соперников, $uploadedBouts боев")
//
//                } catch (e: Exception) {
//                    _syncState.value = UIState.Error("Ошибка: ${e.message}")
//                }
//            }

//            suspend fun downloadFromFirebaseOnly(userId: String) {
//                _syncState.value = UIState.Loading
//
//                try {
//                    println("DEBUG: Только загрузка из Firebase для: $userId")
//
//                    // 1. Очищаем локальные данные пользователя
//                    val localOpponents = localRepository.getOpponentsByUser(userId).first()
//                    for (opponent in localOpponents) {
//                        localRepository.deleteOpponent(opponent.id)
//                    }
//                    println("DEBUG: Локальные данные очищены")
//
//                    // 2. Загружаем из Firebase
//                    val (firebaseBouts, firebaseOpponents) = firebaseRepository.getHomeScreenData(
//                        userId
//                    )
//
//                    var downloadedOpponents = 0
//                    for (firebaseOpponent in firebaseOpponents) {
//                        try {
//                            val localOpponent = LocalOpponent(
//                                id = firebaseOpponent.id.toLongOrNull() ?: 0,
//                                name = firebaseOpponent.name,
//                                weaponHand = firebaseOpponent.weaponHand,
//                                weaponType = firebaseOpponent.weaponType,
//                                comment = firebaseOpponent.comment,
//                                avatarPath = firebaseOpponent.avatarUrl,
//                                createdBy = firebaseOpponent.createdBy,
//                                createdAt = firebaseOpponent.createdAt,
//                                totalBouts = firebaseOpponent.totalBouts,
//                                userWins = firebaseOpponent.userWins,
//                                opponentWins = firebaseOpponent.opponentWins,
//                                draws = firebaseOpponent.draws,
//                                totalUserScore = firebaseOpponent.totalUserScore,
//                                totalOpponentScore = firebaseOpponent.totalOpponentScore,
//                                lastBoutDate = firebaseOpponent.lastBoutDate
//                            )
//
//                            localRepository.addOpponent(localOpponent)
//                            downloadedOpponents++
//                        } catch (e: Exception) {
//                            println("DEBUG: Ошибка загрузки соперника: ${e.message}")
//                        }
//                    }
//
//                    _syncState.value =
//                        UIState.Success("Загружено из Firebase: $downloadedOpponents соперников")
//
//                } catch (e: Exception) {
//                    _syncState.value = UIState.Error("Ошибка: ${e.message}")
//                }
//            }

    fun resetSyncState() {
        _syncState.value = UIState.Idle
    }
}


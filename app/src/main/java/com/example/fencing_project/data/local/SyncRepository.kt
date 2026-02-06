package com.example.fencing_project.data.repository

import android.content.Context
import android.net.Uri
import com.example.fencing_project.R
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
    private val networkUtils: NetworkUtils,

    ) {
    private val syncRef: DatabaseReference
        get() = database.getReference("syncs")

    suspend fun getSyncByUserId(userId: String): Sync? {
        return try {
            val syncsSnapshot = syncRef
                .orderByChild("createdBy")
                .equalTo(userId)
                .get()
                .await()

            for (snapshot in syncsSnapshot.children) {
                val sync = snapshot.getValue(Sync::class.java)
                if (sync != null) {
                    return sync
                }
            }

            null

        } catch (e: Exception) {
            null
        }
    }

    suspend fun deleteSync(syncId: String){
        try {
            syncRef.child(syncId).removeValue().await()
        } catch (e: Exception) {
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
        } catch (e: Exception) {
            throw e
        }
    }

    private val _syncState = MutableStateFlow<UIState<String>>(UIState.Idle)
    val syncState: StateFlow<UIState<String>> = _syncState.asStateFlow()
    private val _restoreState =  MutableStateFlow<UIState<String>>(UIState.Idle)
    val restoreState: StateFlow<UIState<String>> = _restoreState.asStateFlow()

    suspend fun syncWithFirebase(userId: String, showNotification: Boolean = true, context: Context? = null) {
        _syncState.value = UIState.Loading
        if (!networkUtils.isInternetAvailable()) {
            if (context != null && showNotification) {
                NotificationHelper.showSyncErrorNotification(context,
                    context.getString(R.string.sync_error_no_internet_connection))
            }
            _syncState.value = UIState.Error(context?.getString(R.string.no_internet_connection) ?: "Нет подключения к интернету")

            return
        }

        val localOpponents = localRepository.getOpponentsByUser(userId).first()
        val localBouts = localRepository.getBoutsByUser(userId).first()
        val (firebaseBouts, firebaseOpponents) = firebaseRepository.getHomeScreenData(userId)
        var uploadedOpponents = 0
        for (localOpponent in localOpponents) {
            val existingInFirebase = firebaseOpponents.find {
                it.roomId == localOpponent.id && it.createdBy == userId
            }
            var createdBy= localOpponent.createdBy
            if (createdBy=="offline"){
                createdBy=authRepository.getCurrentUser()?.uid!!
            }
            if (existingInFirebase == null) {
                try {
                    val firebaseOpponent = Opponent(
                        roomId = localOpponent.id,
                        id = "",
                        name = localOpponent.name,
                        weaponHand = localOpponent.weaponHand,
                        weaponType = localOpponent.weaponType,
                        comment = localOpponent.comment,
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
                    if (!localOpponent.avatarPath.isNullOrBlank()) {
                        try {
                            val file = File(localOpponent.avatarPath)
                            if (file.exists()) {
                                val uri = Uri.fromFile(file)
                                val uploadedUrl = storageManager.uploadOpponentAvatar(
                                    userId = localOpponent.createdBy,
                                    opponentId = localOpponent.id,
                                    imageUri = uri
                                )
                                avatarUrl = uploadedUrl ?: ""
                                if (avatarUrl.isNotBlank()) {
                                    firebaseRepository.updateOpponentAvatar(
                                        firebaseId,
                                        avatarUrl
                                    )
                                }
                            }
                            uploadedOpponents++
                        } catch (e: Exception) {
                            throw  e
                        }
                    }
                } catch (e: Exception) {
                    _syncState.value = UIState.Error(context?.getString(R.string.error, e.message)
                        ?: "Ошибка: ${e.message}")
                }
            }
        }

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
                } catch (e: Exception) {
                    throw e
                }
            }
        }

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
                } catch (e: Exception) {
                    throw e
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
                } catch (e: Exception) {
                   throw e
                }
            }
        }
        val userAvatar = localStorageManager.getAvatarUrl(userId,null)
        if (userAvatar!="") {
            try {
                val file = File(userAvatar)
                if (file.exists()) {
                    val uri = Uri.fromFile(file)
                    val uploadedUrl = storageManager.uploadUserAvatar(
                        userId=userId,
                        imageUri = uri
                    )
                }

            } catch (e: Exception) {
                throw e
            }
        }

        val message = buildString {
            append("Синхронизация завершена.\n")
            if (uploadedOpponents > 0) append("Добавлено в Firebase: $uploadedOpponents соперников\n")
            if (deletedFromFirebase > 0) append("Удалено из Firebase: $deletedFromFirebase соперников\n")
            if (uploadedBouts > 0) append("Добавлено в Firebase: $uploadedBouts боев\n")
            if (deletedBoutsFromFirebase > 0) append("Удалено из Firebase: $uploadedBouts боев\n")

        }

        _syncState.value = UIState.Success(message)
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
                NotificationHelper.showSyncErrorNotification(context, context.getString(R.string.sync_error_no_internet_connection))
            }
            _syncState.value = UIState.Error(context?.getString(R.string.no_internet_connection)
                ?: "Нет подключения к интернету")

            return
        }
        val sync = getSyncByUserId(userId)
        if(sync==null){
            _syncState.value = UIState.Error(context?.getString(R.string.the_user_has_no_saves) ?: "У пользователя нет сохранений")
            return
        }
        val localOpponents = localRepository.getOpponentsByUser(userId).first()
        val localBouts = localRepository.getBoutsByUser(userId).first()
        val (firebaseBouts, firebaseOpponents) = firebaseRepository.getHomeScreenData(userId)

        var uploadedOpponents = 0
        for (firebaseOpponent in firebaseOpponents) {
            val existingInRoom = localOpponents.find {
                it.id == firebaseOpponent.roomId && it.createdBy == userId
            }

            if (existingInRoom == null) {
                try {
                    val localOpponent = LocalOpponent(
                        name = firebaseOpponent.name,
                        weaponHand = firebaseOpponent.weaponHand,
                        weaponType = firebaseOpponent.weaponType,
                        comment = firebaseOpponent.comment,
                        createdBy = firebaseOpponent.createdBy,
                        createdAt = firebaseOpponent.createdAt,
                    )
                    val roomId = localRepository.addOpponent(localOpponent)
                    firebaseRepository.updateOpponent(
                        firebaseOpponent.copy(roomId=roomId)
                    )
                    uploadedOpponents++
                    var avatarUrl = ""
                    if (firebaseOpponent.avatarUrl!="") {
                        try {
                            val localAvatarPath = localStorageManager.getAvatarPathStr(userId, roomId)
                            if (localAvatarPath.isNotEmpty()) {
                                val success = storageManager.downloadOpponentAvatar(
                                    userId = userId,
                                    opponentId =firebaseOpponent.avatarUrl.substringAfterLast('/').substringBeforeLast('.'),
                                    saveToLocalPath = localAvatarPath
                                )
                                if (success) {
                                    localRepository.updateOpponentAvatar(roomId,localAvatarPath)
                                }
                            }
                        } catch (e: Exception) {
                            throw e
                        }
                    }
                } catch (e: Exception) {
                    _syncState.value = UIState.Error(context?.getString(R.string.error, e.message)
                        ?: "Ошибка: ${e.message}")
                }
            }
        }
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
                } catch (e: Exception) {
                    throw e
                }
            }
        }

        var uploadedBouts = 0
        for (firebaseBout in firebaseBouts) {
            val existingInRoom = localBouts.find {
                it.id == firebaseBout.roomId && it.authorId == userId
            }
            if (existingInRoom == null) {
                val opponent = firebaseRepository.getOpponent(firebaseBout.opponentId)
                try {
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

                } catch (e: Exception) {
                    throw e
                }
            }

        }
        var deletedBoutsFromRoom = 0
        for (localBout in localBouts) {
            val existingFirebase = firebaseBouts.find {
                it.roomId == localBout.id && it.authorId == userId
            }
            if (existingFirebase == null) {
                try {
                    localRepository.deleteBout(localBout.id)
                    deletedBoutsFromRoom++

                } catch (e: Exception) {

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

            }
        } catch (e: Exception) {
            throw e
        }

        val message = buildString {
            append("Загрукза данных завершена.\n")
            if (uploadedOpponents > 0) append("Добавлено в Room: $uploadedOpponents соперников\n")
            if (deletedFromFirebase > 0) append("Удалено из Room: $deletedFromFirebase соперников\n")
            if (uploadedBouts > 0) append("Добавлено в Room: $uploadedBouts боев\n")
            if (deletedBoutsFromRoom > 0) append("Удалено из Room: $uploadedBouts боев\n")

        }
        _syncState.value = UIState.Success(message)

    }
    fun startBackgroundSync(userId: String) {
        syncServiceManager.startBackgroundSync(userId)
    }

    /*fun scheduleSync(userId: String, frequency: AlarmSyncScheduler.Frequency, hour: Int, minute: Int) {
        alarmSyncScheduler.scheduleSync(userId, frequency, hour, minute)
    }

    fun cancelScheduledSync() {
        alarmSyncScheduler.cancelSchedule()
    }

    fun getCurrentSchedule() = alarmSyncScheduler.getCurrentSchedule()

    fun hasActiveSchedule() = alarmSyncScheduler.hasActiveSchedule()*/

    fun startBackgroundRestore(
        userId: String,
    ) {
        syncServiceManager.startBackgroundRestore(userId)
    }


    fun resetSyncState() {
        _syncState.value = UIState.Idle
    }
}


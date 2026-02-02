package com.example.fencing_project.viewmodel

import android.content.Context
import com.example.fencing_project.data.repository.AuthRepository

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fencing_project.data.repository.SyncRepository
import com.example.fencing_project.utils.NetworkUtils
import com.example.fencing_project.utils.UIState
import com.example.fencing_project.work.AlarmSyncScheduler

import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SyncViewModel @Inject constructor(
    private val syncRepository: SyncRepository,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context,
    private val networkUtils: NetworkUtils
) : ViewModel() {

    private val _syncState = MutableStateFlow<UIState<String>>(UIState.Idle)
    val syncState = _syncState.asStateFlow()
    private val _lastSyncDate = MutableStateFlow(0L)
    val lastSyncDate: StateFlow<Long> = _lastSyncDate.asStateFlow()

    private val _hasSyncData = MutableStateFlow(false)
    val hasSyncData: StateFlow<Boolean> = _hasSyncData.asStateFlow()

//    fun syncData() {
//        val userId = authRepository.getCurrentUser()?.uid
//        if (userId != null) {
//            viewModelScope.launch {
//                syncRepository.syncWithFirebase(userId)
//            }
//        } else {
//            _syncState.value = UIState.Error("Войдите в аккаунт")
//        }
//    }

//    fun uploadToCloud() {
//        _syncState.value = UIState.Loading
//        val userId = authRepository.getCurrentUser()?.uid
//        if (userId != null) {
//            viewModelScope.launch {
//                syncRepository.syncWithFirebase(userId)
//
//            }
//            _syncState.value = UIState.Success("OK")
//        } else {
//            _syncState.value = UIState.Error("Войдите в аккаунт")
//        }
//    }

    fun startBackgroundRestore() {
        val userId = authRepository.getUserId()
        if (userId != null) {
            syncRepository.startBackgroundRestore(userId)
            _syncState.value = UIState.Success("Восстановление запущено в фоне")
        } else {
            _syncState.value = UIState.Error("Войдите в аккаунт")
        }
    }

    fun checkSyncData() {
        viewModelScope.launch {
            _syncState.value = UIState.Loading
            val userId = authRepository.getUserId()
            val sync = syncRepository.getSyncByUserId(userId = userId?:"")
            if (sync!=null){
                _lastSyncDate.value = sync.createdAt
                _hasSyncData.value = true
            }
            _syncState.value = UIState.Success("OK")

        }
    }

    fun loadDataFromCloud(){
        if (!networkUtils.isInternetAvailable()) {
            _syncState.value = UIState.Error("Нет подключения к интернету")
            return
        }
        _syncState.value = UIState.Loading
        val userId = authRepository.getUserId()
        if (userId != null) {
            viewModelScope.launch {
                syncRepository.updateLocalData(userId, showNotification = true,context)
                _syncState.value = UIState.Success("OK")
            }
        } else {
            _syncState.value = UIState.Error("Войдите в аккаунт")
        }
    }
    fun uploadToCloud() {
        if (!networkUtils.isInternetAvailable()) {
            _syncState.value = UIState.Error("Нет подключения к интернету")
            return
        }
        _syncState.value = UIState.Loading
        val userId = authRepository.getUserId()
        if (userId != null) {
            viewModelScope.launch {
                syncRepository.syncWithFirebase(userId, showNotification = true,context)
                _syncState.value = UIState.Success("OK")
            }
        } else {
            _syncState.value = UIState.Error("Войдите в аккаунт")
        }
    }
    fun startBackgroundSync() {
        val userId = authRepository.getUserId()
        if (userId != null) {
            syncRepository.startBackgroundSync(userId)
            _syncState.value = UIState.Success("Синхронизация запущена в фоне")
        } else {
            _syncState.value = UIState.Error("Войдите в аккаунт")
        }
    }
    fun scheduleSync(frequency: AlarmSyncScheduler.Frequency, hour: Int, minute: Int) {
        val userId = authRepository.getUserId()
        println("DEBUG: userId = ${userId}")
        if (userId != null) {
            syncRepository.scheduleSync(userId, frequency, hour, minute)
        }
    }

    fun cancelScheduledSync() {
        syncRepository.cancelScheduledSync()
    }

    fun getCurrentSchedule() = syncRepository.getCurrentSchedule()

    fun hasActiveSchedule() = syncRepository.hasActiveSchedule()

//    fun downloadFromCloud() {
//        val userId = authRepository.getCurrentUser()?.uid
//        if (userId != null) {
//            viewModelScope.launch {
//                syncRepository.downloadFromFirebaseOnly(userId)
//            }
//        } else {
//            _syncState.value = UIState.Error("Войдите в аккаунт")
//        }
//    }

    fun resetState() {
        _syncState.value = UIState.Idle
    }
    fun resetSyncState() {
        if (_syncState.value !is UIState.Loading) {
            _syncState.value = UIState.Idle
        }
    }
}

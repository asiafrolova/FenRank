package com.example.fencing_project.viewmodel

import android.content.Context
import com.example.fencing_project.data.repository.AuthRepository

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fencing_project.data.repository.SyncRepository
import com.example.fencing_project.utils.NetworkUtils
import com.example.fencing_project.utils.UIState
import com.example.fencing_project.work.SyncServiceManager

import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class SyncViewModel @Inject constructor(
    private val syncRepository: SyncRepository,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context,
    private val networkUtils: NetworkUtils,
    private val syncServiceManager: SyncServiceManager,
) : ViewModel() {

    private val _syncState = MutableStateFlow<UIState<String>>(UIState.Idle)
    val syncState = _syncState.asStateFlow()
    private val _lastSyncDate = MutableStateFlow(0L)
    val lastSyncDate: StateFlow<Long> = _lastSyncDate.asStateFlow()

    private val _hasSyncData = MutableStateFlow(false)
    val hasSyncData: StateFlow<Boolean> = _hasSyncData.asStateFlow()


    fun setupSyncSchedule(
        userId: String,
        frequency: SyncServiceManager.ScheduleFrequency,
        hour: Int,
        minute: Int,
        dayOfWeek: Int = -1,
        dayOfMonth: Int = -1
    ) {
        val syncManager = SyncServiceManager(context)

        when(frequency) {
            SyncServiceManager.ScheduleFrequency.DAILY -> {
                syncManager.scheduleDailySync(userId, hour, minute)
            }
            SyncServiceManager.ScheduleFrequency.WEEKLY -> {

                val calendarDayOfWeek = when(dayOfWeek) {
                    0 -> Calendar.MONDAY
                    1 -> Calendar.TUESDAY
                    2 -> Calendar.WEDNESDAY
                    3 -> Calendar.THURSDAY
                    4 -> Calendar.FRIDAY
                    5 -> Calendar.SATURDAY
                    6 -> Calendar.SUNDAY
                    else -> Calendar.MONDAY
                }
                syncManager.scheduleWeeklySync(userId, hour, minute, calendarDayOfWeek)
            }
            SyncServiceManager.ScheduleFrequency.MONTHLY -> {
                val safeDayOfMonth = dayOfMonth.coerceIn(1, 31)
                syncManager.scheduleMonthlySync(userId, hour, minute, safeDayOfMonth)
            }
            SyncServiceManager.ScheduleFrequency.DISABLED -> {
                syncManager.cancelAllScheduledSyncs()
            }
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

    fun resetState() {
        _syncState.value = UIState.Idle
    }
    fun resetSyncState() {
        if (_syncState.value !is UIState.Loading) {
            _syncState.value = UIState.Idle
        }
    }


    fun cancelScheduledSync() {
        syncServiceManager.cancelAllScheduledSyncs()
    }

    fun getCurrentSchedule() = syncServiceManager.getCurrentSchedule()

    fun hasActiveSchedule() = syncServiceManager.hasActiveSchedule()

    fun startBackgroundSync() {
        val userId = authRepository.getUserId()
        if (userId != null) {
            _syncState.value = UIState.Success("Синхронизация запущена в фоне")
            syncServiceManager.startBackgroundSync(userId)
        }else {
            _syncState.value = UIState.Error("Войдите в аккаунт")
        }
    }

    fun startBackgroundRestore() {
        val userId = authRepository.getUserId()
        if (userId != null) {
            _syncState.value = UIState.Success("Синхронизация запущена в фоне")
            syncServiceManager.startBackgroundRestore(userId)
        }else {
            _syncState.value = UIState.Error("Войдите в аккаунт")
        }
    }
    fun setupDailySync(userId: String, hour: Int, minute: Int) {
        val syncManager = SyncServiceManager(context)
        syncManager.scheduleDailySync(userId, hour, minute)
    }
}

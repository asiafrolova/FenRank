package com.example.fencing_project.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy

import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncServiceManager @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val SYNC_WORK_TAG = "background_sync"
        private const val RESTORE_WORK_TAG = "background_restore"
        private const val PERIODIC_SYNC_WORK_TAG = "periodic_sync"
    }

    fun startBackgroundSync(userId: String, showNotification: Boolean = true) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val inputData = workDataOf(
            SyncWorker.KEY_USER_ID to userId,
            SyncWorker.KEY_SHOW_NOTIFICATION to showNotification,
            SyncWorker.KEY_TYPE to "sync"
        )

        val workRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .addTag(SYNC_WORK_TAG)
            .build()

        WorkManager.getInstance(context)
            .enqueue(workRequest)
    }

    fun startBackgroundRestore(userId: String, showNotification: Boolean = true) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val inputData = workDataOf(
            RestoreWorker.KEY_USER_ID to userId,
            RestoreWorker.KEY_SHOW_NOTIFICATION to showNotification
        )

        val workRequest = OneTimeWorkRequestBuilder<RestoreWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .addTag(RESTORE_WORK_TAG)
            .build()

        WorkManager.getInstance(context)
            .enqueue(workRequest)
    }

    fun schedulePeriodicSync(
        userId: String,
        frequency: ScheduleFrequency,
        hour: Int,
        minute: Int
    ) {
        cancelScheduledSync()

        val constraints = Constraints.Builder()
            .build()

        val inputData = workDataOf(
            SyncWorker.KEY_USER_ID to userId,
            SyncWorker.KEY_SHOW_NOTIFICATION to true,
            SyncWorker.KEY_TYPE to "sync"
        )


        val initialDelay = calculateInitialDelay(hour, minute)

        val periodicWorkRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            frequency.repeatInterval, frequency.timeUnit
        )
            .setConstraints(constraints)
            .setInputData(inputData)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .addTag(PERIODIC_SYNC_WORK_TAG)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                PERIODIC_SYNC_WORK_TAG,
                ExistingPeriodicWorkPolicy.REPLACE,
                periodicWorkRequest
            )


        saveSchedulePreferences(userId, frequency, hour, minute)
    }

    fun cancelScheduledSync() {
        WorkManager.getInstance(context)
            .cancelAllWorkByTag(PERIODIC_SYNC_WORK_TAG)
        clearSchedulePreferences()
    }

    fun getCurrentSchedule(): ScheduleInfo? {
        val prefs = context.getSharedPreferences("sync_schedule", Context.MODE_PRIVATE)
        val userId = prefs.getString("user_id", null)
        val frequencyOrdinal = prefs.getInt("frequency", -1)
        val hour = prefs.getInt("hour", 2)
        val minute = prefs.getInt("minute", 0)

        if (userId == null || frequencyOrdinal == -1) {
            return null
        }

        val frequency = ScheduleFrequency.values().getOrNull(frequencyOrdinal) ?: return null
        return ScheduleInfo(userId, frequency, hour, minute)
    }

    fun hasActiveSchedule(): Boolean {
        return getCurrentSchedule() != null
    }

    private fun calculateInitialDelay(hour: Int, minute: Int): Long {
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, minute)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)

            if (timeInMillis <= System.currentTimeMillis()) {
                add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
        }
        return calendar.timeInMillis - System.currentTimeMillis()
    }

    private fun saveSchedulePreferences(
        userId: String,
        frequency: ScheduleFrequency,
        hour: Int,
        minute: Int
    ) {
        val prefs = context.getSharedPreferences("sync_schedule", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("user_id", userId)
            .putInt("frequency", frequency.ordinal)
            .putInt("hour", hour)
            .putInt("minute", minute)
            .apply()
    }

    private fun clearSchedulePreferences() {
        val prefs = context.getSharedPreferences("sync_schedule", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    data class ScheduleInfo(
        val userId: String,
        val frequency: ScheduleFrequency,
        val hour: Int,
        val minute: Int
    )

    enum class ScheduleFrequency(
        val displayName: String,
        val repeatInterval: Long,
        val timeUnit: TimeUnit
    ) {


        DAILY("Раз в день", 1, TimeUnit.DAYS),
        WEEKLY("Раз в неделю", 7, TimeUnit.DAYS),
        MONTHLY("Раз в месяц", 30, TimeUnit.DAYS),
        DISABLED("Отключено", 0, TimeUnit.DAYS)
    }

    fun startBackgroundSync(userId: String) {
        BackgroundSyncService.startService(context, userId)
    }
    fun startBackgroundRestore(userId: String) {
        BackgroundRestoreService.startService(context, userId)
    }

    fun stopBackgroundSync() {
        BackgroundSyncService.stopService(context)
    }
    fun stopBackgroundRestore() {
        BackgroundRestoreService.stopService(context)
    }
}
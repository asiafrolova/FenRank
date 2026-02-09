package com.example.fencing_project.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.example.fencing_project.utils.NotificationHelper


class SyncReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SyncReceiver"
        const val ACTION_SYNC = "com.example.fencing_project.ACTION_SYNC"
        private const val ACTION_SCHEDULED_SYNC = "com.example.fencing_project.ACTION_SCHEDULED_SYNC"
        private const val WAKE_LOCK_TAG = "fencing_app:sync_wakelock"
    }

    override fun onReceive(context: Context, intent: Intent) {

        val wakeLock = acquireWakeLock(context)

        try {
            when (intent.action) {
                ACTION_SYNC,
                ACTION_SCHEDULED_SYNC -> {
                    val userId = intent.getStringExtra("user_id")
                    val frequencyName = intent.getStringExtra("frequency")
                    val frequency = if (frequencyName != null) {
                        SyncServiceManager.ScheduleFrequency.valueOf(frequencyName)
                    } else {
                        SyncServiceManager.ScheduleFrequency.DAILY
                    }

                    if (userId != null) {
                        GuaranteedSyncService.startService(context, userId, true)

                        val alarmScheduler = AlarmSyncScheduler(context)
                        val prefs = context.getSharedPreferences("alarm_schedule", Context.MODE_PRIVATE)

                        val hour = prefs.getInt("hour", 2)
                        val minute = prefs.getInt("minute", 0)
                        val dayOfWeek = prefs.getInt("day_of_week", -1)
                        val dayOfMonth = prefs.getInt("day_of_month", -1)

                        alarmScheduler.scheduleSync(hour, minute, userId, frequency, dayOfWeek, dayOfMonth)
                    }
                }

                Intent.ACTION_BOOT_COMPLETED -> {
                    restoreSchedule(context)
                }
            }
        } finally {
            wakeLock?.release()
        }
    }

    private fun acquireWakeLock(context: Context): PowerManager.WakeLock? {
        return try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                WAKE_LOCK_TAG
            )
            wakeLock.acquire(10 * 60 * 1000L)
            wakeLock
        } catch (e: Exception) {
            null
        }
    }

    private fun restoreSchedule(context: Context) {
        val alarmScheduler = AlarmSyncScheduler(context)
        alarmScheduler.restoreScheduleIfNeeded()
    }

}
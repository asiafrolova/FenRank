package com.example.fencing_project.work

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.getSystemService
import java.util.*

class AlarmSyncScheduler(private val context: Context) {

    companion object {
        private const val DAILY_ALARM_REQUEST_CODE = 1001
        private const val WEEKLY_ALARM_REQUEST_CODE = 1002
        private const val MONTHLY_ALARM_REQUEST_CODE = 1003
        private const val ACTION_SCHEDULED_SYNC = "com.example.fencing_project.ACTION_SCHEDULED_SYNC"
    }

    fun scheduleSync(
        hour: Int,
        minute: Int,
        userId: String,
        frequency: SyncServiceManager.ScheduleFrequency = SyncServiceManager.ScheduleFrequency.DAILY,
        dayOfWeek: Int = -1,
        dayOfMonth: Int = -1
    ) {
        cancelAllScheduledSyncs()

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, SyncReceiver::class.java).apply {
            action = ACTION_SCHEDULED_SYNC
            putExtra("user_id", userId)
            putExtra("frequency", frequency.name)
        }

        val requestCode = when(frequency) {
            SyncServiceManager.ScheduleFrequency.DAILY -> DAILY_ALARM_REQUEST_CODE
            SyncServiceManager.ScheduleFrequency.WEEKLY -> WEEKLY_ALARM_REQUEST_CODE
            SyncServiceManager.ScheduleFrequency.MONTHLY -> MONTHLY_ALARM_REQUEST_CODE
            else -> DAILY_ALARM_REQUEST_CODE
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = when(frequency) {
            SyncServiceManager.ScheduleFrequency.DAILY -> calculateDailyTriggerTime(hour, minute)
            SyncServiceManager.ScheduleFrequency.WEEKLY -> calculateWeeklyTriggerTime(hour, minute, dayOfWeek)
            SyncServiceManager.ScheduleFrequency.MONTHLY -> calculateMonthlyTriggerTime(hour, minute, dayOfMonth)
            else -> calculateDailyTriggerTime(hour, minute)
        }

        setAlarm(alarmManager, triggerTime, pendingIntent)

        saveSchedulePreferences(hour, minute, userId, frequency, dayOfWeek, dayOfMonth)
    }

    private fun calculateDailyTriggerTime(hour: Int, minute: Int): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return calendar.timeInMillis
    }

    private fun calculateWeeklyTriggerTime(hour: Int, minute: Int, dayOfWeek: Int): Long {
        val calendar = Calendar.getInstance().apply {

            val currentDayOfWeek = get(Calendar.DAY_OF_WEEK)
            val targetDayOfWeek = if (dayOfWeek in 1..7) dayOfWeek else Calendar.MONDAY

            var daysToAdd = targetDayOfWeek - currentDayOfWeek
            if (daysToAdd < 0) {
                daysToAdd += 7
            }

            add(Calendar.DAY_OF_YEAR, daysToAdd)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)


            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 7)
            }
        }
        return calendar.timeInMillis
    }

    private fun calculateMonthlyTriggerTime(hour: Int, minute: Int, dayOfMonth: Int): Long {
        val calendar = Calendar.getInstance().apply {
            val currentDayOfMonth = get(Calendar.DAY_OF_MONTH)
            val targetDayOfMonth = if (dayOfMonth in 1..31) dayOfMonth else 1

            if (currentDayOfMonth < targetDayOfMonth) {

                set(Calendar.DAY_OF_MONTH, targetDayOfMonth)
            } else {

                add(Calendar.MONTH, 1)
                set(Calendar.DAY_OF_MONTH, targetDayOfMonth)
            }

            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            if (get(Calendar.DAY_OF_MONTH) != targetDayOfMonth) {
                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            }

            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.MONTH, 1)
                set(Calendar.DAY_OF_MONTH, targetDayOfMonth)
                if (get(Calendar.DAY_OF_MONTH) != targetDayOfMonth) {
                    set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                }
            }
        }
        return calendar.timeInMillis
    }

    private fun setAlarm(alarmManager: AlarmManager, triggerTime: Long, pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } else {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    private fun saveSchedulePreferences(
        hour: Int,
        minute: Int,
        userId: String,
        frequency: SyncServiceManager.ScheduleFrequency,
        dayOfWeek: Int,
        dayOfMonth: Int
    ) {
        val prefs = context.getSharedPreferences("alarm_schedule", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putInt("hour", hour)
            putInt("minute", minute)
            putString("user_id", userId)
            putString("frequency", frequency.name)
            putInt("day_of_week", dayOfWeek)
            putInt("day_of_month", dayOfMonth)
            putBoolean("is_scheduled", true)
        }.apply()
    }

    fun cancelAllScheduledSyncs() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        listOf(DAILY_ALARM_REQUEST_CODE, WEEKLY_ALARM_REQUEST_CODE, MONTHLY_ALARM_REQUEST_CODE)
            .forEach { requestCode ->
                val intent = Intent(context, SyncReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.cancel(pendingIntent)
            }

        val prefs = context.getSharedPreferences("alarm_schedule", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    fun restoreScheduleIfNeeded() {
        val prefs = context.getSharedPreferences("alarm_schedule", Context.MODE_PRIVATE)
        if (prefs.getBoolean("is_scheduled", false)) {
            val hour = prefs.getInt("hour", 2)
            val minute = prefs.getInt("minute", 0)
            val userId = prefs.getString("user_id", null)
            val frequencyName = prefs.getString("frequency", SyncServiceManager.ScheduleFrequency.DAILY.name)
            val dayOfWeek = prefs.getInt("day_of_week", -1)
            val dayOfMonth = prefs.getInt("day_of_month", -1)

            val frequency = SyncServiceManager.ScheduleFrequency.valueOf(frequencyName ?: SyncServiceManager.ScheduleFrequency.DAILY.name)

            if (userId != null) {
                scheduleSync(hour, minute, userId, frequency, dayOfWeek, dayOfMonth)
            }
        }
    }
}
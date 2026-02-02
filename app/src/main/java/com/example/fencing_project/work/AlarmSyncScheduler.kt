package com.example.fencing_project.work

import android.app.AlarmManager
import android.app.AlarmManager.INTERVAL_DAY
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmSyncScheduler @Inject constructor(
    private val context: Context
) {

    companion object {
        private const val REQUEST_CODE = 1001
        const val ACTION_SYNC = "com.example.fencing_project.ACTION_SYNC"
        private const val TAG = "AlarmSyncScheduler"

        // Ключи для SharedPreferences
        private const val PREFS_NAME = "sync_schedule"
        private const val KEY_FREQUENCY = "frequency"
        private const val KEY_HOUR = "hour"
        private const val KEY_MINUTE = "minute"
        private const val KEY_USER_ID = "user_id"
    }

    enum class Frequency(val displayName: String, val intervalMillis: Long) {
        DAILY("Раз в день", INTERVAL_DAY),
        WEEKLY("Раз в неделю", INTERVAL_DAY * 7),
        MONTHLY("Раз в месяц", INTERVAL_DAY * 30),
        DISABLED("Отключено", 0)
    }

    fun scheduleSync(userId: String, frequency: Frequency, hour: Int, minute: Int) {
        Log.d(TAG, "=== НАЧАЛО scheduleSync ===")
        Log.d(TAG, "Параметры: userId=$userId, frequency=$frequency, time=$hour:$minute")

        if (frequency == Frequency.DISABLED) {
            Log.d(TAG, "Частота DISABLED, отменяем расписание")
            cancelSchedule()
            clearPreferences()
            Log.d(TAG, "=== КОНЕЦ scheduleSync (DISABLED) ===")
            return
        }

        // Сохраняем настройки
        savePreferences(userId, frequency, hour, minute)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        Log.d(TAG, "AlarmManager получен: $alarmManager")

        val intent = Intent(context, SyncReceiver::class.java).apply {
            action = ACTION_SYNC
            putExtra("user_id", userId)
            Log.d(TAG, "Intent создан: action=$action, userId=$userId")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        Log.d(TAG, "PendingIntent создан: $pendingIntent")

        // Отменяем предыдущий алерт
        Log.d(TAG, "Отменяем предыдущий алерт")
        alarmManager.cancel(pendingIntent)

        // Устанавливаем время первого запуска
        val firstTriggerTime = calculateFirstTriggerTime(hour, minute)

        // Для Android 12+ проверяем разрешение
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val canSchedule = alarmManager.canScheduleExactAlarms()
            Log.d(TAG, "canScheduleExactAlarms: $canSchedule")
            if (!canSchedule) {
                Log.e(TAG, "НЕТ разрешения на точные алерты! Alarm может не сработать")
                // Можно добавить запрос разрешения здесь
            }
        }

        // Устанавливаем алерт
        Log.d(TAG, "Устанавливаем алерт на: ${Date(firstTriggerTime)}")

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    firstTriggerTime,
                    pendingIntent
                )
                Log.d(TAG, "setExactAndAllowWhileIdle успешно")
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    firstTriggerTime,
                    pendingIntent
                )
                Log.d(TAG, "setExact успешно")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException при установке алерта: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при установке алерта: ${e.message}")
        }

        // Устанавливаем повторение
        if (frequency != Frequency.DISABLED && frequency.intervalMillis > 0) {
            Log.d(TAG, "Устанавливаем повторение: каждые ${frequency.intervalMillis}ms")
            try {
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    firstTriggerTime,
                    frequency.intervalMillis,
                    pendingIntent
                )
                Log.d(TAG, "Повторение установлено")
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка установки повторения: ${e.message}")
            }
        }

        // Проверяем, установился ли алерт
        checkNextAlarm(alarmManager)

        Log.d(TAG, "=== КОНЕЦ scheduleSync ===")
    }

    private fun checkNextAlarm(alarmManager: AlarmManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val nextAlarm = alarmManager.nextAlarmClock
            if (nextAlarm != null) {
                Log.d(TAG, "Следующий алерт в системе: ${Date(nextAlarm.triggerTime)}")
            } else {
                Log.d(TAG, "Следующий алерт в системе: НЕТ (null)")
            }
        }
    }

    fun cancelSchedule() {
        Log.d(TAG, "Отменяем расписание синхронизации")
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, SyncReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        clearPreferences()
    }

    fun getCurrentSchedule(): ScheduleInfo? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val userId = prefs.getString(KEY_USER_ID, null)
        val frequencyOrdinal = prefs.getInt(KEY_FREQUENCY, -1)
        val hour = prefs.getInt(KEY_HOUR, 2)
        val minute = prefs.getInt(KEY_MINUTE, 0)

        if (userId == null || frequencyOrdinal == -1) {
            return null
        }

        val frequency = Frequency.values().getOrNull(frequencyOrdinal) ?: return null

        return ScheduleInfo(userId, frequency, hour, minute)
    }

    fun hasActiveSchedule(): Boolean {
        return getCurrentSchedule() != null
    }

    data class ScheduleInfo(
        val userId: String,
        val frequency: Frequency,
        val hour: Int,
        val minute: Int
    )

    private fun calculateFirstTriggerTime(hour: Int, minute: Int): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            Log.d(TAG, "Текущее время: ${this.time}")
            Log.d(TAG, "Целевое время: hour=$hour, minute=$minute")

            // Если указанное время уже прошло сегодня, планируем на завтра
            if (timeInMillis <= System.currentTimeMillis()) {
                Log.d(TAG, "Время уже прошло, планируем на завтра")
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        Log.d(TAG, "Рассчитанное время срабатывания: ${calendar.time}")
        return calendar.timeInMillis
    }

    private fun savePreferences(userId: String, frequency: Frequency, hour: Int, minute: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_USER_ID, userId)
            .putInt(KEY_FREQUENCY, frequency.ordinal)
            .putInt(KEY_HOUR, hour)
            .putInt(KEY_MINUTE, minute)
            .apply()
        Log.d(TAG, "Настройки сохранены: $userId, $frequency, $hour:$minute")
    }

    private fun clearPreferences() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        Log.d(TAG, "Настройки очищены")
    }
}
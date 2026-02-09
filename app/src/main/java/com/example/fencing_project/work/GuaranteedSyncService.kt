package com.example.fencing_project.work

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.*
import androidx.core.app.NotificationCompat
import com.example.fencing_project.R
import com.example.fencing_project.data.repository.SyncRepository
import com.example.fencing_project.utils.NetworkUtils
import com.example.fencing_project.utils.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@AndroidEntryPoint
class GuaranteedSyncService : Service() {

    @Inject
    lateinit var syncRepository: SyncRepository

    @Inject
    lateinit var networkUtils: NetworkUtils

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private var wakeLock: PowerManager.WakeLock? = null

    private val notificationIdGenerator = AtomicInteger(2000)

    companion object {
        const val CHANNEL_ID = "guaranteed_sync_channel"
        const val FOREGROUND_NOTIFICATION_ID = 103
        const val EXTRA_USER_ID = "user_id"
        const val EXTRA_IS_SCHEDULED = "is_scheduled"

        fun startService(context: Context, userId: String, isScheduled: Boolean = true) {
            val intent = Intent(context, GuaranteedSyncService::class.java).apply {
                putExtra(EXTRA_USER_ID, userId)
                putExtra(EXTRA_IS_SCHEDULED, isScheduled)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val userId = intent?.getStringExtra(EXTRA_USER_ID) ?: return START_NOT_STICKY
        val isScheduled = intent.getBooleanExtra(EXTRA_IS_SCHEDULED, true)

        val notification = createNotification(
            if (isScheduled) "Выполняется автоматическая синхронизация..."
            else "Выполняется синхронизация..."
        )
        startForeground(FOREGROUND_NOTIFICATION_ID, notification)

        scope.launch {
            try {

                if (!networkUtils.isInternetAvailable()) {
                    showErrorNotification("Нет подключения к интернету")
                    stopSelf()
                    return@launch
                }

                syncRepository.syncWithFirebase(userId)

                showSuccessNotification(
                    if (isScheduled) "Автоматическая синхронизация завершена"
                    else "Данные успешно синхронизированы"
                )

            } catch (e: Exception) {
                showErrorNotification("Ошибка синхронизации: ${e.message ?: "неизвестная ошибка"}")
            } finally {
                stopSelf()
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
        releaseWakeLock()
    }

    override fun onBind(intent: Intent?): IBinder? = null


    private suspend fun showSuccessNotification(message: String) {
        withContext(Dispatchers.Main) {
            val uniqueId = notificationIdGenerator.incrementAndGet()
            createAndShowNotification(
                title = "Синхронизация данных",
                message = message,
                notificationId = uniqueId,
                isError = false
            )
        }
    }

    private suspend fun showErrorNotification(message: String) {
        withContext(Dispatchers.Main) {
            val uniqueId = notificationIdGenerator.incrementAndGet()
            createAndShowNotification(
                title = "Ошибка синхронизации",
                message = message,
                notificationId = uniqueId,
                isError = true
            )
        }
    }

    private suspend fun showNotification(message: String) {
        withContext(Dispatchers.Main) {
            val uniqueId = notificationIdGenerator.incrementAndGet()
            createAndShowNotification(
                title = "Синхронизация",
                message = message,
                notificationId = uniqueId,
                isError = false
            )
        }
    }

    private fun createAndShowNotification(
        title: String,
        message: String,
        notificationId: Int,
        isError: Boolean = false
    ) {
        val resultChannelId = "sync_result_channel"
        createResultNotificationChannel()

        val builder = NotificationCompat.Builder(this, resultChannelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.sync)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)

        if (isError) {
            builder.setCategory(NotificationCompat.CATEGORY_ERROR)
        }

        val notification = builder.build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(notificationId, notification)
    }

    private fun createResultNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "sync_result_channel",
                "Результаты синхронизации",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Уведомления о результатах синхронизации"
                setShowBadge(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(100, 200, 300, 400)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            if (notificationManager.getNotificationChannel("sync_result_channel") == null) {
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "fencing_app:guaranteed_sync_wakelock"
            )
            wakeLock?.acquire(10 * 60 * 1000L)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Гарантированная синхронизация",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Выполнение синхронизации в фоновом режиме"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    private fun createNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Автосинхронизация")
            .setContentText(text)
            .setSmallIcon(R.drawable.sync)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(false)
            .build()
    }
}
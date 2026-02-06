package com.example.fencing_project.work

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
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
import javax.inject.Inject

@AndroidEntryPoint
class BackgroundSyncService : Service() {

    @Inject
    lateinit var syncRepository: SyncRepository

    @Inject
    lateinit var networkUtils: NetworkUtils

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    companion object {
        const val CHANNEL_ID = "sync_service_channel"
        const val NOTIFICATION_ID = 101
        const val EXTRA_USER_ID = "user_id"
        const val EXTRA_SHOW_NOTIFICATION = "show_notification"
        const val SYNC_TIMEOUT_SECONDS = 60L

        fun startService(context: Context, userId: String, showNotification: Boolean = true) {
            val intent = Intent(context, BackgroundSyncService::class.java).apply {
                putExtra(EXTRA_USER_ID, userId)
                putExtra(EXTRA_SHOW_NOTIFICATION, showNotification)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)

            } else {
                context.startService(intent)

            }
        }

        fun stopService(context: Context) {
            context.stopService(Intent(context, BackgroundSyncService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val userId = intent?.getStringExtra(EXTRA_USER_ID) ?: return START_NOT_STICKY
        val showNotification = intent.getBooleanExtra(EXTRA_SHOW_NOTIFICATION, true)
        val notification = createNotification("Начинаем синхронизацию...", 0, false)
        startForeground(NOTIFICATION_ID, notification)

        scope.launch {
            try {
                if (!networkUtils.isInternetAvailable()) {
                    updateNotification("Нет подключения к интернету", 0, false)
                    if (showNotification) {
                        NotificationHelper.showSyncErrorNotification(
                            this@BackgroundSyncService,
                            "Синхронизация не удалась: нет интернета"
                        )
                    }
                    stopSelf()
                    return@launch
                }
                updateNotification("Подготовка данных...", 10, true)
                updateNotification("Синхронизация данных...", 30, true)
                syncRepository.syncWithFirebase(userId)
                updateNotification("Синхронизация завершена", 100, true)
                if (showNotification) {
                    NotificationHelper.showSyncSuccessNotification(
                        this@BackgroundSyncService,
                        "Данные успешно синхронизированы"
                    )
                }

                stopSelf()
            } catch (e: Exception) {
                updateNotification("Ошибка: ${e.message?.take(50) ?: "неизвестная"}", 0, false)
                if (showNotification) {
                    NotificationHelper.showSyncErrorNotification(
                        this@BackgroundSyncService,
                        e.message ?: "Ошибка синхронизации"
                    )
                }
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Синхронизация данных",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Прогресс синхронизации данных"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(
        text: String,
        progress: Int,
        indeterminate: Boolean = false
    ): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Синхронизация")
            .setContentText(text)
            .setSmallIcon(R.drawable.sync)
            .setProgress(100, progress, indeterminate)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(false)
            .build()
    }

    private fun updateNotification(
        text: String,
        progress: Int,
        showProgress: Boolean = true
    ) {
        val notification = createNotification(text, progress, !showProgress)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
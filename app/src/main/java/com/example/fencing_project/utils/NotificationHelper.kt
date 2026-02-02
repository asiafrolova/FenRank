package com.example.fencing_project.utils


import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.fencing_project.R

object NotificationHelper {

    private const val CHANNEL_ID = "sync_complete_channel"
    private const val NOTIFICATION_ID = 103
    private const val CHANNEL_NAME = "Синхронизация"
    private const val CHANNEL_DESCRIPTION = "Уведомления о синхронизации данных"
    private const val NO_INTERNET_NOTIFICATION_ID = 104

    fun showNoInternetNotification(context: Context) {
        createNotificationChannel(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Синхронизация")
            .setContentText("Нет подключения к интернету")
            .setSmallIcon(R.drawable.sync)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .build()

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(NO_INTERNET_NOTIFICATION_ID, notification)
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESCRIPTION
                setShowBadge(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(100, 200, 300, 400)
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showSyncSuccessNotification(context: Context, message: String = "Синхронизация завершена успешно") {
        createNotificationChannel(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Синхронизация данных")
            .setContentText(message)
            .setSmallIcon(R.drawable.sync) // Используйте вашу иконку
            .setAutoCancel(true) // Уведомление можно смахнуть
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun showSyncErrorNotification(context: Context, errorMessage: String) {
        createNotificationChannel(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Ошибка синхронизации")
            .setContentText(errorMessage.take(100)) // Ограничим длину текста
            .setSmallIcon(R.drawable.sync) // Или создайте иконку ошибки
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .build()

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID + 1, notification) // Другой ID для ошибок
    }

    fun cancelAllNotifications(context: Context) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.cancelAll()
    }
}
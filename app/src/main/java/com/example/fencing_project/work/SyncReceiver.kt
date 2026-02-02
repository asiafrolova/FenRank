package com.example.fencing_project.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.fencing_project.work.BackgroundSyncService


class SyncReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SyncReceiver"
    }

//    override fun onReceive(context: Context, intent: Intent) {
//        if (intent.action == AlarmSyncScheduler.ACTION_SYNC) {
//            val userId = intent.getStringExtra("user_id")
//            if (userId != null) {
//                // Запускаем сервис синхронизации
//                BackgroundSyncService.startService(context, userId)
//            }
//        }
//    }
override fun onReceive(context: Context, intent: Intent) {
    Log.d(TAG, "Получено событие: ${intent.action}")

    when (intent.action) {
        AlarmSyncScheduler.ACTION_SYNC -> {
            val userId = intent.getStringExtra("user_id")
            if (userId != null) {
                Log.d(TAG, "Запускаем синхронизацию для пользователя: $userId")
                // Запускаем сервис с флагом показа уведомления
                BackgroundSyncService.startService(context, userId, showNotification = true)
            }
        }

        Intent.ACTION_BOOT_COMPLETED -> {
            val userId = intent.getStringExtra("user_id")
            if (userId != null) {
                // Запускаем сервис синхронизации
                BackgroundSyncService.startService(context, userId)
            }
        }
    }
}
}
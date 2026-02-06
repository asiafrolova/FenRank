package com.example.fencing_project.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent




class SyncReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SyncReceiver"
        const val ACTION_SYNC = "com.example.fencing_project.ACTION_SYNC"
    }


    override fun onReceive(context: Context, intent: Intent) {


        when (intent.action) {
            ACTION_SYNC -> {
                val userId = intent.getStringExtra("user_id")
                if (userId != null) {



                    val syncManager = SyncServiceManager(context)
                    syncManager.startBackgroundSync(userId, showNotification = true)
                }
            }

            Intent.ACTION_BOOT_COMPLETED -> {
                restoreSchedule(context)
            }
        }
    }

    private fun restoreSchedule(context: Context) {
        val syncManager = SyncServiceManager(context)
        val schedule = syncManager.getCurrentSchedule()
        if (schedule != null) {
            syncManager.schedulePeriodicSync(
                schedule.userId,
                schedule.frequency,
                schedule.hour,
                schedule.minute
            )

        }
    }

}
package com.example.fencing_project.work

import android.content.Context

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncServiceManager @Inject constructor(
    private val context: Context
) {

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
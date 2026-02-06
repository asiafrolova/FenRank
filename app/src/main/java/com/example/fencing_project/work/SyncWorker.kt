package com.example.fencing_project.work

import com.example.fencing_project.utils.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.fencing_project.data.repository.SyncRepository
import com.example.fencing_project.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val syncRepository: SyncRepository,
    private val networkUtils: NetworkUtils
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_USER_ID = "user_id"
        const val KEY_SHOW_NOTIFICATION = "show_notification"
        const val KEY_TYPE = "type"
        const val TAG = "SyncWorker"
    }

    override suspend fun doWork(): Result {
        return try {
            val userId = inputData.getString(KEY_USER_ID) ?: return Result.failure()
            val showNotification = inputData.getBoolean(KEY_SHOW_NOTIFICATION, true)
            val type = inputData.getString(KEY_TYPE) ?: "sync"


            if (!networkUtils.isInternetAvailable()) {
                return Result.retry()
            }

            when (type) {
                "sync" -> {

                    syncRepository.syncWithFirebase(userId)
                    if (showNotification) {
                        showSyncSuccessNotification()
                    }
                }
                "restore" -> {

                    syncRepository.updateLocalData(userId)
                    if (showNotification) {
                        showRestoreSuccessNotification()
                    }
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.failure(workDataOf("error" to e.message))
        }
    }

    private suspend fun showSyncSuccessNotification() {
        withContext(Dispatchers.Main) {
            NotificationHelper.showSyncSuccessNotification(
                context,
                "Данные успешно синхронизированы"
            )
        }
    }

    private suspend fun showRestoreSuccessNotification() {
        withContext(Dispatchers.Main) {
            NotificationHelper.showSyncSuccessNotification(
                context,
                "Данные успешно восстановлены"
            )
        }
    }
}
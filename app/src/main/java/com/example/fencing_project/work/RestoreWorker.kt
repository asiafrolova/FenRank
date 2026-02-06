package com.example.fencing_project.work


import android.content.Context
import androidx.hilt.work.HiltWorker

import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.fencing_project.data.repository.SyncRepository
import com.example.fencing_project.utils.NetworkUtils
import com.example.fencing_project.utils.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class RestoreWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val syncRepository: SyncRepository,
    private val networkUtils: NetworkUtils
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_USER_ID = "user_id"
        const val KEY_SHOW_NOTIFICATION = "show_notification"
    }

    override suspend fun doWork(): Result {
        return try {
            val userId = inputData.getString(KEY_USER_ID) ?: return Result.failure()
            val showNotification = inputData.getBoolean(KEY_SHOW_NOTIFICATION, true)

            if (!networkUtils.isInternetAvailable()) {
                return Result.retry()
            }

            val sync = syncRepository.getSyncByUserId(userId)
            if (sync == null) {
                return Result.failure(workDataOf("error" to "No backup found"))
            }

            syncRepository.updateLocalData(userId)

            if (showNotification) {
                withContext(Dispatchers.Main) {
                    NotificationHelper.showSyncSuccessNotification(
                        context,
                        "Данные успешно восстановлены"
                    )
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.failure(workDataOf("error" to e.message))
        }
    }
}
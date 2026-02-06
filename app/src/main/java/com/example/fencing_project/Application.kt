package com.example.fencing_project

import android.app.Application
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.fencing_project.utils.NotificationHelper
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp
import jakarta.inject.Inject

@HiltAndroidApp
class FencingApp : Application(), Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)

        NotificationHelper.createNotificationChannel(this)
    }



}
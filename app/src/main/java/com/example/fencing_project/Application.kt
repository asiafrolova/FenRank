package com.example.fencing_project

import android.app.Application
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FencingApp : Application(){
    override fun onCreate() {
        super.onCreate()
        // Инициализация Firebase
        FirebaseApp.initializeApp(this)
    }
}
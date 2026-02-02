package com.example.fencing_project.utils

import android.content.Context
import android.content.SharedPreferences

class SharedPrefsManager (context: Context){
    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USER_ID = "user_id" // Firebase UID
        private const val KEY_LOCAL_USER_ID = "local_user_id"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"

    }

    // Исправьте метод saveLoginState
    fun saveLoginState(userId: String) {
        with(prefs.edit()) {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_ID, userId) // Сохраняем UID, а не email

            apply()
        }
    }
    fun saveLocalLogin(){
        with(prefs.edit()) {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putBoolean(KEY_LOCAL_USER_ID, true) // Сохраняем UID, а не email

            apply()
        }
    }


    // Получить userId (Firebase UID)
    fun getUserId(): String?{
        if (isOffline()){
            return "offline"
        }
        return prefs.getString(KEY_USER_ID, null)
    }


    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    fun isOffline(): Boolean = prefs.getBoolean(KEY_LOCAL_USER_ID,false)

    fun logout() {
        prefs.edit().clear().apply()
    }
}
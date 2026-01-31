package com.example.fencing_project.utils

import android.content.Context
import android.content.SharedPreferences

class SharedPrefsManager (context: Context){
    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USER_ID = "user_id" // Firebase UID
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_PASSWORD = "user_password"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"

    }

    // Исправьте метод saveLoginState
    fun saveLoginState(userId: String, email: String, password: String) {
        with(prefs.edit()) {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_ID, userId) // Сохраняем UID, а не email
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_PASSWORD, password)
            apply()
        }
    }


    // Получить userId (Firebase UID)
    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)

    // Получить email
    fun getUserEmail(): String? = prefs.getString(KEY_USER_EMAIL, null)

    // Получить password
    fun getUserPassword(): String? = prefs.getString(KEY_USER_PASSWORD, null)
    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    fun getLogin(): String? = prefs.getString(KEY_USER_EMAIL, null)




    fun logout() {
        prefs.edit().clear().apply()
    }
}
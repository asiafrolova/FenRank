package com.example.fencing_project.utils

import android.content.Context
import android.content.SharedPreferences

class SharedPrefsManager (context: Context){
    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_LOGIN = "login"
        private const val KEY_PASSWORD = "password"
    }
    fun saveLoginState(login: String? = null, password: String? = null) {
        with(prefs.edit()) {
            if (login != null) {
                putString(KEY_LOGIN, login)
            }
            if (password != null) {
                putString(KEY_PASSWORD, password)
            }
            putBoolean(KEY_IS_LOGGED_IN, login!=null && password!=null)
            apply()
        }
    }
    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    fun getLogin(): String? = prefs.getString(KEY_LOGIN, null)

    fun getPassword(): String? = prefs.getString(KEY_PASSWORD, null)

    fun logout() {
        prefs.edit().clear().apply()
    }
}
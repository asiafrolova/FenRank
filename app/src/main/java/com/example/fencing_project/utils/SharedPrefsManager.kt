package com.example.fencing_project.utils

import android.content.Context
import android.content.SharedPreferences

class SharedPrefsManager (context: Context){
    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_LOCAL_USER_ID = "local_user_id"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val FIRST_HOME = "first_home"
        private const val KEY_LANGUAGE = "selected_language"
        private const val KEY_REGISTR = "registr"

    }

    fun isRegistr():Boolean{
        return prefs.getBoolean(KEY_REGISTR,false)
    }
    fun setRegistr(registr:Boolean){
        with(prefs.edit()) {
            putBoolean(KEY_REGISTR,registr)
            apply()
        }
    }
    fun setLanguage(langCode:String){
        with(prefs.edit()) {
            putString(KEY_LANGUAGE,langCode)
            apply()
        }
    }
    fun getLanguage():String{
        return prefs.getString(KEY_LANGUAGE, "ru")?:"ru"
    }
    fun saveLoginState(userId: String) {
        with(prefs.edit()) {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_ID, userId)

            apply()
        }
    }
    fun saveLocalLogin(){
        with(prefs.edit()) {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putBoolean(KEY_LOCAL_USER_ID, true)

            apply()
        }
    }

    fun isFirst():Boolean{
        if(prefs.getBoolean(FIRST_HOME,true)){
            with(prefs.edit()){
                putBoolean(FIRST_HOME,false)
                apply()
            }
            return true
        }
        return false
    }

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
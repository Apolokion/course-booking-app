package biz.pock.coursebookingapp.utils

import android.content.Context
import biz.pock.coursebookingapp.shared.KEY_APP_LANGUAGE
import biz.pock.coursebookingapp.shared.KEY_DARK_MODE
import biz.pock.coursebookingapp.shared.KEY_PENDING_THEME_CHANGE
import biz.pock.coursebookingapp.shared.SHARED_PREFS

object ThemeLanguageUtils {

    fun saveLanguage(language: String, context: Context) {
        val sharedPreferences = context.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE)
        sharedPreferences.edit().putString(KEY_APP_LANGUAGE, language).apply()
    }

    fun getSavedLanguage(context: Context): String {
        val sharedPreferences = context.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE)
        return sharedPreferences.getString(KEY_APP_LANGUAGE, "de") ?: "de"
    }

    fun saveDarkModePreference(isDarkMode: Boolean, context: Context) {
        val sharedPreferences = context.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean(KEY_DARK_MODE, isDarkMode).apply()
    }

    fun isDarkModeEnabled(context: Context): Boolean {
        val sharedPreferences = context.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean(KEY_DARK_MODE, false)
    }

    fun setPendingThemeChange(context: Context, isPending: Boolean) {
        val sharedPreferences = context.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean(KEY_PENDING_THEME_CHANGE, isPending).apply()
    }

    // TODO: Für späteren Gebrauch bei interagierenden UI events
    /*
    fun hasPendingThemeChange(context: Context): Boolean {
        val sharedPreferences = context.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean(KEY_PENDING_THEME_CHANGE, false)
    }
     */
}
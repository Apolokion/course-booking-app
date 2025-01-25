package biz.pock.coursebookingapp.data.auth

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext context: Context
) {
    companion object {
        private const val PREF_NAME = "AuthPrefs"
        private const val KEY_TOKEN = "token"
        private const val KEY_ROLE = "role"
        private const val KEY_USER_ID = "user_id"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    fun saveRole(role: String) {
        prefs.edit().putString(KEY_ROLE, role).apply()
    }

    fun getRole(): String? {
        return prefs.getString(KEY_ROLE, null)
    }

    fun saveUserId(userId: String) {
        prefs.edit().putString(KEY_USER_ID, userId).apply()
    }

    fun getUserId(): String? {
        return prefs.getString(KEY_USER_ID, null)
    }

    fun clearAll() {
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_ROLE)
            .remove(KEY_USER_ID)
            .apply()
    }

    fun hasToken(): Boolean {
        return getToken() != null
    }
}
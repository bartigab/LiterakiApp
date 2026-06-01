package com.example.literakiapp.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SavedSession(
    val userId: Int,
    val username: String,
    val token: String
)

class SessionStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private val _session = MutableStateFlow(loadSession())
    val session: StateFlow<SavedSession?> = _session.asStateFlow()

    fun getSession(): SavedSession? = _session.value

    fun hasActiveSession(): Boolean = !getSession()?.token.isNullOrBlank()

    fun saveSession(session: SavedSession) {
        preferences.edit()
            .putInt(KEY_USER_ID, session.userId)
            .putString(KEY_USERNAME, session.username)
            .putString(KEY_TOKEN, session.token)
            .apply()
        _session.value = session
    }

    fun clearSession() {
        preferences.edit().clear().apply()
        _session.value = null
    }

    private fun loadSession(): SavedSession? {
        val token = preferences.getString(KEY_TOKEN, null)?.trim().orEmpty()
        val username = preferences.getString(KEY_USERNAME, null)?.trim().orEmpty()
        val userId = preferences.getInt(KEY_USER_ID, -1)

        if (token.isBlank() || username.isBlank() || userId <= 0) {
            return null
        }

        return SavedSession(
            userId = userId,
            username = username,
            token = token
        )
    }

    private companion object {
        const val PREFERENCES_NAME = "literaki_session"
        const val KEY_USER_ID = "key_user_id"
        const val KEY_USERNAME = "key_username"
        const val KEY_TOKEN = "key_token"
    }
}


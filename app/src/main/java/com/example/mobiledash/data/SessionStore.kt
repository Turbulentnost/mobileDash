package com.example.mobiledash.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("mobile_dash_session", Context.MODE_PRIVATE)

    suspend fun readSession(): LoginSession? = withContext(Dispatchers.IO) {
        val token = prefs.getString(KEY_TOKEN, null)?.takeIf { it.isNotBlank() } ?: return@withContext null
        val nickname = prefs.getString(KEY_NICKNAME, null)?.takeIf { it.isNotBlank() } ?: return@withContext null
        LoginSession(
            token = token,
            user = DashboardUser(
                id = prefs.getInt(KEY_USER_ID, -1).takeIf { it >= 0 },
                nickname = nickname,
                role = prefs.getString(KEY_ROLE, "").orEmpty(),
                department = prefs.getString(KEY_DEPARTMENT, "").orEmpty(),
            ),
        )
    }

    suspend fun saveSession(session: LoginSession) = withContext(Dispatchers.IO) {
        prefs.edit()
            .putString(KEY_TOKEN, session.token)
            .putInt(KEY_USER_ID, session.user.id ?: -1)
            .putString(KEY_NICKNAME, session.user.nickname)
            .putString(KEY_ROLE, session.user.role)
            .putString(KEY_DEPARTMENT, session.user.department)
            .apply()
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val KEY_TOKEN = "token"
        const val KEY_USER_ID = "user_id"
        const val KEY_NICKNAME = "nickname"
        const val KEY_ROLE = "role"
        const val KEY_DEPARTMENT = "department"
    }
}

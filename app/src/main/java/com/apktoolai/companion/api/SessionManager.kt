package com.apktoolai.companion.api

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("apk_tool_session", Context.MODE_PRIVATE)

    companion object {
        const val DEFAULT_SERVER_URL = "https://apk.zoomnearby.com/"

        @Volatile
        private var instance: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return instance ?: synchronized(this) {
                instance ?: SessionManager(context.applicationContext).also { instance = it }
            }
        }
    }

    var serverUrl: String
        get() {
            val url = prefs.getString("server_url", DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
            return if (url.endsWith("/")) url else "$url/"
        }
        set(value) {
            val normalized = if (value.endsWith("/")) value else "$value/"
            prefs.edit().putString("server_url", normalized).apply()
        }

    var pairingToken: String?
        get() = prefs.getString("pairing_token", null)
        set(value) = prefs.edit().putString("pairing_token", value).apply()

    var pairedProjectName: String?
        get() = prefs.getString("paired_project_name", null)
        set(value) = prefs.edit().putString("paired_project_name", value).apply()

    var projectName: String?
        get() = pairedProjectName
        set(value) { pairedProjectName = value }

    val isPaired: Boolean get() = !pairingToken.isNullOrEmpty()
    val isLoggedIn: Boolean get() = currentUser != null

    var currentUser: User?
        get() {
            val raw = prefs.getString("current_user_json", null) ?: return null
            return try {
                val j = JSONObject(raw)
                User(
                    id = j.optInt("id", 0),
                    username = j.optString("username", ""),
                    email = j.optString("email", ""),
                    userType = j.optString("user_type", "user"),
                    emailVerified = j.optInt("email_verified", 0),
                    decompileLimit = j.optInt("decompile_limit", 1),
                    decompileUsage = j.optInt("decompile_usage", 0),
                    compileLimit = j.optInt("compile_limit", 1),
                    compileUsage = j.optInt("compile_usage", 0),
                    generateKeyLimit = j.optInt("generate_key_limit", 1),
                    generateKeyUsage = j.optInt("generate_key_usage", 0),
                    signApkLimit = j.optInt("sign_apk_limit", 1),
                    signApkUsage = j.optInt("sign_apk_usage", 0)
                )
            } catch (e: Exception) {
                null
            }
        }
        set(value) {
            if (value == null) {
                prefs.edit().remove("current_user_json").apply()
            } else {
                val j = JSONObject().apply {
                    put("id", value.id)
                    put("username", value.username)
                    put("email", value.email)
                    put("user_type", value.userType)
                    put("email_verified", value.emailVerified)
                    put("decompile_limit", value.decompileLimit)
                    put("decompile_usage", value.decompileUsage)
                    put("compile_limit", value.compileLimit)
                    put("compile_usage", value.compileUsage)
                    put("generate_key_limit", value.generateKeyLimit)
                    put("generate_key_usage", value.generateKeyUsage)
                    put("sign_apk_limit", value.signApkLimit)
                    put("sign_apk_usage", value.signApkUsage)
                }
                prefs.edit().putString("current_user_json", j.toString()).apply()
            }
        }

    var currentProjectId: String?
        get() = prefs.getString("current_project_id", null)
        set(value) = prefs.edit().putString("current_project_id", value).apply()

    var lastBuildVersion: Long
        get() = prefs.getLong("last_build_version", -1)
        set(value) = prefs.edit().putLong("last_build_version", value).apply()

    var pendingBuildVersion: Long
        get() = prefs.getLong("pending_build_version", -1)
        set(value) = prefs.edit().putLong("pending_build_version", value).apply()

    fun isAuthenticated(): Boolean = currentUser != null

    fun savePairing(url: String, token: String, name: String) {
        prefs.edit()
            .putString("server_url", url)
            .putString("pairing_token", token)
            .putString("paired_project_name", name)
            .apply()
    }

    fun saveUser(user: User) {
        currentUser = user
    }

    fun logout() {
        prefs.edit()
            .remove("current_user_json")
            .remove("current_project_id")
            .remove("pairing_token")
            .remove("paired_project_name")
            .apply()
    }

    fun clearAuth() {
        logout()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}

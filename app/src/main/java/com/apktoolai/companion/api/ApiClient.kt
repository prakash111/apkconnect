package com.apktoolai.companion.api

import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.concurrent.Executors

class ApiClient(private val session: SessionManager) {

    private val executor = Executors.newFixedThreadPool(4)
    private val mainHandler = Handler(Looper.getMainLooper())

    companion object {
        init {
            // Enable global cookie management for seamless PHP session handling (PHPSESSID)
            if (CookieHandler.getDefault() == null) {
                CookieHandler.setDefault(CookieManager(null, CookiePolicy.ACCEPT_ALL))
            }
        }
    }

    interface ApiCallback<T> {
        fun onSuccess(result: T)
        fun onError(errorMessage: String)
    }

    interface ProgressCallback {
        fun onProgress(percentage: Int, message: String)
    }

    private fun getBaseUrl(): String {
        return session.serverUrl
    }

    private fun getIndexPath(): String {
        val base = getBaseUrl()
        return if (base.endsWith("index.php") || base.endsWith("index.php/")) {
            base
        } else {
            "${base}index.php"
        }
    }

    // -------------------------------------------------------------
    // Core HTTP Methods
    // -------------------------------------------------------------

    fun executePost(
        action: String,
        params: Map<String, String> = emptyMap(),
        callback: ApiCallback<JSONObject>
    ) {
        executor.execute {
            try {
                val url = URL(getIndexPath())
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 15000
                conn.readTimeout = 30000
                conn.doInput = true
                conn.doOutput = true
                conn.useCaches = false
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                conn.setRequestProperty("X-Requested-With", "XMLHttpRequest")

                val merged = HashMap(params)
                merged["action"] = action

                val postData = StringBuilder()
                for ((key, value) in merged) {
                    if (postData.isNotEmpty()) postData.append("&")
                    postData.append(URLEncoder.encode(key, "UTF-8"))
                        .append("=")
                        .append(URLEncoder.encode(value, "UTF-8"))
                }

                conn.outputStream.use { os ->
                    os.write(postData.toString().toByteArray(Charsets.UTF_8))
                    os.flush()
                }

                val responseCode = conn.responseCode
                val stream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
                val responseText = stream?.bufferedReader()?.use { it.readText() } ?: "{}"
                conn.disconnect()

                val json = try {
                    JSONObject(responseText)
                } catch (e: Exception) {
                    JSONObject().apply {
                        put("status", "error")
                        put("message", "Invalid server response ($responseCode): $responseText")
                    }
                }

                mainHandler.post {
                    if (json.optString("status") == "success") {
                        callback.onSuccess(json)
                    } else {
                        val msg = json.optString("message", "Request failed: ${json.optString("status")}")
                        callback.onError(msg)
                    }
                }
            } catch (e: Exception) {
                mainHandler.post {
                    callback.onError(e.message ?: "Network error.")
                }
            }
        }
    }

    fun executeMultipart(
        action: String,
        fileParams: Map<String, File>,
        textParams: Map<String, String> = emptyMap(),
        progressCallback: ProgressCallback? = null,
        callback: ApiCallback<JSONObject>
    ) {
        executor.execute {
            try {
                val boundary = "===APKTOOLAI_BOUNDARY_" + System.currentTimeMillis() + "==="
                val lineEnd = "\r\n"
                val twoHyphens = "--"

                val url = URL(getIndexPath())
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 30000
                conn.readTimeout = 120000
                conn.doInput = true
                conn.doOutput = true
                conn.useCaches = false
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                conn.setRequestProperty("X-Requested-With", "XMLHttpRequest")

                val outputStream = conn.outputStream
                val writer = PrintWriter(OutputStreamWriter(outputStream, "UTF-8"), true)

                val mergedText = HashMap(textParams)
                mergedText["action"] = action

                // Write text parameters
                for ((key, value) in mergedText) {
                    writer.append(twoHyphens).append(boundary).append(lineEnd)
                    writer.append("Content-Disposition: form-data; name=\"").append(key).append("\"").append(lineEnd)
                    writer.append("Content-Type: text/plain; charset=UTF-8").append(lineEnd)
                    writer.append(lineEnd)
                    writer.append(value).append(lineEnd)
                    writer.flush()
                }

                // Write file parameters
                var totalBytes = 0L
                for ((_, file) in fileParams) {
                    totalBytes += file.length()
                }

                var uploadedBytes = 0L
                for ((field, file) in fileParams) {
                    writer.append(twoHyphens).append(boundary).append(lineEnd)
                    writer.append("Content-Disposition: form-data; name=\"").append(field)
                        .append("\"; filename=\"").append(file.name).append("\"").append(lineEnd)
                    writer.append("Content-Type: application/octet-stream").append(lineEnd)
                    writer.append(lineEnd)
                    writer.flush()

                    FileInputStream(file).use { fis ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (fis.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                            uploadedBytes += bytesRead
                            if (totalBytes > 0) {
                                val pct = ((uploadedBytes * 100) / totalBytes).toInt()
                                mainHandler.post {
                                    progressCallback?.onProgress(pct, "Uploading ${file.name} ($pct%)...")
                                }
                            }
                        }
                        outputStream.flush()
                    }
                    writer.append(lineEnd)
                    writer.flush()
                }

                writer.append(twoHyphens).append(boundary).append(twoHyphens).append(lineEnd)
                writer.flush()
                writer.close()

                val responseCode = conn.responseCode
                val stream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
                val responseText = stream?.bufferedReader()?.use { it.readText() } ?: "{}"
                conn.disconnect()

                val json = try {
                    JSONObject(responseText)
                } catch (e: Exception) {
                    JSONObject().apply {
                        put("status", "error")
                        put("message", "Invalid server response ($responseCode): $responseText")
                    }
                }

                mainHandler.post {
                    if (json.optString("status") == "success") {
                        callback.onSuccess(json)
                    } else {
                        val msg = json.optString("message", "Request failed: ${json.optString("status")}")
                        callback.onError(msg)
                    }
                }
            } catch (e: Exception) {
                mainHandler.post {
                    callback.onError(e.message ?: "Upload failed.")
                }
            }
        }
    }

    // -------------------------------------------------------------
    // Download APK & Verify Integrity
    // -------------------------------------------------------------

    fun downloadApk(
        downloadUrl: String,
        expectedSha256: String?,
        expectedSize: Long,
        destFile: File,
        progressCallback: ProgressCallback,
        callback: ApiCallback<File>
    ) {
        executor.execute {
            try {
                val url = URL(downloadUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 15000
                conn.readTimeout = 60000
                val responseCode = conn.responseCode
                if (responseCode !in 200..299) {
                    throw IOException("Server responded with HTTP $responseCode")
                }

                val contentLength = conn.contentLength.toLong()
                val totalLength = if (contentLength > 0) contentLength else expectedSize
                var downloadedBytes = 0L

                val digest = MessageDigest.getInstance("SHA-256")
                val isStream = conn.inputStream
                destFile.parentFile?.mkdirs()
                val os = FileOutputStream(destFile)

                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (isStream.read(buffer).also { bytesRead = it } != -1) {
                    os.write(buffer, 0, bytesRead)
                    digest.update(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead
                    if (totalLength > 0) {
                        val pct = ((downloadedBytes * 100) / totalLength).toInt()
                        mainHandler.post {
                            progressCallback.onProgress(pct, "Downloading update ($pct%)...")
                        }
                    }
                }
                os.flush()
                os.close()
                isStream.close()
                conn.disconnect()

                // Validate sha256 checksum if provided
                if (!expectedSha256.isNullOrBlank()) {
                    val calcSha256 = digest.digest().joinToString("") { "%02x".format(it) }
                    if (!calcSha256.equals(expectedSha256.trim(), ignoreCase = true)) {
                        destFile.delete()
                        throw IOException("SHA-256 verification failed! Expected: $expectedSha256, Found: $calcSha256")
                    }
                }

                mainHandler.post {
                    callback.onSuccess(destFile)
                }
            } catch (e: Exception) {
                mainHandler.post {
                    callback.onError(e.message ?: "APK download failed.")
                }
            }
        }
    }

    // -------------------------------------------------------------
    // Authentication & Account Endpoints
    // -------------------------------------------------------------

    fun login(usernameOrEmail: String, password: String, callback: ApiCallback<User>) {
        // The backend's login handler has been observed to key off different
        // field names depending on install/version ("username", "email", or
        // "username_or_email"). Sending all common aliases with the same
        // value guarantees a match regardless of which one the server reads,
        // which fixes the "both fields are required" false-negative even
        // though the user filled in both fields correctly.
        val loginParams = mutableMapOf(
            "username_or_email" to usernameOrEmail,
            "username" to usernameOrEmail,
            "login" to usernameOrEmail,
            "password" to password
        )
        if (usernameOrEmail.contains("@")) {
            loginParams["email"] = usernameOrEmail
        }
        executePost("login", loginParams, object : ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                val u = result.optJSONObject("user")
                if (u != null) {
                    val user = User(
                        id = u.optInt("id", 0),
                        username = u.optString("username", ""),
                        email = u.optString("email", ""),
                        userType = u.optString("user_type", "user"),
                        emailVerified = u.optInt("email_verified", 0),
                        decompileLimit = u.optInt("decompile_limit", 1),
                        decompileUsage = u.optInt("decompile_usage", 0),
                        compileLimit = u.optInt("compile_limit", 1),
                        compileUsage = u.optInt("compile_usage", 0),
                        generateKeyLimit = u.optInt("generate_key_limit", 1),
                        generateKeyUsage = u.optInt("generate_key_usage", 0),
                        signApkLimit = u.optInt("sign_apk_limit", 1),
                        signApkUsage = u.optInt("sign_apk_usage", 0)
                    )
                    session.currentUser = user
                    callback.onSuccess(user)
                } else {
                    callback.onError("User data missing in login response.")
                }
            }

            override fun onError(errorMessage: String) {
                callback.onError(errorMessage)
            }
        })
    }

    fun register(email: String, phone: String, username: String, password: String, callback: ApiCallback<String>) {
        // Backend docs list phone/mobile as part of registration - send both
        // common key spellings ("phone" and "mobile") for compatibility.
        val registerParams = mapOf(
            "email" to email,
            "phone" to phone,
            "mobile" to phone,
            "username" to username,
            "password" to password
        )
        executePost("register", registerParams, object : ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                callback.onSuccess(result.optString("message", "Registration successful."))
            }

            override fun onError(errorMessage: String) {
                callback.onError(errorMessage)
            }
        })
    }

    fun requestPasswordReset(email: String, callback: ApiCallback<String>) {
        executePost("request_password_reset", mapOf("email" to email), object : ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                callback.onSuccess(result.optString("message", "Password reset instructions sent."))
            }

            override fun onError(errorMessage: String) {
                callback.onError(errorMessage)
            }
        })
    }

    fun resetPassword(token: String, newPass: String, callback: ApiCallback<String>) {
        executePost("reset_password", mapOf("token" to token, "new_password" to newPass), object : ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                callback.onSuccess(result.optString("message", "Password reset successfully."))
            }

            override fun onError(errorMessage: String) {
                callback.onError(errorMessage)
            }
        })
    }

    fun verifyEmail(token: String, callback: ApiCallback<String>) {
        executePost("verify_email", mapOf("token" to token), object : ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                callback.onSuccess(result.optString("message", "Email verified successfully."))
            }

            override fun onError(errorMessage: String) {
                callback.onError(errorMessage)
            }
        })
    }

    fun getUserInfo(callback: ApiCallback<User>) {
        executePost("get_user_info", emptyMap(), object : ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                val u = result.optJSONObject("user")
                if (u != null) {
                    val user = User(
                        id = u.optInt("id", 0),
                        username = u.optString("username", ""),
                        email = u.optString("email", ""),
                        userType = u.optString("user_type", "user"),
                        emailVerified = u.optInt("email_verified", 0),
                        decompileLimit = u.optInt("decompile_limit", 1),
                        decompileUsage = u.optInt("decompile_usage", 0),
                        compileLimit = u.optInt("compile_limit", 1),
                        compileUsage = u.optInt("compile_usage", 0),
                        generateKeyLimit = u.optInt("generate_key_limit", 1),
                        generateKeyUsage = u.optInt("generate_key_usage", 0),
                        signApkLimit = u.optInt("sign_apk_limit", 1),
                        signApkUsage = u.optInt("sign_apk_usage", 0)
                    )
                    session.currentUser = user
                    callback.onSuccess(user)
                } else {
                    callback.onError("User profile unavailable.")
                }
            }

            override fun onError(errorMessage: String) {
                callback.onError(errorMessage)
            }
        })
    }

    fun getLimits(callback: ApiCallback<UserLimits>) {
        executePost("get_limits", emptyMap(), object : ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                val lim = result.optJSONObject("limits")
                if (lim != null) {
                    val limits = UserLimits(
                        decompileLimit = lim.optInt("decompile_limit", 1),
                        decompileUsage = lim.optInt("decompile_usage", 0),
                        compileLimit = lim.optInt("compile_limit", 1),
                        compileUsage = lim.optInt("compile_usage", 0),
                        generateKeyLimit = lim.optInt("generate_key_limit", 1),
                        generateKeyUsage = lim.optInt("generate_key_usage", 0),
                        signApkLimit = lim.optInt("sign_apk_limit", 1),
                        signApkUsage = lim.optInt("sign_apk_usage", 0)
                    )
                    callback.onSuccess(limits)
                } else {
                    callback.onError("Usage limits missing.")
                }
            }

            override fun onError(errorMessage: String) {
                callback.onError(errorMessage)
            }
        })
    }

    // -------------------------------------------------------------
    // Projects & Workspaces Endpoints
    // -------------------------------------------------------------

    fun getProjects(callback: ApiCallback<List<ProjectItem>>) {
        executePost("get_projects", emptyMap(), object : ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                val list = ArrayList<ProjectItem>()
                val arr = result.optJSONArray("projects") ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    list.add(
                        ProjectItem(
                            id = o.optInt("id", 0),
                            projectId = o.optString("project_id", ""),
                            projectName = o.optString("project_name", o.optString("apk_name", "project")),
                            sourceApk = o.optString("source_apk", o.optString("apk_name", "")),
                            projectPath = o.optString("project_path", ""),
                            projectRoot = o.optString("project_root", ""),
                            unsignedApk = o.optString("unsigned_apk", ""),
                            signedApk = o.optString("signed_apk", ""),
                            keystorePath = o.optString("keystore_path", ""),
                            keystoreAlias = o.optString("keystore_alias", ""),
                            logoPreviewName = o.optString("logo_preview_name", ""),
                            logoPreviewPath = o.optString("logo_preview_path", ""),
                            logoVersion = o.optLong("logo_version", 0),
                            crashReportToken = o.optString("crash_report_token", ""),
                            createdAt = o.optString("created_at", ""),
                            updatedAt = o.optString("updated_at", ""),
                            apkName = o.optString("apk_name", ""),
                            apkSize = o.optLong("apk_size", 0),
                            packageName = o.optString("package_name", ""),
                            versionCode = o.optString("version_code", ""),
                            versionName = o.optString("version_name", ""),
                            minSdk = o.optString("min_sdk", ""),
                            targetSdk = o.optString("target_sdk", ""),
                            status = o.optString("status", ""),
                            lastStep = o.optString("last_step", ""),
                            userId = o.optInt("user_id", 0)
                        )
                    )
                }
                callback.onSuccess(list)
            }

            override fun onError(errorMessage: String) {
                callback.onError(errorMessage)
            }
        })
    }

    fun switchProject(projectId: String, callback: ApiCallback<JSONObject>) {
        executePost("switch_project", mapOf("project_id" to projectId), object : ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                session.currentProjectId = projectId
                callback.onSuccess(result)
            }

            override fun onError(errorMessage: String) {
                callback.onError(errorMessage)
            }
        })
    }

    fun renameProject(projectId: String, newName: String, callback: ApiCallback<String>) {
        executePost("workflow_rename_project_ajax", mapOf("project_id" to projectId, "new_name" to newName), object : ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                callback.onSuccess(result.optString("message", "Project renamed."))
            }

            override fun onError(errorMessage: String) {
                callback.onError(errorMessage)
            }
        })
    }

    fun deleteProject(projectId: String, callback: ApiCallback<JSONObject>) {
        executePost("delete_project_ajax", mapOf("project_id" to projectId), object : ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                if (session.currentProjectId == projectId) {
                    session.currentProjectId = null
                }
                callback.onSuccess(result)
            }

            override fun onError(errorMessage: String) {
                callback.onError(errorMessage)
            }
        })
    }

    fun closeProject(callback: ApiCallback<String>) {
        executePost("workflow_close_project_ajax", emptyMap(), object : ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                session.currentProjectId = null
                callback.onSuccess("Project closed.")
            }

            override fun onError(errorMessage: String) {
                callback.onError(errorMessage)
            }
        })
    }

    fun resetWorkflow(callback: ApiCallback<String>) {
        executePost("workflow_reset_ajax", emptyMap(), object : ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                callback.onSuccess("Workflow reset.")
            }

            override fun onError(errorMessage: String) {
                callback.onError(errorMessage)
            }
        })
    }

    fun getDirectory(dirPath: String, callback: ApiCallback<Pair<String, List<ProjectFile>>>) {
        executePost("workflow_get_dir_ajax", mapOf("dir_path" to dirPath), object : ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                val currentDir = result.optString("current_dir", "")
                val arr = result.optJSONArray("items") ?: JSONArray()
                val list = ArrayList<ProjectFile>()
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    list.add(
                        ProjectFile(
                            name = o.optString("name", ""),
                            path = o.optString("path", ""),
                            isDir = o.optBoolean("is_dir", false),
                            size = o.optLong("size", 0)
                        )
                    )
                }
                callback.onSuccess(Pair(currentDir, list))
            }

            override fun onError(errorMessage: String) {
                callback.onError(errorMessage)
            }
        })
    }

    fun uploadAndDecompileApk(apkFile: File, progressCallback: ProgressCallback, callback: ApiCallback<JSONObject>) {
        executeMultipart(
            action = "workflow_upload_decompile_ajax",
            fileParams = mapOf("workflow_apk" to apkFile),
            textParams = emptyMap(),
            progressCallback = progressCallback,
            callback = object : ApiCallback<JSONObject> {
                override fun onSuccess(result: JSONObject) {
                    val state = result.optJSONObject("state")
                    if (state != null) {
                        session.currentProjectId = state.optString("project_id", null)
                    }
                    callback.onSuccess(result)
                }

                override fun onError(errorMessage: String) {
                    callback.onError(errorMessage)
                }
            }
        )
    }

    // -------------------------------------------------------------
    // Resources, Code Editor & AI Studios
    // -------------------------------------------------------------

    fun loadStrings(locale: String, callback: ApiCallback<Map<String, String>>) {
        executePost("workflow_load_strings_ajax", mapOf("workflow_locale" to locale), object : ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                val map = HashMap<String, String>()
                val s = result.optJSONObject("strings") ?: result
                val keys = s.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    map[k] = s.optString(k, "")
                }
                callback.onSuccess(map)
            }

            override fun onError(errorMessage: String) {
                callback.onError(errorMessage)
            }
        })
    }

    fun autosaveStrings(locale: String, appName: String, stringsMap: Map<String, String>, callback: ApiCallback<JSONObject>) {
        val params = HashMap<String, String>()
        params["workflow_locale"] = locale
        params["workflow_app_name"] = appName
        for ((k, v) in stringsMap) {
            params["workflow_strings[$k]"] = v
        }
        executePost("workflow_autosave_strings_ajax", params, callback)
    }

    fun applyFirebaseJson(jsonFile: File, callback: ApiCallback<JSONObject>) {
        executeMultipart(
            action = "workflow_apply_firebase_ajax",
            fileParams = mapOf("workflow_firebase_json" to jsonFile),
            callback = callback
        )
    }

    fun uploadLogo(logoFile: File, callback: ApiCallback<JSONObject>) {
        executeMultipart(
            action = "workflow_upload_logo_ajax",
            fileParams = mapOf("workflow_logo" to logoFile),
            callback = callback
        )
    }

    fun generateAiIcon(prompt: String, callback: ApiCallback<JSONObject>) {
        executePost("workflow_ai_generate_icon_ajax", mapOf("ai_icon_prompt" to prompt), callback)
    }

    fun findProject(findText: String, callback: ApiCallback<JSONObject>) {
        executePost("workflow_find_project_ajax", mapOf("workflow_find_text" to findText), callback)
    }

    fun findReplaceProject(findText: String, replaceText: String, callback: ApiCallback<JSONObject>) {
        executePost("workflow_find_replace_ajax", mapOf("workflow_find_text" to findText, "workflow_replace_text" to replaceText), callback)
    }

    fun openEditorFile(filePath: String, binaryOffset: Long = 0, callback: ApiCallback<JSONObject>) {
        executePost("workflow_open_editor_file_ajax", mapOf("workflow_file_path" to filePath, "binary_offset" to binaryOffset.toString()), callback)
    }

    fun saveEditorFile(filePath: String, content: String, binaryOffset: Long = 0, binaryLength: Long = 0, callback: ApiCallback<JSONObject>) {
        executePost(
            "workflow_save_editor_file_ajax",
            mapOf(
                "workflow_file_path" to filePath,
                "workflow_file_content" to content,
                "binary_offset" to binaryOffset.toString(),
                "binary_length" to binaryLength.toString()
            ),
            callback
        )
    }

    fun replaceFile(filePath: String, newFile: File, callback: ApiCallback<JSONObject>) {
        executeMultipart(
            action = "workflow_replace_file_ajax",
            fileParams = mapOf("replacement_file" to newFile),
            textParams = mapOf("workflow_file_path" to filePath),
            callback = callback
        )
    }

    fun searchHex(filePath: String, query: String, callback: ApiCallback<List<HexResult>>) {
        executePost("workflow_search_hex_ajax", mapOf("workflow_file_path" to filePath, "query" to query), object : ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                val list = ArrayList<HexResult>()
                val arr = result.optJSONArray("results") ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    list.add(
                        HexResult(
                            offset = o.optLong("offset", 0),
                            hexOffset = o.optString("hex_offset", ""),
                            pageOffset = o.optLong("page_offset", 0),
                            pageHex = o.optString("page_hex", ""),
                            hexSnippet = o.optString("hex_snippet", ""),
                            asciiSnippet = o.optString("ascii_snippet", "")
                        )
                    )
                }
                callback.onSuccess(list)
            }

            override fun onError(errorMessage: String) {
                callback.onError(errorMessage)
            }
        })
    }

    fun aiReviewEditor(filePath: String, content: String, callback: ApiCallback<JSONObject>) {
        executePost("workflow_ai_review_editor_ajax", mapOf("workflow_file_path" to filePath, "workflow_file_content" to content), callback)
    }

    fun aiFixError(filePath: String = "", callback: ApiCallback<JSONObject>) {
        executePost("workflow_ai_fix_ajax", mapOf("workflow_file_path" to filePath), callback)
    }

    fun aiApplyFix(callback: ApiCallback<JSONObject>) {
        executePost("workflow_ai_apply_fix_ajax", emptyMap(), callback)
    }

    // -------------------------------------------------------------
    // Build, Keystore & Signing Studio
    // -------------------------------------------------------------

    fun buildApk(callback: ApiCallback<JSONObject>) {
        executePost("workflow_build_apk_ajax", emptyMap(), callback)
    }

    fun getKeystores(callback: ApiCallback<List<KeystoreItem>>) {
        executePost("get_keystores", emptyMap(), object : ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                val list = ArrayList<KeystoreItem>()
                val arr = result.optJSONArray("keystores") ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    list.add(
                        KeystoreItem(
                            id = o.optInt("id", 0),
                            username = o.optString("username", ""),
                            fileName = o.optString("file_name", ""),
                            keyAlias = o.optString("key_alias", ""),
                            createdAt = o.optString("created_at", "")
                        )
                    )
                }
                callback.onSuccess(list)
            }

            override fun onError(errorMessage: String) {
                callback.onError(errorMessage)
            }
        })
    }

    fun createKeystore(alias: String, pass: String, callback: ApiCallback<JSONObject>) {
        executePost("workflow_create_keystore_ajax", mapOf("workflow_key_alias" to alias, "workflow_key_password" to pass), callback)
    }

    fun selectKeystore(id: Int, callback: ApiCallback<JSONObject>) {
        executePost("workflow_select_keystore_ajax", mapOf("keystore_id" to id.toString()), callback)
    }

    fun signApk(pass: String, callback: ApiCallback<JSONObject>) {
        executePost("workflow_sign_apk_ajax", mapOf("workflow_sign_password" to pass), callback)
    }

    // -------------------------------------------------------------
    // ADB & Cloud Logging Endpoints
    // -------------------------------------------------------------

    fun adbListDevices(callback: ApiCallback<List<AdbDevice>>) {
        executePost("adb_list_devices_ajax", emptyMap(), object : ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                val list = ArrayList<AdbDevice>()
                val arr = result.optJSONArray("devices") ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    list.add(
                        AdbDevice(
                            serial = o.optString("serial", ""),
                            state = o.optString("state", ""),
                            model = o.optString("model", "")
                        )
                    )
                }
                callback.onSuccess(list)
            }

            override fun onError(errorMessage: String) {
                callback.onError(errorMessage)
            }
        })
    }

    fun adbConnect(host: String, callback: ApiCallback<JSONObject>) {
        executePost("adb_connect_ajax", mapOf("adb_host" to host), callback)
    }

    fun adbDisconnect(host: String, callback: ApiCallback<String>) {
        executePost("adb_disconnect_ajax", mapOf("adb_host" to host), object : ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                callback.onSuccess(result.optString("message", "Disconnected."))
            }

            override fun onError(errorMessage: String) {
                callback.onError(errorMessage)
            }
        })
    }

    fun adbInstallApk(serial: String, variant: String = "signed", callback: ApiCallback<String>) {
        executePost("adb_install_apk_ajax", mapOf("adb_serial" to serial, "apk_variant" to variant), object : ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                callback.onSuccess(result.optString("message", "APK installed."))
            }

            override fun onError(errorMessage: String) {
                callback.onError(errorMessage)
            }
        })
    }

    fun adbReadLogcat(serial: String, filter: String = "all", callback: ApiCallback<List<String>>) {
        executePost("adb_read_logcat_ajax", mapOf("adb_serial" to serial, "log_filter" to filter), object : ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                val list = ArrayList<String>()
                val arr = result.optJSONArray("lines") ?: JSONArray()
                for (i in 0 until arr.length()) {
                    list.add(arr.optString(i))
                }
                callback.onSuccess(list)
            }

            override fun onError(errorMessage: String) {
                callback.onError(errorMessage)
            }
        })
    }

    fun adbClearLogcat(serial: String, callback: ApiCallback<String>) {
        executePost("adb_clear_logcat_ajax", mapOf("adb_serial" to serial), object : ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                callback.onSuccess(result.optString("message", "Logcat cleared."))
            }

            override fun onError(errorMessage: String) {
                callback.onError(errorMessage)
            }
        })
    }

    fun enableCloudLogging(callback: ApiCallback<JSONObject>) {
        executePost("workflow_enable_cloud_logging_ajax", emptyMap(), callback)
    }

    fun getCloudLogs(callback: ApiCallback<String>) {
        executePost("workflow_get_cloud_logs_ajax", emptyMap(), object : ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                val arr = result.optJSONArray("lines") ?: JSONArray()
                val sb = StringBuilder()
                for (i in 0 until arr.length()) {
                    sb.append(arr.optString(i)).append("\n")
                }
                callback.onSuccess(sb.toString().trimEnd())
            }

            override fun onError(errorMessage: String) {
                callback.onError(errorMessage)
            }
        })
    }

    fun clearCloudLogs(callback: ApiCallback<JSONObject>) {
        executePost("workflow_clear_cloud_logs_ajax", emptyMap(), callback)
    }

    // -------------------------------------------------------------
    // AI Settings Endpoints
    // -------------------------------------------------------------

    fun getAiSettings(callback: ApiCallback<AiSettingsData>) {
        executePost("get_ai_settings_ajax", emptyMap(), object : ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                val eff = result.optJSONObject("effective_models") ?: JSONObject()
                val data = AiSettingsData(
                    provider = result.optString("provider", "gemini"),
                    geminiHasKey = result.optBoolean("gemini_has_key", false),
                    geminiMaskedKey = result.optString("gemini_masked_key", ""),
                    openaiHasKey = result.optBoolean("openai_has_key", false),
                    openaiMaskedKey = result.optString("openai_masked_key", ""),
                    geminiTextModel = eff.optString("gemini_text_model", "gemini-3.6-flash"),
                    geminiImageModel = eff.optString("gemini_image_model", "gemini-3.1-flash-image"),
                    openaiTextModel = eff.optString("openai_text_model", "gpt-5.6-sol"),
                    openaiImageModel = eff.optString("openai_image_model", "gpt-image-2")
                )
                callback.onSuccess(data)
            }

            override fun onError(errorMessage: String) {
                callback.onError(errorMessage)
            }
        })
    }

    fun saveApiKey(provider: String, key: String, callback: ApiCallback<String>) {
        executePost("save_api_key_ajax", mapOf("provider" to provider, "api_key" to key), object : ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                callback.onSuccess(result.optString("message", "API key saved."))
            }

            override fun onError(errorMessage: String) {
                callback.onError(errorMessage)
            }
        })
    }

    fun deleteApiKey(provider: String, callback: ApiCallback<String>) {
        executePost("delete_api_key_ajax", mapOf("provider" to provider), object : ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                callback.onSuccess(result.optString("message", "API key removed."))
            }

            override fun onError(errorMessage: String) {
                callback.onError(errorMessage)
            }
        })
    }

    fun saveAiProvider(provider: String, callback: ApiCallback<String>) {
        executePost("save_ai_provider_ajax", mapOf("provider" to provider), object : ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                callback.onSuccess(result.optString("message", "AI provider updated."))
            }

            override fun onError(errorMessage: String) {
                callback.onError(errorMessage)
            }
        })
    }

    fun saveUserAiModels(models: Map<String, String>, callback: ApiCallback<String>) {
        executePost("save_user_ai_models_ajax", models, object : ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                callback.onSuccess(result.optString("message", "AI models saved."))
            }

            override fun onError(errorMessage: String) {
                callback.onError(errorMessage)
            }
        })
    }

    fun resetUserAiModels(callback: ApiCallback<String>) {
        executePost("reset_user_ai_models_ajax", emptyMap(), object : ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                callback.onSuccess(result.optString("message", "AI models reset to default."))
            }

            override fun onError(errorMessage: String) {
                callback.onError(errorMessage)
            }
        })
    }

    // -------------------------------------------------------------
    // Admin Panel Endpoints
    // -------------------------------------------------------------

    fun getUsers(callback: ApiCallback<List<User>>) {
        executePost("get_users", emptyMap(), object : ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                val list = ArrayList<User>()
                val arr = result.optJSONArray("users") ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    list.add(
                        User(
                            id = o.optInt("id", 0),
                            username = o.optString("username", ""),
                            email = o.optString("email", ""),
                            userType = o.optString("user_type", "user"),
                            emailVerified = o.optInt("email_verified", 0),
                            decompileLimit = o.optInt("decompile_limit", 1),
                            decompileUsage = o.optInt("decompile_usage", 0),
                            compileLimit = o.optInt("compile_limit", 1),
                            compileUsage = o.optInt("compile_usage", 0),
                            generateKeyLimit = o.optInt("generate_key_limit", 1),
                            generateKeyUsage = o.optInt("generate_key_usage", 0),
                            signApkLimit = o.optInt("sign_apk_limit", 1),
                            signApkUsage = o.optInt("sign_apk_usage", 0)
                        )
                    )
                }
                callback.onSuccess(list)
            }

            override fun onError(errorMessage: String) {
                callback.onError(errorMessage)
            }
        })
    }

    fun createUser(userMap: Map<String, String>, callback: ApiCallback<String>) {
        executePost("create_user", userMap, object : ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                callback.onSuccess(result.optString("message", "User created successfully."))
            }

            override fun onError(errorMessage: String) {
                callback.onError(errorMessage)
            }
        })
    }

    fun updateLimits(userId: Int, dLimit: Int, cLimit: Int, kLimit: Int, sLimit: Int, callback: ApiCallback<String>) {
        executePost(
            "update_limits",
            mapOf(
                "user_id" to userId.toString(),
                "decompile_limit" to dLimit.toString(),
                "compile_limit" to cLimit.toString(),
                "generate_key_limit" to kLimit.toString(),
                "sign_apk_limit" to sLimit.toString()
            ),
            object : ApiCallback<JSONObject> {
                override fun onSuccess(result: JSONObject) {
                    callback.onSuccess(result.optString("message", "Limits updated."))
                }

                override fun onError(errorMessage: String) {
                    callback.onError(errorMessage)
                }
            }
        )
    }

    fun getContactInquiries(callback: ApiCallback<List<ContactInquiry>>) {
        executePost("get_contact_inquiries", emptyMap(), object : ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                val list = ArrayList<ContactInquiry>()
                val arr = result.optJSONArray("inquiries") ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    list.add(
                        ContactInquiry(
                            id = o.optInt("id", 0),
                            name = o.optString("name", ""),
                            email = o.optString("email", ""),
                            subject = o.optString("subject", ""),
                            message = o.optString("message", ""),
                            status = o.optString("status", "new"),
                            createdAt = o.optString("created_at", "")
                        )
                    )
                }
                callback.onSuccess(list)
            }

            override fun onError(errorMessage: String) {
                callback.onError(errorMessage)
            }
        })
    }

    fun markContactInquiryRead(id: Int, callback: ApiCallback<String>) {
        executePost("mark_contact_inquiry_read", mapOf("id" to id.toString()), object : ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                callback.onSuccess(result.optString("message", "Marked as read."))
            }

            override fun onError(errorMessage: String) {
                callback.onError(errorMessage)
            }
        })
    }

    fun deleteContactInquiry(id: Int, callback: ApiCallback<String>) {
        executePost("delete_contact_inquiry", mapOf("id" to id.toString()), object : ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                callback.onSuccess(result.optString("message", "Inquiry deleted."))
            }

            override fun onError(errorMessage: String) {
                callback.onError(errorMessage)
            }
        })
    }

    fun getAdminBlogs(callback: ApiCallback<List<BlogPost>>) {
        executePost("get_admin_blogs", emptyMap(), object : ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                val list = ArrayList<BlogPost>()
                val arr = result.optJSONArray("blogs") ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    list.add(
                        BlogPost(
                            id = o.optInt("id", 0),
                            title = o.optString("title", ""),
                            slug = o.optString("slug", ""),
                            excerpt = o.optString("excerpt", ""),
                            content = o.optString("content", ""),
                            category = o.optString("category", ""),
                            tags = o.optString("tags", ""),
                            readTime = o.optString("read_time", "5 min read"),
                            views = o.optInt("views", 0),
                            createdAt = o.optString("created_at", "")
                        )
                    )
                }
                callback.onSuccess(list)
            }

            override fun onError(errorMessage: String) {
                callback.onError(errorMessage)
            }
        })
    }

    fun saveAdminBlog(blogMap: Map<String, String>, callback: ApiCallback<String>) {
        executePost("save_admin_blog", blogMap, object : ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                callback.onSuccess(result.optString("message", "Blog saved successfully."))
            }

            override fun onError(errorMessage: String) {
                callback.onError(errorMessage)
            }
        })
    }

    fun deleteAdminBlog(id: Int, callback: ApiCallback<String>) {
        executePost("delete_admin_blog", mapOf("id" to id.toString()), object : ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                callback.onSuccess(result.optString("message", "Blog deleted."))
            }

            override fun onError(errorMessage: String) {
                callback.onError(errorMessage)
            }
        })
    }

    fun getFaqs(callback: ApiCallback<List<FaqItem>>) {
        executePost("get_faqs", emptyMap(), object : ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                val list = ArrayList<FaqItem>()
                val arr = result.optJSONArray("faqs") ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    list.add(
                        FaqItem(
                            id = o.optInt("id", 0),
                            question = o.optString("question", ""),
                            answer = o.optString("answer", ""),
                            category = o.optString("category", ""),
                            sortOrder = o.optInt("sort_order", 0),
                            isActive = o.optInt("is_active", 1)
                        )
                    )
                }
                callback.onSuccess(list)
            }

            override fun onError(errorMessage: String) {
                callback.onError(errorMessage)
            }
        })
    }

    fun getAdminFaqs(callback: ApiCallback<List<FaqItem>>) {
        executePost("get_admin_faqs", emptyMap(), object : ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                val list = ArrayList<FaqItem>()
                val arr = result.optJSONArray("faqs") ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    list.add(
                        FaqItem(
                            id = o.optInt("id", 0),
                            question = o.optString("question", ""),
                            answer = o.optString("answer", ""),
                            category = o.optString("category", ""),
                            sortOrder = o.optInt("sort_order", 0),
                            isActive = o.optInt("is_active", 1)
                        )
                    )
                }
                callback.onSuccess(list)
            }

            override fun onError(errorMessage: String) {
                callback.onError(errorMessage)
            }
        })
    }

    fun saveAdminFaq(faqMap: Map<String, String>, callback: ApiCallback<String>) {
        executePost("save_admin_faq", faqMap, object : ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                callback.onSuccess(result.optString("message", "FAQ saved."))
            }

            override fun onError(errorMessage: String) {
                callback.onError(errorMessage)
            }
        })
    }

    fun deleteAdminFaq(id: Int, callback: ApiCallback<String>) {
        executePost("delete_admin_faq", mapOf("id" to id.toString()), object : ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                callback.onSuccess(result.optString("message", "FAQ deleted."))
            }

            override fun onError(errorMessage: String) {
                callback.onError(errorMessage)
            }
        })
    }

    fun getAdminBackupSettings(callback: ApiCallback<Map<String, String>>) {
        executePost("get_admin_backup_settings", emptyMap(), object : ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                val map = HashMap<String, String>()
                val s = result.optJSONObject("settings") ?: JSONObject()
                val keys = s.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    map[k] = s.optString(k, "")
                }
                callback.onSuccess(map)
            }

            override fun onError(errorMessage: String) {
                callback.onError(errorMessage)
            }
        })
    }

    fun saveAdminBackupSettings(settingsMap: Map<String, String>, callback: ApiCallback<String>) {
        executePost("save_admin_backup_settings", settingsMap, object : ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                callback.onSuccess(result.optString("message", "Backup settings saved."))
            }

            override fun onError(errorMessage: String) {
                callback.onError(errorMessage)
            }
        })
    }

    fun runAdminManualBackup(callback: ApiCallback<String>) {
        executePost("run_admin_manual_backup", emptyMap(), object : ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                callback.onSuccess(result.optString("message", "Backup triggered successfully."))
            }

            override fun onError(errorMessage: String) {
                callback.onError(errorMessage)
            }
        })
    }

    fun getGlobalAiSettings(callback: ApiCallback<Map<String, String>>) {
        executePost("get_global_ai_settings", emptyMap(), object : ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                val map = HashMap<String, String>()
                val s = result.optJSONObject("settings") ?: JSONObject()
                val keys = s.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    map[k] = s.optString(k, "")
                }
                callback.onSuccess(map)
            }

            override fun onError(errorMessage: String) {
                callback.onError(errorMessage)
            }
        })
    }

    fun saveGlobalAiSettings(settingsMap: Map<String, String>, callback: ApiCallback<String>) {
        executePost("save_global_ai_settings", settingsMap, object : ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                callback.onSuccess(result.optString("message", "Global AI settings saved."))
            }

            override fun onError(errorMessage: String) {
                callback.onError(errorMessage)
            }
        })
    }

    // -------------------------------------------------------------
    // Public Community & Support Endpoints
    // -------------------------------------------------------------

    fun getPublicBlogs(callback: ApiCallback<List<BlogPost>>) {
        executePost("get_blogs", emptyMap(), object : ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                val list = ArrayList<BlogPost>()
                val arr = result.optJSONArray("blogs") ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    list.add(
                        BlogPost(
                            id = o.optInt("id", 0),
                            title = o.optString("title", ""),
                            slug = o.optString("slug", ""),
                            excerpt = o.optString("excerpt", ""),
                            content = o.optString("content", ""),
                            category = o.optString("category", ""),
                            tags = o.optString("tags", ""),
                            readTime = o.optString("read_time", "5 min read"),
                            views = o.optInt("views", 0),
                            createdAt = o.optString("created_at", "")
                        )
                    )
                }
                callback.onSuccess(list)
            }

            override fun onError(errorMessage: String) {
                callback.onError(errorMessage)
            }
        })
    }

    // -------------------------------------------------------------
    // Device Companion Polling Endpoints
    // -------------------------------------------------------------

    fun checkDeviceUpdate(token: String, callback: ApiCallback<JSONObject>) {
        executor.execute {
            try {
                val base = getIndexPath()
                val checkUrl = URL(base + (if (base.contains("?")) "&" else "?") + "device_check=1&token=" + URLEncoder.encode(token, "UTF-8"))
                val conn = checkUrl.openConnection() as HttpURLConnection
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val body = stream?.bufferedReader()?.use { it.readText() } ?: "{}"
                conn.disconnect()
                val json = JSONObject(body)
                mainHandler.post {
                    if (json.optString("status") == "success") {
                        callback.onSuccess(json)
                    } else {
                        callback.onError(json.optString("message", "Device check failed."))
                    }
                }
            } catch (e: Exception) {
                mainHandler.post {
                    callback.onError(e.message ?: "Failed to check update.")
                }
            }
        }
    }

    fun getDeviceLogs(token: String, callback: ApiCallback<List<String>>) {
        executor.execute {
            try {
                val base = getIndexPath()
                val logsUrl = URL(base + (if (base.contains("?")) "&" else "?") + "device_logs=1&token=" + URLEncoder.encode(token, "UTF-8"))
                val conn = logsUrl.openConnection() as HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val body = stream?.bufferedReader()?.use { it.readText() } ?: "{}"
                conn.disconnect()
                val json = JSONObject(body)
                mainHandler.post {
                    if (json.optString("status") == "success") {
                        val arr = json.optJSONArray("lines") ?: JSONArray()
                        val list = ArrayList<String>()
                        for (i in 0 until arr.length()) {
                            list.add(arr.optString(i))
                        }
                        callback.onSuccess(list)
                    } else {
                        callback.onError("No logs available.")
                    }
                }
            } catch (e: Exception) {
                mainHandler.post {
                    callback.onError(e.message ?: "Logs polling failed.")
                }
            }
        }
    }

    // -------------------------------------------------------------
    // Convenience Overloads & Aliases
    // -------------------------------------------------------------

    fun selectProject(projectId: String, callback: ApiCallback<JSONObject>) {
        switchProject(projectId, callback)
    }

    fun uploadAndDecompile(apkFile: File, progressCallback: ProgressCallback, callback: ApiCallback<JSONObject>) {
        uploadAndDecompileApk(apkFile, progressCallback, callback)
    }

    fun getFiles(dirPath: String, callback: ApiCallback<List<ProjectFile>>) {
        getDirectory(dirPath, object : ApiCallback<Pair<String, List<ProjectFile>>> {
            override fun onSuccess(result: Pair<String, List<ProjectFile>>) {
                callback.onSuccess(result.second)
            }

            override fun onError(errorMessage: String) {
                callback.onError(errorMessage)
            }
        })
    }

    fun getFileContent(filePath: String, callback: ApiCallback<JSONObject>) {
        openEditorFile(filePath, 0, callback)
    }

    fun saveFileContent(filePath: String, content: String, callback: ApiCallback<JSONObject>) {
        saveEditorFile(filePath, content, 0, 0, callback)
    }

    fun aiReview(filePath: String, content: String, callback: ApiCallback<JSONObject>) {
        aiReviewEditor(filePath, content, callback)
    }

    fun hexSearch(filePath: String, query: String, callback: ApiCallback<List<HexResult>>) {
        searchHex(filePath, query, callback)
    }

    fun hexPatch(filePath: String, offset: Long, patch: String, callback: ApiCallback<JSONObject>) {
        saveEditorFile(filePath, patch, offset, patch.length.toLong(), callback)
    }

    fun saveStrings(locale: String, stringsMap: Map<String, String>, callback: ApiCallback<JSONObject>) {
        autosaveStrings(locale, stringsMap["app_name"] ?: "", stringsMap, callback)
    }

    fun generateIcon(prompt: String, callback: ApiCallback<JSONObject>) {
        generateAiIcon(prompt, callback)
    }

    fun aiFixAll(callback: ApiCallback<JSONObject>) {
        aiApplyFix(callback)
    }

    fun globalFind(query: String, callback: ApiCallback<JSONObject>) {
        findProject(query, callback)
    }

    fun globalReplace(find: String, replace: String, callback: ApiCallback<JSONObject>) {
        findReplaceProject(find, replace, callback)
    }

    fun downloadSignedApk(callback: ApiCallback<File>) {
        val pid = session.currentProjectId ?: ""
        val url = getIndexPath() + (if (getIndexPath().contains("?")) "&" else "?") + "download_signed=1&project_id=" + URLEncoder.encode(pid, "UTF-8")
        val dest = File.createTempFile("signed_apk_", ".apk")
        downloadApk(url, null, 0, dest, object : ProgressCallback {
            override fun onProgress(percentage: Int, message: String) {}
        }, callback)
    }

    fun getAdbDevices(callback: ApiCallback<List<AdbDevice>>) {
        adbListDevices(callback)
    }

    fun getAdbLogcat(filter: String, callback: ApiCallback<String>) {
        adbReadLogcat("", filter, object : ApiCallback<List<String>> {
            override fun onSuccess(result: List<String>) {
                callback.onSuccess(result.joinToString("\n"))
            }

            override fun onError(errorMessage: String) {
                callback.onError(errorMessage)
            }
        })
    }

    fun saveGeminiKey(key: String, callback: ApiCallback<String>) = saveApiKey("gemini", key, callback)
    fun deleteGeminiKey(callback: ApiCallback<String>) = deleteApiKey("gemini", callback)
    fun saveOpenAiKey(key: String, callback: ApiCallback<String>) = saveApiKey("openai", key, callback)
    fun deleteOpenAiKey(callback: ApiCallback<String>) = deleteApiKey("openai", callback)

    fun saveCustomModels(gt: String, gi: String, ot: String, oi: String, callback: ApiCallback<String>) {
        saveUserAiModels(
            mapOf(
                "gemini_text_model" to gt,
                "gemini_image_model" to gi,
                "openai_text_model" to ot,
                "openai_image_model" to oi
            ),
            callback
        )
    }

    fun getAdminUsers(callback: ApiCallback<List<User>>) = getUsers(callback)
    fun getAdminInquiries(callback: ApiCallback<List<ContactInquiry>>) = getContactInquiries(callback)

    fun adminCreateUser(e: String, u: String, p: String, d: Int, c: Int, k: Int, s: Int, callback: ApiCallback<JSONObject>) {
        createUser(
            mapOf(
                "email" to e,
                "username" to u,
                "password" to p,
                "decompile_limit" to d.toString(),
                "compile_limit" to c.toString(),
                "generate_key_limit" to k.toString(),
                "sign_apk_limit" to s.toString()
            ),
            object : ApiCallback<String> {
                override fun onSuccess(result: String) {
                    callback.onSuccess(JSONObject().put("status", "success").put("message", result))
                }

                override fun onError(errorMessage: String) {
                    callback.onError(errorMessage)
                }
            }
        )
    }

    fun adminSaveBlog(id: Int, t: String, c: String, r: String, tg: String, ex: String, ct: String, callback: ApiCallback<JSONObject>) {
        saveAdminBlog(
            mapOf(
                "id" to id.toString(),
                "title" to t,
                "category" to c,
                "read_time" to r,
                "tags" to tg,
                "excerpt" to ex,
                "content" to ct
            ),
            object : ApiCallback<String> {
                override fun onSuccess(result: String) {
                    callback.onSuccess(JSONObject().put("status", "success").put("message", result))
                }

                override fun onError(errorMessage: String) {
                    callback.onError(errorMessage)
                }
            }
        )
    }

    fun adminSaveFaq(id: Int, q: String, c: String, a: String, callback: ApiCallback<JSONObject>) {
        saveAdminFaq(
            mapOf(
                "id" to id.toString(),
                "question" to q,
                "category" to c,
                "answer" to a
            ),
            object : ApiCallback<String> {
                override fun onSuccess(result: String) {
                    callback.onSuccess(JSONObject().put("status", "success").put("message", result))
                }

                override fun onError(errorMessage: String) {
                    callback.onError(errorMessage)
                }
            }
        )
    }

    fun saveBackupSettings(owner: String, repo: String, branch: String, token: String, callback: ApiCallback<JSONObject>) {
        saveAdminBackupSettings(
            mapOf(
                "github_repo_owner" to owner,
                "github_repo_name" to repo,
                "github_branch" to branch,
                "github_token" to token
            ),
            object : ApiCallback<String> {
                override fun onSuccess(result: String) {
                    callback.onSuccess(JSONObject().put("status", "success").put("message", result))
                }

                override fun onError(errorMessage: String) {
                    callback.onError(errorMessage)
                }
            }
        )
    }

    fun runBackup(callback: ApiCallback<JSONObject>) {
        runAdminManualBackup(object : ApiCallback<String> {
            override fun onSuccess(result: String) {
                callback.onSuccess(JSONObject().put("status", "success").put("message", result))
            }

            override fun onError(errorMessage: String) {
                callback.onError(errorMessage)
            }
        })
    }

    fun getPublicFaqs(callback: ApiCallback<List<FaqItem>>) = getFaqs(callback)

    fun submitContactInquiry(name: String, email: String, subject: String, message: String, callback: ApiCallback<JSONObject>) {
        executePost(
            "submit_contact_inquiry",
            mapOf("name" to name, "email" to email, "subject" to subject, "message" to message),
            callback
        )
    }

    fun checkBuildUpdate(callback: ApiCallback<Boolean>) {
        val tok = session.pairingToken ?: ""
        checkDeviceUpdate(
            tok,
            object : ApiCallback<JSONObject> {
                override fun onSuccess(result: JSONObject) {
                    callback.onSuccess(result.optBoolean("has_update", false))
                }

                override fun onError(errorMessage: String) {
                    callback.onError(errorMessage)
                }
            }
        )
    }
}

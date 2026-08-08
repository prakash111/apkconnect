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
import java.util.concurrent.Executors

class ApiClient(private val session: SessionManager) {

    private val executor = Executors.newFixedThreadPool(4)
    private val mainHandler = Handler(Looper.getMainLooper())

    companion object {
        init {
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

                for ((key, value) in mergedText) {
                    writer.append(twoHyphens).append(boundary).append(lineEnd)
                    writer.append("Content-Disposition: form-data; name=\"").append(key).append("\"").append(lineEnd)
                    writer.append("Content-Type: text/plain; charset=UTF-8").append(lineEnd)
                    writer.append(lineEnd)
                    writer.append(value).append(lineEnd)
                    writer.flush()
                }

                var totalBytes = 0L
                for ((_, file) in fileParams) totalBytes += file.length()

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
                        val msg = json.optString("message", "Upload failed: ${json.optString("status")}")
                        callback.onError(msg)
                    }
                }
            } catch (e: Exception) {
                mainHandler.post {
                    callback.onError(e.message ?: "Network error during upload.")
                }
            }
        }
    }

    fun downloadApk(urlStr: String, destinationFile: File, progressCallback: ProgressCallback? = null, callback: ApiCallback<File>) {
        executor.execute {
            try {
                val fullUrl = if (urlStr.startsWith("http://") || urlStr.startsWith("https://")) urlStr else getBaseUrl() + urlStr.removePrefix("/")
                val url = URL(fullUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 30000
                conn.readTimeout = 120000
                conn.doInput = true

                val responseCode = conn.responseCode
                if (responseCode !in 200..299) {
                    val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
                    conn.disconnect()
                    throw IOException("Download failed ($responseCode): $err")
                }

                val totalLength = conn.contentLength.toLong()
                var downloaded = 0L

                destinationFile.parentFile?.mkdirs()
                conn.inputStream.use { input ->
                    FileOutputStream(destinationFile).use { output ->
                        val buffer = ByteArray(8192)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            downloaded += read
                            if (totalLength > 0) {
                                val pct = ((downloaded * 100) / totalLength).toInt()
                                mainHandler.post {
                                    progressCallback?.onProgress(pct, "Downloading ($pct%)...")
                                }
                            }
                        }
                        output.flush()
                    }
                }
                conn.disconnect()

                mainHandler.post {
                    callback.onSuccess(destinationFile)
                }
            } catch (e: Exception) {
                mainHandler.post {
                    callback.onError(e.message ?: "Download error.")
                }
            }
        }
    }

    // -------------------------------------------------------------
    // Auth & Users
    // -------------------------------------------------------------

    fun login(userOrEmail: String, pass: String, callback: ApiCallback<User>) {
        executePost("login", mapOf("login" to userOrEmail, "username" to userOrEmail, "password" to pass), object : ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                val userObj = result.optJSONObject("user")
                val u = if (userObj != null) {
                    User(
                        id = userObj.optInt("id", 0),
                        username = userObj.optString("username", userOrEmail),
                        email = userObj.optString("email", ""),
                        userType = userObj.optString("user_type", "user"),
                        emailVerified = userObj.optInt("email_verified", 0),
                        decompileLimit = userObj.optInt("decompile_limit", 1),
                        decompileUsage = userObj.optInt("decompile_usage", 0),
                        compileLimit = userObj.optInt("compile_limit", 1),
                        compileUsage = userObj.optInt("compile_usage", 0),
                        generateKeyLimit = userObj.optInt("generate_key_limit", 1),
                        generateKeyUsage = userObj.optInt("generate_key_usage", 0),
                        signApkLimit = userObj.optInt("sign_apk_limit", 1),
                        signApkUsage = userObj.optInt("sign_apk_usage", 0)
                    )
                } else {
                    User(username = userOrEmail)
                }
                session.saveUser(u)
                callback.onSuccess(u)
            }

            override fun onError(errorMessage: String) {
                callback.onError(errorMessage)
            }
        })
    }

    fun getLimits(callback: ApiCallback<UserLimits>) {
        executePost("get_limits", emptyMap(), object : ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                val lim = result.optJSONObject("limits") ?: result
                callback.onSuccess(
                    UserLimits(
                        decompileLimit = lim.optInt("decompile_limit", 1),
                        decompileUsage = lim.optInt("decompile_usage", 0),
                        compileLimit = lim.optInt("compile_limit", 1),
                        compileUsage = lim.optInt("compile_usage", 0),
                        generateKeyLimit = lim.optInt("generate_key_limit", 1),
                        generateKeyUsage = lim.optInt("generate_key_usage", 0),
                        signApkLimit = lim.optInt("sign_apk_limit", 1),
                        signApkUsage = lim.optInt("sign_apk_usage", 0)
                    )
                )
            }
            override fun onError(errorMessage: String) {
                callback.onError(errorMessage)
            }
        })
    }

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
                            mobile = o.optString("mobile", ""),
                            userType = o.optString("user_type", "user"),
                            status = o.optString("status", "active"),
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

    // -------------------------------------------------------------
    // Projects & Workspaces
    // -------------------------------------------------------------

    fun getWorkflowState(callback: ApiCallback<JSONObject>) {
        executePost("workflow_state_ajax", emptyMap(), callback)
    }

    fun getProjects(callback: ApiCallback<List<ProjectItem>>) {
        executePost("get_projects", emptyMap(), object : ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                val list = ArrayList<ProjectItem>()
                val arr = result.optJSONArray("projects") ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    list.add(
                        ProjectItem(
                            projectId = o.optString("project_id", ""),
                            projectName = o.optString("project_name", ""),
                            updatedAt = o.optString("updated_at", "")
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

    fun uploadAndDecompileApk(apkFile: File, progressCallback: ProgressCallback? = null, callback: ApiCallback<JSONObject>) {
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

    fun applyFirebaseJson(jsonFile: File, callback: ApiCallback<JSONObject>) {
        executeMultipart(
            action = "workflow_apply_firebase_ajax",
            fileParams = mapOf("workflow_firebase_json" to jsonFile),
            callback = callback
        )
    }

    fun getDirectory(dirPath: String, callback: ApiCallback<Pair<String, List<ProjectFile>>>) {
        executePost("workflow_get_dir_ajax", mapOf("dir_path" to dirPath), object : ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                val currentDir = result.optString("current_dir", "")
                val list = ArrayList<ProjectFile>()
                val items = result.optJSONArray("items") ?: JSONArray()
                for (i in 0 until items.length()) {
                    val o = items.optJSONObject(i) ?: continue
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

    fun openEditorFile(filePath: String, binaryOffset: Long = 0, callback: ApiCallback<EditorFile>) {
        executePost("workflow_open_editor_file_ajax", mapOf("workflow_file_path" to filePath, "binary_offset" to binaryOffset.toString()), object : ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                val state = result.optJSONObject("state")
                val ef = state?.optJSONObject("editor_file")
                if (ef != null) {
                    callback.onSuccess(
                        EditorFile(
                            path = ef.optString("path", filePath),
                            content = ef.optString("content", ""),
                            binary = ef.optBoolean("binary", false),
                            isImage = ef.optBoolean("is_image", false),
                            binaryOffset = ef.optLong("binary_offset", 0),
                            binarySize = ef.optLong("binary_size", 0)
                        )
                    )
                } else {
                    callback.onSuccess(EditorFile(path = filePath, content = result.optString("content", "")))
                }
            }
            override fun onError(errorMessage: String) {
                callback.onError(errorMessage)
            }
        })
    }

    fun saveEditorFile(filePath: String, content: String, callback: ApiCallback<String>) {
        executePost(
            "workflow_save_editor_file_ajax",
            mapOf("workflow_file_path" to filePath, "workflow_file_content" to content),
            object : ApiCallback<JSONObject> {
                override fun onSuccess(result: JSONObject) {
                    callback.onSuccess(result.optString("message", "File saved."))
                }
                override fun onError(errorMessage: String) {
                    callback.onError(errorMessage)
                }
            }
        )
    }

    // -------------------------------------------------------------
    // Find & Replace
    // -------------------------------------------------------------

    fun findInProject(query: String, callback: ApiCallback<FindResult>) {
        executePost("workflow_find_project_ajax", mapOf("workflow_find_text" to query), object : ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                val list = ArrayList<FindMatch>()
                val state = result.optJSONObject("state")
                val findObj = state?.optJSONObject("last_find_only")
                val filesArr = findObj?.optJSONArray("files") ?: JSONArray()
                for (i in 0 until filesArr.length()) {
                    val obj = filesArr.opt(i)
                    if (obj is JSONObject) {
                        list.add(FindMatch(path = obj.optString("path", ""), matches = obj.optInt("matches", 1), snippet = obj.optString("snippet", "")))
                    } else if (obj is String) {
                        list.add(FindMatch(path = obj))
                    }
                }
                callback.onSuccess(FindResult(files = list))
            }
            override fun onError(errorMessage: String) {
                callback.onError(errorMessage)
            }
        })
    }

    fun findAndReplace(find: String, replace: String, callback: ApiCallback<FindReplaceResult>) {
        executePost("workflow_find_replace_ajax", mapOf("workflow_find_text" to find, "workflow_replace_text" to replace), object : ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                val state = result.optJSONObject("state")
                val repObj = state?.optJSONObject("last_find_replace")
                callback.onSuccess(
                    FindReplaceResult(
                        replacements = repObj?.optInt("replacements", 0) ?: 0,
                        filesChanged = repObj?.optInt("files_changed", 0) ?: 0
                    )
                )
            }
            override fun onError(errorMessage: String) {
                callback.onError(errorMessage)
            }
        })
    }

    // -------------------------------------------------------------
    // Strings & Keystores & Build
    // -------------------------------------------------------------

    fun loadStrings(locale: String, callback: ApiCallback<StringData>) {
        executePost("workflow_load_strings_ajax", mapOf("workflow_locale" to locale), object : ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                val state = result.optJSONObject("state")
                val appName = state?.optString("app_name", "") ?: ""
                val arr = state?.optJSONArray("all_strings") ?: JSONArray()
                val list = ArrayList<StringItem>()
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    list.add(StringItem(name = o.optString("name", ""), value = o.optString("value", "")))
                }
                callback.onSuccess(StringData(appName = appName, allStrings = list))
            }
            override fun onError(errorMessage: String) {
                callback.onError(errorMessage)
            }
        })
    }

    fun saveStrings(locale: String, appName: String, stringsMap: Map<String, String>, callback: ApiCallback<String>) {
        val params = HashMap<String, String>()
        params["workflow_locale"] = locale
        params["workflow_app_name"] = appName
        for ((k, v) in stringsMap) {
            params["workflow_strings[$k]"] = v
        }
        executePost("workflow_autosave_strings_ajax", params, object : ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                callback.onSuccess(result.optString("message", "Strings saved."))
            }
            override fun onError(errorMessage: String) {
                callback.onError(errorMessage)
            }
        })
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

    fun createKeystore(alias: String, pass: String, callback: ApiCallback<String>) {
        executePost("workflow_create_keystore_ajax", mapOf("workflow_key_alias" to alias, "workflow_key_password" to pass), object : ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                callback.onSuccess(result.optString("message", "Keystore created."))
            }
            override fun onError(errorMessage: String) {
                callback.onError(errorMessage)
            }
        })
    }

    fun buildApk(keystoreId: String = "", password: String = "", callback: ApiCallback<BuildResult>) {
        executePost("workflow_build_apk_ajax", mapOf("keystore_id" to keystoreId, "sign_password" to password), object : ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                val state = result.optJSONObject("state")
                val signedApk = state?.optString("signed_apk", null)
                callback.onSuccess(BuildResult(success = true, signedApk = signedApk))
            }
            override fun onError(errorMessage: String) {
                callback.onError(errorMessage)
            }
        })
    }

    // -------------------------------------------------------------
    // Blogs & FAQs & Contact
    // -------------------------------------------------------------

    fun getPublicBlogs(callback: ApiCallback<List<BlogItem>>) {
        executePost("get_public_blogs", emptyMap(), object : ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                val list = ArrayList<BlogItem>()
                val arr = result.optJSONArray("blogs") ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    list.add(
                        BlogItem(
                            id = o.optInt("id", 0),
                            title = o.optString("title", ""),
                            slug = o.optString("slug", ""),
                            excerpt = o.optString("excerpt", ""),
                            content = o.optString("content", ""),
                            category = o.optString("category", ""),
                            tags = o.optString("tags", ""),
                            readTime = o.optString("read_time", "5 min read")
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
                            category = o.optString("category", "")
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

    fun submitContactInquiry(name: String, email: String, subject: String, message: String, callback: ApiCallback<String>) {
        executePost(
            "submit_contact_inquiry",
            mapOf("name" to name, "email" to email, "subject" to subject, "message" to message),
            object : ApiCallback<JSONObject> {
                override fun onSuccess(result: JSONObject) {
                    callback.onSuccess(result.optString("message", "Inquiry sent."))
                }
                override fun onError(errorMessage: String) {
                    callback.onError(errorMessage)
                }
            }
        )
    }
}

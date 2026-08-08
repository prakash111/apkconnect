package com.apktoolai.companion

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.apktoolai.companion.api.*
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import org.json.JSONObject
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private lateinit var api: ApiClient
    private val mainHandler = Handler(Looper.getMainLooper())

    // UI - Shell & Topbar
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var btnMenuToggle: ImageButton
    private lateinit var txtTopbarTitle: TextView
    private lateinit var txtTopbarProjectPill: TextView
    private lateinit var btnQuickNewApk: Button
    private lateinit var btnQuickSaveFile: Button
    private lateinit var btnQuickFind: ImageButton
    private lateinit var btnQuickLogout: ImageButton
    private lateinit var globalProgressBar: ProgressBar

    // Navigation Drawer
    private lateinit var navDashboard: TextView
    private lateinit var navProjects: TextView
    private lateinit var navEditor: TextView
    private lateinit var navSearch: TextView
    private lateinit var navStrings: TextView
    private lateinit var navKeystores: TextView
    private lateinit var navBuild: TextView
    private lateinit var navBlog: TextView
    private lateinit var navFaqs: TextView
    private lateinit var navContact: TextView
    private lateinit var navAdminSectionTitle: TextView
    private lateinit var navAdmin: TextView
    private lateinit var txtSidebarUserAvatar: TextView
    private lateinit var txtSidebarUsername: TextView
    private lateinit var btnSidebarLogout: Button

    // Tab Views
    private lateinit var viewAuth: ScrollView
    private lateinit var viewDashboard: ScrollView
    private lateinit var viewProjects: LinearLayout
    private lateinit var viewEditor: LinearLayout
    private lateinit var viewSearch: ScrollView
    private lateinit var viewStrings: ScrollView
    private lateinit var viewKeystores: ScrollView
    private lateinit var viewBuild: ScrollView
    private lateinit var viewBlog: ScrollView
    private lateinit var viewFaqs: ScrollView
    private lateinit var viewContact: ScrollView
    private lateinit var viewAdmin: ScrollView

    // Dashboard Elements
    private lateinit var txtStatDecompiles: TextView
    private lateinit var txtStatCompiles: TextView
    private lateinit var txtStatKeystores: TextView
    private lateinit var txtStatSignings: TextView
    private lateinit var cardActiveProjectBanner: LinearLayout
    private lateinit var txtActiveProjectTitle: TextView
    private lateinit var txtActiveProjectSubtitle: TextView
    private lateinit var btnDashOpenEditor: Button
    private lateinit var btnDashBuildApk: Button
    private lateinit var btnViewAllProjects: Button
    private lateinit var layoutDashProjectsList: LinearLayout

    // Projects Elements
    private lateinit var btnUploadNewApkProject: Button
    private lateinit var layoutProjectsList: LinearLayout

    // Editor Elements
    private lateinit var btnEditorGoUp: ImageButton
    private lateinit var txtEditorBreadcrumb: TextView
    private lateinit var btnEditorSaveFile: Button
    private lateinit var layoutEditorItems: LinearLayout
    private lateinit var editCodeContent: EditText
    private var currentEditorFilePath: String? = null
    private var currentEditorDir: String = ""

    // Search Elements
    private lateinit var editSearchFindQuery: EditText
    private lateinit var editSearchReplaceQuery: EditText
    private lateinit var btnExecuteSearch: Button
    private lateinit var btnExecuteReplace: Button
    private lateinit var txtSearchResultsHeader: TextView
    private lateinit var layoutSearchResultsList: LinearLayout

    // Strings Elements
    private lateinit var editAppNameInput: EditText
    private lateinit var btnSaveAppName: Button
    private lateinit var layoutStringsList: LinearLayout
    private val stringMap = mutableMapOf<String, String>()

    // Keystores Elements
    private lateinit var btnOpenCreateKeystoreDialog: Button
    private lateinit var layoutKeystoresList: LinearLayout

    // Build Elements
    private lateinit var btnStartBuildAndSign: Button
    private lateinit var layoutBuildOutput: LinearLayout
    private lateinit var btnDownloadSignedApk: Button
    private var lastSignedApkUrl: String? = null

    // Blog / FAQs / Contact
    private lateinit var layoutBlogsList: LinearLayout
    private lateinit var layoutFaqsList: LinearLayout
    private lateinit var editContactName: EditText
    private lateinit var editContactEmail: EditText
    private lateinit var editContactSubject: EditText
    private lateinit var editContactMessage: EditText
    private lateinit var btnSubmitContact: Button

    // Admin
    private lateinit var btnAdminCreateUser: Button
    private lateinit var layoutAdminUsersList: LinearLayout

    // Auth
    private lateinit var btnScanQr: Button
    private lateinit var btnOpenLoginDialog: Button
    private lateinit var editPairingKeyInput: EditText
    private lateinit var btnConnectPairingKey: Button

    private var currentActiveTab: String = "dashboard"
    private var activeProjectName: String? = null

    private val qrScanLauncher = registerForActivityResult(ScanContract()) { result ->
        val contents = result.contents
        if (contents != null) {
            editPairingKeyInput.setText(contents)
            handlePairingToken(contents)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        session = SessionManager.getInstance(this)
        api = ApiClient(session)

        initViews()
        setupListeners()

        if (session.isLoggedIn || session.isPaired) {
            switchTab("dashboard")
            refreshAllData()
        } else {
            showAuthView()
        }
    }

    private fun initViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        btnMenuToggle = findViewById(R.id.btnMenuToggle)
        txtTopbarTitle = findViewById(R.id.txtTopbarTitle)
        txtTopbarProjectPill = findViewById(R.id.txtTopbarProjectPill)
        btnQuickNewApk = findViewById(R.id.btnQuickNewApk)
        btnQuickSaveFile = findViewById(R.id.btnQuickSaveFile)
        btnQuickFind = findViewById(R.id.btnQuickFind)
        btnQuickLogout = findViewById(R.id.btnQuickLogout)
        globalProgressBar = findViewById(R.id.globalProgressBar)

        // Nav
        navDashboard = findViewById(R.id.navDashboard)
        navProjects = findViewById(R.id.navProjects)
        navEditor = findViewById(R.id.navEditor)
        navSearch = findViewById(R.id.navSearch)
        navStrings = findViewById(R.id.navStrings)
        navKeystores = findViewById(R.id.navKeystores)
        navBuild = findViewById(R.id.navBuild)
        navBlog = findViewById(R.id.navBlog)
        navFaqs = findViewById(R.id.navFaqs)
        navContact = findViewById(R.id.navContact)
        navAdminSectionTitle = findViewById(R.id.navAdminSectionTitle)
        navAdmin = findViewById(R.id.navAdmin)
        txtSidebarUserAvatar = findViewById(R.id.txtSidebarUserAvatar)
        txtSidebarUsername = findViewById(R.id.txtSidebarUsername)
        btnSidebarLogout = findViewById(R.id.btnSidebarLogout)

        // Views
        viewAuth = findViewById(R.id.viewAuth)
        viewDashboard = findViewById(R.id.viewDashboard)
        viewProjects = findViewById(R.id.viewProjects)
        viewEditor = findViewById(R.id.viewEditor)
        viewSearch = findViewById(R.id.viewSearch)
        viewStrings = findViewById(R.id.viewStrings)
        viewKeystores = findViewById(R.id.viewKeystores)
        viewBuild = findViewById(R.id.viewBuild)
        viewBlog = findViewById(R.id.viewBlog)
        viewFaqs = findViewById(R.id.viewFaqs)
        viewContact = findViewById(R.id.viewContact)
        viewAdmin = findViewById(R.id.viewAdmin)

        // Dashboard
        txtStatDecompiles = findViewById(R.id.txtStatDecompiles)
        txtStatCompiles = findViewById(R.id.txtStatCompiles)
        txtStatKeystores = findViewById(R.id.txtStatKeystores)
        txtStatSignings = findViewById(R.id.txtStatSignings)
        cardActiveProjectBanner = findViewById(R.id.cardActiveProjectBanner)
        txtActiveProjectTitle = findViewById(R.id.txtActiveProjectTitle)
        txtActiveProjectSubtitle = findViewById(R.id.txtActiveProjectSubtitle)
        btnDashOpenEditor = findViewById(R.id.btnDashOpenEditor)
        btnDashBuildApk = findViewById(R.id.btnDashBuildApk)
        btnViewAllProjects = findViewById(R.id.btnViewAllProjects)
        layoutDashProjectsList = findViewById(R.id.layoutDashProjectsList)

        // Projects
        btnUploadNewApkProject = findViewById(R.id.btnUploadNewApkProject)
        layoutProjectsList = findViewById(R.id.layoutProjectsList)

        // Editor
        btnEditorGoUp = findViewById(R.id.btnEditorGoUp)
        txtEditorBreadcrumb = findViewById(R.id.txtEditorBreadcrumb)
        btnEditorSaveFile = findViewById(R.id.btnEditorSaveFile)
        layoutEditorItems = findViewById(R.id.layoutEditorItems)
        editCodeContent = findViewById(R.id.editCodeContent)

        // Search
        editSearchFindQuery = findViewById(R.id.editSearchFindQuery)
        editSearchReplaceQuery = findViewById(R.id.editSearchReplaceQuery)
        btnExecuteSearch = findViewById(R.id.btnExecuteSearch)
        btnExecuteReplace = findViewById(R.id.btnExecuteReplace)
        txtSearchResultsHeader = findViewById(R.id.txtSearchResultsHeader)
        layoutSearchResultsList = findViewById(R.id.layoutSearchResultsList)

        // Strings
        editAppNameInput = findViewById(R.id.editAppNameInput)
        btnSaveAppName = findViewById(R.id.btnSaveAppName)
        layoutStringsList = findViewById(R.id.layoutStringsList)

        // Keystores
        btnOpenCreateKeystoreDialog = findViewById(R.id.btnOpenCreateKeystoreDialog)
        layoutKeystoresList = findViewById(R.id.layoutKeystoresList)

        // Build
        btnStartBuildAndSign = findViewById(R.id.btnStartBuildAndSign)
        layoutBuildOutput = findViewById(R.id.layoutBuildOutput)
        btnDownloadSignedApk = findViewById(R.id.btnDownloadSignedApk)

        // Blog / Faqs / Contact
        layoutBlogsList = findViewById(R.id.layoutBlogsList)
        layoutFaqsList = findViewById(R.id.layoutFaqsList)
        editContactName = findViewById(R.id.editContactName)
        editContactEmail = findViewById(R.id.editContactEmail)
        editContactSubject = findViewById(R.id.editContactSubject)
        editContactMessage = findViewById(R.id.editContactMessage)
        btnSubmitContact = findViewById(R.id.btnSubmitContact)

        // Admin
        btnAdminCreateUser = findViewById(R.id.btnAdminCreateUser)
        layoutAdminUsersList = findViewById(R.id.layoutAdminUsersList)

        // Auth
        btnScanQr = findViewById(R.id.btnScanQr)
        btnOpenLoginDialog = findViewById(R.id.btnOpenLoginDialog)
        editPairingKeyInput = findViewById(R.id.editPairingKeyInput)
        btnConnectPairingKey = findViewById(R.id.btnConnectPairingKey)
    }

    private fun setupListeners() {
        btnMenuToggle.setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }

        btnQuickNewApk.setOnClickListener { switchTab("projects") }
        btnQuickSaveFile.setOnClickListener { saveActiveEditorFile() }
        btnQuickFind.setOnClickListener { switchTab("search") }
        btnQuickLogout.setOnClickListener { handleLogout() }
        btnSidebarLogout.setOnClickListener { handleLogout() }

        navDashboard.setOnClickListener { switchTab("dashboard") }
        navProjects.setOnClickListener { switchTab("projects") }
        navEditor.setOnClickListener { switchTab("editor") }
        navSearch.setOnClickListener { switchTab("search") }
        navStrings.setOnClickListener { switchTab("strings") }
        navKeystores.setOnClickListener { switchTab("keystores") }
        navBuild.setOnClickListener { switchTab("build") }
        navBlog.setOnClickListener { switchTab("blog") }
        navFaqs.setOnClickListener { switchTab("faqs") }
        navContact.setOnClickListener { switchTab("contact") }
        navAdmin.setOnClickListener { switchTab("admin") }

        btnDashOpenEditor.setOnClickListener { switchTab("editor") }
        btnDashBuildApk.setOnClickListener { switchTab("build") }
        btnViewAllProjects.setOnClickListener { switchTab("projects") }

        btnEditorGoUp.setOnClickListener { goUpDirectory() }
        btnEditorSaveFile.setOnClickListener { saveActiveEditorFile() }

        btnExecuteSearch.setOnClickListener { executeSearch() }
        btnExecuteReplace.setOnClickListener { executeReplace() }

        btnSaveAppName.setOnClickListener { saveAppNameAndStrings() }
        btnOpenCreateKeystoreDialog.setOnClickListener { showCreateKeystoreDialog() }
        btnStartBuildAndSign.setOnClickListener { buildAndSignApk() }
        btnDownloadSignedApk.setOnClickListener { installDownloadedApk() }

        btnSubmitContact.setOnClickListener { submitContactInquiry() }

        btnScanQr.setOnClickListener { launchQrScanner() }
        btnOpenLoginDialog.setOnClickListener { showAuthDialog() }
        btnConnectPairingKey.setOnClickListener {
            val key = editPairingKeyInput.text.toString().trim()
            if (key.isNotEmpty()) handlePairingToken(key)
            else toast("Please enter a pairing key.")
        }
    }

    // -------------------------------------------------------------
    // Tab Navigation
    // -------------------------------------------------------------

    private fun switchTab(tab: String) {
        currentActiveTab = tab
        drawerLayout.closeDrawer(GravityCompat.START)

        viewAuth.visibility = View.GONE
        viewDashboard.visibility = View.GONE
        viewProjects.visibility = View.GONE
        viewEditor.visibility = View.GONE
        viewSearch.visibility = View.GONE
        viewStrings.visibility = View.GONE
        viewKeystores.visibility = View.GONE
        viewBuild.visibility = View.GONE
        viewBlog.visibility = View.GONE
        viewFaqs.visibility = View.GONE
        viewContact.visibility = View.GONE
        viewAdmin.visibility = View.GONE

        btnQuickSaveFile.visibility = if (tab == "editor") View.VISIBLE else View.GONE

        when (tab) {
            "dashboard" -> {
                viewDashboard.visibility = View.VISIBLE
                txtTopbarTitle.text = "Dashboard"
                loadDashboardData()
            }
            "projects" -> {
                viewProjects.visibility = View.VISIBLE
                txtTopbarTitle.text = "My Projects"
                loadProjectsList()
            }
            "editor" -> {
                viewEditor.visibility = View.VISIBLE
                txtTopbarTitle.text = "Code & File Editor"
                loadEditorDirectory(currentEditorDir)
            }
            "search" -> {
                viewSearch.visibility = View.VISIBLE
                txtTopbarTitle.text = "Find & Replace"
            }
            "strings" -> {
                viewStrings.visibility = View.VISIBLE
                txtTopbarTitle.text = "App Name & Strings"
                loadStringsList()
            }
            "keystores" -> {
                viewKeystores.visibility = View.VISIBLE
                txtTopbarTitle.text = "Keystores & Signing"
                loadKeystoresList()
            }
            "build" -> {
                viewBuild.visibility = View.VISIBLE
                txtTopbarTitle.text = "Recompile & Build"
            }
            "blog" -> {
                viewBlog.visibility = View.VISIBLE
                txtTopbarTitle.text = "Tutorials & Blog"
                loadBlogsList()
            }
            "faqs" -> {
                viewFaqs.visibility = View.VISIBLE
                txtTopbarTitle.text = "FAQs & Help"
                loadFaqsList()
            }
            "contact" -> {
                viewContact.visibility = View.VISIBLE
                txtTopbarTitle.text = "Contact Support"
            }
            "admin" -> {
                viewAdmin.visibility = View.VISIBLE
                txtTopbarTitle.text = "User Management"
                loadAdminUsersList()
            }
        }
    }

    private fun showAuthView() {
        viewDashboard.visibility = View.GONE
        viewProjects.visibility = View.GONE
        viewEditor.visibility = View.GONE
        viewSearch.visibility = View.GONE
        viewStrings.visibility = View.GONE
        viewKeystores.visibility = View.GONE
        viewBuild.visibility = View.GONE
        viewBlog.visibility = View.GONE
        viewFaqs.visibility = View.GONE
        viewContact.visibility = View.GONE
        viewAdmin.visibility = View.GONE

        viewAuth.visibility = View.VISIBLE
        txtTopbarTitle.text = "APK Tool Studio"
        txtTopbarProjectPill.text = "Sign in to connect"
    }

    // -------------------------------------------------------------
    // Data Loading & Handlers
    // -------------------------------------------------------------

    private fun refreshAllData() {
        val user = session.currentUser
        val username = user?.username ?: session.projectName ?: "Developer"
        txtSidebarUsername.text = username
        txtSidebarUserAvatar.text = username.take(1).uppercase()

        val isAdmin = user?.isAdmin == true
        navAdminSectionTitle.visibility = if (isAdmin) View.VISIBLE else View.GONE
        navAdmin.visibility = if (isAdmin) View.VISIBLE else View.GONE

        api.getWorkflowState(object : ApiClient.ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                val state = result.optJSONObject("state")
                if (state != null) {
                    activeProjectName = state.optString("project_name", null)
                    updateProjectHeader()
                }
            }
            override fun onError(errorMessage: String) {}
        })

        loadDashboardData()
    }

    private fun updateProjectHeader() {
        val name = activeProjectName
        if (!name.isNullOrEmpty()) {
            txtTopbarProjectPill.text = "📦 $name"
            cardActiveProjectBanner.visibility = View.VISIBLE
            txtActiveProjectTitle.text = name
        } else {
            txtTopbarProjectPill.text = "No Project Open"
            cardActiveProjectBanner.visibility = View.GONE
        }
    }

    private fun loadDashboardData() {
        api.getLimits(object : ApiClient.ApiCallback<UserLimits> {
            override fun onSuccess(result: UserLimits) {
                txtStatDecompiles.text = "${result.decompileUsage} / ${result.decompileLimit}"
                txtStatCompiles.text = "${result.compileUsage} / ${result.compileLimit}"
                txtStatKeystores.text = "${result.generateKeyUsage} / ${result.generateKeyLimit}"
                txtStatSignings.text = "${result.signApkUsage} / ${result.signApkLimit}"
            }
            override fun onError(errorMessage: String) {}
        })

        api.getProjects(object : ApiClient.ApiCallback<List<ProjectItem>> {
            override fun onSuccess(result: List<ProjectItem>) {
                layoutDashProjectsList.removeAllViews()
                result.take(4).forEach { proj ->
                    val row = createProjectRowView(proj)
                    layoutDashProjectsList.addView(row)
                }
            }
            override fun onError(errorMessage: String) {}
        })
    }

    private fun loadProjectsList() {
        setBusy(true)
        api.getProjects(object : ApiClient.ApiCallback<List<ProjectItem>> {
            override fun onSuccess(result: List<ProjectItem>) {
                setBusy(false)
                layoutProjectsList.removeAllViews()
                if (result.isEmpty()) {
                    val empty = TextView(this@MainActivity)
                    empty.text = "No decompiled projects found. Upload an APK to get started."
                    empty.setPadding(16, 32, 16, 32)
                    empty.setTextColor(getColor(R.color.text_muted))
                    layoutProjectsList.addView(empty)
                } else {
                    result.forEach { proj ->
                        val row = createProjectRowView(proj)
                        layoutProjectsList.addView(row)
                    }
                }
            }
            override fun onError(errorMessage: String) {
                setBusy(false)
                toast(errorMessage)
            }
        })
    }

    private fun createProjectRowView(proj: ProjectItem): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(16, 16, 16, 16)
            setBackgroundResource(R.drawable.bg_dashboard_card)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, 12)
            layoutParams = params
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        val infoLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val title = TextView(this).apply {
            text = proj.projectName
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(getColor(R.color.text_primary))
        }

        val subtitle = TextView(this).apply {
            val dateStr = proj.updatedAt.take(10)
            text = "Modified: $dateStr"
            textSize = 11f
            setTextColor(getColor(R.color.text_muted))
        }

        infoLayout.addView(title)
        infoLayout.addView(subtitle)

        val btnOpen = Button(this).apply {
            text = "Open Studio"
            textSize = 11f
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(getColor(R.color.primary))
            setOnClickListener { openProjectInStudio(proj.projectId, proj.projectName) }
        }

        card.addView(infoLayout)
        card.addView(btnOpen)
        return card
    }

    private fun openProjectInStudio(projectId: String, projectName: String) {
        setBusy(true)
        api.switchProject(projectId, object : ApiClient.ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                setBusy(false)
                activeProjectName = projectName
                updateProjectHeader()
                toast("Opened \"$projectName\" in Studio")
                switchTab("editor")
            }
            override fun onError(errorMessage: String) {
                setBusy(false)
                toast("Failed to open project: $errorMessage")
            }
        })
    }

    // -------------------------------------------------------------
    // Code & File Editor
    // -------------------------------------------------------------

    private fun loadEditorDirectory(dir: String) {
        setBusy(true)
        currentEditorDir = dir
        txtEditorBreadcrumb.text = if (dir.isEmpty()) "Root /" else "/ $dir"

        api.getDirectory(dir, object : ApiClient.ApiCallback<Pair<String, List<ProjectFile>>> {
            override fun onSuccess(result: Pair<String, List<ProjectFile>>) {
                setBusy(false)
                layoutEditorItems.removeAllViews()
                val files = result.second

                if (files.isEmpty()) {
                    val empty = TextView(this@MainActivity)
                    empty.text = "Empty folder"
                    empty.textSize = 12f
                    empty.setTextColor(getColor(R.color.text_muted))
                    layoutEditorItems.addView(empty)
                } else {
                    files.forEach { file ->
                        val item = TextView(this@MainActivity).apply {
                            text = (if (file.isDir) "📁  " else "📄  ") + file.name
                            textSize = 12f
                            setTextColor(getColor(if (file.isDir) R.color.primary else R.color.text_primary))
                            setPadding(8, 8, 8, 8)
                            setOnClickListener {
                                if (file.isDir) {
                                    loadEditorDirectory(file.path)
                                } else {
                                    openFileInEditor(file.path)
                                }
                            }
                        }
                        layoutEditorItems.addView(item)
                    }
                }
            }
            override fun onError(errorMessage: String) {
                setBusy(false)
                toast("Error loading folder: $errorMessage")
            }
        })
    }

    private fun goUpDirectory() {
        if (currentEditorDir.isEmpty()) return
        val parts = currentEditorDir.split("/").toMutableList()
        parts.removeAt(parts.size - 1)
        loadEditorDirectory(parts.joinToString("/"))
    }

    private fun openFileInEditor(filePath: String) {
        setBusy(true)
        api.openEditorFile(filePath, 0L, object : ApiClient.ApiCallback<EditorFile> {
            override fun onSuccess(result: EditorFile) {
                setBusy(false)
                currentEditorFilePath = result.path
                editCodeContent.setText(result.content)
                txtTopbarTitle.text = "Editing: ${result.path.substringAfterLast('/')}"
                toast("Loaded ${result.path}")
            }
            override fun onError(errorMessage: String) {
                setBusy(false)
                toast("Failed to open file: $errorMessage")
            }
        })
    }

    private fun saveActiveEditorFile() {
        val path = currentEditorFilePath
        if (path == null) {
            toast("No file open in editor to save.")
            return
        }
        val content = editCodeContent.text.toString()
        setBusy(true)
        api.saveEditorFile(path, content, object : ApiClient.ApiCallback<String> {
            override fun onSuccess(result: String) {
                setBusy(false)
                toast("Saved $path successfully.")
            }
            override fun onError(errorMessage: String) {
                setBusy(false)
                toast("Failed to save: $errorMessage")
            }
        })
    }

    // -------------------------------------------------------------
    // Find & Replace
    // -------------------------------------------------------------

    private fun executeSearch() {
        val query = editSearchFindQuery.text.toString().trim()
        if (query.isEmpty()) {
            toast("Please enter a query to search.")
            return
        }
        setBusy(true)
        api.findInProject(query, object : ApiClient.ApiCallback<FindResult> {
            override fun onSuccess(result: FindResult) {
                setBusy(false)
                layoutSearchResultsList.removeAllViews()
                txtSearchResultsHeader.text = "Matching Locations (${result.files.size})"

                if (result.files.isEmpty()) {
                    val empty = TextView(this@MainActivity)
                    empty.text = "No occurrences found for \"$query\""
                    empty.setPadding(8, 16, 8, 16)
                    empty.setTextColor(getColor(R.color.text_muted))
                    layoutSearchResultsList.addView(empty)
                } else {
                    result.files.forEach { match: FindMatch ->
                        val card = LinearLayout(this@MainActivity).apply {
                            orientation = LinearLayout.VERTICAL
                            setPadding(12, 12, 12, 12)
                            setBackgroundResource(R.drawable.bg_dashboard_card)
                            val p = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                            p.setMargins(0, 0, 0, 8)
                            layoutParams = p
                            setOnClickListener {
                                switchTab("editor")
                                openFileInEditor(match.path)
                            }
                        }

                        val pathText = TextView(this@MainActivity).apply {
                            text = "📄 ${match.path}"
                            textSize = 13f
                            setTypeface(null, android.graphics.Typeface.BOLD)
                            setTextColor(getColor(R.color.primary))
                        }

                        val snippet = TextView(this@MainActivity).apply {
                            text = match.snippet
                            textSize = 11f
                            setTextColor(getColor(R.color.text_secondary))
                            setPadding(0, 4, 0, 0)
                        }

                        card.addView(pathText)
                        card.addView(snippet)
                        layoutSearchResultsList.addView(card)
                    }
                }
            }
            override fun onError(errorMessage: String) {
                setBusy(false)
                toast(errorMessage)
            }
        })
    }

    private fun executeReplace() {
        val find = editSearchFindQuery.text.toString().trim()
        val replace = editSearchReplaceQuery.text.toString()
        if (find.isEmpty()) {
            toast("Enter text to find first.")
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Confirm Replace All")
            .setMessage("Replace all occurrences of \"$find\" with \"$replace\" across the project?")
            .setPositiveButton("Replace") { _, _ ->
                setBusy(true)
                api.findAndReplace(find, replace, object : ApiClient.ApiCallback<FindReplaceResult> {
                    override fun onSuccess(result: FindReplaceResult) {
                        setBusy(false)
                        toast("Replaced ${result.replacements} occurrences across ${result.filesChanged} files.")
                        executeSearch()
                    }
                    override fun onError(errorMessage: String) {
                        setBusy(false)
                        toast(errorMessage)
                    }
                })
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // -------------------------------------------------------------
    // App Name & Strings
    // -------------------------------------------------------------

    private fun loadStringsList() {
        setBusy(true)
        api.loadStrings("values", object : ApiClient.ApiCallback<StringData> {
            override fun onSuccess(result: StringData) {
                setBusy(false)
                editAppNameInput.setText(result.appName)
                stringMap.clear()
                layoutStringsList.removeAllViews()

                result.allStrings.forEach { item: StringItem ->
                    stringMap[item.name] = item.value

                    val row = LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        setPadding(8, 8, 8, 8)
                        gravity = android.view.Gravity.CENTER_VERTICAL
                    }

                    val keyLabel = TextView(this@MainActivity).apply {
                        text = item.name
                        textSize = 12f
                        setTypeface(null, android.graphics.Typeface.BOLD)
                        setTextColor(getColor(R.color.text_primary))
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.4f)
                    }

                    val valInput = EditText(this@MainActivity).apply {
                        setText(item.value)
                        textSize = 12f
                        setBackgroundResource(R.drawable.bg_input_field)
                        setPadding(8, 6, 8, 6)
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.6f)
                        setOnFocusChangeListener { _, hasFocus ->
                            if (!hasFocus) stringMap[item.name] = text.toString()
                        }
                    }

                    row.addView(keyLabel)
                    row.addView(valInput)
                    layoutStringsList.addView(row)
                }
            }
            override fun onError(errorMessage: String) {
                setBusy(false)
                toast(errorMessage)
            }
        })
    }

    private fun saveAppNameAndStrings() {
        val appName = editAppNameInput.text.toString().trim()
        setBusy(true)
        api.saveStrings("values", appName, stringMap, object : ApiClient.ApiCallback<String> {
            override fun onSuccess(result: String) {
                setBusy(false)
                toast("App Name & Strings saved successfully.")
            }
            override fun onError(errorMessage: String) {
                setBusy(false)
                toast(errorMessage)
            }
        })
    }

    // -------------------------------------------------------------
    // Keystores
    // -------------------------------------------------------------

    private fun loadKeystoresList() {
        setBusy(true)
        api.getKeystores(object : ApiClient.ApiCallback<List<KeystoreItem>> {
            override fun onSuccess(result: List<KeystoreItem>) {
                setBusy(false)
                layoutKeystoresList.removeAllViews()
                if (result.isEmpty()) {
                    val empty = TextView(this@MainActivity)
                    empty.text = "No keystores created yet. Click '+ New Key' to generate one."
                    empty.setPadding(16, 24, 16, 24)
                    empty.setTextColor(getColor(R.color.text_muted))
                    layoutKeystoresList.addView(empty)
                } else {
                    result.forEach { ks ->
                        val card = LinearLayout(this@MainActivity).apply {
                            orientation = LinearLayout.HORIZONTAL
                            setPadding(14, 14, 14, 14)
                            setBackgroundResource(R.drawable.bg_dashboard_card)
                            val p = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                            p.setMargins(0, 0, 0, 10)
                            layoutParams = p
                            gravity = android.view.Gravity.CENTER_VERTICAL
                        }

                        val info = LinearLayout(this@MainActivity).apply {
                            orientation = LinearLayout.VERTICAL
                            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                        }

                        val alias = TextView(this@MainActivity).apply {
                            text = "🔑 ${ks.keyAlias}"
                            textSize = 14f
                            setTypeface(null, android.graphics.Typeface.BOLD)
                            setTextColor(getColor(R.color.text_primary))
                        }

                        val date = TextView(this@MainActivity).apply {
                            val dt = ks.createdAt.take(10)
                            text = "Created: $dt"
                            textSize = 11f
                            setTextColor(getColor(R.color.text_muted))
                        }

                        info.addView(alias)
                        info.addView(date)

                        val btnDownload = Button(this@MainActivity).apply {
                            text = "Download"
                            textSize = 11f
                            setBackgroundColor(getColor(R.color.primary))
                            setTextColor(0xFFFFFFFF.toInt())
                            setOnClickListener {
                                val url = session.serverUrl + "?download=" + Uri.encode(ks.fileName)
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                startActivity(intent)
                            }
                        }

                        card.addView(info)
                        card.addView(btnDownload)
                        layoutKeystoresList.addView(card)
                    }
                }
            }
            override fun onError(errorMessage: String) {
                setBusy(false)
                toast(errorMessage)
            }
        })
    }

    private fun showCreateKeystoreDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_create_keystore, null)
        val editAlias = view.findViewById<EditText>(R.id.editKeystoreAlias)
        val editPass = view.findViewById<EditText>(R.id.editKeystorePassword)

        AlertDialog.Builder(this)
            .setTitle("Generate RSA 2048 Keystore")
            .setView(view)
            .setPositiveButton("Generate") { _, _ ->
                val alias = editAlias.text.toString().trim()
                val pass = editPass.text.toString().trim()
                if (alias.isNotEmpty() && pass.isNotEmpty()) {
                    setBusy(true)
                    api.createKeystore(alias, pass, object : ApiClient.ApiCallback<String> {
                        override fun onSuccess(result: String) {
                            setBusy(false)
                            toast("Keystore generated successfully.")
                            loadKeystoresList()
                        }
                        override fun onError(errorMessage: String) {
                            setBusy(false)
                            toast("Keystore error: $errorMessage")
                        }
                    })
                } else {
                    toast("Alias and password are required.")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // -------------------------------------------------------------
    // Build & Recompile APK
    // -------------------------------------------------------------

    private fun buildAndSignApk() {
        setBusy(true)
        api.buildApk("", "", object : ApiClient.ApiCallback<BuildResult> {
            override fun onSuccess(result: BuildResult) {
                setBusy(false)
                if (result.success && !result.signedApk.isNullOrEmpty()) {
                    lastSignedApkUrl = session.serverUrl + "?download=" + Uri.encode(result.signedApk)
                    layoutBuildOutput.visibility = View.VISIBLE
                    toast("APK Recompiled & Signed Successfully!")
                } else {
                    toast(result.message ?: "Build complete.")
                }
            }
            override fun onError(errorMessage: String) {
                setBusy(false)
                toast("Build failed: $errorMessage")
            }
        })
    }

    private fun installDownloadedApk() {
        val urlStr = lastSignedApkUrl ?: return
        val destFile = File(cacheDir, "recompiled_signed.apk")
        setBusy(true)
        api.downloadApk(urlStr, destFile, object : ApiClient.ProgressCallback {
            override fun onProgress(percentage: Int, message: String) {}
        }, object : ApiClient.ApiCallback<File> {
            override fun onSuccess(result: File) {
                setBusy(false)
                promptInstallApk(result)
            }
            override fun onError(errorMessage: String) {
                setBusy(false)
                toast("Download failed: $errorMessage")
            }
        })
    }

    private fun promptInstallApk(file: File) {
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    // -------------------------------------------------------------
    // Blogs & FAQs
    // -------------------------------------------------------------

    private fun loadBlogsList() {
        setBusy(true)
        api.getPublicBlogs(object : ApiClient.ApiCallback<List<BlogItem>> {
            override fun onSuccess(result: List<BlogItem>) {
                setBusy(false)
                layoutBlogsList.removeAllViews()
                result.forEach { b: BlogItem ->
                    val card = LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(14, 14, 14, 14)
                        setBackgroundResource(R.drawable.bg_dashboard_card)
                        val p = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        p.setMargins(0, 0, 0, 10)
                        layoutParams = p
                    }

                    val cat = TextView(this@MainActivity).apply {
                        text = b.category.uppercase()
                        textSize = 10f
                        setTypeface(null, android.graphics.Typeface.BOLD)
                        setTextColor(getColor(R.color.primary))
                    }

                    val title = TextView(this@MainActivity).apply {
                        text = b.title
                        textSize = 14f
                        setTypeface(null, android.graphics.Typeface.BOLD)
                        setTextColor(getColor(R.color.text_primary))
                        setPadding(0, 2, 0, 4)
                    }

                    val excerpt = TextView(this@MainActivity).apply {
                        text = b.excerpt
                        textSize = 12f
                        setTextColor(getColor(R.color.text_secondary))
                    }

                    card.addView(cat)
                    card.addView(title)
                    card.addView(excerpt)
                    layoutBlogsList.addView(card)
                }
            }
            override fun onError(errorMessage: String) {
                setBusy(false)
                toast(errorMessage)
            }
        })
    }

    private fun loadFaqsList() {
        setBusy(true)
        api.getFaqs(object : ApiClient.ApiCallback<List<FaqItem>> {
            override fun onSuccess(result: List<FaqItem>) {
                setBusy(false)
                layoutFaqsList.removeAllViews()
                result.forEach { f ->
                    val card = LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(14, 14, 14, 14)
                        setBackgroundResource(R.drawable.bg_dashboard_card)
                        val p = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        p.setMargins(0, 0, 0, 10)
                        layoutParams = p
                    }

                    val q = TextView(this@MainActivity).apply {
                        text = "❓  ${f.question}"
                        textSize = 13f
                        setTypeface(null, android.graphics.Typeface.BOLD)
                        setTextColor(getColor(R.color.text_primary))
                    }

                    val a = TextView(this@MainActivity).apply {
                        text = f.answer
                        textSize = 12f
                        setTextColor(getColor(R.color.text_secondary))
                        setPadding(0, 6, 0, 0)
                    }

                    card.addView(q)
                    card.addView(a)
                    layoutFaqsList.addView(card)
                }
            }
            override fun onError(errorMessage: String) {
                setBusy(false)
                toast(errorMessage)
            }
        })
    }

    // -------------------------------------------------------------
    // Contact Support
    // -------------------------------------------------------------

    private fun submitContactInquiry() {
        val name = editContactName.text.toString().trim()
        val email = editContactEmail.text.toString().trim()
        val subject = editContactSubject.text.toString().trim()
        val message = editContactMessage.text.toString().trim()

        if (name.isEmpty() || email.isEmpty() || subject.isEmpty() || message.isEmpty()) {
            toast("Please fill in all fields.")
            return
        }

        setBusy(true)
        api.submitContactInquiry(name, email, subject, message, object : ApiClient.ApiCallback<String> {
            override fun onSuccess(result: String) {
                setBusy(false)
                toast("Thank you! Inquiry submitted successfully.")
                editContactSubject.setText("")
                editContactMessage.setText("")
            }
            override fun onError(errorMessage: String) {
                setBusy(false)
                toast(errorMessage)
            }
        })
    }

    // -------------------------------------------------------------
    // Admin Panel
    // -------------------------------------------------------------

    private fun loadAdminUsersList() {
        setBusy(true)
        api.getUsers(object : ApiClient.ApiCallback<List<User>> {
            override fun onSuccess(result: List<User>) {
                setBusy(false)
                layoutAdminUsersList.removeAllViews()
                result.forEach { u ->
                    val card = LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(14, 14, 14, 14)
                        setBackgroundResource(R.drawable.bg_dashboard_card)
                        val p = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        p.setMargins(0, 0, 0, 10)
                        layoutParams = p
                    }

                    val name = TextView(this@MainActivity).apply {
                        text = "👤 ${u.username} (${u.userType})"
                        textSize = 14f
                        setTypeface(null, android.graphics.Typeface.BOLD)
                        setTextColor(getColor(if (u.userType == "admin") R.color.danger else R.color.text_primary))
                    }

                    val limits = TextView(this@MainActivity).apply {
                        val decU = u.decompileUsage
                        val decL = u.decompileLimit
                        val comU = u.compileUsage
                        val comL = u.compileLimit
                        text = "Dec: $decU/$decL | Com: $comU/$comL"
                        textSize = 11f
                        setTextColor(getColor(R.color.text_secondary))
                        setPadding(0, 4, 0, 0)
                    }

                    card.addView(name)
                    card.addView(limits)
                    layoutAdminUsersList.addView(card)
                }
            }
            override fun onError(errorMessage: String) {
                setBusy(false)
                toast(errorMessage)
            }
        })
    }

    // -------------------------------------------------------------
    // Auth & Pairing
    // -------------------------------------------------------------

    private fun launchQrScanner() {
        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt("Scan the Studio QR code")
            setBeepEnabled(false)
            setOrientationLocked(true)
        }
        qrScanLauncher.launch(options)
    }

    private fun handlePairingToken(tokenStr: String) {
        try {
            val decoded = String(Base64.decode(tokenStr, Base64.DEFAULT))
            val json = JSONObject(decoded)
            val url = json.getString("url")
            val tok = json.getString("token")
            val name = json.optString("name", "Studio Project")

            session.savePairing(url, tok, name)
            toast("Connected to \"$name\"")
            switchTab("dashboard")
            refreshAllData()
        } catch (e: Exception) {
            toast("Invalid pairing code.")
        }
    }

    private fun showAuthDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_auth, null)
        val editUser = view.findViewById<EditText>(R.id.editAuthUsername)
        val editPass = view.findViewById<EditText>(R.id.editAuthPassword)

        AlertDialog.Builder(this)
            .setTitle("Account Login")
            .setView(view)
            .setPositiveButton("Sign In") { _, _ ->
                val user = editUser.text.toString().trim()
                val pass = editPass.text.toString().trim()
                if (user.isNotEmpty() && pass.isNotEmpty()) {
                    setBusy(true)
                    api.login(user, pass, object : ApiClient.ApiCallback<User> {
                        override fun onSuccess(result: User) {
                            setBusy(false)
                            session.saveUser(result)
                            toast("Welcome back, ${result.username}!")
                            switchTab("dashboard")
                            refreshAllData()
                        }
                        override fun onError(errorMessage: String) {
                            setBusy(false)
                            toast(errorMessage)
                        }
                    })
                } else {
                    toast("Username and password are required.")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun handleLogout() {
        session.logout()
        activeProjectName = null
        showAuthView()
        toast("Signed out successfully.")
    }

    // -------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------

    private fun setBusy(busy: Boolean) {
        globalProgressBar.visibility = if (busy) View.VISIBLE else View.GONE
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}

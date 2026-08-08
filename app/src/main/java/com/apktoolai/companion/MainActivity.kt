package com.apktoolai.companion

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.apktoolai.companion.api.*
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private lateinit var api: ApiClient
    private val mainHandler = Handler(Looper.getMainLooper())

    // Top Bar & Global Banners
    private lateinit var topBarSubtitle: TextView
    private lateinit var btnServerConfig: Button
    private lateinit var btnTopScanQr: Button
    private lateinit var btnTopAuth: Button
    private lateinit var txtActiveProjectPill: TextView
    private lateinit var txtAdminBadge: TextView
    private lateinit var globalUpdateBanner: LinearLayout
    private lateinit var btnBannerInstall: Button
    private lateinit var globalProgressBar: ProgressBar

    // Navigation Tabs
    private lateinit var tabBtnDashboard: Button
    private lateinit var tabBtnProjects: Button
    private lateinit var tabBtnEditor: Button
    private lateinit var tabBtnCustomizer: Button
    private lateinit var tabBtnBuild: Button
    private lateinit var tabBtnDebugger: Button
    private lateinit var tabBtnAiSettings: Button
    private lateinit var tabBtnAdmin: Button
    private lateinit var tabBtnCommunity: Button

    // Studio Panels
    private lateinit var panelDashboard: ScrollView
    private lateinit var panelProjects: ScrollView
    private lateinit var panelEditor: LinearLayout
    private lateinit var panelCustomizer: ScrollView
    private lateinit var panelBuild: ScrollView
    private lateinit var panelDebugger: ScrollView
    private lateinit var panelAiSettings: ScrollView
    private lateinit var panelAdmin: ScrollView
    private lateinit var panelCommunity: ScrollView

    // Dashboard UI
    private lateinit var txtDashWelcome: TextView
    private lateinit var txtDashUserEmail: TextView
    private lateinit var txtMeterDecompile: TextView
    private lateinit var txtMeterCompile: TextView
    private lateinit var txtMeterKeygen: TextView
    private lateinit var txtMeterSign: TextView
    private lateinit var btnQuickUploadDecompile: Button
    private lateinit var btnQuickBuildApk: Button
    private lateinit var btnQuickSignApk: Button
    private lateinit var btnQuickAiFix: Button
    private lateinit var cardActiveProjectOverview: LinearLayout
    private lateinit var txtDashProjectName: TextView
    private lateinit var txtDashProjectPath: TextView
    private lateinit var btnDashCloseProject: Button
    private lateinit var btnDashExploreFiles: Button
    private lateinit var btnDashEditStrings: Button
    private lateinit var btnDashRefreshProjects: Button
    private lateinit var dashProjectsListContainer: LinearLayout

    // Projects & Upload UI
    private lateinit var boxPickApkFile: LinearLayout
    private lateinit var txtSelectedApkName: TextView
    private lateinit var btnUploadDecompileSubmit: Button
    private lateinit var boxDecompileLogs: LinearLayout
    private lateinit var txtDecompileStatus: TextView
    private lateinit var txtDecompileLogOutput: TextView
    private lateinit var projectsListContainer: LinearLayout
    private var pendingUploadApkFile: File? = null

    // Editor & Hex UI
    private lateinit var txtFileBreadcrumb: TextView
    private lateinit var btnFileUpDir: Button
    private lateinit var btnRefreshDir: Button
    private lateinit var scrollDirectoryBrowser: ScrollView
    private lateinit var directoryItemsContainer: LinearLayout
    private lateinit var boxCodeEditorView: LinearLayout
    private lateinit var txtEditorOpenFileName: TextView
    private lateinit var btnEditorSave: Button
    private lateinit var btnEditorAiReview: Button
    private lateinit var btnEditorReplaceFile: Button
    private lateinit var btnEditorClose: Button
    private lateinit var editCodeContent: EditText
    private lateinit var boxHexEditorView: LinearLayout
    private lateinit var editHexSearchQuery: EditText
    private lateinit var btnHexSearch: Button
    private lateinit var btnHexClose: Button
    private lateinit var hexResultsContainer: LinearLayout
    private lateinit var editHexPatchBytes: EditText
    private lateinit var btnHexApplyPatch: Button
    private var currentDirPath = ""
    private var currentEditingFilePath = ""

    // Resources & AI Customizer UI
    private lateinit var editCustomAppName: EditText
    private lateinit var editStringsLocale: EditText
    private lateinit var btnLoadStrings: Button
    private lateinit var btnSaveStrings: Button
    private lateinit var stringsTableContainer: LinearLayout
    private val loadedStringsMap = HashMap<String, String>()
    private lateinit var btnUploadFirebaseJson: Button
    private lateinit var btnPickLogoImage: Button
    private lateinit var editAiIconPrompt: EditText
    private lateinit var btnGenerateAiIcon: Button
    private lateinit var editGlobalFindText: EditText
    private lateinit var editGlobalReplaceText: EditText
    private lateinit var btnGlobalFindOnly: Button
    private lateinit var btnGlobalFindReplace: Button

    // Build & Sign UI
    private lateinit var btnTriggerBuildApk: Button
    private lateinit var boxBuildLogs: LinearLayout
    private lateinit var txtBuildLogOutput: TextView
    private lateinit var btnOpenCreateKeystoreDialog: Button
    private lateinit var keystoresListContainer: LinearLayout
    private lateinit var editSignPassword: EditText
    private lateinit var btnTriggerSignApk: Button
    private lateinit var btnDirectInstallApk: Button

    // ADB & Debugger UI
    private lateinit var btnToggleCloudLogsStream: Button
    private lateinit var btnClearCloudLogs: Button
    private lateinit var txtCloudLogsStream: TextView
    private lateinit var editAdbHostIp: EditText
    private lateinit var btnAdbConnect: Button
    private lateinit var adbDevicesContainer: LinearLayout
    private lateinit var btnAdbReadLogcat: Button
    private lateinit var txtAdbLogcatOutput: TextView
    private var cloudLogsStreaming = false
    private var cloudLogsTimer: Runnable? = null
    private var autoCheckTimer: Runnable? = null

    // AI Settings UI
    private lateinit var rgAiProvider: RadioGroup
    private lateinit var rbProviderGemini: RadioButton
    private lateinit var rbProviderOpenAi: RadioButton
    private lateinit var txtGeminiKeyStatus: TextView
    private lateinit var editGeminiApiKey: EditText
    private lateinit var btnSaveGeminiKey: Button
    private lateinit var btnDeleteGeminiKey: Button
    private lateinit var txtOpenAiKeyStatus: TextView
    private lateinit var editOpenAiApiKey: EditText
    private lateinit var btnSaveOpenAiKey: Button
    private lateinit var btnDeleteOpenAiKey: Button
    private lateinit var editModelGeminiText: EditText
    private lateinit var editModelGeminiImage: EditText
    private lateinit var editModelOpenAiText: EditText
    private lateinit var editModelOpenAiImage: EditText
    private lateinit var btnSaveCustomModels: Button
    private lateinit var btnResetCustomModels: Button

    // Admin UI
    private lateinit var btnAdminCreateUserDialog: Button
    private lateinit var adminUsersContainer: LinearLayout
    private lateinit var adminInquiriesContainer: LinearLayout
    private lateinit var btnAdminCreateBlogDialog: Button
    private lateinit var adminBlogsContainer: LinearLayout
    private lateinit var btnAdminCreateFaqDialog: Button
    private lateinit var adminFaqsContainer: LinearLayout
    private lateinit var editBackupRepoOwner: EditText
    private lateinit var editBackupRepoName: EditText
    private lateinit var editBackupBranch: EditText
    private lateinit var editBackupToken: EditText
    private lateinit var btnSaveBackupSettings: Button
    private lateinit var btnRunManualBackup: Button

    // Community & Support UI
    private lateinit var editContactName: EditText
    private lateinit var editContactEmail: EditText
    private lateinit var editContactSubject: EditText
    private lateinit var editContactMessage: EditText
    private lateinit var btnSubmitContactInquiry: Button
    private lateinit var publicBlogsContainer: LinearLayout
    private lateinit var publicFaqsContainer: LinearLayout

    // Activity Result Launchers
    private val pickApkLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val file = copyUriToTemp(uri, "upload.apk")
            if (file != null) {
                pendingUploadApkFile = file
                txtSelectedApkName.text = "Selected: ${file.name} (${formatSize(file.length())})"
                toast("APK ready for upload.")
            }
        }
    }

    private val pickFirebaseLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val file = copyUriToTemp(uri, "google-services.json")
            if (file != null) {
                setBusy(true, "Injecting Firebase services...")
                api.applyFirebaseJson(file, object : ApiClient.ApiCallback<JSONObject> {
                    override fun onSuccess(result: JSONObject) {
                        setBusy(false, "")
                        toast(result.optString("message", "Firebase values applied."))
                    }

                    override fun onError(errorMessage: String) {
                        setBusy(false, "")
                        toast("Firebase error: $errorMessage")
                    }
                })
            }
        }
    }

    private val pickLogoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val file = copyUriToTemp(uri, "logo.png")
            if (file != null) {
                setBusy(true, "Uploading and scaling icons across all densities...")
                api.uploadLogo(file, object : ApiClient.ApiCallback<JSONObject> {
                    override fun onSuccess(result: JSONObject) {
                        setBusy(false, "")
                        toast(result.optString("message", "Icons updated successfully."))
                    }

                    override fun onError(errorMessage: String) {
                        setBusy(false, "")
                        toast("Logo error: $errorMessage")
                    }
                })
            }
        }
    }

    private val pickReplacementFileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null && currentEditingFilePath.isNotEmpty()) {
            val file = copyUriToTemp(uri, File(currentEditingFilePath).name)
            if (file != null) {
                setBusy(true, "Replacing target file on server...")
                api.replaceFile(currentEditingFilePath, file, object : ApiClient.ApiCallback<JSONObject> {
                    override fun onSuccess(result: JSONObject) {
                        setBusy(false, "")
                        toast("File replaced on server.")
                        openEditorForFile(currentEditingFilePath)
                    }

                    override fun onError(errorMessage: String) {
                        setBusy(false, "")
                        toast("Replace failed: $errorMessage")
                    }
                })
            }
        }
    }

    private val qrScanLauncher = registerForActivityResult(ScanContract()) { result ->
        val contents = result.contents
        if (!contents.isNullOrBlank()) {
            handleScannedCode(contents)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        session = SessionManager(this)
        api = ApiClient(session)

        bindViews()
        setupListeners()
        updateUiForAuthState()
        switchTab("dashboard")

        // Initial Data Load
        refreshDashboardData()
        loadPublicCommunityData()
    }

    override fun onResume() {
        super.onResume()
        startAutoCheckTimer()
        if (cloudLogsStreaming) {
            resumeCloudLogsPolling()
        }
    }

    override fun onPause() {
        super.onPause()
        stopAutoCheckTimer()
        pauseCloudLogsPolling()
    }

    // -------------------------------------------------------------
    // View Binding & Event Setup
    // -------------------------------------------------------------

    private fun bindViews() {
        topBarSubtitle = findViewById(R.id.topBarSubtitle)
        btnServerConfig = findViewById(R.id.btnServerConfig)
        btnTopScanQr = findViewById(R.id.btnTopScanQr)
        btnTopAuth = findViewById(R.id.btnTopAuth)
        txtActiveProjectPill = findViewById(R.id.txtActiveProjectPill)
        txtAdminBadge = findViewById(R.id.txtAdminBadge)
        globalUpdateBanner = findViewById(R.id.globalUpdateBanner)
        btnBannerInstall = findViewById(R.id.btnBannerInstall)
        globalProgressBar = findViewById(R.id.globalProgressBar)

        tabBtnDashboard = findViewById(R.id.tabBtnDashboard)
        tabBtnProjects = findViewById(R.id.tabBtnProjects)
        tabBtnEditor = findViewById(R.id.tabBtnEditor)
        tabBtnCustomizer = findViewById(R.id.tabBtnCustomizer)
        tabBtnBuild = findViewById(R.id.tabBtnBuild)
        tabBtnDebugger = findViewById(R.id.tabBtnDebugger)
        tabBtnAiSettings = findViewById(R.id.tabBtnAiSettings)
        tabBtnAdmin = findViewById(R.id.tabBtnAdmin)
        tabBtnCommunity = findViewById(R.id.tabBtnCommunity)

        panelDashboard = findViewById(R.id.panelDashboard)
        panelProjects = findViewById(R.id.panelProjects)
        panelEditor = findViewById(R.id.panelEditor)
        panelCustomizer = findViewById(R.id.panelCustomizer)
        panelBuild = findViewById(R.id.panelBuild)
        panelDebugger = findViewById(R.id.panelDebugger)
        panelAiSettings = findViewById(R.id.panelAiSettings)
        panelAdmin = findViewById(R.id.panelAdmin)
        panelCommunity = findViewById(R.id.panelCommunity)

        // Dashboard
        txtDashWelcome = findViewById(R.id.txtDashWelcome)
        txtDashUserEmail = findViewById(R.id.txtDashUserEmail)
        txtMeterDecompile = findViewById(R.id.txtMeterDecompile)
        txtMeterCompile = findViewById(R.id.txtMeterCompile)
        txtMeterKeygen = findViewById(R.id.txtMeterKeygen)
        txtMeterSign = findViewById(R.id.txtMeterSign)
        btnQuickUploadDecompile = findViewById(R.id.btnQuickUploadDecompile)
        btnQuickBuildApk = findViewById(R.id.btnQuickBuildApk)
        btnQuickSignApk = findViewById(R.id.btnQuickSignApk)
        btnQuickAiFix = findViewById(R.id.btnQuickAiFix)
        cardActiveProjectOverview = findViewById(R.id.cardActiveProjectOverview)
        txtDashProjectName = findViewById(R.id.txtDashProjectName)
        txtDashProjectPath = findViewById(R.id.txtDashProjectPath)
        btnDashCloseProject = findViewById(R.id.btnDashCloseProject)
        btnDashExploreFiles = findViewById(R.id.btnDashExploreFiles)
        btnDashEditStrings = findViewById(R.id.btnDashEditStrings)
        btnDashRefreshProjects = findViewById(R.id.btnDashRefreshProjects)
        dashProjectsListContainer = findViewById(R.id.dashProjectsListContainer)

        // Projects
        boxPickApkFile = findViewById(R.id.boxPickApkFile)
        txtSelectedApkName = findViewById(R.id.txtSelectedApkName)
        btnUploadDecompileSubmit = findViewById(R.id.btnUploadDecompileSubmit)
        boxDecompileLogs = findViewById(R.id.boxDecompileLogs)
        txtDecompileStatus = findViewById(R.id.txtDecompileStatus)
        txtDecompileLogOutput = findViewById(R.id.txtDecompileLogOutput)
        projectsListContainer = findViewById(R.id.projectsListContainer)

        // Editor
        txtFileBreadcrumb = findViewById(R.id.txtFileBreadcrumb)
        btnFileUpDir = findViewById(R.id.btnFileUpDir)
        btnRefreshDir = findViewById(R.id.btnRefreshDir)
        scrollDirectoryBrowser = findViewById(R.id.scrollDirectoryBrowser)
        directoryItemsContainer = findViewById(R.id.directoryItemsContainer)
        boxCodeEditorView = findViewById(R.id.boxCodeEditorView)
        txtEditorOpenFileName = findViewById(R.id.txtEditorOpenFileName)
        btnEditorSave = findViewById(R.id.btnEditorSave)
        btnEditorAiReview = findViewById(R.id.btnEditorAiReview)
        btnEditorReplaceFile = findViewById(R.id.btnEditorReplaceFile)
        btnEditorClose = findViewById(R.id.btnEditorClose)
        editCodeContent = findViewById(R.id.editCodeContent)
        boxHexEditorView = findViewById(R.id.boxHexEditorView)
        editHexSearchQuery = findViewById(R.id.editHexSearchQuery)
        btnHexSearch = findViewById(R.id.btnHexSearch)
        btnHexClose = findViewById(R.id.btnHexClose)
        hexResultsContainer = findViewById(R.id.hexResultsContainer)
        editHexPatchBytes = findViewById(R.id.editHexPatchBytes)
        btnHexApplyPatch = findViewById(R.id.btnHexApplyPatch)

        // Customizer
        editCustomAppName = findViewById(R.id.editCustomAppName)
        editStringsLocale = findViewById(R.id.editStringsLocale)
        btnLoadStrings = findViewById(R.id.btnLoadStrings)
        btnSaveStrings = findViewById(R.id.btnSaveStrings)
        stringsTableContainer = findViewById(R.id.stringsTableContainer)
        btnUploadFirebaseJson = findViewById(R.id.btnUploadFirebaseJson)
        btnPickLogoImage = findViewById(R.id.btnPickLogoImage)
        editAiIconPrompt = findViewById(R.id.editAiIconPrompt)
        btnGenerateAiIcon = findViewById(R.id.btnGenerateAiIcon)
        editGlobalFindText = findViewById(R.id.editGlobalFindText)
        editGlobalReplaceText = findViewById(R.id.editGlobalReplaceText)
        btnGlobalFindOnly = findViewById(R.id.btnGlobalFindOnly)
        btnGlobalFindReplace = findViewById(R.id.btnGlobalFindReplace)

        // Build
        btnTriggerBuildApk = findViewById(R.id.btnTriggerBuildApk)
        boxBuildLogs = findViewById(R.id.boxBuildLogs)
        txtBuildLogOutput = findViewById(R.id.txtBuildLogOutput)
        btnOpenCreateKeystoreDialog = findViewById(R.id.btnOpenCreateKeystoreDialog)
        keystoresListContainer = findViewById(R.id.keystoresListContainer)
        editSignPassword = findViewById(R.id.editSignPassword)
        btnTriggerSignApk = findViewById(R.id.btnTriggerSignApk)
        btnDirectInstallApk = findViewById(R.id.btnDirectInstallApk)

        // Debugger
        btnToggleCloudLogsStream = findViewById(R.id.btnToggleCloudLogsStream)
        btnClearCloudLogs = findViewById(R.id.btnClearCloudLogs)
        txtCloudLogsStream = findViewById(R.id.txtCloudLogsStream)
        editAdbHostIp = findViewById(R.id.editAdbHostIp)
        btnAdbConnect = findViewById(R.id.btnAdbConnect)
        adbDevicesContainer = findViewById(R.id.adbDevicesContainer)
        btnAdbReadLogcat = findViewById(R.id.btnAdbReadLogcat)
        txtAdbLogcatOutput = findViewById(R.id.txtAdbLogcatOutput)

        // AI Settings
        rgAiProvider = findViewById(R.id.rgAiProvider)
        rbProviderGemini = findViewById(R.id.rbProviderGemini)
        rbProviderOpenAi = findViewById(R.id.rbProviderOpenAi)
        txtGeminiKeyStatus = findViewById(R.id.txtGeminiKeyStatus)
        editGeminiApiKey = findViewById(R.id.editGeminiApiKey)
        btnSaveGeminiKey = findViewById(R.id.btnSaveGeminiKey)
        btnDeleteGeminiKey = findViewById(R.id.btnDeleteGeminiKey)
        txtOpenAiKeyStatus = findViewById(R.id.txtOpenAiKeyStatus)
        editOpenAiApiKey = findViewById(R.id.editOpenAiApiKey)
        btnSaveOpenAiKey = findViewById(R.id.btnSaveOpenAiKey)
        btnDeleteOpenAiKey = findViewById(R.id.btnDeleteOpenAiKey)
        editModelGeminiText = findViewById(R.id.editModelGeminiText)
        editModelGeminiImage = findViewById(R.id.editModelGeminiImage)
        editModelOpenAiText = findViewById(R.id.editModelOpenAiText)
        editModelOpenAiImage = findViewById(R.id.editModelOpenAiImage)
        btnSaveCustomModels = findViewById(R.id.btnSaveCustomModels)
        btnResetCustomModels = findViewById(R.id.btnResetCustomModels)

        // Admin
        btnAdminCreateUserDialog = findViewById(R.id.btnAdminCreateUserDialog)
        adminUsersContainer = findViewById(R.id.adminUsersContainer)
        adminInquiriesContainer = findViewById(R.id.adminInquiriesContainer)
        btnAdminCreateBlogDialog = findViewById(R.id.btnAdminCreateBlogDialog)
        adminBlogsContainer = findViewById(R.id.adminBlogsContainer)
        btnAdminCreateFaqDialog = findViewById(R.id.btnAdminCreateFaqDialog)
        adminFaqsContainer = findViewById(R.id.adminFaqsContainer)
        editBackupRepoOwner = findViewById(R.id.editBackupRepoOwner)
        editBackupRepoName = findViewById(R.id.editBackupRepoName)
        editBackupBranch = findViewById(R.id.editBackupBranch)
        editBackupToken = findViewById(R.id.editBackupToken)
        btnSaveBackupSettings = findViewById(R.id.btnSaveBackupSettings)
        btnRunManualBackup = findViewById(R.id.btnRunManualBackup)

        // Community
        editContactName = findViewById(R.id.editContactName)
        editContactEmail = findViewById(R.id.editContactEmail)
        editContactSubject = findViewById(R.id.editContactSubject)
        editContactMessage = findViewById(R.id.editContactMessage)
        btnSubmitContactInquiry = findViewById(R.id.btnSubmitContactInquiry)
        publicBlogsContainer = findViewById(R.id.publicBlogsContainer)
        publicFaqsContainer = findViewById(R.id.publicFaqsContainer)
    }

    private fun setupListeners() {
        btnServerConfig.setOnClickListener { showServerUrlDialog() }
        btnTopScanQr.setOnClickListener { launchQrScanner() }
        btnTopAuth.setOnClickListener {
            if (session.isAuthenticated()) {
                showUserAccountDialog()
            } else {
                showAuthDialog("login")
            }
        }
        btnBannerInstall.setOnClickListener { handleDownloadAndInstallApk() }

        // Navigation Tabs
        tabBtnDashboard.setOnClickListener { switchTab("dashboard") }
        tabBtnProjects.setOnClickListener { switchTab("projects") }
        tabBtnEditor.setOnClickListener { switchTab("editor") }
        tabBtnCustomizer.setOnClickListener { switchTab("customizer") }
        tabBtnBuild.setOnClickListener { switchTab("build") }
        tabBtnDebugger.setOnClickListener { switchTab("debugger") }
        tabBtnAiSettings.setOnClickListener { switchTab("ai_settings") }
        tabBtnAdmin.setOnClickListener { switchTab("admin") }
        tabBtnCommunity.setOnClickListener { switchTab("community") }

        // Quick Actions
        btnQuickUploadDecompile.setOnClickListener { switchTab("projects") }
        btnQuickBuildApk.setOnClickListener { switchTab("build"); triggerBuildApk() }
        btnQuickSignApk.setOnClickListener { switchTab("build") }
        btnQuickAiFix.setOnClickListener { switchTab("customizer"); triggerAiFix() }

        btnDashCloseProject.setOnClickListener { closeCurrentProject() }
        btnDashExploreFiles.setOnClickListener { switchTab("editor"); loadDirectory("") }
        btnDashEditStrings.setOnClickListener { switchTab("customizer"); loadStringsForLocale("values") }
        btnDashRefreshProjects.setOnClickListener { refreshProjectsList() }

        // Project Upload
        boxPickApkFile.setOnClickListener { pickApkLauncher.launch("application/vnd.android.package-archive") }
        btnUploadDecompileSubmit.setOnClickListener { uploadAndDecompileSelectedApk() }

        // File Explorer & Editor
        btnFileUpDir.setOnClickListener { navigateUpDirectory() }
        btnRefreshDir.setOnClickListener { loadDirectory(currentDirPath) }
        btnEditorSave.setOnClickListener { saveCurrentOpenCodeFile() }
        btnEditorAiReview.setOnClickListener { triggerAiReviewOnFile() }
        btnEditorReplaceFile.setOnClickListener { pickReplacementFileLauncher.launch("*/*") }
        btnEditorClose.setOnClickListener { closeCodeEditor() }

        btnHexSearch.setOnClickListener { performHexSearch() }
        btnHexClose.setOnClickListener { closeHexEditor() }
        btnHexApplyPatch.setOnClickListener { applyHexPatch() }

        // Resources & Customizer
        btnLoadStrings.setOnClickListener { loadStringsForLocale(editStringsLocale.text.toString().trim()) }
        btnSaveStrings.setOnClickListener { saveStringsChanges() }
        btnUploadFirebaseJson.setOnClickListener { pickFirebaseLauncher.launch("application/json") }
        btnPickLogoImage.setOnClickListener { pickLogoLauncher.launch("image/*") }
        btnGenerateAiIcon.setOnClickListener { generateAiLauncherIcon() }
        btnGlobalFindOnly.setOnClickListener { performGlobalFindOnly() }
        btnGlobalFindReplace.setOnClickListener { performGlobalFindReplace() }

        // Build & Keystore
        btnTriggerBuildApk.setOnClickListener { triggerBuildApk() }
        btnOpenCreateKeystoreDialog.setOnClickListener { showCreateKeystoreDialog() }
        btnTriggerSignApk.setOnClickListener { triggerSignApk() }
        btnDirectInstallApk.setOnClickListener { handleDownloadAndInstallApk() }

        // ADB & Debugger
        btnToggleCloudLogsStream.setOnClickListener { toggleCloudLogsStream() }
        btnClearCloudLogs.setOnClickListener { clearCloudLogs() }
        btnAdbConnect.setOnClickListener { connectAdbHost() }
        btnAdbReadLogcat.setOnClickListener { readAdbLogcat("all") }

        // AI Settings
        rgAiProvider.setOnCheckedChangeListener { _, checkedId ->
            val prov = if (checkedId == R.id.rbProviderOpenAi) "openai" else "gemini"
            api.saveAiProvider(prov, object : ApiClient.ApiCallback<String> {
                override fun onSuccess(result: String) { toast(result) }
                override fun onError(errorMessage: String) { toast("Provider error: $errorMessage") }
            })
        }
        btnSaveGeminiKey.setOnClickListener { saveApiKey("gemini", editGeminiApiKey.text.toString().trim()) }
        btnDeleteGeminiKey.setOnClickListener { deleteApiKey("gemini") }
        btnSaveOpenAiKey.setOnClickListener { saveApiKey("openai", editOpenAiApiKey.text.toString().trim()) }
        btnDeleteOpenAiKey.setOnClickListener { deleteApiKey("openai") }
        btnSaveCustomModels.setOnClickListener { saveCustomAiModels() }
        btnResetCustomModels.setOnClickListener { resetCustomAiModels() }

        // Admin
        btnAdminCreateUserDialog.setOnClickListener { showCreateUserDialog() }
        btnAdminCreateBlogDialog.setOnClickListener { showEditBlogDialog(null) }
        btnAdminCreateFaqDialog.setOnClickListener { showEditFaqDialog(null) }
        btnSaveBackupSettings.setOnClickListener { saveAdminBackupSettings() }
        btnRunManualBackup.setOnClickListener { runAdminManualBackup() }

        // Community & Contact
        btnSubmitContactInquiry.setOnClickListener { submitContactInquiryForm() }
    }

    // -------------------------------------------------------------
    // Tab Navigation Switcher
    // -------------------------------------------------------------

    private fun switchTab(tabKey: String) {
        val tabs = listOf(
            Pair("dashboard", Pair(tabBtnDashboard, panelDashboard)),
            Pair("projects", Pair(tabBtnProjects, panelProjects)),
            Pair("editor", Pair(tabBtnEditor, panelEditor)),
            Pair("customizer", Pair(tabBtnCustomizer, panelCustomizer)),
            Pair("build", Pair(tabBtnBuild, panelBuild)),
            Pair("debugger", Pair(tabBtnDebugger, panelDebugger)),
            Pair("ai_settings", Pair(tabBtnAiSettings, panelAiSettings)),
            Pair("admin", Pair(tabBtnAdmin, panelAdmin)),
            Pair("community", Pair(tabBtnCommunity, panelCommunity))
        )

        for ((key, pair) in tabs) {
            val (btn, panel) = pair
            val isActive = key == tabKey
            panel.visibility = if (isActive) View.VISIBLE else View.GONE
            if (isActive) {
                btn.setBackgroundColor(getColor(R.color.primary))
                btn.setTextColor(getColor(R.color.text_inverse))
            } else {
                btn.setBackgroundColor(getColor(android.R.color.transparent))
                btn.setTextColor(getColor(R.color.text_secondary))
            }
        }

        // On tab entry, load contextual data
        when (tabKey) {
            "dashboard" -> refreshDashboardData()
            "projects" -> refreshProjectsList()
            "editor" -> if (currentDirPath.isEmpty()) loadDirectory("")
            "customizer" -> loadStringsForLocale("values")
            "build" -> loadKeystoresList()
            "debugger" -> loadAdbDevicesList()
            "ai_settings" -> loadAiSettings()
            "admin" -> loadAdminPanelData()
            "community" -> loadPublicCommunityData()
        }
    }

    // -------------------------------------------------------------
    // UI State & Auth Updates
    // -------------------------------------------------------------

    private fun updateUiForAuthState() {
        val user = session.currentUser
        topBarSubtitle.text = "Host: ${session.serverUrl}"
        if (user != null) {
            btnTopAuth.text = user.username
            btnTopAuth.setBackgroundColor(getColor(R.color.primary_dark))
            txtDashWelcome.text = "Welcome back, ${user.username}!"
            txtDashUserEmail.text = "Logged in as ${user.email} (${user.userType})"
            txtAdminBadge.visibility = if (user.isAdmin) View.VISIBLE else View.GONE
            tabBtnAdmin.visibility = if (user.isAdmin) View.VISIBLE else View.GONE
            updateLimitsDisplay(user.decompileUsage, user.decompileLimit, user.compileUsage, user.compileLimit, user.generateKeyUsage, user.generateKeyLimit, user.signApkUsage, user.signApkLimit)
        } else {
            btnTopAuth.text = "Login"
            btnTopAuth.setBackgroundColor(getColor(R.color.primary))
            txtDashWelcome.text = "Welcome to APK Tool Studio"
            txtDashUserEmail.text = "Connect to your server account to access all features"
            txtAdminBadge.visibility = View.GONE
            tabBtnAdmin.visibility = View.GONE
        }

        val activePid = session.currentProjectId
        val pairedName = session.pairedProjectName
        if (!activePid.isNullOrBlank()) {
            txtActiveProjectPill.text = "📁 Active: $activePid"
            txtDashProjectName.text = activePid
            cardActiveProjectOverview.visibility = View.VISIBLE
        } else if (!pairedName.isNullOrBlank()) {
            txtActiveProjectPill.text = "🔗 Paired: $pairedName"
            txtDashProjectName.text = pairedName
            cardActiveProjectOverview.visibility = View.VISIBLE
        } else {
            txtActiveProjectPill.text = "No project selected"
            txtDashProjectName.text = "No active project"
            cardActiveProjectOverview.visibility = View.GONE
        }
    }

    private fun updateLimitsDisplay(du: Int, dl: Int, cu: Int, cl: Int, ku: Int, kl: Int, su: Int, sl: Int) {
        txtMeterDecompile.text = "$du / $dl"
        txtMeterCompile.text = "$cu / $cl"
        txtMeterKeygen.text = "$ku / $kl"
        txtMeterSign.text = "$su / $sl"
    }

    // -------------------------------------------------------------
    // Dashboard & Multi-Project Logic
    // -------------------------------------------------------------

    private fun refreshDashboardData() {
        if (!session.isAuthenticated()) return
        api.getUserInfo(object : ApiClient.ApiCallback<User> {
            override fun onSuccess(result: User) {
                updateUiForAuthState()
            }
            override fun onError(errorMessage: String) {}
        })
        refreshProjectsList()
    }

    private fun refreshProjectsList() {
        if (!session.isAuthenticated()) return
        api.getProjects(object : ApiClient.ApiCallback<List<ProjectItem>> {
            override fun onSuccess(result: List<ProjectItem>) {
                renderProjectsList(result, dashProjectsListContainer)
                renderProjectsList(result, projectsListContainer)
            }
            override fun onError(errorMessage: String) {
                toast("Could not load projects: $errorMessage")
            }
        })
    }

    private fun renderProjectsList(projects: List<ProjectItem>, container: LinearLayout) {
        container.removeAllViews()
        if (projects.isEmpty()) {
            val emptyTv = TextView(this).apply {
                text = "No projects yet. Upload an APK above to decompile your first project."
                setTextColor(getColor(R.color.text_muted))
                textSize = 12f
                setPadding(0, 16, 0, 16)
            }
            container.addView(emptyTv)
            return
        }

        val inflater = LayoutInflater.from(this)
        for (p in projects) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = getDrawable(R.drawable.bg_card_rounded)
                setPadding(24, 20, 24, 20)
                val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                params.setMargins(0, 0, 0, 16)
                layoutParams = params
            }

            val topRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
            }

            val titleTv = TextView(this).apply {
                text = p.projectName.ifBlank { p.projectId }
                setTextColor(getColor(R.color.text_primary))
                textSize = 14f
                textStyle = android.graphics.Typeface.BOLD
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            topRow.addView(titleTv)

            val isCurrent = session.currentProjectId == p.projectId
            val statusBadge = TextView(this).apply {
                text = if (isCurrent) "ACTIVE" else "READY"
                background = if (isCurrent) getDrawable(R.drawable.bg_badge_success) else getDrawable(R.drawable.bg_badge_primary)
                setTextColor(if (isCurrent) getColor(R.color.success_dark) else getColor(R.color.primary))
                textSize = 9f
                textStyle = android.graphics.Typeface.BOLD
                setPadding(12, 4, 12, 4)
            }
            topRow.addView(statusBadge)
            card.addView(topRow)

            val descTv = TextView(this).apply {
                text = "ID: ${p.projectId} • Created: ${p.createdAt ?: "Recent"}"
                setTextColor(getColor(R.color.text_muted))
                textSize = 11f
                setPadding(0, 4, 0, 12)
            }
            card.addView(descTv)

            // Action Buttons
            val btnRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            val switchBtn = Button(this, null, androidx.appcompat.R.attr.buttonStyle).apply {
                text = if (isCurrent) "Open Files" else "Select Project"
                textSize = 11f
                setBackgroundColor(if (isCurrent) getColor(R.color.primary) else getColor(R.color.surface_dark_alt))
                setTextColor(getColor(R.color.text_inverse))
                val pms = LinearLayout.LayoutParams(0, 84, 1f).apply { marginEnd = 8 }
                layoutParams = pms
                setOnClickListener {
                    api.switchProject(p.projectId, object : ApiClient.ApiCallback<JSONObject> {
                        override fun onSuccess(result: JSONObject) {
                            session.currentProjectId = p.projectId
                            updateUiForAuthState()
                            toast("Project '${p.projectName}' active.")
                            switchTab("editor")
                        }
                        override fun onError(errorMessage: String) {
                            toast("Failed to open project: $errorMessage")
                        }
                    })
                }
            }
            btnRow.addView(switchBtn)

            val renameBtn = Button(this, null, androidx.appcompat.R.attr.buttonStyle).apply {
                text = "Rename"
                textSize = 11f
                setBackgroundColor(getColor(R.color.border_light))
                setTextColor(getColor(R.color.text_secondary))
                val pms = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 84).apply { marginEnd = 8 }
                layoutParams = pms
                setOnClickListener { showRenameProjectDialog(p.projectId, p.projectName) }
            }
            btnRow.addView(renameBtn)

            val deleteBtn = Button(this, null, androidx.appcompat.R.attr.buttonStyle).apply {
                text = "Delete"
                textSize = 11f
                setBackgroundColor(getColor(R.color.danger_light))
                setTextColor(getColor(R.color.danger))
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 84)
                setOnClickListener {
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("Delete Project")
                        .setMessage("Are you sure you want to delete '${p.projectName}'?")
                        .setPositiveButton("Delete") { _, _ ->
                            api.deleteProject(p.projectId, object : ApiClient.ApiCallback<String> {
                                override fun onSuccess(result: String) {
                                    toast(result)
                                    refreshProjectsList()
                                }
                                override fun onError(errorMessage: String) { toast("Delete error: $errorMessage") }
                            })
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            }
            btnRow.addView(deleteBtn)

            card.addView(btnRow)
            container.addView(card)
        }
    }

    private fun uploadAndDecompileSelectedApk() {
        val file = pendingUploadApkFile
        if (file == null || !file.exists()) {
            toast("Please select an APK file from storage first.")
            return
        }

        setBusy(true, "Uploading APK...")
        boxDecompileLogs.visibility = View.VISIBLE
        txtDecompileStatus.text = "Uploading ${file.name} to server..."
        txtDecompileLogOutput.text = "Starting upload..."

        api.uploadAndDecompileApk(
            apkFile = file,
            progressCallback = object : ApiClient.ProgressCallback {
                override fun onProgress(percentage: Int, message: String) {
                    globalProgressBar.progress = percentage
                    txtDecompileStatus.text = message
                }
            },
            callback = object : ApiClient.ApiCallback<JSONObject> {
                override fun onSuccess(result: JSONObject) {
                    setBusy(false, "")
                    txtDecompileStatus.text = "✅ Decompiled successfully!"
                    txtDecompileLogOutput.text = "Apktool decompilation finished.\nProject root and smali source ready."
                    toast("APK uploaded and decompiled successfully!")
                    refreshProjectsList()
                    updateUiForAuthState()
                    switchTab("editor")
                }

                override fun onError(errorMessage: String) {
                    setBusy(false, "")
                    txtDecompileStatus.text = "❌ Decompile error"
                    txtDecompileLogOutput.text = errorMessage
                    toast("Decompile failed: $errorMessage")
                }
            }
        )
    }

    private fun closeCurrentProject() {
        api.closeProject(object : ApiClient.ApiCallback<String> {
            override fun onSuccess(result: String) {
                session.currentProjectId = null
                updateUiForAuthState()
                toast("Project closed.")
                switchTab("dashboard")
            }
            override fun onError(errorMessage: String) { toast(errorMessage) }
        })
    }

    // -------------------------------------------------------------
    // Directory Explorer, Code Editor & Hex Suite
    // -------------------------------------------------------------

    private fun loadDirectory(relPath: String) {
        currentDirPath = relPath
        txtFileBreadcrumb.text = if (relPath.isEmpty()) "📂 / (project root)" else "📂 $relPath"
        setBusy(true, "Browsing directory...")

        api.getDirectory(relPath, object : ApiClient.ApiCallback<Pair<String, List<ProjectFile>>> {
            override fun onSuccess(result: Pair<String, List<ProjectFile>>) {
                setBusy(false, "")
                scrollDirectoryBrowser.visibility = View.VISIBLE
                boxCodeEditorView.visibility = View.GONE
                boxHexEditorView.visibility = View.GONE
                renderDirectoryItems(result.second)
            }

            override fun onError(errorMessage: String) {
                setBusy(false, "")
                toast("Directory error: $errorMessage")
            }
        })
    }

    private fun navigateUpDirectory() {
        if (currentDirPath.isEmpty()) return
        val idx = currentDirPath.lastIndexOf('/')
        val parent = if (idx > 0) currentDirPath.substring(0, idx) else ""
        loadDirectory(parent)
    }

    private fun renderDirectoryItems(items: List<ProjectFile>) {
        directoryItemsContainer.removeAllViews()
        if (items.isEmpty()) {
            val tv = TextView(this).apply {
                text = "Empty folder."
                setTextColor(getColor(R.color.text_muted))
                textSize = 12f
                setPadding(12, 16, 12, 16)
            }
            directoryItemsContainer.addView(tv)
            return
        }

        for (item in items) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(16, 14, 16, 14)
                background = getDrawable(R.drawable.bg_input_field)
                val pms = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 0, 6)
                }
                layoutParams = pms
                isClickable = true
                isFocusable = true
            }

            val iconTv = TextView(this).apply {
                text = if (item.isDir) "📁" else when {
                    item.name.endsWith(".smali") -> "⚡"
                    item.name.endsWith(".xml") -> "📄"
                    item.name.endsWith(".so") -> "⚙️"
                    item.name.endsWith(".png") || item.name.endsWith(".webp") -> "🖼️"
                    else -> "📝"
                }
                textSize = 16f
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    marginEnd = 12
                }
            }
            row.addView(iconTv)

            val nameTv = TextView(this).apply {
                text = item.name
                setTextColor(getColor(R.color.text_primary))
                textSize = 13f
                textStyle = if (item.isDir) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            row.addView(nameTv)

            if (!item.isDir) {
                val sizeTv = TextView(this).apply {
                    text = formatSize(item.size)
                    setTextColor(getColor(R.color.text_muted))
                    textSize = 11f
                }
                row.addView(sizeTv)
            }

            row.setOnClickListener {
                if (item.isDir) {
                    loadDirectory(item.path)
                } else {
                    if (item.name.endsWith(".so") || item.name.endsWith(".dex")) {
                        openHexEditorForFile(item.path)
                    } else {
                        openEditorForFile(item.path)
                    }
                }
            }

            directoryItemsContainer.addView(row)
        }
    }

    private fun openEditorForFile(filePath: String) {
        currentEditingFilePath = filePath
        setBusy(true, "Opening $filePath...")
        api.openEditorFile(filePath, 0, object : ApiClient.ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                setBusy(false, "")
                scrollDirectoryBrowser.visibility = View.GONE
                boxHexEditorView.visibility = View.GONE
                boxCodeEditorView.visibility = View.VISIBLE
                txtEditorOpenFileName.text = filePath
                val state = result.optJSONObject("state")
                val editorFile = state?.optJSONObject("editor_file")
                editCodeContent.setText(editorFile?.optString("content", "") ?: "")
            }

            override fun onError(errorMessage: String) {
                setBusy(false, "")
                toast("Could not open file: $errorMessage")
            }
        })
    }

    private fun saveCurrentOpenCodeFile() {
        if (currentEditingFilePath.isEmpty()) return
        val content = editCodeContent.text.toString()
        setBusy(true, "Saving file...")
        api.saveEditorFile(currentEditingFilePath, content, 0, 0, object : ApiClient.ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                setBusy(false, "")
                toast(result.optString("message", "File saved successfully."))
            }

            override fun onError(errorMessage: String) {
                setBusy(false, "")
                toast("Save error: $errorMessage")
            }
        })
    }

    private fun triggerAiReviewOnFile() {
        if (currentEditingFilePath.isEmpty()) return
        val content = editCodeContent.text.toString()
        setBusy(true, "AI reviewing file for syntax & errors...")
        api.aiReviewEditor(currentEditingFilePath, content, object : ApiClient.ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                setBusy(false, "")
                val expl = result.optString("explanation", "AI analyzed the file.")
                val changed = result.optBoolean("changed", false)
                AlertDialog.Builder(this@MainActivity)
                    .setTitle(if (changed) "AI Fix Applied" else "AI Code Review")
                    .setMessage(expl)
                    .setPositiveButton("OK", null)
                    .show()
                if (changed) {
                    openEditorForFile(currentEditingFilePath)
                }
            }

            override fun onError(errorMessage: String) {
                setBusy(false, "")
                toast("AI error: $errorMessage")
            }
        })
    }

    private fun closeCodeEditor() {
        boxCodeEditorView.visibility = View.GONE
        scrollDirectoryBrowser.visibility = View.VISIBLE
    }

    private fun openHexEditorForFile(filePath: String) {
        currentEditingFilePath = filePath
        scrollDirectoryBrowser.visibility = View.GONE
        boxCodeEditorView.visibility = View.GONE
        boxHexEditorView.visibility = View.VISIBLE
        hexResultsContainer.removeAllViews()
        editHexSearchQuery.setText("")
        toast("Hex editor opened for $filePath")
    }

    private fun performHexSearch() {
        val query = editHexSearchQuery.text.toString().trim()
        if (query.isEmpty() || currentEditingFilePath.isEmpty()) return
        setBusy(true, "Searching binary hex pattern...")
        api.searchHex(currentEditingFilePath, query, object : ApiClient.ApiCallback<List<HexResult>> {
            override fun onSuccess(result: List<HexResult>) {
                setBusy(false, "")
                renderHexResults(result)
            }

            override fun onError(errorMessage: String) {
                setBusy(false, "")
                toast("Hex search error: $errorMessage")
            }
        })
    }

    private fun renderHexResults(results: List<HexResult>) {
        hexResultsContainer.removeAllViews()
        if (results.isEmpty()) {
            val tv = TextView(this).apply {
                text = "No matches found."
                setTextColor(getColor(R.color.text_muted))
                textSize = 11f
            }
            hexResultsContainer.addView(tv)
            return
        }

        for (r in results) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = getDrawable(R.drawable.bg_card_dark)
                setPadding(12, 10, 12, 10)
                val pms = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 0, 8)
                }
                layoutParams = pms
            }

            val offsetTv = TextView(this).apply {
                text = "OFFSET: 0x${r.hexOffset} (${r.offset})"
                setTextColor(getColor(R.color.hex_offset))
                textSize = 12f
                fontFamily = android.graphics.Typeface.MONOSPACE
                textStyle = android.graphics.Typeface.BOLD
            }
            row.addView(offsetTv)

            val hexTv = TextView(this).apply {
                text = "HEX: ${r.hexSnippet}"
                setTextColor(getColor(R.color.text_inverse))
                textSize = 11f
                fontFamily = android.graphics.Typeface.MONOSPACE
            }
            row.addView(hexTv)

            val asciiTv = TextView(this).apply {
                text = "ASCII: ${r.asciiSnippet}"
                setTextColor(getColor(R.color.hex_ascii))
                textSize = 11f
                fontFamily = android.graphics.Typeface.MONOSPACE
            }
            row.addView(asciiTv)

            row.setOnClickListener {
                editHexPatchBytes.setText(r.hexSnippet)
                toast("Offset 0x${r.hexOffset} selected for patch.")
            }

            hexResultsContainer.addView(row)
        }
    }

    private fun applyHexPatch() {
        val hex = editHexPatchBytes.text.toString().trim().replace(" ", "")
        if (hex.isEmpty() || currentEditingFilePath.isEmpty()) return
        setBusy(true, "Applying binary patch...")
        api.saveEditorFile(currentEditingFilePath, hex, 0, 0, object : ApiClient.ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                setBusy(false, "")
                toast("Binary patch applied successfully.")
            }
            override fun onError(errorMessage: String) {
                setBusy(false, "")
                toast("Patch error: $errorMessage")
            }
        })
    }

    private fun closeHexEditor() {
        boxHexEditorView.visibility = View.GONE
        scrollDirectoryBrowser.visibility = View.VISIBLE
    }

    // -------------------------------------------------------------
    // Strings & AI Customizer Studio
    // -------------------------------------------------------------

    private fun loadStringsForLocale(locale: String) {
        val loc = locale.ifBlank { "values" }
        setBusy(true, "Loading strings for '$loc'...")
        api.loadStrings(loc, object : ApiClient.ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                setBusy(false, "")
                val state = result.optJSONObject("state")
                editCustomAppName.setText(state?.optString("custom_app_name", "") ?: "")
                val stringsJson = state?.optJSONObject("strings") ?: JSONObject()
                loadedStringsMap.clear()
                val keys = stringsJson.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    loadedStringsMap[k] = stringsJson.optString(k, "")
                }
                renderStringsTable(loadedStringsMap)
            }

            override fun onError(errorMessage: String) {
                setBusy(false, "")
                toast("Strings error: $errorMessage")
            }
        })
    }

    private fun renderStringsTable(map: Map<String, String>) {
        stringsTableContainer.removeAllViews()
        if (map.isEmpty()) {
            val tv = TextView(this).apply {
                text = "No strings loaded."
                setTextColor(getColor(R.color.text_muted))
                textSize = 12f
            }
            stringsTableContainer.addView(tv)
            return
        }

        for ((k, v) in map) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = getDrawable(R.drawable.bg_input_field)
                setPadding(12, 10, 12, 10)
                val pms = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 0, 6)
                }
                layoutParams = pms
                isClickable = true
                isFocusable = true
            }

            val keyTv = TextView(this).apply {
                text = "@string/$k"
                setTextColor(getColor(R.color.primary))
                textSize = 12f
                fontFamily = android.graphics.Typeface.MONOSPACE
                textStyle = android.graphics.Typeface.BOLD
            }
            row.addView(keyTv)

            val valTv = TextView(this).apply {
                text = v
                setTextColor(getColor(R.color.text_primary))
                textSize = 12f
            }
            row.addView(valTv)

            row.setOnClickListener { showEditStringDialog(k, v) }
            stringsTableContainer.addView(row)
        }
    }

    private fun saveStringsChanges() {
        val locale = editStringsLocale.text.toString().trim().ifBlank { "values" }
        val appName = editCustomAppName.text.toString().trim()
        setBusy(true, "Saving strings...")
        api.autosaveStrings(locale, appName, loadedStringsMap, object : ApiClient.ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                setBusy(false, "")
                toast(result.optString("message", "Strings saved."))
            }

            override fun onError(errorMessage: String) {
                setBusy(false, "")
                toast("Save error: $errorMessage")
            }
        })
    }

    private fun generateAiLauncherIcon() {
        val prompt = editAiIconPrompt.text.toString().trim()
        if (prompt.isEmpty()) {
            toast("Enter an icon description prompt first.")
            return
        }
        setBusy(true, "Generating AI icon and replacing mipmap targets...")
        api.generateAiIcon(prompt, object : ApiClient.ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                setBusy(false, "")
                toast(result.optString("message", "AI icon applied!"))
            }

            override fun onError(errorMessage: String) {
                setBusy(false, "")
                toast("AI Icon error: $errorMessage")
            }
        })
    }

    private fun performGlobalFindOnly() {
        val find = editGlobalFindText.text.toString().trim()
        if (find.isEmpty()) return
        setBusy(true, "Scanning project files...")
        api.findProject(find, object : ApiClient.ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                setBusy(false, "")
                toast(result.optString("message", "Find complete."))
            }

            override fun onError(errorMessage: String) {
                setBusy(false, "")
                toast("Find error: $errorMessage")
            }
        })
    }

    private fun performGlobalFindReplace() {
        val find = editGlobalFindText.text.toString().trim()
        val replace = editGlobalReplaceText.text.toString()
        if (find.isEmpty()) return
        setBusy(true, "Replacing across project...")
        api.findReplaceProject(find, replace, object : ApiClient.ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                setBusy(false, "")
                toast(result.optString("message", "Find & Replace complete."))
            }

            override fun onError(errorMessage: String) {
                setBusy(false, "")
                toast("Replace error: $errorMessage")
            }
        })
    }

    private fun triggerAiFix() {
        setBusy(true, "AI diagnosing build errors & synthesizing fixes...")
        api.aiFixError("", object : ApiClient.ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                setBusy(false, "")
                val fix = result.optJSONObject("ai_fix")
                val expl = fix?.optString("explanation", "AI analyzed errors.") ?: "No error found."
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("AI Diagnostic Result")
                    .setMessage(expl)
                    .setPositiveButton("Apply Fix") { _, _ ->
                        api.aiApplyFix(object : ApiClient.ApiCallback<JSONObject> {
                            override fun onSuccess(r: JSONObject) { toast("AI Fix applied.") }
                            override fun onError(e: String) { toast("Error: $e") }
                        })
                    }
                    .setNegativeButton("Close", null)
                    .show()
            }

            override fun onError(errorMessage: String) {
                setBusy(false, "")
                toast("AI Fix error: $errorMessage")
            }
        })
    }

    // -------------------------------------------------------------
    // Build, Keystores & Signing Studio
    // -------------------------------------------------------------

    private fun triggerBuildApk() {
        setBusy(true, "Building unsigned APK with Apktool...")
        boxBuildLogs.visibility = View.VISIBLE
        txtBuildLogOutput.text = "Executing apktool build..."

        api.buildApk(object : ApiClient.ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                setBusy(false, "")
                val state = result.optJSONObject("state")
                txtBuildLogOutput.text = state?.optString("last_build_log", "APK built successfully.")
                toast("Unsigned APK built successfully!")
            }

            override fun onError(errorMessage: String) {
                setBusy(false, "")
                txtBuildLogOutput.text = errorMessage
                toast("Build failed: $errorMessage")
            }
        })
    }

    private fun loadKeystoresList() {
        if (!session.isAuthenticated()) return
        api.getKeystores(object : ApiClient.ApiCallback<List<KeystoreItem>> {
            override fun onSuccess(result: List<KeystoreItem>) {
                renderKeystoresList(result)
            }
            override fun onError(errorMessage: String) {
                toast("Could not load keystores: $errorMessage")
            }
        })
    }

    private fun renderKeystoresList(keystores: List<KeystoreItem>) {
        keystoresListContainer.removeAllViews()
        if (keystores.isEmpty()) {
            val tv = TextView(this).apply {
                text = "No keystores created yet. Generate one above."
                setTextColor(getColor(R.color.text_muted))
                textSize = 11f
            }
            keystoresListContainer.addView(tv)
            return
        }

        for (ks in keystores) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                background = getDrawable(R.drawable.bg_input_field)
                setPadding(12, 10, 12, 10)
                val pms = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 0, 6)
                }
                layoutParams = pms
            }

            val titleTv = TextView(this).apply {
                text = "🔑 Alias: ${ks.keyAlias}\nFile: ${ks.fileName}"
                setTextColor(getColor(R.color.text_primary))
                textSize = 11f
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            row.addView(titleTv)

            val selectBtn = Button(this, null, androidx.appcompat.R.attr.buttonStyle).apply {
                text = "Use for Sign"
                textSize = 10f
                setBackgroundColor(getColor(R.color.primary))
                setTextColor(getColor(R.color.text_inverse))
                setOnClickListener {
                    api.selectKeystore(ks.id, object : ApiClient.ApiCallback<JSONObject> {
                        override fun onSuccess(result: JSONObject) {
                            toast("Keystore '${ks.keyAlias}' selected.")
                        }
                        override fun onError(errorMessage: String) { toast(errorMessage) }
                    })
                }
            }
            row.addView(selectBtn)
            keystoresListContainer.addView(row)
        }
    }

    private fun triggerSignApk() {
        val pass = editSignPassword.text.toString().trim()
        if (pass.isEmpty()) {
            toast("Enter keystore password first.")
            return
        }

        setBusy(true, "Zipaligning & Signing APK with Apksigner...")
        api.signApk(pass, object : ApiClient.ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                setBusy(false, "")
                toast(result.optString("message", "APK signed successfully!"))
                btnDirectInstallApk.visibility = View.VISIBLE
            }

            override fun onError(errorMessage: String) {
                setBusy(false, "")
                toast("Sign error: $errorMessage")
            }
        })
    }

    private fun handleDownloadAndInstallApk() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !packageManager.canRequestPackageInstalls()) {
            toast("Allow 'Install unknown apps' for this app, then tap Install again.")
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName"))
            startActivity(intent)
            return
        }

        val tok = session.pairingToken
        val downloadUrl = if (!tok.isNullOrBlank()) {
            val base = session.serverUrl
            base + (if (base.contains("?")) "&" else "?") + "device_download=1&token=" + Uri.encode(tok)
        } else {
            val base = session.serverUrl
            "${base}index.php?download=signed_apk"
        }

        val dest = File(File(cacheDir, "downloads").apply { mkdirs() }, "latest.apk")
        setBusy(true, "Downloading signed APK...")

        api.downloadApk(
            downloadUrl = downloadUrl,
            expectedSha256 = null,
            expectedSize = 0,
            destFile = dest,
            progressCallback = object : ApiClient.ProgressCallback {
                override fun onProgress(percentage: Int, message: String) {
                    globalProgressBar.progress = percentage
                }
            },
            callback = object : ApiClient.ApiCallback<File> {
                override fun onSuccess(result: File) {
                    setBusy(false, "")
                    globalUpdateBanner.visibility = View.GONE
                    installDownloadedApk(result)
                }

                override fun onError(errorMessage: String) {
                    setBusy(false, "")
                    toast("Download failed: $errorMessage")
                }
            }
        )
    }

    private fun installDownloadedApk(file: File) {
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            toast("Could not open package installer: ${e.message}")
        }
    }

    // -------------------------------------------------------------
    // ADB, Debugger & Cloud Log Streaming
    // -------------------------------------------------------------

    private fun loadAdbDevicesList() {
        api.adbListDevices(object : ApiClient.ApiCallback<List<AdbDevice>> {
            override fun onSuccess(result: List<AdbDevice>) {
                renderAdbDevicesList(result)
            }
            override fun onError(errorMessage: String) {
                toast("ADB error: $errorMessage")
            }
        })
    }

    private fun renderAdbDevicesList(devices: List<AdbDevice>) {
        adbDevicesContainer.removeAllViews()
        if (devices.isEmpty()) {
            val tv = TextView(this).apply {
                text = "No wireless or USB ADB devices connected."
                setTextColor(getColor(R.color.text_muted))
                textSize = 11f
            }
            adbDevicesContainer.addView(tv)
            return
        }

        for (dev in devices) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                background = getDrawable(R.drawable.bg_input_field)
                setPadding(12, 10, 12, 10)
                val pms = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 0, 6)
                }
                layoutParams = pms
            }

            val titleTv = TextView(this).apply {
                text = "📱 ${dev.serial} (${dev.state})"
                setTextColor(getColor(R.color.text_primary))
                textSize = 12f
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            row.addView(titleTv)

            val installBtn = Button(this, null, androidx.appcompat.R.attr.buttonStyle).apply {
                text = "Install over ADB"
                textSize = 10f
                setBackgroundColor(getColor(R.color.primary))
                setTextColor(getColor(R.color.text_inverse))
                setOnClickListener {
                    api.adbInstallApk(dev.serial, "signed", object : ApiClient.ApiCallback<String> {
                        override fun onSuccess(r: String) { toast(r) }
                        override fun onError(e: String) { toast(e) }
                    })
                }
            }
            row.addView(installBtn)
            adbDevicesContainer.addView(row)
        }
    }

    private fun connectAdbHost() {
        val host = editAdbHostIp.text.toString().trim()
        if (host.isEmpty()) return
        setBusy(true, "Connecting wireless ADB...")
        api.adbConnect(host, object : ApiClient.ApiCallback<String> {
            override fun onSuccess(result: String) {
                setBusy(false, "")
                toast(result)
                loadAdbDevicesList()
            }
            override fun onError(errorMessage: String) {
                setBusy(false, "")
                toast("Connect error: $errorMessage")
            }
        })
    }

    private fun readAdbLogcat(filter: String) {
        setBusy(true, "Polling logcat...")
        api.adbReadLogcat("", filter, object : ApiClient.ApiCallback<List<String>> {
            override fun onSuccess(result: List<String>) {
                setBusy(false, "")
                val sb = StringBuilder()
                for (l in result) sb.append(l).append("\n")
                txtAdbLogcatOutput.text = if (sb.isEmpty()) "No logcat output." else sb.toString()
            }
            override fun onError(errorMessage: String) {
                setBusy(false, "")
                toast("Logcat error: $errorMessage")
            }
        })
    }

    private fun toggleCloudLogsStream() {
        if (cloudLogsStreaming) {
            cloudLogsStreaming = false
            btnToggleCloudLogsStream.text = "Start Stream"
            pauseCloudLogsPolling()
        } else {
            cloudLogsStreaming = true
            btnToggleCloudLogsStream.text = "Pause Stream"
            resumeCloudLogsPolling()
        }
    }

    private fun resumeCloudLogsPolling() {
        pauseCloudLogsPolling()
        fetchCloudLogsOnce()
        val r = object : Runnable {
            override fun run() {
                fetchCloudLogsOnce()
                mainHandler.postDelayed(this, 2000)
            }
        }
        cloudLogsTimer = r
        mainHandler.postDelayed(r, 2000)
    }

    private fun pauseCloudLogsPolling() {
        cloudLogsTimer?.let { mainHandler.removeCallbacks(it) }
        cloudLogsTimer = null
    }

    private fun fetchCloudLogsOnce() {
        val tok = session.pairingToken
        if (!tok.isNullOrBlank()) {
            api.getDeviceLogs(tok, object : ApiClient.ApiCallback<List<String>> {
                override fun onSuccess(result: List<String>) {
                    val sb = StringBuilder()
                    for (l in result) sb.append(l).append("\n")
                    txtCloudLogsStream.text = if (sb.isEmpty()) "No logs yet." else sb.toString()
                }
                override fun onError(errorMessage: String) {}
            })
        } else {
            api.getCloudLogs(object : ApiClient.ApiCallback<List<String>> {
                override fun onSuccess(result: List<String>) {
                    val sb = StringBuilder()
                    for (l in result) sb.append(l).append("\n")
                    txtCloudLogsStream.text = if (sb.isEmpty()) "No logs yet." else sb.toString()
                }
                override fun onError(errorMessage: String) {}
            })
        }
    }

    private fun clearCloudLogs() {
        api.clearCloudLogs(object : ApiClient.ApiCallback<String> {
            override fun onSuccess(result: String) {
                txtCloudLogsStream.text = "Logs cleared."
                toast(result)
            }
            override fun onError(errorMessage: String) { toast(errorMessage) }
        })
    }

    private fun startAutoCheckTimer() {
        stopAutoCheckTimer()
        val r = object : Runnable {
            override fun run() {
                val tok = session.pairingToken
                if (!tok.isNullOrBlank()) {
                    api.checkDeviceUpdate(tok, object : ApiClient.ApiCallback<JSONObject> {
                        override fun onSuccess(result: JSONObject) {
                            val hasBuild = result.optBoolean("has_build", false) && result.optBoolean("is_signed", false)
                            val remoteVer = result.optLong("build_version", 0)
                            val lastSeen = session.lastBuildVersion
                            if (hasBuild && remoteVer > lastSeen) {
                                globalUpdateBanner.visibility = View.VISIBLE
                            }
                        }
                        override fun onError(errorMessage: String) {}
                    })
                }
                mainHandler.postDelayed(this, 30000)
            }
        }
        autoCheckTimer = r
        mainHandler.postDelayed(r, 30000)
    }

    private fun stopAutoCheckTimer() {
        autoCheckTimer?.let { mainHandler.removeCallbacks(it) }
        autoCheckTimer = null
    }

    // -------------------------------------------------------------
    // AI Settings Studio
    // -------------------------------------------------------------

    private fun loadAiSettings() {
        if (!session.isAuthenticated()) return
        api.getAiSettings(object : ApiClient.ApiCallback<AiSettingsData> {
            override fun onSuccess(result: AiSettingsData) {
                if (result.provider.equals("openai", true)) {
                    rbProviderOpenAi.isChecked = true
                } else {
                    rbProviderGemini.isChecked = true
                }
                txtGeminiKeyStatus.text = if (result.geminiHasKey) "Configured (${result.geminiMaskedKey})" else "No key saved"
                txtOpenAiKeyStatus.text = if (result.openaiHasKey) "Configured (${result.openaiMaskedKey})" else "No key saved"
                editModelGeminiText.setText(result.geminiTextModel)
                editModelGeminiImage.setText(result.geminiImageModel)
                editModelOpenAiText.setText(result.openaiTextModel)
                editModelOpenAiImage.setText(result.openaiImageModel)
            }
            override fun onError(errorMessage: String) {
                toast("AI Settings error: $errorMessage")
            }
        })
    }

    private fun saveApiKey(provider: String, key: String) {
        if (key.isEmpty()) {
            toast("Enter API key first.")
            return
        }
        setBusy(true, "Saving $provider API key...")
        api.saveApiKey(provider, key, object : ApiClient.ApiCallback<String> {
            override fun onSuccess(result: String) {
                setBusy(false, "")
                toast(result)
                loadAiSettings()
            }
            override fun onError(errorMessage: String) {
                setBusy(false, "")
                toast("Key error: $errorMessage")
            }
        })
    }

    private fun deleteApiKey(provider: String) {
        setBusy(true, "Removing $provider key...")
        api.deleteApiKey(provider, object : ApiClient.ApiCallback<String> {
            override fun onSuccess(result: String) {
                setBusy(false, "")
                toast(result)
                loadAiSettings()
            }
            override fun onError(errorMessage: String) {
                setBusy(false, "")
                toast(errorMessage)
            }
        })
    }

    private fun saveCustomAiModels() {
        val map = mapOf(
            "gemini_text_model" to editModelGeminiText.text.toString().trim(),
            "gemini_image_model" to editModelGeminiImage.text.toString().trim(),
            "openai_text_model" to editModelOpenAiText.text.toString().trim(),
            "openai_image_model" to editModelOpenAiImage.text.toString().trim()
        )
        setBusy(true, "Saving models...")
        api.saveUserAiModels(map, object : ApiClient.ApiCallback<String> {
            override fun onSuccess(result: String) {
                setBusy(false, "")
                toast(result)
            }
            override fun onError(errorMessage: String) {
                setBusy(false, "")
                toast(errorMessage)
            }
        })
    }

    private fun resetCustomAiModels() {
        setBusy(true, "Resetting AI models...")
        api.resetUserAiModels(object : ApiClient.ApiCallback<String> {
            override fun onSuccess(result: String) {
                setBusy(false, "")
                toast(result)
                loadAiSettings()
            }
            override fun onError(errorMessage: String) {
                setBusy(false, "")
                toast(errorMessage)
            }
        })
    }

    // -------------------------------------------------------------
    // Admin Panel Studio
    // -------------------------------------------------------------

    private fun loadAdminPanelData() {
        val u = session.currentUser
        if (u == null || !u.isAdmin) return

        // 1. Users
        api.getUsers(object : ApiClient.ApiCallback<List<User>> {
            override fun onSuccess(result: List<User>) { renderAdminUsers(result) }
            override fun onError(errorMessage: String) {}
        })

        // 2. Inquiries
        api.getContactInquiries(object : ApiClient.ApiCallback<List<ContactInquiry>> {
            override fun onSuccess(result: List<ContactInquiry>) { renderAdminInquiries(result) }
            override fun onError(errorMessage: String) {}
        })

        // 3. Blogs
        api.getAdminBlogs(object : ApiClient.ApiCallback<List<BlogPost>> {
            override fun onSuccess(result: List<BlogPost>) { renderAdminBlogs(result) }
            override fun onError(errorMessage: String) {}
        })

        // 4. FAQs
        api.getAdminFaqs(object : ApiClient.ApiCallback<List<FaqItem>> {
            override fun onSuccess(result: List<FaqItem>) { renderAdminFaqs(result) }
            override fun onError(errorMessage: String) {}
        })

        // 5. Backup
        api.getAdminBackupSettings(object : ApiClient.ApiCallback<Map<String, String>> {
            override fun onSuccess(result: Map<String, String>) {
                editBackupRepoOwner.setText(result["github_backup_repo_owner"] ?: "prakash111")
                editBackupRepoName.setText(result["github_backup_repo_name"] ?: "magic-ai")
                editBackupBranch.setText(result["github_backup_branch"] ?: "magicai")
                editBackupToken.setText(result["github_backup_token"] ?: "")
            }
            override fun onError(errorMessage: String) {}
        })
    }

    private fun renderAdminUsers(users: List<User>) {
        adminUsersContainer.removeAllViews()
        for (u in users) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = getDrawable(R.drawable.bg_input_field)
                setPadding(12, 10, 12, 10)
                val pms = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 0, 8)
                }
                layoutParams = pms
            }

            val titleTv = TextView(this).apply {
                text = "${u.username} (${u.email}) • [${u.userType.uppercase()}]"
                setTextColor(getColor(R.color.text_primary))
                textSize = 12f
                textStyle = android.graphics.Typeface.BOLD
            }
            row.addView(titleTv)

            val quotasTv = TextView(this).apply {
                text = "Decomp: ${u.decompileUsage}/${u.decompileLimit} | Comp: ${u.compileUsage}/${u.compileLimit} | Keys: ${u.generateKeyUsage}/${u.generateKeyLimit} | Sign: ${u.signApkUsage}/${u.signApkLimit}"
                setTextColor(getColor(R.color.text_muted))
                textSize = 10f
                setPadding(0, 2, 0, 6)
            }
            row.addView(quotasTv)

            val editBtn = Button(this, null, androidx.appcompat.R.attr.buttonStyle).apply {
                text = "Update Limits (+100)"
                textSize = 10f
                setBackgroundColor(getColor(R.color.primary))
                setTextColor(getColor(R.color.text_inverse))
                setOnClickListener {
                    api.updateLimits(u.id, u.decompileLimit + 100, u.compileLimit + 100, u.generateKeyLimit + 100, u.signApkLimit + 100, object : ApiClient.ApiCallback<String> {
                        override fun onSuccess(r: String) {
                            toast(r)
                            loadAdminPanelData()
                        }
                        override fun onError(e: String) { toast(e) }
                    })
                }
            }
            row.addView(editBtn)
            adminUsersContainer.addView(row)
        }
    }

    private fun renderAdminInquiries(inquiries: List<ContactInquiry>) {
        adminInquiriesContainer.removeAllViews()
        if (inquiries.isEmpty()) {
            val tv = TextView(this).apply {
                text = "No inquiries received."
                setTextColor(getColor(R.color.text_muted))
                textSize = 11f
            }
            adminInquiriesContainer.addView(tv)
            return
        }

        for (inq in inquiries) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = getDrawable(R.drawable.bg_input_field)
                setPadding(12, 10, 12, 10)
                val pms = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 0, 8)
                }
                layoutParams = pms
            }

            val titleTv = TextView(this).apply {
                text = "From: ${inq.name} <${inq.email}> • ${inq.subject}"
                setTextColor(getColor(R.color.text_primary))
                textSize = 12f
                textStyle = android.graphics.Typeface.BOLD
            }
            row.addView(titleTv)

            val msgTv = TextView(this).apply {
                text = inq.message
                setTextColor(getColor(R.color.text_secondary))
                textSize = 11f
                setPadding(0, 4, 0, 6)
            }
            row.addView(msgTv)

            val btnRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }

            if (!inq.isRead) {
                val markReadBtn = Button(this, null, androidx.appcompat.R.attr.buttonStyle).apply {
                    text = "Mark Read"
                    textSize = 10f
                    setBackgroundColor(getColor(R.color.primary))
                    setTextColor(getColor(R.color.text_inverse))
                    val pms = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 72).apply { marginEnd = 6 }
                    layoutParams = pms
                    setOnClickListener {
                        api.markContactInquiryRead(inq.id, object : ApiClient.ApiCallback<String> {
                            override fun onSuccess(r: String) { loadAdminPanelData() }
                            override fun onError(e: String) { toast(e) }
                        })
                    }
                }
                btnRow.addView(markReadBtn)
            }

            val deleteBtn = Button(this, null, androidx.appcompat.R.attr.buttonStyle).apply {
                text = "Delete"
                textSize = 10f
                setBackgroundColor(getColor(R.color.danger_light))
                setTextColor(getColor(R.color.danger))
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 72)
                setOnClickListener {
                    api.deleteContactInquiry(inq.id, object : ApiClient.ApiCallback<String> {
                        override fun onSuccess(r: String) { loadAdminPanelData() }
                        override fun onError(e: String) { toast(e) }
                    })
                }
            }
            btnRow.addView(deleteBtn)
            row.addView(btnRow)

            adminInquiriesContainer.addView(row)
        }
    }

    private fun renderAdminBlogs(blogs: List<BlogPost>) {
        adminBlogsContainer.removeAllViews()
        for (b in blogs) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                background = getDrawable(R.drawable.bg_input_field)
                setPadding(12, 10, 12, 10)
                val pms = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 0, 6)
                }
                layoutParams = pms
            }

            val titleTv = TextView(this).apply {
                text = "${b.title}\n[${b.category}] • ${b.readTime}"
                setTextColor(getColor(R.color.text_primary))
                textSize = 12f
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            row.addView(titleTv)

            val editBtn = Button(this, null, androidx.appcompat.R.attr.buttonStyle).apply {
                text = "Edit"
                textSize = 10f
                setBackgroundColor(getColor(R.color.primary))
                setTextColor(getColor(R.color.text_inverse))
                val pms = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 72).apply { marginEnd = 6 }
                layoutParams = pms
                setOnClickListener { showEditBlogDialog(b) }
            }
            row.addView(editBtn)

            val deleteBtn = Button(this, null, androidx.appcompat.R.attr.buttonStyle).apply {
                text = "Delete"
                textSize = 10f
                setBackgroundColor(getColor(R.color.danger_light))
                setTextColor(getColor(R.color.danger))
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 72)
                setOnClickListener {
                    api.deleteAdminBlog(b.id, object : ApiClient.ApiCallback<String> {
                        override fun onSuccess(r: String) { loadAdminPanelData() }
                        override fun onError(e: String) { toast(e) }
                    })
                }
            }
            row.addView(deleteBtn)

            adminBlogsContainer.addView(row)
        }
    }

    private fun renderAdminFaqs(faqs: List<FaqItem>) {
        adminFaqsContainer.removeAllViews()
        for (f in faqs) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                background = getDrawable(R.drawable.bg_input_field)
                setPadding(12, 10, 12, 10)
                val pms = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 0, 6)
                }
                layoutParams = pms
            }

            val titleTv = TextView(this).apply {
                text = "${f.question}\nCategory: ${f.category}"
                setTextColor(getColor(R.color.text_primary))
                textSize = 12f
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            row.addView(titleTv)

            val editBtn = Button(this, null, androidx.appcompat.R.attr.buttonStyle).apply {
                text = "Edit"
                textSize = 10f
                setBackgroundColor(getColor(R.color.primary))
                setTextColor(getColor(R.color.text_inverse))
                val pms = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 72).apply { marginEnd = 6 }
                layoutParams = pms
                setOnClickListener { showEditFaqDialog(f) }
            }
            row.addView(editBtn)

            val deleteBtn = Button(this, null, androidx.appcompat.R.attr.buttonStyle).apply {
                text = "Delete"
                textSize = 10f
                setBackgroundColor(getColor(R.color.danger_light))
                setTextColor(getColor(R.color.danger))
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 72)
                setOnClickListener {
                    api.deleteAdminFaq(f.id, object : ApiClient.ApiCallback<String> {
                        override fun onSuccess(r: String) { loadAdminPanelData() }
                        override fun onError(e: String) { toast(e) }
                    })
                }
            }
            row.addView(deleteBtn)

            adminFaqsContainer.addView(row)
        }
    }

    private fun saveAdminBackupSettings() {
        val map = mapOf(
            "github_backup_repo_owner" to editBackupRepoOwner.text.toString().trim(),
            "github_backup_repo_name" to editBackupRepoName.text.toString().trim(),
            "github_backup_branch" to editBackupBranch.text.toString().trim(),
            "github_backup_token" to editBackupToken.text.toString().trim(),
            "auto_backup_enabled" to "1",
            "auto_backup_frequency" to "weekly"
        )
        setBusy(true, "Saving GitHub backup configuration...")
        api.saveAdminBackupSettings(map, object : ApiClient.ApiCallback<String> {
            override fun onSuccess(result: String) {
                setBusy(false, "")
                toast(result)
            }
            override fun onError(errorMessage: String) {
                setBusy(false, "")
                toast(errorMessage)
            }
        })
    }

    private fun runAdminManualBackup() {
        setBusy(true, "Triggering instant GitHub backup...")
        api.runAdminManualBackup(object : ApiClient.ApiCallback<String> {
            override fun onSuccess(result: String) {
                setBusy(false, "")
                toast(result)
            }
            override fun onError(errorMessage: String) {
                setBusy(false, "")
                toast(errorMessage)
            }
        })
    }

    // -------------------------------------------------------------
    // Community, Blogs & FAQs Hub
    // -------------------------------------------------------------

    private fun loadPublicCommunityData() {
        api.getPublicBlogs(object : ApiClient.ApiCallback<List<BlogPost>> {
            override fun onSuccess(result: List<BlogPost>) { renderPublicBlogs(result) }
            override fun onError(errorMessage: String) {}
        })

        api.getFaqs(object : ApiClient.ApiCallback<List<FaqItem>> {
            override fun onSuccess(result: List<FaqItem>) { renderPublicFaqs(result) }
            override fun onError(errorMessage: String) {}
        })
    }

    private fun renderPublicBlogs(blogs: List<BlogPost>) {
        publicBlogsContainer.removeAllViews()
        for (b in blogs) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = getDrawable(R.drawable.bg_input_field)
                setPadding(16, 14, 16, 14)
                val pms = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 0, 10)
                }
                layoutParams = pms
            }

            val titleTv = TextView(this).apply {
                text = b.title
                setTextColor(getColor(R.color.primary))
                textSize = 14f
                textStyle = android.graphics.Typeface.BOLD
            }
            card.addView(titleTv)

            val metaTv = TextView(this).apply {
                text = "Category: ${b.category} • Read: ${b.readTime}"
                setTextColor(getColor(R.color.text_muted))
                textSize = 11f
                setPadding(0, 2, 0, 6)
            }
            card.addView(metaTv)

            val excerptTv = TextView(this).apply {
                text = b.excerpt
                setTextColor(getColor(R.color.text_secondary))
                textSize = 12f
            }
            card.addView(excerptTv)

            card.setOnClickListener {
                AlertDialog.Builder(this)
                    .setTitle(b.title)
                    .setMessage("${b.excerpt}\n\n${b.content}")
                    .setPositiveButton("Close", null)
                    .show()
            }

            publicBlogsContainer.addView(card)
        }
    }

    private fun renderPublicFaqs(faqs: List<FaqItem>) {
        publicFaqsContainer.removeAllViews()
        for (f in faqs) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = getDrawable(R.drawable.bg_input_field)
                setPadding(16, 12, 16, 12)
                val pms = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 0, 8)
                }
                layoutParams = pms
            }

            val qTv = TextView(this).apply {
                text = "❓ ${f.question}"
                setTextColor(getColor(R.color.text_primary))
                textSize = 13f
                textStyle = android.graphics.Typeface.BOLD
            }
            card.addView(qTv)

            val aTv = TextView(this).apply {
                text = f.answer
                setTextColor(getColor(R.color.text_secondary))
                textSize = 12f
                setPadding(0, 4, 0, 0)
            }
            card.addView(aTv)

            publicFaqsContainer.addView(card)
        }
    }

    private fun submitContactInquiryForm() {
        val name = editContactName.text.toString().trim()
        val email = editContactEmail.text.toString().trim()
        val subject = editContactSubject.text.toString().trim()
        val message = editContactMessage.text.toString().trim()

        if (name.isEmpty() || email.isEmpty() || subject.isEmpty() || message.isEmpty()) {
            toast("Please fill all contact fields.")
            return
        }

        setBusy(true, "Submitting inquiry to server...")
        api.submitContactInquiry(name, email, subject, message, object : ApiClient.ApiCallback<String> {
            override fun onSuccess(result: String) {
                setBusy(false, "")
                toast(result)
                editContactSubject.setText("")
                editContactMessage.setText("")
            }

            override fun onError(errorMessage: String) {
                setBusy(false, "")
                toast("Inquiry error: $errorMessage")
            }
        })
    }

    // -------------------------------------------------------------
    // Interactive Dialogs & QR Scan Flow
    // -------------------------------------------------------------

    private fun showAuthDialog(initialMode: String) {
        val dialog = Dialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_auth, null)
        dialog.setContentView(view)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        val txtTitle = view.findViewById<TextView>(R.id.txtAuthDialogTitle)
        val btnModeLogin = view.findViewById<Button>(R.id.btnAuthModeLogin)
        val btnModeReg = view.findViewById<Button>(R.id.btnAuthModeRegister)
        val btnModeForgot = view.findViewById<Button>(R.id.btnAuthModeForgot)
        val editEmail = view.findViewById<EditText>(R.id.editAuthEmail)
        val editUser = view.findViewById<EditText>(R.id.editAuthUsername)
        val editPass = view.findViewById<EditText>(R.id.editAuthPassword)
        val btnSubmit = view.findViewById<Button>(R.id.btnAuthSubmit)

        var mode = initialMode

        fun updateAuthMode(m: String) {
            mode = m
            when (m) {
                "login" -> {
                    txtTitle.text = "Account Login"
                    editEmail.visibility = View.GONE
                    editUser.visibility = View.VISIBLE
                    editPass.visibility = View.VISIBLE
                    btnSubmit.text = "Sign In"
                }
                "register" -> {
                    txtTitle.text = "Register Account"
                    editEmail.visibility = View.VISIBLE
                    editUser.visibility = View.VISIBLE
                    editPass.visibility = View.VISIBLE
                    btnSubmit.text = "Create Account"
                }
                "forgot" -> {
                    txtTitle.text = "Reset Password"
                    editEmail.visibility = View.VISIBLE
                    editUser.visibility = View.GONE
                    editPass.visibility = View.GONE
                    btnSubmit.text = "Send Reset Link"
                }
            }
        }

        updateAuthMode(initialMode)

        btnModeLogin.setOnClickListener { updateAuthMode("login") }
        btnModeReg.setOnClickListener { updateAuthMode("register") }
        btnModeForgot.setOnClickListener { updateAuthMode("forgot") }

        btnSubmit.setOnClickListener {
            when (mode) {
                "login" -> {
                    val u = editUser.text.toString().trim()
                    val p = editPass.text.toString().trim()
                    if (u.isEmpty() || p.isEmpty()) { toast("Enter credentials."); return@setOnClickListener }
                    setBusy(true, "Logging in...")
                    api.login(u, p, object : ApiClient.ApiCallback<User> {
                        override fun onSuccess(result: User) {
                            setBusy(false, "")
                            dialog.dismiss()
                            updateUiForAuthState()
                            toast("Welcome, ${result.username}!")
                            refreshDashboardData()
                        }
                        override fun onError(errorMessage: String) {
                            setBusy(false, "")
                            toast("Login failed: $errorMessage")
                        }
                    })
                }
                "register" -> {
                    val e = editEmail.text.toString().trim()
                    val u = editUser.text.toString().trim()
                    val p = editPass.text.toString().trim()
                    if (e.isEmpty() || u.isEmpty() || p.isEmpty()) { toast("Fill all fields."); return@setOnClickListener }
                    setBusy(true, "Creating account...")
                    api.register(e, u, p, object : ApiClient.ApiCallback<String> {
                        override fun onSuccess(result: String) {
                            setBusy(false, "")
                            dialog.dismiss()
                            toast(result)
                        }
                        override fun onError(errorMessage: String) {
                            setBusy(false, "")
                            toast("Registration error: $errorMessage")
                        }
                    })
                }
                "forgot" -> {
                    val e = editEmail.text.toString().trim()
                    if (e.isEmpty()) { toast("Enter email."); return@setOnClickListener }
                    setBusy(true, "Requesting password reset...")
                    api.requestPasswordReset(e, object : ApiClient.ApiCallback<String> {
                        override fun onSuccess(result: String) {
                            setBusy(false, "")
                            dialog.dismiss()
                            toast(result)
                        }
                        override fun onError(errorMessage: String) {
                            setBusy(false, "")
                            toast(errorMessage)
                        }
                    })
                }
            }
        }

        dialog.show()
    }

    private fun showUserAccountDialog() {
        val u = session.currentUser ?: return
        AlertDialog.Builder(this)
            .setTitle("User Profile (${u.username})")
            .setMessage("Email: ${u.email}\nRole: ${u.userType.uppercase()}\nVerified: ${if (u.emailVerified == 1) "Yes" else "No"}")
            .setPositiveButton("Logout") { _, _ ->
                session.clearAuth()
                updateUiForAuthState()
                toast("Logged out successfully.")
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showServerUrlDialog() {
        val dialog = Dialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_server_url, null)
        dialog.setContentView(view)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        val editUrl = view.findViewById<EditText>(R.id.editServerUrlInput)
        val btnSave = view.findViewById<Button>(R.id.btnSaveServerUrl)

        editUrl.setText(session.serverUrl)

        btnSave.setOnClickListener {
            val url = editUrl.text.toString().trim()
            if (url.isNotEmpty()) {
                session.serverUrl = url
                updateUiForAuthState()
                dialog.dismiss()
                toast("Connected to: $url")
                refreshDashboardData()
            }
        }

        dialog.show()
    }

    private fun showCreateUserDialog() {
        val dialog = Dialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_create_user, null)
        dialog.setContentView(view)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        val editEmail = view.findViewById<EditText>(R.id.editNewUserEmail)
        val editUser = view.findViewById<EditText>(R.id.editNewUserUsername)
        val editPass = view.findViewById<EditText>(R.id.editNewUserPassword)
        val editD = view.findViewById<EditText>(R.id.editNewUserDecompileLimit)
        val editC = view.findViewById<EditText>(R.id.editNewUserCompileLimit)
        val editK = view.findViewById<EditText>(R.id.editNewUserKeygenLimit)
        val editS = view.findViewById<EditText>(R.id.editNewUserSignLimit)
        val btnSubmit = view.findViewById<Button>(R.id.btnSubmitCreateUser)

        btnSubmit.setOnClickListener {
            val map = mapOf(
                "email" to editEmail.text.toString().trim(),
                "username" to editUser.text.toString().trim(),
                "password" to editPass.text.toString().trim(),
                "user_type" to "user",
                "decompile_limit" to editD.text.toString().trim(),
                "compile_limit" to editC.text.toString().trim(),
                "generate_key_limit" to editK.text.toString().trim(),
                "sign_apk_limit" to editS.text.toString().trim()
            )
            setBusy(true, "Creating user...")
            api.createUser(map, object : ApiClient.ApiCallback<String> {
                override fun onSuccess(result: String) {
                    setBusy(false, "")
                    dialog.dismiss()
                    toast(result)
                    loadAdminPanelData()
                }
                override fun onError(errorMessage: String) {
                    setBusy(false, "")
                    toast("Create user error: $errorMessage")
                }
            })
        }

        dialog.show()
    }

    private fun showCreateKeystoreDialog() {
        val dialog = Dialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_create_keystore, null)
        dialog.setContentView(view)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        val editAlias = view.findViewById<EditText>(R.id.editKeystoreAlias)
        val editPass = view.findViewById<EditText>(R.id.editKeystorePassword)
        val btnSubmit = view.findViewById<Button>(R.id.btnSubmitCreateKeystore)

        btnSubmit.setOnClickListener {
            val a = editAlias.text.toString().trim()
            val p = editPass.text.toString().trim()
            if (a.isEmpty() || p.isEmpty()) { toast("Alias and password required."); return@setOnClickListener }
            setBusy(true, "Generating 2048-bit RSA Keystore...")
            api.createKeystore(a, p, object : ApiClient.ApiCallback<JSONObject> {
                override fun onSuccess(result: JSONObject) {
                    setBusy(false, "")
                    dialog.dismiss()
                    toast("Keystore generated successfully!")
                    loadKeystoresList()
                }
                override fun onError(errorMessage: String) {
                    setBusy(false, "")
                    toast("Keystore error: $errorMessage")
                }
            })
        }

        dialog.show()
    }

    private fun showEditBlogDialog(existing: BlogPost?) {
        val dialog = Dialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_edit_blog, null)
        dialog.setContentView(view)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        val txtTitle = view.findViewById<TextView>(R.id.txtBlogDialogTitle)
        val editTitle = view.findViewById<EditText>(R.id.editBlogTitle)
        val editCat = view.findViewById<EditText>(R.id.editBlogCategory)
        val editRead = view.findViewById<EditText>(R.id.editBlogReadTime)
        val editTags = view.findViewById<EditText>(R.id.editBlogTags)
        val editExcerpt = view.findViewById<EditText>(R.id.editBlogExcerpt)
        val editContent = view.findViewById<EditText>(R.id.editBlogContent)
        val btnSubmit = view.findViewById<Button>(R.id.btnSubmitBlog)

        if (existing != null) {
            txtTitle.text = "Edit Blog Post"
            editTitle.setText(existing.title)
            editCat.setText(existing.category)
            editRead.setText(existing.readTime)
            editTags.setText(existing.tags)
            editExcerpt.setText(existing.excerpt)
            editContent.setText(existing.content)
            btnSubmit.text = "Update Post"
        }

        btnSubmit.setOnClickListener {
            val map = mapOf(
                "id" to (existing?.id?.toString() ?: "0"),
                "title" to editTitle.text.toString().trim(),
                "category" to editCat.text.toString().trim(),
                "read_time" to editRead.text.toString().trim(),
                "tags" to editTags.text.toString().trim(),
                "excerpt" to editExcerpt.text.toString().trim(),
                "content" to editContent.text.toString().trim()
            )
            setBusy(true, "Saving blog post...")
            api.saveAdminBlog(map, object : ApiClient.ApiCallback<String> {
                override fun onSuccess(result: String) {
                    setBusy(false, "")
                    dialog.dismiss()
                    toast(result)
                    loadAdminPanelData()
                }
                override fun onError(errorMessage: String) {
                    setBusy(false, "")
                    toast("Blog error: $errorMessage")
                }
            })
        }

        dialog.show()
    }

    private fun showEditFaqDialog(existing: FaqItem?) {
        val dialog = Dialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_edit_faq, null)
        dialog.setContentView(view)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        val editQ = view.findViewById<EditText>(R.id.editFaqQuestion)
        val editCat = view.findViewById<EditText>(R.id.editFaqCategory)
        val editA = view.findViewById<EditText>(R.id.editFaqAnswer)
        val btnSubmit = view.findViewById<Button>(R.id.btnSubmitFaq)

        if (existing != null) {
            editQ.setText(existing.question)
            editCat.setText(existing.category)
            editA.setText(existing.answer)
        }

        btnSubmit.setOnClickListener {
            val map = mapOf(
                "id" to (existing?.id?.toString() ?: "0"),
                "question" to editQ.text.toString().trim(),
                "category" to editCat.text.toString().trim(),
                "answer" to editA.text.toString().trim()
            )
            setBusy(true, "Saving FAQ...")
            api.saveAdminFaq(map, object : ApiClient.ApiCallback<String> {
                override fun onSuccess(result: String) {
                    setBusy(false, "")
                    dialog.dismiss()
                    toast(result)
                    loadAdminPanelData()
                }
                override fun onError(errorMessage: String) {
                    setBusy(false, "")
                    toast("FAQ error: $errorMessage")
                }
            })
        }

        dialog.show()
    }

    private fun showEditStringDialog(key: String, currentVal: String) {
        val dialog = Dialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_edit_string, null)
        dialog.setContentView(view)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        val txtKey = view.findViewById<TextView>(R.id.txtStringKeyName)
        val editVal = view.findViewById<EditText>(R.id.editStringValInput)
        val btnSubmit = view.findViewById<Button>(R.id.btnSubmitStringVal)

        txtKey.text = "String: @string/$key"
        editVal.setText(currentVal)

        btnSubmit.setOnClickListener {
            val newVal = editVal.text.toString()
            loadedStringsMap[key] = newVal
            renderStringsTable(loadedStringsMap)
            dialog.dismiss()
            toast("Updated string locally. Tap Save to sync.")
        }

        dialog.show()
    }

    private fun showRenameProjectDialog(projectId: String, currentName: String) {
        val input = EditText(this).apply { setText(currentName) }
        AlertDialog.Builder(this)
            .setTitle("Rename Project")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    api.renameProject(projectId, newName, object : ApiClient.ApiCallback<String> {
                        override fun onSuccess(r: String) {
                            toast(r)
                            refreshProjectsList()
                        }
                        override fun onError(e: String) { toast(e) }
                    })
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun launchQrScanner() {
        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt("Scan project or server QR code from web app")
            setBeepEnabled(false)
            setOrientationLocked(true)
            setCaptureActivity(PortraitCaptureActivity::class.java)
        }
        qrScanLauncher.launch(options)
    }

    private fun handleScannedCode(raw: String) {
        try {
            val decoded = String(Base64.decode(raw, Base64.DEFAULT))
            val json = JSONObject(decoded)
            val url = json.getString("url")
            val tok = json.getString("token")
            val name = json.optString("name", "app")

            session.serverUrl = url
            session.pairingToken = tok
            session.pairedProjectName = name
            updateUiForAuthState()
            toast("Paired with project '$name'")
            fetchCloudLogsOnce()
        } catch (e: Exception) {
            // Direct URL fallback
            if (raw.startsWith("http://") || raw.startsWith("https://")) {
                session.serverUrl = raw
                updateUiForAuthState()
                toast("Server host updated.")
            } else {
                toast("Unrecognized QR code format.")
            }
        }
    }

    // -------------------------------------------------------------
    // Utility Helpers
    // -------------------------------------------------------------

    private fun setBusy(busy: Boolean, message: String) {
        globalProgressBar.visibility = if (busy) View.VISIBLE else View.GONE
        if (busy) globalProgressBar.isIndeterminate = true
        if (message.isNotEmpty()) {
            topBarSubtitle.text = message
        } else {
            topBarSubtitle.text = "Host: ${session.serverUrl}"
        }
    }

    private fun copyUriToTemp(uri: Uri, outputName: String): File? {
        return try {
            val dir = File(cacheDir, "uploads").apply { mkdirs() }
            val out = File(dir, outputName)
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(out).use { output ->
                    input.copyTo(output)
                }
            }
            out
        } catch (e: Exception) {
            null
        }
    }

    private fun formatSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 * 1024 -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
            bytes >= 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}

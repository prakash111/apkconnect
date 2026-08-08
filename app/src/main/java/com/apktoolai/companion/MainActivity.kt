package com.apktoolai.companion

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.apktoolai.companion.api.*
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.URLDecoder

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

    // Auth Hub (Shown when user is NOT logged in)
    private lateinit var panelAuthHub: ScrollView
    private lateinit var tabAuthSignIn: Button
    private lateinit var tabAuthRegister: Button
    private lateinit var tabAuthForgot: Button
    private lateinit var tabAuthQr: Button
    private lateinit var tabAuthSupport: Button

    private lateinit var cardAuthSignIn: LinearLayout
    private lateinit var cardAuthRegister: LinearLayout
    private lateinit var cardAuthForgot: LinearLayout
    private lateinit var cardAuthQr: LinearLayout
    private lateinit var cardAuthSupport: LinearLayout

    private lateinit var editHubLoginUser: EditText
    private lateinit var editHubLoginPass: EditText
    private lateinit var btnHubTogglePass: ImageButton
    private lateinit var btnHubSubmitLogin: Button
    private lateinit var btnHubGoRegister: Button
    private lateinit var btnHubGoForgot: Button

    private lateinit var editHubRegEmail: EditText
    private lateinit var editHubRegPhone: EditText
    private lateinit var editHubRegUser: EditText
    private lateinit var editHubRegPass: EditText
    private lateinit var btnHubSubmitRegister: Button
    private lateinit var btnHubGoLogin: Button

    private lateinit var editHubForgotEmail: EditText
    private lateinit var btnHubSubmitForgot: Button
    private lateinit var btnHubBackLogin: Button

    private lateinit var btnHubLaunchScanQr: Button
    private lateinit var editHubManualToken: EditText
    private lateinit var btnHubSubmitToken: Button
    private lateinit var hubFaqsContainer: LinearLayout

    // Studio Navigation & Panels Container (Shown when user IS logged in)
    private lateinit var studioNavScroll: HorizontalScrollView
    private lateinit var studioPanelsContainer: FrameLayout
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

    private var isPasswordVisible = false

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

    private val requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            startQrScannerActivity()
        } else {
            toast("Camera permission is required to scan QR codes. You can also paste the pairing code manually.")
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

        // Initial Data Load
        if (session.isAuthenticated()) {
            refreshDashboardData()
            switchTab("dashboard")
        } else {
            loadPublicCommunityData()
            switchAuthHubTab("signin")
        }
    }

    override fun onResume() {
        super.onResume()
        if (session.isAuthenticated()) {
            startAutoCheckTimer()
            if (cloudLogsStreaming) {
                resumeCloudLogsPolling()
            }
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

        // Auth Hub
        panelAuthHub = findViewById(R.id.panelAuthHub)
        tabAuthSignIn = findViewById(R.id.tabAuthSignIn)
        tabAuthRegister = findViewById(R.id.tabAuthRegister)
        tabAuthForgot = findViewById(R.id.tabAuthForgot)
        tabAuthQr = findViewById(R.id.tabAuthQr)
        tabAuthSupport = findViewById(R.id.tabAuthSupport)

        cardAuthSignIn = findViewById(R.id.cardAuthSignIn)
        cardAuthRegister = findViewById(R.id.cardAuthRegister)
        cardAuthForgot = findViewById(R.id.cardAuthForgot)
        cardAuthQr = findViewById(R.id.cardAuthQr)
        cardAuthSupport = findViewById(R.id.cardAuthSupport)

        editHubLoginUser = findViewById(R.id.editHubLoginUser)
        editHubLoginPass = findViewById(R.id.editHubLoginPass)
        btnHubTogglePass = findViewById(R.id.btnHubTogglePass)
        btnHubSubmitLogin = findViewById(R.id.btnHubSubmitLogin)
        btnHubGoRegister = findViewById(R.id.btnHubGoRegister)
        btnHubGoForgot = findViewById(R.id.btnHubGoForgot)

        editHubRegEmail = findViewById(R.id.editHubRegEmail)
        editHubRegPhone = findViewById(R.id.editHubRegPhone)
        editHubRegUser = findViewById(R.id.editHubRegUser)
        editHubRegPass = findViewById(R.id.editHubRegPass)
        btnHubSubmitRegister = findViewById(R.id.btnHubSubmitRegister)
        btnHubGoLogin = findViewById(R.id.btnHubGoLogin)

        editHubForgotEmail = findViewById(R.id.editHubForgotEmail)
        btnHubSubmitForgot = findViewById(R.id.btnHubSubmitForgot)
        btnHubBackLogin = findViewById(R.id.btnHubBackLogin)

        btnHubLaunchScanQr = findViewById(R.id.btnHubLaunchScanQr)
        editHubManualToken = findViewById(R.id.editHubManualToken)
        btnHubSubmitToken = findViewById(R.id.btnHubSubmitToken)
        hubFaqsContainer = findViewById(R.id.hubFaqsContainer)

        // Studio Navigation & Container
        studioNavScroll = findViewById(R.id.studioNavScroll)
        studioPanelsContainer = findViewById(R.id.studioPanelsContainer)
        tabBtnDashboard = findViewById(R.id.tabBtnDashboard)
        tabBtnProjects = findViewById(R.id.tabBtnProjects)
        tabBtnEditor = findViewById(R.id.tabBtnEditor)
        tabBtnCustomizer = findViewById(R.id.tabBtnCustomizer)
        tabBtnBuild = findViewById(R.id.tabBtnBuild)
        tabBtnDebugger = findViewById(R.id.tabBtnDebugger)
        tabBtnAiSettings = findViewById(R.id.tabBtnAiSettings)
        tabBtnAdmin = findViewById(R.id.tabBtnAdmin)
        tabBtnCommunity = findViewById(R.id.tabBtnCommunity)

        // Studio Panels
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
                panelAuthHub.visibility = View.VISIBLE
                studioNavScroll.visibility = View.GONE
                studioPanelsContainer.visibility = View.GONE
                switchAuthHubTab("signin")
            }
        }
        btnBannerInstall.setOnClickListener { handleDownloadAndInstallApk() }

        // Auth Hub Navigation
        tabAuthSignIn.setOnClickListener { switchAuthHubTab("signin") }
        tabAuthRegister.setOnClickListener { switchAuthHubTab("register") }
        tabAuthForgot.setOnClickListener { switchAuthHubTab("forgot") }
        tabAuthQr.setOnClickListener { switchAuthHubTab("qr") }
        tabAuthSupport.setOnClickListener { switchAuthHubTab("support") }

        // Auth Hub Form Submissions
        btnHubSubmitLogin.setOnClickListener { handleHubLogin() }
        btnHubGoRegister.setOnClickListener { switchAuthHubTab("register") }
        btnHubGoForgot.setOnClickListener { switchAuthHubTab("forgot") }

        btnHubTogglePass.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                editHubLoginPass.transformationMethod = HideReturnsTransformationMethod.getInstance()
                btnHubTogglePass.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            } else {
                editHubLoginPass.transformationMethod = PasswordTransformationMethod.getInstance()
                btnHubTogglePass.setImageResource(android.R.drawable.ic_menu_view)
            }
            editHubLoginPass.setSelection(editHubLoginPass.text.length)
        }

        btnHubSubmitRegister.setOnClickListener { handleHubRegister() }
        btnHubGoLogin.setOnClickListener { switchAuthHubTab("signin") }

        btnHubSubmitForgot.setOnClickListener { handleHubForgot() }
        btnHubBackLogin.setOnClickListener { switchAuthHubTab("signin") }

        btnHubLaunchScanQr.setOnClickListener { launchQrScanner() }
        btnHubSubmitToken.setOnClickListener {
            val tok = editHubManualToken.text.toString().trim()
            if (tok.isEmpty()) {
                toast("Please enter or paste a pairing code.")
            } else {
                handleScannedCode(tok)
            }
        }

        // Studio Navigation Tabs
        tabBtnDashboard.setOnClickListener { checkAuthAndSwitchTab("dashboard") }
        tabBtnProjects.setOnClickListener { checkAuthAndSwitchTab("projects") }
        tabBtnEditor.setOnClickListener { checkAuthAndSwitchTab("editor") }
        tabBtnCustomizer.setOnClickListener { checkAuthAndSwitchTab("customizer") }
        tabBtnBuild.setOnClickListener { checkAuthAndSwitchTab("build") }
        tabBtnDebugger.setOnClickListener { checkAuthAndSwitchTab("debugger") }
        tabBtnAiSettings.setOnClickListener { checkAuthAndSwitchTab("ai_settings") }
        tabBtnAdmin.setOnClickListener { checkAuthAndSwitchTab("admin") }
        tabBtnCommunity.setOnClickListener { checkAuthAndSwitchTab("community") }

        // Quick Actions
        btnQuickUploadDecompile.setOnClickListener { checkAuthAndSwitchTab("projects") }
        btnQuickBuildApk.setOnClickListener {
            if (session.isAuthenticated()) {
                switchTab("build")
                triggerBuildApk()
            } else {
                promptLoginRequired()
            }
        }
        btnQuickSignApk.setOnClickListener { checkAuthAndSwitchTab("build") }
        btnQuickAiFix.setOnClickListener {
            if (session.isAuthenticated()) {
                switchTab("customizer")
                triggerAiFix()
            } else {
                promptLoginRequired()
            }
        }

        btnDashCloseProject.setOnClickListener { closeCurrentProject() }
        btnDashExploreFiles.setOnClickListener { checkAuthAndSwitchTab("editor"); loadDirectory("") }
        btnDashEditStrings.setOnClickListener { checkAuthAndSwitchTab("customizer"); loadStringsForLocale("values") }
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
                override fun onSuccess(result: String) { toast("Provider updated: $prov") }
                override fun onError(errorMessage: String) { toast("Error: $errorMessage") }
            })
        }
        btnSaveGeminiKey.setOnClickListener { saveGeminiApiKey() }
        btnDeleteGeminiKey.setOnClickListener { deleteGeminiApiKey() }
        btnSaveOpenAiKey.setOnClickListener { saveOpenAiApiKey() }
        btnDeleteOpenAiKey.setOnClickListener { deleteOpenAiApiKey() }
        btnSaveCustomModels.setOnClickListener { saveCustomModels() }
        btnResetCustomModels.setOnClickListener { resetCustomModels() }

        // Admin
        btnAdminCreateUserDialog.setOnClickListener { showCreateUserDialog() }
        btnAdminCreateBlogDialog.setOnClickListener { showEditBlogDialog(null) }
        btnAdminCreateFaqDialog.setOnClickListener { showEditFaqDialog(null) }
        btnSaveBackupSettings.setOnClickListener { saveGitHubBackupSettings() }
        btnRunManualBackup.setOnClickListener { runManualBackup() }

        // Community & Support
        btnSubmitContactInquiry.setOnClickListener { submitContactInquiryForm() }
    }

    // -------------------------------------------------------------
    // Auth Hub Tab Switcher
    // -------------------------------------------------------------

    private fun switchAuthHubTab(tab: String) {
        val tabButtons = listOf(
            Pair("signin", Pair(tabAuthSignIn, cardAuthSignIn)),
            Pair("register", Pair(tabAuthRegister, cardAuthRegister)),
            Pair("forgot", Pair(tabAuthForgot, cardAuthForgot)),
            Pair("qr", Pair(tabAuthQr, cardAuthQr)),
            Pair("support", Pair(tabAuthSupport, cardAuthSupport))
        )

        for ((key, pair) in tabButtons) {
            val (btn, card) = pair
            val isActive = key == tab
            card.visibility = if (isActive) View.VISIBLE else View.GONE
            if (isActive) {
                btn.setBackgroundColor(getColor(R.color.primary))
                btn.setTextColor(getColor(R.color.text_inverse))
            } else {
                btn.setBackgroundColor(getColor(R.color.surface_dark_border))
                btn.setTextColor(getColor(R.color.text_secondary))
            }
        }

        if (tab == "support") {
            loadPublicCommunityData()
        }
    }

    private fun handleHubLogin() {
        editHubLoginUser.error = null
        editHubLoginPass.error = null

        val u = editHubLoginUser.text.toString().trim()
        val p = editHubLoginPass.text.toString().trim()

        var hasError = false
        if (u.isEmpty()) {
            editHubLoginUser.error = "Enter your username or email"
            hasError = true
        }
        if (p.isEmpty()) {
            editHubLoginPass.error = "Enter your password"
            hasError = true
        }
        if (hasError) {
            toast("Please enter your username and password.")
            return
        }

        btnHubSubmitLogin.isEnabled = false
        btnHubSubmitLogin.text = "Signing in..."
        setBusy(true, "Signing in to Studio...")
        api.login(u, p, object : ApiClient.ApiCallback<User> {
            override fun onSuccess(result: User) {
                setBusy(false, "")
                btnHubSubmitLogin.isEnabled = true
                btnHubSubmitLogin.text = "Sign In & Unlock Studio"
                updateUiForAuthState()
                toast("Welcome, ${result.username}!")
                switchTab("dashboard")
                refreshDashboardData()
            }

            override fun onError(errorMessage: String) {
                setBusy(false, "")
                btnHubSubmitLogin.isEnabled = true
                btnHubSubmitLogin.text = "Sign In & Unlock Studio"
                toast("Login failed: $errorMessage")
            }
        })
    }

    private fun handleHubRegister() {
        editHubRegEmail.error = null
        editHubRegPhone.error = null
        editHubRegUser.error = null
        editHubRegPass.error = null

        val e = editHubRegEmail.text.toString().trim()
        val ph = editHubRegPhone.text.toString().trim()
        val u = editHubRegUser.text.toString().trim()
        val p = editHubRegPass.text.toString().trim()

        var hasError = false
        if (e.isEmpty()) { editHubRegEmail.error = "Email is required"; hasError = true }
        if (ph.isEmpty()) { editHubRegPhone.error = "Mobile number is required"; hasError = true }
        if (u.isEmpty()) { editHubRegUser.error = "Username is required"; hasError = true }
        if (p.isEmpty()) { editHubRegPass.error = "Password is required"; hasError = true }
        else if (p.length < 6) { editHubRegPass.error = "Use at least 6 characters"; hasError = true }
        if (hasError) {
            toast("Please fill all registration fields.")
            return
        }

        btnHubSubmitRegister.isEnabled = false
        btnHubSubmitRegister.text = "Creating account..."
        setBusy(true, "Creating account...")
        api.register(e, ph, u, p, object : ApiClient.ApiCallback<String> {
            override fun onSuccess(result: String) {
                setBusy(false, "")
                btnHubSubmitRegister.isEnabled = true
                btnHubSubmitRegister.text = "Register & Create Account"
                toast(result)
                switchAuthHubTab("signin")
                editHubLoginUser.setText(u)
                editHubLoginPass.setText(p)
            }

            override fun onError(errorMessage: String) {
                setBusy(false, "")
                btnHubSubmitRegister.isEnabled = true
                btnHubSubmitRegister.text = "Register & Create Account"
                toast("Registration error: $errorMessage")
            }
        })
    }

    private fun handleHubForgot() {
        editHubForgotEmail.error = null
        val e = editHubForgotEmail.text.toString().trim()
        if (e.isEmpty()) {
            editHubForgotEmail.error = "Enter your registered email"
            toast("Please enter your email address.")
            return
        }

        btnHubSubmitForgot.isEnabled = false
        btnHubSubmitForgot.text = "Sending..."
        setBusy(true, "Requesting password reset...")
        api.requestPasswordReset(e, object : ApiClient.ApiCallback<String> {
            override fun onSuccess(result: String) {
                setBusy(false, "")
                btnHubSubmitForgot.isEnabled = true
                btnHubSubmitForgot.text = "Send Password Reset Link"
                toast(result)
                switchAuthHubTab("signin")
            }

            override fun onError(errorMessage: String) {
                setBusy(false, "")
                btnHubSubmitForgot.isEnabled = true
                btnHubSubmitForgot.text = "Send Password Reset Link"
                toast(errorMessage)
            }
        })
    }

    // -------------------------------------------------------------
    // Tab Navigation Switcher & Auth Protection
    // -------------------------------------------------------------

    private fun checkAuthAndSwitchTab(tabKey: String) {
        if (!session.isAuthenticated() && tabKey != "community") {
            promptLoginRequired()
            return
        }
        switchTab(tabKey)
    }

    private fun promptLoginRequired() {
        panelAuthHub.visibility = View.VISIBLE
        studioNavScroll.visibility = View.GONE
        studioPanelsContainer.visibility = View.GONE
        switchAuthHubTab("signin")
        toast("Please log in to access this feature.")
    }

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
            panelAuthHub.visibility = View.GONE
            studioNavScroll.visibility = View.VISIBLE
            studioPanelsContainer.visibility = View.VISIBLE

            btnTopAuth.text = user.username
            btnTopAuth.setBackgroundColor(getColor(R.color.primary_dark))
            txtDashWelcome.text = "Welcome back, ${user.username}!"
            txtDashUserEmail.text = "Logged in as ${user.email} (${user.userType.uppercase()})"
            txtAdminBadge.visibility = if (user.isAdmin) View.VISIBLE else View.GONE
            tabBtnAdmin.visibility = if (user.isAdmin) View.VISIBLE else View.GONE
            updateLimitsDisplay(user.decompileUsage, user.decompileLimit, user.compileUsage, user.compileLimit, user.generateKeyUsage, user.generateKeyLimit, user.signApkUsage, user.signApkLimit)
        } else {
            panelAuthHub.visibility = View.VISIBLE
            studioNavScroll.visibility = View.GONE
            studioPanelsContainer.visibility = View.GONE

            btnTopAuth.text = "Login"
            btnTopAuth.setBackgroundColor(getColor(R.color.primary))
            txtDashWelcome.text = "Welcome to APK Tool Studio"
            txtDashUserEmail.text = "Sign in to access all studio features"
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
                background = ContextCompat.getDrawable(this@MainActivity, R.drawable.bg_card_rounded)
                setPadding(24, 20, 24, 20)
                val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                params.setMargins(0, 0, 0, 16)
                layoutParams = params
            }

            val titleRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
            }

            val nameTv = TextView(this).apply {
                text = p.projectName
                setTextColor(getColor(R.color.text_primary))
                textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            titleRow.addView(nameTv)

            if (p.projectId == session.currentProjectId) {
                val activeBadge = TextView(this).apply {
                    text = "ACTIVE"
                    background = ContextCompat.getDrawable(this@MainActivity, R.drawable.bg_badge_success)
                    setTextColor(getColor(R.color.success))
                    textSize = 9f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setPadding(12, 4, 12, 4)
                }
                titleRow.addView(activeBadge)
            }

            card.addView(titleRow)

            val metaTv = TextView(this).apply {
                text = "Created: ${p.createdAt ?: "recent"} • Token: ${p.crashReportToken?.take(8) ?: "N/A"}"
                setTextColor(getColor(R.color.text_muted))
                textSize = 10f
                setPadding(0, 4, 0, 12)
            }
            card.addView(metaTv)

            val actionsRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            val btnOpen = Button(this, null, 0, com.google.android.material.R.style.Widget_MaterialComponents_Button_UnelevatedButton).apply {
                text = "Open in Studio"
                textSize = 10f
                setBackgroundColor(getColor(R.color.primary))
                setTextColor(getColor(R.color.text_inverse))
                val lp = LinearLayout.LayoutParams(0, 90, 1f).apply { marginEnd = 8 }
                layoutParams = lp
                setOnClickListener { selectActiveProject(p.projectId) }
            }
            actionsRow.addView(btnOpen)

            val btnDelete = Button(this, null, 0, com.google.android.material.R.style.Widget_MaterialComponents_Button_UnelevatedButton).apply {
                text = "Delete"
                textSize = 10f
                setBackgroundColor(getColor(R.color.danger_container))
                setTextColor(getColor(R.color.danger))
                val lp = LinearLayout.LayoutParams(0, 90, 1f)
                layoutParams = lp
                setOnClickListener { deleteProject(p.projectId) }
            }
            actionsRow.addView(btnDelete)

            card.addView(actionsRow)
            container.addView(card)
        }
    }

    private fun selectActiveProject(projectId: String) {
        setBusy(true, "Selecting project: $projectId...")
        api.selectProject(projectId, object : ApiClient.ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                setBusy(false, "")
                session.currentProjectId = projectId
                session.pairedProjectName = result.optString("project_name", projectId)
                updateUiForAuthState()
                toast("Active project switched to: $projectId")
                refreshDashboardData()
            }
            override fun onError(errorMessage: String) {
                setBusy(false, "")
                toast("Error selecting project: $errorMessage")
            }
        })
    }

    private fun deleteProject(projectId: String) {
        AlertDialog.Builder(this)
            .setTitle("Delete Project")
            .setMessage("Are you sure you want to completely delete project '$projectId' and all decompiled smali sources from the server?")
            .setPositiveButton("Delete Permanently") { _, _ ->
                setBusy(true, "Deleting project...")
                api.deleteProject(projectId, object : ApiClient.ApiCallback<JSONObject> {
                    override fun onSuccess(result: JSONObject) {
                        setBusy(false, "")
                        toast(result.optString("message", "Project deleted."))
                        if (session.currentProjectId == projectId) {
                            session.currentProjectId = null
                            updateUiForAuthState()
                        }
                        refreshProjectsList()
                    }
                    override fun onError(errorMessage: String) {
                        setBusy(false, "")
                        toast("Failed to delete: $errorMessage")
                    }
                })
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun closeCurrentProject() {
        session.currentProjectId = null
        session.pairedProjectName = null
        updateUiForAuthState()
        toast("Closed active project.")
    }

    // -------------------------------------------------------------
    // APK Upload & Decompilation
    // -------------------------------------------------------------

    private fun uploadAndDecompileSelectedApk() {
        val file = pendingUploadApkFile
        if (file == null || !file.exists()) {
            toast("Please pick an APK file first.")
            return
        }

        setBusy(true, "Uploading and decompiling ${file.name}...")
        boxDecompileLogs.visibility = View.VISIBLE
        txtDecompileStatus.text = "Decompiling ${file.name} on server..."
        txtDecompileLogOutput.text = "Starting multi-threaded apktool decompiler...\nExtracting AndroidManifest.xml and smali bytecode..."

        api.uploadAndDecompile(file, object : ApiClient.ProgressCallback {
            override fun onProgress(percentage: Int, message: String) {
                txtDecompileStatus.text = "Decompiling: $percentage% - $message"
            }
        }, object : ApiClient.ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                setBusy(false, "")
                txtDecompileStatus.text = "Decompilation Successful!"
                txtDecompileLogOutput.text = result.optString("log", "APK decompiled successfully.")
                val pid = result.optString("project_id", "")
                if (pid.isNotEmpty()) {
                    session.currentProjectId = pid
                    session.pairedProjectName = file.nameWithoutExtension
                    updateUiForAuthState()
                }
                toast("APK decompiled successfully!")
                refreshProjectsList()
            }

            override fun onError(errorMessage: String) {
                setBusy(false, "")
                txtDecompileStatus.text = "Decompilation Failed"
                txtDecompileLogOutput.text = errorMessage
                toast("Decompile error: $errorMessage")
            }
        })
    }

    // -------------------------------------------------------------
    // Smali / Code / Hex Editor Logic
    // -------------------------------------------------------------

    private fun loadDirectory(relPath: String) {
        currentDirPath = relPath
        txtFileBreadcrumb.text = if (relPath.isEmpty()) "/" else "/$relPath"
        setBusy(true, "Reading directory: $relPath...")
        api.getFiles(relPath, object : ApiClient.ApiCallback<List<ProjectFile>> {
            override fun onSuccess(result: List<ProjectFile>) {
                setBusy(false, "")
                renderDirectoryItems(result)
            }
            override fun onError(errorMessage: String) {
                setBusy(false, "")
                toast("Failed to list files: $errorMessage")
            }
        })
    }

    private fun renderDirectoryItems(files: List<ProjectFile>) {
        directoryItemsContainer.removeAllViews()
        if (files.isEmpty()) {
            val tv = TextView(this).apply {
                text = "Empty folder"
                setTextColor(getColor(R.color.text_muted))
                textSize = 12f
                setPadding(12, 12, 12, 12)
            }
            directoryItemsContainer.addView(tv)
            return
        }

        for (f in files) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(12, 12, 12, 12)
                background = ContextCompat.getDrawable(this@MainActivity, R.drawable.bg_input_field)
                val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 0, 8)
                }
                layoutParams = params
            }

            val iconTv = TextView(this).apply {
                text = if (f.isDir) "📁" else when {
                    f.name.endsWith(".smali") -> "⚙️"
                    f.name.endsWith(".xml") -> "📄"
                    f.name.endsWith(".json") -> "📦"
                    f.name.endsWith(".so") -> "🔩"
                    else -> "📝"
                }
                textSize = 14f
                setPadding(0, 0, 12, 0)
            }
            row.addView(iconTv)

            val nameTv = TextView(this).apply {
                text = f.name
                setTextColor(getColor(R.color.text_primary))
                textSize = 12f
                setTypeface(android.graphics.Typeface.MONOSPACE)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            row.addView(nameTv)

            val sizeTv = TextView(this).apply {
                text = if (f.isDir) "DIR" else formatSize(f.size)
                setTextColor(getColor(R.color.text_muted))
                textSize = 10f
            }
            row.addView(sizeTv)

            row.setOnClickListener {
                if (f.isDir) {
                    loadDirectory(f.path)
                } else {
                    openEditorForFile(f.path)
                }
            }

            directoryItemsContainer.addView(row)
        }
    }

    private fun navigateUpDirectory() {
        if (currentDirPath.isEmpty()) return
        val idx = currentDirPath.lastIndexOf('/')
        val parent = if (idx == -1) "" else currentDirPath.substring(0, idx)
        loadDirectory(parent)
    }

    private fun openEditorForFile(relPath: String) {
        currentEditingFilePath = relPath
        txtEditorOpenFileName.text = relPath
        scrollDirectoryBrowser.visibility = View.GONE
        boxCodeEditorView.visibility = View.VISIBLE

        setBusy(true, "Loading file: $relPath...")
        api.getFileContent(relPath, object : ApiClient.ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                setBusy(false, "")
                val content = result.optString("content", "")
                val isBin = result.optBoolean("is_binary", false)
                if (isBin) {
                    editCodeContent.setText("[Binary file - Hex editor view available]")
                    editCodeContent.isEnabled = false
                } else {
                    editCodeContent.setText(content)
                    editCodeContent.isEnabled = true
                }
            }
            override fun onError(errorMessage: String) {
                setBusy(false, "")
                toast("Error loading file: $errorMessage")
            }
        })
    }

    private fun saveCurrentOpenCodeFile() {
        if (currentEditingFilePath.isEmpty()) return
        val newContent = editCodeContent.text.toString()
        setBusy(true, "Saving: $currentEditingFilePath...")
        api.saveFileContent(currentEditingFilePath, newContent, object : ApiClient.ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                setBusy(false, "")
                toast("File saved successfully.")
            }
            override fun onError(errorMessage: String) {
                setBusy(false, "")
                toast("Save failed: $errorMessage")
            }
        })
    }

    private fun triggerAiReviewOnFile() {
        if (currentEditingFilePath.isEmpty()) return
        val content = editCodeContent.text.toString()
        setBusy(true, "AI Code Review running...")
        api.aiReview(currentEditingFilePath, content, object : ApiClient.ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                setBusy(false, "")
                val review = result.optString("review", "No suggestions.")
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("AI Smali / Code Review")
                    .setMessage(review)
                    .setPositiveButton("Close", null)
                    .show()
            }
            override fun onError(errorMessage: String) {
                setBusy(false, "")
                toast("AI Review Error: $errorMessage")
            }
        })
    }

    private fun closeCodeEditor() {
        boxCodeEditorView.visibility = View.GONE
        scrollDirectoryBrowser.visibility = View.VISIBLE
        currentEditingFilePath = ""
    }

    private fun performHexSearch() {
        if (currentEditingFilePath.isEmpty()) {
            toast("Open a file first to search in Hex.")
            return
        }
        val q = editHexSearchQuery.text.toString().trim()
        if (q.isEmpty()) return
        setBusy(true, "Hex searching...")
        api.hexSearch(currentEditingFilePath, q, object : ApiClient.ApiCallback<List<HexResult>> {
            override fun onSuccess(result: List<HexResult>) {
                setBusy(false, "")
                boxHexEditorView.visibility = View.VISIBLE
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
        for (r in results) {
            val tv = TextView(this).apply {
                text = "${r.hexOffset}: ${r.hexSnippet} | ${r.asciiSnippet}"
                setTextColor(getColor(R.color.hex_ascii))
                textSize = 10f
                setTypeface(android.graphics.Typeface.MONOSPACE)
                setPadding(8, 8, 8, 8)
                background = ContextCompat.getDrawable(this@MainActivity, R.drawable.bg_input_field)
            }
            hexResultsContainer.addView(tv)
        }
    }

    private fun applyHexPatch() {
        val patch = editHexPatchBytes.text.toString().trim()
        if (currentEditingFilePath.isEmpty() || patch.isEmpty()) return
        setBusy(true, "Applying hex patch...")
        api.hexPatch(currentEditingFilePath, 0, patch, object : ApiClient.ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                setBusy(false, "")
                toast("Hex patch applied successfully.")
            }
            override fun onError(errorMessage: String) {
                setBusy(false, "")
                toast("Hex patch error: $errorMessage")
            }
        })
    }

    private fun closeHexEditor() {
        boxHexEditorView.visibility = View.GONE
    }

    // -------------------------------------------------------------
    // Resources & AI Customizer Logic
    // -------------------------------------------------------------

    private fun loadStringsForLocale(locale: String) {
        setBusy(true, "Loading strings ($locale)...")
        api.loadStrings(locale, object : ApiClient.ApiCallback<Map<String, String>> {
            override fun onSuccess(result: Map<String, String>) {
                setBusy(false, "")
                loadedStringsMap.clear()
                loadedStringsMap.putAll(result)
                if (result.containsKey("app_name")) {
                    editCustomAppName.setText(result["app_name"])
                }
                renderStringsTable()
            }
            override fun onError(errorMessage: String) {
                setBusy(false, "")
                toast("Failed to load strings: $errorMessage")
            }
        })
    }

    private fun renderStringsTable() {
        stringsTableContainer.removeAllViews()
        for ((key, value) in loadedStringsMap.entries.take(20)) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(8, 8, 8, 8)
            }
            val keyTv = TextView(this).apply {
                text = key
                setTextColor(getColor(R.color.text_primary))
                textSize = 11f
                setTypeface(null, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.4f)
            }
            val valTv = TextView(this).apply {
                text = value
                setTextColor(getColor(R.color.text_secondary))
                textSize = 11f
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.6f)
            }
            row.addView(keyTv)
            row.addView(valTv)
            row.setOnClickListener { showEditStringDialog(key, value) }
            stringsTableContainer.addView(row)
        }
    }

    private fun showEditStringDialog(key: String, currentVal: String) {
        val dialog = Dialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_edit_string, null)
        dialog.setContentView(view)
        val txtKey = view.findViewById<TextView>(R.id.txtStringKeyName)
        val editVal = view.findViewById<EditText>(R.id.editStringValInput)
        val btnSubmit = view.findViewById<Button>(R.id.btnSubmitStringVal)

        txtKey.text = "Key: $key"
        editVal.setText(currentVal)

        btnSubmit.setOnClickListener {
            val newVal = editVal.text.toString()
            loadedStringsMap[key] = newVal
            renderStringsTable()
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun saveStringsChanges() {
        val appName = editCustomAppName.text.toString().trim()
        if (appName.isNotEmpty()) {
            loadedStringsMap["app_name"] = appName
        }
        val locale = editStringsLocale.text.toString().trim().ifEmpty { "values" }
        setBusy(true, "Saving strings.xml ($locale)...")
        api.saveStrings(locale, loadedStringsMap, object : ApiClient.ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                setBusy(false, "")
                toast("Strings saved successfully.")
            }
            override fun onError(errorMessage: String) {
                setBusy(false, "")
                toast("Strings save error: $errorMessage")
            }
        })
    }

    private fun generateAiLauncherIcon() {
        val prompt = editAiIconPrompt.text.toString().trim()
        if (prompt.isEmpty()) {
            toast("Please enter an icon prompt description.")
            return
        }
        setBusy(true, "Generating AI icon: '$prompt'...")
        api.generateIcon(prompt, object : ApiClient.ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                setBusy(false, "")
                toast("AI Icon generated and scaled to all densities!")
            }
            override fun onError(errorMessage: String) {
                setBusy(false, "")
                toast("AI Icon error: $errorMessage")
            }
        })
    }

    private fun triggerAiFix() {
        setBusy(true, "Running full AI smali logic scan...")
        api.aiFixAll(object : ApiClient.ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                setBusy(false, "")
                toast(result.optString("message", "AI scan complete."))
            }
            override fun onError(errorMessage: String) {
                setBusy(false, "")
                toast("AI Fix Error: $errorMessage")
            }
        })
    }

    private fun performGlobalFindOnly() {
        val query = editGlobalFindText.text.toString().trim()
        if (query.isEmpty()) return
        setBusy(true, "Searching across all project files...")
        api.globalFind(query, object : ApiClient.ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                setBusy(false, "")
                val matches = result.optInt("count", 0)
                toast("Found $matches occurrences across files.")
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
        api.globalReplace(find, replace, object : ApiClient.ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                setBusy(false, "")
                val replaced = result.optInt("replaced", 0)
                toast("Replaced $replaced instances across project.")
            }
            override fun onError(errorMessage: String) {
                setBusy(false, "")
                toast("Replace error: $errorMessage")
            }
        })
    }

    // -------------------------------------------------------------
    // Build & Keystore Signer Logic
    // -------------------------------------------------------------

    private fun triggerBuildApk() {
        setBusy(true, "Recompiling APK with apktool...")
        boxBuildLogs.visibility = View.VISIBLE
        txtBuildLogOutput.text = "Starting APK build process...\nCompiling resources & smali..."

        api.buildApk(object : ApiClient.ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                setBusy(false, "")
                txtBuildLogOutput.text = result.optString("log", "APK built successfully.")
                toast("APK compiled successfully!")
            }
            override fun onError(errorMessage: String) {
                setBusy(false, "")
                txtBuildLogOutput.text = errorMessage
                toast("Build failed: $errorMessage")
            }
        })
    }

    private fun loadKeystoresList() {
        api.getKeystores(object : ApiClient.ApiCallback<List<KeystoreItem>> {
            override fun onSuccess(result: List<KeystoreItem>) {
                renderKeystores(result)
            }
            override fun onError(errorMessage: String) {
                toast("Failed to load keystores: $errorMessage")
            }
        })
    }

    private fun renderKeystores(keystores: List<KeystoreItem>) {
        keystoresListContainer.removeAllViews()
        for (k in keystores) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(8, 8, 8, 8)
                background = ContextCompat.getDrawable(this@MainActivity, R.drawable.bg_input_field)
            }
            val titleTv = TextView(this).apply {
                text = "${k.fileName} (Alias: ${k.keyAlias})"
                setTextColor(getColor(R.color.text_primary))
                textSize = 12f
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            row.addView(titleTv)
            keystoresListContainer.addView(row)
        }
    }

    private fun showCreateKeystoreDialog() {
        val dialog = Dialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_create_keystore, null)
        dialog.setContentView(view)

        val editAlias = view.findViewById<EditText>(R.id.editKeystoreAlias)
        val editPass = view.findViewById<EditText>(R.id.editKeystorePassword)
        val btnSubmit = view.findViewById<Button>(R.id.btnSubmitCreateKeystore)

        btnSubmit.setOnClickListener {
            val alias = editAlias.text.toString().trim()
            val pass = editPass.text.toString().trim()
            if (alias.isEmpty() || pass.isEmpty()) {
                toast("Please fill all fields.")
                return@setOnClickListener
            }
            setBusy(true, "Generating RSA 2048-bit JKS...")
            api.createKeystore(alias, pass, object : ApiClient.ApiCallback<JSONObject> {
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

    private fun triggerSignApk() {
        val pass = editSignPassword.text.toString().trim()
        if (pass.isEmpty()) {
            toast("Please enter the keystore password.")
            return
        }
        setBusy(true, "Signing APK with jarsigner...")
        api.signApk(pass, object : ApiClient.ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                setBusy(false, "")
                toast("APK signed successfully! Ready for installation.")
            }
            override fun onError(errorMessage: String) {
                setBusy(false, "")
                toast("Signing failed: $errorMessage")
            }
        })
    }

    private fun handleDownloadAndInstallApk() {
        setBusy(true, "Downloading signed APK...")
        api.downloadSignedApk(object : ApiClient.ApiCallback<File> {
            override fun onSuccess(apkFile: File) {
                setBusy(false, "")
                installApkFile(apkFile)
            }
            override fun onError(errorMessage: String) {
                setBusy(false, "")
                toast("Download failed: $errorMessage")
            }
        })
    }

    private fun installApkFile(apkFile: File) {
        try {
            val uri = FileProvider.getUriForFile(this, "${applicationContext.packageName}.fileprovider", apkFile)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            startActivity(intent)
        } catch (e: Exception) {
            toast("Install error: ${e.message}")
        }
    }

    // -------------------------------------------------------------
    // Cloud Debugger & Wireless ADB Logic
    // -------------------------------------------------------------

    private fun toggleCloudLogsStream() {
        cloudLogsStreaming = !cloudLogsStreaming
        btnToggleCloudLogsStream.text = if (cloudLogsStreaming) "Pause Stream" else "Start Stream"
        if (cloudLogsStreaming) {
            resumeCloudLogsPolling()
            toast("Cloud log stream started (2s auto-polling).")
        } else {
            pauseCloudLogsPolling()
            toast("Cloud log stream paused.")
        }
    }

    private fun resumeCloudLogsPolling() {
        cloudLogsTimer = object : Runnable {
            override fun run() {
                fetchCloudLogsOnce()
                if (cloudLogsStreaming) {
                    mainHandler.postDelayed(this, 2000)
                }
            }
        }
        mainHandler.post(cloudLogsTimer!!)
    }

    private fun pauseCloudLogsPolling() {
        cloudLogsTimer?.let { mainHandler.removeCallbacks(it) }
        cloudLogsTimer = null
    }

    private fun fetchCloudLogsOnce() {
        api.getCloudLogs(object : ApiClient.ApiCallback<String> {
            override fun onSuccess(result: String) {
                txtCloudLogsStream.text = result
            }
            override fun onError(errorMessage: String) {}
        })
    }

    private fun clearCloudLogs() {
        api.clearCloudLogs(object : ApiClient.ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                txtCloudLogsStream.text = ""
                toast("Cloud logs cleared.")
            }
            override fun onError(errorMessage: String) {
                toast("Clear failed: $errorMessage")
            }
        })
    }

    private fun connectAdbHost() {
        val host = editAdbHostIp.text.toString().trim()
        if (host.isEmpty()) return
        setBusy(true, "Connecting ADB to $host...")
        api.adbConnect(host, object : ApiClient.ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                setBusy(false, "")
                toast(result.optString("message", "ADB connected."))
                loadAdbDevicesList()
            }
            override fun onError(errorMessage: String) {
                setBusy(false, "")
                toast("ADB error: $errorMessage")
            }
        })
    }

    private fun loadAdbDevicesList() {
        api.getAdbDevices(object : ApiClient.ApiCallback<List<AdbDevice>> {
            override fun onSuccess(result: List<AdbDevice>) {
                renderAdbDevices(result)
            }
            override fun onError(errorMessage: String) {}
        })
    }

    private fun renderAdbDevices(devices: List<AdbDevice>) {
        adbDevicesContainer.removeAllViews()
        for (d in devices) {
            val tv = TextView(this).apply {
                text = "📱 ${d.serial} [${d.state.uppercase()}] ${d.model}"
                setTextColor(getColor(R.color.accent))
                textSize = 11f
                setPadding(8, 6, 8, 6)
            }
            adbDevicesContainer.addView(tv)
        }
    }

    private fun readAdbLogcat(filter: String) {
        setBusy(true, "Fetching Logcat...")
        api.getAdbLogcat(filter, object : ApiClient.ApiCallback<String> {
            override fun onSuccess(result: String) {
                setBusy(false, "")
                txtAdbLogcatOutput.text = result
            }
            override fun onError(errorMessage: String) {
                setBusy(false, "")
                toast("Logcat error: $errorMessage")
            }
        })
    }

    // -------------------------------------------------------------
    // AI Settings Logic
    // -------------------------------------------------------------

    private fun loadAiSettings() {
        api.getAiSettings(object : ApiClient.ApiCallback<AiSettingsData> {
            override fun onSuccess(result: AiSettingsData) {
                if (result.provider == "openai") {
                    rbProviderOpenAi.isChecked = true
                } else {
                    rbProviderGemini.isChecked = true
                }
                txtGeminiKeyStatus.text = if (result.geminiHasKey) "Status: Saved (${result.geminiMaskedKey})" else "Status: No key saved"
                txtOpenAiKeyStatus.text = if (result.openaiHasKey) "Status: Saved (${result.openaiMaskedKey})" else "Status: No key saved"
                editModelGeminiText.setText(result.geminiTextModel)
                editModelGeminiImage.setText(result.geminiImageModel)
                editModelOpenAiText.setText(result.openaiTextModel)
                editModelOpenAiImage.setText(result.openaiImageModel)
            }
            override fun onError(errorMessage: String) {}
        })
    }

    private fun saveGeminiApiKey() {
        val key = editGeminiApiKey.text.toString().trim()
        if (key.isEmpty()) return
        setBusy(true, "Saving Gemini Key...")
        api.saveGeminiKey(key, object : ApiClient.ApiCallback<String> {
            override fun onSuccess(result: String) {
                setBusy(false, "")
                toast(result)
                editGeminiApiKey.setText("")
                loadAiSettings()
            }
            override fun onError(errorMessage: String) {
                setBusy(false, "")
                toast(errorMessage)
            }
        })
    }

    private fun deleteGeminiApiKey() {
        setBusy(true, "Deleting Gemini Key...")
        api.deleteGeminiKey(object : ApiClient.ApiCallback<String> {
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

    private fun saveOpenAiApiKey() {
        val key = editOpenAiApiKey.text.toString().trim()
        if (key.isEmpty()) return
        setBusy(true, "Saving OpenAI Key...")
        api.saveOpenAiKey(key, object : ApiClient.ApiCallback<String> {
            override fun onSuccess(result: String) {
                setBusy(false, "")
                toast(result)
                editOpenAiApiKey.setText("")
                loadAiSettings()
            }
            override fun onError(errorMessage: String) {
                setBusy(false, "")
                toast(errorMessage)
            }
        })
    }

    private fun deleteOpenAiApiKey() {
        setBusy(true, "Deleting OpenAI Key...")
        api.deleteOpenAiKey(object : ApiClient.ApiCallback<String> {
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

    private fun saveCustomModels() {
        val gt = editModelGeminiText.text.toString().trim()
        val gi = editModelGeminiImage.text.toString().trim()
        val ot = editModelOpenAiText.text.toString().trim()
        val oi = editModelOpenAiImage.text.toString().trim()
        setBusy(true, "Saving custom model identifiers...")
        api.saveCustomModels(gt, gi, ot, oi, object : ApiClient.ApiCallback<String> {
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

    private fun resetCustomModels() {
        editModelGeminiText.setText("gemini-3.6-flash")
        editModelGeminiImage.setText("gemini-3.1-flash-image")
        editModelOpenAiText.setText("gpt-5.6-sol")
        editModelOpenAiImage.setText("gpt-image-2")
        saveCustomModels()
    }

    // -------------------------------------------------------------
    // Admin Panel Logic
    // -------------------------------------------------------------

    private fun loadAdminPanelData() {
        if (!session.isAuthenticated() || session.currentUser?.isAdmin != true) return
        loadAdminUsers()
        loadAdminInquiries()
        loadAdminBlogs()
        loadAdminFaqs()
    }

    private fun loadAdminUsers() {
        api.getAdminUsers(object : ApiClient.ApiCallback<List<User>> {
            override fun onSuccess(result: List<User>) {
                renderAdminUsers(result)
            }
            override fun onError(errorMessage: String) {}
        })
    }

    private fun renderAdminUsers(users: List<User>) {
        adminUsersContainer.removeAllViews()
        for (u in users) {
            val tv = TextView(this).apply {
                text = "${u.username} (${u.email}) [${u.userType.uppercase()}]"
                setTextColor(getColor(R.color.text_primary))
                textSize = 12f
                setPadding(8, 8, 8, 8)
            }
            adminUsersContainer.addView(tv)
        }
    }

    private fun loadAdminInquiries() {
        api.getAdminInquiries(object : ApiClient.ApiCallback<List<ContactInquiry>> {
            override fun onSuccess(result: List<ContactInquiry>) {
                renderAdminInquiries(result)
            }
            override fun onError(errorMessage: String) {}
        })
    }

    private fun renderAdminInquiries(inquiries: List<ContactInquiry>) {
        adminInquiriesContainer.removeAllViews()
        for (i in inquiries) {
            val tv = TextView(this).apply {
                text = "From: ${i.name} <${i.email}>\nSubject: ${i.subject}\nMessage: ${i.message}"
                setTextColor(getColor(R.color.text_secondary))
                textSize = 11f
                setPadding(8, 8, 8, 8)
                background = ContextCompat.getDrawable(this@MainActivity, R.drawable.bg_input_field)
            }
            adminInquiriesContainer.addView(tv)
        }
    }

    private fun loadAdminBlogs() {
        api.getAdminBlogs(object : ApiClient.ApiCallback<List<BlogPost>> {
            override fun onSuccess(result: List<BlogPost>) {
                renderAdminBlogs(result)
            }
            override fun onError(errorMessage: String) {}
        })
    }

    private fun renderAdminBlogs(blogs: List<BlogPost>) {
        adminBlogsContainer.removeAllViews()
        for (b in blogs) {
            val tv = TextView(this).apply {
                text = "📰 ${b.title} (${b.category})"
                setTextColor(getColor(R.color.text_primary))
                textSize = 12f
                setPadding(8, 8, 8, 8)
            }
            adminBlogsContainer.addView(tv)
        }
    }

    private fun loadAdminFaqs() {
        api.getAdminFaqs(object : ApiClient.ApiCallback<List<FaqItem>> {
            override fun onSuccess(result: List<FaqItem>) {
                renderAdminFaqs(result)
            }
            override fun onError(errorMessage: String) {}
        })
    }

    private fun renderAdminFaqs(faqs: List<FaqItem>) {
        adminFaqsContainer.removeAllViews()
        for (f in faqs) {
            val tv = TextView(this).apply {
                text = "Q: ${f.question}\nA: ${f.answer}"
                setTextColor(getColor(R.color.text_secondary))
                textSize = 11f
                setPadding(8, 8, 8, 8)
            }
            adminFaqsContainer.addView(tv)
        }
    }

    private fun showCreateUserDialog() {
        val dialog = Dialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_create_user, null)
        dialog.setContentView(view)

        val editEmail = view.findViewById<EditText>(R.id.editNewUserEmail)
        val editUser = view.findViewById<EditText>(R.id.editNewUserUsername)
        val editPass = view.findViewById<EditText>(R.id.editNewUserPassword)
        val editDec = view.findViewById<EditText>(R.id.editNewUserDecompileLimit)
        val editCom = view.findViewById<EditText>(R.id.editNewUserCompileLimit)
        val editKey = view.findViewById<EditText>(R.id.editNewUserKeygenLimit)
        val editSig = view.findViewById<EditText>(R.id.editNewUserSignLimit)
        val btnSubmit = view.findViewById<Button>(R.id.btnSubmitCreateUser)

        btnSubmit.setOnClickListener {
            val e = editEmail.text.toString().trim()
            val u = editUser.text.toString().trim()
            val p = editPass.text.toString().trim()
            val d = editDec.text.toString().toIntOrNull() ?: 10
            val c = editCom.text.toString().toIntOrNull() ?: 10
            val k = editKey.text.toString().toIntOrNull() ?: 10
            val s = editSig.text.toString().toIntOrNull() ?: 10

            if (e.isEmpty() || u.isEmpty() || p.isEmpty()) {
                toast("Please fill required fields.")
                return@setOnClickListener
            }
            setBusy(true, "Creating user account...")
            api.adminCreateUser(e, u, p, d, c, k, s, object : ApiClient.ApiCallback<JSONObject> {
                override fun onSuccess(result: JSONObject) {
                    setBusy(false, "")
                    dialog.dismiss()
                    toast("User created successfully!")
                    loadAdminUsers()
                }
                override fun onError(errorMessage: String) {
                    setBusy(false, "")
                    toast("Create user error: $errorMessage")
                }
            })
        }
        dialog.show()
    }

    private fun showEditBlogDialog(post: BlogPost?) {
        val dialog = Dialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_edit_blog, null)
        dialog.setContentView(view)

        val editTitle = view.findViewById<EditText>(R.id.editBlogTitle)
        val editCat = view.findViewById<EditText>(R.id.editBlogCategory)
        val editRead = view.findViewById<EditText>(R.id.editBlogReadTime)
        val editTags = view.findViewById<EditText>(R.id.editBlogTags)
        val editExc = view.findViewById<EditText>(R.id.editBlogExcerpt)
        val editCont = view.findViewById<EditText>(R.id.editBlogContent)
        val btnSubmit = view.findViewById<Button>(R.id.btnSubmitBlog)

        if (post != null) {
            editTitle.setText(post.title)
            editCat.setText(post.category)
            editRead.setText(post.readTime)
            editTags.setText(post.tags)
            editExc.setText(post.excerpt)
            editCont.setText(post.content)
        }

        btnSubmit.setOnClickListener {
            val t = editTitle.text.toString().trim()
            val c = editCat.text.toString().trim()
            val r = editRead.text.toString().trim()
            val tg = editTags.text.toString().trim()
            val ex = editExc.text.toString().trim()
            val ct = editCont.text.toString()

            if (t.isEmpty() || ct.isEmpty()) {
                toast("Title and content are required.")
                return@setOnClickListener
            }

            setBusy(true, "Saving article...")
            api.adminSaveBlog(post?.id ?: 0, t, c, r, tg, ex, ct, object : ApiClient.ApiCallback<JSONObject> {
                override fun onSuccess(result: JSONObject) {
                    setBusy(false, "")
                    dialog.dismiss()
                    toast("Article published!")
                    loadAdminBlogs()
                }
                override fun onError(errorMessage: String) {
                    setBusy(false, "")
                    toast("Blog error: $errorMessage")
                }
            })
        }
        dialog.show()
    }

    private fun showEditFaqDialog(faq: FaqItem?) {
        val dialog = Dialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_edit_faq, null)
        dialog.setContentView(view)

        val editQ = view.findViewById<EditText>(R.id.editFaqQuestion)
        val editC = view.findViewById<EditText>(R.id.editFaqCategory)
        val editA = view.findViewById<EditText>(R.id.editFaqAnswer)
        val btnSubmit = view.findViewById<Button>(R.id.btnSubmitFaq)

        if (faq != null) {
            editQ.setText(faq.question)
            editC.setText(faq.category)
            editA.setText(faq.answer)
        }

        btnSubmit.setOnClickListener {
            val q = editQ.text.toString().trim()
            val c = editC.text.toString().trim()
            val a = editA.text.toString().trim()
            if (q.isEmpty() || a.isEmpty()) return@setOnClickListener

            setBusy(true, "Saving FAQ...")
            api.adminSaveFaq(faq?.id ?: 0, q, c, a, object : ApiClient.ApiCallback<JSONObject> {
                override fun onSuccess(result: JSONObject) {
                    setBusy(false, "")
                    dialog.dismiss()
                    toast("FAQ saved!")
                    loadAdminFaqs()
                }
                override fun onError(errorMessage: String) {
                    setBusy(false, "")
                    toast("FAQ error: $errorMessage")
                }
            })
        }
        dialog.show()
    }

    private fun saveGitHubBackupSettings() {
        val owner = editBackupRepoOwner.text.toString().trim()
        val repo = editBackupRepoName.text.toString().trim()
        val branch = editBackupBranch.text.toString().trim()
        val token = editBackupToken.text.toString().trim()

        setBusy(true, "Saving GitHub Backup settings...")
        api.saveBackupSettings(owner, repo, branch, token, object : ApiClient.ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                setBusy(false, "")
                toast("GitHub configuration saved.")
            }
            override fun onError(errorMessage: String) {
                setBusy(false, "")
                toast("Backup config error: $errorMessage")
            }
        })
    }

    private fun runManualBackup() {
        setBusy(true, "Executing manual GitHub backup...")
        api.runBackup(object : ApiClient.ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                setBusy(false, "")
                toast(result.optString("message", "Backup completed successfully!"))
            }
            override fun onError(errorMessage: String) {
                setBusy(false, "")
                toast("Backup error: $errorMessage")
            }
        })
    }

    // -------------------------------------------------------------
    // Community & Support Logic
    // -------------------------------------------------------------

    private fun loadPublicCommunityData() {
        api.getPublicBlogs(object : ApiClient.ApiCallback<List<BlogPost>> {
            override fun onSuccess(result: List<BlogPost>) {
                renderPublicBlogs(result)
            }
            override fun onError(errorMessage: String) {}
        })

        api.getPublicFaqs(object : ApiClient.ApiCallback<List<FaqItem>> {
            override fun onSuccess(result: List<FaqItem>) {
                renderPublicFaqs(result, publicFaqsContainer)
                renderPublicFaqs(result, hubFaqsContainer)
            }
            override fun onError(errorMessage: String) {}
        })
    }

    private fun renderPublicBlogs(blogs: List<BlogPost>) {
        publicBlogsContainer.removeAllViews()
        for (b in blogs) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = ContextCompat.getDrawable(this@MainActivity, R.drawable.bg_card_rounded)
                setPadding(16, 16, 16, 16)
                val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 0, 12)
                }
                layoutParams = lp
            }
            val titleTv = TextView(this).apply {
                text = b.title
                setTextColor(getColor(R.color.text_primary))
                textSize = 13f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            val excTv = TextView(this).apply {
                text = b.excerpt
                setTextColor(getColor(R.color.text_secondary))
                textSize = 11f
                setPadding(0, 4, 0, 0)
            }
            card.addView(titleTv)
            card.addView(excTv)
            publicBlogsContainer.addView(card)
        }
    }

    private fun renderPublicFaqs(faqs: List<FaqItem>, container: LinearLayout) {
        container.removeAllViews()
        for (f in faqs) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = ContextCompat.getDrawable(this@MainActivity, R.drawable.bg_card_rounded)
                setPadding(16, 14, 16, 14)
                val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 0, 10)
                }
                layoutParams = lp
            }
            val qTv = TextView(this).apply {
                text = "❓ ${f.question}"
                setTextColor(getColor(R.color.accent))
                textSize = 12f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            val aTv = TextView(this).apply {
                text = f.answer
                setTextColor(getColor(R.color.text_secondary))
                textSize = 11f
                setPadding(0, 4, 0, 0)
            }
            card.addView(qTv)
            card.addView(aTv)
            container.addView(card)
        }
    }

    private fun submitContactInquiryForm() {
        val n = editContactName.text.toString().trim()
        val e = editContactEmail.text.toString().trim()
        val s = editContactSubject.text.toString().trim()
        val m = editContactMessage.text.toString().trim()

        if (n.isEmpty() || e.isEmpty() || m.isEmpty()) {
            toast("Please fill your name, email, and message.")
            return
        }

        setBusy(true, "Submitting inquiry...")
        api.submitContactInquiry(n, e, s, m, object : ApiClient.ApiCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                setBusy(false, "")
                toast(result.optString("message", "Inquiry submitted!"))
                editContactSubject.setText("")
                editContactMessage.setText("")
            }
            override fun onError(errorMessage: String) {
                setBusy(false, "")
                toast("Error submitting inquiry: $errorMessage")
            }
        })
    }

    // -------------------------------------------------------------
    // Auto-Check Timer
    // -------------------------------------------------------------

    private fun startAutoCheckTimer() {
        autoCheckTimer = object : Runnable {
            override fun run() {
                if (session.isAuthenticated()) {
                    api.checkBuildUpdate(object : ApiClient.ApiCallback<Boolean> {
                        override fun onSuccess(hasUpdate: Boolean) {
                            globalUpdateBanner.visibility = if (hasUpdate) View.VISIBLE else View.GONE
                        }
                        override fun onError(errorMessage: String) {}
                    })
                }
                mainHandler.postDelayed(this, 15000)
            }
        }
        mainHandler.postDelayed(autoCheckTimer!!, 5000)
    }

    private fun stopAutoCheckTimer() {
        autoCheckTimer?.let { mainHandler.removeCallbacks(it) }
        autoCheckTimer = null
    }

    // -------------------------------------------------------------
    // User Profile & Account Dialog
    // -------------------------------------------------------------

    private fun showUserAccountDialog() {
        val u = session.currentUser ?: return
        AlertDialog.Builder(this)
            .setTitle("User Profile (${u.username})")
            .setMessage("Email: ${u.email}\nRole: ${u.userType.uppercase()}\nServer: ${session.serverUrl}\nDecompiles Used: ${u.decompileUsage}/${u.decompileLimit}\nCompiles Used: ${u.compileUsage}/${u.compileLimit}")
            .setPositiveButton("Logout") { _, _ ->
                session.clearAuth()
                updateUiForAuthState()
                toast("Logged out successfully.")
                switchAuthHubTab("signin")
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showServerUrlDialog() {
        val dialog = Dialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_server_url, null)
        dialog.setContentView(view)

        val editUrl = view.findViewById<EditText>(R.id.editServerUrlInput)
        val btnSave = view.findViewById<Button>(R.id.btnSaveServerUrl)

        editUrl.setText(session.serverUrl)

        btnSave.setOnClickListener {
            val u = editUrl.text.toString().trim()
            if (u.isNotEmpty()) {
                session.serverUrl = u
                updateUiForAuthState()
                toast("Server URL updated.")
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    // -------------------------------------------------------------
    // QR Code Scanner & Pairing
    // -------------------------------------------------------------

    private fun launchQrScanner() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startQrScannerActivity()
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startQrScannerActivity() {
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
            // Case 1: Base64 JSON (as produced by web dashboard: btoa(JSON.stringify({url, token, name})))
            var jsonStr = ""
            try {
                val decodedBytes = Base64.decode(raw.trim(), Base64.DEFAULT)
                jsonStr = String(decodedBytes, Charsets.UTF_8)
            } catch (e: Exception) {
                jsonStr = raw.trim()
            }

            if (jsonStr.startsWith("{") && jsonStr.endsWith("}")) {
                val json = JSONObject(jsonStr)
                val url = json.optString("url", "")
                val tok = json.optString("token", "")
                val name = json.optString("name", "app")
                val authToken = json.optString("auth_token", "")

                if (url.isNotEmpty()) session.serverUrl = url
                if (tok.isNotEmpty()) session.pairingToken = tok
                if (name.isNotEmpty()) session.pairedProjectName = name

                if (authToken.isNotEmpty()) {
                    session.pairingToken = authToken
                }

                updateUiForAuthState()
                toast("Paired with project '$name'")
                fetchCloudLogsOnce()
                if (session.isAuthenticated()) {
                    refreshDashboardData()
                }
                return
            }

            // Case 2: Standard URL format (e.g. https://apk.zoomnearby.com/?pairing=abc or ?token=abc)
            if (raw.startsWith("http://") || raw.startsWith("https://")) {
                val uri = Uri.parse(raw)
                val tokenParam = uri.getQueryParameter("token")
                    ?: uri.getQueryParameter("pairing")
                    ?: uri.getQueryParameter("pairing_token")
                    ?: uri.getQueryParameter("crash_token")

                val base = "${uri.scheme}://${uri.host}${if (uri.port != -1) ":${uri.port}" else ""}${uri.path ?: "/"}"
                session.serverUrl = base

                if (!tokenParam.isNullOrEmpty()) {
                    session.pairingToken = tokenParam
                    session.pairedProjectName = uri.getQueryParameter("name") ?: "web_project"
                    toast("Server & pairing token saved.")
                } else {
                    toast("Server host updated.")
                }

                updateUiForAuthState()
                fetchCloudLogsOnce()
                return
            }

            // Case 3: Raw pairing token string (e.g. pair_abcdef123 or project token)
            if (raw.isNotBlank()) {
                session.pairingToken = raw.trim()
                session.pairedProjectName = "project"
                updateUiForAuthState()
                toast("Pairing token saved.")
                fetchCloudLogsOnce()
                return
            }

            toast("Unrecognized QR code format.")
        } catch (e: Exception) {
            toast("QR parse error: ${e.message}")
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

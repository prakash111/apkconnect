package com.apktoolai.companion.api

import org.json.JSONObject

data class User(
    val id: Int = 0,
    val username: String = "",
    val email: String = "",
    val userType: String = "user",
    val emailVerified: Int = 0,
    val decompileLimit: Int = 1,
    val decompileUsage: Int = 0,
    val compileLimit: Int = 1,
    val compileUsage: Int = 0,
    val generateKeyLimit: Int = 1,
    val generateKeyUsage: Int = 0,
    val signApkLimit: Int = 1,
    val signApkUsage: Int = 0
) {
    val isAdmin: Boolean get() = userType.equals("admin", ignoreCase = true)
}

data class UserLimits(
    val decompileLimit: Int = 1,
    val decompileUsage: Int = 0,
    val compileLimit: Int = 1,
    val compileUsage: Int = 0,
    val generateKeyLimit: Int = 1,
    val generateKeyUsage: Int = 0,
    val signApkLimit: Int = 1,
    val signApkUsage: Int = 0,
    val maxUploadBytes: Long = 100 * 1024 * 1024L
)

data class ProjectItem(
    val id: Int = 0,
    val projectId: String = "",
    val projectName: String = "",
    val sourceApk: String? = null,
    val projectPath: String? = null,
    val projectRoot: String? = null,
    val unsignedApk: String? = null,
    val signedApk: String? = null,
    val keystorePath: String? = null,
    val keystoreAlias: String? = null,
    val logoPreviewName: String? = null,
    val logoPreviewPath: String? = null,
    val logoVersion: Long = 0,
    val crashReportToken: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val apkName: String? = null,
    val apkSize: Long = 0,
    val packageName: String? = null,
    val versionCode: String? = null,
    val versionName: String? = null,
    val minSdk: String? = null,
    val targetSdk: String? = null,
    val status: String? = null,
    val lastStep: String? = null,
    val userId: Int = 0
)

data class ProjectState(
    val projectId: String = "",
    val projectName: String = "",
    val projectPath: String = "",
    val projectRoot: String = "",
    val sourceApk: String? = null,
    val unsignedApk: String? = null,
    val signedApk: String? = null,
    val keystorePath: String? = null,
    val keystoreAlias: String? = null,
    val logoPreviewName: String? = null,
    val logoPreviewPath: String? = null,
    val logoVersion: Long = 0,
    val crashReportToken: String? = null,
    val lastBuildLog: String? = null,
    val lastBuildFailed: Boolean = false,
    val selectedLocale: String = "values",
    val editorFilePath: String? = null,
    val editorFileContent: String? = null,
    val editorFileIsBinary: Boolean = false
)

data class ProjectFile(
    val name: String = "",
    val path: String = "",
    val isDir: Boolean = false,
    val size: Long = 0
)

data class StringItem(
    val name: String = "",
    var value: String = ""
)

data class KeystoreItem(
    val id: Int = 0,
    val username: String = "",
    val fileName: String = "",
    val keyAlias: String = "",
    val createdAt: String = ""
)

data class ContactInquiry(
    val id: Int = 0,
    val name: String = "",
    val email: String = "",
    val subject: String = "",
    val message: String = "",
    val status: String = "new",
    val createdAt: String = ""
) {
    val isRead: Boolean get() = status.equals("read", ignoreCase = true)
}

data class BlogPost(
    val id: Int = 0,
    val title: String = "",
    val slug: String = "",
    val excerpt: String = "",
    val content: String = "",
    val category: String = "",
    val tags: String = "",
    val readTime: String = "5 min read",
    val views: Int = 0,
    val createdAt: String = ""
)

data class FaqItem(
    val id: Int = 0,
    val question: String = "",
    val answer: String = "",
    val category: String = "",
    val sortOrder: Int = 0,
    val isActive: Int = 1
)

data class AdbDevice(
    val serial: String = "",
    val state: String = "",
    val model: String = ""
)

data class HexResult(
    val offset: Long = 0,
    val hexOffset: String = "",
    val pageOffset: Long = 0,
    val pageHex: String = "",
    val hexSnippet: String = "",
    val asciiSnippet: String = ""
)

data class AiSettingsData(
    val provider: String = "gemini",
    val geminiHasKey: Boolean = false,
    val geminiMaskedKey: String = "",
    val openaiHasKey: Boolean = false,
    val openaiMaskedKey: String = "",
    val geminiTextModel: String = "gemini-3.6-flash",
    val geminiImageModel: String = "gemini-3.1-flash-image",
    val openaiTextModel: String = "gpt-5.6-sol",
    val openaiImageModel: String = "gpt-image-2"
)

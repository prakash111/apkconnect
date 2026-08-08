# APK Tool Studio - Android Native Application

Full-featured native Android companion and mobile engineering studio connecting directly to the shared MySQL database and backend API.

## Features Implemented in Native Android App
1. **User Authentication & Quota Management**:
   - Secure Login (Username/Email & Password) with persistent session handling.
   - Registration, Email Verification status, Forgot Password & Password Reset.
   - Live Usage & Quota meters: Decompile limit/usage, Compile limit/usage, Keygen limit/usage, Signing limit/usage, Max upload size.
   - Admin badge & access controls.

2. **Multi-Project Management & Workflows**:
   - View, switch, rename, and delete projects in real-time on the shared MySQL database.
   - Upload APK binaries directly from device storage and trigger automated decompilation with Apktool.
   - Interactive Project File Explorer: Recursive folder navigation, breadcrumbs, folder/file icons, and file size metadata.

3. **Code, Smali & Visual Hex Editor**:
   - Full code editor for Smali bytecode, XML layouts, AndroidManifest, JSON, and Properties with instant save.
   - Replace project file with external images or binaries.
   - Visual Hex Editor for native `.so`, DEX, and ELF binaries: search byte patterns, view hex grid with ASCII preview, data inspector, and direct byte patching with offset.
   - AI Error Diagnostic & Automatic Fix: Diagnose build errors, review code for malformed syntax, and apply fixes with backup safety.

4. **Resource & AI Customizer Studio**:
   - Multi-locale Strings Translator (`values`, `values-es`, etc.) and App Name customization.
   - Google Services / Firebase `google-services.json` auto-injector.
   - App Icon / Launcher Customizer: Upload gallery images to auto-replace all mipmap densities (`mdpi` to `xxxhdpi`).
   - AI Launcher Icon Generator: Text prompt to generate new icons using AI models.
   - Global Project Find & Replace across all project files.

5. **Build, Keystores & Cryptographic Signing**:
   - Recompile project back into unsigned APK with live apktool logs.
   - Keystore Manager: View keystores, generate RSA 2048-bit JKS Keystores via `keytool`, select keystore, and delete.
   - Zipalign & Apksigner v2/v3 signing.
   - Direct APK Download with progress bar, SHA-256 integrity check, and instant launch of Android Package Installer.

6. **Device & Debugging Studio**:
   - Wireless ADB Device Manager: IP:port connect, device listing, disconnect, APK install over ADB, and live Logcat with filters.
   - Cloud Debug Logs: QR Code / Pairing token connect, live streaming device logs (2s auto-polling, pause/resume), and clear logs.
   - Background Auto-Check (every 30s) with persistent "New build ready" banner.

7. **AI Configuration Settings**:
   - Provider switcher (Google Gemini / OpenAI).
   - API Key manager (Gemini & OpenAI) with masked preview and delete.
   - Custom Model Selection for Gemini text/image and OpenAI text/image models.

8. **Admin Panel (for Administrator Accounts)**:
   - User account management & quota updates (+100 or custom limits).
   - Provision new user accounts with custom limits.
   - Contact inquiries inbox: read messages, mark as read, delete.
   - SEO Blog Post manager: create rich posts, edit, delete.
   - FAQ manager: add questions/answers, sort order, active status, edit, delete.
   - GitHub & Auto-Backup configuration and manual backup trigger.
   - Global default AI settings.

9. **Public Hub, Tutorials & Support**:
   - Searchable Blog & Tutorial reader.
   - Searchable FAQ viewer.
   - Documentation viewer.
   - Direct Contact Us inquiry submission form.

## Database & Backend Connection
- Server URL: Configurable in-app (defaults to `https://apk.zoomnearby.com/`).
- Database: MySQL database `apktool` (tables: `users`, `projects`, `key_details`, `jobs`, `contact_inquiries`, `blogs`, `faqs`, `app_settings`).
- Sessions: Maintained via `CookieManager` (`PHPSESSID`) and token credentials across all REST and Multipart API calls.

## Building the App
- **Codemagic (CI)**: Works with `codemagic.yaml` workflows `android-debug` and `android-release`.
- **Android Studio**: Open `/android-debugger` folder, sync Gradle, and run `assembleDebug` or `assembleRelease`.

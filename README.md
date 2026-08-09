# APK Tool Companion (React Native / Android)

A native Android companion app for your **APK Tool Studio** web platform. This app is a
**client** — all decompiling, recompiling, signing, and AI work still happens on your existing
PHP backend (apktool / keytool / apksigner). The app talks to it over the same `index.php`
AJAX endpoints the web UI uses.

## 1. Requirements

- Node.js 18+
- A JDK 17 and Android SDK (only needed for local builds — Codemagic provides these in CI)
- Your APK Tool Studio backend already deployed and reachable over HTTPS (or HTTP for local
  testing — cleartext traffic is allowed in this build, see note below)

## 2. Install & run locally

```bash
npm install
npx react-native run-android      # requires an emulator or a device with USB debugging
```

On first launch the app asks for your **server URL** (the root folder where `index.php` lives,
e.g. `https://apk.example.com`). This is stored on-device with AsyncStorage and can be changed
later from Settings → "Change server URL".

## 3. How it talks to your backend

- `src/api/client.ts` is the single HTTP layer. It POSTs `action=<name>` plus parameters to
  `<serverUrl>/index.php`, exactly like the web app's AJAX calls, and expects the same
  `{status, message, ...}` JSON shape.
- Auth is your existing PHP **session cookie** — `login` sets it, and every subsequent request
  sends `credentials: 'include'`. No token/JWT changes were made on the backend.
- File uploads (APK, images, `google-services.json`, replacement files) use `multipart/form-data`
  via `apiUpload()`.

Every backend action referenced in `index.php` has a typed wrapper under `src/api/`:
`auth.ts`, `projects.ts`, `workflow.ts`, `keystore.ts`, `ai.ts`, `adb.ts`, `admin.ts`.

## 4. Feature map (web → app)

| Web feature | App screen |
|---|---|
| Login / Register / Forgot / Reset password | `src/screens/auth/*` |
| Usage limits dashboard | `DashboardScreen` |
| Project list / rename / delete / switch | `ProjectsTab` |
| Upload APK & decompile | `ProjectsTab → New Project` |
| File browser | `WorkflowTab → Files` |
| Text file editor + AI review | `WorkflowTab → Files → (open a file)` |
| Binary/image replace | same editor, auto-detected for `.png/.jpg/.so/...` |
| App name & strings.xml editor | `WorkflowTab → App Name & Strings` |
| Project-wide find / find & replace | `WorkflowTab → Find & Replace` |
| Hex/binary search inside `.so` files | `WorkflowTab → Hex Search` |
| Apply `google-services.json` | `WorkflowTab → Firebase Config` |
| Replace launcher icon / AI-generate icon | `WorkflowTab → Logo & Icon` |
| AI build-error diagnosis & auto-fix | `WorkflowTab → AI Tools` |
| Keystore create / select | `WorkflowTab → Keystore` |
| Build (recompile) & sign | `WorkflowTab → Build & Sign` |
| ADB device connect / install | `WorkflowTab → ADB Devices` |
| Logcat viewer | `WorkflowTab → Logcat` |
| Cloud (non-ADB) debug logging | `WorkflowTab → Cloud Debug Logging` |
| AI provider & API key management | `SettingsTab → AI Settings` |
| **Admin:** users & limits | `AdminTab → Users` |
| **Admin:** contact form inquiries | `AdminTab → Contact Inquiries` |
| **Admin:** blog CRUD | `AdminTab → Blog Posts` |
| **Admin:** FAQ CRUD | `AdminTab → FAQs` |
| **Admin:** GitHub/cloud backup settings + manual run | `AdminTab → Backup Settings` |
| **Admin:** global AI defaults | `AdminTab → Global AI Defaults` |

The Admin tab only appears for accounts where the backend reports `user_type: "admin"`.

## 5. Building with Codemagic

`codemagic.yaml` is already at the project root with two workflows:

- **android-debug** — runs on every push, produces a debug APK, no signing setup needed.
- **android-release** — signed release build. Create an environment variable group named
  `android_signing` in Codemagic (Team settings → Environment variables) with these **secure**
  variables before running it:
  - `CM_KEYSTORE` — your release `.jks`/`.keystore` file, base64-encoded
    (`base64 -i your.keystore | pbcopy`)
  - `CM_KEYSTORE_PASSWORD`
  - `CM_KEY_ALIAS`
  - `CM_KEY_PASSWORD`

  It builds on the `release/*` branch pattern by default — adjust `triggering.branch_patterns`
  to match your workflow.

To build locally with your own release key instead, create `android/keystore.properties` (not
committed) and reference `MYAPP_RELEASE_STORE_FILE`, `MYAPP_RELEASE_STORE_PASSWORD`,
`MYAPP_RELEASE_KEY_ALIAS`, `MYAPP_RELEASE_KEY_PASSWORD` as Gradle properties, or pass them with
`-P` flags to `./gradlew assembleRelease`.

## 6. Notes & things to double check before shipping

- **`android:usesCleartextTraffic="true"`** is set in `AndroidManifest.xml` so the app can talk
  to an `http://` backend during setup/testing. If your production server is HTTPS-only (it
  should be), you can safely remove that attribute for the Play Store build.
- **Rotate your GitHub backup token.** Your uploaded `index.php` has a hardcoded fallback GitHub
  personal access token in the `get_admin_backup_settings` handler. Treat it as compromised,
  revoke it in GitHub → Settings → Developer settings, and paste a fresh one into
  Admin → Backup Settings once rotated.
- ADB features (`AdbScreen`, `LogcatScreen`) call your backend's ADB endpoints — the *server*
  needs `adb` installed and network/USB access to target devices, same as the web version. The
  phone running this app does not run adb itself.
- Cookie persistence across app restarts is best-effort (`persistSessionCookies` /
  `@react-native-cookies/cookies`). If your PHP session lifetime is short, users may need to log
  in again after a while — that mirrors how the web app already behaves.
- This app was scaffolded with React Native 0.76.5 / package `com.apktoolai.companion` to match
  your existing `codemagic.yaml`. Bump `versionCode`/`versionName` in
  `android/app/build.gradle` for each release you publish.

## 7. Project structure

```
src/
  api/            typed wrappers for every backend AJAX action
  components/     shared UI primitives (Button, Card, Input, Banner, ...)
  context/        AuthContext (session/limits), ProjectContext (open project state)
  navigation/     React Navigation stacks/tabs
  screens/
    auth/         login, register, forgot/reset password
    projects/     project list, new project (upload+decompile)
    workflow/     all per-project tools
    settings/     account, AI settings
    admin/        admin-only sections
  theme/          color/spacing tokens
  types/          shared TS interfaces
```

# APK Tool Companion (Android)

A tiny companion app so anyone can install builds from your hosted APK Tool
straight to their own phone over the internet - no ADB, no USB, no local
network setup, no port forwarding.

## What it does
1. Tap **Scan QR Code** and point the camera at the QR code shown on the web
   app's "Cloud Debug Logs" card (once cloud logging is enabled for a
   project) - no typing, no long codes. A "paste the code manually" fallback
   is still there for devices without camera access.
2. The app quietly re-checks the server in the background every ~30s while
   it's open, so the moment a new build lands on the backend a green
   **"New build ready - reinstall to reload the latest changes"** banner
   appears on its own. You can also tap **Check for Update** manually.
3. Tap **Realtime Logs → Start** to stream device/debug/network log lines
   the app has reported to the backend, updating every ~2 seconds.
4. Only a cryptographically verified signed APK is offered. Tap **Install
   signed APK**; the app verifies its size, SHA-256 digest, APK structure,
   and signer before handing it to Android's package installer.
5. Android always requires confirmation for a normal APK installation;
   silent installs are reserved for device-owner/root/system apps.

## What it can't do (Android platform limit, not a bug)
This app cannot read another app's raw logcat - Android only allows that via
ADB or root, never to a regular installed app. The **Realtime Logs** panel
instead shows what the web app's **Cloud Debug Logs** feature has captured:
it injects a small reporter directly into the built APK, so the target app
reports its own crashes/debug output to your server the moment it happens,
and this app polls that same feed - no ADB needed either.

## Building on Codemagic (CI)
This repo includes a ready-to-use `codemagic.yaml` at the project root with
two workflows:

- **android-debug** - builds an unsigned debug APK. Works with zero setup.
- **android-release** - builds a signed release APK. Needs a keystore added
  as encrypted environment variables first (see below).

Steps:
1. Push this project to a Git repository (GitHub, GitLab, or Bitbucket) -
   Codemagic builds from a connected repo, not a direct zip upload.
2. In Codemagic: **Add application** → select your repo → it will detect
   `codemagic.yaml` automatically and list both workflows.
3. Pick **android-debug**, click **Start new build**. No extra
   configuration needed - the `gradle wrapper` step in the YAML generates
   the Gradle wrapper on the CI machine itself (the wrapper jar isn't
   committed to the repo), then builds `assembleDebug`.
4. The built `.apk` shows up under the build's **Artifacts** tab.

### Signed release builds
To use **android-release**:
1. In Codemagic → your app → **Environment variables**, create a group
   named `companion_keystore` with:
   - `CM_KEYSTORE` - your `.jks`/`.keystore` file, base64-encoded
     (`base64 -i your.keystore | pbcopy` on macOS, or
     `base64 -w0 your.keystore` on Linux)
   - `CM_KEYSTORE_PASSWORD`
   - `CM_KEY_ALIAS`
   - `CM_KEY_PASSWORD`
   - mark all four as **secret**
2. In `codemagic.yaml`, uncomment the `groups: [companion_keystore]` line
   under the `android-release` workflow's `environment:` section.
3. Run the **android-release** workflow.

## Building locally in Android Studio
1. Open this folder in **Android Studio** (File → Open). Let it sync Gradle
   (it'll use Android Studio's bundled Gradle automatically).
2. Build → Build Bundle(s) / APK(s) → Build APK(s), or just Run ▶ on a
   connected/emulated device.
3. Requires: Android Studio Hedgehog+ (AGP 8.4), JDK 17. minSdk 24
   (Android 7.0+), targetSdk 34.

## Getting paired
In the web app, open a project → **8. Cloud Debug Logs** → **Enable Cloud
Debug Logging** → a "Pair the companion app" box appears with a QR code.
Open this app, tap **Scan QR Code**, and point the camera at it. A text
fallback code is also available behind "Show text code" for devices without
camera access - paste that into this app's **Connect** field instead.

## Distributing the built APK to end users
Once you've built `app-debug.apk` (or a signed release build), host it
somewhere (e.g. upload it as a project/build in your own APK Tool instance,
or any file host) so users can install the companion app itself once. After
that, everything else happens through pairing codes - no further manual
APK installs needed for future builds.

## Notes
- `usesCleartextTraffic="true"` is set so this also works against
  HTTP-only self-hosted servers; if your APK Tool install is HTTPS-only
  (recommended) this has no effect either way.
- The pairing "token" is a long random secret embedded in the pairing code
  - it's a bearer credential. Anyone with the pairing code can check for
  and download builds for that project. Don't share it publicly; re-enabling
  cloud logging doesn't rotate it, so if a code ever leaks, you'd need to
  add a rotate/reset option server-side (not included in this MVP).

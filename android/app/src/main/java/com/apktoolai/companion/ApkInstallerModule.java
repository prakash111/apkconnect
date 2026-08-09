package com.apktoolai.companion;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Downloads a signed APK (with the caller's cookie/auth header if provided) to the app's
 * private cache dir, reporting progress, then hands it to the system package installer via a
 * FileProvider content:// URI so no legacy "unknown storage" access is required.
 */
public class ApkInstallerModule extends ReactContextBaseJavaModule {

  private static final String EVENT_PROGRESS = "ApkDownloadProgress";
  private Thread downloadThread;
  private volatile boolean cancelled = false;

  ApkInstallerModule(ReactApplicationContext context) {
    super(context);
  }

  @NonNull
  @Override
  public String getName() {
    return "ApkInstaller";
  }

  private void emit(String event, WritableMap params) {
    ReactApplicationContext ctx = getReactApplicationContext();
    if (ctx == null || !ctx.hasActiveReactInstance()) return;
    ctx.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(event, params);
  }

  /**
   * Downloads the APK at `url` (optionally sending a Cookie header for session auth) into
   * <cache>/apks/<fileName>, emitting "ApkDownloadProgress" ({progress: 0..1, bytesWritten,
   * totalBytes}) along the way, then resolves with the local file path.
   */
  @ReactMethod
  public void downloadApk(String url, String fileName, String cookieHeader, Promise promise) {
    cancelled = false;
    downloadThread = new Thread(() -> {
      HttpURLConnection conn = null;
      try {
        File dir = new File(getReactApplicationContext().getCacheDir(), "apks");
        if (!dir.exists()) dir.mkdirs();
        File outFile = new File(dir, fileName);

        URL u = new URL(url);
        conn = (HttpURLConnection) u.openConnection();
        conn.setRequestProperty("Accept", "*/*");
        if (cookieHeader != null && !cookieHeader.isEmpty()) {
          conn.setRequestProperty("Cookie", cookieHeader);
        }
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);
        conn.connect();

        int status = conn.getResponseCode();
        if (status < 200 || status >= 300) {
          promise.reject("HTTP_" + status, "Server returned HTTP " + status);
          return;
        }

        long totalBytes = conn.getContentLengthLong();
        long bytesWritten = 0;
        byte[] buffer = new byte[8192];

        try (InputStream in = conn.getInputStream();
             FileOutputStream out = new FileOutputStream(outFile)) {
          int read;
          long lastEmit = 0;
          while ((read = in.read(buffer)) != -1) {
            if (cancelled) {
              promise.reject("CANCELLED", "Download cancelled.");
              return;
            }
            out.write(buffer, 0, read);
            bytesWritten += read;

            // Throttle progress events to roughly every 64KB to avoid flooding the JS bridge.
            if (bytesWritten - lastEmit > 65536 || bytesWritten == totalBytes) {
              lastEmit = bytesWritten;
              WritableMap p = Arguments.createMap();
              p.putDouble("bytesWritten", bytesWritten);
              p.putDouble("totalBytes", totalBytes);
              p.putDouble("progress", totalBytes > 0 ? ((double) bytesWritten / totalBytes) : -1);
              emit(EVENT_PROGRESS, p);
            }
          }
        }

        promise.resolve(outFile.getAbsolutePath());
      } catch (Exception e) {
        promise.reject("DOWNLOAD_FAILED", e.getMessage(), e);
      } finally {
        if (conn != null) conn.disconnect();
      }
    }, "ApkDownloadThread");
    downloadThread.start();
  }

  @ReactMethod
  public void cancelDownload(Promise promise) {
    cancelled = true;
    promise.resolve(true);
  }

  /**
   * Launches the system package installer for a previously-downloaded APK file path,
   * using a FileProvider content:// URI (required on Android N+).
   */
  @ReactMethod
  public void installApk(String filePath, Promise promise) {
    try {
      Context context = getReactApplicationContext();
      File file = new File(filePath);
      if (!file.exists()) {
        promise.reject("FILE_NOT_FOUND", "APK file does not exist: " + filePath);
        return;
      }

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
          && !context.getPackageManager().canRequestPackageInstalls()) {
        // Let the caller know so the JS side can prompt the user to grant the
        // "install unknown apps" permission before retrying.
        promise.reject(
            "INSTALL_PERMISSION_REQUIRED",
            "The user must grant \"Install unknown apps\" permission for this app first.");
        return;
      }

      Uri apkUri = FileProvider.getUriForFile(
          context, context.getPackageName() + ".apkprovider", file);

      Intent intent = new Intent(Intent.ACTION_VIEW);
      intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

      context.startActivity(intent);
      promise.resolve(true);
    } catch (Exception e) {
      promise.reject("INSTALL_FAILED", e.getMessage(), e);
    }
  }

  /** Opens the system settings screen where the user can allow this app to install unknown apps. */
  @ReactMethod
  public void openInstallPermissionSettings(Promise promise) {
    try {
      Context context = getReactApplicationContext();
      Intent intent;
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        intent = new Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
      } else {
        intent = new Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS);
      }
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      context.startActivity(intent);
      promise.resolve(true);
    } catch (Exception e) {
      promise.reject("SETTINGS_FAILED", e.getMessage(), e);
    }
  }
}

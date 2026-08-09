import { NativeEventEmitter, NativeModules, Platform } from 'react-native';
import CookieManager from '@react-native-cookies/cookies';
import { getBaseUrl } from './client';

const { ApkInstaller } = NativeModules as {
  ApkInstaller?: {
    downloadApk: (url: string, fileName: string, cookieHeader: string) => Promise<string>;
    cancelDownload: () => Promise<boolean>;
    installApk: (filePath: string) => Promise<boolean>;
    openInstallPermissionSettings: () => Promise<boolean>;
  };
};

export interface DownloadProgress {
  bytesWritten: number;
  totalBytes: number;
  /** 0..1, or -1 if the server didn't send a Content-Length. */
  progress: number;
}

/**
 * Subscribes to native download progress events. Returns an unsubscribe function.
 * Android-only; no-op elsewhere.
 */
export function onApkDownloadProgress(listener: (p: DownloadProgress) => void): () => void {
  if (Platform.OS !== 'android' || !ApkInstaller) return () => {};
  const emitter = new NativeEventEmitter(NativeModules.ApkInstaller);
  const sub = emitter.addListener('ApkDownloadProgress', listener);
  return () => sub.remove();
}

async function buildCookieHeader(baseUrl: string): Promise<string> {
  try {
    const cookies = await CookieManager.get(baseUrl);
    return Object.entries(cookies)
      .map(([name, c]: [string, any]) => `${name}=${c.value}`)
      .join('; ');
  } catch {
    return '';
  }
}

/**
 * Downloads the given server-relative signed-APK filename from this project's backend and
 * launches the system package installer for it. Reports progress via `onProgress`.
 *
 * Throws with code `INSTALL_PERMISSION_REQUIRED` if the user hasn't granted this app
 * "install unknown apps" permission yet — call `openInstallPermissionSettings()` and retry.
 */
export async function downloadAndInstallApk(
  serverApkPath: string,
  onProgress?: (p: DownloadProgress) => void,
): Promise<void> {
  if (Platform.OS !== 'android' || !ApkInstaller) {
    throw new Error('In-app APK install is only supported on Android.');
  }

  const baseUrl = await getBaseUrl();
  const cleanBase = baseUrl.replace(/\/+$/, '').replace(/\/index\.php$/i, '');
  const downloadUrl = `${cleanBase}/index.php?download=${encodeURIComponent(serverApkPath)}`;
  const fileName = (serverApkPath.split(/[/\\]/).pop() || 'signed.apk').replace(/[^a-zA-Z0-9._-]/g, '_');
  const cookieHeader = await buildCookieHeader(cleanBase);

  const unsubscribe = onProgress ? onApkDownloadProgress(onProgress) : () => {};
  try {
    const localPath = await ApkInstaller.downloadApk(downloadUrl, fileName, cookieHeader);
    await ApkInstaller.installApk(localPath);
  } finally {
    unsubscribe();
  }
}

export async function cancelApkDownload(): Promise<void> {
  if (Platform.OS !== 'android' || !ApkInstaller) return;
  await ApkInstaller.cancelDownload();
}

export async function openInstallPermissionSettings(): Promise<void> {
  if (Platform.OS !== 'android' || !ApkInstaller) return;
  await ApkInstaller.openInstallPermissionSettings();
}

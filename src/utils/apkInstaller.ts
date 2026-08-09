import { NativeModules, Linking, Platform, Alert } from 'react-native';
import RNFS from 'react-native-fs';
import { getBaseUrl } from '../api/client';

const { ApkInstaller } = NativeModules;

export interface InstallProgress {
  bytesWritten: number;
  contentLength: number;
  percentage: number;
}

/**
 * Resolves the full download URL for an APK path stored on the server.
 */
export async function getApkDownloadUrl(apkPath: string): Promise<string> {
  const baseUrl = await getBaseUrl();
  const cleanBase = baseUrl.replace(/\/+$/, '').replace(/\/index\.php$/i, '');
  return `${cleanBase}/index.php?download=${encodeURIComponent(apkPath)}`;
}

/**
 * Downloads a signed APK directly to the device cache/downloads and invokes
 * the native Android package installer.
 */
export async function downloadAndInstallApk(
  apkPath: string,
  onProgress?: (progress: InstallProgress) => void,
): Promise<{ success: boolean; message?: string }> {
  try {
    if (!apkPath) {
      throw new Error('No APK path provided for installation.');
    }

    const downloadUrl = await getApkDownloadUrl(apkPath);
    const fileName = apkPath.split(/[/\\]/).pop() || 'app-signed.apk';
    const targetDir = Platform.OS === 'android' ? RNFS.CachesDirectoryPath : RNFS.DocumentDirectoryPath;
    const localFilePath = `${targetDir}/${fileName}`;

    // Remove any previously downloaded stale file with the same name
    const exists = await RNFS.exists(localFilePath);
    if (exists) {
      try {
        await RNFS.unlink(localFilePath);
      } catch {
        // non-fatal
      }
    }

    // Download the APK file
    const downloadRes = await RNFS.downloadFile({
      fromUrl: downloadUrl,
      toFile: localFilePath,
      background: true,
      progressInterval: 250,
      progress: (res) => {
        if (onProgress && res.contentLength > 0) {
          const pct = Math.min(100, Math.round((res.bytesWritten / res.contentLength) * 100));
          onProgress({
            bytesWritten: res.bytesWritten,
            contentLength: res.contentLength,
            percentage: pct,
          });
        }
      },
    }).promise;

    if (downloadRes.statusCode >= 200 && downloadRes.statusCode < 300) {
      if (onProgress) {
        onProgress({ bytesWritten: 100, contentLength: 100, percentage: 100 });
      }

      // Invoke Native Package Installer
      if (Platform.OS === 'android' && ApkInstaller?.installApk) {
        try {
          await ApkInstaller.installApk(localFilePath);
          return { success: true, message: 'Package installer launched.' };
        } catch (nativeErr: any) {
          // Fallback to Linking.openURL
          await Linking.openURL(downloadUrl);
          return { success: true, message: 'Opened download in installer.' };
        }
      } else {
        // Fallback to open URL
        await Linking.openURL(downloadUrl);
        return { success: true, message: 'Opened download URL.' };
      }
    } else {
      throw new Error(`Server returned HTTP ${downloadRes.statusCode} during download.`);
    }
  } catch (err: any) {
    // If RNFS download fails or is not available, fallback to Linking.openURL directly
    try {
      const downloadUrl = await getApkDownloadUrl(apkPath);
      await Linking.openURL(downloadUrl);
      return { success: true, message: 'Opened download in browser/system downloader.' };
    } catch (fallbackErr: any) {
      throw new Error(err?.message || fallbackErr?.message || 'Could not download and install APK.');
    }
  }
}

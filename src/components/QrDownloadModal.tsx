import React, { useState, useEffect } from 'react';
import {
  Modal,
  View,
  Text,
  StyleSheet,
  Image,
  TouchableOpacity,
  ScrollView,
  Share,
} from 'react-native';
import { Card, Button, Input, SectionTitle, HelperText, ProgressBar } from './UI';
import { colors, radius, spacing } from '../theme/theme';
import { downloadAndInstallApk, InstallProgress } from '../utils/apkInstaller';
import { getBaseUrl } from '../api/client';

export interface QrDownloadModalProps {
  visible: boolean;
  onClose: () => void;
  apkPath?: string;
  projectName?: string;
  crashReportToken?: string;
}

export function QrDownloadModal({
  visible,
  onClose,
  apkPath,
  projectName = 'Application',
  crashReportToken,
}: QrDownloadModalProps) {
  const [downloadUrl, setDownloadUrl] = useState('');
  const [copied, setCopied] = useState(false);
  const [installing, setInstalling] = useState(false);
  const [installProgress, setInstallProgress] = useState<InstallProgress | null>(null);
  const [customQrUrl, setCustomQrUrl] = useState('');
  const [statusMessage, setStatusMessage] = useState('');

  useEffect(() => {
    async function prepareUrl() {
      if (!apkPath) return;
      const base = await getBaseUrl();
      const cleanBase = base.trim().replace(/\/+$/, '').replace(/\/index\.php$/i, '');
      let fullUrl = '';
      if (crashReportToken) {
        fullUrl = `${cleanBase}/index.php?device_download=1&token=${encodeURIComponent(crashReportToken)}`;
      } else {
        fullUrl = `${cleanBase}/index.php?download=${encodeURIComponent(apkPath)}`;
      }
      setDownloadUrl(fullUrl);
    }
    if (visible) {
      prepareUrl();
      setCopied(false);
      setStatusMessage('');
    }
  }, [visible, apkPath, crashReportToken]);

  const qrImageUrl = downloadUrl
    ? `https://api.qrserver.com/v1/create-qr-code/?size=250x250&margin=10&data=${encodeURIComponent(downloadUrl)}`
    : '';

  const fileName = apkPath ? apkPath.split(/[/\\]/).pop() : 'signed_app.apk';

  const handleCopyLink = async () => {
    if (!downloadUrl) return;
    try {
      await Share.share({
        message: downloadUrl,
        title: `Download ${projectName} Signed APK`,
      });
      setCopied(true);
      setTimeout(() => setCopied(false), 3000);
    } catch {
      // ignore
    }
  };

  const handleInstallFromModal = async (targetUrl?: string) => {
    const urlToUse = targetUrl || apkPath || downloadUrl;
    if (!urlToUse) return;
    setInstalling(true);
    setInstallProgress({ bytesWritten: 0, contentLength: 100, percentage: 0 });
    setStatusMessage('Downloading signed application...');
    try {
      const res = await downloadAndInstallApk(urlToUse, (prog) => {
        setInstallProgress(prog);
        setStatusMessage(`Downloading: ${prog.percentage}%`);
      });
      if (res.success) {
        setStatusMessage('Download completed! Prompting Android installer...');
      }
    } catch (e: any) {
      setStatusMessage(`Error: ${e?.message || 'Failed to install'}`);
    } finally {
      setInstalling(false);
      setInstallProgress(null);
    }
  };

  return (
    <Modal
      visible={visible}
      animationType="slide"
      transparent
      onRequestClose={onClose}>
      <View style={styles.backdrop}>
        <View style={styles.modalContent}>
          <ScrollView contentContainerStyle={styles.scroll}>
            {/* MODAL HEADER */}
            <View style={styles.headerRow}>
              <View style={{ flex: 1 }}>
                <Text style={styles.modalTitle}>📲 QR Scan & Install Application</Text>
                <Text style={styles.modalSub}>{projectName} • Signed APK Ready</Text>
              </View>
              <TouchableOpacity onPress={onClose} style={styles.closeBtn}>
                <Text style={styles.closeBtnText}>✕</Text>
              </TouchableOpacity>
            </View>

            {/* EMBEDDED CLOUD DEBUGGING NOTICE CARD */}
            <View style={styles.cloudNoticeCard}>
              <View style={styles.cloudNoticeHeader}>
                <Text style={styles.cloudNoticeTitle}>☁️ Cloud Debug Logging Active & Ready</Text>
                <View style={styles.cloudBadge}>
                  <Text style={styles.cloudBadgeText}>EMBEDDED CLOUD DEBUGGING</Text>
                </View>
              </View>
              <Text style={styles.cloudNoticeText}>
                Cloud logging is directly embedded into your project's Smali bytecode. When your signed APK runs on any Android device, it streams crashes, native exceptions, network traffic, and Logcat debug logs straight to the Developer Tools below in real time over the cloud — no QR codes, pairing, or local ADB connection required.
              </Text>
            </View>

            {/* QR CODE CONTAINER CARD */}
            <Card style={styles.qrCard}>
              <Text style={styles.qrSectionHeader}>Scan QR Code to Download & Install</Text>
              <HelperText>
                Point any Android phone camera or QR scanner app at the code below to immediately download and launch package installation.
              </HelperText>

              {qrImageUrl ? (
                <View style={styles.qrWrapper}>
                  <View style={styles.qrBox}>
                    <Image
                      source={{ uri: qrImageUrl }}
                      style={styles.qrImage}
                      resizeMode="contain"
                    />
                  </View>
                  <Text style={styles.fileNameText}>📦 {fileName}</Text>
                </View>
              ) : null}

              {/* DIRECT URL DISPLAY & COPY BUTTON */}
              <View style={styles.urlBox}>
                <Text style={styles.urlLabel}>Direct Download URL:</Text>
                <Text style={styles.urlText} numberOfLines={2} selectable>
                  {downloadUrl}
                </Text>
              </View>

              <View style={styles.actionBtnRow}>
                <Button
                  title={copied ? '✓ Link Shared/Copied' : '📋 Share / Copy Link'}
                  variant="secondary"
                  small
                  onPress={handleCopyLink}
                  style={{ flex: 1 }}
                />
                <Button
                  title={installing ? '📲 Downloading…' : '⬇ Install Now'}
                  small
                  onPress={() => handleInstallFromModal(apkPath)}
                  loading={installing}
                  style={{ flex: 1 }}
                />
              </View>
            </Card>

            {/* INSTALL PROGRESS BAR IF INSTALLING */}
            {installProgress && installProgress.percentage > 0 ? (
              <View style={styles.progressBox}>
                <View style={styles.progressRow}>
                  <Text style={styles.progressText}>Downloading APK...</Text>
                  <Text style={styles.progressPct}>{installProgress.percentage}%</Text>
                </View>
                <ProgressBar value={installProgress.percentage / 100} />
              </View>
            ) : null}

            {statusMessage ? (
              <Text style={styles.statusText}>{statusMessage}</Text>
            ) : null}

            {/* MANUAL QR SCANNED URL INPUT FOR DEVICE INSTALLATION */}
            <Card style={styles.scanInputCard}>
              <SectionTitle>📷 Scanned QR URL / Manual Download</SectionTitle>
              <HelperText>
                Have a scanned QR code URL or custom APK download link? Paste or type it here to install directly on this Android device.
              </HelperText>
              <Input
                placeholder="https://apk.zoomnearby.com/index.php?download=..."
                value={customQrUrl}
                onChangeText={setCustomQrUrl}
                autoCapitalize="none"
                style={{ marginTop: 8 }}
              />
              <Button
                title="⚡ Download & Install Scanned QR URL"
                variant="secondary"
                disabled={!customQrUrl.trim()}
                onPress={() => handleInstallFromModal(customQrUrl.trim())}
                loading={installing}
                style={{ marginTop: 8 }}
              />
            </Card>

            <Button title="Close" variant="ghost" onPress={onClose} style={{ marginTop: 12 }} />
          </ScrollView>
        </View>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  backdrop: {
    flex: 1,
    backgroundColor: 'rgba(5, 10, 20, 0.85)',
    justifyContent: 'center',
    padding: spacing.md,
  },
  modalContent: {
    backgroundColor: '#0a1220',
    borderRadius: radius.md,
    borderColor: '#1d4ed8',
    borderWidth: 1.5,
    maxHeight: '90%',
    overflow: 'hidden',
  },
  scroll: {
    padding: spacing.md,
  },
  headerRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: spacing.sm,
  },
  modalTitle: {
    color: '#ffffff',
    fontSize: 17,
    fontWeight: '800',
  },
  modalSub: {
    color: '#93c5fd',
    fontSize: 12,
    marginTop: 2,
  },
  closeBtn: {
    width: 32,
    height: 32,
    borderRadius: 16,
    backgroundColor: 'rgba(255, 255, 255, 0.1)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  closeBtnText: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: '700',
  },
  cloudNoticeCard: {
    backgroundColor: '#0c1a2e',
    borderColor: '#1d4ed8',
    borderWidth: 1,
    borderRadius: radius.sm,
    padding: spacing.sm,
    marginBottom: spacing.md,
  },
  cloudNoticeHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    flexWrap: 'wrap',
    gap: 6,
    marginBottom: 6,
  },
  cloudNoticeTitle: {
    color: '#93c5fd',
    fontWeight: '800',
    fontSize: 13,
  },
  cloudBadge: {
    backgroundColor: 'rgba(34, 197, 94, 0.2)',
    borderColor: 'rgba(34, 197, 94, 0.5)',
    borderWidth: 1,
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: 10,
  },
  cloudBadgeText: {
    color: '#4ade80',
    fontSize: 9.5,
    fontWeight: '800',
  },
  cloudNoticeText: {
    color: '#cbd5e1',
    fontSize: 11.5,
    lineHeight: 16,
  },
  qrCard: {
    backgroundColor: '#060d19',
    borderColor: '#22c55e',
    borderWidth: 1.5,
    alignItems: 'center',
  },
  qrSectionHeader: {
    color: '#4ade80',
    fontSize: 15,
    fontWeight: '800',
    marginBottom: 4,
    textAlign: 'center',
  },
  qrWrapper: {
    alignItems: 'center',
    marginVertical: spacing.md,
  },
  qrBox: {
    padding: 10,
    backgroundColor: '#ffffff',
    borderRadius: 12,
    elevation: 4,
    shadowColor: '#000',
    shadowOpacity: 0.3,
    shadowRadius: 6,
    shadowOffset: { width: 0, height: 3 },
  },
  qrImage: {
    width: 210,
    height: 210,
  },
  fileNameText: {
    color: '#86efac',
    fontSize: 12,
    fontFamily: 'monospace',
    marginTop: 8,
    fontWeight: '700',
  },
  urlBox: {
    width: '100%',
    backgroundColor: '#0c1628',
    borderRadius: radius.sm,
    padding: spacing.sm,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.1)',
    marginBottom: spacing.sm,
  },
  urlLabel: {
    color: '#94a3b8',
    fontSize: 11,
    fontWeight: '600',
    marginBottom: 2,
  },
  urlText: {
    color: '#38bdf8',
    fontSize: 11,
    fontFamily: 'monospace',
  },
  actionBtnRow: {
    flexDirection: 'row',
    gap: spacing.sm,
    width: '100%',
  },
  progressBox: {
    marginVertical: spacing.sm,
    backgroundColor: 'rgba(0,0,0,0.3)',
    padding: spacing.sm,
    borderRadius: radius.sm,
  },
  progressRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 4,
  },
  progressText: { color: '#86efac', fontSize: 11, fontWeight: '600' },
  progressPct: { color: '#ffffff', fontSize: 11, fontWeight: '700' },
  statusText: {
    color: '#86efac',
    fontSize: 12,
    textAlign: 'center',
    marginVertical: 4,
    fontWeight: '600',
  },
  scanInputCard: {
    backgroundColor: '#081222',
    borderColor: colors.border,
    borderWidth: 1,
    marginTop: spacing.xs,
  },
});

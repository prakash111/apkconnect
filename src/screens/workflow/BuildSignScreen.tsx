import React, { useState } from 'react';
import { Text, StyleSheet, ScrollView, View, Alert } from 'react-native';
import { Card, Input, Label, Button, Banner, HelperText, SectionTitle, ProgressBar } from '../../components/UI';
import * as workflowApi from '../../api/workflow';
import * as keystoreApi from '../../api/keystore';
import { downloadAndInstallApk, openInstallPermissionSettings } from '../../api/apkInstall';
import { useProject } from '../../context/ProjectContext';
import { colors, radius, spacing } from '../../theme/theme';

function cleanLog(rawLog?: string): string {
  if (!rawLog) return '';
  let log = rawLog;
  log = log.replace(/file:\/\/\/[^\s"':]+/gi, '');
  log = log.replace(/(?:\/[^\s"':]+)?\/uploads\/workflow-projects\/[^\s"':]+\/[^\s"':]+\/decompiled\/?/gi, 'decompiled/');
  log = log.replace(/(?:\/[^\s"':]+)?\/uploads\/workflow-projects\/[^\s"':]+\/[^\s"':]+\/?/gi, '');
  log = log.replace(/\/home\/[^\s"':]+\/htdocs\/[^\s"':]+\/?/gi, '');
  log = log.replace(/\/home\/[^\s"':]+\/?/gi, '');
  return log;
}

function getFileName(path?: string): string {
  if (!path) return '';
  return path.split(/[/\\]/).pop() || path;
}

export default function BuildSignScreen({ navigation }: any) {
  const { state, refreshState } = useProject();
  const [signPassword, setSignPassword] = useState('');
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [building, setBuilding] = useState(false);
  const [signing, setSigning] = useState(false);
  const [downloading, setDownloading] = useState(false);
  const [downloadProgress, setDownloadProgress] = useState(0);
  const [enablingCloudLog, setEnablingCloudLog] = useState(false);

  const onBuild = async () => {
    setBuilding(true);
    setError('');
    setMessage('');
    try {
      const res = await workflowApi.buildApk();
      if (res.status === 'success') {
        setMessage(res.message || 'Build succeeded.');
        await refreshState();
      } else {
        setError(res.message || 'Build failed.');
        await refreshState();
      }
    } catch (e: any) {
      setError(e?.message || 'Build failed.');
    } finally {
      setBuilding(false);
    }
  };

  const onSign = async () => {
    if (!signPassword) {
      setError('Enter your keystore password.');
      return;
    }
    setSigning(true);
    setError('');
    setMessage('');
    try {
      const res = await keystoreApi.signApk(signPassword);
      if (res.status === 'success') {
        setMessage(res.message || 'APK signed successfully.');
        await refreshState();
      } else {
        setError(res.message || 'Signing failed.');
      }
    } catch (e: any) {
      setError(e?.message || 'Signing failed.');
    } finally {
      setSigning(false);
    }
  };

  const onDownloadSignedApk = async () => {
    if (!state?.signed_apk) return;
    setDownloading(true);
    setDownloadProgress(0);
    setError('');
    setMessage('');
    try {
      await downloadAndInstallApk(state.signed_apk, p => {
        if (p.progress >= 0) setDownloadProgress(p.progress);
      });
      setMessage('Downloaded — opening the installer…');
    } catch (e: any) {
      if (e?.code === 'INSTALL_PERMISSION_REQUIRED' || /INSTALL_PERMISSION_REQUIRED/.test(e?.message || '')) {
        Alert.alert(
          'Allow installs from this app',
          'To install the APK directly, allow "APKTOOL Studio" to install unknown apps in the next screen, then come back and tap Download & Install again.',
          [
            { text: 'Cancel', style: 'cancel' },
            { text: 'Open settings', onPress: () => openInstallPermissionSettings() },
          ],
        );
      } else {
        setError(e?.message || 'Download failed.');
      }
    } finally {
      setDownloading(false);
    }
  };

  const onEnableCloudLogging = async () => {
    setEnablingCloudLog(true);
    setError('');
    setMessage('');
    try {
      const res = await workflowApi.enableCloudLogging();
      if (res.status === 'success') {
        setMessage(res.message || 'Cloud logging enabled — rebuild & sign again for it to take effect.');
        await refreshState();
      } else {
        setError(res.message || 'Could not enable cloud logging.');
      }
    } catch (e: any) {
      setError(e?.message || 'Could not enable cloud logging.');
    } finally {
      setEnablingCloudLog(false);
    }
  };

  const sanitizedLog = cleanLog(state?.last_build_log);
  const signedApkFileName = getFileName(state?.signed_apk);
  const unsignedApkFileName = getFileName(state?.unsigned_apk);
  const activeKeystoreAlias = state?.keystore_alias || 'None selected';

  return (
    <ScrollView style={styles.flex} contentContainerStyle={styles.content}>
      <Banner type="error" message={error} />
      <Banner type="success" message={message} />

      {/* 1. BUILD UNSIGNED APK */}
      <Card>
        <SectionTitle>1. Build (Recompile)</SectionTitle>
        <HelperText>Recompiles the decompiled project back into an Android APK binary with apktool.</HelperText>
        <Button title="Build unsigned APK" onPress={onBuild} loading={building} />
        {state?.unsigned_apk ? (
          <View style={styles.badgeRow}>
            <Text style={styles.badgeSuccess}>✓ Unsigned APK ready:</Text>
            <Text style={styles.badgeFile}>{unsignedApkFileName}</Text>
          </View>
        ) : null}
      </Card>

      {/* 2. SIGN APK */}
      <Card>
        <SectionTitle>2. Sign APK</SectionTitle>
        <HelperText>
          Signs the binary with active keystore ({activeKeystoreAlias}). To change keystore, visit Keystore section.
        </HelperText>
        <Label>Keystore password</Label>
        <Input
          value={signPassword}
          onChangeText={setSignPassword}
          secureTextEntry
          placeholder="Enter keystore password"
        />
        <Button
          title="Sign APK"
          onPress={onSign}
          loading={signing}
          disabled={!state?.unsigned_apk || !state?.keystore_alias}
        />
      </Card>

      {/* 3. SIGNED APK OUTPUT CARD */}
      {state?.signed_apk ? (
        <Card style={styles.outputCard}>
          <View style={styles.outputHeader}>
            <View style={styles.outputIconCircle}>
              <Text style={styles.outputEmoji}>🎉</Text>
            </View>
            <View style={{ flex: 1 }}>
              <Text style={styles.outputTitle}>Signed APK Ready</Text>
              <Text style={styles.outputFileName}>{signedApkFileName}</Text>
            </View>
          </View>

          <Text style={styles.outputDesc}>
            Your Android package has been signed with V2/V3 schemes and is ready for installation or store publishing.
          </Text>

          {downloading ? (
            <View style={{ marginBottom: spacing.sm }}>
              <ProgressBar value={downloadProgress} />
              <Text style={styles.progressLabel}>
                {downloadProgress > 0 ? `Downloading… ${Math.round(downloadProgress * 100)}%` : 'Downloading…'}
              </Text>
            </View>
          ) : null}

          <View style={styles.outputBtnRow}>
            <Button
              title="⬇ Download & Install"
              onPress={onDownloadSignedApk}
              loading={downloading}
              style={styles.downloadBtn}
            />
            <Button
              title="📲 Install via ADB"
              variant="secondary"
              onPress={() => navigation.navigate('Adb')}
              style={styles.adbBtn}
            />
          </View>

          <View style={styles.outputBtnRow}>
            <Button
              title={state?.cloud_logging_enabled ? '☁ Cloud logging: ON' : '☁ Enable cloud logging'}
              variant={state?.cloud_logging_enabled ? 'ghost' : 'secondary'}
              onPress={onEnableCloudLogging}
              loading={enablingCloudLog}
              style={{ flex: 1, marginTop: spacing.sm }}
            />
            <Button
              title="📋 View device logs"
              variant="ghost"
              onPress={() => navigation.navigate('CloudLogging')}
              style={{ flex: 1, marginTop: spacing.sm }}
            />
          </View>
          {state?.cloud_logging_enabled ? (
            <HelperText>
              Cloud logging is baked into the build above. Once installed on a device, logs stream to the
              "View device logs" screen in this app and in the web dashboard.
            </HelperText>
          ) : (
            <HelperText>
              Enabling cloud logging injects a lightweight shim into the project so logs from any device stream
              back here — you'll need to rebuild & re-sign afterward for it to apply.
            </HelperText>
          )}
        </Card>
      ) : null}

      {/* 4. BUILD & COMPILER LOGS */}
      {sanitizedLog ? (
        <Card>
          <View style={styles.logHeader}>
            <SectionTitle>Build Logs (decompiled/)</SectionTitle>
            {state?.last_build_failed ? (
              <View style={styles.statusBadgeFailed}>
                <Text style={styles.statusBadgeTextFailed}>FAILED</Text>
              </View>
            ) : (
              <View style={styles.statusBadgeOk}>
                <Text style={styles.statusBadgeTextOk}>SUCCESS</Text>
              </View>
            )}
          </View>
          <ScrollView style={styles.logBox} nestedScrollEnabled>
            <Text style={styles.logText} selectable>
              {sanitizedLog}
            </Text>
          </ScrollView>
          {state?.last_build_failed ? (
            <Button
              title="✨ Ask AI to Analyze & Fix This Error"
              variant="secondary"
              onPress={() => navigation.navigate('AiTools')}
              style={{ marginTop: spacing.sm }}
            />
          ) : null}
        </Card>
      ) : null}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1, backgroundColor: colors.bg },
  content: { padding: spacing.md, paddingBottom: spacing.xl },
  badgeRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: spacing.sm,
    flexWrap: 'wrap',
    gap: 4,
  },
  badgeSuccess: { color: colors.success, fontWeight: '700', fontSize: 13 },
  badgeFile: { color: colors.text, fontSize: 13, fontFamily: 'monospace' },
  outputCard: {
    borderColor: 'rgba(34, 197, 94, 0.4)',
    backgroundColor: '#0f241a',
    borderWidth: 1.5,
  },
  outputHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: spacing.sm,
  },
  outputIconCircle: {
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: 'rgba(34, 197, 94, 0.2)',
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: spacing.sm,
  },
  outputEmoji: { fontSize: 22 },
  outputTitle: { color: '#ffffff', fontWeight: '800', fontSize: 17 },
  outputFileName: { color: '#86efac', fontSize: 13, fontFamily: 'monospace', marginTop: 2 },
  outputDesc: { color: '#d1fae5', fontSize: 13, lineHeight: 18, marginBottom: spacing.md },
  outputBtnRow: {
    flexDirection: 'row',
    gap: spacing.sm,
  },
  downloadBtn: { flex: 1, backgroundColor: colors.success, marginTop: 0 },
  adbBtn: { flex: 1, marginTop: 0 },
  progressLabel: { color: '#d1fae5', fontSize: 11, marginTop: 4, textAlign: 'center' },
  logHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: spacing.xs,
  },
  statusBadgeOk: {
    backgroundColor: 'rgba(34, 197, 94, 0.2)',
    paddingHorizontal: 8,
    paddingVertical: 2,
    borderRadius: 4,
  },
  statusBadgeTextOk: { color: colors.success, fontSize: 11, fontWeight: '700' },
  statusBadgeFailed: {
    backgroundColor: 'rgba(239, 68, 68, 0.2)',
    paddingHorizontal: 8,
    paddingVertical: 2,
    borderRadius: 4,
  },
  statusBadgeTextFailed: { color: colors.danger, fontSize: 11, fontWeight: '700' },
  logBox: {
    maxHeight: 240,
    backgroundColor: '#090d16',
    borderRadius: 8,
    padding: spacing.sm,
    borderWidth: 1,
    borderColor: colors.border,
  },
  logText: {
    color: '#cbd5e1',
    fontFamily: 'monospace',
    fontSize: 11,
    lineHeight: 16,
  },
});

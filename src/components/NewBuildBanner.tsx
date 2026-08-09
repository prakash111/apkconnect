import React, { useState } from 'react';
import { View, Text, StyleSheet, Pressable } from 'react-native';
import { useProject } from '../context/ProjectContext';
import { downloadAndInstallApk, openInstallPermissionSettings } from '../api/apkInstall';
import { ProgressBar } from './UI';
import { colors, radius, spacing } from '../theme/theme';

/**
 * Renders a small "New build available" strip above the tab bar whenever the polled
 * project state reports a signed APK that's different from the one the user last saw.
 * Lets them install it right away without hunting for the Build & Sign screen.
 */
export default function NewBuildBanner() {
  const { newBuildAvailable, newBuildFile, dismissNewBuild } = useProject();
  const [installing, setInstalling] = useState(false);
  const [progress, setProgress] = useState(0);

  if (!newBuildAvailable || !newBuildFile) return null;

  const fileName = newBuildFile.split(/[/\\]/).pop() || newBuildFile;

  const onInstall = async () => {
    setInstalling(true);
    setProgress(0);
    try {
      await downloadAndInstallApk(newBuildFile, p => {
        if (p.progress >= 0) setProgress(p.progress);
      });
      dismissNewBuild();
    } catch (e: any) {
      if (e?.code === 'INSTALL_PERMISSION_REQUIRED' || /INSTALL_PERMISSION_REQUIRED/.test(e?.message || '')) {
        openInstallPermissionSettings();
      }
    } finally {
      setInstalling(false);
    }
  };

  return (
    <View style={styles.wrap}>
      <View style={styles.iconCircle}>
        <Text style={styles.icon}>🎉</Text>
      </View>
      <View style={styles.body}>
        <Text style={styles.title}>New build available</Text>
        <Text style={styles.subtitle} numberOfLines={1}>
          {fileName} is signed and ready to install.
        </Text>
        {installing ? (
          <View style={{ marginTop: 6 }}>
            <ProgressBar value={progress} />
          </View>
        ) : null}
      </View>
      <Pressable style={styles.installBtn} onPress={onInstall} disabled={installing}>
        <Text style={styles.installBtnText}>{installing ? '…' : 'Install'}</Text>
      </Pressable>
      <Pressable style={styles.dismissBtn} onPress={dismissNewBuild} hitSlop={8}>
        <Text style={styles.dismissText}>✕</Text>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#0f241a',
    borderColor: 'rgba(34, 197, 94, 0.4)',
    borderWidth: 1,
    borderRadius: radius.md,
    marginHorizontal: spacing.md,
    marginTop: spacing.sm,
    padding: spacing.sm,
  },
  iconCircle: {
    width: 32,
    height: 32,
    borderRadius: 16,
    backgroundColor: 'rgba(34, 197, 94, 0.2)',
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: spacing.sm,
  },
  icon: { fontSize: 16 },
  body: { flex: 1 },
  title: { color: '#ffffff', fontWeight: '700', fontSize: 13 },
  subtitle: { color: '#86efac', fontSize: 11, marginTop: 1 },
  installBtn: {
    backgroundColor: colors.success,
    borderRadius: radius.sm,
    paddingHorizontal: 12,
    paddingVertical: 8,
    marginLeft: spacing.sm,
  },
  installBtnText: { color: '#04140c', fontWeight: '700', fontSize: 12 },
  dismissBtn: { paddingHorizontal: 8, paddingVertical: 8, marginLeft: 4 },
  dismissText: { color: colors.textMuted, fontSize: 14 },
});

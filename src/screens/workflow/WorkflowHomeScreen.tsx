import React, { useCallback, useEffect, useState } from 'react';
import { View, Text, StyleSheet, ScrollView, Pressable } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { Card, EmptyState, Button, Banner, Input } from '../../components/UI';
import { useProject } from '../../context/ProjectContext';
import { AiResponseModal } from '../../components/AiResponseModal';
import * as aiApi from '../../api/ai';
import { colors, radius, spacing } from '../../theme/theme';

const TOOLS: { key: string; title: string; subtitle: string; icon: string }[] = [
  { key: 'FileBrowser', title: 'Files', subtitle: 'Browse decompiled project files', icon: '📁' },
  { key: 'StringsEditor', title: 'App Name & Strings', subtitle: 'Edit strings.xml / app name', icon: '🔤' },
  { key: 'FindReplace', title: 'Find & Replace', subtitle: 'Search across the whole project', icon: '🔍' },
  { key: 'HexSearch', title: 'Hex / Binary Search', subtitle: 'Search inside .so and binary files', icon: '🧬' },
  { key: 'FirebaseConfig', title: 'Firebase Config', subtitle: 'Apply google-services.json values', icon: '🔥' },
  { key: 'LogoIcon', title: 'Logo & Icon', subtitle: 'Replace launcher icon, or AI-generate one', icon: '🎨' },
  { key: 'AiTools', title: 'AI Tools', subtitle: 'AI review, error fix, icon generation', icon: '✨' },
  { key: 'KeystoreScreen', title: 'Keystore', subtitle: 'Create or select a signing keystore', icon: '🔑' },
  { key: 'BuildSign', title: 'Build & Sign', subtitle: 'Recompile and sign the APK', icon: '🛠️' },
  { key: 'Adb', title: 'ADB Devices', subtitle: 'Connect devices & install the APK', icon: '📲' },
  { key: 'Logcat', title: 'Logcat', subtitle: 'Read device logs over ADB', icon: '🧾' },
  { key: 'CloudLogging', title: 'Cloud Debug Logging', subtitle: 'Remote crash/log capture, no ADB needed', icon: '☁️' },
];

export default function WorkflowHomeScreen({ route, navigation }: any) {
  const {
    state,
    refreshState,
    hasProject,
    recentAction,
    setRecentAction,
    showActionModal,
    setShowActionModal,
  } = useProject();

  const [successMsg, setSuccessMsg] = useState(route?.params?.message || '');

  useEffect(() => {
    if (route?.params?.message) {
      setSuccessMsg(route.params.message);
    }
  }, [route?.params?.message]);

  useFocusEffect(
    useCallback(() => {
      refreshState();
    }, [refreshState]),
  );

  if (!hasProject) {
    return (
      <View style={styles.flex}>
        <EmptyState
          title="No project open"
          subtitle="Open a project from the Projects tab, or start a new one."
        />
        <View style={{ paddingHorizontal: spacing.md }}>
          <Button title="Go to Projects" onPress={() => navigation.getParent()?.navigate('ProjectsTab')} />
        </View>
      </View>
    );
  }

  return (
    <ScrollView style={styles.flex} contentContainerStyle={styles.content}>
      {successMsg ? <Banner type="success" message={successMsg} /> : null}
      <Card>
        <Text style={styles.projectName}>{state?.project_name}</Text>
        <Text style={styles.projectMeta}>Project ID: {state?.project_id}</Text>
        {state?.unsigned_apk ? <Text style={styles.badgeGood}>✓ Unsigned APK built</Text> : null}
        {state?.signed_apk ? <Text style={styles.badgeGood}>✓ Signed APK ready</Text> : null}
      </Card>

      {/* Button to view required steps for recent actions */}
      {recentAction ? (
        <Card style={styles.recentActionCard}>
          <View style={styles.recentActionHeader}>
            <Text style={styles.recentActionTitle}>💡 AI Recent Action & Guidance</Text>
            <Text style={styles.recentActionBadge}>READY</Text>
          </View>
          {recentAction.prompt ? (
            <Text style={styles.recentActionPrompt} numberOfLines={2}>
              "{recentAction.prompt}"
            </Text>
          ) : null}
          <Button
            title="📋 Show Required Steps for Recent Action"
            onPress={() => setShowActionModal(true)}
            variant="secondary"
            style={styles.showStepsBtn}
            textStyle={{ color: '#38BDF8', fontWeight: '800' }}
          />
        </Card>
      ) : null}



      <View style={styles.grid}>
        {TOOLS.map(tool => (
          <Pressable key={tool.key} style={styles.tile} onPress={() => navigation.navigate(tool.key)}>
            <Text style={styles.tileIcon}>{tool.icon}</Text>
            <Text style={styles.tileTitle}>{tool.title}</Text>
            <Text style={styles.tileSubtitle}>{tool.subtitle}</Text>
          </Pressable>
        ))}
      </View>

      {/* Modal Popup displaying required steps */}
      <AiResponseModal
        visible={showActionModal}
        onClose={() => setShowActionModal(false)}
        userPrompt={recentAction?.prompt}
        message={recentAction?.message || ''}
        navigation={navigation}
      />
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1, backgroundColor: colors.bg },
  content: { padding: spacing.md, paddingBottom: spacing.xl },
  projectName: { color: colors.text, fontSize: 18, fontWeight: '800' },
  projectMeta: { color: colors.textMuted, fontSize: 12, marginTop: 4 },
  badgeGood: { color: colors.success, marginTop: 6, fontWeight: '600', fontSize: 12 },
  recentActionCard: {
    backgroundColor: '#0F172A',
    borderColor: '#38BDF8',
    borderWidth: 1.5,
    borderRadius: radius.md,
  },
  recentActionHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 6,
  },
  recentActionTitle: {
    color: '#F8FAFC',
    fontSize: 14,
    fontWeight: '800',
  },
  recentActionBadge: {
    backgroundColor: '#0284C7',
    color: '#FFFFFF',
    fontSize: 10,
    fontWeight: '800',
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: 4,
  },
  recentActionPrompt: {
    color: '#94A3B8',
    fontSize: 12,
    fontStyle: 'italic',
    marginBottom: spacing.xs,
  },
  showStepsBtn: {
    marginTop: spacing.xs,
    backgroundColor: '#1E293B',
    borderColor: '#38BDF8',
    borderWidth: 1,
  },
  promptCard: { backgroundColor: '#0F172A', borderColor: '#334155', borderWidth: 1 },
  promptTitle: { color: '#F8FAFC', fontSize: 16, fontWeight: '700', marginBottom: 4 },
  promptSubtitle: { color: '#94A3B8', fontSize: 12, marginBottom: spacing.sm, lineHeight: 16 },
  promptInput: { minHeight: 80, color: '#F8FAFC', backgroundColor: '#1E293B', borderColor: '#334155' },
  submitBtn: { marginTop: spacing.sm, backgroundColor: colors.primary },
  grid: { flexDirection: 'row', flexWrap: 'wrap', justifyContent: 'space-between' },
  tile: {
    width: '48%',
    backgroundColor: colors.surface,
    borderRadius: radius.md,
    borderWidth: 1,
    borderColor: colors.border,
    padding: spacing.md,
    marginBottom: spacing.sm,
  },
  tileIcon: { fontSize: 26, marginBottom: 6 },
  tileTitle: { color: colors.text, fontWeight: '700', fontSize: 14 },
  tileSubtitle: { color: colors.textMuted, fontSize: 11, marginTop: 4 },
});

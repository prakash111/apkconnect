import React, { useCallback, useState } from 'react';
import { View, Text, FlatList, StyleSheet, Pressable, Alert, Modal, Image } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { Button, EmptyState, LoadingOverlay, Banner, Input, Label, SectionTitle } from '../../components/UI';
import * as projectsApi from '../../api/projects';
import { getBaseUrl } from '../../api/client';
import { useProject } from '../../context/ProjectContext';
import { ProjectSummary } from '../../types';
import { colors, radius, spacing } from '../../theme/theme';

export default function ProjectListScreen({ navigation }: any) {
  const [projects, setProjects] = useState<ProjectSummary[]>([]);
  const [baseUrl, setBaseUrl] = useState<string>('');
  const [loading, setLoading] = useState(false);
  const [busyId, setBusyId] = useState<string | null>(null);
  const [error, setError] = useState('');
  const [renameTarget, setRenameTarget] = useState<ProjectSummary | null>(null);
  const [renameValue, setRenameValue] = useState('');
  const { refreshState } = useProject();

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const url = await getBaseUrl();
      setBaseUrl(url);
      const res = await projectsApi.getProjects();
      if (res.status === 'success') setProjects(res.projects || []);
      else setError(res.message || 'Could not load projects.');
    } catch (e: any) {
      setError(e?.message || 'Could not load projects.');
    } finally {
      setLoading(false);
    }
  }, []);

  useFocusEffect(
    useCallback(() => {
      load();
    }, [load]),
  );

  const openProject = async (p: ProjectSummary) => {
    setBusyId(p.project_id);
    try {
      const res = await projectsApi.switchProject(p.project_id);
      if (res.status === 'success') {
        await refreshState();
        navigation.navigate('WorkflowTab', { screen: 'WorkflowHome' });
      } else {
        Alert.alert('Error', res.message || 'Could not open project.');
      }
    } catch (e: any) {
      Alert.alert('Error', e?.message || 'Could not open project.');
    } finally {
      setBusyId(null);
    }
  };

  const renameProject = (p: ProjectSummary) => {
    setRenameTarget(p);
    setRenameValue(p.project_name);
  };

  const submitRename = async () => {
    if (!renameTarget || !renameValue.trim()) return;
    setBusyId(renameTarget.project_id);
    try {
      const res = await projectsApi.renameProject(renameTarget.project_id, renameValue.trim());
      if (res.status === 'success') {
        setProjects(res.projects || []);
        setRenameTarget(null);
      } else {
        Alert.alert('Error', res.message || 'Rename failed.');
      }
    } catch (e: any) {
      Alert.alert('Error', e?.message || 'Rename failed.');
    } finally {
      setBusyId(null);
    }
  };

  const removeProject = (p: ProjectSummary) => {
    Alert.alert('Delete project', `Delete "${p.project_name}"? This cannot be undone.`, [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Delete',
        style: 'destructive',
        onPress: async () => {
          setBusyId(p.project_id);
          try {
            const res = await projectsApi.deleteProject(p.project_id);
            if (res.status === 'success') setProjects(res.projects || []);
            else Alert.alert('Error', res.message || 'Delete failed.');
          } finally {
            setBusyId(null);
          }
        },
      },
    ]);
  };

  return (
    <View style={styles.flex}>
      <LoadingOverlay visible={loading && projects.length === 0} label="Loading projects…" />
      <View style={styles.header}>
        <Button title="+ New Project" onPress={() => navigation.navigate('NewProject')} />
      </View>
      {error ? (
        <View style={{ paddingHorizontal: spacing.md }}>
          <Banner type="error" message={error} />
        </View>
      ) : null}
      <FlatList
        data={projects}
        keyExtractor={item => item.project_id}
        contentContainerStyle={styles.list}
        refreshing={loading}
        onRefresh={load}
        ListEmptyComponent={
          !loading ? (
            <EmptyState
              title="No projects yet"
              subtitle="Upload an APK to decompile it and start a new project."
            />
          ) : null
        }
        renderItem={({ item }) => {
          const iconUri = item.icon_base64
            ? item.icon_base64
            : item.launcher_icon && baseUrl
            ? `${baseUrl}/index.php?action=get_project_icon&project_id=${item.project_id}`
            : null;
          return (
            <View style={styles.row}>
              <View style={styles.rowTop}>
                <View style={styles.iconCircle}>
                  {iconUri ? (
                    <Image source={{ uri: iconUri }} style={styles.projectIconImage} />
                  ) : (
                    <Text style={styles.projectEmoji}>📦</Text>
                  )}
                </View>
                <Pressable style={styles.rowMain} onPress={() => openProject(item)} disabled={busyId === item.project_id}>
                  <Text style={styles.rowTitle}>{item.project_name}</Text>
                  <Text style={styles.rowSub}>ID: {item.project_id}</Text>
                </Pressable>
              </View>
              <View style={styles.rowActions}>
                <Button
                  title={busyId === item.project_id ? "Opening…" : "Open Project"}
                  small
                  onPress={() => openProject(item)}
                  loading={busyId === item.project_id}
                  style={styles.openBtn}
                />
                <Pressable onPress={() => renameProject(item)} style={styles.secondaryBtn}>
                  <Text style={styles.secondaryBtnText}>Rename</Text>
                </Pressable>
                <Pressable onPress={() => removeProject(item)} style={[styles.secondaryBtn, styles.deleteBtn]}>
                  <Text style={styles.deleteBtnText}>Delete</Text>
                </Pressable>
              </View>
            </View>
          );
        }}
      />

      <Modal visible={!!renameTarget} transparent animationType="slide" onRequestClose={() => setRenameTarget(null)}>
        <View style={styles.modalBackdrop}>
          <View style={styles.modalCard}>
            <SectionTitle>Rename project</SectionTitle>
            <Label>Project name</Label>
            <Input value={renameValue} onChangeText={setRenameValue} autoFocus />
            <Button title="Save" onPress={submitRename} loading={busyId === renameTarget?.project_id} />
            <Button title="Cancel" variant="ghost" onPress={() => setRenameTarget(null)} />
          </View>
        </View>
      </Modal>
    </View>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1, backgroundColor: colors.bg },
  header: { padding: spacing.md },
  list: { paddingHorizontal: spacing.md, paddingBottom: spacing.xl },
  row: {
    backgroundColor: colors.surface,
    borderRadius: radius.md,
    borderWidth: 1,
    borderColor: colors.border,
    padding: spacing.md,
    marginBottom: spacing.sm,
  },
  rowTop: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: spacing.sm,
  },
  iconCircle: {
    width: 44,
    height: 44,
    borderRadius: 10,
    backgroundColor: colors.surfaceAlt,
    borderWidth: 1,
    borderColor: colors.border,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: spacing.sm,
    overflow: 'hidden',
  },
  projectIconImage: { width: 44, height: 44, borderRadius: 10, resizeMode: 'cover' },
  projectEmoji: { fontSize: 20 },
  rowMain: { flex: 1 },
  rowTitle: { color: colors.text, fontWeight: '700', fontSize: 16 },
  rowSub: { color: colors.textMuted, fontSize: 12, marginTop: 2 },
  rowActions: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
    marginTop: spacing.xs,
  },
  openBtn: { flex: 2, marginTop: 0 },
  secondaryBtn: {
    paddingVertical: 8,
    paddingHorizontal: 12,
    borderRadius: radius.sm,
    backgroundColor: colors.surfaceAlt,
    borderWidth: 1,
    borderColor: colors.border,
    alignItems: 'center',
    justifyContent: 'center',
  },
  secondaryBtnText: { color: colors.text, fontWeight: '600', fontSize: 13 },
  deleteBtn: { borderColor: 'rgba(239, 68, 68, 0.3)' },
  deleteBtnText: { color: colors.danger, fontWeight: '600', fontSize: 13 },
  modalBackdrop: { flex: 1, backgroundColor: 'rgba(0,0,0,0.6)', justifyContent: 'flex-end' },
  modalCard: {
    backgroundColor: colors.surface,
    borderTopLeftRadius: 20,
    borderTopRightRadius: 20,
    padding: spacing.md,
  },
});

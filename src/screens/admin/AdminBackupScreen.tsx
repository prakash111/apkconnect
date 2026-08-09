import React, { useCallback, useState } from 'react';
import { View, Text, StyleSheet, ScrollView, Pressable } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { Card, Input, Label, Button, Banner, HelperText, SectionTitle, LoadingOverlay } from '../../components/UI';
import * as adminApi from '../../api/admin';
import { BackupSettings } from '../../api/admin';
import { colors, radius, spacing } from '../../theme/theme';

const FREQUENCIES = ['daily', 'weekly', 'monthly'];

export default function AdminBackupScreen() {
  const [settings, setSettings] = useState<Partial<BackupSettings>>({});
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [runningBackup, setRunningBackup] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const res = await adminApi.getAdminBackupSettings();
      if (res.status === 'success') setSettings(res.settings || {});
      else setError(res.message || 'Could not load settings.');
    } catch (e: any) {
      setError(e?.message || 'Could not load settings.');
    } finally {
      setLoading(false);
    }
  }, []);

  useFocusEffect(
    useCallback(() => {
      load();
    }, [load]),
  );

  const update = (field: keyof BackupSettings, value: string) => {
    setSettings(prev => ({ ...prev, [field]: value }));
  };

  const onSave = async () => {
    setSaving(true);
    setError('');
    setMessage('');
    try {
      const res = await adminApi.saveAdminBackupSettings(settings);
      if (res.status === 'success') setMessage('Settings saved.');
      else setError(res.message || 'Save failed.');
    } catch (e: any) {
      setError(e?.message || 'Save failed.');
    } finally {
      setSaving(false);
    }
  };

  const onRunBackup = async () => {
    setRunningBackup(true);
    setError('');
    setMessage('');
    try {
      const res = await adminApi.runAdminManualBackup();
      if (res.status === 'success') {
        setMessage(res.message || 'Backup complete.');
        await load();
      } else {
        setError(res.message || 'Backup failed.');
      }
    } catch (e: any) {
      setError(e?.message || 'Backup failed.');
    } finally {
      setRunningBackup(false);
    }
  };

  return (
    <ScrollView style={styles.flex} contentContainerStyle={styles.content}>
      <LoadingOverlay visible={loading} label="Loading…" />
      <Banner type="error" message={error} />
      <Banner type="success" message={message} />

      <Card>
        <SectionTitle>GitHub backup destination</SectionTitle>
        <HelperText>
          Full database + file backups of this install are pushed to a GitHub repo you control.
        </HelperText>
        <Label>Repo owner</Label>
        <Input
          value={settings.github_backup_repo_owner || ''}
          onChangeText={v => update('github_backup_repo_owner', v)}
          autoCapitalize="none"
        />
        <Label>Repo name</Label>
        <Input
          value={settings.github_backup_repo_name || ''}
          onChangeText={v => update('github_backup_repo_name', v)}
          autoCapitalize="none"
        />
        <Label>Branch</Label>
        <Input
          value={settings.github_backup_branch || ''}
          onChangeText={v => update('github_backup_branch', v)}
          autoCapitalize="none"
        />
        <Label>Upload directory</Label>
        <Input
          value={settings.github_backup_upload_dir || ''}
          onChangeText={v => update('github_backup_upload_dir', v)}
          autoCapitalize="none"
        />
        <Label>Personal access token</Label>
        <Input
          value={settings.github_backup_token || ''}
          onChangeText={v => update('github_backup_token', v)}
          autoCapitalize="none"
          secureTextEntry
        />
        <HelperText>
          This token is stored server-side. If your server's default token has ever been exposed
          (e.g. checked into source control), rotate it on GitHub and paste the new one here.
        </HelperText>
      </Card>

      <Card>
        <SectionTitle>Schedule</SectionTitle>
        <Pressable
          style={styles.toggleRow}
          onPress={() => update('auto_backup_enabled', settings.auto_backup_enabled === '1' ? '0' : '1')}>
          <Text style={styles.toggleLabel}>Automatic backups</Text>
          <View style={[styles.toggle, settings.auto_backup_enabled === '1' && styles.toggleOn]}>
            <Text style={styles.toggleText}>{settings.auto_backup_enabled === '1' ? 'ON' : 'OFF'}</Text>
          </View>
        </Pressable>
        <Label>Frequency</Label>
        <View style={styles.freqRow}>
          {FREQUENCIES.map(f => (
            <Pressable
              key={f}
              onPress={() => update('auto_backup_frequency', f)}
              style={[styles.freqChip, settings.auto_backup_frequency === f && styles.freqChipActive]}>
              <Text
                style={[
                  styles.freqText,
                  settings.auto_backup_frequency === f && styles.freqTextActive,
                ]}>
                {f}
              </Text>
            </Pressable>
          ))}
        </View>
        <Button title="Save settings" onPress={onSave} loading={saving} />
      </Card>

      <Card>
        <SectionTitle>Manual backup</SectionTitle>
        {settings.last_github_backup ? (
          <Text style={styles.lastBackup}>Last backup: {settings.last_github_backup}</Text>
        ) : (
          <Text style={styles.lastBackup}>No backup run yet.</Text>
        )}
        <Button title="Run backup now" onPress={onRunBackup} loading={runningBackup} />
      </Card>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1, backgroundColor: colors.bg },
  content: { padding: spacing.md, paddingBottom: spacing.xl },
  toggleRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: spacing.sm,
  },
  toggleLabel: { color: colors.text, fontWeight: '600' },
  toggle: { paddingHorizontal: 12, paddingVertical: 6, borderRadius: 20, backgroundColor: colors.surfaceAlt },
  toggleOn: { backgroundColor: colors.success },
  toggleText: { color: '#fff', fontWeight: '700', fontSize: 11 },
  freqRow: { flexDirection: 'row', gap: spacing.sm, marginBottom: spacing.sm },
  freqChip: {
    paddingHorizontal: 14,
    paddingVertical: 8,
    borderRadius: radius.sm,
    backgroundColor: colors.surfaceAlt,
    borderWidth: 1,
    borderColor: colors.border,
  },
  freqChipActive: { backgroundColor: colors.primary, borderColor: colors.primary },
  freqText: { color: colors.textMuted, fontWeight: '600', fontSize: 12 },
  freqTextActive: { color: '#fff' },
  lastBackup: { color: colors.textMuted, fontSize: 12, marginBottom: spacing.sm },
});

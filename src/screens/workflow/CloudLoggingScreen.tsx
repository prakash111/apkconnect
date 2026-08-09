import React, { useCallback, useState } from 'react';
import { View, Text, ScrollView, StyleSheet } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { Card, Button, Banner, HelperText, SectionTitle } from '../../components/UI';
import * as workflowApi from '../../api/workflow';
import { useProject } from '../../context/ProjectContext';
import { colors, spacing } from '../../theme/theme';

export default function CloudLoggingScreen() {
  const { state, refreshState } = useProject();
  const [lines, setLines] = useState<string[]>([]);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [enabling, setEnabling] = useState(false);
  const [loading, setLoading] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      await refreshState();
      const res = await workflowApi.getCloudLogs();
      if (res.status === 'success') setLines(res.lines || []);
    } catch {
      // non-fatal
    } finally {
      setLoading(false);
    }
  }, [refreshState]);

  useFocusEffect(
    useCallback(() => {
      load();
    }, [load]),
  );

  const onEnable = async () => {
    setEnabling(true);
    setError('');
    setMessage('');
    try {
      const res = await workflowApi.enableCloudLogging();
      if (res.status === 'success') {
        setMessage(res.message || 'Enabled. Rebuild the APK for this to take effect.');
        await refreshState();
      } else {
        setError(res.message || 'Enable failed.');
      }
    } catch (e: any) {
      setError(e?.message || 'Enable failed.');
    } finally {
      setEnabling(false);
    }
  };

  const onClear = async () => {
    try {
      await workflowApi.clearCloudLogs();
      setLines([]);
    } catch (e: any) {
      setError(e?.message || 'Clear failed.');
    }
  };

  const isEnabled = !!state?.cloud_logging_enabled;

  return (
    <View style={styles.flex}>
      <Card>
        <SectionTitle>Cloud Debug Logging</SectionTitle>
        <Banner type="error" message={error} />
        <Banner type="success" message={message} />

        <View style={[styles.statusBadge, isEnabled ? styles.statusBadgeEnabled : styles.statusBadgeDisabled]}>
          <Text style={styles.statusBadgeDot}>{isEnabled ? '🟢' : '⚪'}</Text>
          <Text style={[styles.statusBadgeText, isEnabled ? styles.statusTextEnabled : styles.statusTextDisabled]}>
            {isEnabled ? 'Cloud Debug Logging is ENABLED' : 'Cloud Debug Logging is NOT ENABLED'}
          </Text>
        </View>

        <HelperText>
          Injects a lightweight logging shim into the project that reports crashes/logs back to
          your server — useful for devices you can't reach over ADB. Rebuild after enabling.
        </HelperText>
        <Button
          title={isEnabled ? 'Re-enable / regenerate token' : 'Enable cloud logging'}
          onPress={onEnable}
          loading={enabling}
        />
      </Card>

      <Card>
        <View style={styles.logHeader}>
          <SectionTitle>Device logs</SectionTitle>
          <Button title="Clear" small variant="ghost" onPress={onClear} />
        </View>
        <ScrollView style={styles.logBox} refreshControl={undefined}>
          {lines.length === 0 ? (
            <Text style={styles.emptyLog}>{loading ? 'Loading…' : 'No logs received yet.'}</Text>
          ) : (
            lines.map((line, idx) => (
              <Text key={idx} style={styles.logLine}>
                {line}
              </Text>
            ))
          )}
        </ScrollView>
        <Button title="Refresh" small variant="secondary" onPress={load} />
      </Card>
    </View>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1, backgroundColor: colors.bg, padding: spacing.md },
  statusBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: 8,
    marginVertical: spacing.xs,
    borderWidth: 1,
  },
  statusBadgeEnabled: {
    backgroundColor: 'rgba(34, 197, 94, 0.15)',
    borderColor: colors.success,
  },
  statusBadgeDisabled: {
    backgroundColor: 'rgba(148, 163, 184, 0.1)',
    borderColor: colors.border,
  },
  statusBadgeDot: { fontSize: 12, marginRight: 8 },
  statusBadgeText: { fontWeight: '700', fontSize: 13 },
  statusTextEnabled: { color: colors.success },
  statusTextDisabled: { color: colors.textMuted },
  logHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  logBox: { maxHeight: 260, backgroundColor: colors.surfaceAlt, borderRadius: 8, padding: spacing.sm },
  logLine: { color: colors.text, fontFamily: 'monospace', fontSize: 11, marginBottom: 2 },
  emptyLog: { color: colors.textMuted, fontSize: 12 },
});

import React, { useCallback, useState } from 'react';
import { View, Text, ScrollView, StyleSheet, Pressable } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { Button, Banner, HelperText } from '../../components/UI';
import * as adbApi from '../../api/adb';
import { colors, spacing } from '../../theme/theme';

const FILTERS: Array<'all' | 'error' | 'network'> = ['all', 'error', 'network'];

export default function LogcatScreen({ route }: any) {
  const serial: string = route.params?.serial ?? '';
  const [filter, setFilter] = useState<'all' | 'error' | 'network'>('all');
  const [lines, setLines] = useState<string[]>([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const load = useCallback(async () => {
    if (!serial) return;
    setLoading(true);
    setError('');
    try {
      const res = await adbApi.readLogcat(serial, filter);
      if (res.status === 'success') setLines(res.lines || []);
      else setError(res.message || 'Could not read logcat.');
    } catch (e: any) {
      setError(e?.message || 'Could not read logcat.');
    } finally {
      setLoading(false);
    }
  }, [serial, filter]);

  useFocusEffect(
    useCallback(() => {
      load();
    }, [load]),
  );

  const onClear = async () => {
    try {
      await adbApi.clearLogcat(serial);
      setLines([]);
    } catch (e: any) {
      setError(e?.message || 'Clear failed.');
    }
  };

  if (!serial) {
    return (
      <View style={styles.flex}>
        <HelperText>Open Logcat from a device in the ADB Devices screen.</HelperText>
      </View>
    );
  }

  return (
    <View style={styles.flex}>
      <Banner type="error" message={error} />
      <View style={styles.filterRow}>
        {FILTERS.map(f => (
          <Pressable
            key={f}
            onPress={() => setFilter(f)}
            style={[styles.filterChip, filter === f && styles.filterChipActive]}>
            <Text style={[styles.filterText, filter === f && styles.filterTextActive]}>{f}</Text>
          </Pressable>
        ))}
      </View>
      <View style={styles.actionsRow}>
        <Button title="Refresh" small onPress={load} loading={loading} />
        <Button title="Clear device log" small variant="ghost" onPress={onClear} />
      </View>
      <ScrollView style={styles.logBox}>
        {lines.map((line, idx) => (
          <Text key={idx} style={styles.logLine}>
            {line}
          </Text>
        ))}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1, backgroundColor: colors.bg, padding: spacing.md },
  filterRow: { flexDirection: 'row', gap: spacing.sm, marginBottom: spacing.sm },
  filterChip: {
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 20,
    backgroundColor: colors.surfaceAlt,
    borderWidth: 1,
    borderColor: colors.border,
  },
  filterChipActive: { backgroundColor: colors.primary, borderColor: colors.primary },
  filterText: { color: colors.textMuted, fontSize: 12, fontWeight: '600' },
  filterTextActive: { color: '#fff' },
  actionsRow: { flexDirection: 'row', gap: spacing.sm, marginBottom: spacing.sm },
  logBox: { flex: 1, backgroundColor: colors.surfaceAlt, borderRadius: 8, padding: spacing.sm },
  logLine: { color: colors.text, fontFamily: 'monospace', fontSize: 11, marginBottom: 2 },
});

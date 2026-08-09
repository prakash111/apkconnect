import React, { useCallback, useState } from 'react';
import { Text, View, StyleSheet, RefreshControl, ScrollView } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { Card, SectionTitle, ProgressBar, Button } from '../components/UI';
import { useAuth } from '../context/AuthContext';
import { colors, spacing } from '../theme/theme';

function LimitRow({ label, usage, limit }: { label: string; usage?: number; limit?: number }) {
  const u = usage ?? 0;
  const l = limit ?? 0;
  const pct = l > 0 ? u / l : 0;
  return (
    <View style={styles.limitRow}>
      <View style={styles.limitHeader}>
        <Text style={styles.limitLabel}>{label}</Text>
        <Text style={styles.limitValue}>
          {u} / {l > 0 ? l : '∞'}
        </Text>
      </View>
      <ProgressBar value={pct} />
    </View>
  );
}

export default function DashboardScreen({ navigation }: any) {
  const { user, limits, refreshLimits, logout } = useAuth();
  const [refreshing, setRefreshing] = useState(false);

  useFocusEffect(
    useCallback(() => {
      refreshLimits();
    }, [refreshLimits]),
  );

  const onRefresh = async () => {
    setRefreshing(true);
    await refreshLimits();
    setRefreshing(false);
  };

  return (
    <ScrollView
      style={styles.flex}
      contentContainerStyle={styles.content}
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={colors.primary} />}>
      <Text style={styles.greeting}>Hi, {user?.username} 👋</Text>
      <Text style={styles.sub}>Here's your account usage this cycle</Text>

      <Card>
        <SectionTitle>Usage limits</SectionTitle>
        <LimitRow label="Decompiles" usage={limits?.decompile_usage} limit={limits?.decompile_limit} />
        <LimitRow label="Builds (compile)" usage={limits?.compile_usage} limit={limits?.compile_limit} />
        <LimitRow label="Keystores generated" usage={limits?.generate_key_usage} limit={limits?.generate_key_limit} />
        <LimitRow label="APK signs" usage={limits?.sign_apk_usage} limit={limits?.sign_apk_limit} />
      </Card>

      <Card>
        <SectionTitle>Quick actions</SectionTitle>
        <Button title="Start a new project" onPress={() => navigation.navigate('ProjectsTab', { screen: 'NewProject' })} />
        <Button
          title="Open my projects"
          variant="secondary"
          onPress={() => navigation.navigate('ProjectsTab', { screen: 'ProjectList' })}
        />
      </Card>

      <Button title="Log out" variant="ghost" onPress={logout} />
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1, backgroundColor: colors.bg },
  content: { padding: spacing.md, paddingBottom: spacing.xl },
  greeting: { color: colors.text, fontSize: 22, fontWeight: '800' },
  sub: { color: colors.textMuted, marginTop: 4, marginBottom: spacing.md },
  limitRow: { marginBottom: spacing.md },
  limitHeader: { flexDirection: 'row', justifyContent: 'space-between', marginBottom: 6 },
  limitLabel: { color: colors.text, fontWeight: '600' },
  limitValue: { color: colors.textMuted },
});

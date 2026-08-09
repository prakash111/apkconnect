import React, { useCallback, useState } from 'react';
import { View, Text, StyleSheet, FlatList, Pressable } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { Card, Input, Label, Button, Banner, HelperText, SectionTitle } from '../../components/UI';
import * as keystoreApi from '../../api/keystore';
import { useProject } from '../../context/ProjectContext';
import { Keystore } from '../../types';
import { colors, radius, spacing } from '../../theme/theme';

function getFileName(path?: string): string {
  if (!path) return '';
  return path.split(/[/\\]/).pop() || path;
}

export default function KeystoreScreen() {
  const { state, refreshState } = useProject();
  const [alias, setAlias] = useState('');
  const [password, setPassword] = useState('');
  const [keystores, setKeystores] = useState<Keystore[]>([]);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [creating, setCreating] = useState(false);
  const [selectingId, setSelectingId] = useState<number | null>(null);

  const load = useCallback(async () => {
    try {
      const res = await keystoreApi.getKeystores();
      if (res.status === 'success') setKeystores(res.keystores || []);
    } catch {
      // non-fatal
    }
  }, []);

  useFocusEffect(
    useCallback(() => {
      load();
    }, [load]),
  );

  const onCreate = async () => {
    if (!alias.trim() || !password.trim()) {
      setError('Alias and password are required.');
      return;
    }
    setCreating(true);
    setError('');
    setMessage('');
    try {
      const res = await keystoreApi.createKeystore(alias.trim(), password.trim());
      if (res.status === 'success') {
        setMessage(res.message || 'Keystore generated successfully.');
        setAlias('');
        setPassword('');
        await refreshState();
        await load();
      } else {
        setError(res.message || 'Keystore generation failed.');
      }
    } catch (e: any) {
      setError(e?.message || 'Keystore generation failed.');
    } finally {
      setCreating(false);
    }
  };

  const onSelect = async (ks: Keystore) => {
    setSelectingId(ks.id);
    setError('');
    setMessage('');
    try {
      const res = await keystoreApi.selectKeystore(ks.id);
      if (res.status === 'success') {
        setMessage(res.message || `Keystore "${ks.key_alias}" applied.`);
        await refreshState();
      } else {
        setError(res.message || 'Failed to select keystore.');
      }
    } catch (e: any) {
      setError(e?.message || 'Failed to select keystore.');
    } finally {
      setSelectingId(null);
    }
  };

  return (
    <View style={styles.flex}>
      <FlatList
        data={keystores}
        keyExtractor={item => String(item.id)}
        contentContainerStyle={styles.list}
        keyboardShouldPersistTaps="handled"
        ListHeaderComponent={
          <View>
            <Banner type="error" message={error} />
            <Banner type="success" message={message} />

            {/* Active Keystore */}
            <Card>
              <SectionTitle>Active Keystore</SectionTitle>
              <View style={styles.activeRow}>
                <Text style={styles.activeEmoji}>🔑</Text>
                <View style={{ flex: 1 }}>
                  <Text style={styles.activeAlias}>
                    {state?.keystore_alias || 'None selected yet'}
                  </Text>
                  {state?.keystore_path ? (
                    <Text style={styles.activeFile}>
                      {getFileName(state.keystore_path)}
                    </Text>
                  ) : (
                    <Text style={styles.activeSub}>
                      Select or generate a keystore below to sign APKs.
                    </Text>
                  )}
                </View>
              </View>
            </Card>

            {/* Create new keystore */}
            <Card>
              <SectionTitle>Create New Keystore</SectionTitle>
              <Label>Key Alias</Label>
              <Input
                value={alias}
                onChangeText={setAlias}
                placeholder="e.g. my_release_key"
                autoCapitalize="none"
                autoCorrect={false}
              />
              <Label>Keystore Password</Label>
              <Input
                value={password}
                onChangeText={setPassword}
                secureTextEntry
                placeholder="Enter strong password"
              />
              <HelperText>
                Remember this password — it is required when signing your recompiled APK.
              </HelperText>
              <Button
                title="Generate Keystore (RSA 2048)"
                onPress={onCreate}
                loading={creating}
                style={{ marginTop: spacing.sm }}
              />
            </Card>

            {keystores.length > 0 && (
              <View style={styles.existingHeader}>
                <SectionTitle>Available Keystores ({keystores.length})</SectionTitle>
              </View>
            )}
          </View>
        }
        renderItem={({ item }) => {
          const isActive = state?.keystore_alias === item.key_alias;
          const fileName = getFileName(item.file_name);
          return (
            <Pressable
              style={[styles.ksRow, isActive && styles.ksRowActive]}
              onPress={() => onSelect(item)}
              disabled={selectingId === item.id}>
              <View style={styles.ksMain}>
                <Text style={styles.ksAlias}>{item.key_alias}</Text>
                <Text style={styles.ksFile} numberOfLines={1}>
                  📁 {fileName}
                </Text>
              </View>
              {isActive ? (
                <View style={styles.activeBadge}>
                  <Text style={styles.activeBadgeText}>ACTIVE</Text>
                </View>
              ) : (
                <Button
                  title={selectingId === item.id ? "…" : "Use"}
                  small
                  variant="secondary"
                  onPress={() => onSelect(item)}
                  loading={selectingId === item.id}
                  style={styles.useBtn}
                />
              )}
            </Pressable>
          );
        }}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1, backgroundColor: colors.bg },
  list: { padding: spacing.md, paddingBottom: spacing.xl },
  activeRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: spacing.xs,
  },
  activeEmoji: { fontSize: 28, marginRight: spacing.sm },
  activeAlias: { color: colors.text, fontWeight: '800', fontSize: 16 },
  activeFile: { color: colors.success, fontSize: 12, fontFamily: 'monospace', marginTop: 2 },
  activeSub: { color: colors.textMuted, fontSize: 12, marginTop: 2 },
  existingHeader: { marginTop: spacing.sm, marginBottom: spacing.xs },
  ksRow: {
    backgroundColor: colors.surface,
    borderRadius: radius.sm,
    borderWidth: 1,
    borderColor: colors.border,
    padding: spacing.sm,
    marginBottom: spacing.sm,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  ksRowActive: {
    borderColor: colors.primary,
    backgroundColor: '#12182d',
  },
  ksMain: { flex: 1, marginRight: spacing.sm },
  ksAlias: { color: colors.text, fontWeight: '700', fontSize: 14 },
  ksFile: { color: colors.textMuted, fontSize: 11, fontFamily: 'monospace', marginTop: 3 },
  activeBadge: {
    backgroundColor: 'rgba(99, 102, 241, 0.2)',
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 4,
    borderWidth: 1,
    borderColor: colors.primary,
  },
  activeBadgeText: { color: colors.primary, fontWeight: '800', fontSize: 11 },
  useBtn: { minWidth: 60, marginTop: 0 },
});

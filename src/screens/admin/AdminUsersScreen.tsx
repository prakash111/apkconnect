import React, { useCallback, useState } from 'react';
import { View, Text, FlatList, StyleSheet, Pressable, Modal } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { Card, Input, Label, Button, Banner, HelperText, SectionTitle, EmptyState, LoadingOverlay } from '../../components/UI';
import * as adminApi from '../../api/admin';
import { colors, radius, spacing } from '../../theme/theme';

interface AdminUser {
  id: number;
  username: string;
  email: string;
  user_type: 'user' | 'admin';
  decompile_limit: number;
  compile_limit: number;
  generate_key_limit: number;
  sign_apk_limit: number;
}

export default function AdminUsersScreen() {
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [showCreate, setShowCreate] = useState(false);
  const [newEmail, setNewEmail] = useState('');
  const [newUsername, setNewUsername] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [creating, setCreating] = useState(false);

  const [editing, setEditing] = useState<AdminUser | null>(null);
  const [limits, setLimits] = useState({ decompile: '', compile: '', keys: '', signs: '' });
  const [savingLimits, setSavingLimits] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const res = await adminApi.getUsers();
      if (res.status === 'success') setUsers(res.users || []);
      else setError(res.message || 'Could not load users.');
    } catch (e: any) {
      setError(e?.message || 'Could not load users.');
    } finally {
      setLoading(false);
    }
  }, []);

  useFocusEffect(
    useCallback(() => {
      load();
    }, [load]),
  );

  const onCreate = async () => {
    if (!newEmail || !newUsername || !newPassword) {
      setError('Email, username and password are required.');
      return;
    }
    setCreating(true);
    setError('');
    try {
      const res = await adminApi.createUser({ email: newEmail, username: newUsername, password: newPassword });
      if (res.status === 'success') {
        setShowCreate(false);
        setNewEmail('');
        setNewUsername('');
        setNewPassword('');
        await load();
      } else {
        setError(res.message || 'Create failed.');
      }
    } catch (e: any) {
      setError(e?.message || 'Create failed.');
    } finally {
      setCreating(false);
    }
  };

  const openLimits = (u: AdminUser) => {
    setEditing(u);
    setLimits({
      decompile: String(u.decompile_limit ?? 0),
      compile: String(u.compile_limit ?? 0),
      keys: String(u.generate_key_limit ?? 0),
      signs: String(u.sign_apk_limit ?? 0),
    });
  };

  const onSaveLimits = async () => {
    if (!editing) return;
    setSavingLimits(true);
    setError('');
    try {
      const res = await adminApi.updateLimits(editing.id, {
        decompile_limit: Number(limits.decompile) || 0,
        compile_limit: Number(limits.compile) || 0,
        generate_key_limit: Number(limits.keys) || 0,
        sign_apk_limit: Number(limits.signs) || 0,
      });
      if (res.status === 'success') {
        setEditing(null);
        await load();
      } else {
        setError(res.message || 'Update failed.');
      }
    } catch (e: any) {
      setError(e?.message || 'Update failed.');
    } finally {
      setSavingLimits(false);
    }
  };

  return (
    <View style={styles.flex}>
      <LoadingOverlay visible={loading && users.length === 0} label="Loading users…" />
      <View style={styles.header}>
        <Button title={showCreate ? 'Cancel' : '+ New user'} variant="secondary" onPress={() => setShowCreate(v => !v)} />
      </View>
      {showCreate ? (
        <Card>
          <SectionTitle>Create user</SectionTitle>
          <Banner type="error" message={error} />
          <Label>Email</Label>
          <Input value={newEmail} onChangeText={setNewEmail} autoCapitalize="none" keyboardType="email-address" />
          <Label>Username</Label>
          <Input value={newUsername} onChangeText={setNewUsername} autoCapitalize="none" />
          <Label>Password</Label>
          <Input value={newPassword} onChangeText={setNewPassword} secureTextEntry />
          <Button title="Create" onPress={onCreate} loading={creating} />
        </Card>
      ) : null}

      <FlatList
        data={users}
        keyExtractor={item => String(item.id)}
        contentContainerStyle={styles.list}
        refreshing={loading}
        onRefresh={load}
        ListEmptyComponent={!loading ? <EmptyState title="No users found" /> : null}
        renderItem={({ item }) => (
          <Pressable style={styles.row} onPress={() => openLimits(item)}>
            <Text style={styles.username}>
              {item.username} {item.user_type === 'admin' ? '👑' : ''}
            </Text>
            <Text style={styles.email}>{item.email}</Text>
            <Text style={styles.limitsLine}>
              decompile {item.decompile_limit} · build {item.compile_limit} · keys{' '}
              {item.generate_key_limit} · sign {item.sign_apk_limit}
            </Text>
          </Pressable>
        )}
      />

      <Modal visible={!!editing} transparent animationType="slide" onRequestClose={() => setEditing(null)}>
        <View style={styles.modalBackdrop}>
          <View style={styles.modalCard}>
            <SectionTitle>Limits for {editing?.username}</SectionTitle>
            <Banner type="error" message={error} />
            <Label>Decompile limit</Label>
            <Input
              value={limits.decompile}
              onChangeText={v => setLimits(l => ({ ...l, decompile: v }))}
              keyboardType="numeric"
            />
            <Label>Build (compile) limit</Label>
            <Input
              value={limits.compile}
              onChangeText={v => setLimits(l => ({ ...l, compile: v }))}
              keyboardType="numeric"
            />
            <Label>Keystore generation limit</Label>
            <Input
              value={limits.keys}
              onChangeText={v => setLimits(l => ({ ...l, keys: v }))}
              keyboardType="numeric"
            />
            <Label>Sign limit</Label>
            <Input
              value={limits.signs}
              onChangeText={v => setLimits(l => ({ ...l, signs: v }))}
              keyboardType="numeric"
            />
            <HelperText>Use 0 for unlimited, depending on how your backend interprets it.</HelperText>
            <Button title="Save" onPress={onSaveLimits} loading={savingLimits} />
            <Button title="Cancel" variant="ghost" onPress={() => setEditing(null)} />
          </View>
        </View>
      </Modal>
    </View>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1, backgroundColor: colors.bg },
  header: { padding: spacing.md, paddingBottom: 0 },
  list: { padding: spacing.md, paddingBottom: spacing.xl },
  row: {
    backgroundColor: colors.surface,
    borderRadius: radius.sm,
    borderWidth: 1,
    borderColor: colors.border,
    padding: spacing.sm,
    marginBottom: 8,
  },
  username: { color: colors.text, fontWeight: '700' },
  email: { color: colors.textMuted, fontSize: 12, marginTop: 2 },
  limitsLine: { color: colors.textMuted, fontSize: 11, marginTop: 6 },
  modalBackdrop: { flex: 1, backgroundColor: 'rgba(0,0,0,0.6)', justifyContent: 'flex-end' },
  modalCard: {
    backgroundColor: colors.surface,
    borderTopLeftRadius: 20,
    borderTopRightRadius: 20,
    padding: spacing.md,
  },
});

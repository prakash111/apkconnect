import React, { useCallback, useState } from 'react';
import { View, Text, FlatList, StyleSheet, Pressable, Alert } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import {
  Card,
  Input,
  Label,
  Button,
  Banner,
  SectionTitle,
  EmptyState,
  LoadingOverlay,
} from '../../components/UI';
import * as adminApi from '../../api/admin';
import { FaqInput } from '../../api/admin';
import { colors, radius, spacing } from '../../theme/theme';

const EMPTY: FaqInput = { question: '', answer: '', category: '', is_active: 1 };

export default function AdminFaqsScreen() {
  const [faqs, setFaqs] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [editing, setEditing] = useState<FaqInput | null>(null);
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const res = await adminApi.getAdminFaqs();
      if (res.status === 'success') setFaqs(res.faqs || []);
      else setError(res.message || 'Could not load FAQs.');
    } catch (e: any) {
      setError(e?.message || 'Could not load FAQs.');
    } finally {
      setLoading(false);
    }
  }, []);

  useFocusEffect(
    useCallback(() => {
      load();
    }, [load]),
  );

  const onSave = async () => {
    if (!editing?.question || !editing?.answer) {
      setError('Question and answer are required.');
      return;
    }
    setSaving(true);
    setError('');
    try {
      const res = await adminApi.saveAdminFaq(editing);
      if (res.status === 'success') {
        setEditing(null);
        await load();
      } else {
        setError(res.message || 'Save failed.');
      }
    } catch (e: any) {
      setError(e?.message || 'Save failed.');
    } finally {
      setSaving(false);
    }
  };

  const onDelete = (id: number) => {
    Alert.alert('Delete FAQ', 'This cannot be undone.', [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Delete',
        style: 'destructive',
        onPress: async () => {
          try {
            await adminApi.deleteAdminFaq(id);
            await load();
          } catch (e: any) {
            Alert.alert('Error', e?.message || 'Delete failed.');
          }
        },
      },
    ]);
  };

  if (editing) {
    return (
      <View style={styles.flex}>
        <Card>
          <SectionTitle>{editing.id ? 'Edit FAQ' : 'New FAQ'}</SectionTitle>
          <Banner type="error" message={error} />
          <Label>Question</Label>
          <Input value={editing.question} onChangeText={v => setEditing({ ...editing, question: v })} style={styles.inputLight} />
          <Label>Answer</Label>
          <Input
            value={editing.answer}
            onChangeText={v => setEditing({ ...editing, answer: v })}
            multiline
            style={[styles.inputLight, { minHeight: 140 }]}
          />
          <Label>Category</Label>
          <Input value={editing.category} onChangeText={v => setEditing({ ...editing, category: v })} style={styles.inputLight} />
          <Button title="Save" onPress={onSave} loading={saving} />
          <Button title="Cancel" variant="ghost" onPress={() => setEditing(null)} />
        </Card>
      </View>
    );
  }

  return (
    <View style={styles.flex}>
      <LoadingOverlay visible={loading && faqs.length === 0} label="Loading…" />
      <View style={styles.header}>
        <Button title="+ New FAQ" variant="secondary" onPress={() => setEditing(EMPTY)} />
      </View>
      <FlatList
        data={faqs}
        keyExtractor={item => String(item.id)}
        contentContainerStyle={styles.list}
        refreshing={loading}
        onRefresh={load}
        ListEmptyComponent={!loading ? <EmptyState title="No FAQs yet" /> : null}
        renderItem={({ item }) => (
          <View style={styles.row}>
            <Pressable style={styles.rowMain} onPress={() => setEditing(item)}>
              <Text style={styles.question}>{item.question}</Text>
            </Pressable>
            <Pressable onPress={() => onDelete(item.id)}>
              <Text style={styles.delete}>Delete</Text>
            </Pressable>
          </View>
        )}
      />
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
    flexDirection: 'row',
    alignItems: 'center',
  },
  rowMain: { flex: 1 },
  question: { color: colors.text, fontWeight: '700' },
  delete: { color: colors.danger, fontWeight: '600', fontSize: 12 },
  inputLight: { color: '#F8FAFC', backgroundColor: '#1E293B', borderColor: '#334155' },
});

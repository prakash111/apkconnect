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
import { BlogInput } from '../../api/admin';
import { colors, radius, spacing } from '../../theme/theme';

const EMPTY: BlogInput = { title: '', excerpt: '', content: '', category: '', tags: '' };

export default function AdminBlogsScreen() {
  const [blogs, setBlogs] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [editing, setEditing] = useState<BlogInput | null>(null);
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const res = await adminApi.getAdminBlogs();
      if (res.status === 'success') setBlogs(res.blogs || []);
      else setError(res.message || 'Could not load blogs.');
    } catch (e: any) {
      setError(e?.message || 'Could not load blogs.');
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
    if (!editing?.title || !editing?.content) {
      setError('Title and content are required.');
      return;
    }
    setSaving(true);
    setError('');
    try {
      const res = await adminApi.saveAdminBlog(editing);
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
    Alert.alert('Delete post', 'This cannot be undone.', [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Delete',
        style: 'destructive',
        onPress: async () => {
          try {
            await adminApi.deleteAdminBlog(id);
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
          <SectionTitle>{editing.id ? 'Edit post' : 'New post'}</SectionTitle>
          <Banner type="error" message={error} />
          <Label>Title</Label>
          <Input value={editing.title} onChangeText={v => setEditing({ ...editing, title: v })} style={styles.inputLight} />
          <Label>Excerpt</Label>
          <Input value={editing.excerpt} onChangeText={v => setEditing({ ...editing, excerpt: v })} multiline style={styles.inputLight} />
          <Label>Content</Label>
          <Input value={editing.content} onChangeText={v => setEditing({ ...editing, content: v })} multiline style={[styles.inputLight, { minHeight: 200 }]} />
          <Label>Category</Label>
          <Input value={editing.category} onChangeText={v => setEditing({ ...editing, category: v })} style={styles.inputLight} />
          <Label>Tags (comma separated)</Label>
          <Input value={editing.tags} onChangeText={v => setEditing({ ...editing, tags: v })} style={styles.inputLight} />
          <Button title="Save" onPress={onSave} loading={saving} />
          <Button title="Cancel" variant="ghost" onPress={() => setEditing(null)} />
        </Card>
      </View>
    );
  }

  return (
    <View style={styles.flex}>
      <LoadingOverlay visible={loading && blogs.length === 0} label="Loading…" />
      <View style={styles.header}>
        <Button title="+ New post" variant="secondary" onPress={() => setEditing(EMPTY)} />
      </View>
      <FlatList
        data={blogs}
        keyExtractor={item => String(item.id)}
        contentContainerStyle={styles.list}
        refreshing={loading}
        onRefresh={load}
        ListEmptyComponent={!loading ? <EmptyState title="No blog posts yet" /> : null}
        renderItem={({ item }) => (
          <View style={styles.row}>
            <Pressable style={styles.rowMain} onPress={() => setEditing(item)}>
              <Text style={styles.title}>{item.title}</Text>
              <Text style={styles.meta}>{item.category}</Text>
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
  title: { color: colors.text, fontWeight: '700' },
  meta: { color: colors.textMuted, fontSize: 11, marginTop: 2 },
  delete: { color: colors.danger, fontWeight: '600', fontSize: 12 },
  inputLight: { color: '#F8FAFC', backgroundColor: '#1E293B', borderColor: '#334155' },
  inputLightText: { color: '#F8FAFC', backgroundColor: '#1E293B' },
});

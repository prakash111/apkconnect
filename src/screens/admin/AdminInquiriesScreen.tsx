import React, { useCallback, useState } from 'react';
import { View, Text, FlatList, StyleSheet, Pressable, Alert } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { EmptyState, LoadingOverlay, Banner } from '../../components/UI';
import * as adminApi from '../../api/admin';
import { colors, radius, spacing } from '../../theme/theme';

interface Inquiry {
  id: number;
  name: string;
  email: string;
  message: string;
  is_read: 0 | 1;
  created_at: string;
}

export default function AdminInquiriesScreen() {
  const [items, setItems] = useState<Inquiry[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const res = await adminApi.getContactInquiries();
      if (res.status === 'success') setItems(res.inquiries || []);
      else setError(res.message || 'Could not load inquiries.');
    } catch (e: any) {
      setError(e?.message || 'Could not load inquiries.');
    } finally {
      setLoading(false);
    }
  }, []);

  useFocusEffect(
    useCallback(() => {
      load();
    }, [load]),
  );

  const onOpen = async (item: Inquiry) => {
    if (!item.is_read) {
      try {
        await adminApi.markContactInquiryRead(item.id);
        setItems(prev => prev.map(i => (i.id === item.id ? { ...i, is_read: 1 } : i)));
      } catch {
        // non-fatal
      }
    }
    Alert.alert(item.name, `${item.email}\n\n${item.message}`);
  };

  const onDelete = (item: Inquiry) => {
    Alert.alert('Delete inquiry', `Delete message from ${item.name}?`, [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Delete',
        style: 'destructive',
        onPress: async () => {
          try {
            await adminApi.deleteContactInquiry(item.id);
            setItems(prev => prev.filter(i => i.id !== item.id));
          } catch (e: any) {
            Alert.alert('Error', e?.message || 'Delete failed.');
          }
        },
      },
    ]);
  };

  return (
    <View style={styles.flex}>
      <LoadingOverlay visible={loading && items.length === 0} label="Loading…" />
      {error ? (
        <View style={{ padding: spacing.md }}>
          <Banner type="error" message={error} />
        </View>
      ) : null}
      <FlatList
        data={items}
        keyExtractor={item => String(item.id)}
        contentContainerStyle={styles.list}
        refreshing={loading}
        onRefresh={load}
        ListEmptyComponent={!loading ? <EmptyState title="No messages" /> : null}
        renderItem={({ item }) => (
          <Pressable style={styles.row} onPress={() => onOpen(item)} onLongPress={() => onDelete(item)}>
            <View style={styles.rowHeader}>
              <Text style={styles.name}>{!item.is_read ? '● ' : ''}{item.name}</Text>
              <Text style={styles.date}>{item.created_at}</Text>
            </View>
            <Text style={styles.email}>{item.email}</Text>
            <Text style={styles.message} numberOfLines={2}>
              {item.message}
            </Text>
          </Pressable>
        )}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1, backgroundColor: colors.bg },
  list: { padding: spacing.md, paddingBottom: spacing.xl },
  row: {
    backgroundColor: colors.surface,
    borderRadius: radius.sm,
    borderWidth: 1,
    borderColor: colors.border,
    padding: spacing.sm,
    marginBottom: 8,
  },
  rowHeader: { flexDirection: 'row', justifyContent: 'space-between' },
  name: { color: colors.text, fontWeight: '700' },
  date: { color: colors.textMuted, fontSize: 11 },
  email: { color: colors.textMuted, fontSize: 12, marginTop: 2 },
  message: { color: colors.text, fontSize: 13, marginTop: 6 },
});

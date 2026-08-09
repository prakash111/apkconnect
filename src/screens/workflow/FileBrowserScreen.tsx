import React, { useCallback, useState } from 'react';
import { View, Text, FlatList, Pressable, StyleSheet } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { EmptyState, LoadingOverlay, Banner } from '../../components/UI';
import * as workflowApi from '../../api/workflow';
import { DirItem } from '../../types';
import { colors, radius, spacing } from '../../theme/theme';

function humanSize(bytes: number) {
  if (!bytes) return '';
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

export default function FileBrowserScreen({ navigation, route }: any) {
  const dirPath: string = route.params?.dirPath ?? '';
  const [items, setItems] = useState<DirItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const res = await workflowApi.getDir(dirPath);
      if (res.status === 'success') setItems(res.items || []);
      else setError(res.message || 'Could not list directory.');
    } catch (e: any) {
      setError(e?.message || 'Could not list directory.');
    } finally {
      setLoading(false);
    }
  }, [dirPath]);

  useFocusEffect(
    useCallback(() => {
      navigation.setOptions({ title: dirPath || 'Project Files' });
      load();
    }, [load, navigation, dirPath]),
  );

  const openItem = (item: DirItem) => {
    if (item.is_dir) {
      navigation.push('FileBrowser', { dirPath: item.path });
    } else if (/\.so$/i.test(item.name)) {
      navigation.navigate('HexSearch', { filePath: item.path });
    } else {
      navigation.navigate('FileEditor', { filePath: item.path });
    }
  };

  return (
    <View style={styles.flex}>
      <LoadingOverlay visible={loading && items.length === 0} label="Loading files…" />
      {error ? (
        <View style={{ padding: spacing.md }}>
          <Banner type="error" message={error} />
        </View>
      ) : null}
      <FlatList
        data={items}
        keyExtractor={item => item.path}
        contentContainerStyle={styles.list}
        refreshing={loading}
        onRefresh={load}
        ListEmptyComponent={!loading ? <EmptyState title="Empty folder" /> : null}
        renderItem={({ item }) => (
          <Pressable style={styles.row} onPress={() => openItem(item)}>
            <Text style={styles.icon}>{item.is_dir ? '📁' : '📄'}</Text>
            <View style={styles.rowMain}>
              <Text style={styles.name} numberOfLines={1}>
                {item.name}
              </Text>
              {!item.is_dir ? <Text style={styles.size}>{humanSize(item.size)}</Text> : null}
            </View>
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
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.surface,
    borderRadius: radius.sm,
    borderWidth: 1,
    borderColor: colors.border,
    padding: spacing.sm,
    marginBottom: 8,
  },
  icon: { fontSize: 20, marginRight: spacing.sm },
  rowMain: { flex: 1 },
  name: { color: colors.text, fontWeight: '600' },
  size: { color: colors.textMuted, fontSize: 11, marginTop: 2 },
});

import React, { useCallback, useState } from 'react';
import { View, Text, FlatList, StyleSheet } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { Card, Input, Label, Button, Banner, HelperText, EmptyState } from '../../components/UI';
import * as workflowApi from '../../api/workflow';
import { colors, radius, spacing } from '../../theme/theme';

interface HexResult {
  offset: number;
  hex_offset: string;
  hex_snippet: string;
  ascii_snippet: string;
}

export default function HexSearchScreen({ navigation, route }: any) {
  const filePath: string = route.params?.filePath ?? '';
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<HexResult[]>([]);
  const [count, setCount] = useState(0);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useFocusEffect(
    useCallback(() => {
      navigation.setOptions({ title: filePath ? filePath.split('/').pop() : 'Hex Search' });
    }, [navigation, filePath]),
  );

  const onSearch = async () => {
    if (!filePath) {
      setError('Open this screen from a .so file in the file browser.');
      return;
    }
    if (!query) {
      setError('Enter a hex string (e.g. DEADBEEF) or plain text to search for.');
      return;
    }
    setLoading(true);
    setError('');
    try {
      const res = await workflowApi.searchHex(filePath, query);
      if (res.status === 'success') {
        setResults(res.results || []);
        setCount(res.count || 0);
      } else {
        setError(res.message || 'Search failed.');
      }
    } catch (e: any) {
      setError(e?.message || 'Search failed.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <View style={styles.flex}>
      <Card>
        <Banner type="error" message={error} />
        <Label>File</Label>
        <Text style={styles.filePath}>{filePath || '(open from Files → a .so file)'}</Text>
        <Label>Search query</Label>
        <Input
          value={query}
          onChangeText={setQuery}
          autoCapitalize="none"
          autoCorrect={false}
          placeholder="DE AD BE EF  or  plain text"
        />
        <HelperText>
          Accepts hex bytes (with or without spaces) or plain ASCII text. Matches show the byte
          offset plus a hex/ASCII preview.
        </HelperText>
        <Button title="Search" onPress={onSearch} loading={loading} />
      </Card>
      <FlatList
        data={results}
        keyExtractor={(item, idx) => `${item.offset}-${idx}`}
        contentContainerStyle={styles.list}
        ListHeaderComponent={count > 0 ? <Text style={styles.count}>{count} match(es)</Text> : null}
        ListEmptyComponent={!loading && query ? <EmptyState title="No matches" /> : null}
        renderItem={({ item }) => (
          <View style={styles.resultRow}>
            <Text style={styles.offset}>0x{item.hex_offset}</Text>
            <Text style={styles.hex}>{item.hex_snippet}</Text>
            <Text style={styles.ascii}>{item.ascii_snippet}</Text>
          </View>
        )}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1, backgroundColor: colors.bg, padding: spacing.md },
  filePath: { color: colors.textMuted, fontSize: 12, marginBottom: 4 },
  list: { paddingBottom: spacing.xl },
  count: { color: colors.textMuted, marginBottom: spacing.sm },
  resultRow: {
    backgroundColor: colors.surface,
    borderRadius: radius.sm,
    borderWidth: 1,
    borderColor: colors.border,
    padding: spacing.sm,
    marginBottom: 8,
  },
  offset: { color: '#38BDF8', fontFamily: 'monospace', fontSize: 12, fontWeight: '700' },
  hex: { color: '#F8FAFC', fontFamily: 'monospace', fontSize: 12, marginTop: 4 },
  ascii: { color: '#CBD5E1', fontFamily: 'monospace', fontSize: 12, marginTop: 2 },
});

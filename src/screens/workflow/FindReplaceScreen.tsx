import React, { useState } from 'react';
import { View, Text, StyleSheet, FlatList, Pressable } from 'react-native';
import { Card, Input, Label, Button, Banner, HelperText, SectionTitle } from '../../components/UI';
import * as workflowApi from '../../api/workflow';
import { colors, radius, spacing } from '../../theme/theme';

interface MatchedLine {
  line: number;
  snippet: string;
}

interface MatchedFile {
  path: string;
  matches?: number;
  name_match?: boolean;
  is_so?: boolean;
  offsets?: string[];
  lines?: MatchedLine[];
}

export default function FindReplaceScreen({ navigation }: any) {
  const [find, setFind] = useState('');
  const [replace, setReplace] = useState('');
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState<'find' | 'replace' | null>(null);
  const [matchedFiles, setMatchedFiles] = useState<MatchedFile[]>([]);
  const [searchRan, setSearchRan] = useState(false);

  const onFind = async () => {
    if (!find.trim()) {
      setError('Enter text to find.');
      return;
    }
    setBusy('find');
    setError('');
    setMessage('');
    setSearchRan(true);
    try {
      const res = await workflowApi.findInProject(find.trim());
      if (res.status === 'success') {
        const files: MatchedFile[] =
          res.files ||
          res.result?.files ||
          res.result?.matched_files ||
          res.state?.last_find_only?.files ||
          res.state?.last_find_only?.matched_files ||
          [];
        setMatchedFiles(files);
        setMessage(res.message || `Found occurrences in ${files.length} file(s).`);
      } else {
        setError(res.message || 'Search failed.');
        setMatchedFiles([]);
      }
    } catch (e: any) {
      setError(e?.message || 'Search failed.');
      setMatchedFiles([]);
    } finally {
      setBusy(null);
    }
  };

  const onReplace = async () => {
    if (!find.trim()) {
      setError('Enter text to find.');
      return;
    }
    setBusy('replace');
    setError('');
    setMessage('');
    setSearchRan(true);
    try {
      const res = await workflowApi.findReplaceInProject(find.trim(), replace);
      if (res.status === 'success') {
        const files: MatchedFile[] =
          res.files ||
          res.result?.files ||
          res.result?.matched_files ||
          res.state?.last_find_replace?.files ||
          res.state?.last_find_replace?.matched_files ||
          [];
        setMatchedFiles(files);
        setMessage(res.message || `Replaced occurrences in ${files.length} file(s).`);
      } else {
        setError(res.message || 'Replace failed.');
      }
    } catch (e: any) {
      setError(e?.message || 'Replace failed.');
    } finally {
      setBusy(null);
    }
  };

  const openInEditor = (filePath: string, lineNumber?: number) => {
    navigation.navigate('FileEditor', { filePath, lineNumber });
  };

  const renderHighlightedSnippet = (snippet: string, query: string) => {
    if (!query.trim() || !snippet) {
      return <Text style={styles.snippetText} numberOfLines={1}>{snippet}</Text>;
    }
    const safeQuery = query.trim().replace(/[-[\]{}()*+?.,\\^$|#\s]/g, '\\$&');
    const parts = snippet.split(new RegExp(`(${safeQuery})`, 'gi'));
    return (
      <Text style={styles.snippetText} numberOfLines={1}>
        {parts.map((part, i) =>
          part.toLowerCase() === query.trim().toLowerCase() ? (
            <Text key={i} style={styles.highlightedMatch}>
              {part}
            </Text>
          ) : (
            <Text key={i}>{part}</Text>
          )
        )}
      </Text>
    );
  };

  return (
    <View style={styles.flex}>
      <FlatList
        data={matchedFiles}
        keyExtractor={item => item.path}
        contentContainerStyle={styles.list}
        keyboardShouldPersistTaps="handled"
        ListHeaderComponent={
          <View>
            <Card>
              <SectionTitle>Search & Replace in Project</SectionTitle>
              <Banner type="error" message={error} />
              <Banner type="success" message={message} />
              <Label>Find Text</Label>
              <Input
                value={find}
                onChangeText={setFind}
                placeholder="Search across all code & XML…"
                autoCapitalize="none"
                autoCorrect={false}
              />
              <Label>Replace with (optional)</Label>
              <Input
                value={replace}
                onChangeText={setReplace}
                placeholder="Replacement string…"
                autoCapitalize="none"
                autoCorrect={false}
              />
              <HelperText>
                Scans every Smali bytecode, XML, JSON, and source file in the decompiled project.
              </HelperText>
              <View style={styles.btnRow}>
                <Button
                  title="Find Only"
                  variant="secondary"
                  onPress={onFind}
                  loading={busy === 'find'}
                  style={{ flex: 1 }}
                />
                <Button
                  title="Find & Replace"
                  onPress={onReplace}
                  loading={busy === 'replace'}
                  style={{ flex: 1 }}
                />
              </View>
            </Card>

            {searchRan && (
              <View style={styles.resultsHeader}>
                <SectionTitle>Matching Files ({matchedFiles.length})</SectionTitle>
              </View>
            )}
          </View>
        }
        renderItem={({ item }) => (
          <View style={styles.resultCard}>
            <View style={styles.resultMain}>
              <Text style={styles.filePath} numberOfLines={2}>
                📄 {item.path}
              </Text>
              <View style={styles.metaRow}>
                {item.matches !== undefined && item.matches > 0 && (
                  <View style={styles.matchBadge}>
                    <Text style={styles.matchBadgeText}>{item.matches} match(es)</Text>
                  </View>
                )}
                {item.name_match && (
                  <View style={styles.nameMatchBadge}>
                    <Text style={styles.nameMatchBadgeText}>Filename match</Text>
                  </View>
                )}
                {item.is_so && (
                  <View style={styles.binaryBadge}>
                    <Text style={styles.binaryBadgeText}>.so Binary</Text>
                  </View>
                )}
              </View>
              {item.lines && item.lines.length > 0 && (
                <View style={styles.snippetContainer}>
                  {item.lines.map((l, idx) => (
                    <Pressable
                      key={idx}
                      style={styles.snippetRow}
                      onPress={() => openInEditor(item.path, l.line)}>
                      <Text style={styles.lineBadge}>L{l.line}</Text>
                      {renderHighlightedSnippet(l.snippet, find)}
                    </Pressable>
                  ))}
                </View>
              )}
              {item.offsets && item.offsets.length > 0 && (
                <Text style={styles.offsetText}>
                  Offsets: {item.offsets.join(', ')}
                </Text>
              )}
            </View>

            <Button
              title="Edit File"
              small
              variant="secondary"
              onPress={() => openInEditor(item.path)}
              style={styles.editBtn}
            />
          </View>
        )}
        ListEmptyComponent={
          searchRan && !busy ? (
            <View style={styles.emptyContainer}>
              <Text style={styles.emptyEmoji}>🔍</Text>
              <Text style={styles.emptyText}>No matching occurrences found in project files.</Text>
            </View>
          ) : null
        }
      />
    </View>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1, backgroundColor: colors.bg },
  list: { padding: spacing.md, paddingBottom: spacing.xl },
  btnRow: {
    flexDirection: 'row',
    gap: spacing.sm,
    marginTop: spacing.md,
  },
  resultsHeader: {
    marginTop: spacing.sm,
    marginBottom: spacing.xs,
  },
  resultCard: {
    backgroundColor: colors.surface,
    borderRadius: radius.sm,
    borderWidth: 1,
    borderColor: colors.border,
    padding: spacing.sm,
    marginBottom: spacing.sm,
    flexDirection: 'row',
    alignItems: 'flex-start',
    justifyContent: 'space-between',
  },
  resultMain: { flex: 1, marginRight: spacing.sm },
  filePath: {
    color: colors.text,
    fontFamily: 'monospace',
    fontSize: 13,
    fontWeight: '600',
  },
  metaRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    marginTop: 4,
    flexWrap: 'wrap',
  },
  matchBadge: {
    backgroundColor: 'rgba(99, 102, 241, 0.2)',
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: 4,
  },
  matchBadgeText: { color: colors.primary, fontSize: 11, fontWeight: '700' },
  nameMatchBadge: {
    backgroundColor: 'rgba(34, 197, 94, 0.2)',
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: 4,
  },
  nameMatchBadgeText: { color: colors.success, fontSize: 11, fontWeight: '700' },
  binaryBadge: {
    backgroundColor: 'rgba(245, 158, 11, 0.2)',
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: 4,
  },
  binaryBadgeText: { color: colors.warning, fontSize: 11, fontWeight: '700' },
  snippetContainer: {
    marginTop: 6,
    backgroundColor: '#0F172A',
    borderColor: '#1E293B',
    borderWidth: 1,
    borderRadius: radius.xs,
    padding: 6,
  },
  snippetRow: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 3,
    paddingHorizontal: 4,
    gap: 6,
  },
  lineBadge: {
    color: '#A5B4FC',
    fontWeight: '700',
    fontSize: 10,
    fontFamily: 'monospace',
    backgroundColor: '#312E81',
    paddingHorizontal: 5,
    paddingVertical: 1,
    borderRadius: 3,
  },
  snippetText: {
    color: '#F8FAFC',
    fontFamily: 'monospace',
    fontSize: 12,
    flex: 1,
  },
  highlightedMatch: {
    backgroundColor: '#3730A3',
    color: '#FEF08A',
    fontWeight: '700',
    borderRadius: 3,
    paddingHorizontal: 3,
  },
  offsetText: {
    color: '#38BDF8',
    fontSize: 11,
    fontFamily: 'monospace',
    marginTop: 4,
  },
  editBtn: { minWidth: 80, marginTop: 0 },
  emptyContainer: {
    padding: spacing.xl,
    alignItems: 'center',
  },
  emptyEmoji: { fontSize: 32, marginBottom: spacing.xs },
  emptyText: { color: colors.textMuted, fontSize: 14, textAlign: 'center' },
});

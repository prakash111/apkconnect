import React, { useCallback, useMemo, useState } from 'react';
import {
  View,
  Text,
  FlatList,
  StyleSheet,
  Pressable,
  Modal,
  Alert,
  ScrollView,
} from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { Input, Button, Banner, LoadingOverlay, Label, SectionTitle, Card } from '../../components/UI';
import * as workflowApi from '../../api/workflow';
import { colors, radius, spacing } from '../../theme/theme';

export default function StringsEditorScreen() {
  const [appName, setAppName] = useState('');
  const [values, setValues] = useState<Record<string, string>>({});
  const [initialValues, setInitialValues] = useState<Record<string, string>>({});
  const [locales, setLocales] = useState<string[]>(['values']);
  const [selectedLocale, setSelectedLocale] = useState('values');
  const [searchQuery, setSearchQuery] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  // Add String Modal State
  const [addModalVisible, setAddModalVisible] = useState(false);
  const [newKey, setNewKey] = useState('');
  const [newValue, setNewValue] = useState('');

  const load = useCallback(async (localeToLoad = selectedLocale) => {
    setLoading(true);
    setError('');
    try {
      const res = await workflowApi.loadStrings(localeToLoad);
      if (res.status === 'success') {
        const state = res.state;
        const stringsMap: Record<string, string> = {};

        // 1. Load from all_strings if available
        if (Array.isArray(state?.all_strings)) {
          state.all_strings.forEach((item: any) => {
            if (item && item.name) {
              stringsMap[item.name] = String(item.value ?? '');
            }
          });
        }

        // 2. Load from editor_file strings if available
        if (state?.editor_file?.strings && typeof state.editor_file.strings === 'object') {
          Object.assign(stringsMap, state.editor_file.strings);
        }

        setValues(stringsMap);
        setInitialValues(stringsMap);

        // Auto-populate App Name from decompiled project detection
        const detectedName = state?.app_name || stringsMap['app_name'] || '';
        setAppName(detectedName);

        if (Array.isArray(state?.locale_files) && state.locale_files.length > 0) {
          setLocales(state.locale_files);
        }
      } else {
        setError(res.message || 'Could not load strings.');
      }
    } catch (e: any) {
      setError(e?.message || 'Could not load strings.');
    } finally {
      setLoading(false);
    }
  }, [selectedLocale]);

  useFocusEffect(
    useCallback(() => {
      load(selectedLocale);
    }, [load, selectedLocale]),
  );

  const updateValue = (key: string, value: string) => {
    setValues(prev => ({ ...prev, [key]: value }));
  };

  const deleteString = (key: string) => {
    Alert.alert('Delete string', `Are you sure you want to remove "${key}"?`, [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Delete',
        style: 'destructive',
        onPress: () => {
          setValues(prev => {
            const next = { ...prev };
            delete next[key];
            return next;
          });
        },
      },
    ]);
  };

  const handleAddString = () => {
    const trimmedKey = newKey.trim().replace(/[^a-zA-Z0-9_]/g, '_');
    if (!trimmedKey) {
      Alert.alert('Invalid Key', 'Enter a valid string identifier (e.g. action_save).');
      return;
    }
    setValues(prev => ({ ...prev, [trimmedKey]: newValue }));
    setNewKey('');
    setNewValue('');
    setAddModalVisible(false);
    setMessage(`Added string "${trimmedKey}". Remember to tap Save.`);
  };

  const onSave = async () => {
    setSaving(true);
    setError('');
    setMessage('');
    try {
      const payload = { ...values };
      if (appName.trim()) {
        payload['app_name'] = appName.trim();
      }
      const res = await workflowApi.autosaveStrings(selectedLocale, payload, appName.trim());
      if (res.status === 'success') {
        setMessage('App name and strings saved successfully.');
        setInitialValues(payload);
      } else {
        setError(res.message || 'Save failed.');
      }
    } catch (e: any) {
      setError(e?.message || 'Save failed.');
    } finally {
      setSaving(false);
    }
  };

  const onReset = () => {
    setValues(initialValues);
    setAppName(initialValues['app_name'] || '');
    setMessage('Reverted unsaved changes.');
  };

  const filteredEntries = useMemo(() => {
    const q = searchQuery.toLowerCase().trim();
    return Object.entries(values)
      .filter(([k]) => k !== 'app_name')
      .filter(([k, v]) => !q || k.toLowerCase().includes(q) || String(v).toLowerCase().includes(q));
  }, [values, searchQuery]);

  return (
    <View style={styles.flex}>
      <LoadingOverlay visible={loading} label="Loading project strings…" />

      <FlatList
        data={filteredEntries}
        keyExtractor={([k]) => k}
        contentContainerStyle={styles.list}
        keyboardShouldPersistTaps="handled"
        ListHeaderComponent={
          <View>
            <Banner type="error" message={error} />
            <Banner type="success" message={message} />

            {/* App Name Section */}
            <Card>
              <SectionTitle>📱 App Name</SectionTitle>
              <Label>Application Title (auto-filled from decompiled project)</Label>
              <Input
                value={appName}
                onChangeText={setAppName}
                placeholder="My Application"
              />
            </Card>

            {/* Locale & Search Header */}
            <Card>
              <SectionTitle>🔍 Project Strings & Values</SectionTitle>
              {locales.length > 1 && (
                <View style={styles.localeRow}>
                  <Label>Locale File:</Label>
                  <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.localeScroll}>
                    {locales.map(loc => (
                      <Pressable
                        key={loc}
                        onPress={() => {
                          setSelectedLocale(loc);
                          load(loc);
                        }}
                        style={[
                          styles.localeChip,
                          selectedLocale === loc && styles.localeChipActive,
                        ]}>
                        <Text
                          style={[
                            styles.localeChipText,
                            selectedLocale === loc && styles.localeChipTextActive,
                          ]}>
                          {loc}
                        </Text>
                      </Pressable>
                    ))}
                  </ScrollView>
                </View>
              )}

              <Label>Search / Filter Strings</Label>
              <Input
                value={searchQuery}
                onChangeText={setSearchQuery}
                placeholder="Filter by key or text content…"
                autoCapitalize="none"
                autoCorrect={false}
              />

              <View style={styles.headerActionRow}>
                <Button
                  title="+ Add New String"
                  small
                  variant="secondary"
                  onPress={() => setAddModalVisible(true)}
                  style={styles.addBtn}
                />
                <Text style={styles.countText}>
                  Showing {filteredEntries.length} of {Object.keys(values).length} string(s)
                </Text>
              </View>
            </Card>
          </View>
        }
        renderItem={({ item: [key, value] }) => (
          <View style={styles.row}>
            <View style={styles.rowTop}>
              <View style={styles.keyBadge}>
                <Text style={styles.keyText}>{key}</Text>
              </View>
              <Pressable onPress={() => deleteString(key)} style={styles.deleteBtn}>
                <Text style={styles.deleteBtnText}>Remove</Text>
              </Pressable>
            </View>
            <Input
              value={value}
              onChangeText={v => updateValue(key, v)}
              multiline
              placeholder="String value…"
              style={styles.stringInput}
            />
          </View>
        )}
        ListEmptyComponent={
          !loading ? (
            <View style={styles.emptyContainer}>
              <Text style={styles.emptyText}>
                {searchQuery ? 'No matching strings found.' : 'No strings found in this locale.'}
              </Text>
            </View>
          ) : null
        }
        ListFooterComponent={
          <View style={styles.footerRow}>
            <Button
              title="Save All Changes"
              onPress={onSave}
              loading={saving}
              style={styles.saveBtn}
            />
            <Button
              title="Reset"
              variant="secondary"
              onPress={onReset}
              style={styles.resetBtn}
            />
          </View>
        }
      />

      {/* Add New String Modal */}
      <Modal
        visible={addModalVisible}
        transparent
        animationType="slide"
        onRequestClose={() => setAddModalVisible(false)}>
        <View style={styles.modalBackdrop}>
          <View style={styles.modalCard}>
            <SectionTitle>Add New String</SectionTitle>
            <Label>String Key / Identifier</Label>
            <Input
              value={newKey}
              onChangeText={setNewKey}
              placeholder="e.g. custom_greeting"
              autoCapitalize="none"
              autoCorrect={false}
              autoFocus
            />
            <Label>String Value</Label>
            <Input
              value={newValue}
              onChangeText={setNewValue}
              placeholder="e.g. Hello, Welcome!"
              multiline
            />
            <View style={styles.modalBtnRow}>
              <Button title="Add to Strings" onPress={handleAddString} style={{ flex: 1 }} />
              <Button
                title="Cancel"
                variant="ghost"
                onPress={() => setAddModalVisible(false)}
                style={{ flex: 1 }}
              />
            </View>
          </View>
        </View>
      </Modal>
    </View>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1, backgroundColor: colors.bg },
  list: { padding: spacing.md, paddingBottom: spacing.xl },
  localeRow: { marginBottom: spacing.sm },
  localeScroll: { flexDirection: 'row', marginTop: 4 },
  localeChip: {
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: radius.sm,
    backgroundColor: colors.surfaceAlt,
    borderWidth: 1,
    borderColor: colors.border,
    marginRight: 6,
  },
  localeChipActive: {
    backgroundColor: colors.primary,
    borderColor: colors.primary,
  },
  localeChipText: { color: colors.textMuted, fontSize: 12, fontWeight: '600' },
  localeChipTextActive: { color: '#ffffff' },
  headerActionRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginTop: spacing.sm,
  },
  addBtn: { marginTop: 0 },
  countText: { color: colors.textMuted, fontSize: 12 },
  row: {
    backgroundColor: colors.surface,
    borderRadius: radius.sm,
    borderWidth: 1,
    borderColor: colors.border,
    padding: spacing.sm,
    marginBottom: spacing.sm,
  },
  rowTop: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 6,
  },
  keyBadge: {
    backgroundColor: colors.surfaceAlt,
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: 4,
    borderWidth: 1,
    borderColor: colors.border,
    flex: 1,
    marginRight: spacing.sm,
  },
  keyText: {
    color: colors.primary,
    fontSize: 12,
    fontWeight: '700',
    fontFamily: 'monospace',
  },
  deleteBtn: { paddingVertical: 4, paddingHorizontal: 6 },
  deleteBtnText: { color: colors.danger, fontSize: 12, fontWeight: '600' },
  stringInput: {
    minHeight: 40,
    fontSize: 14,
    color: '#f8fafc',
  },
  emptyContainer: { padding: spacing.lg, alignItems: 'center' },
  emptyText: { color: colors.textMuted, fontSize: 14 },
  footerRow: {
    flexDirection: 'row',
    gap: spacing.sm,
    marginTop: spacing.md,
    marginBottom: spacing.xl,
  },
  saveBtn: { flex: 2, marginTop: 0 },
  resetBtn: { flex: 1, marginTop: 0 },
  modalBackdrop: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.65)',
    justifyContent: 'flex-end',
  },
  modalCard: {
    backgroundColor: colors.surface,
    borderTopLeftRadius: 20,
    borderTopRightRadius: 20,
    padding: spacing.md,
    borderWidth: 1,
    borderColor: colors.border,
  },
  modalBtnRow: {
    flexDirection: 'row',
    gap: spacing.sm,
    marginTop: spacing.md,
  },
});

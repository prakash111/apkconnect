import React, { useCallback, useState } from 'react';
import { View, Text, StyleSheet, Pressable, ScrollView } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { Card, Input, Label, Button, Banner, HelperText, SectionTitle, LoadingOverlay } from '../../components/UI';
import * as aiApi from '../../api/ai';
import { colors, radius, spacing } from '../../theme/theme';

type Provider = 'gemini' | 'openai';

export default function AiSettingsScreen() {
  const [provider, setProvider] = useState<Provider>('gemini');
  const [hasGeminiKey, setHasGeminiKey] = useState(false);
  const [hasOpenAiKey, setHasOpenAiKey] = useState(false);
  const [geminiKey, setGeminiKey] = useState('');
  const [openAiKey, setOpenAiKey] = useState('');
  const [textModel, setTextModel] = useState('');
  const [imageModel, setImageModel] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await aiApi.getAiSettings();
      if (res.status === 'success') {
        const prov = (res.settings?.provider || res.provider || 'gemini') as Provider;
        setProvider(prov);
        setHasGeminiKey(Boolean(res.settings?.has_gemini_key ?? res.settings?.gemini_has_key ?? res.gemini_has_key ?? res.has_gemini_key));
        setHasOpenAiKey(Boolean(res.settings?.has_openai_key ?? res.settings?.openai_has_key ?? res.openai_has_key ?? res.has_openai_key));
        setTextModel(res.settings?.text_model ?? res.user_models?.[`${prov}_text_model`] ?? res.effective_models?.[`${prov}_text_model`] ?? '');
        setImageModel(res.settings?.image_model ?? res.user_models?.[`${prov}_image_model`] ?? res.effective_models?.[`${prov}_image_model`] ?? '');
      }
    } catch (e: any) {
      setError(e?.message || 'Could not load AI settings.');
    } finally {
      setLoading(false);
    }
  }, []);

  useFocusEffect(
    useCallback(() => {
      load();
    }, [load]),
  );

  const onSelectProvider = async (p: Provider) => {
    setProvider(p);
    setError('');
    setMessage('');
    try {
      const res = await aiApi.saveAiProvider(p);
      if (res.status !== 'success') setError(res.message || 'Could not switch provider.');
      else await load();
    } catch (e: any) {
      setError(e?.message || 'Could not switch provider.');
    }
  };

  const onSaveKey = async (p: Provider) => {
    const key = p === 'gemini' ? geminiKey : openAiKey;
    if (!key) {
      setError('Enter an API key first.');
      return;
    }
    setSaving(true);
    setError('');
    setMessage('');
    try {
      const res = await aiApi.saveApiKey(p, key);
      if (res.status === 'success') {
        setMessage(res.message || 'API key saved.');
        if (p === 'gemini') {
          setHasGeminiKey(true);
          setGeminiKey('');
        } else {
          setHasOpenAiKey(true);
          setOpenAiKey('');
        }
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

  const onDeleteKey = async (p: Provider) => {
    setSaving(true);
    setError('');
    setMessage('');
    try {
      const res = await aiApi.deleteApiKey(p);
      if (res.status === 'success') {
        setMessage(res.message || 'API key removed.');
        if (p === 'gemini') setHasGeminiKey(false);
        else setHasOpenAiKey(false);
        await load();
      } else {
        setError(res.message || 'Delete failed.');
      }
    } catch (e: any) {
      setError(e?.message || 'Delete failed.');
    } finally {
      setSaving(false);
    }
  };

  const onSaveModels = async () => {
    setSaving(true);
    setError('');
    setMessage('');
    try {
      const fields =
        provider === 'gemini'
          ? { gemini_text_model: textModel, gemini_image_model: imageModel }
          : { openai_text_model: textModel, openai_image_model: imageModel };
      const res = await aiApi.saveUserAiModels(fields);
      if (res.status === 'success') setMessage('Model preferences saved.');
      else setError(res.message || 'Save failed.');
    } catch (e: any) {
      setError(e?.message || 'Save failed.');
    } finally {
      setSaving(false);
    }
  };

  const onResetModels = async () => {
    setSaving(true);
    setError('');
    setMessage('');
    try {
      const res = await aiApi.resetUserAiModels();
      if (res.status === 'success') {
        setMessage('Reset to defaults.');
        await load();
      }
    } finally {
      setSaving(false);
    }
  };

  return (
    <ScrollView style={styles.flex} contentContainerStyle={styles.content}>
      <LoadingOverlay visible={loading} label="Loading AI settings…" />
      <Banner type="error" message={error} />
      <Banner type="success" message={message} />

      <Card>
        <SectionTitle>Provider</SectionTitle>
        <View style={styles.providerRow}>
          {(['gemini', 'openai'] as Provider[]).map(p => (
            <Pressable
              key={p}
              onPress={() => onSelectProvider(p)}
              style={[styles.providerChip, provider === p && styles.providerChipActive]}>
              <Text style={[styles.providerText, provider === p && styles.providerTextActive]}>
                {p === 'gemini' ? 'Google Gemini' : 'OpenAI'}
              </Text>
            </Pressable>
          ))}
        </View>
      </Card>

      <Card>
        <SectionTitle>Gemini API key</SectionTitle>
        <Text style={styles.statusText}>{hasGeminiKey ? '● Key saved' : '○ No key saved'}</Text>
        <Input
          value={geminiKey}
          onChangeText={setGeminiKey}
          placeholder="AIza…"
          secureTextEntry
          autoCapitalize="none"
        />
        <View style={styles.rowBtns}>
          <Button title="Save Key" small onPress={() => onSaveKey('gemini')} loading={saving} style={{ flex: 1 }} />
          {hasGeminiKey ? (
            <Button title="Remove Key" small variant="danger" onPress={() => onDeleteKey('gemini')} style={{ flex: 1 }} />
          ) : null}
        </View>
      </Card>

      <Card>
        <SectionTitle>OpenAI API key</SectionTitle>
        <Text style={styles.statusText}>{hasOpenAiKey ? '● Key saved' : '○ No key saved'}</Text>
        <Input
          value={openAiKey}
          onChangeText={setOpenAiKey}
          placeholder="sk-…"
          secureTextEntry
          autoCapitalize="none"
        />
        <View style={styles.rowBtns}>
          <Button title="Save Key" small onPress={() => onSaveKey('openai')} loading={saving} style={{ flex: 1 }} />
          {hasOpenAiKey ? (
            <Button title="Remove Key" small variant="danger" onPress={() => onDeleteKey('openai')} style={{ flex: 1 }} />
          ) : null}
        </View>
      </Card>

      <Card>
        <SectionTitle>Model overrides (optional)</SectionTitle>
        <HelperText>Leave blank to use the account/server defaults.</HelperText>
        <Label>Text model</Label>
        <Input value={textModel} onChangeText={setTextModel} autoCapitalize="none" />
        <Label>Image model</Label>
        <Input value={imageModel} onChangeText={setImageModel} autoCapitalize="none" />
        <View style={styles.rowBtns}>
          <Button title="Save Models" small onPress={onSaveModels} loading={saving} style={{ flex: 1 }} />
          <Button title="Reset Defaults" small variant="secondary" onPress={onResetModels} style={{ flex: 1 }} />
        </View>
      </Card>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1, backgroundColor: colors.bg },
  content: { padding: spacing.md, paddingBottom: spacing.xl },
  providerRow: { flexDirection: 'row', gap: spacing.sm },
  providerChip: {
    flex: 1,
    paddingVertical: 10,
    borderRadius: radius.sm,
    backgroundColor: colors.surfaceAlt,
    borderWidth: 1,
    borderColor: colors.border,
    alignItems: 'center',
  },
  providerChipActive: { backgroundColor: colors.primary, borderColor: colors.primary },
  providerText: { color: colors.textMuted, fontWeight: '600' },
  providerTextActive: { color: '#fff' },
  statusText: { color: colors.textMuted, fontSize: 12, marginBottom: spacing.sm },
  rowBtns: { flexDirection: 'row', gap: spacing.sm, marginTop: spacing.sm },
});

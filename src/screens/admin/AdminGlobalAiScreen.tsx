import React, { useCallback, useState } from 'react';
import { StyleSheet, ScrollView } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { Card, Input, Label, Button, Banner, SectionTitle, HelperText, LoadingOverlay } from '../../components/UI';
import * as aiApi from '../../api/ai';
import { colors, spacing } from '../../theme/theme';

export default function AdminGlobalAiScreen() {
  const [settings, setSettings] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const res = await aiApi.getGlobalAiSettings();
      if (res.status === 'success') setSettings(res.settings || {});
      else setError(res.message || 'Could not load settings.');
    } catch (e: any) {
      setError(e?.message || 'Could not load settings.');
    } finally {
      setLoading(false);
    }
  }, []);

  useFocusEffect(
    useCallback(() => {
      load();
    }, [load]),
  );

  const update = (field: string, value: string) => setSettings(prev => ({ ...prev, [field]: value }));

  const onSave = async () => {
    setSaving(true);
    setError('');
    setMessage('');
    try {
      const res = await aiApi.saveGlobalAiSettings(settings);
      if (res.status === 'success') setMessage('Global AI defaults saved.');
      else setError(res.message || 'Save failed.');
    } catch (e: any) {
      setError(e?.message || 'Save failed.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <ScrollView style={styles.flex} contentContainerStyle={styles.content}>
      <LoadingOverlay visible={loading} label="Loading…" />
      <Card>
        <SectionTitle>Global AI defaults</SectionTitle>
        <Banner type="error" message={error} />
        <Banner type="success" message={message} />
        <HelperText>
          These apply to any user who hasn't set their own provider/model preferences, and are
          used for server-side operations like the platform's own API key fallback.
        </HelperText>
        <Label>Default provider</Label>
        <Input
          value={settings.default_provider || ''}
          onChangeText={v => update('default_provider', v)}
          placeholder="gemini or openai"
          autoCapitalize="none"
        />
        <Label>Default Gemini text model</Label>
        <Input
          value={settings.gemini_text_model || ''}
          onChangeText={v => update('gemini_text_model', v)}
          autoCapitalize="none"
        />
        <Label>Default Gemini image model</Label>
        <Input
          value={settings.gemini_image_model || ''}
          onChangeText={v => update('gemini_image_model', v)}
          autoCapitalize="none"
        />
        <Label>Default OpenAI text model</Label>
        <Input
          value={settings.openai_text_model || ''}
          onChangeText={v => update('openai_text_model', v)}
          autoCapitalize="none"
        />
        <Label>Default OpenAI image model</Label>
        <Input
          value={settings.openai_image_model || ''}
          onChangeText={v => update('openai_image_model', v)}
          autoCapitalize="none"
        />
        <Button title="Save" onPress={onSave} loading={saving} />
      </Card>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1, backgroundColor: colors.bg },
  content: { padding: spacing.md, paddingBottom: spacing.xl },
});

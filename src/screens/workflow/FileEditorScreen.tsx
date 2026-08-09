import React, { useCallback, useState } from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { pick, types } from 'react-native-document-picker';
import { Input, Button, Banner, LoadingOverlay, HelperText } from '../../components/UI';
import * as workflowApi from '../../api/workflow';
import * as aiApi from '../../api/ai';
import { colors, radius, spacing } from '../../theme/theme';

const IMAGE_EXT = /\.(png|jpg|jpeg|webp|gif)$/i;

export default function FileEditorScreen({ navigation, route }: any) {
  const filePath: string = route.params?.filePath ?? '';
  const lineNumber: number | undefined = route.params?.lineNumber;
  const [content, setContent] = useState('');
  const [binary, setBinary] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [aiBusy, setAiBusy] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState(lineNumber ? `Targeting line ${lineNumber} in file.` : '');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const res = await workflowApi.openEditorFile(filePath);
      if (res.status === 'success') {
        setContent(res.state?.editor_file?.content ?? '');
        setBinary(!!res.state?.editor_file?.binary);
      } else {
        setError(res.message || 'Could not open file.');
      }
    } catch (e: any) {
      setError(e?.message || 'Could not open file.');
    } finally {
      setLoading(false);
    }
  }, [filePath]);

  useFocusEffect(
    useCallback(() => {
      navigation.setOptions({ title: filePath.split('/').pop() });
      load();
    }, [load, navigation, filePath]),
  );

  const onSave = async () => {
    setSaving(true);
    setError('');
    setMessage('');
    try {
      const res = await workflowApi.saveEditorFile(filePath, content);
      if (res.status === 'success') setMessage('Saved.');
      else setError(res.message || 'Save failed.');
    } catch (e: any) {
      setError(e?.message || 'Save failed.');
    } finally {
      setSaving(false);
    }
  };

  const onAiReview = async () => {
    setAiBusy(true);
    setError('');
    setMessage('');
    try {
      const res = await aiApi.aiReviewEditorFile(filePath, content);
      if (res.status === 'success') {
        if (res.changed) {
          setContent(res.state?.editor_file?.content ?? content);
          setMessage(`AI made corrections: ${res.explanation ?? ''}`);
        } else {
          setMessage(res.explanation || 'No issues found.');
        }
      } else {
        setError(res.message || 'AI review failed.');
      }
    } catch (e: any) {
      setError(e?.message || 'AI review failed.');
    } finally {
      setAiBusy(false);
    }
  };

  const onReplaceImage = async () => {
    setError('');
    try {
      const [result] = await pick({ type: [types.images, types.allFiles] });
      setSaving(true);
      const res = await workflowApi.replaceFile(filePath, {
        uri: result.uri,
        name: result.name || 'replacement',
        type: result.type || 'application/octet-stream',
      });
      if (res.status === 'success') setMessage('File replaced.');
      else setError(res.message || 'Replace failed.');
    } catch (e: any) {
      if (e?.message && !/cancel/i.test(e.message)) setError(e.message);
    } finally {
      setSaving(false);
    }
  };

  if (binary && !loading) {
    return (
      <View style={styles.centerFlex}>
        <Text style={styles.title}>{filePath}</Text>
        <HelperText>
          This is a binary/image file and can't be edited as text. You can replace it with a new
          file, or use Hex Search for .so binaries.
        </HelperText>
        <Banner type="error" message={error} />
        <Banner type="success" message={message} />
        {IMAGE_EXT.test(filePath) ? (
          <Button title="Replace with image…" onPress={onReplaceImage} loading={saving} />
        ) : (
          <Button
            title="Open in Hex Search"
            onPress={() => navigation.replace('HexSearch', { filePath })}
          />
        )}
      </View>
    );
  }

  return (
    <View style={styles.flex}>
      <LoadingOverlay visible={loading} label="Opening file…" />
      <View style={styles.toolbar}>
        <Banner type="error" message={error} />
        <Banner type="success" message={message} />
      </View>
      <Input
        value={content}
        onChangeText={setContent}
        multiline
        style={styles.editor}
        placeholder="File is empty"
        autoCapitalize="none"
        autoCorrect={false}
      />
      <View style={styles.actions}>
        <Button title="💾 Save File" onPress={onSave} loading={saving} style={{ flex: 1 }} />
        <Button title="✨ AI Review & Fix" onPress={onAiReview} loading={aiBusy} variant="secondary" style={{ flex: 1 }} />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1, backgroundColor: colors.bg, padding: spacing.md },
  centerFlex: { flex: 1, backgroundColor: colors.bg, padding: spacing.md, justifyContent: 'center' },
  toolbar: { marginBottom: spacing.xs },
  title: { color: '#f8fafc', fontWeight: '700', fontSize: 16, marginBottom: spacing.sm },
  editor: {
    flex: 1,
    fontFamily: 'monospace',
    fontSize: 13.5,
    lineHeight: 21,
    color: '#f8fafc',
    backgroundColor: '#070b14',
    borderWidth: 1.5,
    borderColor: '#233047',
    borderRadius: radius.sm,
    padding: 14,
    textAlignVertical: 'top',
  },
  actions: {
    flexDirection: 'row',
    gap: spacing.sm,
    marginTop: spacing.sm,
  },
});

import React, { useState } from 'react';
import { View, Text, StyleSheet, ScrollView } from 'react-native';
import { Card, Button, Banner, HelperText, SectionTitle, Input } from '../../components/UI';
import { AiResponseModal } from '../../components/AiResponseModal';
import * as aiApi from '../../api/ai';
import { useProject } from '../../context/ProjectContext';
import { colors, radius, spacing } from '../../theme/theme';

export default function AiToolsScreen({ navigation }: any) {
  const {
    state,
    refreshState,
    recentAction,
    setRecentAction,
    showActionModal,
    setShowActionModal,
  } = useProject();

  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState<'diagnose' | 'apply' | 'prompt' | null>(null);

  // Command Prompt state
  const [customPrompt, setCustomPrompt] = useState('');
  const [promptResponse, setPromptResponse] = useState('');
  const [promptError, setPromptError] = useState('');

  const fix = state?.ai_fix;

  const onSubmitPrompt = async () => {
    if (!customPrompt.trim()) return;
    const userText = customPrompt.trim();
    setBusy('prompt');
    setPromptError('');
    setPromptResponse('');
    try {
      const res = await aiApi.aiSubmitCustomPrompt(userText);
      const actionMessage =
        res.explanation || res.response || res.ai_response || res.guidance || res.text || res.message || 'Request executed successfully within project context.';

      if (res.status === 'error' || res.out_of_scope) {
        setPromptError(res.message || 'Request is out of scope.');
      } else {
        setRecentAction({
          prompt: userText,
          message: actionMessage,
          timestamp: 'Just now',
        });
        setPromptResponse(actionMessage);
        setCustomPrompt('');
        setShowActionModal(true);
        await refreshState();
      }
    } catch (e: any) {
      setPromptError(e?.message || 'Could not process request.');
    } finally {
      setBusy(null);
    }
  };

  const onDiagnose = async () => {
    setBusy('diagnose');
    setError('');
    setMessage('');
    try {
      const res = await aiApi.aiFixBuildError();
      if (res.status === 'success') {
        await refreshState();
        const msg =
          res.explanation || res.response || res.ai_response || res.guidance || res.text || res.message || 'Diagnosis complete.';
        setMessage(msg);
        setRecentAction({
          prompt: 'Diagnose last build failure',
          message: msg,
          timestamp: 'Just now',
        });
        setShowActionModal(true);
      } else {
        setError(res.message || 'Diagnosis failed.');
      }
    } catch (e: any) {
      setError(e?.message || 'Diagnosis failed.');
    } finally {
      setBusy(null);
    }
  };

  const onApply = async () => {
    setBusy('apply');
    setError('');
    setMessage('');
    try {
      const res = await aiApi.aiApplyFix();
      if (res.status === 'success') {
        await refreshState();
        setMessage(res.message || 'Fix applied.');
      } else {
        setError(res.message || 'Apply failed.');
      }
    } catch (e: any) {
      setError(e?.message || 'Apply failed.');
    } finally {
      setBusy(null);
    }
  };

  return (
    <ScrollView style={styles.flex} contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
      {/* Command Prompt & Custom Requests Input Block */}
      <Card style={styles.promptCard}>
        <Text style={styles.promptTitle}>💻 Command Prompt & Custom Requests</Text>
        <Text style={styles.promptSubtitle}>
          Type your required code modifications or feature requests for this decompiled project context:
        </Text>
        
        {promptError ? <Banner type="error" message={promptError} /> : null}
        {promptResponse ? <Banner type="success" message={promptResponse} /> : null}

        <Input
          value={customPrompt}
          onChangeText={setCustomPrompt}
          multiline
          placeholder="e.g. Add dark theme toggle, modify strings.xml, or update Smali bytecode..."
          style={styles.promptInput}
          autoCapitalize="none"
          autoCorrect={false}
        />

        <Button
          title="Submit Request"
          onPress={onSubmitPrompt}
          loading={busy === 'prompt'}
          disabled={!customPrompt.trim()}
          style={styles.submitBtn}
        />
      </Card>

      {/* Recent Action Steps Trigger */}
      {recentAction ? (
        <Card style={{ borderColor: '#38BDF8', borderWidth: 1 }}>
          <SectionTitle>📋 AI Action Steps & Guidance</SectionTitle>
          <HelperText>
            View the detailed required steps and code changes for recent actions.
          </HelperText>
          <Button
            title="📋 Show Required Steps for Recent Action"
            onPress={() => setShowActionModal(true)}
            variant="secondary"
            textStyle={{ color: '#38BDF8', fontWeight: '800' }}
          />
        </Card>
      ) : null}

      <Card>
        <SectionTitle>AI Build Error Fix</SectionTitle>
        <Banner type="error" message={error} />
        <Banner type="success" message={message} />
        <HelperText>
          After a failed build, tap "Diagnose" — the AI will inspect the build log, locate the
          offending file, and suggest a fix.
        </HelperText>
        <Button
          title="Diagnose last build failure"
          onPress={onDiagnose}
          loading={busy === 'diagnose'}
          disabled={!state?.last_build_failed}
        />
        {!state?.last_build_failed ? (
          <HelperText>No failed build recorded yet for this project.</HelperText>
        ) : null}
      </Card>

      {fix ? (
        <Card>
          <SectionTitle>Suggested fix</SectionTitle>
          <Text style={styles.fixFile}>{fix.file}</Text>
          {fix.explanation ? <Text style={styles.fixExplanation}>{fix.explanation}</Text> : null}
          {fix.has_fix && !fix.applied ? (
            <Button title="Apply this fix" onPress={onApply} loading={busy === 'apply'} />
          ) : fix.applied ? (
            <HelperText>Already applied. Try building again.</HelperText>
          ) : (
            <HelperText>AI found no safe automatic change.</HelperText>
          )}
        </Card>
      ) : null}

      <Card>
        <SectionTitle>Other AI tools</SectionTitle>
        <Button
          title="AI Review a file"
          variant="secondary"
          onPress={() => navigation.navigate('FileBrowser', { dirPath: '' })}
        />
        <Button
          title="AI-generate an app icon"
          variant="secondary"
          onPress={() => navigation.navigate('LogoIcon')}
        />
      </Card>

      <AiResponseModal
        visible={showActionModal}
        onClose={() => setShowActionModal(false)}
        userPrompt={recentAction?.prompt}
        message={recentAction?.message || ''}
        navigation={navigation}
      />
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1, backgroundColor: colors.bg },
  content: { padding: spacing.md, paddingBottom: spacing.xl },
  promptCard: { backgroundColor: '#0F172A', borderColor: '#334155', borderWidth: 1 },
  promptTitle: { color: '#F8FAFC', fontSize: 16, fontWeight: '700', marginBottom: 4 },
  promptSubtitle: { color: '#94A3B8', fontSize: 12, marginBottom: spacing.sm, lineHeight: 16 },
  promptInput: { minHeight: 80, color: '#F8FAFC', backgroundColor: '#1E293B', borderColor: '#334155' },
  submitBtn: { marginTop: spacing.sm, backgroundColor: colors.primary },
  fixFile: { color: colors.text, fontWeight: '700', fontFamily: 'monospace', fontSize: 13 },
  fixExplanation: { color: colors.textMuted, marginTop: 6, marginBottom: spacing.sm, fontSize: 13 },
});

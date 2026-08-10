import React, { useState } from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { Card, Button, Banner, HelperText, SectionTitle } from '../../components/UI';
import { AiResponseModal } from '../../components/AiResponseModal';
import * as aiApi from '../../api/ai';
import { useProject } from '../../context/ProjectContext';
import { colors, spacing } from '../../theme/theme';

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
  const [busy, setBusy] = useState<'diagnose' | 'apply' | null>(null);

  const fix = state?.ai_fix;

  const onDiagnose = async () => {
    setBusy('diagnose');
    setError('');
    setMessage('');
    try {
      const res = await aiApi.aiFixBuildError();
      if (res.status === 'success') {
        await refreshState();
        const msg = res.message || 'Diagnosis complete.';
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
    <View style={styles.flex}>
      {/* Recent Action Steps Trigger */}
      {recentAction ? (
        <Card style={{ borderColor: '#38BDF8', borderWidth: 1 }}>
          <SectionTitle>📋 Action Steps & Guidance</SectionTitle>
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
    </View>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1, backgroundColor: colors.bg, padding: spacing.md },
  fixFile: { color: colors.text, fontWeight: '700', fontFamily: 'monospace', fontSize: 13 },
  fixExplanation: { color: colors.textMuted, marginTop: 6, marginBottom: spacing.sm, fontSize: 13 },
});

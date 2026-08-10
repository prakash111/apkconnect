import React, { useState } from 'react';
import { View, Text, StyleSheet, ScrollView } from 'react-native';
import CodeEditor from '../../components/CodeEditor';
import { Card, Button, Banner, HelperText, SectionTitle, Label } from '../../components/UI';
import * as workflowApi from '../../api/workflow';
import { colors, radius, spacing } from '../../theme/theme';

export default function FirebaseConfigScreen() {
  const [fileName, setFileName] = useState('');
  const [fileUri, setFileUri] = useState('');
  const [jsonText, setJsonText] = useState('');
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(false);
  const [appliedData, setAppliedData] = useState<any>(null);

  const pickFile = async () => {
    setError('');
    try {
      const [result] = await (require('react-native-document-picker')).pick({ type: [(require('react-native-document-picker')).types.allFiles] });
      if (!result.name?.toLowerCase().endsWith('.json')) {
        setError('Please choose a google-services.json file.');
        return;
      }
      setFileName(result.name);
      setFileUri(result.uri);
    } catch (e: any) {
      if (e?.message && !/cancel/i.test(e.message)) setError(e.message);
    }
  };

  const onApply = async () => {
    if (!fileUri && !jsonText.trim()) {
      setError('Choose a google-services.json file or paste JSON content.');
      return;
    }
    setLoading(true);
    setError('');
    setMessage('');
    setAppliedData(null);
    try {
      let uploadFileDescriptor = {
        uri: fileUri,
        name: fileName || 'google-services.json',
        type: 'application/json',
      };

      if (!fileUri && jsonText.trim()) {
        const RNFS = require('react-native-fs');
        const path = `${RNFS.CachesDirectoryPath}/google-services.json`;
        await RNFS.writeFile(path, jsonText, 'utf8');
        uploadFileDescriptor = {
          uri: `file://${path}`,
          name: 'google-services.json',
          type: 'application/json',
        };
      }

      const res = await workflowApi.applyFirebaseConfig(uploadFileDescriptor);
      if (res.status === 'success') {
        setMessage(res.message || 'Firebase values applied successfully.');
        setAppliedData({
          packageName: res.package_name,
          projectId: res.project_id,
          appId: res.app_id,
          apiKey: res.api_key ? (res.api_key.substring(0, 6) + '...' + res.api_key.substring(res.api_key.length - 4)) : '',
          updatesCount: res.updated_keys_count || (res.updates ? Object.keys(res.updates).length : 0),
          updates: res.updates || {},
        });
      } else {
        setError(res.message || 'Apply failed.');
      }
    } catch (e: any) {
      setError(e?.message || 'Apply failed.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <ScrollView style={styles.flex} contentContainerStyle={styles.content}>
      <Card>
        <SectionTitle>Firebase Config</SectionTitle>
        <Banner type="error" message={error} />
        <Banner type="success" message={message} />
        <HelperText>
          Upload your app's google-services.json or edit the JSON configuration below. Matching values (API keys, app IDs, project
          numbers) will be written into the project's strings.xml.
        </HelperText>
        <Button title={fileName || 'Choose google-services.json'} variant="secondary" onPress={pickFile} />
        
        <Label style={{ marginTop: spacing.sm }}>or Paste/Edit JSON Code:</Label>
        <CodeEditor
          value={jsonText}
          onChangeText={setJsonText}
          language="json"
          placeholder={`{\n  "project_info": {\n    "project_number": "123456789",\n    "project_id": "my-app"\n  }\n}`}
          style={{ height: 180, marginVertical: spacing.xs }}
        />

        <Button title="Apply to project" onPress={onApply} loading={loading} disabled={!fileUri && !jsonText.trim()} />
      </Card>

      {appliedData && (
        <Card style={{ marginTop: spacing.sm }}>
          <SectionTitle>Applied Firebase Details</SectionTitle>
          {appliedData.packageName ? (
            <View style={styles.detailRow}>
              <Text style={styles.detailLabel}>Package Name:</Text>
              <Text style={styles.detailVal}>{appliedData.packageName}</Text>
            </View>
          ) : null}
          {appliedData.projectId ? (
            <View style={styles.detailRow}>
              <Text style={styles.detailLabel}>Project ID:</Text>
              <Text style={styles.detailVal}>{appliedData.projectId}</Text>
            </View>
          ) : null}
          {appliedData.appId ? (
            <View style={styles.detailRow}>
              <Text style={styles.detailLabel}>App ID:</Text>
              <Text style={styles.detailVal}>{appliedData.appId}</Text>
            </View>
          ) : null}
          {appliedData.apiKey ? (
            <View style={styles.detailRow}>
              <Text style={styles.detailLabel}>API Key:</Text>
              <Text style={styles.detailVal}>{appliedData.apiKey}</Text>
            </View>
          ) : null}
          <View style={styles.detailRow}>
            <Text style={styles.detailLabel}>Updated Strings:</Text>
            <Text style={styles.detailValHighlight}>{appliedData.updatesCount} key(s) auto-filled</Text>
          </View>

          {appliedData.updates && Object.keys(appliedData.updates).length > 0 && (
            <View style={styles.updatesBox}>
              <Text style={styles.updatesTitle}>Extracted Key/Value Pairs:</Text>
              {Object.entries(appliedData.updates).map(([k, v]: [string, any]) => (
                <View key={k} style={styles.kvRow}>
                  <Text style={styles.kKey}>{k}:</Text>
                  <Text style={styles.kVal} numberOfLines={1}>{String(v)}</Text>
                </View>
              ))}
            </View>
          )}
        </Card>
      )}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1, backgroundColor: colors.bg },
  content: { padding: spacing.md },
  detailRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: 4,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
  },
  detailLabel: { color: colors.textMuted, fontSize: 12 },
  detailVal: { color: colors.text, fontWeight: '700', fontSize: 13, fontFamily: 'monospace' },
  detailValHighlight: { color: colors.success, fontWeight: '800', fontSize: 13 },
  updatesBox: { marginTop: spacing.sm, backgroundColor: colors.surfaceAlt, padding: spacing.xs, borderRadius: radius.xs },
  updatesTitle: { color: colors.text, fontWeight: '700', fontSize: 12, marginBottom: 4 },
  kvRow: { flexDirection: 'row', alignItems: 'center', paddingVertical: 2 },
  kKey: { color: colors.primary, fontSize: 11, fontFamily: 'monospace', fontWeight: '700', width: 140 },
  kVal: { color: colors.textSubtle, fontSize: 11, fontFamily: 'monospace', flex: 1 },
});

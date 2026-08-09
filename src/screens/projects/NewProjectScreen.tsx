import React, { useState } from 'react';
import { View, Text, StyleSheet, Alert } from 'react-native';
import { Screen, Card, SectionTitle, Button, Banner, HelperText } from '../../components/UI';
import * as projectsApi from '../../api/projects';
import { useProject } from '../../context/ProjectContext';
import { colors, spacing } from '../../theme/theme';

export default function NewProjectScreen({ navigation }: any) {
  const [fileName, setFileName] = useState('');
  const [fileUri, setFileUri] = useState('');
  const [fileType, setFileType] = useState('application/vnd.android.package-archive');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { refreshState } = useProject();

  const pickApk = async () => {
    setError('');
    try {
      const [result] = await (require('react-native-document-picker')).pick({ type: [(require('react-native-document-picker')).types.allFiles] });
      if (!result.name?.toLowerCase().endsWith('.apk')) {
        setError('Please choose a .apk file.');
        return;
      }
      setFileName(result.name);
      setFileUri(result.uri);
      setFileType(result.type || 'application/vnd.android.package-archive');
    } catch (e: any) {
      if (e?.message && !/cancel/i.test(e.message)) setError(e.message);
    }
  };

  const onDecompile = async () => {
    if (!fileUri) {
      setError('Choose an APK file first.');
      return;
    }
    setLoading(true);
    setError('');
    try {
      const res = await projectsApi.uploadAndDecompile({
        uri: fileUri,
        name: fileName,
        type: fileType,
      });
      if (res.status === 'success') {
        await refreshState();
        const successMsg = res.message || 'APK uploaded and decompiled successfully.';
        Alert.alert(
          'Decompiled Successfully! 🎉',
          successMsg,
          [
            {
              text: 'Open Project',
              onPress: () => {
                navigation.replace('WorkflowTab', {
                  screen: 'WorkflowHome',
                  params: { message: successMsg }
                });
              }
            }
          ],
          { cancelable: false }
        );
      } else {
        setError(res.message || 'Decompile failed.');
      }
    } catch (e: any) {
      setError(e?.message || 'Decompile failed.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Screen>
      <Card>
        <SectionTitle>New project</SectionTitle>
        <Banner type="error" message={error} />
        <HelperText>
          Pick an APK from your device. It will be uploaded to your server and decompiled with
          apktool, the same as the web version.
        </HelperText>
        <Button title={fileName ? 'Change APK file' : 'Choose APK file'} variant="secondary" onPress={pickApk} />
        {fileName ? (
          <View style={styles.filePreview}>
            <Text style={styles.fileName}>{fileName}</Text>
          </View>
        ) : null}
        <Button title="Upload & Decompile" onPress={onDecompile} loading={loading} disabled={!fileUri} />
      </Card>
    </Screen>
  );
}

const styles = StyleSheet.create({
  filePreview: {
    backgroundColor: colors.surfaceAlt,
    borderRadius: 8,
    padding: spacing.sm,
    marginTop: spacing.sm,
  },
  fileName: { color: colors.text },
});

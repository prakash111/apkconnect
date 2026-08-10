import React, { useState } from 'react';
import { View, Text, StyleSheet, Modal, Pressable } from 'react-native';
import { Screen, Card, SectionTitle, Button, Banner, HelperText } from '../../components/UI';
import * as projectsApi from '../../api/projects';
import { useProject } from '../../context/ProjectContext';
import { colors, radius, spacing } from '../../theme/theme';

export default function NewProjectScreen({ navigation }: any) {
  const [fileName, setFileName] = useState('');
  const [fileUri, setFileUri] = useState('');
  const [fileType, setFileType] = useState('application/vnd.android.package-archive');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [successModalVisible, setSuccessModalVisible] = useState(false);
  const [successMsg, setSuccessMsg] = useState('');
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
        const msg = res.message || 'APK uploaded and decompiled successfully.';
        setSuccessMsg(msg);
        setSuccessModalVisible(true);
      } else {
        setError(res.message || 'Decompile failed.');
      }
    } catch (e: any) {
      setError(e?.message || 'Decompile failed.');
    } finally {
      setLoading(false);
    }
  };

  const handleOpenProject = () => {
    setSuccessModalVisible(false);
    // Route to Studio/Workflow tab and its home screen
    if (navigation.getParent()) {
      navigation.getParent().navigate('WorkflowTab', {
        screen: 'WorkflowHome',
        params: { message: successMsg }
      });
    } else {
      navigation.navigate('WorkflowTab', {
        screen: 'WorkflowHome',
        params: { message: successMsg }
      });
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

      {/* Decompiled Successfully Modal */}
      <Modal
        animationType="fade"
        transparent={true}
        visible={successModalVisible}
        onRequestClose={() => setSuccessModalVisible(false)}>
        <View style={styles.modalOverlay}>
          <View style={styles.modalContainer}>
            <Text style={styles.modalTitle}>Decompiled Successfully! 🎉</Text>
            <Text style={styles.modalMessage}>{successMsg}</Text>
            
            <Button
              title="OPEN PROJECT"
              onPress={handleOpenProject}
              style={styles.openProjectBtn}
            />

            {/* UX Enhancement Hint */}
            <Text style={styles.uxHint}>
              💡 For a better editor experience, please use the web browser.
            </Text>
          </View>
        </View>
      </Modal>
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
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.75)',
    justifyContent: 'center',
    alignItems: 'center',
    padding: spacing.md,
  },
  modalContainer: {
    width: '90%',
    maxWidth: 400,
    backgroundColor: colors.surface,
    borderRadius: radius.md,
    borderColor: colors.border,
    borderWidth: 1,
    padding: spacing.lg,
    alignItems: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 8,
    elevation: 10,
  },
  modalTitle: {
    color: colors.text,
    fontSize: 20,
    fontWeight: '800',
    textAlign: 'center',
    marginBottom: spacing.xs,
  },
  modalMessage: {
    color: colors.textMuted,
    fontSize: 14,
    textAlign: 'center',
    marginBottom: spacing.md,
  },
  openProjectBtn: {
    width: '100%',
    backgroundColor: colors.primary,
    marginTop: spacing.xs,
  },
  uxHint: {
    color: '#94A3B8',
    fontSize: 13,
    textAlign: 'center',
    marginTop: 14,
    lineHeight: 18,
  },
});


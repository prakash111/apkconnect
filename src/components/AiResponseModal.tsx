import React, { useState } from 'react';
import {
  Modal,
  View,
  Text,
  StyleSheet,
  ScrollView,
  Pressable,
  Clipboard,
} from 'react-native';
import { colors, radius, spacing } from '../theme/theme';

export interface ActionStep {
  id: number;
  title: string;
  filePath?: string;
  description: string;
  codeSnippet?: string;
}

export interface AiResponseModalProps {
  visible: boolean;
  onClose: () => void;
  title?: string;
  userPrompt?: string;
  message: string;
  navigation?: any;
}

/**
 * Parses raw text/markdown into structured steps, file paths, and code snippets.
 */
function parseMessageToSteps(message: string): { steps: ActionStep[]; filePaths: string[] } {
  if (!message) return { steps: [], filePaths: [] };

  const steps: ActionStep[] = [];
  const filePathsSet = new Set<string>();

  // Extract explicit file path references (e.g. res/values/strings.xml, AndroidManifest.xml)
  const pathRegex = /`?([a-zA-Z0-9_\-\.\/]+\.(xml|java|smali|kt|json|gradle|txt|properties))`?/g;
  let pathMatch;
  while ((pathMatch = pathRegex.exec(message)) !== null) {
    if (pathMatch[1] && !pathMatch[1].includes('http')) {
      filePathsSet.add(pathMatch[1]);
    }
  }

  // Split into sentences / blocks to construct steps
  const codeBlockRegex = /```(?:xml|java|smali|json|kotlin|groovy)?\n?([\s\S]*?)```/g;
  const codeBlocks: string[] = [];
  let codeMatch;
  while ((codeMatch = codeBlockRegex.exec(message)) !== null) {
    codeBlocks.push(codeMatch[1].trim());
  }

  // Split content by sentences or newlines
  const paragraphs = message
    .replace(/```[\s\S]*?```/g, ' [CODE_BLOCK] ')
    .split(/\n\n|\.\s+/)
    .map(p => p.trim())
    .filter(p => p.length > 0);

  let stepId = 1;
  let codeIndex = 0;

  paragraphs.forEach(p => {
    // Check if this paragraph contains a code placeholder
    const hasCode = p.includes('[CODE_BLOCK]');
    const cleanText = p.replace(/\[CODE_BLOCK\]/g, '').trim();

    // Check for associated file paths in this paragraph
    let fileForStep: string | undefined = undefined;
    filePathsSet.forEach(fp => {
      if (p.includes(fp) && !fileForStep) {
        fileForStep = fp;
      }
    });

    if (cleanText || hasCode) {
      const snippet = hasCode && codeBlocks[codeIndex] ? codeBlocks[codeIndex] : undefined;
      if (hasCode && codeBlocks[codeIndex]) {
        codeIndex++;
      }

      // Generate a title based on context
      let title = `Required Action ${stepId}`;
      if (fileForStep) {
        title = `Modify ${fileForStep}`;
      } else if (cleanText.toLowerCase().includes('manifest')) {
        title = 'Update AndroidManifest.xml';
      } else if (cleanText.toLowerCase().includes('string') || cleanText.toLowerCase().includes('name')) {
        title = 'Update Application Name & Strings';
      } else if (cleanText.toLowerCase().includes('no change') || cleanText.toLowerCase().includes('not a specific')) {
        title = 'Verification & Note';
      }

      steps.push({
        id: stepId++,
        title,
        filePath: fileForStep,
        description: cleanText || 'Apply the following code snippet:',
        codeSnippet: snippet,
      });
    }
  });

  // Fallback if no steps parsed cleanly
  if (steps.length === 0) {
    steps.push({
      id: 1,
      title: 'Action Guidance',
      description: message,
      codeSnippet: codeBlocks[0],
    });
  }

  return { steps, filePaths: Array.from(filePathsSet) };
}

export function AiResponseModal({
  visible,
  onClose,
  title = 'Action Steps & Required Guidance',
  userPrompt,
  message,
  navigation,
}: AiResponseModalProps) {
  const [copiedIndex, setCopiedIndex] = useState<number | null>(null);

  const { steps, filePaths } = parseMessageToSteps(message);

  const handleCopy = (text: string, index: number) => {
    Clipboard.setString(text);
    setCopiedIndex(index);
    setTimeout(() => setCopiedIndex(null), 2000);
  };

  const handleNavigateTool = (filePath?: string) => {
    onClose();
    if (!navigation) return;

    if (filePath && (filePath.includes('strings') || filePath.includes('values'))) {
      navigation.navigate('StringsEditor');
    } else if (filePath) {
      navigation.navigate('FileEditor', { filePath });
    } else {
      navigation.navigate('FileBrowser', { dirPath: '' });
    }
  };

  return (
    <Modal
      visible={visible}
      transparent
      animationType="slide"
      onRequestClose={onClose}>
      <View style={styles.backdrop}>
        <View style={styles.modalCard}>
          {/* Modal Header */}
          <View style={styles.header}>
            <View style={styles.headerLeft}>
              <Text style={styles.headerBadge}>💡 AI ACTION STEPS</Text>
              <Text style={styles.headerTitle}>{title}</Text>
            </View>
            <Pressable style={styles.closeBtn} onPress={onClose}>
              <Text style={styles.closeBtnText}>✕</Text>
            </Pressable>
          </View>

          <ScrollView style={styles.body} contentContainerStyle={styles.bodyContent}>
            {/* User Requested Action Context */}
            {userPrompt ? (
              <View style={styles.userPromptCard}>
                <Text style={styles.userPromptLabel}>WHAT YOU ASKED:</Text>
                <Text style={styles.userPromptText}>{userPrompt}</Text>
              </View>
            ) : null}

            {/* Overview Banner */}
            <View style={styles.summaryBox}>
              <Text style={styles.summaryTitle}>📋 Action Breakdown & Steps</Text>
              <Text style={styles.summaryText}>
                Follow the step-by-step instructions below to complete the requested changes across your decompiled project.
              </Text>
            </View>

            {/* Parsed Steps List */}
            {steps.map((step, idx) => (
              <View key={step.id} style={styles.stepCard}>
                <View style={styles.stepHeader}>
                  <View style={styles.stepBadge}>
                    <Text style={styles.stepBadgeText}>Step {step.id}</Text>
                  </View>
                  <Text style={styles.stepTitle}>{step.title}</Text>
                </View>

                {step.filePath ? (
                  <View style={styles.fileTag}>
                    <Text style={styles.fileTagIcon}>📄</Text>
                    <Text style={styles.fileTagText}>{step.filePath}</Text>
                  </View>
                ) : null}

                <Text style={styles.stepDesc}>{step.description}</Text>

                {/* Code Snippet Card */}
                {step.codeSnippet ? (
                  <View style={styles.codeContainer}>
                    <View style={styles.codeHeader}>
                      <Text style={styles.codeLang}>XML / CODE</Text>
                      <Pressable
                        style={styles.copyBtn}
                        onPress={() => handleCopy(step.codeSnippet!, idx)}>
                        <Text style={styles.copyBtnText}>
                          {copiedIndex === idx ? '✓ Copied' : '📋 Copy Code'}
                        </Text>
                      </Pressable>
                    </View>
                    <ScrollView horizontal showsHorizontalScrollIndicator={false}>
                      <Text style={styles.codeText}>{step.codeSnippet}</Text>
                    </ScrollView>
                  </View>
                ) : null}
              </View>
            ))}

            {/* Raw Message Accordion / Full Content View */}
            <View style={styles.rawBox}>
              <Text style={styles.rawTitle}>Full Response Text</Text>
              <Text style={styles.rawText}>{message}</Text>
            </View>
          </ScrollView>

          {/* Modal Footer Quick Actions */}
          <View style={styles.footer}>
            <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.footerActions}>
              {filePaths.some(p => p.includes('strings')) ? (
                <Pressable
                  style={[styles.actionBtn, styles.actionBtnPrimary]}
                  onPress={() => handleNavigateTool('res/values/strings.xml')}>
                  <Text style={styles.actionBtnTextPrimary}>🔤 Open Strings Editor</Text>
                </Pressable>
              ) : null}

              <Pressable
                style={styles.actionBtn}
                onPress={() => handleNavigateTool()}>
                <Text style={styles.actionBtnText}>📁 Browse Files</Text>
              </Pressable>

              <Pressable
                style={[styles.actionBtn, styles.actionBtnClose]}
                onPress={onClose}>
                <Text style={styles.actionBtnTextClose}>Got it / Close</Text>
              </Pressable>
            </ScrollView>
          </View>
        </View>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  backdrop: {
    flex: 1,
    backgroundColor: 'rgba(7, 13, 25, 0.85)',
    justifyContent: 'flex-end',
  },
  modalCard: {
    backgroundColor: '#0F172A',
    borderTopLeftRadius: radius.md * 1.5,
    borderTopRightRadius: radius.md * 1.5,
    maxHeight: '90%',
    minHeight: '60%',
    borderWidth: 1,
    borderColor: '#334155',
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm + 4,
    borderBottomWidth: 1,
    borderBottomColor: '#1E293B',
    backgroundColor: '#1E293B',
    borderTopLeftRadius: radius.md * 1.5,
    borderTopRightRadius: radius.md * 1.5,
  },
  headerLeft: {
    flex: 1,
  },
  headerBadge: {
    color: '#38BDF8',
    fontSize: 10,
    fontWeight: '800',
    letterSpacing: 1,
    marginBottom: 2,
  },
  headerTitle: {
    color: '#F8FAFC',
    fontSize: 16,
    fontWeight: '700',
  },
  closeBtn: {
    width: 32,
    height: 32,
    borderRadius: 16,
    backgroundColor: '#334155',
    alignItems: 'center',
    justifyContent: 'center',
    marginLeft: spacing.sm,
  },
  closeBtnText: {
    color: '#94A3B8',
    fontSize: 16,
    fontWeight: '700',
  },
  body: {
    flex: 1,
  },
  bodyContent: {
    padding: spacing.md,
    paddingBottom: spacing.xl,
  },
  userPromptCard: {
    backgroundColor: '#1E293B',
    borderRadius: radius.sm,
    padding: spacing.sm + 2,
    borderLeftWidth: 4,
    borderLeftColor: '#38BDF8',
    marginBottom: spacing.md,
  },
  userPromptLabel: {
    color: '#38BDF8',
    fontSize: 10,
    fontWeight: '800',
    letterSpacing: 0.8,
    marginBottom: 4,
  },
  userPromptText: {
    color: '#E2E8F0',
    fontSize: 13,
    fontWeight: '600',
  },
  summaryBox: {
    backgroundColor: '#1E293B',
    borderRadius: radius.sm,
    padding: spacing.sm + 4,
    marginBottom: spacing.md,
    borderWidth: 1,
    borderColor: '#334155',
  },
  summaryTitle: {
    color: '#F8FAFC',
    fontSize: 14,
    fontWeight: '700',
    marginBottom: 4,
  },
  summaryText: {
    color: '#94A3B8',
    fontSize: 12,
    lineHeight: 18,
  },
  stepCard: {
    backgroundColor: '#1E293B',
    borderRadius: radius.md,
    padding: spacing.md,
    marginBottom: spacing.md,
    borderWidth: 1,
    borderColor: '#334155',
  },
  stepHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: spacing.xs,
  },
  stepBadge: {
    backgroundColor: '#0284C7',
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: 12,
    marginRight: spacing.xs,
  },
  stepBadgeText: {
    color: '#FFFFFF',
    fontSize: 11,
    fontWeight: '800',
  },
  stepTitle: {
    color: '#F8FAFC',
    fontSize: 14,
    fontWeight: '700',
    flex: 1,
  },
  fileTag: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#0F172A',
    alignSelf: 'flex-start',
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: radius.sm,
    marginVertical: 6,
    borderWidth: 1,
    borderColor: '#334155',
  },
  fileTagIcon: {
    fontSize: 12,
    marginRight: 4,
  },
  fileTagText: {
    color: '#38BDF8',
    fontSize: 11,
    fontFamily: 'monospace',
    fontWeight: '600',
  },
  stepDesc: {
    color: '#CBD5E1',
    fontSize: 13,
    lineHeight: 20,
    marginTop: 4,
  },
  codeContainer: {
    backgroundColor: '#070D19',
    borderRadius: radius.sm,
    borderWidth: 1,
    borderColor: '#334155',
    marginTop: spacing.sm,
    overflow: 'hidden',
  },
  codeHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    backgroundColor: '#0F172A',
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderBottomWidth: 1,
    borderBottomColor: '#1E293B',
  },
  codeLang: {
    color: '#64748B',
    fontSize: 10,
    fontWeight: '800',
    letterSpacing: 0.5,
  },
  copyBtn: {
    backgroundColor: '#1E293B',
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 4,
  },
  copyBtnText: {
    color: '#38BDF8',
    fontSize: 11,
    fontWeight: '700',
  },
  codeText: {
    color: '#A5F3FC',
    fontFamily: 'monospace',
    fontSize: 12,
    padding: 10,
    lineHeight: 18,
  },
  rawBox: {
    marginTop: spacing.sm,
    padding: spacing.sm,
    backgroundColor: '#0F172A',
    borderRadius: radius.sm,
    borderWidth: 1,
    borderColor: '#1E293B',
  },
  rawTitle: {
    color: '#64748B',
    fontSize: 11,
    fontWeight: '700',
    marginBottom: 4,
  },
  rawText: {
    color: '#94A3B8',
    fontSize: 11,
    lineHeight: 16,
  },
  footer: {
    padding: spacing.md,
    borderTopWidth: 1,
    borderTopColor: '#1E293B',
    backgroundColor: '#0F172A',
  },
  footerActions: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
  },
  actionBtn: {
    backgroundColor: '#1E293B',
    paddingHorizontal: 16,
    paddingVertical: 10,
    borderRadius: radius.sm,
    borderWidth: 1,
    borderColor: '#334155',
  },
  actionBtnPrimary: {
    backgroundColor: colors.primary,
    borderColor: colors.primary,
  },
  actionBtnClose: {
    backgroundColor: '#334155',
  },
  actionBtnText: {
    color: '#E2E8F0',
    fontWeight: '700',
    fontSize: 13,
  },
  actionBtnTextPrimary: {
    color: '#FFFFFF',
    fontWeight: '700',
    fontSize: 13,
  },
  actionBtnTextClose: {
    color: '#94A3B8',
    fontWeight: '700',
    fontSize: 13,
  },
});

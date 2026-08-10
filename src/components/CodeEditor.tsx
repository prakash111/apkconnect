import React, { useState, useEffect, useRef } from 'react';
import {
  View,
  Text,
  TextInput,
  StyleSheet,
  ScrollView,
  Pressable,
  Platform,
} from 'react-native';
import { colors, radius, spacing } from '../theme/theme';

interface CodeEditorProps {
  value: string;
  onChangeText: (text: string) => void;
  filePath?: string;
  placeholder?: string;
  editable?: boolean;
}

export function CodeEditor({
  value,
  onChangeText,
  filePath = '',
  placeholder = 'Type code here…',
  editable = true,
}: CodeEditorProps) {
  // --- History Stack for Undo/Redo ---
  const [history, setHistory] = useState<string[]>([value || '']);
  const [historyIndex, setHistoryIndex] = useState<number>(0);
  const isInternalChange = useRef(false);

  // Synchronize initial or external value changes
  useEffect(() => {
    if (!isInternalChange.current) {
      setHistory([value || '']);
      setHistoryIndex(0);
    }
    isInternalChange.current = false;
  }, [filePath]);

  const handleTextChange = (newText: string) => {
    isInternalChange.current = true;
    onChangeText(newText);

    // Debounce or record history step
    const newHistory = history.slice(0, historyIndex + 1);
    if (newHistory[newHistory.length - 1] !== newText) {
      newHistory.push(newText);
      // Limit history size to 50 entries
      if (newHistory.length > 50) newHistory.shift();
      setHistory(newHistory);
      setHistoryIndex(newHistory.length - 1);
    }
  };

  const canUndo = historyIndex > 0;
  const canRedo = historyIndex < history.length - 1;

  const onUndo = () => {
    if (canUndo) {
      const prevText = history[historyIndex - 1];
      setHistoryIndex(historyIndex - 1);
      isInternalChange.current = true;
      onChangeText(prevText);
    }
  };

  const onRedo = () => {
    if (canRedo) {
      const nextText = history[historyIndex + 1];
      setHistoryIndex(historyIndex + 1);
      isInternalChange.current = true;
      onChangeText(nextText);
    }
  };

  // --- File Type & Language Mode Detection ---
  const ext = filePath.split('.').pop()?.toLowerCase() || '';
  let langLabel = 'Plain Text';
  if (ext === 'xml') langLabel = 'XML';
  else if (ext === 'json') langLabel = 'JSON';
  else if (ext === 'smali') langLabel = 'Smali';
  else if (ext === 'java' || ext === 'kt') langLabel = 'Java/Kotlin';
  else if (ext === 'gradle') langLabel = 'Gradle';

  // --- Line Numbers ---
  const lines = value ? value.split('\n') : [''];
  const lineCount = lines.length;
  const lineNumbers = Array.from({ length: lineCount }, (_, i) => i + 1);

  // --- View Modes: Edit Mode vs Syntax Highlighted Preview ---
  const [viewMode, setViewMode] = useState<'edit' | 'highlight'>('edit');

  // --- Simple Tokenizer / Highlighted Line Renderer ---
  const renderHighlightedLine = (line: string, index: number) => {
    if (ext === 'xml') {
      // Basic XML syntax highlighting
      const tagMatch = line.match(/^(\s*)(<\/?[a-zA-Z0-9_-]+)(.*?)(\/?>)(.*)$/);
      if (tagMatch) {
        const [, indent, tag, attrs, close, rest] = tagMatch;
        return (
          <Text key={index} style={styles.codeLineText}>
            <Text style={styles.codeIndent}>{indent}</Text>
            <Text style={styles.synTag}>{tag}</Text>
            <Text style={styles.synAttr}>{attrs}</Text>
            <Text style={styles.synTag}>{close}</Text>
            <Text style={styles.synText}>{rest}</Text>
          </Text>
        );
      }
    } else if (ext === 'json') {
      // Basic JSON syntax highlighting
      const kvMatch = line.match(/^(\s*)(".*?")(\s*:\s*)(.*)$/);
      if (kvMatch) {
        const [, indent, key, colon, val] = kvMatch;
        return (
          <Text key={index} style={styles.codeLineText}>
            <Text style={styles.codeIndent}>{indent}</Text>
            <Text style={styles.synJsonKey}>{key}</Text>
            <Text style={styles.synColon}>{colon}</Text>
            <Text style={styles.synJsonVal}>{val}</Text>
          </Text>
        );
      }
    } else if (ext === 'smali') {
      // Basic Smali syntax highlighting
      const smaliMatch = line.match(/^(\s*)(\.[a-zA-Z_-]+|const-[a-z0-9/]+|invoke-[a-z0-9/]+|return-[a-z0-9]+|move-[a-z0-9]+|goto|if-[a-z0-9]+)(.*)$/);
      if (smaliMatch) {
        const [, indent, op, rest] = smaliMatch;
        return (
          <Text key={index} style={styles.codeLineText}>
            <Text style={styles.codeIndent}>{indent}</Text>
            <Text style={styles.synSmaliOp}>{op}</Text>
            <Text style={styles.synSmaliReg}>{rest}</Text>
          </Text>
        );
      }
    }

    return (
      <Text key={index} style={styles.codeLineText}>
        {line || ' '}
      </Text>
    );
  };

  return (
    <View style={styles.container}>
      {/* Editor Control Toolbar (Undo, Redo, Mode, Language & Line Badge) */}
      <View style={styles.toolbar}>
        <View style={styles.toolbarLeft}>
          <Pressable
            onPress={onUndo}
            disabled={!canUndo}
            style={[styles.toolBtn, !canUndo && styles.toolBtnDisabled]}>
            <Text style={[styles.toolBtnText, !canUndo && styles.toolBtnTextDisabled]}>
              ↩️ Undo
            </Text>
          </Pressable>

          <Pressable
            onPress={onRedo}
            disabled={!canRedo}
            style={[styles.toolBtn, !canRedo && styles.toolBtnDisabled]}>
            <Text style={[styles.toolBtnText, !canRedo && styles.toolBtnTextDisabled]}>
              ↪️ Redo
            </Text>
          </Pressable>

          <Pressable
            onPress={() => setViewMode(viewMode === 'edit' ? 'highlight' : 'edit')}
            style={[styles.toolBtn, styles.toolBtnActive]}>
            <Text style={styles.toolBtnTextActive}>
              {viewMode === 'edit' ? '👁️ Highlight Mode' : '✏️ Edit Mode'}
            </Text>
          </Pressable>
        </View>

        <View style={styles.toolbarRight}>
          <View style={styles.badge}>
            <Text style={styles.badgeText}>{langLabel}</Text>
          </View>
          <View style={styles.badgeLines}>
            <Text style={styles.badgeLinesText}>{lineCount} L</Text>
          </View>
        </View>
      </View>

      {/* Editor Main Layout (Gutter + Content) */}
      <View style={styles.editorBody}>
        {/* Left Gutter: Line Numbers */}
        <ScrollView
          style={styles.gutter}
          showsVerticalScrollIndicator={false}
          scrollEnabled={false}>
          {lineNumbers.map(n => (
            <Text key={n} style={styles.gutterText}>
              {n}
            </Text>
          ))}
        </ScrollView>

        {/* Right Pane: Code Area */}
        <View style={styles.codePane}>
          {viewMode === 'edit' ? (
            <TextInput
              value={value}
              onChangeText={handleTextChange}
              multiline
              editable={editable}
              placeholder={placeholder}
              placeholderTextColor="#64748B"
              autoCapitalize="none"
              autoCorrect={false}
              spellCheck={false}
              style={styles.textInput}
            />
          ) : (
            <ScrollView style={styles.highlightScrollView}>
              {lines.map((line, idx) => renderHighlightedLine(line, idx))}
            </ScrollView>
          )}
        </View>
      </View>
    </View>
}

export default CodeEditor;

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#070D19',
    borderColor: '#233047',
    borderWidth: 1.5,
    borderRadius: radius.sm,
    overflow: 'hidden',
  },
  toolbar: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: '#0F172A',
    borderBottomWidth: 1,
    borderBottomColor: '#1E293B',
    paddingHorizontal: spacing.xs,
    paddingVertical: 6,
  },
  toolbarLeft: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  toolbarRight: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  toolBtn: {
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: radius.xs,
    backgroundColor: '#1E293B',
    borderWidth: 1,
    borderColor: '#334155',
  },
  toolBtnDisabled: {
    opacity: 0.4,
  },
  toolBtnActive: {
    backgroundColor: 'rgba(99, 102, 241, 0.2)',
    borderColor: '#6366F1',
  },
  toolBtnText: {
    color: '#E2E8F0',
    fontSize: 11,
    fontWeight: '600',
  },
  toolBtnTextDisabled: {
    color: '#64748B',
  },
  toolBtnTextActive: {
    color: '#818CF8',
    fontSize: 11,
    fontWeight: '700',
  },
  badge: {
    backgroundColor: '#1E293B',
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: 4,
    borderWidth: 1,
    borderColor: '#334155',
  },
  badgeText: {
    color: '#38BDF8',
    fontSize: 10,
    fontWeight: '700',
    fontFamily: Platform.OS === 'ios' ? 'Menlo' : 'monospace',
  },
  badgeLines: {
    backgroundColor: 'rgba(245, 158, 11, 0.15)',
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: 4,
  },
  badgeLinesText: {
    color: '#F59E0B',
    fontSize: 10,
    fontWeight: '700',
    fontFamily: Platform.OS === 'ios' ? 'Menlo' : 'monospace',
  },
  editorBody: {
    flex: 1,
    flexDirection: 'row',
  },
  gutter: {
    width: 42,
    backgroundColor: '#0B1120',
    borderRightWidth: 1,
    borderRightColor: '#1E293B',
    paddingVertical: 10,
    paddingHorizontal: 4,
  },
  gutterText: {
    color: '#64748B',
    fontFamily: Platform.OS === 'ios' ? 'Menlo' : 'monospace',
    fontSize: 12.5,
    lineHeight: 20,
    textAlign: 'right',
  },
  codePane: {
    flex: 1,
    backgroundColor: '#070D19',
  },
  textInput: {
    flex: 1,
    fontFamily: Platform.OS === 'ios' ? 'Menlo' : 'monospace',
    fontSize: 13,
    lineHeight: 20,
    color: '#F8FAFC',
    paddingHorizontal: 10,
    paddingVertical: 10,
    textAlignVertical: 'top',
  },
  highlightScrollView: {
    flex: 1,
    paddingHorizontal: 10,
    paddingVertical: 10,
  },
  codeLineText: {
    fontFamily: Platform.OS === 'ios' ? 'Menlo' : 'monospace',
    fontSize: 13,
    lineHeight: 20,
    color: '#F8FAFC',
  },
  codeIndent: {
    color: '#334155',
  },
  synTag: {
    color: '#C084FC', // XML tag purple
    fontWeight: '700',
  },
  synAttr: {
    color: '#38BDF8', // XML attr cyan
  },
  synText: {
    color: '#F8FAFC',
  },
  synJsonKey: {
    color: '#F59E0B', // JSON key amber
    fontWeight: '700',
  },
  synColon: {
    color: '#94A3B8',
  },
  synJsonVal: {
    color: '#4ADE80', // JSON value green
  },
  synSmaliOp: {
    color: '#F43F5E', // Smali opcode pink
    fontWeight: '700',
  },
  synSmaliReg: {
    color: '#60A5FA', // Smali register blue
  },
});

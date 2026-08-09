import React from 'react';
import {
  ActivityIndicator,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  TextInputProps,
  View,
} from 'react-native';
import { colors, radius, spacing } from '../theme/theme';

export function Screen({
  children,
  scroll = true,
}: {
  children: React.ReactNode;
  scroll?: boolean;
}) {
  const Wrapper: any = scroll ? ScrollView : View;
  return (
    <KeyboardAvoidingView
      style={styles.flex}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <Wrapper
        style={styles.flex}
        contentContainerStyle={scroll ? styles.scrollContent : styles.flex}
        keyboardShouldPersistTaps="handled">
        {children}
      </Wrapper>
    </KeyboardAvoidingView>
  );
}

export function Card({ children, style }: { children: React.ReactNode; style?: any }) {
  return <View style={[styles.card, style]}>{children}</View>;
}

export function SectionTitle({ children }: { children: React.ReactNode }) {
  return <Text style={styles.sectionTitle}>{children}</Text>;
}

export function Label({ children }: { children: React.ReactNode }) {
  return <Text style={styles.label}>{children}</Text>;
}

export function HelperText({ children }: { children: React.ReactNode }) {
  return <Text style={styles.helper}>{children}</Text>;
}

export function Input(props: TextInputProps) {
  return (
    <TextInput
      placeholderTextColor="#64748b"
      style={[styles.input, props.multiline ? styles.inputMultiline : null, props.style]}
      {...props}
    />
  );
}

export function Button({
  title,
  onPress,
  loading,
  disabled,
  variant = 'primary',
  small,
  style,
  textStyle,
}: {
  title: string;
  onPress: () => void;
  loading?: boolean;
  disabled?: boolean;
  variant?: 'primary' | 'secondary' | 'danger' | 'ghost';
  small?: boolean;
  style?: any;
  textStyle?: any;
}) {
  const isDisabled = disabled || loading;
  return (
    <Pressable
      onPress={onPress}
      disabled={isDisabled}
      style={({ pressed }) => [
        styles.btn,
        variant === 'secondary' && styles.btnSecondary,
        variant === 'danger' && styles.btnDanger,
        variant === 'ghost' && styles.btnGhost,
        small && styles.btnSmall,
        style,
        isDisabled && styles.btnDisabled,
        pressed && !isDisabled && styles.btnPressed,
      ]}>
      {loading ? (
        <ActivityIndicator size={small ? 'small' : 'small'} color={variant === 'ghost' ? colors.primary : '#fff'} />
      ) : (
        <Text
          style={[
            styles.btnText,
            variant === 'ghost' && styles.btnTextGhost,
            variant === 'secondary' && styles.btnTextSecondary,
            small && styles.btnTextSmall,
            textStyle,
          ]}>
          {title}
        </Text>
      )}
    </Pressable>
  );
}

export function Banner({
  type = 'info',
  message,
}: {
  type?: 'info' | 'success' | 'error' | 'warning';
  message: string;
}) {
  if (!message) return null;
  return (
    <View
      style={[
        styles.banner,
        type === 'success' && styles.bannerSuccess,
        type === 'error' && styles.bannerError,
        type === 'warning' && styles.bannerWarning,
      ]}>
      <Text style={styles.bannerText}>{message}</Text>
    </View>
  );
}

export function EmptyState({ title, subtitle }: { title: string; subtitle?: string }) {
  return (
    <View style={styles.empty}>
      <Text style={styles.emptyTitle}>{title}</Text>
      {subtitle ? <Text style={styles.emptySubtitle}>{subtitle}</Text> : null}
    </View>
  );
}

export function LoadingOverlay({ visible, label }: { visible: boolean; label?: string }) {
  if (!visible) return null;
  return (
    <View style={styles.overlay}>
      <ActivityIndicator size="large" color={colors.primary} />
      {label ? <Text style={styles.overlayLabel}>{label}</Text> : null}
    </View>
  );
}

export function ProgressBar({ value }: { value: number }) {
  const pct = Math.max(0, Math.min(1, value));
  return (
    <View style={styles.progressTrack}>
      <View style={[styles.progressFill, { width: `${pct * 100}%` }]} />
    </View>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1, backgroundColor: colors.bg },
  scrollContent: { padding: spacing.md, paddingBottom: spacing.xl },
  card: {
    backgroundColor: colors.surface,
    borderRadius: radius.md,
    padding: spacing.md,
    marginBottom: spacing.md,
    borderWidth: 1,
    borderColor: colors.border,
  },
  sectionTitle: {
    color: colors.text,
    fontSize: 18,
    fontWeight: '700',
    marginBottom: spacing.sm,
  },
  label: {
    color: colors.textMuted,
    fontSize: 13,
    fontWeight: '600',
    marginBottom: 6,
    marginTop: spacing.sm,
  },
  helper: {
    color: colors.textMuted,
    fontSize: 12,
    marginTop: 4,
    lineHeight: 16,
  },
  input: {
    backgroundColor: colors.surfaceAlt,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radius.sm,
    paddingHorizontal: 12,
    paddingVertical: 10,
    color: '#f8fafc',
    fontSize: 15,
  },
  inputMultiline: {
    minHeight: 120,
    textAlignVertical: 'top',
    color: '#f8fafc',
  },
  btn: {
    backgroundColor: colors.primary,
    paddingVertical: 12,
    paddingHorizontal: 16,
    borderRadius: radius.sm,
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: spacing.sm,
    minHeight: 44,
  },
  btnSmall: {
    paddingVertical: 8,
    paddingHorizontal: 14,
    minHeight: 36,
    marginTop: 0,
  },
  btnSecondary: {
    backgroundColor: colors.surfaceAlt,
    borderWidth: 1,
    borderColor: colors.border,
  },
  btnDanger: { backgroundColor: colors.danger },
  btnGhost: { backgroundColor: 'transparent' },
  btnDisabled: { opacity: 0.5 },
  btnPressed: { opacity: 0.85 },
  btnText: { color: '#ffffff', fontWeight: '700', fontSize: 15 },
  btnTextSecondary: { color: '#e2e8f0' },
  btnTextSmall: { fontSize: 13 },
  btnTextGhost: { color: colors.primary },
  banner: {
    backgroundColor: '#1e293b',
    borderRadius: radius.sm,
    padding: spacing.sm,
    marginBottom: spacing.md,
    borderLeftWidth: 4,
    borderLeftColor: colors.primary,
  },
  bannerSuccess: { borderLeftColor: colors.success },
  bannerError: { borderLeftColor: colors.danger },
  bannerWarning: { borderLeftColor: colors.warning },
  bannerText: { color: colors.text, fontSize: 13 },
  empty: { alignItems: 'center', paddingVertical: spacing.xl },
  emptyTitle: { color: colors.text, fontWeight: '700', fontSize: 16 },
  emptySubtitle: { color: colors.textMuted, fontSize: 13, marginTop: 4, textAlign: 'center' },
  overlay: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: 'rgba(11,17,32,0.75)',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 999,
  },
  overlayLabel: { color: colors.text, marginTop: spacing.sm },
  progressTrack: {
    height: 8,
    borderRadius: 4,
    backgroundColor: colors.surfaceAlt,
    overflow: 'hidden',
  },
  progressFill: {
    height: 8,
    backgroundColor: colors.primary,
  },
});

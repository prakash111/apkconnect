import React, { useState } from 'react';
import { Text, View, StyleSheet, Pressable } from 'react-native';
import { Screen, Card, Label, Input, Button, Banner } from '../../components/UI';
import { requestPasswordReset } from '../../api/auth';
import { colors, spacing } from '../../theme/theme';

export default function ForgotPasswordScreen({ navigation }: any) {
  const [email, setEmail] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const onSubmit = async () => {
    setError('');
    setMessage('');
    if (!email) {
      setError('Enter your account email.');
      return;
    }
    setLoading(true);
    try {
      const res = await requestPasswordReset(email);
      if (res.status === 'success') setMessage(res.message || 'Check your email for a reset link.');
      else setError(res.message || 'Something went wrong.');
    } catch (e: any) {
      setError(e?.message || 'Something went wrong.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Screen>
      <View style={styles.header}>
        <Text style={styles.title}>Reset your password</Text>
        <Text style={styles.subtitle}>We'll email you a reset link</Text>
      </View>
      <Card>
        <Banner type="error" message={error} />
        <Banner type="success" message={message} />
        <Label>Email</Label>
        <Input
          value={email}
          onChangeText={setEmail}
          autoCapitalize="none"
          autoCorrect={false}
          keyboardType="email-address"
        />
        <Button title="Send reset link" onPress={onSubmit} loading={loading} />
      </Card>
      <Pressable onPress={() => navigation.navigate('ResetPassword')} style={styles.footerRow}>
        <Text style={styles.footerText}>Already have a reset token? Enter it here</Text>
      </Pressable>
      <Pressable onPress={() => navigation.navigate('Login')} style={styles.footerRow}>
        <Text style={styles.footerText}>Back to login</Text>
      </Pressable>
    </Screen>
  );
}

const styles = StyleSheet.create({
  header: { marginBottom: spacing.lg, marginTop: spacing.xl },
  title: { color: colors.text, fontSize: 24, fontWeight: '800', textAlign: 'center' },
  subtitle: { color: colors.textMuted, fontSize: 14, textAlign: 'center', marginTop: 6 },
  footerRow: { alignItems: 'center', marginTop: spacing.sm },
  footerText: { color: colors.textMuted },
});

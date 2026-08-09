import React, { useState } from 'react';
import { Text, View, StyleSheet, Pressable } from 'react-native';
import { Screen, Card, Label, Input, Button, Banner } from '../../components/UI';
import { useAuth } from '../../context/AuthContext';
import { colors, spacing } from '../../theme/theme';

export default function LoginScreen({ navigation }: any) {
  const { login } = useAuth();
  const [loginId, setLoginId] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const onSubmit = async () => {
    setError('');
    if (!loginId || !password) {
      setError('Enter your username/email and password.');
      return;
    }
    setLoading(true);
    try {
      const res = await login(loginId, password);
      if (!res.ok) setError(res.message || 'Login failed.');
    } catch (e: any) {
      setError(e?.message || 'Login failed.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Screen>
      <View style={styles.header}>
        <Text style={styles.title}>Welcome back</Text>
        <Text style={styles.subtitle}>Sign in to your APKTOOL Studio account</Text>
      </View>
      <Card>
        <Banner type="error" message={error} />
        <Label>Username or Email</Label>
        <Input value={loginId} onChangeText={setLoginId} autoCapitalize="none" autoCorrect={false} />
        <Label>Password</Label>
        <Input value={password} onChangeText={setPassword} secureTextEntry />
        <Button title="Log In" onPress={onSubmit} loading={loading} />
        <Pressable onPress={() => navigation.navigate('ForgotPassword')} style={styles.linkRow}>
          <Text style={styles.link}>Forgot password?</Text>
        </Pressable>
      </Card>
      <Pressable onPress={() => navigation.navigate('Register')} style={styles.footerRow}>
        <Text style={styles.footerText}>
          Don't have an account? <Text style={styles.link}>Create one</Text>
        </Text>
      </Pressable>
      <Pressable onPress={() => navigation.navigate('ServerSetup')} style={styles.footerRow}>
        <Text style={styles.footerTextMuted}>Change server URL</Text>
      </Pressable>
    </Screen>
  );
}

const styles = StyleSheet.create({
  header: { marginBottom: spacing.lg, marginTop: spacing.xl },
  title: { color: colors.text, fontSize: 26, fontWeight: '800', textAlign: 'center' },
  subtitle: { color: colors.textMuted, fontSize: 14, textAlign: 'center', marginTop: 6 },
  linkRow: { marginTop: spacing.md, alignItems: 'center' },
  link: { color: colors.primary, fontWeight: '700' },
  footerRow: { alignItems: 'center', marginTop: spacing.sm },
  footerText: { color: colors.textMuted },
  footerTextMuted: { color: colors.textMuted, fontSize: 12 },
});

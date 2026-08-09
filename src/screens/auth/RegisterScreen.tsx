import React, { useState } from 'react';
import { Text, View, StyleSheet, Pressable } from 'react-native';
import { Screen, Card, Label, Input, Button, Banner } from '../../components/UI';
import { useAuth } from '../../context/AuthContext';
import { colors, spacing } from '../../theme/theme';

export default function RegisterScreen({ navigation }: any) {
  const { register } = useAuth();
  const [email, setEmail] = useState('');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);

  const onSubmit = async () => {
    setError('');
    setSuccess('');
    if (!email || !username || !password) {
      setError('All fields are required.');
      return;
    }
    setLoading(true);
    try {
      const res = await register(email, username, password);
      if (res.ok) {
        setSuccess(res.message || 'Registered! Check your email to verify your account.');
      } else {
        setError(res.message || 'Registration failed.');
      }
    } catch (e: any) {
      setError(e?.message || 'Registration failed.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Screen>
      <View style={styles.header}>
        <Text style={styles.title}>Create account</Text>
        <Text style={styles.subtitle}>Start decompiling, editing, and signing APKs</Text>
      </View>
      <Card>
        <Banner type="error" message={error} />
        <Banner type="success" message={success} />
        <Label>Email</Label>
        <Input
          value={email}
          onChangeText={setEmail}
          autoCapitalize="none"
          autoCorrect={false}
          keyboardType="email-address"
        />
        <Label>Username</Label>
        <Input value={username} onChangeText={setUsername} autoCapitalize="none" autoCorrect={false} />
        <Label>Password</Label>
        <Input value={password} onChangeText={setPassword} secureTextEntry />
        <Button title="Create Account" onPress={onSubmit} loading={loading} />
      </Card>
      <Pressable onPress={() => navigation.navigate('Login')} style={styles.footerRow}>
        <Text style={styles.footerText}>
          Already have an account? <Text style={styles.link}>Log in</Text>
        </Text>
      </Pressable>
    </Screen>
  );
}

const styles = StyleSheet.create({
  header: { marginBottom: spacing.lg, marginTop: spacing.xl },
  title: { color: colors.text, fontSize: 26, fontWeight: '800', textAlign: 'center' },
  subtitle: { color: colors.textMuted, fontSize: 14, textAlign: 'center', marginTop: 6 },
  footerRow: { alignItems: 'center', marginTop: spacing.md },
  footerText: { color: colors.textMuted },
  link: { color: colors.primary, fontWeight: '700' },
});

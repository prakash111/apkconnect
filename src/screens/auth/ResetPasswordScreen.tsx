import React, { useState } from 'react';
import { Text, View, StyleSheet } from 'react-native';
import { Screen, Card, Label, Input, Button, Banner, HelperText } from '../../components/UI';
import { resetPassword } from '../../api/auth';
import { colors, spacing } from '../../theme/theme';

export default function ResetPasswordScreen({ navigation }: any) {
  const [token, setToken] = useState('');
  const [password, setPassword] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const onSubmit = async () => {
    setError('');
    setMessage('');
    if (!token || password.length < 6) {
      setError('Enter the token from your email and a password of at least 6 characters.');
      return;
    }
    setLoading(true);
    try {
      const res = await resetPassword(token, password);
      if (res.status === 'success') {
        setMessage(res.message || 'Password reset. You can now log in.');
        setTimeout(() => navigation.navigate('Login'), 1200);
      } else {
        setError(res.message || 'Reset failed.');
      }
    } catch (e: any) {
      setError(e?.message || 'Reset failed.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Screen>
      <View style={styles.header}>
        <Text style={styles.title}>Set a new password</Text>
      </View>
      <Card>
        <Banner type="error" message={error} />
        <Banner type="success" message={message} />
        <Label>Reset token (from the email link)</Label>
        <Input value={token} onChangeText={setToken} autoCapitalize="none" />
        <HelperText>
          Open the reset link from your email — the token is the value after ?token= in the URL.
        </HelperText>
        <Label>New password</Label>
        <Input value={password} onChangeText={setPassword} secureTextEntry />
        <Button title="Reset password" onPress={onSubmit} loading={loading} />
      </Card>
    </Screen>
  );
}

const styles = StyleSheet.create({
  header: { marginBottom: spacing.lg, marginTop: spacing.xl },
  title: { color: colors.text, fontSize: 24, fontWeight: '800', textAlign: 'center' },
});

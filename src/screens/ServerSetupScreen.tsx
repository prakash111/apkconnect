import React, { useEffect, useState } from 'react';
import { Text, View, StyleSheet } from 'react-native';
import { Screen, Card, Label, Input, Button, HelperText, Banner } from '../components/UI';
import { getBaseUrl, setBaseUrl } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { colors, spacing } from '../theme/theme';

export default function ServerSetupScreen({ navigation }: any) {
  const [url, setUrl] = useState('');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  const { refreshServerConfigured } = useAuth();

  useEffect(() => {
    (async () => {
      const stored = await getBaseUrl();
      if (stored) setUrl(stored);
    })();
  }, []);

  const onSave = async () => {
    setError('');
    const trimmed = url.trim();
    if (!/^https?:\/\/.+/.test(trimmed)) {
      setError('Enter a full URL including http:// or https://');
      return;
    }
    setSaving(true);
    try {
      await setBaseUrl(trimmed);
      await refreshServerConfigured();
      if (navigation.canGoBack()) {
        navigation.goBack();
      } else {
        navigation.replace('Login');
      }
    } finally {
      setSaving(false);
    }
  };

  return (
    <Screen>
      <View style={styles.header}>
        <Text style={styles.title}>APKTOOL Studio</Text>
        <Text style={styles.subtitle}>Connect to your APKTOOL Studio backend</Text>
      </View>
      <Card>
        <Banner type="error" message={error} />
        <Label>Server URL</Label>
        <Input
          placeholder="https://apk.example.com"
          value={url}
          onChangeText={setUrl}
          autoCapitalize="none"
          autoCorrect={false}
          keyboardType="url"
        />
        <HelperText>
          This is the root URL where your APKTOOL Studio PHP app is deployed (the folder that
          contains index.php). E.g. https://apk.zoomnearby.com
        </HelperText>
        <Button title="Continue" onPress={onSave} loading={saving} />
      </Card>
    </Screen>
  );
}

const styles = StyleSheet.create({
  header: { marginBottom: spacing.lg, marginTop: spacing.xl },
  title: { color: colors.text, fontSize: 26, fontWeight: '800', textAlign: 'center' },
  subtitle: { color: colors.textMuted, fontSize: 14, textAlign: 'center', marginTop: 6 },
});

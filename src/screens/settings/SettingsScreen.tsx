import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { Card, Button, SectionTitle } from '../../components/UI';
import { useAuth } from '../../context/AuthContext';
import { colors, spacing } from '../../theme/theme';

export default function SettingsScreen({ navigation }: any) {
  const { user, logout } = useAuth();

  return (
    <View style={styles.flex}>
      <Card>
        <SectionTitle>Account</SectionTitle>
        <Text style={styles.userText}>{user?.username}</Text>
        <Button title="Log out" variant="danger" onPress={logout} />
      </Card>

      <Card>
        <SectionTitle>AI</SectionTitle>
        <Button title="AI Provider & API Keys" variant="secondary" onPress={() => navigation.navigate('AiSettings')} />
      </Card>

      <Card>
        <SectionTitle>Server</SectionTitle>
        <Button
          title="Change server URL"
          variant="secondary"
          onPress={() => navigation.navigate('ServerSetup')}
        />
      </Card>

      {user?.user_type === 'admin' ? (
        <Card>
          <SectionTitle>Admin</SectionTitle>
          <Button
            title="Open Admin Panel"
            onPress={() => navigation.getParent()?.navigate('AdminTab')}
          />
        </Card>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1, backgroundColor: colors.bg, padding: spacing.md },
  userText: { color: colors.text, fontSize: 16, fontWeight: '700', marginBottom: spacing.sm },
});

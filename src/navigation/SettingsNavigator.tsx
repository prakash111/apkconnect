import React from 'react';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import SettingsScreen from '../screens/settings/SettingsScreen';
import AiSettingsScreen from '../screens/settings/AiSettingsScreen';
import ServerSetupScreen from '../screens/ServerSetupScreen';
import { colors } from '../theme/theme';

const Stack = createNativeStackNavigator();

export default function SettingsNavigator() {
  return (
    <Stack.Navigator
      screenOptions={{
        headerStyle: { backgroundColor: colors.surface },
        headerTintColor: colors.text,
        headerShadowVisible: false,
        contentStyle: { backgroundColor: colors.bg },
      }}>
      <Stack.Screen name="SettingsHome" component={SettingsScreen} options={{ title: 'Settings' }} />
      <Stack.Screen name="AiSettings" component={AiSettingsScreen} options={{ title: 'AI Settings' }} />
      <Stack.Screen name="ServerSetup" component={ServerSetupScreen} options={{ title: 'Server URL' }} />
    </Stack.Navigator>
  );
}

import React from 'react';
import { Text, View } from 'react-native';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import DashboardScreen from '../screens/DashboardScreen';
import ProjectsNavigator from './ProjectsNavigator';
import WorkflowNavigator from './WorkflowNavigator';
import SettingsNavigator from './SettingsNavigator';
import AdminNavigator from './AdminNavigator';
import NewBuildBanner from '../components/NewBuildBanner';
import { useAuth } from '../context/AuthContext';
import { colors } from '../theme/theme';

const Tab = createBottomTabNavigator();

const ICONS: Record<string, string> = {
  DashboardTab: '🏠',
  ProjectsTab: '📦',
  WorkflowTab: '🛠️',
  SettingsTab: '⚙️',
  AdminTab: '🛡️',
};

export default function MainNavigator() {
  const { user } = useAuth();
  const isAdmin = user?.user_type === 'admin';

  return (
    <View style={{ flex: 1, backgroundColor: colors.bg }}>
      <NewBuildBanner />
      <Tab.Navigator
        screenOptions={({ route }) => ({
          headerShown: false,
          tabBarActiveTintColor: colors.primary,
          tabBarInactiveTintColor: colors.textMuted,
          tabBarStyle: {
            backgroundColor: colors.surface,
            borderTopColor: colors.border,
            height: 62,
            paddingTop: 4,
            paddingBottom: 8,
          },
          tabBarLabelStyle: {
            fontSize: 11,
            fontWeight: '600',
            marginTop: 2,
          },
          tabBarIcon: ({ focused }) => (
            <Text style={{ fontSize: focused ? 20 : 18 }}>{ICONS[route.name]}</Text>
          ),
        })}>
        <Tab.Screen name="DashboardTab" component={DashboardScreen} options={{ title: 'Home' }} />
        <Tab.Screen name="ProjectsTab" component={ProjectsNavigator} options={{ title: 'Projects' }} />
        <Tab.Screen name="WorkflowTab" component={WorkflowNavigator} options={{ title: 'Studio' }} />
        <Tab.Screen name="SettingsTab" component={SettingsNavigator} options={{ title: 'Settings' }} />
        {isAdmin ? (
          <Tab.Screen name="AdminTab" component={AdminNavigator} options={{ title: 'Admin' }} />
        ) : null}
      </Tab.Navigator>
    </View>
  );
}

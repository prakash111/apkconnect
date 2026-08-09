import React from 'react';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import AdminHomeScreen from '../screens/admin/AdminHomeScreen';
import AdminUsersScreen from '../screens/admin/AdminUsersScreen';
import AdminInquiriesScreen from '../screens/admin/AdminInquiriesScreen';
import AdminBlogsScreen from '../screens/admin/AdminBlogsScreen';
import AdminFaqsScreen from '../screens/admin/AdminFaqsScreen';
import AdminBackupScreen from '../screens/admin/AdminBackupScreen';
import AdminGlobalAiScreen from '../screens/admin/AdminGlobalAiScreen';
import { colors } from '../theme/theme';

const Stack = createNativeStackNavigator();

export default function AdminNavigator() {
  return (
    <Stack.Navigator
      screenOptions={{
        headerStyle: { backgroundColor: colors.surface },
        headerTintColor: colors.text,
        headerShadowVisible: false,
        contentStyle: { backgroundColor: colors.bg },
      }}>
      <Stack.Screen name="AdminHome" component={AdminHomeScreen} options={{ title: 'Admin' }} />
      <Stack.Screen name="AdminUsers" component={AdminUsersScreen} options={{ title: 'Users' }} />
      <Stack.Screen
        name="AdminInquiries"
        component={AdminInquiriesScreen}
        options={{ title: 'Contact Inquiries' }}
      />
      <Stack.Screen name="AdminBlogs" component={AdminBlogsScreen} options={{ title: 'Blog Posts' }} />
      <Stack.Screen name="AdminFaqs" component={AdminFaqsScreen} options={{ title: 'FAQs' }} />
      <Stack.Screen
        name="AdminBackup"
        component={AdminBackupScreen}
        options={{ title: 'Backup Settings' }}
      />
      <Stack.Screen
        name="AdminGlobalAi"
        component={AdminGlobalAiScreen}
        options={{ title: 'Global AI Defaults' }}
      />
    </Stack.Navigator>
  );
}

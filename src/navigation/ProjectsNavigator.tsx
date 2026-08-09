import React from 'react';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import ProjectListScreen from '../screens/projects/ProjectListScreen';
import NewProjectScreen from '../screens/projects/NewProjectScreen';
import { colors } from '../theme/theme';

const Stack = createNativeStackNavigator();

export default function ProjectsNavigator() {
  return (
    <Stack.Navigator
      screenOptions={{
        headerStyle: { backgroundColor: colors.surface },
        headerTintColor: colors.text,
        headerShadowVisible: false,
        contentStyle: { backgroundColor: colors.bg },
      }}>
      <Stack.Screen name="ProjectList" component={ProjectListScreen} options={{ title: 'My Projects' }} />
      <Stack.Screen name="NewProject" component={NewProjectScreen} options={{ title: 'New Project' }} />
    </Stack.Navigator>
  );
}

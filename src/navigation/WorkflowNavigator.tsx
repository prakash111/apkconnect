import React from 'react';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import WorkflowHomeScreen from '../screens/workflow/WorkflowHomeScreen';
import FileBrowserScreen from '../screens/workflow/FileBrowserScreen';
import FileEditorScreen from '../screens/workflow/FileEditorScreen';
import StringsEditorScreen from '../screens/workflow/StringsEditorScreen';
import FindReplaceScreen from '../screens/workflow/FindReplaceScreen';
import HexSearchScreen from '../screens/workflow/HexSearchScreen';
import FirebaseConfigScreen from '../screens/workflow/FirebaseConfigScreen';
import LogoIconScreen from '../screens/workflow/LogoIconScreen';
import AiToolsScreen from '../screens/workflow/AiToolsScreen';
import KeystoreScreen from '../screens/workflow/KeystoreScreen';
import BuildSignScreen from '../screens/workflow/BuildSignScreen';
import AdbScreen from '../screens/workflow/AdbScreen';
import LogcatScreen from '../screens/workflow/LogcatScreen';
import CloudLoggingScreen from '../screens/workflow/CloudLoggingScreen';
import { colors } from '../theme/theme';

const Stack = createNativeStackNavigator();

export default function WorkflowNavigator() {
  return (
    <Stack.Navigator
      screenOptions={{
        headerStyle: { backgroundColor: colors.surface },
        headerTintColor: colors.text,
        headerShadowVisible: false,
        contentStyle: { backgroundColor: colors.bg },
      }}>
      <Stack.Screen name="WorkflowHome" component={WorkflowHomeScreen} options={{ title: 'Project' }} />
      <Stack.Screen name="FileBrowser" component={FileBrowserScreen} options={{ title: 'Files' }} />
      <Stack.Screen name="FileEditor" component={FileEditorScreen} options={{ title: 'Edit File' }} />
      <Stack.Screen
        name="StringsEditor"
        component={StringsEditorScreen}
        options={{ title: 'App Name & Strings' }}
      />
      <Stack.Screen name="FindReplace" component={FindReplaceScreen} options={{ title: 'Find & Replace' }} />
      <Stack.Screen name="HexSearch" component={HexSearchScreen} options={{ title: 'Hex Search' }} />
      <Stack.Screen
        name="FirebaseConfig"
        component={FirebaseConfigScreen}
        options={{ title: 'Firebase Config' }}
      />
      <Stack.Screen name="LogoIcon" component={LogoIconScreen} options={{ title: 'Logo & Icon' }} />
      <Stack.Screen name="AiTools" component={AiToolsScreen} options={{ title: 'AI Tools' }} />
      <Stack.Screen name="KeystoreScreen" component={KeystoreScreen} options={{ title: 'Keystore' }} />
      <Stack.Screen name="BuildSign" component={BuildSignScreen} options={{ title: 'Build & Sign' }} />
      <Stack.Screen name="Adb" component={AdbScreen} options={{ title: 'ADB Devices' }} />
      <Stack.Screen name="Logcat" component={LogcatScreen} options={{ title: 'Logcat' }} />
      <Stack.Screen
        name="CloudLogging"
        component={CloudLoggingScreen}
        options={{ title: 'Cloud Debug Logging' }}
      />
    </Stack.Navigator>
  );
}

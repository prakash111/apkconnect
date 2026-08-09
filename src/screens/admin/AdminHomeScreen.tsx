import React from 'react';
import { View, StyleSheet, Pressable, Text, ScrollView } from 'react-native';
import { colors, radius, spacing } from '../../theme/theme';

const SECTIONS = [
  { key: 'AdminUsers', title: 'Users', subtitle: 'Manage accounts & usage limits', icon: '👥' },
  { key: 'AdminInquiries', title: 'Contact Inquiries', subtitle: 'Messages from the contact form', icon: '✉️' },
  { key: 'AdminBlogs', title: 'Blog Posts', subtitle: 'Create & edit blog content', icon: '📝' },
  { key: 'AdminFaqs', title: 'FAQs', subtitle: 'Manage the FAQ list', icon: '❓' },
  { key: 'AdminBackup', title: 'Backup Settings', subtitle: 'GitHub/cloud backups of the install', icon: '💾' },
  { key: 'AdminGlobalAi', title: 'Global AI Defaults', subtitle: 'Default provider & models for all users', icon: '🤖' },
];

export default function AdminHomeScreen({ navigation }: any) {
  return (
    <ScrollView style={styles.flex} contentContainerStyle={styles.content}>
      <View style={styles.grid}>
        {SECTIONS.map(s => (
          <Pressable key={s.key} style={styles.tile} onPress={() => navigation.navigate(s.key)}>
            <Text style={styles.icon}>{s.icon}</Text>
            <Text style={styles.title}>{s.title}</Text>
            <Text style={styles.subtitle}>{s.subtitle}</Text>
          </Pressable>
        ))}
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1, backgroundColor: colors.bg },
  content: { padding: spacing.md, paddingBottom: spacing.xl },
  grid: { flexDirection: 'row', flexWrap: 'wrap', justifyContent: 'space-between' },
  tile: {
    width: '48%',
    backgroundColor: colors.surface,
    borderRadius: radius.md,
    borderWidth: 1,
    borderColor: colors.border,
    padding: spacing.md,
    marginBottom: spacing.sm,
  },
  icon: { fontSize: 26, marginBottom: 6 },
  title: { color: colors.text, fontWeight: '700', fontSize: 14 },
  subtitle: { color: colors.textMuted, fontSize: 11, marginTop: 4 },
});

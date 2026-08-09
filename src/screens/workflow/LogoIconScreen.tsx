import React, { useEffect, useState } from 'react';
import { View, Text, StyleSheet, Image, ScrollView, ActivityIndicator } from 'react-native';
import { launchImageLibrary } from 'react-native-image-picker';
import { Card, Button, Banner, HelperText, SectionTitle, Input, Label } from '../../components/UI';
import * as workflowApi from '../../api/workflow';
import * as aiApi from '../../api/ai';
import { getBaseUrl } from '../../api/client';
import { useProject } from '../../context/ProjectContext';
import { colors, radius, spacing } from '../../theme/theme';

export default function LogoIconScreen() {
  const { state, refreshState } = useProject();
  const [baseUrl, setBaseUrlState] = useState('');
  const [imageUri, setImageUri] = useState('');
  const [imageName, setImageName] = useState('');
  const [imageType, setImageType] = useState('image/png');
  const [prompt, setPrompt] = useState('');
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [uploading, setUploading] = useState(false);
  const [generating, setGenerating] = useState(false);
  const [imgLoading, setImgLoading] = useState(true);

  useEffect(() => {
    (async () => {
      const url = await getBaseUrl();
      setBaseUrlState(url.replace(/\/+$/, '').replace(/\/index\.php$/i, ''));
    })();
  }, []);

  const activeIconPath = state?.logo_preview_path || state?.default_icon_path || state?.primary_icon?.storage_path || (state?.icon_targets && state.icon_targets[0]?.storage_path) || '';
  
  let activeIconUrl = '';
  if (state?.primary_icon?.base64) {
    activeIconUrl = state.primary_icon.base64;
  } else if (state?.app_icon_url && state.app_icon_url.startsWith('data:')) {
    activeIconUrl = state.app_icon_url;
  } else if (baseUrl && activeIconPath) {
    activeIconUrl = `${baseUrl}/index.php?view=${encodeURIComponent(activeIconPath)}&v=${state?.logo_version || 1}`;
  } else if (state?.app_icon_url && baseUrl && !state.app_icon_url.startsWith('http')) {
    activeIconUrl = `${baseUrl}/${state.app_icon_url.replace(/^\/+/, '')}`;
  } else if (state?.app_icon_url) {
    activeIconUrl = state.app_icon_url;
  }

  const pickImage = async () => {
    setError('');
    const result = await launchImageLibrary({ mediaType: 'photo' });
    if (result.didCancel) return;
    const asset = result.assets?.[0];
    if (!asset?.uri) return;
    setImageUri(asset.uri);
    setImageName(asset.fileName || 'logo.png');
    setImageType(asset.type || 'image/png');
  };

  const onUpload = async () => {
    if (!imageUri) {
      setError('Pick an image first.');
      return;
    }
    setUploading(true);
    setError('');
    setMessage('');
    try {
      const res = await workflowApi.uploadLogo({ uri: imageUri, name: imageName, type: imageType });
      if (res.status === 'success') {
        setMessage(res.message || 'Icon applied to all launcher targets.');
        setImageUri('');
        setImageName('');
        await refreshState();
      } else {
        setError(res.message || 'Upload failed.');
      }
    } catch (e: any) {
      setError(e?.message || 'Upload failed.');
    } finally {
      setUploading(false);
    }
  };

  const onGenerate = async () => {
    if (!prompt) {
      setError('Describe the icon you want.');
      return;
    }
    setGenerating(true);
    setError('');
    setMessage('');
    try {
      const res = await aiApi.aiGenerateIcon(prompt);
      if (res.status === 'success') {
        setMessage(res.message || 'AI icon generated and applied.');
        setPrompt('');
        await refreshState();
      } else {
        setError(res.message || 'Generation failed.');
      }
    } catch (e: any) {
      setError(e?.message || 'Generation failed.');
    } finally {
      setGenerating(false);
    }
  };

  return (
    <ScrollView style={styles.flex} contentContainerStyle={styles.content}>
      <Banner type="error" message={error} />
      <Banner type="success" message={message} />

      {/* 1. CURRENT APP ICON FROM DECOMPILED PROJECT */}
      <Card style={styles.previewCard}>
        <SectionTitle>Current App Icon</SectionTitle>
        <HelperText>
          Default launcher icon detected from decompiled project assets.
        </HelperText>

        <View style={styles.iconPreviewContainer}>
          {activeIconUrl ? (
            <View style={styles.iconFrame}>
              <Image
                source={{ uri: activeIconUrl }}
                style={styles.iconImage}
                onLoadStart={() => setImgLoading(true)}
                onLoadEnd={() => setImgLoading(false)}
                resizeMode="contain"
              />
              {imgLoading && (
                <View style={styles.imgLoader}>
                  <ActivityIndicator color={colors.primary} />
                </View>
              )}
            </View>
          ) : (
            <View style={styles.placeholderFrame}>
              <Text style={styles.placeholderEmoji}>📱</Text>
              <Text style={styles.placeholderText}>Default Icon</Text>
            </View>
          )}

          <View style={styles.iconDetails}>
            <Text style={styles.iconDetailTitle}>
              {state?.logo_preview_name || (state?.default_icon_relative && state.default_icon_relative !== 'default_icon.svg' ? state.default_icon_relative : 'ic_launcher.png')}
            </Text>
            <Text style={styles.iconDetailSub}>
              {state?.default_icon_density && state.default_icon_density !== 'default' ? `Target: ${state.default_icon_density} (${state.default_icon_size}×${state.default_icon_size}px)` : 'Default Android app icon'}
            </Text>
            {state?.logo_preview_name ? (
              <View style={styles.customBadge}>
                <Text style={styles.customBadgeText}>Custom Icon Applied</Text>
              </View>
            ) : (
              <View style={styles.defaultBadge}>
                <Text style={styles.defaultBadgeText}>
                  {state?.primary_icon && !state.primary_icon.is_default ? 'Extracted Project Icon' : 'Default Project Icon'}
                </Text>
              </View>
            )}
          </View>
        </View>
      </Card>

      {/* 2. UPLOAD & REPLACE LOGO */}
      <Card>
        <SectionTitle>Upload New Logo / Icon</SectionTitle>
        <HelperText>
          Pick a PNG/JPG from your device gallery. It will be automatically resized into mdpi, hdpi, xhdpi, xxhdpi, and xxxhdpi.
        </HelperText>
        <Button
          title={imageName ? `Selected: ${imageName}` : '📁 Choose Image from Gallery'}
          variant="secondary"
          onPress={pickImage}
        />
        <Button
          title="Apply Icon to Project"
          onPress={onUpload}
          loading={uploading}
          disabled={!imageUri}
          style={{ marginTop: spacing.sm }}
        />
      </Card>

      {/* 3. GENERATE WITH AI */}
      <Card>
        <SectionTitle>✨ Generate Icon with AI</SectionTitle>
        <Label>Icon Description</Label>
        <Input
          value={prompt}
          onChangeText={setPrompt}
          placeholder="e.g. Modern minimalist shield with neon cyan gradient"
          multiline
        />
        <HelperText>Generates a high-res icon and updates all app targets.</HelperText>
        <Button
          title="Generate & Apply with AI"
          onPress={onGenerate}
          loading={generating}
          disabled={!prompt.trim()}
          style={{ marginTop: spacing.sm }}
        />
      </Card>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1, backgroundColor: colors.bg },
  content: { padding: spacing.md, paddingBottom: spacing.xl },
  previewCard: {
    backgroundColor: colors.surface,
    borderColor: colors.border,
  },
  iconPreviewContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: spacing.sm,
    padding: spacing.sm,
    backgroundColor: colors.surfaceAlt,
    borderRadius: radius.sm,
    borderWidth: 1,
    borderColor: colors.border,
  },
  iconFrame: {
    width: 76,
    height: 76,
    borderRadius: 16,
    backgroundColor: '#090d16',
    borderWidth: 1,
    borderColor: colors.border,
    alignItems: 'center',
    justifyContent: 'center',
    overflow: 'hidden',
  },
  iconImage: {
    width: 68,
    height: 68,
    borderRadius: 12,
  },
  imgLoader: {
    ...StyleSheet.absoluteFillObject,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: 'rgba(9,13,22,0.7)',
  },
  placeholderFrame: {
    width: 76,
    height: 76,
    borderRadius: 16,
    backgroundColor: '#090d16',
    borderWidth: 1,
    borderColor: colors.border,
    alignItems: 'center',
    justifyContent: 'center',
  },
  placeholderEmoji: { fontSize: 32 },
  placeholderText: { color: colors.textMuted, fontSize: 10, marginTop: 2 },
  iconDetails: {
    flex: 1,
    marginLeft: spacing.md,
  },
  iconDetailTitle: {
    color: colors.text,
    fontSize: 14,
    fontWeight: '700',
    fontFamily: 'monospace',
  },
  iconDetailSub: {
    color: colors.textMuted,
    fontSize: 12,
    marginTop: 2,
  },
  customBadge: {
    alignSelf: 'flex-start',
    backgroundColor: 'rgba(34, 197, 94, 0.2)',
    paddingHorizontal: 8,
    paddingVertical: 2,
    borderRadius: 4,
    marginTop: 6,
  },
  customBadgeText: { color: colors.success, fontSize: 11, fontWeight: '700' },
  defaultBadge: {
    alignSelf: 'flex-start',
    backgroundColor: 'rgba(99, 102, 241, 0.2)',
    paddingHorizontal: 8,
    paddingVertical: 2,
    borderRadius: 4,
    marginTop: 6,
  },
  defaultBadgeText: { color: colors.primary, fontSize: 11, fontWeight: '700' },
});

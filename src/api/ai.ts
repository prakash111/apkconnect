import { apiCall } from './client';

export async function getAiSettings() {
  return apiCall('get_ai_settings_ajax');
}

export async function saveApiKey(provider: 'gemini' | 'openai', api_key: string) {
  return apiCall('save_api_key_ajax', { provider, api_key });
}

export async function deleteApiKey(provider: 'gemini' | 'openai') {
  return apiCall('delete_api_key_ajax', { provider });
}

export async function saveAiProvider(provider: 'gemini' | 'openai') {
  return apiCall('save_ai_provider_ajax', { provider });
}

export interface AiModelFields {
  gemini_text_model?: string;
  gemini_image_model?: string;
  openai_text_model?: string;
  openai_image_model?: string;
}

export async function saveUserAiModels(models: AiModelFields) {
  return apiCall('save_user_ai_models_ajax', models);
}

export async function resetUserAiModels() {
  return apiCall('reset_user_ai_models_ajax');
}

// --- In-workflow AI actions ---

/** Review the file currently open in the editor for syntax / resource errors. */
export async function aiReviewEditorFile(workflow_file_path: string, workflow_file_content: string) {
  return apiCall('workflow_ai_review_editor_ajax', { workflow_file_path, workflow_file_content });
}

/** Diagnose the most recent build failure and attempt an automatic fix. */
export async function aiFixBuildError(workflow_file_path = '') {
  return apiCall('workflow_ai_fix_ajax', { workflow_file_path });
}

/** Apply a previously-suggested AI fix. */
export async function aiApplyFix() {
  return apiCall('workflow_ai_apply_fix_ajax');
}

/** Generate a launcher icon from a text prompt and apply it to the project. */
export async function aiGenerateIcon(ai_icon_prompt: string) {
  return apiCall('workflow_ai_generate_icon_ajax', { ai_icon_prompt });
}

// --- Admin: global AI defaults ---

export async function getGlobalAiSettings() {
  return apiCall('get_global_ai_settings');
}

export async function saveGlobalAiSettings(settings: Record<string, string>) {
  return apiCall('save_global_ai_settings', settings);
}

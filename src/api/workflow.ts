import { apiCall, apiUpload, UploadFile } from './client';

export async function getDir(dir_path = '') {
  return apiCall('workflow_get_dir_ajax', { dir_path });
}

export async function openEditorFile(workflow_file_path: string, binary_offset = 0) {
  return apiCall('workflow_open_editor_file_ajax', { workflow_file_path, binary_offset });
}

export async function saveEditorFile(workflow_file_path: string, workflow_file_content: string) {
  return apiCall('workflow_save_editor_file_ajax', { workflow_file_path, workflow_file_content });
}

export async function searchHex(workflow_file_path: string, query: string) {
  return apiCall('workflow_search_hex_ajax', { workflow_file_path, query });
}

export async function replaceFile(workflow_file_path: string, file: UploadFile) {
  return apiUpload('workflow_replace_file_ajax', { workflow_file_path }, { replacement_file: file });
}

export async function loadStrings(workflow_locale = 'values') {
  return apiCall('workflow_load_strings_ajax', { workflow_locale });
}

export async function autosaveStrings(
  workflow_locale: string,
  workflow_strings: Record<string, string>,
  workflow_app_name = '',
) {
  return apiCall('workflow_autosave_strings_ajax', {
    workflow_locale,
    workflow_strings,
    workflow_app_name,
  });
}

export async function findInProject(workflow_find_text: string) {
  return apiCall('workflow_find_project_ajax', { workflow_find_text });
}

export async function findReplaceInProject(workflow_find_text: string, workflow_replace_text: string) {
  return apiCall('workflow_find_replace_ajax', { workflow_find_text, workflow_replace_text });
}

export async function applyFirebaseConfig(jsonFile: UploadFile) {
  return apiUpload('workflow_apply_firebase_ajax', {}, { workflow_firebase_json: jsonFile });
}

export async function uploadLogo(logoFile: UploadFile) {
  return apiUpload('workflow_upload_logo_ajax', {}, { workflow_logo: logoFile });
}

export async function buildApk() {
  return apiCall('workflow_build_apk_ajax');
}

// --- Cloud (device) debug logging, injected into the rebuilt APK ---
export async function enableCloudLogging() {
  return apiCall('workflow_enable_cloud_logging_ajax');
}

export async function getCloudLogs() {
  return apiCall('workflow_get_cloud_logs_ajax');
}

export async function clearCloudLogs() {
  return apiCall('workflow_clear_cloud_logs_ajax');
}

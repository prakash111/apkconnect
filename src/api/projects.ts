import { apiCall, apiUpload, UploadFile } from './client';

export async function getProjects() {
  return apiCall('get_projects');
}

export async function switchProject(project_id: string) {
  return apiCall('switch_project', { project_id });
}

export async function renameProject(project_id: string, new_name: string) {
  return apiCall('workflow_rename_project_ajax', { project_id, new_name });
}

export async function deleteProject(project_id: string) {
  return apiCall('delete_project_ajax', { project_id });
}

export async function closeProject() {
  return apiCall('workflow_close_project_ajax');
}

export async function resetWorkflow() {
  return apiCall('workflow_reset_ajax');
}

export async function uploadAndDecompile(apkFile: UploadFile) {
  return apiUpload('workflow_upload_decompile_ajax', {}, { workflow_apk: apkFile });
}

export async function getWorkflowState(locale = 'values') {
  return apiCall('workflow_state_ajax', { workflow_locale: locale });
}

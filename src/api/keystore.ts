import { apiCall } from './client';

export async function createKeystore(workflow_key_alias: string, workflow_key_password: string) {
  return apiCall('workflow_create_keystore_ajax', { workflow_key_alias, workflow_key_password });
}

export async function selectKeystore(keystore_id: number) {
  return apiCall('workflow_select_keystore_ajax', { keystore_id });
}

export async function signApk(workflow_sign_password: string) {
  return apiCall('workflow_sign_apk_ajax', { workflow_sign_password });
}

export async function getKeystores() {
  return apiCall('get_keystores');
}

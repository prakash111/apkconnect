import { apiCall } from './client';

export async function listDevices() {
  return apiCall('adb_list_devices_ajax');
}

export async function connectDevice(adb_host: string) {
  return apiCall('adb_connect_ajax', { adb_host });
}

export async function disconnectDevice(adb_host: string) {
  return apiCall('adb_disconnect_ajax', { adb_host });
}

export async function installApk(adb_serial: string, apk_variant: 'signed' | 'unsigned' = 'signed') {
  return apiCall('adb_install_apk_ajax', { adb_serial, apk_variant });
}

export async function readLogcat(adb_serial: string, log_filter: 'all' | 'error' | 'network' = 'all') {
  return apiCall('adb_read_logcat_ajax', { adb_serial, log_filter });
}

export async function clearLogcat(adb_serial: string) {
  return apiCall('adb_clear_logcat_ajax', { adb_serial });
}

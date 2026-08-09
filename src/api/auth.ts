import { apiCall, persistSessionCookies, clearSession } from './client';

export async function login(login: string, password: string) {
  const res = await apiCall('login', { login, password });
  if (res.status === 'success') await persistSessionCookies();
  return res;
}

export async function register(email: string, username: string, password: string) {
  return apiCall('register', { email, username, password });
}

export async function logout() {
  const res = await apiCall('logout');
  await clearSession();
  return res;
}

export async function requestPasswordReset(email: string) {
  return apiCall('request_password_reset', { email });
}

export async function resetPassword(token: string, password: string) {
  return apiCall('reset_password', { token, password });
}

export async function verifyEmail(token: string) {
  return apiCall('verify_email', { token });
}

export async function getLimits() {
  return apiCall('get_limits');
}

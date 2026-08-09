import AsyncStorage from '@react-native-async-storage/async-storage';
import CookieManager from '@react-native-cookies/cookies';

/**
 * APKTOOL Studio – API client
 * -----------------------------------------------------------------------
 * The backend (your existing PHP app) exposes a single entry point,
 * `index.php`, that accepts POST requests with an `action` field and
 * returns JSON: { status: 'success' | 'error', message?, ...payload }.
 *
 * Authentication is PHP session-cookie based (see `login` / `logout`
 * actions in index.php). React Native's networking stack on Android
 * (OkHttp) keeps cookies for the lifetime of the app process, and we
 * additionally persist + restore them via @react-native-cookies/cookies
 * so a logged-in session survives an app restart.
 */

const BASE_URL_KEY = 'apktool.baseUrl';
const DEFAULT_BASE_URL = ''; // set on first run via ServerSetupScreen

let cachedBaseUrl: string | null = null;

export async function getBaseUrl(): Promise<string> {
  if (cachedBaseUrl !== null) return cachedBaseUrl;
  const stored = await AsyncStorage.getItem(BASE_URL_KEY);
  cachedBaseUrl = stored ?? DEFAULT_BASE_URL;
  return cachedBaseUrl;
}

export async function setBaseUrl(url: string): Promise<void> {
  let trimmed = url.trim().replace(/\/+$/, '');
  trimmed = trimmed.replace(/\/index\.php$/i, '');
  cachedBaseUrl = trimmed;
  await AsyncStorage.setItem(BASE_URL_KEY, trimmed);
}

function endpoint(baseUrl: string): string {
  // Backend root exposes index.php as the single AJAX router.
  const clean = baseUrl.trim().replace(/\/+$/, '').replace(/\/index\.php$/i, '');
  return `${clean}/index.php`;
}

export class ApiError extends Error {
  status: number;
  constructor(message: string, status = 0) {
    super(message);
    this.status = status;
  }
}

export interface ApiResponse {
  status: 'success' | 'error';
  message?: string;
  [key: string]: any;
}

/**
 * Call a plain (non file-upload) AJAX action.
 */
export async function apiCall(
  action: string,
  params: Record<string, any> = {},
): Promise<ApiResponse> {
  const baseUrl = await getBaseUrl();
  if (!baseUrl) {
    throw new ApiError('Server URL is not configured yet. Set it in Settings.');
  }

  const form = new URLSearchParams();
  form.append('action', action);
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null) return;
    form.append(key, typeof value === 'object' ? JSON.stringify(value) : String(value));
  });

  let res: Response;
  try {
    res = await fetch(endpoint(baseUrl), {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
        Accept: 'application/json',
        'X-Requested-With': 'XMLHttpRequest',
      },
      body: form.toString(),
      credentials: 'include',
    });
  } catch (e: any) {
    throw new ApiError(`Network error: ${e?.message ?? 'could not reach server'}`);
  }

  return parseResponse(res);
}

/**
 * Call an AJAX action that uploads one or more files (multipart/form-data).
 * `files` maps the PHP $_FILES field name to a picked file descriptor.
 */
export interface UploadFile {
  uri: string;
  name: string;
  type: string;
}

export async function apiUpload(
  action: string,
  params: Record<string, any> = {},
  files: Record<string, UploadFile> = {},
): Promise<ApiResponse> {
  const baseUrl = await getBaseUrl();
  if (!baseUrl) {
    throw new ApiError('Server URL is not configured yet. Set it in Settings.');
  }

  const form = new FormData();
  form.append('action', action);
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null) return;
    form.append(key, typeof value === 'object' ? JSON.stringify(value) : String(value));
  });
  Object.entries(files).forEach(([fieldName, file]) => {
    // @ts-ignore React Native FormData file shape
    form.append(fieldName, { uri: file.uri, name: file.name, type: file.type });
  });

  let res: Response;
  try {
    res = await fetch(endpoint(baseUrl), {
      method: 'POST',
      headers: {
        Accept: 'application/json',
        'X-Requested-With': 'XMLHttpRequest',
      },
      body: form,
      credentials: 'include',
    });
  } catch (e: any) {
    throw new ApiError(`Network error: ${e?.message ?? 'could not reach server'}`);
  }

  return parseResponse(res);
}

async function parseResponse(res: Response): Promise<ApiResponse> {
  const text = await res.text();
  let json: ApiResponse;
  try {
    json = JSON.parse(text);
  } catch {
    const preview = text ? text.replace(/<[^>]*>?/gm, ' ').replace(/\s+/g, ' ').trim().slice(0, 100) : '';
    throw new ApiError(
      `Server returned a non-JSON response (HTTP ${res.status})${preview ? `: "${preview}..."` : ''}. Check the server URL in Settings.`,
      res.status,
    );
  }
  if (!res.ok && !json.status) {
    throw new ApiError(json.message ?? `Request failed (HTTP ${res.status})`, res.status);
  }
  return json;
}

/** Persist the session cookies now (call after a successful login). */
export async function persistSessionCookies(): Promise<void> {
  const baseUrl = await getBaseUrl();
  if (!baseUrl) return;
  try {
    const cookies = await CookieManager.get(baseUrl);
    await AsyncStorage.setItem('apktool.cookies', JSON.stringify(cookies));
  } catch {
    // non-fatal – cookie persistence is best-effort
  }
}

/** Clear cookies + cached session on logout. */
export async function clearSession(): Promise<void> {
  try {
    await CookieManager.clearAll();
  } catch {
    // ignore
  }
  await AsyncStorage.removeItem('apktool.cookies');
  await AsyncStorage.removeItem('apktool.user');
}

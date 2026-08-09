export interface User {
  id?: number;
  username: string;
  user_type: 'user' | 'admin';
}

export interface ProjectSummary {
  project_id: string;
  project_name: string;
  created_at?: string;
  updated_at?: string;
  [key: string]: any;
}

export interface WorkflowState {
  project_id?: string;
  project_name?: string;
  project_path?: string;
  project_root?: string;
  source_apk?: string;
  unsigned_apk?: string;
  signed_apk?: string;
  keystore_path?: string;
  keystore_alias?: string;
  logo_preview_name?: string;
  logo_preview_path?: string;
  logo_version?: number;
  editor_file?: EditorFile;
  last_build_log?: string;
  last_build_failed?: boolean;
  ai_fix?: {
    file: string;
    explanation?: string;
    fixed_content: string | null;
    has_fix: boolean;
    applied?: boolean;
  };
  cloud_logging_enabled?: boolean;
  [key: string]: any;
}

export interface EditorFile {
  path: string;
  content?: string;
  binary?: boolean;
  [key: string]: any;
}

export interface DirItem {
  name: string;
  path: string;
  is_dir: boolean;
  size: number;
}

export interface Limits {
  decompile_usage?: number;
  decompile_limit?: number;
  compile_usage?: number;
  compile_limit?: number;
  generate_key_usage?: number;
  generate_key_limit?: number;
  sign_apk_usage?: number;
  sign_apk_limit?: number;
  max_upload_bytes?: number;
  [key: string]: any;
}

export interface Keystore {
  id: number;
  file_name: string;
  key_alias: string;
  created_at?: string;
}

export interface AdbDevice {
  serial: string;
  state?: string;
  [key: string]: any;
}

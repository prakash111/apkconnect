import { apiCall } from './client';

// --- Users & limits ---
export async function getUsers() {
  return apiCall('get_users');
}

export interface NewUserInput {
  email: string;
  mobile?: string;
  username: string;
  password: string;
  user_type?: 'user' | 'admin';
  decompile_limit?: number;
  compile_limit?: number;
  generate_key_limit?: number;
  sign_apk_limit?: number;
}

export async function createUser(input: NewUserInput) {
  return apiCall('create_user', input);
}

export async function updateLimits(
  user_id: number,
  limits: {
    decompile_limit: number;
    compile_limit: number;
    generate_key_limit: number;
    sign_apk_limit: number;
  },
) {
  return apiCall('update_limits', { user_id, ...limits });
}

// --- Contact inquiries ---
export async function getContactInquiries() {
  return apiCall('get_contact_inquiries');
}

export async function deleteContactInquiry(id: number) {
  return apiCall('delete_contact_inquiry', { id });
}

export async function markContactInquiryRead(id: number) {
  return apiCall('mark_contact_inquiry_read', { id });
}

// --- Blogs ---
export interface BlogInput {
  id?: number;
  title: string;
  slug?: string;
  excerpt?: string;
  content: string;
  category?: string;
  tags?: string;
  read_time?: string;
  image_url?: string;
}

export async function getAdminBlogs() {
  return apiCall('get_admin_blogs');
}

export async function saveAdminBlog(blog: BlogInput) {
  return apiCall('save_admin_blog', blog);
}

export async function deleteAdminBlog(id: number) {
  return apiCall('delete_admin_blog', { id });
}

// --- FAQs ---
export interface FaqInput {
  id?: number;
  question: string;
  answer: string;
  category?: string;
  sort_order?: number;
  is_active?: 0 | 1;
}

export async function getFaqs() {
  return apiCall('get_faqs');
}

export async function getAdminFaqs() {
  return apiCall('get_admin_faqs');
}

export async function saveAdminFaq(faq: FaqInput) {
  return apiCall('save_admin_faq', faq);
}

export async function deleteAdminFaq(id: number) {
  return apiCall('delete_admin_faq', { id });
}

// --- Backup settings (GitHub / cloud backup of the whole SaaS install) ---
export interface BackupSettings {
  github_backup_repo_owner: string;
  github_backup_repo_name: string;
  github_backup_branch: string;
  github_backup_upload_dir: string;
  github_backup_token: string;
  auto_backup_enabled: string;
  auto_backup_frequency: string;
  last_github_backup?: string;
  last_github_backup_file?: string;
}

export async function getAdminBackupSettings() {
  return apiCall('get_admin_backup_settings');
}

export async function saveAdminBackupSettings(settings: Partial<BackupSettings>) {
  return apiCall('save_admin_backup_settings', settings);
}

export async function runAdminManualBackup() {
  return apiCall('run_admin_manual_backup');
}

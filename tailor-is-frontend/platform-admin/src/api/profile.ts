import request from './request';

export interface AdminProfile {
  id: number;
  username: string;
  realName: string;
  phone: string;
  email: string;
  avatar: string;
}

export interface ProfileUpdateForm {
  realName: string;
  phone: string;
  email: string;
}

export interface PasswordUpdateForm {
  oldPassword: string;
  newPassword: string;
}

const mockProfile: AdminProfile = {
  id: 1,
  username: 'admin',
  realName: '系统管理员',
  phone: '13800138000',
  email: 'admin@tailoris.com',
  avatar: '',
};

export async function getProfile(): Promise<AdminProfile> {
  try {
    return await request<unknown, AdminProfile>({
      url: '/admin/profile',
      method: 'get',
      showError: false,
    });
  } catch {
    return mockProfile;
  }
}

export async function updateProfile(data: ProfileUpdateForm): Promise<AdminProfile> {
  try {
    return await request<unknown, AdminProfile>({
      url: '/admin/profile',
      method: 'put',
      data,
      showError: false,
    });
  } catch {
    return { ...mockProfile, ...data };
  }
}

export async function updatePassword(data: PasswordUpdateForm): Promise<void> {
  try {
    await request<unknown, void>({
      url: '/admin/profile/password',
      method: 'put',
      data,
      showError: false,
    });
  } catch {
    // Mock success
  }
}
export const API_BASE = '/api/v1';

export const API_PATHS = {
  auth: {
    login: `${API_BASE}/auth/login`,
    logout: `${API_BASE}/auth/logout`,
    userInfo: `${API_BASE}/auth/userinfo`,
  },
  admin: {
    dashboard: {
      stats: `${API_BASE}/admin/dashboard/stats`,
    },
    user: {
      base: `${API_BASE}/admin/users`,
      byId: (id: number) => `${API_BASE}/admin/users/${id}`,
      status: (id: number) => `${API_BASE}/admin/users/${id}/status`,
      password: (id: number) => `${API_BASE}/admin/users/${id}/password/reset`,
      roles: (id: number) => `${API_BASE}/admin/users/${id}/roles`,
      allRoles: `${API_BASE}/admin/roles/all`,
    },
    role: {
      base: `${API_BASE}/admin/roles`,
      byId: (id: number) => `${API_BASE}/admin/roles/${id}`,
      permissions: (id: number) => `${API_BASE}/admin/roles/${id}/permissions`,
    },
    order: {
      base: `${API_BASE}/admin/order`,
      byId: (id: number) => `${API_BASE}/admin/order/${id}`,
      status: (id: number) => `${API_BASE}/admin/order/${id}/status`,
    },
    product: {
      base: `${API_BASE}/admin/product`,
      byId: (id: number) => `${API_BASE}/admin/product/${id}`,
    },
    merchant: {
      base: `${API_BASE}/admin/merchant`,
      byId: (id: number) => `${API_BASE}/admin/merchant/${id}`,
    },
    community: {
      base: `${API_BASE}/admin/community`,
      byId: (id: number) => `${API_BASE}/admin/community/${id}`,
    },
    settings: {
      base: `${API_BASE}/admin/system/settings`,
      basic: `${API_BASE}/admin/system/settings/basic`,
      security: `${API_BASE}/admin/system/settings/security`,
      thirdParty: `${API_BASE}/admin/system/settings/third-party`,
    },
    auditLog: {
      base: `${API_BASE}/admin/audit-logs`,
    },
  },
};

export default API_PATHS;

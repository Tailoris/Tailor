export const API_BASE = '/api/v1'

export const API_PATHS = {
  auth: {
    login: `${API_BASE}/auth/login`,
    logout: `${API_BASE}/auth/logout`,
    userInfo: `${API_BASE}/auth/userinfo`
  },
  admin: {
    dashboard: {
      overview: `${API_BASE}/admin/dashboard/overview`,
      statistics: `${API_BASE}/admin/dashboard/statistics`,
      charts: `${API_BASE}/admin/dashboard/charts`
    },
    user: {
      base: `${API_BASE}/admin/users`,
      byId: (id: number) => `${API_BASE}/admin/users/${id}`,
      status: (id: number) => `${API_BASE}/admin/users/${id}/status`,
      password: (id: number) => `${API_BASE}/admin/users/${id}/password/reset`,
      roles: (id: number) => `${API_BASE}/admin/users/${id}/roles`,
      allRoles: `${API_BASE}/admin/roles/all`
    },
    role: {
      base: `${API_BASE}/admin/roles`,
      byId: (id: number) => `${API_BASE}/admin/roles/${id}`,
      permissions: (id: number) => `${API_BASE}/admin/roles/${id}/permissions`
    },
    order: {
      base: `${API_BASE}/admin/order`,
      byId: (id: number) => `${API_BASE}/admin/order/${id}`,
      status: (id: number) => `${API_BASE}/admin/order/${id}/status`
    },
    product: {
      base: `${API_BASE}/admin/product`,
      byId: (id: number) => `${API_BASE}/admin/product/${id}`
    },
    merchant: {
      base: `${API_BASE}/admin/merchant`,
      byId: (id: number) => `${API_BASE}/admin/merchant/${id}`
    },
    community: {
      base: `${API_BASE}/admin/community`,
      byId: (id: number) => `${API_BASE}/admin/community/${id}`
    },
    settings: {
      base: `${API_BASE}/admin/settings`,
      system: `${API_BASE}/admin/settings/system`,
      security: `${API_BASE}/admin/settings/security`,
      notification: `${API_BASE}/admin/settings/notification`
    },
    auditLog: {
      base: `${API_BASE}/admin/audit-log`,
      list: `${API_BASE}/admin/audit-log/list`
    }
  }
}

export default API_PATHS
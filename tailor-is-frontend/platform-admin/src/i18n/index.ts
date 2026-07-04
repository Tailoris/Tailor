import { createI18n } from 'vue-i18n'

/**
 * 平台管理后台国际化配置
 * 🔒 M-20修复: 引入 vue-i18n，支持中英文切换
 */

const messages = {
  'zh-CN': {
    common: {
      confirm: '确认',
      cancel: '取消',
      save: '保存',
      delete: '删除',
      edit: '编辑',
      add: '新增',
      search: '搜索',
      reset: '重置',
      submit: '提交',
      back: '返回',
      loading: '加载中...',
      success: '操作成功',
      failed: '操作失败',
      confirmDelete: '确认删除？',
      yes: '是',
      no: '否',
      noData: '暂无数据',
      detail: '详情',
      export: '导出',
      permissions: '权限'
    },
    nav: {
      dashboard: '仪表盘',
      system: '系统管理',
      login: '登录',
      logout: '退出登录'
    },
    auth: {
      username: '用户名',
      password: '密码',
      loginTitle: '平台管理登录',
      forgotPassword: '忘记密码',
      rememberMe: '记住我'
    },
    menu: {
      dashboard: '仪表盘',
      userManage: '用户管理',
      roleManage: '角色管理',
      auditLog: '操作日志',
      settings: '系统设置'
    },
    dashboard: {
      title: '仪表盘',
      totalUsers: '总用户数',
      totalOrders: '总订单数',
      totalRevenue: '总收入',
      activeModules: '活跃模块',
      userGrowth: '用户增长',
      orderGrowth: '订单增长',
      revenueGrowth: '收入增长',
      moduleStatus: '模块状态',
      revenueTrend: '收入趋势'
    },
    user: {
      title: '用户管理',
      username: '用户名',
      nickname: '昵称',
      phone: '手机号',
      email: '邮箱',
      status: '状态',
      createTime: '创建时间',
      active: '正常',
      disabled: '禁用',
      assignRole: '分配角色',
      toggleStatus: '切换状态',
      resetPassword: '重置密码'
    },
    role: {
      title: '角色管理',
      name: '角色名称',
      code: '角色编码',
      description: '描述',
      userCount: '关联用户数',
      addRole: '新增角色',
      editRole: '编辑角色'
    },
    auditLog: {
      title: '操作日志',
      operator: '操作人',
      actionType: '操作类型',
      target: '操作对象',
      description: '操作描述',
      ip: 'IP地址',
      userAgent: 'User-Agent',
      requestParams: '请求参数',
      createTime: '操作时间',
      exportLogs: '导出日志',
      login: '登录',
      create: '创建',
      update: '更新',
      delete: '删除',
      export: '导出',
      permission: '权限变更'
    },
    settings: {
      title: '系统设置',
      general: '通用设置',
      security: '安全设置',
      notification: '通知设置'
    },
    error: {
      networkError: '网络连接失败',
      serverError: '服务器错误',
      unauthorized: '未登录',
      forbidden: '无权限',
      notFound: '资源不存在',
      timeout: '请求超时'
    }
  },
  'en-US': {
    common: {
      confirm: 'Confirm',
      cancel: 'Cancel',
      save: 'Save',
      delete: 'Delete',
      edit: 'Edit',
      add: 'Add',
      search: 'Search',
      reset: 'Reset',
      submit: 'Submit',
      back: 'Back',
      loading: 'Loading...',
      success: 'Success',
      failed: 'Failed',
      confirmDelete: 'Confirm delete?',
      yes: 'Yes',
      no: 'No',
      noData: 'No Data',
      detail: 'Detail',
      export: 'Export',
      permissions: 'Permissions'
    },
    nav: {
      dashboard: 'Dashboard',
      system: 'System',
      login: 'Login',
      logout: 'Logout'
    },
    auth: {
      username: 'Username',
      password: 'Password',
      loginTitle: 'Platform Admin Login',
      forgotPassword: 'Forgot Password',
      rememberMe: 'Remember Me'
    },
    menu: {
      dashboard: 'Dashboard',
      userManage: 'Users',
      roleManage: 'Roles',
      auditLog: 'Audit Logs',
      settings: 'Settings'
    },
    dashboard: {
      title: 'Dashboard',
      totalUsers: 'Total Users',
      totalOrders: 'Total Orders',
      totalRevenue: 'Total Revenue',
      activeModules: 'Active Modules',
      userGrowth: 'User Growth',
      orderGrowth: 'Order Growth',
      revenueGrowth: 'Revenue Growth',
      moduleStatus: 'Module Status',
      revenueTrend: 'Revenue Trend'
    },
    user: {
      title: 'User Management',
      username: 'Username',
      nickname: 'Nickname',
      phone: 'Phone',
      email: 'Email',
      status: 'Status',
      createTime: 'Create Time',
      active: 'Active',
      disabled: 'Disabled',
      assignRole: 'Assign Role',
      toggleStatus: 'Toggle Status',
      resetPassword: 'Reset Password'
    },
    role: {
      title: 'Role Management',
      name: 'Role Name',
      code: 'Role Code',
      description: 'Description',
      userCount: 'User Count',
      addRole: 'Add Role',
      editRole: 'Edit Role'
    },
    auditLog: {
      title: 'Audit Logs',
      operator: 'Operator',
      actionType: 'Action Type',
      target: 'Target',
      description: 'Description',
      ip: 'IP Address',
      userAgent: 'User-Agent',
      requestParams: 'Request Params',
      createTime: 'Create Time',
      exportLogs: 'Export Logs',
      login: 'Login',
      create: 'Create',
      update: 'Update',
      delete: 'Delete',
      export: 'Export',
      permission: 'Permission'
    },
    settings: {
      title: 'Settings',
      general: 'General',
      security: 'Security',
      notification: 'Notification'
    },
    error: {
      networkError: 'Network Error',
      serverError: 'Server Error',
      unauthorized: 'Unauthorized',
      forbidden: 'Forbidden',
      notFound: 'Not Found',
      timeout: 'Request Timeout'
    }
  }
}

const i18n = createI18n({
  legacy: false,
  locale: localStorage.getItem('language') || 'zh-CN',
  fallbackLocale: 'zh-CN',
  messages
})

export default i18n

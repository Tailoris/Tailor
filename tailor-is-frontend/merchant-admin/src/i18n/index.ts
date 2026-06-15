import { createI18n } from 'vue-i18n'

/**
 * 商家后台国际化配置 - 修复 F-M04
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
      no: '否'
    },
    menu: {
      dashboard: '仪表盘',
      product: '商品管理',
      order: '订单管理',
      merchant: '店铺管理',
      statistics: '数据统计',
      settings: '系统设置'
    },
    product: {
      name: '商品名称',
      price: '价格',
      stock: '库存',
      status: '状态',
      onShelf: '已上架',
      offShelf: '已下架',
      draft: '草稿'
    },
    order: {
      orderNo: '订单号',
      amount: '金额',
      status: '状态',
      createTime: '创建时间',
      pendingPay: '待支付',
      paid: '已支付',
      shipped: '已发货',
      completed: '已完成',
      cancelled: '已取消'
    },
    error: {
      networkError: '网络连接失败',
      serverError: '服务器错误',
      unauthorized: '未登录',
      forbidden: '无权限',
      notFound: '资源不存在'
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
      no: 'No'
    },
    menu: {
      dashboard: 'Dashboard',
      product: 'Products',
      order: 'Orders',
      merchant: 'Store',
      statistics: 'Statistics',
      settings: 'Settings'
    },
    product: {
      name: 'Product Name',
      price: 'Price',
      stock: 'Stock',
      status: 'Status',
      onShelf: 'On Shelf',
      offShelf: 'Off Shelf',
      draft: 'Draft'
    },
    order: {
      orderNo: 'Order No.',
      amount: 'Amount',
      status: 'Status',
      createTime: 'Create Time',
      pendingPay: 'Pending Payment',
      paid: 'Paid',
      shipped: 'Shipped',
      completed: 'Completed',
      cancelled: 'Cancelled'
    },
    error: {
      networkError: 'Network Error',
      serverError: 'Server Error',
      unauthorized: 'Unauthorized',
      forbidden: 'Forbidden',
      notFound: 'Not Found'
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

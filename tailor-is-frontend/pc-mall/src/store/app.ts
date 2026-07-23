import { defineStore } from 'pinia';
import { ref, computed } from 'vue';

export interface ToastMessage {
  id: number;
  type: 'success' | 'error' | 'warning' | 'info';
  message: string;
  duration?: number;
}

export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref(false);
  const isGlobalLoading = ref(false);
  const toasts = ref<ToastMessage[]>([]);
  let toastIdCounter = 0;

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value;
  }

  function setGlobalLoading(loading: boolean) {
    isGlobalLoading.value = loading;
  }

  function showToast(message: string, type: ToastMessage['type'] = 'info', duration = 3000) {
    const id = ++toastIdCounter;
    const toast: ToastMessage = { id, type, message, duration };
    toasts.value.push(toast);
    
    if (duration > 0) {
      setTimeout(() => {
        removeToast(id);
      }, duration);
    }
    
    return id;
  }

  function showSuccess(message: string, duration = 3000) {
    return showToast(message, 'success', duration);
  }

  function showError(message: string, duration = 3000) {
    return showToast(message, 'error', duration);
  }

  function showWarning(message: string, duration = 3000) {
    return showToast(message, 'warning', duration);
  }

  function showInfo(message: string, duration = 3000) {
    return showToast(message, 'info', duration);
  }

  function removeToast(id: number) {
    const index = toasts.value.findIndex((t) => t.id === id);
    if (index > -1) {
      toasts.value.splice(index, 1);
    }
  }

  function clearToasts() {
    toasts.value = [];
  }

  const successToasts = computed(() => toasts.value.filter((t) => t.type === 'success'));
  const errorToasts = computed(() => toasts.value.filter((t) => t.type === 'error'));
  const warningToasts = computed(() => toasts.value.filter((t) => t.type === 'warning'));
  const infoToasts = computed(() => toasts.value.filter((t) => t.type === 'info'));

  return {
    sidebarCollapsed,
    isGlobalLoading,
    toasts,
    successToasts,
    errorToasts,
    warningToasts,
    infoToasts,
    toggleSidebar,
    setGlobalLoading,
    showToast,
    showSuccess,
    showError,
    showWarning,
    showInfo,
    removeToast,
    clearToasts,
  };
});
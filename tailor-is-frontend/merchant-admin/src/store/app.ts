import { defineStore } from 'pinia'
import { ref, onMounted, onUnmounted } from 'vue'

export interface BreadcrumbItem {
  title: string
  path?: string
}

export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref(false)
  const breadcrumbs = ref<BreadcrumbItem[]>([])
  const device = ref<'xs' | 'sm' | 'md' | 'lg' | 'xl' | 'xxl'>('lg')

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  function setBreadcrumbs(items: BreadcrumbItem[]) {
    breadcrumbs.value = items
  }

  function updateDevice() {
    const width = window.innerWidth
    if (width < 576) device.value = 'xs'
    else if (width < 768) device.value = 'sm'
    else if (width < 992) device.value = 'md'
    else if (width < 1200) device.value = 'lg'
    else if (width < 2560) device.value = 'xl'
    else device.value = 'xxl'
  }

  function initDeviceListener() {
    updateDevice()
    window.addEventListener('resize', updateDevice)
  }

  function destroyDeviceListener() {
    window.removeEventListener('resize', updateDevice)
  }

  return {
    sidebarCollapsed,
    breadcrumbs,
    device,
    toggleSidebar,
    setBreadcrumbs,
    updateDevice,
    initDeviceListener,
    destroyDeviceListener,
  }
})

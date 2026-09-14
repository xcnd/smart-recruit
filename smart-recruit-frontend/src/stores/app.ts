import { defineStore } from 'pinia'
import { ref } from 'vue'
import { useSettingsStore } from './settings'

export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref(false)
  const loading = ref(false)
  const currentPageTitle = ref('')

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  function setPageTitle(title: string) {
    currentPageTitle.value = title
    const systemName = useSettingsStore().systemName
    document.title = title ? `${title} - ${systemName}` : `${systemName} - 智能招聘管理系统`
  }

  return {
    sidebarCollapsed,
    loading,
    currentPageTitle,
    toggleSidebar,
    setPageTitle,
  }
})

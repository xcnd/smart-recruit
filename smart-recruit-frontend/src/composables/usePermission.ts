import { useUserStore } from '@/stores/user'

export function usePermission() {
  const userStore = useUserStore()

  function hasPermission(perm: string): boolean {
    return userStore.hasPermission(perm)
  }

  function hasAnyPermission(perms: string[]): boolean {
    return perms.some((p) => userStore.hasPermission(p))
  }

  function hasAllPermissions(perms: string[]): boolean {
    return perms.every((p) => userStore.hasPermission(p))
  }

  return {
    hasPermission,
    hasAnyPermission,
    hasAllPermissions,
  }
}

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, phoneLogin as phoneLoginApi, getMe } from '@/api/auth'
import {
  getToken, setToken, removeToken,
  getUserInfo, setUserInfo, removeUserInfo,
  getRefreshToken, setRefreshToken, removeRefreshToken,
  getRemember,
} from '@/utils/storage'
import type { UserVO } from '@/types/models'

export const useUserStore = defineStore('user', () => {
  const remember = ref(getRemember())
  const token = ref<string>(getToken() || '')
  const refreshToken = ref<string>(getRefreshToken() || '')
  const userInfo = ref<UserVO | null>(getUserInfo() as UserVO | null)
  const permissions = ref<string[]>([])
  const initialized = ref(false)

  const isLoggedIn = computed(() => !!token.value)
  const userName = computed(() => userInfo.value?.realName || userInfo.value?.name || '')
  const userAvatar = computed(() => userInfo.value?.avatar || '')

  /** 求职者角色 ID（与后端 Constants.DEFAULT_ROLE_CANDIDATE_ID 对应） */
  const CANDIDATE_ROLE_ID = '300008'

  /** 当前用户是否是求职者（仅有角色 300008，无其他员工/管理员角色） */
  const isCandidate = computed(() => {
    const roleIds = userInfo.value?.roleIds
    if (!roleIds) return false
    const ids = roleIds.split(',').map(id => id.trim()).filter(Boolean)
    return ids.length === 1 && ids[0] === CANDIDATE_ROLE_ID
  })

  async function phoneLogin(mobile: string, verificationCode: string, rememberMe = false) {
    const res = await phoneLoginApi({ mobile, verificationCode })
    token.value = res.accessToken
    refreshToken.value = res.refreshToken
    userInfo.value = res.userInfo
    permissions.value = res.userInfo.permissions || []
    remember.value = rememberMe
    initialized.value = true

    setToken(res.accessToken, rememberMe)
    setRefreshToken(res.refreshToken, rememberMe)
    setUserInfo(res.userInfo as unknown as Record<string, unknown>, rememberMe)
    return res
  }

  async function login(account: string, password: string, captchaId?: string, captchaCode?: string, rememberMe = false) {
    const res = await loginApi({ account, password, captchaId, captchaCode })
    token.value = res.accessToken
    refreshToken.value = res.refreshToken
    userInfo.value = res.userInfo
    permissions.value = res.userInfo.permissions || []
    remember.value = rememberMe
    initialized.value = true

    setToken(res.accessToken, rememberMe)
    setRefreshToken(res.refreshToken, rememberMe)
    setUserInfo(res.userInfo as unknown as Record<string, unknown>, rememberMe)
    return res
  }

  async function fetchUserInfo() {
    try {
      const user = await getMe()
      userInfo.value = user
      permissions.value = user.permissions || []
      initialized.value = true
      setUserInfo(user as unknown as Record<string, unknown>, remember.value)
    } catch {
      logout()
    }
  }

  function logout() {
    token.value = ''
    refreshToken.value = ''
    userInfo.value = null
    permissions.value = []
    initialized.value = false
    remember.value = false
    removeToken()
    removeRefreshToken()
    removeUserInfo()
  }

  function hasPermission(perm: string): boolean {
    if (!permissions.value.length) return false
    if (permissions.value.includes('*:*:*')) return true
    return permissions.value.includes(perm)
  }

  function updateAvatar(url: string) {
    if (userInfo.value) {
      userInfo.value.avatar = url
      setUserInfo(userInfo.value as unknown as Record<string, unknown>, remember.value)
    }
  }

  function updateEmail(email: string) {
    if (userInfo.value) {
      userInfo.value.email = email
      setUserInfo(userInfo.value as unknown as Record<string, unknown>, remember.value)
    }
  }

  function updateRealName(name: string) {
    if (userInfo.value) {
      userInfo.value.realName = name
      setUserInfo(userInfo.value as unknown as Record<string, unknown>, remember.value)
    }
  }

  return {
    token,
    refreshToken,
    userInfo,
    permissions,
    remember,
    initialized,
    isLoggedIn,
    isCandidate,
    userName,
    userAvatar,
    login,
    phoneLogin,
    fetchUserInfo,
    logout,
    hasPermission,
    updateAvatar,
    updateEmail,
    updateRealName,
  }
})

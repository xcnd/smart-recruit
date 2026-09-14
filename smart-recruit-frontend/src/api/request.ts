import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage, ElNotification } from 'element-plus'
import {
  getToken,
  getRefreshToken,
  removeToken,
  removeRefreshToken,
  removeUserInfo,
  updateToken,
  updateRefreshToken,
  setRedirectPath,
} from '@/utils/storage'
import type { LoginResultVO } from '@/types/models'

const request = axios.create({
  baseURL: '/api/v1',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// ---- Silent token refresh state ----

let isRefreshing = false
let refreshSubscribers: Array<(token: string) => void> = []
let isLoggingOut = false
let lastLogoutTime = 0

function subscribeToRefresh(cb: (token: string) => void) {
  refreshSubscribers.push(cb)
}

function broadcastNewToken(token: string) {
  refreshSubscribers.forEach((cb) => cb(token))
  refreshSubscribers = []
}

// ---- Force logout (production-grade) ----

function forceLogout() {
  // 防抖：1 秒内不重复触发
  const now = Date.now()
  if (isLoggingOut && now - lastLogoutTime < 1000) return
  isLoggingOut = true
  lastLogoutTime = now

  // 保存当前页面路径，供登录后跳回
  const currentPath = window.location.pathname + window.location.search
  if (currentPath !== '/login' && !currentPath.startsWith('/login?')) {
    setRedirectPath(currentPath)
  }

  // 清除本地登录态
  removeToken()
  removeRefreshToken()
  removeUserInfo()
  broadcastNewToken('') // 清空队列（reject）

  // 使用 ElNotification 代替 ElMessage，更符合大厂产品体验
  ElNotification({
    title: '登录已过期',
    message: '为了您的账户安全，请重新登录',
    type: 'warning',
    duration: 4500,
    offset: 64,
  })

  // 延迟跳转，给用户阅读通知的时间
  setTimeout(() => {
    const redirect = currentPath !== '/login' ? `?redirect=${encodeURIComponent(currentPath)}` : ''
    window.location.href = `/login${redirect}`
  }, 1500)
}

// ---- Silent token refresh ----

/**
 * 使用 refresh token 静默续期，成功后重试原请求。
 * 同时发起的多个 401 请求会被排队，待 refresh 完成后批量重试。
 */
async function tryRefreshAndRetry(
  originalConfig: InternalAxiosRequestConfig
): Promise<any> {
  // 已经在刷新中：排队等待
  if (isRefreshing) {
    return new Promise((resolve) => {
      subscribeToRefresh((newToken: string) => {
        originalConfig.headers.Authorization = `Bearer ${newToken}`
        resolve(request(originalConfig))
      })
    })
  }

  isRefreshing = true

  try {
    const rt = getRefreshToken()
    if (!rt) {
      // 没有 refresh token，直接登出
      forceLogout()
      return Promise.reject(new Error('No refresh token available'))
    }

    // 使用原始 axios 实例刷新 token（绕过拦截器，避免无限循环）
    const resp = await axios.post('/api/v1/auth/refresh', null, {
      params: { refreshToken: rt },
    })

    const apiResp = resp.data
    if (apiResp.code !== 200 && apiResp.code !== 0) {
      throw new Error(apiResp.message || 'Token refresh failed')
    }

    const loginResult: LoginResultVO = apiResp.data

    // 原地更新 storage（保持 "记住我" 偏好不变）
    updateToken(loginResult.accessToken)
    updateRefreshToken(loginResult.refreshToken)

    // 通知所有排队请求使用新 token 重试
    broadcastNewToken(loginResult.accessToken)

    // 重试当前请求
    originalConfig.headers.Authorization = `Bearer ${loginResult.accessToken}`
    return request(originalConfig)
  } catch {
    // 刷新失败（refresh token 也过期/无效）→ 强制登出
    broadcastNewToken('') // 清空队列
    forceLogout()
    return Promise.reject(new Error('Token refresh failed, logging out'))
  } finally {
    isRefreshing = false
  }
}

// ---- Request interceptor ----

request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error: AxiosError) => {
    return Promise.reject(error)
  }
)

// ---- Response interceptor ----

request.interceptors.response.use(
  (response) => {
    const res = response.data

    // Direct download or non-standard response
    if (response.config.responseType === 'blob' || !res || res.code === undefined) {
      return response
    }

    if (res.code === 200 || res.code === 0) {
      return res.data
    }

    // 认证失败 → 尝试静默刷新 token
    if (res.code === 40003 || res.code === 40004) {
      return tryRefreshAndRetry(response.config)
    }

    // 重复投递 → 交由业务组件自行展示，不在此弹出全局通知
    if (res.code === 40901) {
      const err = new Error(res.message || 'Duplicate resource') as Error & { code?: number }
      err.code = res.code
      return Promise.reject(err)
    }

    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  (error: AxiosError<{ code?: number; message?: string }>) => {
    if (error.response) {
      const { status, data } = error.response

      // HTTP 401 → 尝试静默刷新 token
      if (status === 401) {
        // 排除登录接口自身的 401（邮箱/密码错误）
        if (error.config?.url?.includes('/auth/login')) {
          ElMessage.error(data?.message || '登录失败，请检查邮箱和密码')
          return Promise.reject(error)
        }
        return tryRefreshAndRetry(error.config!)
      }

      if (status === 403) {
        ElMessage.error(data?.message || '没有访问权限')
        return Promise.reject(error)
      }

      if (data?.message) {
        ElMessage.error(data.message)
      } else {
        ElMessage.error(`请求失败 (${status})`)
      }
    } else if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请稍后重试')
    } else {
      ElMessage.error('网络异常，请检查网络连接')
    }

    return Promise.reject(error)
  }
)

export default request

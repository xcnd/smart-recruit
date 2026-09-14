const TOKEN_KEY = 'sr_token'
const USER_KEY = 'sr_user'
const REFRESH_KEY = 'sr_refresh'
const REMEMBER_KEY = 'sr_remember'
const REDIRECT_KEY = 'sr_redirect'

// ---- Remember flag ----

export function getRemember(): boolean {
  return localStorage.getItem(REMEMBER_KEY) === 'true'
}

function setRememberFlag(remember: boolean): void {
  localStorage.setItem(REMEMBER_KEY, String(remember))
}

// ---- Token ----

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY) || sessionStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string, remember: boolean): void {
  const storage = remember ? localStorage : sessionStorage
  storage.setItem(TOKEN_KEY, token)
  setRememberFlag(remember)
}

export function removeToken(): void {
  localStorage.removeItem(TOKEN_KEY)
  sessionStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(REMEMBER_KEY)
}

export function updateToken(token: string): void {
  if (localStorage.getItem(TOKEN_KEY)) {
    localStorage.setItem(TOKEN_KEY, token)
  } else if (sessionStorage.getItem(TOKEN_KEY)) {
    sessionStorage.setItem(TOKEN_KEY, token)
  }
}

// ---- Refresh Token ----

export function getRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_KEY) || sessionStorage.getItem(REFRESH_KEY)
}

export function setRefreshToken(refreshToken: string, remember: boolean): void {
  const storage = remember ? localStorage : sessionStorage
  storage.setItem(REFRESH_KEY, refreshToken)
}

export function removeRefreshToken(): void {
  localStorage.removeItem(REFRESH_KEY)
  sessionStorage.removeItem(REFRESH_KEY)
}

export function updateRefreshToken(token: string): void {
  if (localStorage.getItem(REFRESH_KEY)) {
    localStorage.setItem(REFRESH_KEY, token)
  } else if (sessionStorage.getItem(REFRESH_KEY)) {
    sessionStorage.setItem(REFRESH_KEY, token)
  }
}

// ---- User Info ----

export function getUserInfo(): Record<string, unknown> | null {
  const raw = localStorage.getItem(USER_KEY) || sessionStorage.getItem(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch {
    return null
  }
}

export function setUserInfo(user: Record<string, unknown>, remember: boolean): void {
  const storage = remember ? localStorage : sessionStorage
  storage.setItem(USER_KEY, JSON.stringify(user))
}

export function removeUserInfo(): void {
  localStorage.removeItem(USER_KEY)
  sessionStorage.removeItem(USER_KEY)
}

// ---- Redirect Path ----

export function getRedirectPath(): string | null {
  return sessionStorage.getItem(REDIRECT_KEY)
}

export function setRedirectPath(path: string): void {
  sessionStorage.setItem(REDIRECT_KEY, path)
}

export function removeRedirectPath(): void {
  sessionStorage.removeItem(REDIRECT_KEY)
}

// ---- Clear All ----

export function clearAll(): void {
  removeToken()
  removeRefreshToken()
  removeUserInfo()
}

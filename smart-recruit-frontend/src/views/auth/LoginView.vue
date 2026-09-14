<template>
  <div class="auth-container">
    <!-- Left Brand Panel -->
    <aside class="brand-panel">
      <div class="brand-content">
        <div class="brand-header">
          <div class="brand-icon">
            <svg viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg">
              <rect width="40" height="40" rx="10" fill="white" fill-opacity="0.15"/>
              <path d="M12 28V12l8 12L28 12v16" stroke="white" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" fill="none"/>
            </svg>
          </div>
          <h1 class="brand-name">{{ settingsStore.systemName }}</h1>
          <p class="brand-tagline">AI 驱动的智能招聘平台</p>
        </div>

        <div class="features-list">
          <div class="feature-item">
            <div class="feature-icon">
              <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
                <path d="M16.667 5L7.5 14.167L3.333 10" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </div>
            <span>AI 智能简历解析与人才匹配</span>
          </div>
          <div class="feature-item">
            <div class="feature-icon">
              <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
                <path d="M16.667 5L7.5 14.167L3.333 10" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </div>
            <span>全流程招聘管理，从职位发布到入职</span>
          </div>
          <div class="feature-item">
            <div class="feature-icon">
              <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
                <path d="M16.667 5L7.5 14.167L3.333 10" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </div>
            <span>数据驱动决策，实时招聘分析看板</span>
          </div>
          <div class="feature-item">
            <div class="feature-icon">
              <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
                <path d="M16.667 5L7.5 14.167L3.333 10" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </div>
            <span>企业级安全保障，数据加密与权限管控</span>
          </div>
        </div>
      </div>

      <div class="brand-footer">
        <p v-html="settingsStore.copyrightText"></p>
      </div>

      <!-- Decorative grid -->
      <div class="bg-grid"></div>
    </aside>

    <!-- Right Form Panel -->
    <main class="form-panel">
      <div class="form-wrapper">
        <div class="form-header">
          <h2 class="form-title">欢迎回来</h2>
          <p class="form-subtitle">请登录您的账号以继续</p>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          size="large"
          class="login-form"
          @submit.prevent="handleLogin"
        >
          <el-form-item prop="account" :label="undefined">
            <template #label>
              <span class="field-label">邮箱/账号</span>
            </template>
            <el-input
              v-model="form.account"
              placeholder="请输入邮箱或账号"
              class="form-input"
            />
          </el-form-item>

          <el-form-item prop="password" :label="undefined" class="password-form-item">
            <template #label>
              <span class="field-label password-label-row">
                <span>密码</span>
                <router-link to="/forgot-password" class="forgot-link">忘记密码？</router-link>
              </span>
            </template>
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              class="form-input"
              show-password
              @keyup.enter="handleLogin"
            />
          </el-form-item>

          <el-form-item prop="captchaCode" :label="undefined">
            <template #label>
              <span class="field-label">验证码</span>
            </template>
            <div class="captcha-row">
              <el-input
                v-model="form.captchaCode"
                placeholder="请输入验证码"
                class="form-input captcha-input"
                maxlength="4"
                @keyup.enter="handleLogin"
              />
              <div class="captcha-img-wrapper" @click="refreshCaptcha" title="点击刷新验证码">
                <img
                  v-show="!captchaLoading && captcha.captchaImage"
                  :src="captcha.captchaImage"
                  alt="验证码"
                  class="captcha-img"
                />
                <div v-if="captchaLoading || !captcha.captchaImage" class="captcha-placeholder">
                  <span class="spinner captcha-spinner"></span>
                </div>
                <div class="captcha-overlay">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M23 4v6h-6M1 20v-6h6"/>
                    <path d="M3.51 9a9 9 0 0114.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0020.49 15"/>
                  </svg>
                </div>
              </div>
            </div>
          </el-form-item>

          <div class="form-actions">
            <label class="remember-row">
              <span class="remember-check">
                <input type="checkbox" v-model="form.remember" />
                <span class="check-mark"></span>
              </span>
              <span class="remember-text">保持登录状态</span>
            </label>
          </div>

          <button
            type="submit"
            class="submit-btn"
            :disabled="loading"
            @click="handleLogin"
          >
            <span v-if="loading" class="spinner"></span>
            <span>{{ loading ? '登录中...' : '登录' }}</span>
          </button>
        </el-form>

        <div class="form-footer">
          <span class="footer-text">还没有账号？</span>
          <router-link to="/register" class="footer-link">
            立即注册
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none" class="arrow-icon">
              <path d="M6 12L10 8L6 4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </router-link>
        </div>

        <div class="form-footer" style="margin-top: 14px;">
          <router-link to="/phone-login" class="footer-link">
            手机号快捷登录
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none" class="arrow-icon">
              <path d="M6 12L10 8L6 4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </router-link>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useSettingsStore } from '@/stores/settings'
import { getRedirectPath, removeRedirectPath } from '@/utils/storage'
import { getCaptcha } from '@/api/auth'

const router = useRouter()
const route = useRoute()
const settingsStore = useSettingsStore()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const captchaLoading = ref(true)
let lastRefreshTime = 0

const form = reactive({
  account: '',
  password: '',
  captchaCode: '',
  remember: false,
})

const captcha = reactive({
  captchaId: '',
  captchaImage: '',
})

const rules: FormRules = {
  account: [
    { required: true, message: '请输入邮箱或账号', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码不能少于6位', trigger: 'blur' },
  ],
  captchaCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { min: 4, max: 4, message: '验证码为4位字符', trigger: 'blur' },
  ],
}

async function refreshCaptcha() {
  // 防抖：300ms 内重复点击忽略
  const now = Date.now()
  if (now - lastRefreshTime < 300) return
  lastRefreshTime = now

  captchaLoading.value = true
  try {
    const res = await getCaptcha()
    captcha.captchaId = res.captchaId
    captcha.captchaImage = res.captchaImage
    form.captchaCode = ''
  } catch {
    // 加载验证码失败，静默处理
  } finally {
    captchaLoading.value = false
  }
}

// 组件 setup 阶段立即发起请求，比 onMounted 更早触发
refreshCaptcha()

async function handleLogin(e?: Event) {
  e?.preventDefault()
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await userStore.login(form.account, form.password, captcha.captchaId, form.captchaCode, form.remember)
      ElMessage.success('登录成功')
      // 求职者登录后跳转到投递页面而非管理后台
      if (userStore.isCandidate) {
        const redirect = (route.query.redirect as string) || getRedirectPath() || '/my-applications'
        removeRedirectPath()
        router.push(redirect)
        return
      }
      const redirect = (route.query.redirect as string) || getRedirectPath() || '/dashboard'
      removeRedirectPath()
      router.push(redirect)
    } catch (_err: unknown) {
      // 错误提示已由全局响应拦截器统一处理
      // 刷新验证码防止重放
      refreshCaptcha()
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
/* ================================================================
   Layout
   ================================================================ */
.auth-container {
  display: flex;
  min-height: 100vh;
  background: #f8f9fb;
}

/* ================================================================
   Brand Panel (Left)
   ================================================================ */
.brand-panel {
  position: relative;
  width: 48%;
  background: linear-gradient(160deg, #1e1b4b 0%, #312e81 40%, #4338ca 100%);
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 80px 64px;
  overflow: hidden;
}

.bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255,255,255,0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255,255,255,0.03) 1px, transparent 1px);
  background-size: 60px 60px;
  pointer-events: none;
}

.brand-content {
  position: relative;
  z-index: 1;
  max-width: 480px;
}

.brand-header {
  margin-bottom: 56px;
}

.brand-icon {
  width: 48px;
  height: 48px;
  margin-bottom: 24px;
}

.brand-name {
  font-size: 32px;
  font-weight: 700;
  color: #ffffff;
  margin: 0 0 10px;
  letter-spacing: -0.5px;
}

.brand-tagline {
  font-size: 15px;
  color: rgba(255, 255, 255, 0.6);
  margin: 0;
  font-weight: 400;
}

.features-list {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 14px;
  color: rgba(255, 255, 255, 0.8);
  font-size: 14px;
  font-weight: 400;
}

.feature-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  color: #818cf8;
  flex-shrink: 0;
}

.brand-footer {
  position: relative;
  z-index: 1;
  margin-top: auto;
  padding-top: 40px;
}

.brand-footer p {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.35);
  margin: 0;
}

/* ================================================================
   Form Panel (Right)
   ================================================================ */
.form-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.form-wrapper {
  width: 100%;
  max-width: 420px;
}

.form-header {
  margin-bottom: 36px;
}

.form-title {
  font-size: 26px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 8px;
  letter-spacing: -0.3px;
}

.form-subtitle {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

/* ================================================================
   Form Elements
   ================================================================ */
.login-form {
  display: flex;
  flex-direction: column;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 20px;
}

.login-form :deep(.el-form-item__label) {
  padding-bottom: 0;
}

/* Force password label to stretch full width so the "忘记密码" link aligns right */
.password-form-item :deep(.el-form-item__label) {
  display: flex;
  width: 100%;
}

.password-label-row {
  width: 100%;
}

.field-label {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  font-size: 13px;
  font-weight: 500;
  color: #374151;
  margin-bottom: 6px;
}

.forgot-link {
  font-size: 12px;
  font-weight: 400;
  color: #6366f1;
  text-decoration: none;
  margin-left: 12px;
  white-space: nowrap;
  transition: color 0.15s;
}

.forgot-link:hover {
  color: #4f46e5;
}

.forgot-link:focus-visible {
  outline: 2px solid #6366f1;
  outline-offset: 2px;
  border-radius: 2px;
}

.form-input :deep(.el-input__wrapper) {
  background: #ffffff;
  border: 1.5px solid #e5e7eb;
  border-radius: 10px;
  box-shadow: none;
  transition: border-color 0.15s, box-shadow 0.15s;
  padding: 2px 14px;
}

.form-input :deep(.el-input__wrapper:hover) {
  border-color: #d1d5db;
}

.form-input :deep(.el-input__wrapper.is-focus) {
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}

.form-input :deep(.el-input__inner) {
  font-size: 14px;
  color: #1f2937;
  height: 44px;
}

.form-input :deep(.el-input__inner::placeholder) {
  color: #9ca3af;
}

.form-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.remember-row {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  user-select: none;
}

.remember-check {
  position: relative;
  width: 16px;
  height: 16px;
}

.remember-check input {
  position: absolute;
  opacity: 0;
  width: 100%;
  height: 100%;
  cursor: pointer;
}

.check-mark {
  display: block;
  width: 16px;
  height: 16px;
  border: 1.5px solid #d1d5db;
  border-radius: 4px;
  transition: all 0.15s;
  position: relative;
}

.remember-check input:checked + .check-mark {
  background: #6366f1;
  border-color: #6366f1;
}

.remember-check input:checked + .check-mark::after {
  content: '';
  position: absolute;
  left: 4.5px;
  top: 1.5px;
  width: 5px;
  height: 8px;
  border: solid white;
  border-width: 0 2px 2px 0;
  transform: rotate(45deg);
}

.remember-text {
  font-size: 13px;
  color: #6b7280;
}

.submit-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  height: 46px;
  background: #1e1b4b;
  color: #ffffff;
  font-size: 15px;
  font-weight: 600;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.15s, transform 0.1s, opacity 0.15s;
}

.submit-btn:hover {
  background: #312e81;
}

.submit-btn:active {
  transform: scale(0.99);
}

.submit-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.form-footer {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 4px;
  margin-top: 28px;
}

.footer-text {
  font-size: 13px;
  color: #9ca3af;
}

.footer-link {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  font-size: 13px;
  font-weight: 500;
  color: #6366f1;
  text-decoration: none;
  transition: color 0.15s;
}

.footer-link:hover {
  color: #4f46e5;
}

.arrow-icon {
  transition: transform 0.15s;
}

.footer-link:hover .arrow-icon {
  transform: translateX(2px);
}

/* ================================================================
   Captcha
   ================================================================ */
.captcha-row {
  display: flex;
  gap: 12px;
  align-items: center;
}

.captcha-input {
  flex: 1;
}

.captcha-img-wrapper {
  position: relative;
  width: 110px;
  height: 40px;
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  flex-shrink: 0;
  border: 1.5px solid #e5e7eb;
  transition: border-color 0.15s;
}

.captcha-img-wrapper:hover {
  border-color: #6366f1;
}

.captcha-img {
  width: 100%;
  height: 100%;
  display: block;
}

.captcha-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f3f4f6;
}

.captcha-spinner {
  width: 16px;
  height: 16px;
  border-width: 2px;
  border-color: rgba(99, 102, 241, 0.2);
  border-top-color: #6366f1;
}

.captcha-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(30, 27, 75, 0.5);
  color: #ffffff;
  opacity: 0;
  transition: opacity 0.15s;
}

.captcha-img-wrapper:hover .captcha-overlay {
  opacity: 1;
}

/* ================================================================
   Responsive
   ================================================================ */
@media (max-width: 960px) {
  .brand-panel {
    display: none;
  }

  .form-panel {
    padding: 32px 24px;
  }

  .form-wrapper {
    max-width: 100%;
  }
}
</style>

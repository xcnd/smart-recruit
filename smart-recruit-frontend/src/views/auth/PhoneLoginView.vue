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
          <h1 class="brand-name">SmartRecruit</h1>
          <p class="brand-tagline">AI 驱动的智能招聘平台</p>
        </div>

        <div class="features-list">
          <div class="feature-item">
            <div class="feature-icon">
              <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
                <path d="M16.667 5L7.5 14.167L3.333 10" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </div>
            <span>一键登录，极速投递</span>
          </div>
          <div class="feature-item">
            <div class="feature-icon">
              <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
                <path d="M16.667 5L7.5 14.167L3.333 10" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </div>
            <span>海量名企岗位推荐</span>
          </div>
          <div class="feature-item">
            <div class="feature-icon">
              <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
                <path d="M16.667 5L7.5 14.167L3.333 10" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </div>
            <span>AI 智能简历解析与匹配</span>
          </div>
          <div class="feature-item">
            <div class="feature-icon">
              <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
                <path d="M16.667 5L7.5 14.167L3.333 10" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </div>
            <span>实时追踪投递进度</span>
          </div>
        </div>
      </div>

      <div class="brand-footer">
        <p>&copy; 2026 SmartRecruit. 保留所有权利。</p>
      </div>

      <div class="bg-grid"></div>
    </aside>

    <!-- Right Form Panel -->
    <main class="form-panel">
      <div class="form-wrapper">
        <div class="form-header">
          <h2 class="form-title">手机号登录</h2>
          <p class="form-subtitle">首次登录将自动注册账号</p>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          size="large"
          class="login-form"
          @submit.prevent
        >
          <!-- Phone Input -->
          <el-form-item prop="mobile" class="phone-form-item">
            <label class="field-label">手机号</label>
            <div class="phone-input-wrapper">
              <span class="phone-prefix">+86</span>
              <el-input
                v-model="form.mobile"
                placeholder="请输入手机号"
                maxlength="11"
                class="phone-input"
              />
            </div>
          </el-form-item>

          <!-- Verification Code -->
          <el-form-item prop="verificationCode" class="code-form-item">
            <label class="field-label">验证码</label>
            <div class="code-row">
              <el-input
                v-model="form.verificationCode"
                placeholder="请输入验证码"
                maxlength="6"
                class="code-input"
                @keyup.enter="handleLogin"
              />
              <button
                type="button"
                class="send-code-btn"
                :disabled="countdown > 0"
                @click="sendCode"
              >
                {{ countdown > 0 ? `${countdown}s后重发` : '获取验证码' }}
              </button>
            </div>
          </el-form-item>

          <!-- Terms Agreement -->
          <div class="terms-row">
            <label class="remember-row">
              <span class="remember-check">
                <input type="checkbox" v-model="form.agreed" />
                <span class="check-mark"></span>
              </span>
              <span class="terms-text">
                我已阅读并同意<a href="javascript:void(0)" class="terms-link">《用户协议》</a>和<a href="javascript:void(0)" class="terms-link">《隐私政策》</a>
              </span>
            </label>
          </div>

          <!-- Submit Button -->
          <button
            type="submit"
            class="submit-btn"
            :disabled="loading || !form.agreed"
            @click="handleLogin"
          >
            <span v-if="loading" class="spinner"></span>
            <span>{{ loading ? '登录中...' : '登录 / 注册' }}</span>
          </button>
        </el-form>

        <div class="form-footer">
          <router-link to="/login" class="footer-link">
            员工账号登录
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
import { getRedirectPath } from '@/utils/storage'
import { sendPhoneCode } from '@/api/auth'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const countdown = ref(0)
let countdownTimer: ReturnType<typeof setInterval> | null = null

const form = reactive({
  mobile: '',
  verificationCode: '',
  agreed: false,
})

const rules: FormRules = {
  mobile: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' },
  ],
  verificationCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { min: 6, max: 6, message: '验证码为6位数字', trigger: 'blur' },
  ],
}

function startCountdown() {
  countdown.value = 60
  countdownTimer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      if (countdownTimer) {
        clearInterval(countdownTimer)
        countdownTimer = null
      }
    }
  }, 1000)
}

async function sendCode() {
  if (!/^1[3-9]\d{9}$/.test(form.mobile)) {
    ElMessage.warning('请输入正确的手机号')
    return
  }
  try {
    await sendPhoneCode({ mobile: form.mobile })
    ElMessage.success('验证码已发送')
    startCountdown()
  } catch {
    // 错误提示已由全局响应拦截器处理
  }
}

async function handleLogin() {
  if (loading.value) return
  if (!formRef.value) return

  // Capture redirect URL before async operation to prevent loss
  const redirectUrl = (route.query.redirect as string) || getRedirectPath() || '/my-applications'

  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await userStore.phoneLogin(form.mobile, form.verificationCode, false)
      ElMessage.success('登录成功')
      router.push(redirectUrl)
    } catch {
      // 错误提示已由全局拦截器处理
      form.verificationCode = ''
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
   Brand Panel (Left) — mirrors LoginView.vue
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
   Form Panel (Right) — mirrors LoginView.vue
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

.login-form :deep(.el-form-item__content) {
  line-height: normal;
}

.field-label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: #374151;
  margin-bottom: 6px;
  line-height: 1;
}

/* ================================================================
   Phone Input with +86 Prefix
   ================================================================ */
.phone-input-wrapper {
  display: flex;
  align-items: stretch;
}

.phone-prefix {
  display: flex;
  align-items: center;
  padding: 0 14px;
  font-size: 14px;
  font-weight: 500;
  color: #374151;
  background: #f3f4f6;
  border: 1.5px solid #e5e7eb;
  border-right: none;
  border-radius: 10px 0 0 10px;
  white-space: nowrap;
  flex-shrink: 0;
}

.phone-input {
  flex: 1;
}

.phone-input :deep(.el-input__wrapper) {
  background: #ffffff;
  border: 1.5px solid #e5e7eb;
  border-radius: 0 10px 10px 0;
  box-shadow: none;
  transition: border-color 0.15s, box-shadow 0.15s;
  padding: 2px 14px;
}

.phone-input :deep(.el-input__wrapper:hover) {
  border-color: #d1d5db;
}

.phone-input-wrapper:focus-within .phone-prefix,
.phone-input :deep(.el-input__wrapper.is-focus) {
  border-color: #6366f1;
}

.phone-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}

.phone-input :deep(.el-input__inner) {
  font-size: 14px;
  color: #1f2937;
  height: 44px;
}

.phone-input :deep(.el-input__inner::placeholder) {
  color: #9ca3af;
}

/* ================================================================
   Verification Code Row
   ================================================================ */
.code-row {
  display: flex;
  gap: 12px;
  align-items: stretch;
}

.code-input {
  flex: 1;
}

.code-input :deep(.el-input__wrapper) {
  background: #ffffff;
  border: 1.5px solid #e5e7eb;
  border-radius: 10px;
  box-shadow: none;
  transition: border-color 0.15s, box-shadow 0.15s;
  padding: 2px 14px;
}

.code-input :deep(.el-input__wrapper:hover) {
  border-color: #d1d5db;
}

.code-input :deep(.el-input__wrapper.is-focus) {
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}

.code-input :deep(.el-input__inner) {
  font-size: 14px;
  color: #1f2937;
  height: 44px;
}

.code-input :deep(.el-input__inner::placeholder) {
  color: #9ca3af;
}

.send-code-btn {
  width: 120px;
  height: 44px;
  padding: 0 12px;
  font-size: 13px;
  font-weight: 500;
  color: #6366f1;
  background: #ffffff;
  border: 1.5px solid #6366f1;
  border-radius: 10px;
  cursor: pointer;
  white-space: nowrap;
  flex-shrink: 0;
  transition: all 0.15s;
  line-height: 44px;
}

.send-code-btn:hover:not(:disabled) {
  color: #ffffff;
  background: #6366f1;
}

.send-code-btn:disabled {
  color: #9ca3af;
  border-color: #d1d5db;
  cursor: not-allowed;
  background: #f9fafb;
}

/* ================================================================
   Terms Checkbox
   ================================================================ */
.terms-row {
  margin-bottom: 24px;
}

.remember-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  cursor: pointer;
  user-select: none;
}

.remember-check {
  position: relative;
  width: 16px;
  height: 16px;
  margin-top: 1px;
  flex-shrink: 0;
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

.terms-text {
  font-size: 13px;
  color: #6b7280;
  line-height: 1.5;
}

.terms-link {
  color: #6366f1;
  text-decoration: none;
  transition: color 0.15s;
}

.terms-link:hover {
  color: #4f46e5;
}

/* ================================================================
   Submit Button
   ================================================================ */
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

.submit-btn:hover:not(:disabled) {
  background: #312e81;
}

.submit-btn:active:not(:disabled) {
  transform: scale(0.99);
}

.submit-btn:disabled {
  opacity: 0.5;
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

/* ================================================================
   Footer
   ================================================================ */
.form-footer {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 28px;
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

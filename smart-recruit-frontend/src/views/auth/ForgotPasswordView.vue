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
          <h1 class="brand-name">找回密码</h1>
          <p class="brand-tagline">通过邮箱验证码快速重置您的登录密码</p>
        </div>

        <div class="features-list">
          <div class="feature-item">
            <div class="feature-icon">
              <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
                <path d="M16.667 5L7.5 14.167L3.333 10" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </div>
            <span>验证码发送至注册邮箱</span>
          </div>
          <div class="feature-item">
            <div class="feature-icon">
              <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
                <path d="M16.667 5L7.5 14.167L3.333 10" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </div>
            <span>5 分钟内有效，请在有效期内操作</span>
          </div>
          <div class="feature-item">
            <div class="feature-icon">
              <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
                <path d="M16.667 5L7.5 14.167L3.333 10" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </div>
            <span>重置后使用新密码登录</span>
          </div>
        </div>
      </div>

      <div class="brand-footer">
        <p v-html="settingsStore.copyrightText"></p>
      </div>

      <div class="bg-grid"></div>
    </aside>

    <!-- Right Form Panel -->
    <main class="form-panel">
      <div class="form-wrapper">
        <!-- Step 1: Enter Email -->
        <template v-if="currentStep === 'send'">
          <div class="form-header">
            <h2 class="form-title">找回密码</h2>
            <p class="form-subtitle">请输入您的注册邮箱，我们将发送验证码</p>
          </div>

          <el-form
            ref="emailFormRef"
            :model="emailForm"
            :rules="emailRules"
            label-position="top"
            size="large"
            class="auth-form"
            @submit.prevent="handleSendCode"
          >
            <el-form-item prop="email" :label="undefined">
              <template #label>
                <span class="field-label">注册邮箱</span>
              </template>
              <el-input
                v-model="emailForm.email"
                placeholder="name@company.com"
                class="form-input"
                @keyup.enter="handleSendCode"
              />
            </el-form-item>

            <button
              type="submit"
              class="submit-btn"
              :disabled="sendingCode"
              @click="handleSendCode"
            >
              <span v-if="sendingCode" class="spinner"></span>
              <span>{{ sendingCode ? '发送中...' : '发送验证码' }}</span>
            </button>
          </el-form>
        </template>

        <!-- Step 2: Enter Code + New Password -->
        <template v-else>
          <div class="form-header">
            <h2 class="form-title">重置密码</h2>
            <p class="form-subtitle">请输入邮箱验证码并设置新密码</p>
          </div>

          <el-form
            ref="resetFormRef"
            :model="resetForm"
            :rules="resetRules"
            label-position="top"
            size="large"
            class="auth-form"
            @submit.prevent="handleResetPassword"
          >
            <!-- Read-only email display -->
            <el-form-item :label="undefined">
              <template #label>
                <span class="field-label">注册邮箱</span>
              </template>
              <div class="readonly-email">{{ resetForm.email }}</div>
            </el-form-item>

            <el-form-item prop="verificationCode" :label="undefined">
              <template #label>
                <span class="field-label">邮箱验证码</span>
              </template>
              <div class="code-row">
                <el-input
                  v-model="resetForm.verificationCode"
                  placeholder="6位数字验证码"
                  class="form-input code-input"
                  maxlength="6"
                />
                <button
                  type="button"
                  class="code-btn"
                  :class="{ 'code-btn--counting': codeCooldown > 0 }"
                  :disabled="sendCodeDisabled"
                  @click="handleResendCode"
                >
                  <span v-if="sendingCode" class="spinner spinner--dark"></span>
                  <span>{{ sendCodeText }}</span>
                </button>
              </div>
            </el-form-item>

            <el-form-item prop="newPassword" :label="undefined">
              <template #label>
                <span class="field-label">新密码</span>
              </template>
              <el-input
                v-model="resetForm.newPassword"
                type="password"
                placeholder="至少 6 位字符"
                class="form-input"
                show-password
              />
            </el-form-item>

            <el-form-item prop="confirmPassword" :label="undefined">
              <template #label>
                <span class="field-label">确认新密码</span>
              </template>
              <el-input
                v-model="resetForm.confirmPassword"
                type="password"
                placeholder="请再次输入新密码"
                class="form-input"
                show-password
                @keyup.enter="handleResetPassword"
              />
            </el-form-item>

            <button
              type="submit"
              class="submit-btn"
              :disabled="loading"
              @click="handleResetPassword"
            >
              <span v-if="loading" class="spinner"></span>
              <span>{{ loading ? '重置中...' : '重置密码' }}</span>
            </button>
          </el-form>

          <!-- Back to send email -->
          <div class="step-back">
            <button type="button" class="back-link" @click="handleBackToSend">
              返回上一步
            </button>
          </div>
        </template>

        <!-- Footer Links -->
        <div class="form-footer">
          <span class="footer-text">{{ currentStep === 'send' ? '想起密码了？' : '还没有账号？' }}</span>
          <router-link to="/login" class="footer-link">
            立即登录
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
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { sendVerificationCode, resetPassword } from '@/api/auth'
import { useSettingsStore } from '@/stores/settings'

const router = useRouter()
const settingsStore = useSettingsStore()

// Step management
const currentStep = ref<'send' | 'reset'>('send')

// Step 1: Email
const emailFormRef = ref<FormInstance>()
const sendingCode = ref(false)
const emailForm = reactive({
  email: '',
})

const emailRules: FormRules = {
  email: [
    { required: true, message: '请输入注册邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' },
  ],
}

// Step 2: Code + Password
const resetFormRef = ref<FormInstance>()
const loading = ref(false)
const codeCooldown = ref(0)
let cooldownTimer: ReturnType<typeof setInterval> | null = null

const sendCodeDisabled = computed(() => {
  return codeCooldown.value > 0 || sendingCode.value
})

const sendCodeText = computed(() => {
  if (sendingCode.value) return '发送中...'
  if (codeCooldown.value > 0) return `${codeCooldown.value}s`
  return '重新获取'
})

const resetForm = reactive({
  email: '',
  verificationCode: '',
  newPassword: '',
  confirmPassword: '',
})

const validateConfirmPassword = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (value !== resetForm.newPassword) {
    callback(new Error('两次输入的新密码不一致'))
  } else {
    callback()
  }
}

const validatePasswordRule = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  const error = settingsStore.validatePassword(value)
  if (error) callback(new Error(error))
  else callback()
}

const resetRules: FormRules = {
  verificationCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码为6位数字', trigger: 'blur' },
  ],
  newPassword: [
    { required: true, message: '请设置新密码', trigger: 'blur' },
    { validator: validatePasswordRule, trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
}

// Step 1: Send verification code
async function handleSendCode(e?: Event) {
  e?.preventDefault()
  if (!emailFormRef.value) return

  await emailFormRef.value.validate(async (valid) => {
    if (!valid) return
    sendingCode.value = true
    try {
      await sendVerificationCode({ email: emailForm.email.trim(), purpose: 'reset' })
      ElMessage.success('验证码已发送至您的邮箱，请注意查收')
      resetForm.email = emailForm.email.trim()
      startCooldown()
      currentStep.value = 'reset'
    } catch (_err: unknown) {
      // 错误提示已由全局响应拦截器统一处理
    } finally {
      sendingCode.value = false
    }
  })
}

// Step 2: Resend verification code
async function handleResendCode() {
  sendingCode.value = true
  try {
    await sendVerificationCode({ email: resetForm.email, purpose: 'reset' })
    ElMessage.success('验证码已重新发送')
    startCooldown()
  } catch (_err: unknown) {
    // 错误提示已由全局响应拦截器统一处理
  } finally {
    sendingCode.value = false
  }
}

function startCooldown() {
  clearCooldown()
  codeCooldown.value = 60
  cooldownTimer = setInterval(() => {
    codeCooldown.value--
    if (codeCooldown.value <= 0) {
      clearCooldown()
    }
  }, 1000)
}

function clearCooldown() {
  if (cooldownTimer) {
    clearInterval(cooldownTimer)
    cooldownTimer = null
  }
}

// Step 2: Reset password
async function handleResetPassword(e?: Event) {
  e?.preventDefault()
  if (!resetFormRef.value) return

  await resetFormRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await resetPassword({
        email: resetForm.email,
        verificationCode: resetForm.verificationCode,
        newPassword: resetForm.newPassword,
      })
      ElMessage.success('密码重置成功！即将跳转到登录页面...')
      setTimeout(() => {
        router.push('/login')
      }, 1500)
    } catch (_err: unknown) {
      // 错误提示已由全局响应拦截器统一处理
    } finally {
      loading.value = false
    }
  })
}

// Back to Step 1
function handleBackToSend() {
  currentStep.value = 'send'
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
  overflow-y: auto;
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
.auth-form {
  display: flex;
  flex-direction: column;
}

.auth-form :deep(.el-form-item) {
  margin-bottom: 20px;
}

.auth-form :deep(.el-form-item__label) {
  padding-bottom: 0;
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

/* Read-only email display */
.readonly-email {
  display: flex;
  align-items: center;
  height: 44px;
  padding: 0 14px;
  background: #f3f4f6;
  border: 1.5px solid #e5e7eb;
  border-radius: 10px;
  font-size: 14px;
  color: #374151;
  font-weight: 500;
}

/* Verification code row */
.code-row {
  display: flex;
  gap: 10px;
}

.code-input {
  flex: 1;
}

.code-btn {
  flex-shrink: 0;
  min-width: 94px;
  height: 44px;
  font-size: 13px;
  font-weight: 500;
  color: #6366f1;
  background: #eef2ff;
  border: 1.5px solid #c7d2fe;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.15s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  white-space: nowrap;
  padding: 0 14px;
}

.code-btn:hover:not(:disabled) {
  background: #e0e7ff;
  border-color: #a5b4fc;
}

.code-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.code-btn--counting {
  color: #9ca3af;
  background: #f3f4f6;
  border-color: #e5e7eb;
}

/* Submit button */
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
  margin-top: 4px;
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

/* Spinner */
.spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

.spinner--dark {
  border-color: rgba(99, 102, 241, 0.2);
  border-top-color: #6366f1;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Step back link */
.step-back {
  display: flex;
  justify-content: center;
  margin-top: 18px;
}

.back-link {
  font-size: 13px;
  color: #6366f1;
  background: none;
  border: none;
  cursor: pointer;
  text-decoration: none;
  transition: color 0.15s;
}

.back-link:hover {
  color: #4f46e5;
}

/* Footer */
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

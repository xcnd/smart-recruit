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
          <h1 class="brand-name">创建您的账号</h1>
          <p class="brand-tagline">开始使用 AI 驱动的智能招聘解决方案</p>
        </div>

        <div class="benefits-list">
          <div class="benefit-item">
            <div class="benefit-icon">
              <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
                <circle cx="9" cy="9" r="8" stroke="currentColor" stroke-width="1.5"/>
                <path d="M5.5 9L8 11.5L12.5 6.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </div>
            <div class="benefit-text">
              <strong>免费开始使用</strong>
              <span>注册即享基础版功能，无隐藏费用</span>
            </div>
          </div>
          <div class="benefit-item">
            <div class="benefit-icon">
              <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
                <circle cx="9" cy="9" r="8" stroke="currentColor" stroke-width="1.5"/>
                <path d="M5.5 9L8 11.5L12.5 6.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </div>
            <div class="benefit-text">
              <strong>秒级开通</strong>
              <span>邮箱验证后即刻激活，无需等待审核</span>
            </div>
          </div>
          <div class="benefit-item">
            <div class="benefit-icon">
              <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
                <circle cx="9" cy="9" r="8" stroke="currentColor" stroke-width="1.5"/>
                <path d="M5.5 9L8 11.5L12.5 6.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </div>
            <div class="benefit-text">
              <strong>企业级安全</strong>
              <span>数据加密存储，SOC 2 合规认证</span>
            </div>
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
        <div class="form-header">
          <h2 class="form-title">注册账号</h2>
          <p class="form-subtitle">填写以下信息，验证邮箱后即可完成注册</p>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          size="large"
          class="register-form"
          @submit.prevent="handleRegister"
        >
          <div class="two-cols">
            <el-form-item prop="username" :label="undefined" class="col-item">
              <template #label>
                <span class="field-label">用户名</span>
              </template>
              <el-input
                v-model="form.username"
                placeholder="字母、数字或中文"
                class="form-input"
              />
            </el-form-item>

            <el-form-item prop="realName" :label="undefined" class="col-item">
              <template #label>
                <span class="field-label">真实姓名</span>
              </template>
              <el-input
                v-model="form.realName"
                placeholder="请输入姓名"
                class="form-input"
              />
            </el-form-item>
          </div>

          <el-form-item prop="email" :label="undefined">
            <template #label>
              <span class="field-label">邮箱地址</span>
            </template>
            <el-input
              v-model="form.email"
              :placeholder="`用户名${settingsStore.emailSuffix}`"
              class="form-input"
            />
          </el-form-item>

          <el-form-item prop="verificationCode" :label="undefined">
            <template #label>
              <span class="field-label">邮箱验证码</span>
            </template>
            <div class="code-row">
              <el-input
                v-model="form.verificationCode"
                placeholder="6位数字验证码"
                class="form-input code-input"
                maxlength="6"
              />
              <button
                type="button"
                class="code-btn"
                :class="{ 'code-btn--counting': codeCooldown > 0 }"
                :disabled="sendCodeDisabled"
                @click="handleSendCode"
              >
                <span v-if="sendingCode" class="spinner spinner--dark"></span>
                <span>{{ sendCodeText }}</span>
              </button>
            </div>
          </el-form-item>

          <el-form-item v-if="referralCodeFromUrl" :label="undefined">
            <template #label>
              <span class="field-label">内推码</span>
            </template>
            <el-input
              v-model="form.referralCode"
              disabled
              class="form-input referral-code-input"
              placeholder="内推码已自动填入"
            />
            <p class="referral-code-note">通过内推链接注册，内推码已自动填写</p>
          </el-form-item>

          <el-form-item prop="password" :label="undefined">
            <template #label>
              <span class="field-label">密码</span>
            </template>
            <el-input
              v-model="form.password"
              type="password"
              placeholder="至少 6 位字符"
              class="form-input"
              show-password
            />
          </el-form-item>

          <el-form-item prop="confirmPassword" :label="undefined">
            <template #label>
              <span class="field-label">确认密码</span>
            </template>
            <el-input
              v-model="form.confirmPassword"
              type="password"
              placeholder="请再次输入密码"
              class="form-input"
              show-password
              @keyup.enter="handleRegister"
            />
          </el-form-item>

          <button
            type="submit"
            class="submit-btn"
            :disabled="loading"
            @click="handleRegister"
          >
            <span v-if="loading" class="spinner"></span>
            <span>{{ loading ? '注册中...' : '创建账号' }}</span>
          </button>
        </el-form>

        <div class="form-footer">
          <span class="footer-text">已有账号？</span>
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
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { register, sendVerificationCode } from '@/api/auth'
import { useSettingsStore } from '@/stores/settings'

const route = useRoute()
const router = useRouter()
const settingsStore = useSettingsStore()

const referralCodeFromUrl = computed(() => (route.query.rc as string) || '')

const formRef = ref<FormInstance>()
const loading = ref(false)
const sendingCode = ref(false)
const codeCooldown = ref(0)
let cooldownTimer: ReturnType<typeof setInterval> | null = null

const sendCodeDisabled = computed(() => {
  return codeCooldown.value > 0 || sendingCode.value
})

const sendCodeText = computed(() => {
  if (sendingCode.value) return '发送中...'
  if (codeCooldown.value > 0) return `${codeCooldown.value}s`
  return '获取验证码'
})

const form = reactive({
  username: '',
  realName: '',
  email: '',
  password: '',
  confirmPassword: '',
  verificationCode: '',
  referralCode: referralCodeFromUrl.value,
})

const validateConfirmPassword = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const validatePasswordRule = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  const error = settingsStore.validatePassword(value)
  if (error) callback(new Error(error))
  else callback()
}

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 32, message: '用户名长度应为2-32个字符', trigger: 'blur' },
    {
      pattern: /^[a-zA-Z0-9_\-\u4e00-\u9fa5]+$/,
      message: '用户名只能包含字母、数字、下划线、中划线及中文',
      trigger: 'blur',
    },
  ],
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' },
    { min: 1, max: 32, message: '姓名长度应为1-32个字符', trigger: 'blur' },
  ],
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请设置登录密码', trigger: 'blur' },
    { validator: validatePasswordRule, trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
  verificationCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码为6位数字', trigger: 'blur' },
  ],
}

async function handleSendCode() {
  const emailValue = form.email.trim()
  if (!emailValue) {
    ElMessage.warning('请先输入邮箱地址')
    return
  }
  const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailPattern.test(emailValue)) {
    ElMessage.warning('请输入正确的邮箱格式')
    return
  }

  sendingCode.value = true
  try {
    await sendVerificationCode({ email: emailValue })
    ElMessage.success('验证码已发送至您的邮箱，请注意查收')
    startCooldown()
  } catch (_err: unknown) {
    // 错误提示已由全局响应拦截器统一处理
  } finally {
    sendingCode.value = false
  }
}

function startCooldown() {
  codeCooldown.value = 60
  cooldownTimer = setInterval(() => {
    codeCooldown.value--
    if (codeCooldown.value <= 0) {
      if (cooldownTimer) {
        clearInterval(cooldownTimer)
        cooldownTimer = null
      }
    }
  }, 1000)
}

async function handleRegister(e?: Event) {
  e?.preventDefault()
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await register({
        username: form.username,
        email: form.email,
        password: form.password,
        realName: form.realName,
        verificationCode: form.verificationCode,
        referralCode: form.referralCode || undefined,
      })
      ElMessage.success('注册成功！即将跳转到登录页面...')
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
  margin-bottom: 48px;
}

.brand-icon {
  width: 48px;
  height: 48px;
  margin-bottom: 24px;
}

.brand-name {
  font-size: 30px;
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

.benefits-list {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.benefit-item {
  display: flex;
  align-items: flex-start;
  gap: 14px;
}

.benefit-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  color: #818cf8;
  flex-shrink: 0;
  margin-top: 1px;
}

.benefit-text {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.benefit-text strong {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.9);
  font-weight: 600;
}

.benefit-text span {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.5);
  font-weight: 400;
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
  max-width: 460px;
}

.form-header {
  margin-bottom: 32px;
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
.register-form {
  display: flex;
  flex-direction: column;
}

.register-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.register-form :deep(.el-form-item__label) {
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

/* Two-column layout */
.two-cols {
  display: flex;
  gap: 14px;
}

.col-item {
  flex: 1;
}

.referral-code-note {
  font-size: 12px;
  color: #1677ff;
  margin-top: 4px;
  line-height: 1.4;
}

.referral-code-input :deep(.el-input__inner) {
  background: #f0f8ff;
  color: #1677ff;
  font-weight: 600;
  font-family: 'SF Mono', 'JetBrains Mono', 'Consolas', monospace;
  letter-spacing: 0.1em;
}

/* Verification code */
.code-row {
  display: flex;
  gap: 10px;
}

.code-input {
  flex: 1;
}

.code-btn {
  flex-shrink: 0;
  min-width: 110px;
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
  margin-top: 6px;
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

  .two-cols {
    flex-direction: column;
    gap: 0;
  }
}
</style>

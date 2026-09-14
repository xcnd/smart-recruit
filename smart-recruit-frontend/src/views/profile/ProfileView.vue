<template>
  <div class="profile-root">
    <!-- Loading Skeleton -->
    <template v-if="loading">
      <div class="skeleton-root">
        <div class="skeleton-hero"></div>
        <div class="skeleton-card"></div>
      </div>
    </template>

    <!-- Content -->
    <template v-else>
      <!-- ================================================================== -->
      <!-- Hero Cover Section                                                  -->
      <!-- ================================================================== -->
      <div class="hero-card">
        <div class="hero-bg">
          <div class="hero-bg-layer-1"></div>
          <div class="hero-bg-layer-2"></div>
          <div class="hero-bg-dots"></div>
        </div>
        <div class="hero-body">
          <!-- Avatar -->
          <div class="hero-avatar-wrap" @click="triggerAvatarUpload" title="点击更换头像">
            <el-avatar :size="88" :src="profile.avatar" class="hero-avatar">
              {{ avatarInitial }}
            </el-avatar>
            <div class="hero-avatar-overlay">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"/>
                <circle cx="12" cy="13" r="4"/>
              </svg>
            </div>
            <input ref="avatarInput" type="file" accept="image/*" style="display:none" @change="handleAvatarChange" />
          </div>
          <!-- Info -->
          <div class="hero-info">
            <h1 class="hero-name">{{ profile.realName || profile.username }}</h1>
            <div class="hero-meta">
              <span class="hero-email">{{ profile.email }}</span>
              <span class="hero-dot">·</span>
              <span class="hero-dept">{{ profile.departmentName || '未分配部门' }}</span>
            </div>
            <div class="hero-tags">
              <el-tag v-for="role in profile.roleNames" :key="role" size="small" effect="plain" round class="hero-tag">
                {{ role }}
              </el-tag>
              <span class="hero-status" :class="profile.status === 1 ? 'is-active' : 'is-disabled'">
                <span class="hero-status-dot"></span>
                {{ profile.status === 1 ? '账户正常' : '已禁用' }}
              </span>
            </div>
          </div>
          <!-- Stats -->
          <div class="hero-stats">
            <div class="hero-stat">
              <div class="hero-stat-icon hero-stat-icon--time">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>
                </svg>
              </div>
              <div class="hero-stat-body">
                <span class="hero-stat-label">最近登录</span>
                <span class="hero-stat-value">{{ formatTime(profile.lastLoginTime) }}</span>
              </div>
            </div>
            <div class="hero-stat">
              <div class="hero-stat-icon hero-stat-icon--reg">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="8.5" cy="7" r="4"/><polyline points="17 11 19 13 23 9"/>
                </svg>
              </div>
              <div class="hero-stat-body">
                <span class="hero-stat-label">注册时间</span>
                <span class="hero-stat-value">{{ formatTime(profile.createTime) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- ================================================================== -->
      <!-- Tab Navigation                                                      -->
      <!-- ================================================================== -->
      <div class="tab-bar">
        <button
          class="tab-btn"
          :class="{ active: activeTab === 'info' }"
          @click="activeTab = 'info'"
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/>
          </svg>
          个人信息
        </button>
        <button
          class="tab-btn"
          :class="{ active: activeTab === 'security' }"
          @click="activeTab = 'security'"
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/>
          </svg>
          安全设置
        </button>
      </div>

      <!-- ================================================================== -->
      <!-- Tab: 个人信息                                                        -->
      <!-- ================================================================== -->
      <div v-show="activeTab === 'info'" class="tab-panel">
        <!-- Basic Info -->
        <div class="section-card">
          <div class="section-header">
            <h3 class="section-title">基本信息</h3>
            <p class="section-desc">您的个人身份信息</p>
          </div>
          <div class="form-grid">
            <div class="form-field">
              <label class="form-label">用户名</label>
              <div class="field-static">{{ profile.username }}</div>
            </div>
            <div class="form-field">
              <label class="form-label required">真实姓名</label>
              <input
                v-model="form.realName"
                type="text"
                class="field-input"
                placeholder="请输入真实姓名"
                maxlength="32"
              />
              <span v-if="formErrors.realName" class="field-error">{{ formErrors.realName }}</span>
            </div>
            <div class="form-field">
              <label class="form-label">性别</label>
              <div class="gender-group">
                <label
                  v-for="opt in genderOptions"
                  :key="opt.value"
                  class="gender-option"
                  :class="{ checked: form.gender === opt.value }"
                >
                  <input v-model="form.gender" type="radio" :value="opt.value" />
                  <span class="gender-mark"></span>
                  {{ opt.label }}
                </label>
              </div>
            </div>
            <div class="form-field">
              <label class="form-label">邮箱</label>
              <div class="field-static field-static--muted">
                {{ profile.email }}
                <el-tooltip content="如需修改邮箱，请前往「安全设置」标签页" placement="top">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="cursor:help;opacity:0.5;margin-left:4px">
                    <circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/>
                  </svg>
                </el-tooltip>
              </div>
            </div>
            <div class="form-field">
              <label class="form-label">手机号</label>
              <input
                v-model="form.mobile"
                type="text"
                inputmode="numeric"
                class="field-input"
                placeholder="请输入手机号"
                maxlength="11"
                readonly
                @focus="onMobileFocus"
              />
              <span v-if="formErrors.mobile" class="field-error">{{ formErrors.mobile }}</span>
            </div>
          </div>
        </div>

        <!-- Work Info -->
        <div class="section-card">
          <div class="section-header">
            <h3 class="section-title">工作信息</h3>
            <p class="section-desc">部门与角色，如需修改请联系管理员</p>
          </div>
          <div class="form-grid">
            <div class="form-field">
              <label class="form-label">所属部门</label>
              <div class="field-static">{{ profile.departmentName || '-' }}</div>
            </div>
            <div class="form-field">
              <label class="form-label">角色</label>
              <div class="field-static">
                <el-tag
                  v-for="role in profile.roleNames"
                  :key="role"
                  size="small"
                  effect="light"
                  round
                  style="margin-right:6px;margin-bottom:4px"
                >
                  {{ role }}
                </el-tag>
                <span v-if="!profile.roleNames?.length">-</span>
              </div>
            </div>
            <div class="form-field">
              <label class="form-label">登录 IP</label>
              <div class="field-static field-static--muted">{{ profile.lastLoginIp || '-' }}</div>
            </div>
          </div>
        </div>

        <!-- Save -->
        <div class="sticky-footer">
          <button class="btn btn-primary" :disabled="savingInfo" @click="handleSaveInfo">
            <span v-if="savingInfo" class="spin"></span>
            {{ savingInfo ? '保存中...' : '保存修改' }}
          </button>
          <button class="btn btn-ghost" :disabled="savingInfo" @click="handleResetInfo">重置</button>
        </div>
      </div>

      <!-- ================================================================== -->
      <!-- Tab: 安全设置                                                        -->
      <!-- ================================================================== -->
      <div v-show="activeTab === 'security'" class="tab-panel">
        <!-- Change Password -->
        <div class="section-card">
          <div class="section-header">
            <h3 class="section-title">修改密码</h3>
            <p class="section-desc">定期更换密码可以保护账号安全</p>
          </div>
          <div class="form-grid form-grid--narrow">
            <div class="form-field">
              <label class="form-label required">当前密码</label>
              <input
                v-model="pwdForm.oldPassword"
                type="password"
                class="field-input"
                placeholder="请输入当前密码"
              />
              <span v-if="pwdErrors.oldPassword" class="field-error">{{ pwdErrors.oldPassword }}</span>
            </div>
            <div class="form-field">
              <label class="form-label required">新密码</label>
              <input
                v-model="pwdForm.newPassword"
                type="password"
                class="field-input"
                placeholder="至少 6 位字符"
                maxlength="64"
              />
              <span v-if="pwdErrors.newPassword" class="field-error">{{ pwdErrors.newPassword }}</span>
            </div>
            <div class="form-field">
              <label class="form-label required">确认新密码</label>
              <input
                v-model="pwdForm.confirmPassword"
                type="password"
                class="field-input"
                placeholder="请再次输入新密码"
                maxlength="64"
              />
              <span v-if="pwdErrors.confirmPassword" class="field-error">{{ pwdErrors.confirmPassword }}</span>
            </div>
          </div>
          <div class="section-action">
            <button class="btn btn-primary btn-sm" :disabled="savingPwd" @click="handleChangePassword">
              <span v-if="savingPwd" class="spin"></span>
              {{ savingPwd ? '修改中...' : '更新密码' }}
            </button>
          </div>
        </div>

        <!-- Change Email -->
        <div class="section-card">
          <div class="section-header">
            <h3 class="section-title">修改邮箱</h3>
            <p class="section-desc">当前邮箱：<strong>{{ profile.email }}</strong></p>
          </div>
          <div class="email-change-flow">
            <!-- Step 1: enter new email + send code -->
            <div class="form-grid form-grid--narrow">
              <div class="form-field">
                <label class="form-label required">新邮箱地址</label>
                <input
                  v-model="emailForm.newEmail"
                  type="email"
                  class="field-input"
                  placeholder="请输入新的邮箱地址"
                />
                <span v-if="emailErrors.newEmail" class="field-error">{{ emailErrors.newEmail }}</span>
              </div>
              <div class="form-field">
                <label class="form-label required">验证码</label>
                <div class="input-with-btn">
                  <input
                    v-model="emailForm.verificationCode"
                    type="text"
                    class="field-input"
                    placeholder="6位数字验证码"
                    maxlength="6"
                  />
                  <button
                    class="btn btn-outline btn-sm"
                    :disabled="emailCodeCooldown > 0 || sendingEmailCode"
                    @click="handleSendEmailCode"
                  >
                    <span v-if="sendingEmailCode" class="spin spin--dark"></span>
                    <span>{{ emailCodeText }}</span>
                  </button>
                </div>
                <span v-if="emailErrors.verificationCode" class="field-error">{{ emailErrors.verificationCode }}</span>
              </div>
            </div>
          </div>
          <div class="section-action">
            <button class="btn btn-primary btn-sm" :disabled="savingEmail" @click="handleChangeEmail">
              <span v-if="savingEmail" class="spin"></span>
              {{ savingEmail ? '验证中...' : '确认修改' }}
            </button>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { getMyProfile, updateMyProfile, changePassword, changeEmail, uploadAvatar } from '@/api/system'
import { sendVerificationCode } from '@/api/auth'
import { useSettingsStore } from '@/stores/settings'
import { useUserStore } from '@/stores/user'
import type { UserDetailVO } from '@/types/models'

const userStore = useUserStore()
const settingsStore = useSettingsStore()

// ---- State ----
const loading = ref(true)
const activeTab = ref<'info' | 'security'>('info')
const savingInfo = ref(false)
const savingPwd = ref(false)
const savingEmail = ref(false)
const sendingEmailCode = ref(false)
const emailCodeCooldown = ref(0)
const avatarInput = ref<HTMLInputElement>()

let cooldownTimer: ReturnType<typeof setInterval> | null = null

const profile = reactive<UserDetailVO>({
  id: 0, username: '', realName: '', email: '', mobile: '',
  avatar: '', gender: 0, deptId: 0, departmentName: '',
  roleIds: [], roleNames: [], status: 1,
  lastLoginTime: '', lastLoginIp: '', createTime: '', updateTime: '',
})

// ---- Profile Form ----
const form = reactive({ realName: '', mobile: '', gender: 0 })
const formErrors = reactive({ realName: '', mobile: '' })

const genderOptions = [
  { label: '男', value: 1 },
  { label: '女', value: 2 },
  { label: '保密', value: 0 },
]

// ---- Password Form ----
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const pwdErrors = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

// ---- Email Form ----
const emailForm = reactive({ newEmail: '', verificationCode: '' })
const emailErrors = reactive({ newEmail: '', verificationCode: '' })

const emailCodeText = computed(() => {
  if (sendingEmailCode.value) return '发送中...'
  if (emailCodeCooldown.value > 0) return `${emailCodeCooldown.value}s`
  return '获取验证码'
})

// ---- Computed ----
const avatarInitial = computed(() => profile.realName?.charAt(0) || profile.username?.charAt(0) || 'U')

// ---- Data Loading ----
async function loadProfile() {
  loading.value = true
  try {
    const data = await getMyProfile()
    Object.assign(profile, data)
    form.realName = data.realName || ''
    form.mobile = (data.mobile && /^1[3-9]\d{9}$/.test(data.mobile)) ? data.mobile : ''
    form.gender = data.gender ?? 0
  } catch {
    ElMessage.error('加载个人信息失败')
  } finally {
    loading.value = false
  }
}

// ---- Avatar Upload ----
function triggerAvatarUpload() {
  avatarInput.value?.click()
}

function handleAvatarChange(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return
  // 头像业务限制 2MB；同时遵守系统上传大小与类型限制
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过 2MB')
    return
  }
  const uploadError = settingsStore.validateUploadFile(file)
  if (uploadError) {
    ElMessage.warning(uploadError)
    return
  }
  // Show preview immediately while uploading
  const previewUrl = URL.createObjectURL(file)
  const uploadFile = async () => {
    try {
      const avatarUrl = await uploadAvatar(file)
      profile.avatar = avatarUrl
      userStore.updateAvatar(avatarUrl)
      ElMessage.success('头像更新成功')
    } catch {
      // handled by interceptor
    } finally {
      URL.revokeObjectURL(previewUrl)
    }
  }
  // Show local preview
  profile.avatar = previewUrl
  uploadFile()
  // Reset input to allow re-uploading the same file
  ;(e.target as HTMLInputElement).value = ''
}

// ---- Save Profile ----
function validateInfoForm(): boolean {
  let valid = true
  formErrors.realName = ''
  formErrors.mobile = ''
  if (!form.realName.trim()) {
    formErrors.realName = '请输入真实姓名'
    valid = false
  } else if (form.realName.length > 32) {
    formErrors.realName = '姓名不能超过32个字符'
    valid = false
  }
  if (form.mobile && !/^1[3-9]\d{9}$/.test(form.mobile)) {
    formErrors.mobile = '请输入正确的手机号'
    valid = false
  }
  return valid
}

async function handleSaveInfo() {
  if (!validateInfoForm()) return
  savingInfo.value = true
  try {
    const result = await updateMyProfile({
      realName: form.realName,
      mobile: form.mobile || undefined,
      gender: form.gender,
    })
    userStore.updateRealName(result.realName)
    ElMessage.success('个人信息更新成功')
  } catch {
    // handled by interceptor
  } finally {
    savingInfo.value = false
  }
}

function onMobileFocus(e: FocusEvent) {
  const input = e.target as HTMLInputElement
  input.removeAttribute('readonly')
}

function handleResetInfo() {
  form.realName = profile.realName || ''
  form.mobile = profile.mobile || ''
  form.gender = profile.gender ?? 0
  formErrors.realName = ''
  formErrors.mobile = ''
}

// ---- Change Password ----
function validatePwdForm(): boolean {
  let valid = true
  pwdErrors.oldPassword = ''
  pwdErrors.newPassword = ''
  pwdErrors.confirmPassword = ''
  if (!pwdForm.oldPassword) { pwdErrors.oldPassword = '请输入当前密码'; valid = false }
  if (!pwdForm.newPassword) { pwdErrors.newPassword = '请输入新密码'; valid = false }
  else {
    const policyError = settingsStore.validatePassword(pwdForm.newPassword)
    if (policyError) { pwdErrors.newPassword = policyError; valid = false }
  }
  if (!pwdForm.confirmPassword) { pwdErrors.confirmPassword = '请再次输入新密码'; valid = false }
  else if (pwdForm.newPassword !== pwdForm.confirmPassword) { pwdErrors.confirmPassword = '两次输入的密码不一致'; valid = false }
  return valid
}

async function handleChangePassword() {
  if (!validatePwdForm()) return
  savingPwd.value = true
  try {
    await changePassword({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
    ElMessage.success('密码修改成功，请妥善保管')
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
  } catch {
    // handled by interceptor
  } finally {
    savingPwd.value = false
  }
}

// ---- Change Email ----
function validateEmailForm(): boolean {
  let valid = true
  emailErrors.newEmail = ''
  emailErrors.verificationCode = ''
  if (!emailForm.newEmail) { emailErrors.newEmail = '请输入新邮箱'; valid = false }
  else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(emailForm.newEmail)) { emailErrors.newEmail = '邮箱格式不正确'; valid = false }
  if (!emailForm.verificationCode) { emailErrors.verificationCode = '请输入验证码'; valid = false }
  return valid
}

async function handleSendEmailCode() {
  if (!emailForm.newEmail) { emailErrors.newEmail = '请先输入新邮箱'; return }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(emailForm.newEmail)) { emailErrors.newEmail = '邮箱格式不正确'; return }
  sendingEmailCode.value = true
  try {
    await sendVerificationCode({ email: emailForm.newEmail, purpose: 'register' })
    ElMessage.success('验证码已发送至新邮箱')
    startEmailCooldown()
  } catch {
    // handled by interceptor
  } finally {
    sendingEmailCode.value = false
  }
}

function startEmailCooldown() {
  emailCodeCooldown.value = 60
  if (cooldownTimer) clearInterval(cooldownTimer)
  cooldownTimer = setInterval(() => {
    emailCodeCooldown.value--
    if (emailCodeCooldown.value <= 0) {
      if (cooldownTimer) { clearInterval(cooldownTimer); cooldownTimer = null }
    }
  }, 1000)
}

async function handleChangeEmail() {
  if (!validateEmailForm()) return
  savingEmail.value = true
  try {
    await changeEmail({ newEmail: emailForm.newEmail, verificationCode: emailForm.verificationCode })
    profile.email = emailForm.newEmail
    userStore.updateEmail(emailForm.newEmail)
    ElMessage.success('邮箱修改成功')
    emailForm.newEmail = ''
    emailForm.verificationCode = ''
  } catch {
    // handled by interceptor
  } finally {
    savingEmail.value = false
  }
}

// ---- Helpers ----
function formatTime(time?: string): string {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit',
    hour12: false, timeZone: 'Asia/Shanghai',
  })
}

onMounted(() => loadProfile())
</script>

<style scoped>
/* ================================================================
   Root
   ================================================================ */
.profile-root { max-width: 880px; margin: 0 auto; }

/* ================================================================
   Hero Card
   ================================================================ */
.hero-card {
  position: relative;
  border-radius: 16px;
  overflow: hidden;
  margin-bottom: 20px;
}

.hero-bg {
  position: absolute;
  inset: 0;
  overflow: hidden;
}

.hero-bg-layer-1 {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #1e1b4b 0%, #3730a3 30%, #6366f1 60%, #8b5cf6 100%);
}

.hero-bg-layer-2 {
  position: absolute;
  inset: 0;
  opacity: 0.15;
  background:
    radial-gradient(circle at 20% 80%, #a78bfa 0%, transparent 50%),
    radial-gradient(circle at 80% 20%, #818cf8 0%, transparent 50%),
    radial-gradient(circle at 60% 50%, #c4b5fd 0%, transparent 40%);
}

.hero-bg-dots {
  position: absolute;
  inset: 0;
  background-image:
    radial-gradient(circle, rgba(255,255,255,0.08) 1px, transparent 1px);
  background-size: 24px 24px;
}

.hero-body {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 28px;
  padding: 36px 40px;
}

/* Avatar */
.hero-avatar-wrap {
  position: relative;
  flex-shrink: 0;
  cursor: pointer;
  border-radius: 50%;
}

.hero-avatar {
  border: 4px solid rgba(255,255,255,0.3);
  box-shadow: 0 8px 32px rgba(0,0,0,0.2);
  background: linear-gradient(135deg, #818cf8, #c084fc);
  color: white;
  font-size: 36px;
  font-weight: 700;
  transition: border-color 0.2s;
}

.hero-avatar-wrap:hover .hero-avatar {
  border-color: rgba(255,255,255,0.6);
}

.hero-avatar-overlay {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: rgba(0,0,0,0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s;
}

.hero-avatar-wrap:hover .hero-avatar-overlay {
  opacity: 1;
}

/* Info */
.hero-info {
  flex: 1;
  min-width: 0;
}

.hero-name {
  font-size: 26px;
  font-weight: 700;
  color: #ffffff;
  margin: 0 0 8px;
  letter-spacing: -0.4px;
}

.hero-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 12px;
}

.hero-email {
  font-size: 14px;
  color: rgba(255,255,255,0.7);
}

.hero-dot {
  color: rgba(255,255,255,0.3);
}

.hero-dept {
  font-size: 14px;
  color: rgba(255,255,255,0.7);
}

.hero-tags {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.hero-tag {
  background: rgba(255,255,255,0.15) !important;
  border-color: rgba(255,255,255,0.2) !important;
  color: rgba(255,255,255,0.85) !important;
}

.hero-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: rgba(255,255,255,0.6);
  padding: 2px 10px;
  border-radius: 100px;
  background: rgba(255,255,255,0.1);
}

.hero-status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #a3e635;
}

.hero-status.is-disabled .hero-status-dot {
  background: #f87171;
}

.hero-status.is-active .hero-status-dot {
  box-shadow: 0 0 6px #a3e635;
}

/* Stats */
.hero-stats {
  display: flex;
  flex-direction: column;
  gap: 14px;
  flex-shrink: 0;
}

.hero-stat {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.hero-stat-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.hero-stat-icon--time {
  background: rgba(167,139,250,0.25);
  color: #c4b5fd;
}

.hero-stat-icon--reg {
  background: rgba(129,140,248,0.25);
  color: #a5b4fc;
}

.hero-stat-body {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.hero-stat-label {
  font-size: 11px;
  color: rgba(255,255,255,0.45);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.hero-stat-value {
  font-size: 13px;
  color: rgba(255,255,255,0.8);
  font-weight: 500;
}

/* ================================================================
   Tab Bar
   ================================================================ */
.tab-bar {
  display: flex;
  gap: 4px;
  background: var(--c-bg);
  border-radius: 12px;
  padding: 4px;
  margin-bottom: 20px;
  border: 1px solid var(--c-border);
}

.tab-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 40px;
  font-size: 14px;
  font-weight: 500;
  color: var(--c-text-secondary);
  background: transparent;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.tab-btn:hover {
  color: var(--c-text);
  background: var(--c-bg-hover);
}

.tab-btn.active {
  color: var(--c-text);
  background: var(--c-card);
  box-shadow: 0 1px 3px rgba(0,0,0,0.06), 0 1px 2px rgba(0,0,0,0.04);
}

/* ================================================================
   Tab Panel
   ================================================================ */
.tab-panel {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* ================================================================
   Section Card
   ================================================================ */
.section-card {
  background: var(--c-card);
  border: 1px solid var(--c-border);
  border-radius: 14px;
  padding: 28px 32px;
}

.section-header {
  margin-bottom: 24px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--c-text);
  margin: 0 0 4px;
}

.section-desc {
  font-size: 13px;
  color: var(--c-text-muted);
  margin: 0;
}

.section-desc strong {
  color: var(--c-text-secondary);
}

.section-action {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid var(--c-border-light);
}

/* ================================================================
   Form Grid
   ================================================================ */
.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px 32px;
}

.form-grid--narrow {
  max-width: 560px;
  grid-template-columns: 1fr;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--c-text-secondary);
}

.form-label.required::after {
  content: ' *';
  color: #ef4444;
}

/* Static value */
.field-static {
  display: flex;
  align-items: center;
  height: 40px;
  padding: 0 14px;
  background: var(--c-bg);
  border: 1.5px solid var(--c-border);
  border-radius: 10px;
  font-size: 14px;
  color: var(--c-text);
}

.field-static--muted {
  color: var(--c-text-muted);
}

/* Input */
.field-input {
  height: 40px;
  padding: 0 14px;
  background: var(--c-card);
  border: 1.5px solid var(--c-border);
  border-radius: 10px;
  font-size: 14px;
  color: var(--c-text);
  outline: none;
  transition: border-color 0.15s, box-shadow 0.15s;
  font-family: inherit;
}

.field-input::placeholder {
  color: #9ca3af;
}

.field-input:focus {
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99,102,241,0.1);
}

.field-error {
  font-size: 12px;
  color: #ef4444;
}

/* Gender */
.gender-group {
  display: flex;
  gap: 8px;
  height: 40px;
}

.gender-option {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 0 16px;
  border: 1.5px solid var(--c-border);
  border-radius: 10px;
  font-size: 13px;
  color: var(--c-text-secondary);
  cursor: pointer;
  transition: all 0.15s;
  user-select: none;
}

.gender-option:hover {
  border-color: #c7d2fe;
}

.gender-option.checked {
  border-color: #6366f1;
  color: #6366f1;
  background: #eef2ff;
}

.gender-option input {
  display: none;
}

.gender-mark {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  border: 2px solid var(--c-border);
  transition: all 0.15s;
  flex-shrink: 0;
}

.gender-option.checked .gender-mark {
  border-color: #6366f1;
  border-width: 5px;
}

/* Input with inline button */
.input-with-btn {
  display: flex;
  gap: 10px;
}

.input-with-btn .field-input {
  flex: 1;
}

/* ================================================================
   Buttons
   ================================================================ */
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 42px;
  padding: 0 24px;
  font-size: 14px;
  font-weight: 600;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.15s;
  font-family: inherit;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-sm {
  height: 36px;
  padding: 0 18px;
  font-size: 13px;
  font-weight: 500;
}

.btn-primary {
  background: #1e1b4b;
  color: #ffffff;
}

.btn-primary:hover:not(:disabled) {
  background: #3730a3;
}

.btn-ghost {
  background: transparent;
  color: var(--c-text-secondary);
  border: 1.5px solid var(--c-border);
}

.btn-ghost:hover:not(:disabled) {
  background: var(--c-bg-hover);
  color: var(--c-text);
}

.btn-outline {
  background: #eef2ff;
  color: #6366f1;
  border: 1.5px solid #c7d2fe;
  flex-shrink: 0;
  min-width: 100px;
  white-space: nowrap;
}

.btn-outline:hover:not(:disabled) {
  background: #e0e7ff;
  border-color: #a5b4fc;
}

.btn-outline:disabled {
  background: #f3f4f6;
  color: #9ca3af;
  border-color: #e5e7eb;
}

/* Spinner */
.spin {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
  flex-shrink: 0;
}

.spin--dark {
  border-color: rgba(99,102,241,0.2);
  border-top-color: #6366f1;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Sticky footer */
.sticky-footer {
  display: flex;
  gap: 12px;
  position: sticky;
  bottom: 20px;
  padding: 16px 24px;
  background: var(--c-card);
  border: 1px solid var(--c-border);
  border-radius: 14px;
  box-shadow: 0 -4px 24px rgba(0,0,0,0.06);
}

/* ================================================================
   Skeleton
   ================================================================ */
.skeleton-root {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.skeleton-hero {
  height: 180px;
  background: var(--c-bg);
  border-radius: 16px;
  animation: shimmer 1.5s infinite;
}

.skeleton-card {
  height: 200px;
  background: var(--c-bg);
  border-radius: 14px;
  animation: shimmer 1.5s infinite;
}

@keyframes shimmer {
  0% { opacity: 0.6; }
  50% { opacity: 1; }
  100% { opacity: 0.6; }
}

/* ================================================================
   Responsive
   ================================================================ */
@media (max-width: 768px) {
  .hero-body {
    flex-direction: column;
    text-align: center;
    padding: 28px 24px;
    gap: 16px;
  }

  .hero-info { align-items: center; }
  .hero-meta { justify-content: center; }
  .hero-tags { justify-content: center; }
  .hero-stats { flex-direction: row; gap: 24px; }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .tab-bar {
    flex-direction: column;
    gap: 2px;
  }

  .sticky-footer {
    flex-direction: column;
  }
}
</style>

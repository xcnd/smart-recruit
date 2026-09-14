import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { getPublicConfigs } from '@/api/system'

/**
 * 全局系统设置 Store。
 *
 * <p>应用启动时加载公开系统配置，提供类型化的响应式取值，
 * 供页面标题、品牌、密码策略、上传限制、入职配置等场景即时消费；
 * 系统设置页保存成功后调用 {@link refresh} 即可立即生效。</p>
 */
export const useSettingsStore = defineStore('settings', () => {
  const configs = ref<Record<string, string>>({})
  const loaded = ref(false)
  const loading = ref(false)

  /**
   * 从服务端加载公开配置。
   */
  async function load() {
    if (loaded.value || loading.value) return
    loading.value = true
    try {
      configs.value = await getPublicConfigs()
      loaded.value = true
    } catch {
      // 网络异常时保留默认值，页面仍可正常使用
      loaded.value = true
    } finally {
      loading.value = false
    }
  }

  /**
   * 保存成功后强制刷新（系统设置页调用，保证立即生效）。
   */
  async function refresh() {
    loading.value = true
    try {
      configs.value = await getPublicConfigs()
      loaded.value = true
    } finally {
      loading.value = false
    }
  }

  /**
   * 静默刷新：窗口重新聚焦时拉取最新配置，不阻塞界面。
   */
  async function refreshSilently() {
    if (loading.value) return
    try {
      configs.value = await getPublicConfigs()
      loaded.value = true
    } catch {
      // 静默失败，保留现有配置
    }
  }

  // 窗口重新聚焦时同步最新配置（其他管理员修改后即时感知）
  if (typeof window !== 'undefined') {
    window.addEventListener('focus', () => {
      void refreshSilently()
    })
  }

  /**
   * 读取配置值，缺失或为空时返回默认值。
   */
  function get(key: string, fallback = ''): string {
    const value = configs.value[key]
    return value !== undefined && value !== '' ? value : fallback
  }

  // ==================== 通用 ====================
  const systemName = computed(() => get('system_name', 'SmartRecruit'))
  const copyrightText = computed(() =>
    get('copyright_text', '© 2026 SmartRecruit. All rights reserved.'))
  const companyName = computed(() => get('company_name', 'SmartRecruit 科技有限公司'))
  const companyCreditCode = computed(() => get('company_credit_code', ''))
  const companyAddress = computed(() => get('company_address', ''))
  const companyLegalRep = computed(() => get('company_legal_representative', ''))

  // ==================== 邮件 ====================
  const emailSuffix = computed(() => get('email_suffix', '@company.com'))
  const emailSenderName = computed(() => get('email_sender_name', 'SmartRecruit'))
  const emailSenderAddress = computed(() => get('email_sender_address', ''))

  // ==================== 安全策略 ====================
  const passwordMinLength = computed(() => Number(get('password_min_length', '6')))
  const passwordMaxLength = computed(() => Number(get('password_max_length', '64')))
  const passwordRequireSpecial = computed(() => get('password_require_special', '0') === '1')
  const loginMaxAttempts = computed(() => Number(get('login_max_attempts', '5')))
  const sessionTimeoutMinutes = computed(() => Number(get('session_timeout_minutes', '120')))
  const watermarkEnabled = computed(() => get('watermark_enabled', '0') === '1')

  const passwordPolicyText = computed(() => {
    const parts = [`${passwordMinLength.value}~${passwordMaxLength.value} 位`]
    if (passwordRequireSpecial.value) parts.push('须包含特殊字符（!@#$%^&* 等）')
    return `密码长度需为 ${parts.join('，')}`
  })

  /**
   * 校验密码是否满足当前系统密码策略，返回错误信息（通过时返回 null）。
   */
  function validatePassword(password: string): string | null {
    if (!password) return '请输入密码'
    if (password.length < passwordMinLength.value) {
      return `密码长度不能少于 ${passwordMinLength.value} 位`
    }
    if (password.length > passwordMaxLength.value) {
      return `密码长度不能超过 ${passwordMaxLength.value} 位`
    }
    if (passwordRequireSpecial.value && !/[!@#$%^&*()\-_=+[\]{}|;:'",.<>/?~`]/.test(password)) {
      return '密码必须包含至少一个特殊字符（如 !@#$%^&*）'
    }
    return null
  }

  // ==================== 文件管理 ====================
  const uploadMaxSizeMb = computed(() => Number(get('upload_max_size_mb', '10')))
  const uploadAllowedExtensions = computed(() =>
    get('upload_allowed_extensions', '.pdf,.jpg,.jpeg,.png,.gif,.doc,.docx,.xls,.xlsx')
      .split(',').map(s => s.trim()).filter(Boolean))

  /**
   * 校验上传文件是否符合系统配置（大小 + 扩展名），返回错误信息（通过时返回 null）。
   */
  function validateUploadFile(file: File): string | null {
    const maxBytes = uploadMaxSizeMb.value * 1024 * 1024
    if (file.size > maxBytes) {
      return `文件大小不能超过 ${uploadMaxSizeMb.value} MB`
    }
    const name = file.name.toLowerCase()
    const allowed = uploadAllowedExtensions.value
    if (allowed.length > 0 && !allowed.some(ext => name.endsWith(ext.toLowerCase()))) {
      return `不支持的文件类型：${file.name}，允许的类型：${allowed.join(', ')}`
    }
    return null
  }

  // ==================== 入职管理 ====================
  const onboardingDefaultPassword = computed(() => get('onboarding_default_password', '123456'))
  const onboardingTrainingModules = computed(() =>
    get('onboarding_training_modules', '公司文化与制度,信息安全培训,岗位技能培训,合规培训,团队介绍')
      .split(',').map(s => s.trim()).filter(Boolean))
  const onboardingWelcomeTemplate = computed(() =>
    get('onboarding_welcome_template',
      '亲爱的 {{employeeName}}：\n\n欢迎加入 {{departmentName}} 部门！\n您的职位：{{jobTitle}}（{{level}}）\n入职日期：{{onboardDate}}\n\n我们为您准备了完善的入职培训计划，您的导师和伙伴将协助您快速融入团队。\n\n期待与您共同成长！\n\n—— 人力资源部'))

  // ==================== AI 引擎 ====================
  const aiRiskThresholdLow = computed(() => Number(get('ai_risk_threshold_low', '0.25')))
  const aiRiskThresholdMedium = computed(() => Number(get('ai_risk_threshold_medium', '0.50')))
  const aiRiskThresholdHigh = computed(() => Number(get('ai_risk_threshold_high', '0.75')))

  // ==================== AI 简历筛选 ====================
  const aiScreenWeights = computed(() => ({
    education: Number(get('ai_screen_weight_education', '20')),
    skill: Number(get('ai_screen_weight_skill', '35')),
    experience: Number(get('ai_screen_weight_experience', '20')),
    behavior: Number(get('ai_screen_weight_behavior', '20')),
    semantic: Number(get('ai_screen_weight_semantic', '15')),
  }))
  const aiScreenPassScore = computed(() => Number(get('ai_screen_pass_score', '70')))

  /**
   * 将系统名称同步到浏览器标签页（无页面标题时兜底）。
   */
  function applyToDocument(suffix = '') {
    document.title = suffix
      ? `${suffix} - ${systemName.value}`
      : `${systemName.value} - 智能招聘管理系统`
  }

  return {
    configs,
    loaded,
    loading,
    load,
    refresh,
    refreshSilently,
    get,
    systemName,
    copyrightText,
    companyName,
    companyCreditCode,
    companyAddress,
    companyLegalRep,
    emailSuffix,
    emailSenderName,
    emailSenderAddress,
    passwordMinLength,
    passwordMaxLength,
    passwordRequireSpecial,
    loginMaxAttempts,
    sessionTimeoutMinutes,
    watermarkEnabled,
    passwordPolicyText,
    validatePassword,
    uploadMaxSizeMb,
    uploadAllowedExtensions,
    validateUploadFile,
    onboardingDefaultPassword,
    onboardingTrainingModules,
    onboardingWelcomeTemplate,
    aiRiskThresholdLow,
    aiRiskThresholdMedium,
    aiRiskThresholdHigh,
    aiScreenWeights,
    aiScreenPassScore,
    applyToDocument,
  }
})

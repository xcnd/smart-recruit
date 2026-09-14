<template>
  <div class="settings-page">
    <!-- Page Header -->
    <div class="page-header">
      <div class="page-title">
        <el-icon :size="22"><Setting /></el-icon>
        <span>系统设置</span>
        <el-tag size="small" type="success" effect="plain">保存后立即生效</el-tag>
      </div>
      <el-text type="info" size="small">
        管理系统全局配置参数，保存后各模块与页面即时生效，请谨慎操作
      </el-text>
      <div v-if="lastUpdated" class="page-meta">
        最近更新：{{ lastUpdated }} · 操作人：{{ lastUpdatedBy || '—' }}
      </div>
    </div>

    <!-- Layout: Left Menu + Right Content -->
    <div class="settings-layout">
      <!-- Left Category Menu -->
      <div class="settings-sidebar">
        <el-menu
          :default-active="activeCategory"
          class="category-menu"
          @select="onCategorySelect"
        >
          <el-menu-item v-for="cat in categories" :key="cat.key" :index="cat.key">
            <el-icon :size="16"><component :is="cat.icon" /></el-icon>
            <span>{{ cat.label }}</span>
          </el-menu-item>
        </el-menu>
      </div>

      <!-- Right Content Area -->
      <div class="settings-content" v-loading="loading">
        <template v-if="activeCategory === 'general'">
          <el-card shadow="never">
            <template #header>
              <div class="card-header">
                <div>
                  <span class="card-title">通用设置</span>
                  <div class="card-desc">系统名称与版权信息，保存后立即同步到页面标题、侧边栏品牌、登录页与页脚</div>
                </div>
                <el-tag size="small" effect="plain">基础</el-tag>
              </div>
            </template>
            <el-form label-width="140px" label-position="left">
              <el-form-item label="系统名称">
                <el-input v-model="form.systemName" maxlength="64" style="max-width: 420px;" />
                <div class="field-desc">显示在页面标题、侧边栏品牌、登录页、邮件标题、通知等位置</div>
              </el-form-item>
              <el-form-item label="版权信息">
                <el-input v-model="form.copyrightText" maxlength="256" style="max-width: 420px;" />
                <div class="field-desc">页面底部版权声明，支持 HTML</div>
              </el-form-item>
              <el-divider content-position="left">公司信息（用于合同甲方自动带出）</el-divider>
              <el-form-item label="公司名称">
                <el-input v-model="form.companyName" maxlength="128" style="max-width: 420px;" />
                <div class="field-desc">合同甲方（用人单位）名称</div>
              </el-form-item>
              <el-form-item label="统一社会信用代码">
                <el-input v-model="form.companyCreditCode" maxlength="64" style="max-width: 420px;" />
                <div class="field-desc">合同甲方统一社会信用代码</div>
              </el-form-item>
              <el-form-item label="公司住所">
                <el-input v-model="form.companyAddress" maxlength="256" style="max-width: 420px;" />
                <div class="field-desc">合同甲方注册住所</div>
              </el-form-item>
              <el-form-item label="法定代表人">
                <el-input v-model="form.companyLegalRep" maxlength="64" style="max-width: 420px;" />
                <div class="field-desc">合同甲方法定代表人</div>
              </el-form-item>
            </el-form>
          </el-card>
        </template>

        <template v-if="activeCategory === 'email'">
          <el-card shadow="never">
            <template #header>
              <div class="card-header">
                <div>
                  <span class="card-title">邮件配置</span>
                  <div class="card-desc">影响注册邮箱后缀、验证码邮件与测评邀请邮件的发件信息</div>
                </div>
                <el-tag size="small" type="warning" effect="plain">邮件</el-tag>
              </div>
            </template>
            <el-form label-width="140px" label-position="left">
              <el-form-item label="邮箱后缀">
                <el-input v-model="form.emailSuffix" maxlength="128" style="max-width: 420px;">
                  <template #prepend>用户名</template>
                </el-input>
                <div class="field-desc">用户邮箱 = 用户名 + 此后缀，如 user@company.com</div>
              </el-form-item>
              <el-form-item label="发件人名称">
                <el-input v-model="form.emailSenderName" maxlength="64" style="max-width: 420px;" />
                <div class="field-desc">系统发出的邮件中显示的发送者名称</div>
              </el-form-item>
              <el-form-item label="发件人邮箱">
                <el-input v-model="form.emailSenderAddress" maxlength="128" style="max-width: 420px;" />
                <div class="field-desc">系统发出的邮件 From 地址（需与 SMTP 账号匹配），留空使用 SMTP 账号</div>
              </el-form-item>
            </el-form>
          </el-card>
        </template>

        <template v-if="activeCategory === 'security'">
          <el-card shadow="never">
            <template #header>
              <div class="card-header">
                <div>
                  <span class="card-title">安全策略</span>
                  <div class="card-desc">密码策略对注册、修改密码、重置密码立即生效；登录锁定与会话超时对新的登录生效</div>
                </div>
                <el-tag size="small" type="danger" effect="plain">安全</el-tag>
              </div>
            </template>
            <el-form label-width="160px" label-position="left">
              <el-form-item label="密码最小长度">
                <el-input-number v-model="form.passwordMinLength" :min="4" :max="32" style="width: 200px;" />
                <div class="field-desc">用户密码允许的最短字符数</div>
              </el-form-item>
              <el-form-item label="密码最大长度">
                <el-input-number v-model="form.passwordMaxLength" :min="8" :max="128" style="width: 200px;" />
                <div class="field-desc">用户密码允许的最长字符数</div>
              </el-form-item>
              <el-form-item label="密码需含特殊字符">
                <el-switch v-model="form.passwordRequireSpecial" active-value="1" inactive-value="0" />
                <span class="switch-label">{{ form.passwordRequireSpecial === '1' ? '是 — 须包含 !@#$%^&* 等' : '否 — 不强制要求特殊字符' }}</span>
              </el-form-item>
              <el-form-item label="登录失败锁定次数">
                <el-input-number v-model="form.loginMaxAttempts" :min="1" :max="20" style="width: 200px;" />
                <div class="field-desc">连续登录失败超过此次数后账号临时锁定 30 分钟</div>
              </el-form-item>
              <el-form-item label="会话超时（分钟）">
                <el-input-number v-model="form.sessionTimeoutMinutes" :min="5" :max="1440" style="width: 200px;" />
                <div class="field-desc">用户无操作超过此时长后需重新登录（对保存后的新登录生效）</div>
              </el-form-item>
              <el-form-item label="页面水印">
                <el-switch v-model="form.watermarkEnabled" active-value="1" inactive-value="0" />
                <span class="switch-label">
                  {{ form.watermarkEnabled === '1' ? '开启 — 全页面显示账号、姓名与时间水印' : '关闭 — 不显示水印' }}
                </span>
                <div class="field-desc">开启后所有页面显示半透明水印（防截图泄露），保存后立即生效</div>
              </el-form-item>
            </el-form>
          </el-card>
        </template>

        <template v-if="activeCategory === 'file'">
          <el-card shadow="never">
            <template #header>
              <div class="card-header">
                <div>
                  <span class="card-title">文件管理</span>
                  <div class="card-desc">限制全平台文件上传的大小与类型，保存后立即对新的上传生效</div>
                </div>
                <el-tag size="small" type="success" effect="plain">存储</el-tag>
              </div>
            </template>
            <el-form label-width="160px" label-position="left">
              <el-form-item label="上传大小限制（MB）">
                <el-input-number v-model="form.uploadMaxSizeMb" :min="1" :max="100" style="width: 200px;" />
                <div class="field-desc">单个文件上传的最大体积</div>
              </el-form-item>
              <el-form-item label="允许的文件类型">
                <el-input
                  v-model="form.uploadAllowedExtensions"
                  placeholder="如 .pdf,.jpg,.png"
                  style="max-width: 520px;"
                />
                <div class="field-desc">逗号分隔的扩展名列表，如 .pdf,.jpg,.png,.docx</div>
              </el-form-item>
            </el-form>
          </el-card>
        </template>

        <template v-if="activeCategory === 'onboarding'">
          <el-card shadow="never">
            <template #header>
              <div class="card-header">
                <div>
                  <span class="card-title">入职管理</span>
                  <div class="card-desc">影响入职流程的账号默认密码、培训模块清单与欢迎消息内容</div>
                </div>
                <el-tag size="small" type="primary" effect="plain">入职</el-tag>
              </div>
            </template>
            <el-form label-width="140px" label-position="left">
              <el-form-item label="默认初始密码">
                <el-input v-model="form.onboardingDefaultPassword" maxlength="32" style="max-width: 300px;" show-password />
                <div class="field-desc">创建新员工入职账号且未指定密码时的默认密码</div>
              </el-form-item>
              <el-form-item label="培训模块列表">
                <el-input
                  v-model="form.onboardingTrainingModules"
                  placeholder="逗号分隔，如 公司文化与制度,信息安全培训"
                  style="max-width: 520px;"
                />
                <div class="field-desc">逗号分隔的培训模块名称，将出现在入职培训步骤中</div>
              </el-form-item>
              <el-form-item label="欢迎消息模板">
                <el-input
                  v-model="form.onboardingWelcomeTemplate"
                  type="textarea"
                  :rows="8"
                  style="max-width: 640px;"
                />
                <div class="field-desc">
                  支持变量：<code>{<!-- -->{employeeName}}</code>
                  <code>{<!-- -->{departmentName}}</code>
                  <code>{<!-- -->{jobTitle}}</code>
                  <code>{<!-- -->{level}}</code>
                  <code>{<!-- -->{onboardDate}}</code>
                </div>
              </el-form-item>
            </el-form>
          </el-card>
        </template>

        <template v-if="activeCategory === 'ai'">
          <el-card shadow="never">
            <template #header>
              <div class="card-header">
                <div>
                  <span class="card-title">AI 引擎</span>
                  <div class="card-desc">留任风险评估的等级切分阈值，保存后立即对新的评估生效</div>
                </div>
                <el-tag size="small" type="info" effect="plain">智能</el-tag>
              </div>
            </template>
            <el-form label-width="160px" label-position="left">
              <el-form-item label="低风险阈值">
                <el-input-number v-model="form.aiRiskThresholdLow" :min="0" :max="1" :step="0.05" :precision="2" style="width: 200px;" />
                <div class="field-desc">低于此值为低留任风险</div>
              </el-form-item>
              <el-form-item label="中风险阈值">
                <el-input-number v-model="form.aiRiskThresholdMedium" :min="0" :max="1" :step="0.05" :precision="2" style="width: 200px;" />
                <div class="field-desc">介于低风险和高风险之间为中风险</div>
              </el-form-item>
              <el-form-item label="高风险阈值">
                <el-input-number v-model="form.aiRiskThresholdHigh" :min="0" :max="1" :step="0.05" :precision="2" style="width: 200px;" />
                <div class="field-desc">高于此值为高留任风险</div>
              </el-form-item>
            </el-form>
          </el-card>
        </template>

        <template v-if="activeCategory === 'ai-screen'">
          <el-card shadow="never">
            <template #header>
              <div class="card-header">
                <div>
                  <span class="card-title">AI 筛选配置</span>
                  <div class="card-desc">AI 简历初筛的 5 个维度权重与最低通过分数线，保存后对新的筛选立即生效</div>
                </div>
                <el-tag size="small" type="info" effect="plain">智能</el-tag>
              </div>
            </template>
            <el-form label-width="200px" label-position="left">
              <div class="weight-sum-hint" :class="{ 'weight-ok': weightSum === 100, 'weight-warn': weightSum !== 100 }">
                五项权重合计：{{ weightSum }}%（应为 100%）
              </div>
              <el-form-item label="学历权重（%）">
                <el-input-number v-model="form.aiScreenWeightEducation" :min="0" :max="100" style="width: 200px;" />
                <div class="field-desc">教育背景与职位要求的匹配度权重</div>
              </el-form-item>
              <el-form-item label="技能权重（%）">
                <el-input-number v-model="form.aiScreenWeightSkill" :min="0" :max="100" style="width: 200px;" />
                <div class="field-desc">候选人技能与职位要求技能的匹配度权重</div>
              </el-form-item>
              <el-form-item label="经验权重（%）">
                <el-input-number v-model="form.aiScreenWeightExperience" :min="0" :max="100" style="width: 200px;" />
                <div class="field-desc">相关工作经验的深度和广度权重</div>
              </el-form-item>
              <el-form-item label="行为权重（%）">
                <el-input-number v-model="form.aiScreenWeightBehavior" :min="0" :max="100" style="width: 200px;" />
                <div class="field-desc">团队协作、学习能力、职业稳定性等综合表现权重</div>
              </el-form-item>
              <el-form-item label="语义权重（%）">
                <el-input-number v-model="form.aiScreenWeightSemantic" :min="0" :max="100" style="width: 200px;" />
                <div class="field-desc">简历内容与职位描述在语义上的整体契合度权重</div>
              </el-form-item>
              <el-form-item label="最低通过分数线">
                <el-input-number v-model="form.aiScreenPassScore" :min="0" :max="100" style="width: 200px;" />
                <div class="field-desc">综合评分达到此分数即视为通过初筛（建议 60~75）</div>
              </el-form-item>
            </el-form>
          </el-card>
        </template>

        <!-- Save Actions -->
        <div class="settings-actions">
          <el-button
            type="primary"
            :loading="submitting"
            :disabled="!hasChanges"
            @click="handleSave"
          >
            <el-icon v-if="!submitting"><Check /></el-icon>
            <span>{{ submitting ? '保存中...' : '保存设置' }}</span>
          </el-button>
          <el-button
            :disabled="!hasChanges || submitting"
            @click="handleReset"
          >
            重置
          </el-button>
          <el-text type="info" size="small" class="save-tip">保存成功后立即生效，无需重启</el-text>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Setting, Check, Cpu, Files, Lock, Message, Tickets, ScaleToOriginal,
} from '@element-plus/icons-vue'
import { getConfigs, updateConfigs } from '@/api/system'
import { useSettingsStore } from '@/stores/settings'
import type { SysConfigVO } from '@/types/models'

// --------------- Category definitions ---------------
interface CategoryItem {
  key: string
  label: string
  icon: any
}

const categories: CategoryItem[] = [
  { key: 'general', label: '通用设置', icon: Setting },
  { key: 'email', label: '邮件配置', icon: Message },
  { key: 'security', label: '安全策略', icon: Lock },
  { key: 'file', label: '文件管理', icon: Files },
  { key: 'onboarding', label: '入职管理', icon: Tickets },
  { key: 'ai', label: 'AI 引擎', icon: Cpu },
  { key: 'ai-screen', label: 'AI 筛选配置', icon: ScaleToOriginal },
]

const activeCategory = ref('general')
const settingsStore = useSettingsStore()

// --------------- Form state ---------------
interface SettingsForm {
  [key: string]: string | number
  systemName: string
  copyrightText: string
  companyName: string
  companyCreditCode: string
  companyAddress: string
  companyLegalRep: string
  emailSuffix: string
  emailSenderName: string
  emailSenderAddress: string
  passwordMinLength: number
  passwordMaxLength: number
  passwordRequireSpecial: string
  loginMaxAttempts: number
  sessionTimeoutMinutes: number
  watermarkEnabled: string
  uploadMaxSizeMb: number
  uploadAllowedExtensions: string
  onboardingDefaultPassword: string
  onboardingTrainingModules: string
  onboardingWelcomeTemplate: string
  aiRiskThresholdLow: number
  aiRiskThresholdMedium: number
  aiRiskThresholdHigh: number
  aiScreenWeightEducation: number
  aiScreenWeightSkill: number
  aiScreenWeightExperience: number
  aiScreenWeightBehavior: number
  aiScreenWeightSemantic: number
  aiScreenPassScore: number
}

const form = reactive<SettingsForm>({
  systemName: 'SmartRecruit',
  copyrightText: '',
  companyName: 'SmartRecruit 科技有限公司',
  companyCreditCode: '',
  companyAddress: '',
  companyLegalRep: '',
  emailSuffix: '@company.com',
  emailSenderName: 'SmartRecruit',
  emailSenderAddress: '',
  passwordMinLength: 6,
  passwordMaxLength: 64,
  passwordRequireSpecial: '0',
  loginMaxAttempts: 5,
  sessionTimeoutMinutes: 120,
  watermarkEnabled: '0',
  uploadMaxSizeMb: 10,
  uploadAllowedExtensions: '',
  onboardingDefaultPassword: '123456',
  onboardingTrainingModules: '',
  onboardingWelcomeTemplate: '',
  aiRiskThresholdLow: 0.25,
  aiRiskThresholdMedium: 0.50,
  aiRiskThresholdHigh: 0.75,
  aiScreenWeightEducation: 20,
  aiScreenWeightSkill: 35,
  aiScreenWeightExperience: 20,
  aiScreenWeightBehavior: 20,
  aiScreenWeightSemantic: 15,
  aiScreenPassScore: 70,
})

const loading = ref(true)
const submitting = ref(false)
const lastUpdated = ref('')
const lastUpdatedBy = ref('')

// Snapshot for change detection
const originalValues = reactive<Record<string, any>>({})

// Numeric fields for value conversion
const numericFields = [
  'passwordMinLength', 'passwordMaxLength', 'loginMaxAttempts',
  'sessionTimeoutMinutes', 'uploadMaxSizeMb',
  'aiRiskThresholdLow', 'aiRiskThresholdMedium', 'aiRiskThresholdHigh',
  'aiScreenWeightEducation', 'aiScreenWeightSkill', 'aiScreenWeightExperience',
  'aiScreenWeightBehavior', 'aiScreenWeightSemantic', 'aiScreenPassScore',
]

// Detect if any field in the current category has changed
const hasChanges = computed(() => {
  const fields = categoryFields[activeCategory.value]
  if (!fields) return false
  return fields.some(f => String((form as any)[f]) !== String(originalValues[f]))
})

/** AI 筛选 5 维权重合计（保存时校验必须为 100）。 */
const weightSum = computed(() =>
  form.aiScreenWeightEducation + form.aiScreenWeightSkill
  + form.aiScreenWeightExperience + form.aiScreenWeightBehavior
  + form.aiScreenWeightSemantic)

// Map category to its form fields
const categoryFields: Record<string, string[]> = {
  general: ['systemName', 'copyrightText', 'companyName', 'companyCreditCode', 'companyAddress', 'companyLegalRep'],
  email: ['emailSuffix', 'emailSenderName', 'emailSenderAddress'],
  security: ['passwordMinLength', 'passwordMaxLength', 'passwordRequireSpecial', 'loginMaxAttempts', 'sessionTimeoutMinutes', 'watermarkEnabled'],
  file: ['uploadMaxSizeMb', 'uploadAllowedExtensions'],
  onboarding: ['onboardingDefaultPassword', 'onboardingTrainingModules', 'onboardingWelcomeTemplate'],
  ai: ['aiRiskThresholdLow', 'aiRiskThresholdMedium', 'aiRiskThresholdHigh'],
  'ai-screen': ['aiScreenWeightEducation', 'aiScreenWeightSkill', 'aiScreenWeightExperience',
    'aiScreenWeightBehavior', 'aiScreenWeightSemantic', 'aiScreenPassScore'],
}

// Field-to-configKey mapping
const fieldToKey: Record<string, string> = {
  systemName: 'system_name',
  copyrightText: 'copyright_text',
  companyName: 'company_name',
  companyCreditCode: 'company_credit_code',
  companyAddress: 'company_address',
  companyLegalRep: 'company_legal_representative',
  emailSuffix: 'email_suffix',
  emailSenderName: 'email_sender_name',
  emailSenderAddress: 'email_sender_address',
  passwordMinLength: 'password_min_length',
  passwordMaxLength: 'password_max_length',
  passwordRequireSpecial: 'password_require_special',
  loginMaxAttempts: 'login_max_attempts',
  sessionTimeoutMinutes: 'session_timeout_minutes',
  watermarkEnabled: 'watermark_enabled',
  uploadMaxSizeMb: 'upload_max_size_mb',
  uploadAllowedExtensions: 'upload_allowed_extensions',
  onboardingDefaultPassword: 'onboarding_default_password',
  onboardingTrainingModules: 'onboarding_training_modules',
  onboardingWelcomeTemplate: 'onboarding_welcome_template',
  aiRiskThresholdLow: 'ai_risk_threshold_low',
  aiRiskThresholdMedium: 'ai_risk_threshold_medium',
  aiRiskThresholdHigh: 'ai_risk_threshold_high',
  aiScreenWeightEducation: 'ai_screen_weight_education',
  aiScreenWeightSkill: 'ai_screen_weight_skill',
  aiScreenWeightExperience: 'ai_screen_weight_experience',
  aiScreenWeightBehavior: 'ai_screen_weight_behavior',
  aiScreenWeightSemantic: 'ai_screen_weight_semantic',
  aiScreenPassScore: 'ai_screen_pass_score',
}

const keyToField = Object.fromEntries(
  Object.entries(fieldToKey).map(([k, v]) => [v, k])
)

// --------------- Lifecycle ---------------
onMounted(async () => {
  loading.value = true
  try {
    const configs: SysConfigVO[] = await getConfigs()
    for (const config of configs) {
      const field = keyToField[config.configKey]
      if (!field) continue
      if (numericFields.includes(field)) {
        (form as any)[field] = Number(config.configValue)
      } else {
        (form as any)[field] = config.configValue
      }
    }

    // 审计信息：取最近一次更新的配置项时间与操作人
    const updated = configs
      .filter(c => c.updateTime)
      .sort((a, b) => b.updateTime.localeCompare(a.updateTime))[0]
    if (updated) {
      lastUpdated.value = updated.updateTime.replace('T', ' ').slice(0, 19)
      lastUpdatedBy.value = updated.updateBy || ''
    }
  } catch {
    // fallback to defaults
  } finally {
    loading.value = false
  }

  snapshotAll()
})

function snapshotAll() {
  for (const field of Object.keys(fieldToKey)) {
    originalValues[field] = String((form as any)[field])
  }
}

// --------------- Category switch ---------------
function onCategorySelect(key: string) {
  activeCategory.value = key
}

// --------------- Validation ---------------
function validateCategory(category: string): boolean {
  switch (category) {
    case 'general':
      if (!form.systemName.trim()) {
        ElMessage.warning('系统名称不能为空')
        return false
      }
      return true
    case 'email':
      if (!form.emailSuffix.trim()) {
        ElMessage.warning('邮箱后缀不能为空')
        return false
      }
      if (form.emailSenderAddress.trim()
        && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.emailSenderAddress.trim())) {
        ElMessage.warning('发件人邮箱格式不正确')
        return false
      }
      return true
    case 'security':
      if (form.passwordMinLength > form.passwordMaxLength) {
        ElMessage.warning('密码最小长度不能大于最大长度')
        return false
      }
      return true
    case 'file': {
      const extensions = form.uploadAllowedExtensions.split(',')
        .map(s => s.trim()).filter(Boolean)
      if (extensions.length === 0) {
        ElMessage.warning('至少需要填写一个允许的文件类型')
        return false
      }
      return true
    }
    case 'onboarding':
      if (!form.onboardingDefaultPassword.trim()) {
        ElMessage.warning('默认初始密码不能为空')
        return false
      }
      if (form.onboardingTrainingModules.split(',').map(s => s.trim()).filter(Boolean).length === 0) {
        ElMessage.warning('至少需要一个培训模块')
        return false
      }
      if (!form.onboardingWelcomeTemplate.trim()) {
        ElMessage.warning('欢迎消息模板不能为空')
        return false
      }
      // 默认密码需满足当前表单中的密码策略，否则入职建号会失败
      if (form.onboardingDefaultPassword.length < form.passwordMinLength
        || form.onboardingDefaultPassword.length > form.passwordMaxLength) {
        ElMessage.warning(`默认初始密码长度需在 ${form.passwordMinLength}~${form.passwordMaxLength} 位之间，与密码策略保持一致`)
        return false
      }
      return true
    case 'ai':
      if (!(form.aiRiskThresholdLow <= form.aiRiskThresholdMedium
        && form.aiRiskThresholdMedium <= form.aiRiskThresholdHigh)) {
        ElMessage.warning('AI 风险阈值必须满足：低风险 ≤ 中风险 ≤ 高风险')
        return false
      }
      return true
    case 'ai-screen':
      if (weightSum.value !== 100) {
        ElMessage.warning(`AI 筛选五项权重合计必须为 100%，当前为 ${weightSum.value}%`)
        return false
      }
      if (form.aiScreenPassScore < 0 || form.aiScreenPassScore > 100) {
        ElMessage.warning('最低通过分数线需在 0~100 之间')
        return false
      }
      return true
    default:
      return true
  }
}

// --------------- Save / Reset ---------------
async function handleSave() {
  if (!validateCategory(activeCategory.value)) return

  submitting.value = true
  try {
    const fields = categoryFields[activeCategory.value]
    const items = fields.map(f => ({
      configKey: fieldToKey[f],
      configValue: String((form as any)[f]),
    }))

    await updateConfigs(items)

    // 更新快照，并刷新全局配置（标题、品牌、页脚、密码策略、上传限制等立即生效）
    for (const f of fields) {
      originalValues[f] = String((form as any)[f])
    }
    await settingsStore.refresh()

    const label = categories.find(c => c.key === activeCategory.value)?.label || ''
    ElMessage.success(`${label}保存成功，已立即生效`)
  } catch {
    // HTTP interceptor handles errors
  } finally {
    submitting.value = false
  }
}

function handleReset() {
  const fields = categoryFields[activeCategory.value]
  for (const f of fields) {
    const original = originalValues[f]
    if (numericFields.includes(f)) {
      (form as any)[f] = Number(original)
    } else {
      (form as any)[f] = original
    }
  }
}
</script>

<style scoped>
.settings-page {
  max-width: 1100px;
}

/* Page Header */
.page-header {
  margin-bottom: 24px;
}

.page-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
  color: var(--c-text);
  margin-bottom: 4px;
}

.page-meta {
  margin-top: 8px;
  font-size: 12px;
  color: var(--c-text-muted);
}

/* Layout */
.settings-layout {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

/* Left Sidebar */
.settings-sidebar {
  flex-shrink: 0;
  width: 180px;
}

.category-menu {
  border-right: none;
  border-radius: var(--c-radius-lg);
  overflow: hidden;
}

.category-menu :deep(.el-menu-item) {
  height: 44px;
  line-height: 44px;
  font-size: 14px;
}

.category-menu :deep(.el-menu-item .el-icon) {
  margin-right: 8px;
}

/* Right Content */
.settings-content {
  flex: 1;
  min-width: 0;
}

.card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
}

.card-desc {
  margin-top: 4px;
  font-size: 12px;
  color: var(--c-text-muted);
}

/* Form */
.field-desc {
  font-size: 12px;
  color: var(--c-text-muted);
  margin-top: 4px;
  margin-left: 12px;
  line-height: 1.5;
}

.field-desc code {
  background: var(--el-fill-color-light);
  padding: 1px 4px;
  border-radius: 3px;
  font-size: 11px;
  margin: 0 1px;
}

.weight-sum-hint {
  font-size: 13px;
  font-weight: 600;
  margin: 0 0 16px 200px;
  padding: 8px 12px;
  border-radius: 6px;
  display: inline-block;
}

.weight-sum-hint.weight-ok {
  color: #059669;
  background: #ecfdf5;
}

.weight-sum-hint.weight-warn {
  color: #d97706;
  background: #fffbeb;
}

.switch-label {
  margin-left: 10px;
  font-size: 13px;
  color: var(--c-text-secondary);
}

/* Actions */
.settings-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid var(--c-border-light);
}

.save-tip {
  margin-left: 8px;
}
</style>

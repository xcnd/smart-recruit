<template>
  <div class="rd-page" v-loading="loading">
    <!-- Top Bar -->
    <div class="rd-topbar">
      <el-button text :icon="ArrowLeft" @click="$router.push('/referral')" class="rd-back-btn">
        返回内推管理
      </el-button>
      <div class="rd-topbar-actions" v-if="program">
        <el-button :icon="Share" @click="openShareDialog">分享链接</el-button>
        <el-button v-if="isAdmin" @click="$router.push(`/referral/${program.id}/jobs`)">管理职位</el-button>
      </div>
    </div>

    <!-- Error State -->
    <div v-if="error" class="sr-section" style="text-align: center; padding: 80px 24px;">
      <el-icon :size="48" color="var(--c-danger)"><WarningFilled /></el-icon>
      <p style="margin-top: 16px; font-size: 16px; color: var(--c-text-secondary);">加载失败，请稍后重试</p>
      <p style="margin-top: 8px; font-size: 13px; color: var(--c-text-muted);">{{ error }}</p>
      <el-button type="primary" style="margin-top: 20px;" @click="fetchDetail">重新加载</el-button>
    </div>

    <!-- Not Found -->
    <div v-else-if="!loading && !program" class="sr-section" style="text-align: center; padding: 80px 24px;">
      <el-empty description="未找到该内推计划">
        <el-button type="primary" @click="$router.replace('/referral')">返回内推管理</el-button>
      </el-empty>
    </div>

    <!-- Content -->
    <template v-else-if="program">
      <!-- Hero Header Card -->
      <div class="rd-hero">
        <div class="rd-hero-top">
          <div class="rd-hero-title-row">
            <h1 class="rd-hero-title">{{ program.title }}</h1>
            <el-tag
              :type="program.status === 1 ? 'success' : 'danger'"
              size="large"
              effect="dark"
              class="rd-status-tag"
            >
              {{ program.status === 1 ? '启用中' : '已停用' }}
            </el-tag>
          </div>
          <div class="rd-hero-bonus">
            <span class="rd-hero-bonus-label">内推奖金</span>
            <span class="rd-hero-bonus-value">{{ formatCurrency(program.bonusAmount) }}</span>
          </div>
        </div>

        <div class="rd-hero-meta">
          <span class="rd-hero-meta-item">
            <el-icon><Calendar /></el-icon>{{ formatDate(program.startDate) }}
            <template v-if="program.endDate"> 至 {{ formatDate(program.endDate) }}</template>
          </span>
          <span class="rd-meta-divider">·</span>
          <span class="rd-hero-meta-item" v-if="program.createBy">
            <el-icon><User /></el-icon>{{ program.createBy }}
          </span>
        </div>

        <!-- CTA Buttons -->
        <div class="rd-hero-actions">
          <el-button
            type="primary"
            size="large"
            :icon="UserFilled"
            :disabled="program.status === 0"
            @click="openReferDialog"
          >
            立即推荐
          </el-button>
          <el-button
            v-if="isAdmin"
            size="large"
            :icon="DocumentAdd"
            @click="$router.push(`/referral/${program.id}/jobs`)"
          >
            管理职位
          </el-button>
        </div>

        <!-- Stats Row -->
        <div class="rd-stats-row">
          <div class="rd-stat-card">
            <div class="rd-stat-icon" style="background: var(--c-primary-bg); color: var(--c-primary);">
              <el-icon :size="20"><Briefcase /></el-icon>
            </div>
            <div class="rd-stat-value">{{ program.bonusAmount.toLocaleString() }}</div>
            <div class="rd-stat-label">默认奖金 (元)</div>
          </div>
          <div class="rd-stat-card">
            <div class="rd-stat-icon" style="background: var(--c-success-bg); color: var(--c-success);">
              <el-icon :size="20"><Calendar /></el-icon>
            </div>
            <div class="rd-stat-value">{{ formatDate(program.startDate) }}</div>
            <div class="rd-stat-label">开始日期</div>
          </div>
          <div class="rd-stat-card">
            <div class="rd-stat-icon" style="background: var(--c-info-bg); color: var(--c-info);">
              <el-icon :size="20"><Clock /></el-icon>
            </div>
            <div class="rd-stat-value">{{ formatDate(program.endDate) || '长期有效' }}</div>
            <div class="rd-stat-label">结束日期</div>
          </div>
          <div class="rd-stat-card">
            <div class="rd-stat-icon" style="background: var(--c-warning-bg); color: var(--c-warning);">
              <el-icon :size="20"><Tickets /></el-icon>
            </div>
            <div class="rd-stat-value">{{ bonusStructureEntries.length || '-' }}</div>
            <div class="rd-stat-label">奖金阶梯数</div>
          </div>
        </div>
      </div>

      <!-- Body: Two-column Layout -->
      <div class="rd-body">
        <!-- Left Column -->
        <div class="rd-main">
          <!-- Description -->
          <div class="rd-section" v-if="program.description">
            <h2 class="rd-section-title">计划说明</h2>
            <div class="rd-description">{{ program.description }}</div>
          </div>

          <!-- Bonus Structure -->
          <div class="rd-section" v-if="bonusStructureEntries.length">
            <h2 class="rd-section-title">奖金发放阶段</h2>
            <div class="rd-bonus-grid">
              <div
                v-for="(item, index) in bonusStructureEntries"
                :key="item.key"
                class="rd-bonus-card"
              >
                <div class="rd-bonus-card-step">第{{ index + 1 }}期</div>
                <div class="rd-bonus-card-label">{{ item.label }}</div>
                <div class="rd-bonus-card-amount">¥{{ item.amount.toLocaleString('zh-CN') }}</div>
              </div>
            </div>
            <div class="rd-bonus-total">
              合计：<strong>¥{{ bonusTotal.toLocaleString('zh-CN') }}</strong>
            </div>
          </div>
        </div>

        <!-- Right Sidebar -->
        <div class="rd-sidebar">
          <div class="rd-sidebar-card">
            <h3 class="rd-sidebar-title">基本信息</h3>
            <div class="rd-sidebar-item">
              <span class="rd-sidebar-key">状态</span>
              <span class="rd-sidebar-value">
                <el-tag :type="program.status === 1 ? 'success' : 'danger'" size="small">
                  {{ program.status === 1 ? '启用中' : '已停用' }}
                </el-tag>
              </span>
            </div>
            <div class="rd-sidebar-item">
              <span class="rd-sidebar-key">默认奖金</span>
              <span class="rd-sidebar-value rd-sidebar-value-bold">{{ formatCurrency(program.bonusAmount) }}</span>
            </div>
            <div class="rd-sidebar-item">
              <span class="rd-sidebar-key">开始日期</span>
              <span class="rd-sidebar-value">{{ formatDate(program.startDate) }}</span>
            </div>
            <div class="rd-sidebar-item">
              <span class="rd-sidebar-key">结束日期</span>
              <span class="rd-sidebar-value">{{ formatDate(program.endDate) || '长期有效' }}</span>
            </div>
            <div class="rd-sidebar-item">
              <span class="rd-sidebar-key">创建人</span>
              <span class="rd-sidebar-value">{{ program.createBy || '-' }}</span>
            </div>
            <div class="rd-sidebar-item">
              <span class="rd-sidebar-key">创建时间</span>
              <span class="rd-sidebar-value">{{ formatDateTime(program.createTime) }}</span>
            </div>
            <div class="rd-sidebar-item">
              <span class="rd-sidebar-key">更新时间</span>
              <span class="rd-sidebar-value">{{ formatDateTime(program.updateTime) }}</span>
            </div>
          </div>

          <!-- Quick Links -->
          <div class="rd-sidebar-card">
            <h3 class="rd-sidebar-title">快捷操作</h3>
            <div class="rd-sidebar-links">
              <div v-if="isAdmin" class="rd-sidebar-link-item" @click="$router.push(`/referral/${program.id}/jobs`)">
                <el-icon :size="16"><Setting /></el-icon>
                <span>管理计划职位</span>
              </div>
              <div class="rd-sidebar-link-item" @click="$router.push('/referral/records')">
                <el-icon :size="16"><List /></el-icon>
                <span>查看内推记录</span>
              </div>
              <div class="rd-sidebar-link-item" @click="$router.push('/referral/leaderboard')">
                <el-icon :size="16"><Trophy /></el-icon>
                <span>内推排行榜</span>
              </div>
              <div class="rd-sidebar-link-item" @click="$router.push('/referral/policy')">
                <el-icon :size="16"><Document /></el-icon>
                <span>内推政策</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- Share Dialog -->
    <el-dialog
      v-model="shareDialogVisible"
      title="生成分享链接"
      width="560px"
      :close-on-click-modal="false"
    >
      <div class="rd-share-section">
        <div class="rd-share-label">计划名称</div>
        <div class="rd-share-value">{{ program?.title || '-' }}</div>
      </div>

      <div class="rd-share-section">
        <div class="rd-share-label">内推奖金</div>
        <div class="rd-share-value" style="color: var(--c-primary); font-weight: 600;">
          ¥{{ (program?.bonusAmount || 0).toLocaleString('zh-CN') }}
        </div>
      </div>

      <el-divider />

      <div v-if="!shareUrl" class="rd-share-generate">
        <p class="rd-share-hint">生成固定长度的分享令牌和链接</p>
        <el-button type="primary" :loading="shareGenerating" @click="generateShareToken">
          生成分享链接
        </el-button>
      </div>

      <template v-else>
        <div v-if="shareReferralCode" class="rd-share-section">
          <div class="rd-share-label">
            内推码
          </div>
          <div class="rd-share-url-box rd-share-code-box">
            <code class="rd-share-url rd-share-code">{{ shareReferralCode }}</code>
          </div>
          <p class="rd-share-code-hint">通过此内推码提交的推荐或注册将与您绑定</p>
        </div>

        <div class="rd-share-section">
          <div class="rd-share-label">
            分享链接
          </div>
          <div class="rd-share-url-box">
            <code class="rd-share-url">{{ shareUrl }}</code>
          </div>
        </div>

        <div class="rd-share-actions">
          <el-button @click="generateShareToken" :loading="shareGenerating">重新生成</el-button>
          <el-button type="primary" :icon="CopyDocument" @click="copyShareUrl">
            {{ shareCopied ? '已复制' : '复制链接' }}
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- Referral Dialog -->
    <el-dialog
      v-model="referDialogVisible"
      title="推荐候选人"
      width="600px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="referForm"
        :rules="referRules"
        label-width="120px"
        label-position="right"
        @submit.prevent
      >
        <el-divider content-position="left">关联信息</el-divider>
        <el-form-item label="计划职位" prop="programJobId">
          <el-select
            v-model="referForm.programJobId"
            placeholder="请选择计划关联职位"
            style="width: 100%;"
            :loading="loadingProgramJobs"
          >
            <el-option
              v-for="pj in programJobs"
              :key="pj.id"
              :label="getJobPositionTitle(pj.jobPositionId) + (pj.bonusAmount ? ' (奖金¥' + pj.bonusAmount + ')' : '')"
              :value="pj.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="目标职位" prop="jobId">
          <el-select
            v-model="referForm.jobId"
            placeholder="请选择目标职位"
            style="width: 100%;"
            :loading="loadingJobs"
            filterable
          >
            <el-option
              v-for="job in availableJobs"
              :key="job.id"
              :label="job.title + ' (' + job.departmentName + ')'"
              :value="job.id"
            />
          </el-select>
        </el-form-item>

        <el-divider content-position="left">候选人信息</el-divider>
        <el-form-item label="候选人ID" prop="candidateId">
          <el-input
            v-model="referForm.candidateId"
            placeholder="请输入候选人ID"
          />
        </el-form-item>
        <el-form-item label="候选人姓名" prop="candidateName">
          <el-input
            v-model="referForm.candidateName"
            placeholder="请输入候选人姓名"
            maxlength="50"
          />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input
            v-model="referForm.candidatePhone"
            placeholder="选填"
            maxlength="20"
          />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input
            v-model="referForm.candidateEmail"
            placeholder="选填"
            maxlength="100"
          />
        </el-form-item>
        <el-form-item label="与候选人关系">
          <el-select v-model="referForm.relationship" placeholder="请选择关系" style="width: 100%;">
            <el-option label="同事" value="同事" />
            <el-option label="朋友" value="朋友" />
            <el-option label="前同事" value="前同事" />
            <el-option label="校友" value="校友" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>

        <el-divider content-position="left">推荐人信息</el-divider>
        <el-form-item label="推荐人姓名">
          <el-input v-model="referForm.referrerName" placeholder="自动填充当前用户" maxlength="50" />
        </el-form-item>
        <el-form-item label="推荐人部门">
          <el-input v-model="referForm.referrerDept" placeholder="自动填充当前部门" maxlength="100" />
        </el-form-item>

        <el-divider content-position="left">其他</el-divider>
        <el-form-item label="推荐备注">
          <el-input
            v-model="referForm.referralNote"
            type="textarea"
            :rows="3"
            placeholder="选填"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="推荐奖金">
          <el-input-number
            v-model="referForm.bonus"
            :min="0"
            :step="500"
            controls-position="right"
            style="width: 100%;"
            placeholder="默认使用计划奖金"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="referDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleRefer">确认推荐</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import {
  ArrowLeft,
  Share,
  WarningFilled,
  UserFilled,
  DocumentAdd,
  Briefcase,
  Calendar,
  Clock,
  Tickets,
  User,
  Setting,
  List,
  Trophy,
  Document,
  CopyDocument,
} from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import request from '@/api/request'
import { getPrograms, getProgramJobs, createReferral } from '@/api/referral'
import { getJobs } from '@/api/job'
import type { ReferralProgramVO, RefProgramJobVO, JobVO } from '@/types/models'
import { useUserStore } from '@/stores/user'

// ---- Router & Store ----
const route = useRoute()
const userStore = useUserStore()

// ---- Permission ----
const isAdmin = computed(() => {
  const roleName = userStore.userInfo?.roleName || ''
  return roleName.includes('超级管理员') || roleName.includes('HR管理员')
})

// ---- State ----
const loading = ref(true)
const error = ref<string | null>(null)
const program = ref<ReferralProgramVO | null>(null)

// Share Dialog
const shareDialogVisible = ref(false)
const shareUrl = ref('')
const shareReferralCode = ref('')
const shareCopied = ref(false)
const shareGenerating = ref(false)

async function generateShareToken() {
  if (!program.value) return
  shareGenerating.value = true
  shareCopied.value = false
  try {
    const data = await request.post('/referrals/share-token', {
      programId: Number(program.value.id),
      referrerName: userStore.userInfo?.realName || userStore.userInfo?.name || '',
    })
    shareUrl.value = window.location.origin + data.url
    shareReferralCode.value = data.referralCode || ''
  } catch {
    ElMessage.error('生成分享链接失败，请稍后重试')
  } finally {
    shareGenerating.value = false
  }
}

function openShareDialog() {
  shareUrl.value = ''
  shareReferralCode.value = ''
  shareCopied.value = false
  shareDialogVisible.value = true
}

async function copyShareUrl() {
  try {
    await navigator.clipboard.writeText(shareUrl.value)
    shareCopied.value = true
    setTimeout(() => { shareCopied.value = false }, 2000)
  } catch {
    ElMessage.warning('复制失败，请手动复制')
  }
}

// Referral Dialog
const referDialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()

// Dropdown data
const programJobs = ref<RefProgramJobVO[]>([])
const availableJobs = ref<JobVO[]>([])
const loadingProgramJobs = ref(false)
const loadingJobs = ref(false)
// Maps jobPositionId → job title for program job dropdown display
const jobTitleMap = ref<Record<string, string>>({})

function getJobPositionTitle(jobPositionId: string): string {
  return jobTitleMap.value[jobPositionId] || '职位 #' + jobPositionId
}

interface ReferForm {
  programJobId: string | undefined
  candidateId: string
  candidateName: string
  candidatePhone: string
  candidateEmail: string
  jobId: string | undefined
  referrerName: string
  referrerDept: string
  relationship: string
  referralNote: string
  bonus: number | undefined
}

const referForm = reactive<ReferForm>({
  programJobId: undefined,
  candidateId: '',
  candidateName: '',
  candidatePhone: '',
  candidateEmail: '',
  jobId: undefined,
  referrerName: '',
  referrerDept: '',
  relationship: '',
  referralNote: '',
  bonus: undefined,
})

const referRules: FormRules<ReferForm> = {
  programJobId: [{ required: true, message: '请选择计划职位', trigger: 'change' }],
  candidateId: [{ required: true, message: '请输入候选人ID', trigger: 'blur' }],
  candidateName: [{ required: true, message: '请输入候选人姓名', trigger: 'blur' }],
  jobId: [{ required: true, message: '请选择目标职位', trigger: 'change' }],
}

// ---- Computed ----
interface BonusStructureEntry {
  key: string
  label: string
  amount: number
}

const NON_TIER_KEYS = new Set(['currency', 'note', 'remark', 'version'])

/** Bonus structure key → Chinese label mapping */
const BONUS_LABEL_MAP: Record<string, string> = {
  onboarding: '入职发放',
  one_year: '满一年发放',
  probation: '通过试用期',
  sign_on: '签约发放',
  referral: '推荐奖金',
  performance: '绩效奖金',
  retention: '留任奖金',
}

function resolveBonusLabel(key: string): string {
  return BONUS_LABEL_MAP[key] || key
}

function isEmptyObject(v: unknown): boolean {
  return v !== null && typeof v === 'object' && !Array.isArray(v) && Object.keys(v as object).length === 0
}

function extractAmount(value: unknown): number {
  if (typeof value === 'number') return value
  if (value && typeof value === 'object' && !Array.isArray(value)) {
    const obj = value as Record<string, unknown>
    if (typeof obj.amount === 'number') return obj.amount
    if (typeof obj.value === 'number') return obj.value
    // Object with no recognizable numeric field → skip
    return 0
  }
  return 0
}

const bonusStructureEntries = computed<BonusStructureEntry[]>(() => {
  let raw: unknown = program.value?.bonusStructure
  if (!raw) return []

  // JSON string from API → parse into object
  if (typeof raw === 'string') {
    try {
      raw = JSON.parse(raw)
    } catch {
      return []
    }
  }

  // If it's an array, try to detect if it's garbage (all empty objects)
  if (Array.isArray(raw)) {
    if (raw.length === 0) return []
    if (raw.every(isEmptyObject)) return []
    // Convert valid array to object keyed by index
    const src = Object.fromEntries(
      raw.map((item: unknown, i: number) => [String(i), item])
    )
    return extractEntries(src)
  }

  if (typeof raw === 'object') {
    return extractEntries(raw as Record<string, unknown>)
  }

  return []
})

function extractEntries(source: Record<string, unknown>): BonusStructureEntry[] {
  return Object.entries(source)
    .filter(([key, value]) => {
      if (NON_TIER_KEYS.has(key)) return false
      if (isEmptyObject(value)) return false
      // Also skip if key is purely numeric and value has no amount
      if (/^\d+$/.test(key) && extractAmount(value) <= 0) return false
      return true
    })
    .map(([key, value]) => {
      const amount = extractAmount(value)
      const label = value && typeof value === 'object' && !Array.isArray(value)
        ? ((value as Record<string, unknown>).tierName as string)
          || ((value as Record<string, unknown>).tier as string)
          || ((value as Record<string, unknown>).description as string)
          || resolveBonusLabel(key)
        : resolveBonusLabel(key)
      return { key, label, amount }
    })
    .filter(entry => entry.amount > 0)
}

const bonusTotal = computed(() =>
  bonusStructureEntries.value.reduce((sum, e) => sum + e.amount, 0)
)

// ---- Helpers ----
function formatCurrency(amount: number | null | undefined): string {
  if (amount == null || amount === 0) return '-'
  return `¥${amount.toLocaleString('zh-CN')}`
}

function formatDate(dateStr: string | undefined): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  if (isNaN(d.getTime())) return ''
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function formatDateTime(dateStr: string | undefined): string {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  if (isNaN(d.getTime())) return '-'
  const date = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
  const time = `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}:${String(d.getSeconds()).padStart(2, '0')}`
  return `${date} ${time}`
}

function resetReferForm() {
  referForm.programJobId = undefined
  referForm.candidateId = ''
  referForm.candidateName = ''
  referForm.candidatePhone = ''
  referForm.candidateEmail = ''
  referForm.jobId = undefined
  referForm.referrerName = userStore.userInfo?.realName || userStore.userInfo?.name || ''
  referForm.referrerDept = userStore.userInfo?.departmentName || ''
  referForm.relationship = ''
  referForm.referralNote = ''
  referForm.bonus = program.value?.bonusAmount ?? undefined
}

// ---- API ----
async function fetchDetail() {
  const programId = route.params.id as string
  if (!programId) {
    error.value = '缺少计划ID参数'
    loading.value = false
    return
  }

  loading.value = true
  error.value = null
  program.value = null

  try {
    const res = await getPrograms()
    const list = Array.isArray(res) ? res : []
    const found = list.find((p) => String(p.id) === String(programId))
    program.value = found ?? null
  } catch (err: unknown) {
    error.value = err instanceof Error ? err.message : '加载计划详情失败'
  } finally {
    loading.value = false
  }
}

async function openReferDialog() {
  resetReferForm()
  referDialogVisible.value = true

  // Load program jobs and available jobs for dropdowns
  if (program.value) {
    loadingProgramJobs.value = true
    loadingJobs.value = true
    try {
      const [pjList, jobResult] = await Promise.all([
        getProgramJobs(Number(program.value.id)),
        getJobs({ page: 1, size: 200 }),
      ])
      programJobs.value = Array.isArray(pjList) ? pjList : []
      // getJobs returns PageResult, extract records
      availableJobs.value = jobResult?.records ?? (Array.isArray(jobResult) ? jobResult : [])
      // Only show published jobs
      availableJobs.value = availableJobs.value.filter(j => j.status === 1)
      // Build jobTitle lookup map for program job dropdown labels
      jobTitleMap.value = {}
      for (const job of availableJobs.value) {
        jobTitleMap.value[String(job.id)] = job.title
      }
    } catch {
      // Silently ignore load failures for dropdowns
    } finally {
      loadingProgramJobs.value = false
      loadingJobs.value = false
    }
  }
}

async function handleRefer() {
  if (!formRef.value || !program.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const p = program.value!
      await createReferral({
        programId: Number(p.id),
        programJobId: Number(referForm.programJobId),
        referrerId: Number(userStore.userInfo?.id ?? 0),
        referrerName: referForm.referrerName || undefined,
        referrerDept: referForm.referrerDept || undefined,
        candidateId: Number(referForm.candidateId),
        candidateName: referForm.candidateName || undefined,
        candidatePhone: referForm.candidatePhone || undefined,
        candidateEmail: referForm.candidateEmail || undefined,
        jobId: Number(referForm.jobId),
        relationship: referForm.relationship || undefined,
        referralNote: referForm.referralNote || undefined,
        bonus: referForm.bonus,
      })
      ElMessage.success('推荐成功！')
      referDialogVisible.value = false
    } catch {
      // Error already shown by the request interceptor
    } finally {
      submitting.value = false
    }
  })
}

onMounted(async () => {
  await fetchDetail()
  if (route.query.refer === '1' && program.value) {
    openReferDialog()
  }
})
</script>

<style scoped>
/* ---- Top Bar ---- */
.rd-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.rd-back-btn {
  font-size: 14px;
  color: var(--c-text-secondary);
}

.rd-topbar-actions {
  display: flex;
  gap: 8px;
}

/* ---- Hero Header ---- */
.rd-hero {
  background: var(--c-card);
  border-radius: var(--c-radius-lg);
  box-shadow: var(--c-shadow-sm);
  border: 1px solid var(--c-border);
  padding: 28px 32px;
  margin-bottom: 24px;
  position: relative;
  overflow: hidden;
}

.rd-hero::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, var(--c-primary), var(--c-primary-light));
}

.rd-hero-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 12px;
}

.rd-hero-title-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.rd-hero-title {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: var(--c-text);
  line-height: 1.3;
}

.rd-status-tag {
  flex-shrink: 0;
}

.rd-hero-bonus {
  text-align: right;
  flex-shrink: 0;
}

.rd-hero-bonus-label {
  display: block;
  font-size: 12px;
  color: var(--c-text-muted);
  margin-bottom: 4px;
}

.rd-hero-bonus-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--c-primary);
}

.rd-hero-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--c-text-secondary);
  font-size: 13px;
  margin-bottom: 20px;
}

.rd-hero-meta-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.rd-meta-divider {
  color: var(--c-border);
}

.rd-hero-actions {
  display: flex;
  gap: 10px;
  margin-bottom: 24px;
}

/* ---- Stats Row ---- */
.rd-stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  padding-top: 20px;
  border-top: 1px solid var(--c-border);
}

.rd-stat-card {
  text-align: center;
  padding: 8px;
}

.rd-stat-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--c-radius-md);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 8px;
}

.rd-stat-value {
  font-size: 20px;
  font-weight: 700;
  color: var(--c-text);
  line-height: 1.2;
}

.rd-stat-label {
  font-size: 12px;
  color: var(--c-text-muted);
  margin-top: 2px;
}

/* ---- Body Layout ---- */
.rd-body {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 24px;
  align-items: start;
}

/* ---- Main Content ---- */
.rd-main {
  min-width: 0;
}

.rd-section {
  background: var(--c-card);
  border-radius: var(--c-radius-lg);
  box-shadow: var(--c-shadow-sm);
  border: 1px solid var(--c-border);
  padding: 24px;
  margin-bottom: 20px;
}

.rd-section-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--c-text);
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--c-border);
}

.rd-description {
  font-size: 14px;
  color: var(--c-text-secondary);
  line-height: 1.8;
  white-space: pre-wrap;
}

/* ---- Bonus Grid ---- */
.rd-bonus-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 12px;
}

.rd-bonus-card {
  background: var(--c-primary-bg);
  border: 1px solid var(--c-border);
  border-radius: var(--c-radius-md);
  padding: 16px;
  text-align: center;
  transition: var(--c-transition);
}

.rd-bonus-card:hover {
  border-color: var(--c-primary-light);
}

.rd-bonus-card-step {
  font-size: 11px;
  font-weight: 600;
  color: var(--c-primary);
  background: rgba(79, 70, 229, 0.1);
  display: inline-block;
  padding: 2px 10px;
  border-radius: 999px;
  margin-bottom: 8px;
}

.rd-bonus-card-label {
  font-size: 13px;
  color: var(--c-text-secondary);
  margin-bottom: 6px;
}

.rd-bonus-card-amount {
  font-size: 20px;
  font-weight: 700;
  color: var(--c-primary);
}

.rd-bonus-total {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid var(--c-border);
  text-align: right;
  font-size: 13px;
  color: var(--c-text-secondary);
}

.rd-bonus-total strong {
  font-size: 16px;
  color: var(--c-primary);
}

/* ---- Sidebar ---- */
.rd-sidebar {
  position: sticky;
  top: 84px;
}

.rd-sidebar-card {
  background: var(--c-card);
  border-radius: var(--c-radius-lg);
  box-shadow: var(--c-shadow-sm);
  border: 1px solid var(--c-border);
  padding: 20px;
  margin-bottom: 16px;
}

.rd-sidebar-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--c-text);
  margin-bottom: 16px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--c-border);
}

.rd-sidebar-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
}

.rd-sidebar-item:not(:last-child) {
  border-bottom: 1px solid var(--c-border-light);
}

.rd-sidebar-key {
  font-size: 13px;
  color: var(--c-text-muted);
}

.rd-sidebar-value {
  font-size: 13px;
  color: var(--c-text-secondary);
  text-align: right;
}

.rd-sidebar-value-bold {
  font-weight: 600;
  color: var(--c-primary);
}

.rd-sidebar-links {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.rd-sidebar-link-item {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 8px 12px;
  font-size: 13px;
  color: var(--c-text-secondary);
  background: var(--c-card);
  border: 1px solid var(--c-border);
  border-radius: var(--c-radius-sm);
  cursor: pointer;
  transition: var(--c-transition);
  line-height: 1.4;
}

.rd-sidebar-link-item:hover {
  color: var(--c-primary);
  border-color: var(--c-primary-light);
  background: var(--c-primary-bg);
}

.rd-sidebar-link-item .el-icon {
  flex-shrink: 0;
}

/* ---- Share Dialog ---- */
.rd-share-section {
  margin-bottom: 16px;
}

.rd-share-label {
  font-size: 13px;
  color: var(--c-text-muted);
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.rd-share-value {
  font-size: 15px;
  color: var(--c-text);
  font-weight: 500;
}

.rd-share-url-box {
  background: var(--c-bg);
  border: 1px solid var(--c-border);
  border-radius: var(--c-radius-md);
  padding: 12px 16px;
  margin-top: 6px;
  word-break: break-all;
}

.rd-share-url {
  font-size: 13px;
  color: var(--c-primary);
  font-family: 'SF Mono', 'Menlo', 'Monaco', 'Consolas', monospace;
  line-height: 1.6;
}

.rd-share-code-box {
  text-align: center;
}

.rd-share-code {
  font-size: 24px;
  font-weight: 700;
  letter-spacing: 0.12em;
  color: #1677ff;
}

.rd-share-code-hint {
  font-size: 12px;
  color: var(--c-text-muted);
  margin-top: 6px;
  text-align: center;
}

.rd-share-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
}

.rd-share-generate {
  text-align: center;
  padding: 20px 0;
}

.rd-share-hint {
  font-size: 13px;
  color: var(--c-text-muted);
  margin-bottom: 16px;
}

/* ---- Responsive ---- */
@media (max-width: 1024px) {
  .rd-body {
    grid-template-columns: 1fr;
  }

  .rd-stats-row {
    grid-template-columns: repeat(2, 1fr);
  }

  .rd-hero-top {
    flex-direction: column;
    gap: 12px;
  }

  .rd-hero-bonus {
    text-align: left;
  }
}

@media (max-width: 640px) {
  .rd-hero {
    padding: 20px 16px;
  }

  .rd-hero-title {
    font-size: 20px;
  }

  .rd-hero-bonus-value {
    font-size: 22px;
  }

  .rd-stats-row {
    grid-template-columns: 1fr 1fr;
  }
}
</style>

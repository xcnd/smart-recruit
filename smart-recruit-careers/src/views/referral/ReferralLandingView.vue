<template>
  <div class="rl-page" v-loading="loading">
    <!-- Error State -->
    <div v-if="error" class="rl-state-wrap">
      <div class="rl-state-card">
        <div class="rl-state-icon is-error">
          <svg width="48" height="48" viewBox="0 0 48 48" fill="none"><circle cx="24" cy="24" r="20" stroke="#f56c6c" stroke-width="2" fill="#fef0f0"/><path d="M24 14v14M24 34v0" stroke="#f56c6c" stroke-width="2.5" stroke-linecap="round"/></svg>
        </div>
        <p class="rl-state-title">加载失败</p>
        <p class="rl-state-desc">{{ error }}</p>
        <el-button type="primary" @click="fetchData">重新加载</el-button>
      </div>
    </div>

    <!-- Not Found -->
    <div v-else-if="!loading && (!landingData || !landingData.program)" class="rl-state-wrap">
      <div class="rl-state-card">
        <el-empty description="未找到该内推计划" :image-size="100" />
      </div>
    </div>

    <!-- Program Disabled -->
    <div v-else-if="landingData?.program?.status === 0" class="rl-state-wrap">
      <div class="rl-state-card">
        <div class="rl-state-icon is-warn">
          <svg width="48" height="48" viewBox="0 0 48 48" fill="none"><circle cx="24" cy="24" r="20" stroke="#e6a23c" stroke-width="2" fill="#fdf6ec"/><path d="M24 14v14M24 34v0" stroke="#e6a23c" stroke-width="2.5" stroke-linecap="round"/></svg>
        </div>
        <p class="rl-state-title">该内推计划已停用</p>
        <p class="rl-state-desc">请联系管理员了解更多信息</p>
      </div>
    </div>

    <!-- Main Content -->
    <template v-else-if="landingData?.program">
      <!-- TOP NAV -->
      <header class="rl-header">
        <div class="rl-header-inner">
          <div class="rl-header-brand">
            <svg class="rl-logo" width="28" height="28" viewBox="0 0 28 28" fill="none">
              <rect width="28" height="28" rx="6" fill="#1677ff"/>
              <path d="M8 20V8l6 8.5L20 8v12" stroke="#fff" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" fill="none"/>
            </svg>
            <span class="rl-brand-name">SmartRecruit</span>
          </div>
          <div class="rl-header-right">
            <template v-if="userStore.isLoggedIn">
              <span v-if="maskedPhone" class="rl-user-phone">
                <svg width="16" height="16" viewBox="0 0 16 16" fill="none" class="rl-user-icon">
                  <rect x="2" y="1" width="12" height="14" rx="2" stroke="currentColor" stroke-width="1.3"/>
                  <circle cx="8" cy="10.5" r="1.5" fill="currentColor"/>
                </svg>
                {{ maskedPhone }}
              </span>
              <a :href="`/my-applications?ref=/p/${route.params.token}`" class="rl-header-btn">我的投递</a>
              <button class="rl-logout-btn" @click="handleLogout">退出</button>
            </template>
          </div>
        </div>
      </header>

      <!-- HERO -->
      <section class="rl-hero">
        <div class="rl-hero-body">
          <div class="rl-hero-text">
            <div class="rl-hero-badge">
              <span class="rl-badge-dot" :class="landingData.program.status === 1 ? 'is-active' : 'is-inactive'"></span>
              {{ landingData.program.status === 1 ? '招聘中' : '已停用' }}
            </div>
            <h1 class="rl-hero-title">{{ landingData.program.title }}</h1>
            <p class="rl-hero-desc" v-if="landingData.program.description">{{ landingData.program.description }}</p>
            <div class="rl-hero-meta">
              <span v-if="landingData.program.startDate" class="rl-meta-item">
                <svg width="14" height="14" viewBox="0 0 14 14" fill="none"><rect x="1" y="2" width="12" height="11" rx="1.5" stroke="currentColor" stroke-width="1.2"/><path d="M4 1v3M10 1v3M1 6h12" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/></svg>
                {{ formatDate(landingData.program.startDate) }} 起
              </span>
              <span v-if="!landingData.program.endDate" class="rl-meta-item">
                <svg width="14" height="14" viewBox="0 0 14 14" fill="none"><circle cx="7" cy="7" r="5.5" stroke="currentColor" stroke-width="1.2"/><path d="M7 4v3.5L9 9" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/></svg>
                长期有效
              </span>
              <span v-else-if="landingData.program.endDate" class="rl-meta-item">至 {{ formatDate(landingData.program.endDate) }}</span>
              <span v-if="landingData.program.createBy" class="rl-meta-item rl-meta-creator">{{ landingData.program.createBy }}</span>
            </div>
          </div>
          <div class="rl-hero-bonus-box">
            <div class="rl-hero-bonus-bg"></div>
            <div class="rl-hero-bonus-inner">
              <span class="rl-hero-bonus-label">推荐奖金</span>
              <span class="rl-hero-bonus-value">¥{{ (landingData.program.bonusAmount || 0).toLocaleString('zh-CN') }}</span>
            </div>
          </div>
        </div>

        <!-- Apply Form Removed → Self-apply is done on the job detail page -->

        <!-- Job List -->
        <div class="rl-kpi-strip">
          <div class="rl-kpi-item">
            <span class="rl-kpi-value">¥{{ (landingData.program.bonusAmount || 0).toLocaleString('zh-CN') }}</span>
            <span class="rl-kpi-label">推荐奖金</span>
          </div>
          <div class="rl-kpi-divider"></div>
          <div class="rl-kpi-item">
            <span class="rl-kpi-value">{{ (landingData.jobs || []).length }}</span>
            <span class="rl-kpi-label">在招职位</span>
          </div>
          <div class="rl-kpi-divider"></div>
          <div class="rl-kpi-item">
            <span class="rl-kpi-value">{{ bonusStructureEntries.length || '-' }}</span>
            <span class="rl-kpi-label">发放阶段</span>
          </div>
          <div class="rl-kpi-divider"></div>
          <div class="rl-kpi-item">
            <span class="rl-kpi-value">{{ formatDate(landingData.program.startDate) || '-' }}</span>
            <span class="rl-kpi-label">开始日期</span>
          </div>
          <template v-if="landingData.referralCode">
            <div class="rl-kpi-divider"></div>
            <div class="rl-kpi-item rl-kpi-code">
              <span class="rl-kpi-value rl-kpi-code-val">{{ landingData.referralCode }}</span>
              <span class="rl-kpi-label">内推码</span>
            </div>
          </template>
        </div>
      </section>

      <!-- BONUS STAGES (Timeline) -->
      <section v-if="bonusStructureEntries.length" class="rl-section">
        <div class="rl-section-hd">
          <h2 class="rl-section-title">奖金发放阶段</h2>
          <span class="rl-section-sub">合计 <strong>¥{{ bonusTotal.toLocaleString('zh-CN') }}</strong></span>
        </div>
        <div class="rl-bonus-timeline">
          <div class="rl-bonus-line"></div>
          <div
            v-for="(item, index) in bonusStructureEntries"
            :key="item.key"
            class="rl-bonus-node"
          >
            <div class="rl-bn-circle">
              <span class="rl-bn-num">{{ index + 1 }}</span>
              <svg class="rl-bn-ring" width="44" height="44" viewBox="0 0 44 44"><circle cx="22" cy="22" r="20" stroke="#1677ff" stroke-width="2" fill="none" stroke-dasharray="126" stroke-dashoffset="0"/></svg>
            </div>
            <div class="rl-bn-label">{{ item.label }}</div>
            <div class="rl-bn-amount">¥{{ item.amount.toLocaleString('zh-CN') }}</div>
          </div>
        </div>
      </section>

      <!-- JOB POSITIONS -->
      <section v-if="(landingData.jobs || []).length" class="rl-section">
        <div class="rl-section-hd">
          <h2 class="rl-section-title">招聘职位</h2>
          <span class="rl-section-sub">共 {{ totalFilteredCount }} 个职位</span>
          <div class="rl-jobs-search">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索职位名称"
              clearable
              :prefix-icon="Search"
              size="default"
              @input="onSearchChange"
              class="rl-search-input"
            />
          </div>
        </div>

        <!-- Empty search result -->
        <div v-if="!paginatedJobs.length" class="rl-state-wrap">
          <el-empty description="未找到匹配的职位" :image-size="80" />
        </div>

        <div v-else class="rl-jobs-grid">
          <div v-for="job in paginatedJobs" :key="job.id" class="rl-job-card">
            <div class="rl-job-hd">
              <h3 class="rl-job-title">{{ job.jobTitle || '职位 #' + job.jobPositionId }}</h3>
              <div class="rl-job-tags">
                <span v-if="job.headCount != null" class="rl-job-tag rl-tag--hc">招{{ job.headCount }}人</span>
                <span v-if="getTagLabel(job.tag)" class="rl-job-tag" :class="'rl-tag--' + getTagKey(job.tag)">{{ getTagLabel(job.tag) }}</span>
              </div>
            </div>
            <div class="rl-job-salary" v-if="job.minSalary || job.maxSalary">
              <svg width="14" height="14" viewBox="0 0 14 14" fill="none"><rect x="1.5" y="3" width="11" height="8" rx="1.5" stroke="#1677ff" stroke-width="1.2"/><circle cx="7" cy="7" r="2" stroke="#1677ff" stroke-width="1.2"/><path d="M1 5h12" stroke="#1677ff" stroke-width="1.2"/></svg>
              {{ formatSalary(job.minSalary, job.maxSalary) }}
            </div>
            <div class="rl-job-bonus-line">
              <svg width="14" height="14" viewBox="0 0 14 14" fill="none"><circle cx="7" cy="7" r="5.5" stroke="#999" stroke-width="1.2"/><path d="M7 4v3.5l2.5 2" stroke="#999" stroke-width="1.2" stroke-linecap="round"/></svg>
              <span>推荐奖金</span>
              <strong>¥{{ (job.bonusAmount || landingData.program.bonusAmount || 0).toLocaleString('zh-CN') }}</strong>
            </div>
            <a :href="`/referral/position/${job.id}`" class="rl-job-apply" @click.prevent="goToPositionDetail(Number(job.id))">投递此职位 →</a>
          </div>
        </div>

        <!-- Pagination -->
        <div v-if="totalFilteredCount > pageSize" class="rl-pagination">
          <el-pagination
            v-model:current-page="currentPage"
            :page-size="pageSize"
            :total="totalFilteredCount"
            layout="prev, pager, next"
            background
            size="small"
          />
        </div>
      </section>

      <!-- FOOTER -->
      <footer class="rl-footer">
        <div class="rl-footer-inner">
          <span class="rl-footer-brand">SmartRecruit</span>
          <span class="rl-footer-divider">|</span>
          <span>智能招聘管理系统</span>
        </div>
      </footer>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

function goToPositionDetail(programJobId: number) {
  const token = route.params.token as string
  if (token) {
    router.push(`/p/${token}/job/${programJobId}`)
  } else {
    router.push(`/p/job/${programJobId}`)
  }
}

// ---- Types ----
interface LandingJob {
  id: string
  jobPositionId: string
  isEnabled: number
  bonusAmount: number | null
  tag: number | null
  jobTitle: string | null
  minSalary: number | null
  maxSalary: number | null
  headCount: number | null
}

interface LandingProgram {
  id: string
  title: string
  description: string
  bonusAmount: number
  bonusStructure?: unknown
  startDate?: string
  endDate?: string
  status: number
  createBy?: string
}

interface LandingData {
  program: LandingProgram
  jobs: LandingJob[]
  referralCode?: string
  referrerName?: string
  source?: string
}

// ---- State ----
const loading = ref(true)
const maskedPhone = computed(() => {
  const mobile = userStore.userInfo?.mobile
  if (!mobile || mobile.length !== 11) return null
  return mobile.slice(0, 3) + '****' + mobile.slice(7)
})
const error = ref<string | null>(null)
const landingData = ref<LandingData | null>(null)

// ---- Search & Pagination ----
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = 9

const filteredJobs = computed(() => {
  const jobs = landingData.value?.jobs || []
  if (!searchKeyword.value.trim()) return jobs
  const kw = searchKeyword.value.trim().toLowerCase()
  return jobs.filter(j => (j.jobTitle || '').toLowerCase().includes(kw))
})

const totalFilteredCount = computed(() => filteredJobs.value.length)

const paginatedJobs = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredJobs.value.slice(start, start + pageSize)
})

function onSearchChange() {
  currentPage.value = 1
}

// ---- Bonus Structure ----
interface BonusStructureEntry {
  key: string
  label: string
  amount: number
}

const NON_TIER_KEYS = new Set(['currency', 'note', 'remark', 'version'])

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
    return 0
  }
  return 0
}

function extractEntries(source: Record<string, unknown>): BonusStructureEntry[] {
  return Object.entries(source)
    .filter(([key, value]) => {
      if (NON_TIER_KEYS.has(key)) return false
      if (isEmptyObject(value)) return false
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

const bonusStructureEntries = computed<BonusStructureEntry[]>(() => {
  let raw: unknown = landingData.value?.program?.bonusStructure
  if (!raw) return []

  if (typeof raw === 'string') {
    try { raw = JSON.parse(raw) } catch { return [] }
  }

  if (Array.isArray(raw)) {
    if (raw.length === 0) return []
    if (raw.every(isEmptyObject)) return []
    const src = Object.fromEntries(raw.map((item: unknown, i: number) => [String(i), item]))
    return extractEntries(src)
  }

  if (typeof raw === 'object') {
    return extractEntries(raw as Record<string, unknown>)
  }

  return []
})

const bonusTotal = computed(() =>
  bonusStructureEntries.value.reduce((sum, e) => sum + e.amount, 0)
)

// ---- Tag helpers ----
function getTagLabel(tag: number | null): string {
  const map: Record<number, string> = { 0: '急聘', 1: '高额奖金', 2: '技术岗', 3: '实习' }
  return tag != null ? (map[tag] || '') : ''
}

function getTagKey(tag: number | null): string {
  const map: Record<number, string> = { 0: 'urgent', 1: 'bonus', 2: 'tech', 3: 'intern' }
  return tag != null ? (map[tag] || 'default') : 'default'
}

// ---- Helpers ----
function formatSalary(min: number | null, max: number | null): string {
  if (!min && !max) return ''
  const minStr = min ? `¥${(min / 1000).toFixed(0)}K` : ''
  const maxStr = max ? `¥${(max / 1000).toFixed(0)}K` : ''
  return min && max ? `${minStr} - ${maxStr}` : (minStr || maxStr)
}

function formatDate(dateStr: string | undefined): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  if (isNaN(d.getTime())) return ''
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function handleLogout() {
  userStore.logout()
  ElMessage.success('已退出登录')
  router.push({ name: 'PhoneLogin' })
}

// ---- API ----
async function fetchData() {
  const token = route.params.token as string
  if (!token) {
    error.value = '缺少访问令牌'
    loading.value = false
    return
  }

  loading.value = true
  error.value = null
  try {
    const res = await axios.get(`/api/v1/referrals/public/programs/${token}`)
    if (res.data && res.data.code === 0) {
      landingData.value = res.data.data
    } else {
      error.value = res.data?.message || '加载失败'
    }
  } catch (err: unknown) {
    error.value = err instanceof Error ? err.message : '网络请求失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
/* ============================================================
   VARIABLES
   ============================================================ */
.rl-page {
  --rl-primary: #1677ff;
  --rl-primary-dark: #0958d9;
  --rl-primary-light: #e6f4ff;
  --rl-bg: #f5f7fa;
  --rl-card-bg: #ffffff;
  --rl-text: #1a1a2e;
  --rl-text-secondary: #5a5d72;
  --rl-text-muted: #979bb0;
  --rl-border: #eaecf2;
  --rl-border-light: #f0f1f5;
  --rl-success: #52c41a;
  --rl-warning: #fa8c16;
  --rl-danger: #f5222d;
  --rl-radius-sm: 6px;
  --rl-radius: 10px;
  --rl-radius-lg: 16px;
  --rl-shadow-sm: 0 1px 2px rgba(0,0,0,.04);
  --rl-shadow: 0 1px 3px rgba(0,0,0,.06), 0 1px 2px rgba(0,0,0,.04);
  --rl-shadow-lg: 0 4px 12px rgba(0,0,0,.06), 0 2px 4px rgba(0,0,0,.04);
  --rl-font: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;

  min-height: 100vh;
  background: var(--rl-bg);
  font-family: var(--rl-font);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

/* State */
.rl-state-wrap {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  padding: 40px 20px;
  background: var(--rl-bg);
}

.rl-state-card {
  background: var(--rl-card-bg);
  border-radius: var(--rl-radius-lg);
  padding: 64px 48px;
  text-align: center;
  max-width: 420px;
  width: 100%;
  box-shadow: var(--rl-shadow-lg);
  border: 1px solid var(--rl-border);
}

.rl-state-icon {
  margin-bottom: 16px;
}

.rl-state-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--rl-text);
  margin: 0 0 8px;
}

.rl-state-desc {
  font-size: 14px;
  color: var(--rl-text-muted);
  margin: 0 0 24px;
  line-height: 1.6;
}

/* ============================================================
   HEADER
   ============================================================ */
.rl-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(255,255,255,.88);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--rl-border);
}

.rl-header-inner {
  max-width: 960px;
  margin: 0 auto;
  padding: 0 24px;
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.rl-header-brand {
  display: flex;
  align-items: center;
  gap: 8px;
}

.rl-logo {
  flex-shrink: 0;
}

.rl-brand-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--rl-text);
  letter-spacing: -0.2px;
}

.rl-header-cta {
  font-size: 13px;
  font-weight: 500;
  color: var(--rl-primary);
  text-decoration: none;
  padding: 5px 14px;
  border-radius: var(--rl-radius-sm);
  border: 1px solid var(--rl-primary);
  transition: all .15s;
}

.rl-header-cta:hover {
  background: var(--rl-primary);
  color: #fff;
}

.rl-header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.rl-user-phone {
  font-size: 13px;
  font-weight: 500;
  color: var(--rl-text-secondary);
  display: flex;
  align-items: center;
  gap: 6px;
}

.rl-user-icon {
  color: var(--rl-primary);
  flex-shrink: 0;
}

.rl-header-btn {
  font-size: 13px;
  font-weight: 500;
  color: var(--rl-primary);
  text-decoration: none;
  padding: 5px 14px;
  border-radius: var(--rl-radius-sm);
  border: 1px solid var(--rl-primary);
  transition: all .15s;
}

.rl-header-btn:hover {
  background: var(--rl-primary);
  color: #fff;
}

.rl-logout-btn {
  font-size: 13px;
  font-weight: 500;
  color: var(--rl-text-secondary);
  background: none;
  border: 1px solid var(--rl-border);
  border-radius: var(--rl-radius-sm);
  padding: 5px 14px;
  cursor: pointer;
  transition: all .15s;
}

.rl-logout-btn:hover {
  color: #f56c6c;
  border-color: #f56c6c;
}

/* ============================================================
   HERO
   ============================================================ */
.rl-hero {
  max-width: 960px;
  margin: 0 auto;
  padding: 40px 24px 0;
}

.rl-hero-body {
  display: grid;
  grid-template-columns: 1fr 280px;
  gap: 40px;
  align-items: center;
  background: var(--rl-card-bg);
  border-radius: var(--rl-radius-lg);
  padding: 40px 44px;
  box-shadow: var(--rl-shadow);
  border: 1px solid var(--rl-border);
  position: relative;
  overflow: hidden;
}

.rl-hero-body::after {
  content: '';
  position: absolute;
  top: -120px;
  right: -80px;
  width: 320px;
  height: 320px;
  background: radial-gradient(circle, rgba(22,119,255,.04) 0%, transparent 70%);
  border-radius: 50%;
  pointer-events: none;
}

.rl-hero-text {
  position: relative;
  z-index: 1;
}

.rl-hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 500;
  color: var(--rl-text-secondary);
  background: var(--rl-bg);
  padding: 3px 10px;
  border-radius: 100px;
  margin-bottom: 14px;
}

.rl-badge-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.rl-badge-dot.is-active {
  background: var(--rl-success);
  box-shadow: 0 0 0 3px rgba(82,196,26,.2);
}

.rl-badge-dot.is-inactive {
  background: var(--rl-danger);
}

.rl-hero-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--rl-text);
  margin: 0 0 12px;
  letter-spacing: -0.5px;
  line-height: 1.3;
}

.rl-hero-desc {
  font-size: 14px;
  color: var(--rl-text-secondary);
  line-height: 1.7;
  margin: 0 0 16px;
  max-width: 480px;
}

.rl-hero-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  font-size: 13px;
  color: var(--rl-text-muted);
}

.rl-meta-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.rl-meta-creator {
  margin-left: 4px;
  padding-left: 10px;
  border-left: 1px solid var(--rl-border);
}

/* Hero Bonus Box */
.rl-hero-bonus-box {
  position: relative;
  z-index: 1;
  text-align: center;
}

.rl-hero-bonus-bg {
  position: absolute;
  inset: -16px -8px;
  background: linear-gradient(135deg, #f0f5ff 0%, #e6f4ff 100%);
  border-radius: var(--rl-radius);
  border: 1px solid #d6e4ff;
}

.rl-hero-bonus-inner {
  position: relative;
  z-index: 1;
  padding: 24px 20px 20px;
}

.rl-hero-bonus-label {
  display: block;
  font-size: 12px;
  font-weight: 500;
  color: var(--rl-text-muted);
  text-transform: uppercase;
  letter-spacing: 1px;
  margin-bottom: 6px;
}

.rl-hero-bonus-value {
  font-size: 36px;
  font-weight: 800;
  color: var(--rl-primary);
  letter-spacing: -1px;
  line-height: 1;
}

.rl-hero-btn {
  position: relative;
  z-index: 1;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: 16px;
  background: var(--rl-primary);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  padding: 10px 24px;
  border-radius: var(--rl-radius);
  text-decoration: none;
  transition: background .15s;
}

.rl-hero-btn:hover {
  background: var(--rl-primary-dark);
}

/* KPI Strip */
.rl-kpi-strip {
  display: flex;
  align-items: center;
  background: var(--rl-card-bg);
  border-radius: var(--rl-radius);
  margin-top: 16px;
  padding: 18px 0;
  box-shadow: var(--rl-shadow-sm);
  border: 1px solid var(--rl-border);
}

.rl-kpi-item {
  flex: 1;
  text-align: center;
}

.rl-kpi-value {
  display: block;
  font-size: 18px;
  font-weight: 700;
  color: var(--rl-text);
  line-height: 1.2;
}

.rl-kpi-label {
  font-size: 12px;
  color: var(--rl-text-muted);
  margin-top: 2px;
}

.rl-kpi-code-val {
  font-family: 'SF Mono', 'JetBrains Mono', 'Consolas', monospace;
  letter-spacing: 0.12em;
  color: #1677ff;
}

.rl-kpi-divider {
  width: 1px;
  height: 32px;
  background: var(--rl-border-light);
  flex-shrink: 0;
}

/* ============================================================
   SECTIONS
   ============================================================ */
.rl-section {
  max-width: 960px;
  margin: 0 auto;
  padding: 32px 24px;
}

.rl-section-hd {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 24px;
}

.rl-section-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--rl-text);
  margin: 0;
  letter-spacing: -0.2px;
}

.rl-section-sub {
  font-size: 13px;
  color: var(--rl-text-muted);
  margin-left: 12px;
}

.rl-section-sub strong {
  color: var(--rl-primary);
  font-weight: 600;
}

/* ============================================================
   BONUS TIMELINE
   ============================================================ */
.rl-bonus-timeline {
  display: flex;
  justify-content: center;
  gap: 0;
  position: relative;
  padding: 20px 0;
  background: var(--rl-card-bg);
  border-radius: var(--rl-radius);
  border: 1px solid var(--rl-border);
  box-shadow: var(--rl-shadow-sm);
  overflow-x: auto;
}

.rl-bonus-line {
  position: absolute;
  top: 42px;
  left: 12%;
  right: 12%;
  height: 2px;
  background: var(--rl-border);
}

.rl-bonus-node {
  flex: 0 0 auto;
  width: 160px;
  text-align: center;
  position: relative;
  z-index: 1;
  padding: 0 8px;
}

.rl-bn-circle {
  position: relative;
  width: 44px;
  height: 44px;
  margin: 0 auto 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.rl-bn-ring {
  position: absolute;
}

.rl-bn-num {
  font-size: 15px;
  font-weight: 700;
  color: var(--rl-primary);
  position: relative;
  z-index: 1;
}

.rl-bn-label {
  font-size: 13px;
  color: var(--rl-text-secondary);
  margin-bottom: 6px;
  font-weight: 500;
}

.rl-bn-amount {
  font-size: 18px;
  font-weight: 700;
  color: var(--rl-text);
}

/* ============================================================
   JOBS GRID
   ============================================================ */
.rl-jobs-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.rl-job-card {
  background: var(--rl-card-bg);
  border: 1px solid var(--rl-border);
  border-radius: var(--rl-radius);
  padding: 20px 22px;
  transition: border-color .2s, box-shadow .2s;
  display: flex;
  flex-direction: column;
}

.rl-job-card:hover {
  border-color: #bdd3f5;
  box-shadow: 0 2px 8px rgba(22,119,255,.06);
}

.rl-job-hd {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 12px;
  min-height: 0;
}

.rl-job-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--rl-text);
  margin: 0;
  line-height: 1.4;
  flex: 1;
  min-width: 0;
}

.rl-job-tags {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}

.rl-job-tag {
  font-size: 11px;
  font-weight: 500;
  padding: 2px 8px;
  border-radius: 100px;
  white-space: nowrap;
}

.rl-tag--urgent { background: #fff1f0; color: #cf1322; border: 1px solid #ffa39e; }
.rl-tag--bonus { background: #fff7e6; color: #d46b08; border: 1px solid #ffd591; }
.rl-tag--tech { background: #e6f4ff; color: #0958d9; border: 1px solid #91caff; }
.rl-tag--intern { background: #f6ffed; color: #389e0d; border: 1px solid #b7eb8f; }
.rl-tag--default { background: #f5f5f5; color: #8c8c8c; border: 1px solid #d9d9d9; }

.rl-job-salary {
  font-size: 15px;
  font-weight: 600;
  color: #fa8c16;
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 10px;
}

.rl-job-bonus-line {
  font-size: 12px;
  color: var(--rl-text-muted);
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 14px;
}

.rl-job-bonus-line strong {
  font-weight: 600;
  color: var(--rl-text-secondary);
}

.rl-job-apply {
  margin-top: auto;
  font-size: 12px;
  font-weight: 500;
  color: var(--rl-primary);
  text-decoration: none;
  padding: 6px 0;
  transition: opacity .15s;
}

.rl-job-apply:hover {
  opacity: .75;
}

/* Headcount tag */
.rl-tag--hc {
  background: #e6f4ff;
  color: #1677ff;
  border: 1px solid #91caff;
}

/* Jobs Search */
.rl-jobs-search {
  margin-left: auto;
}

.rl-search-input {
  width: 220px;
}

/* Pagination */
.rl-pagination {
  display: flex;
  justify-content: center;
  margin-top: 28px;
  padding-top: 20px;
}

/* ============================================================
   FORM (removed — self-apply is done on the job detail page)
   ============================================================ */

/* ============================================================
   FOOTER
   ============================================================ */
.rl-footer {
  max-width: 960px;
  margin: 0 auto;
  padding: 24px;
  text-align: center;
}

.rl-footer-inner {
  font-size: 12px;
  color: var(--rl-text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.rl-footer-brand {
  font-weight: 600;
  color: var(--rl-text-secondary);
}

.rl-footer-divider {
  color: var(--rl-border);
}

/* ============================================================
   RESPONSIVE
   ============================================================ */
@media (max-width: 768px) {
  .rl-hero-body {
    grid-template-columns: 1fr;
    gap: 24px;
    padding: 28px 24px;
  }

  .rl-hero-title {
    font-size: 23px;
  }

  .rl-hero-bonus-value {
    font-size: 30px;
  }

  .rl-jobs-grid {
    grid-template-columns: 1fr 1fr;
  }

  .rl-kpi-strip {
    flex-wrap: wrap;
    gap: 12px 0;
  }

  .rl-kpi-item {
    flex: 1 1 50%;
  }

  .rl-kpi-divider:nth-child(2),
  .rl-kpi-divider:nth-child(6) {
    display: none;
  }

  .rl-bonus-timeline {
    justify-content: flex-start;
    padding: 20px 16px;
  }

  .rl-bonus-node {
    width: 140px;
  }

  .rl-section {
    padding: 24px 16px;
  }

}

@media (max-width: 480px) {
  .rl-jobs-grid {
    grid-template-columns: 1fr;
  }

  .rl-hero-body {
    padding: 24px 18px;
  }

  .rl-hero-title {
    font-size: 20px;
  }

  .rl-kpi-item {
    flex: 1 1 50%;
  }
}
</style>

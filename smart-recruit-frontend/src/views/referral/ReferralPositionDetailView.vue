<template>
  <div class="rpd-page" v-loading="loading">
    <!-- Top Bar -->
    <div class="rpd-topbar">
      <el-button text :icon="ArrowLeft" @click="$router.back()" class="rpd-back-btn">
        返回
      </el-button>
      <el-tag v-if="program" :type="program.status === 1 ? 'success' : 'danger'" size="small">
        {{ program.status === 1 ? '计划启用中' : '计划已停用' }}
      </el-tag>
    </div>

    <!-- Error State -->
    <div v-if="error" class="rpd-center-box">
      <el-icon :size="48" color="var(--c-danger)"><WarningFilled /></el-icon>
      <p class="rpd-center-title">加载失败</p>
      <p class="rpd-center-desc">{{ error }}</p>
      <el-button type="primary" @click="fetchDetail">重新加载</el-button>
    </div>

    <!-- Not Found -->
    <div v-else-if="!loading && !detail" class="rpd-center-box">
      <el-empty description="未找到该职位信息" :image-size="100" />
      <el-button type="primary" @click="$router.replace('/referral')">返回内推管理</el-button>
    </div>

    <!-- Program Disabled Warning -->
    <div v-else-if="program && program.status === 0" class="rpd-center-box">
      <el-icon :size="48" color="var(--c-warning)"><WarningFilled /></el-icon>
      <p class="rpd-center-title">该职位所属的内推计划已停用</p>
      <p class="rpd-center-desc">此职位暂时无法接受内推推荐</p>
      <el-button @click="$router.back()">返回上一页</el-button>
    </div>

    <!-- Content -->
    <template v-else-if="detail">
      <!-- Hero Header -->
      <div class="rpd-hero">
        <div class="rpd-hero-top">
          <div class="rpd-hero-main">
            <div class="rpd-hero-program">{{ program?.title || '内推计划' }}</div>
            <h1 class="rpd-hero-title">{{ detail.programJob?.jobTitle || jobPosition?.title || '职位详情' }}</h1>
            <div class="rpd-hero-tags">
              <el-tag v-if="jobPosition?.departmentName" size="small" effect="plain" type="info">{{ jobPosition.departmentName }}</el-tag>
              <el-tag v-if="detail.programJob?.headCount != null" size="small" effect="plain" type="primary">招{{ detail.programJob.headCount }}人</el-tag>
              <el-tag v-if="detail.programJob?.tag === 1" type="danger" size="small" effect="dark">急聘</el-tag>
              <el-tag v-else-if="detail.programJob?.tag === 2" type="primary" size="small" effect="dark">高额奖金</el-tag>
              <el-tag v-if="positionTypeLabel" size="small" effect="plain">{{ positionTypeLabel }}</el-tag>
            </div>
          </div>
          <div class="rpd-hero-bonus">
            <div class="rpd-hero-bonus-val">
              <span class="rpd-currency">¥</span>
              {{ formatBonus(programJobBonus) }}
            </div>
            <div class="rpd-hero-bonus-label">推荐奖金</div>
          </div>
        </div>
        <div class="rpd-hero-meta">
          <span class="rpd-meta-item" v-if="salaryText">{{ salaryText }}</span>
          <span class="rpd-meta-divider" v-if="salaryText && jobPosition?.location">·</span>
          <span class="rpd-meta-item" v-if="jobPosition?.location">{{ jobPosition.location }}</span>
          <span class="rpd-meta-divider" v-if="jobPosition?.location || salaryText">·</span>
          <span class="rpd-meta-item" v-if="experienceLabel">{{ experienceLabel }}</span>
          <span class="rpd-meta-divider">·</span>
          <span class="rpd-meta-item" v-if="educationLabel">{{ educationLabel }}</span>
        </div>
        <div class="rpd-hero-actions">
          <el-button
            type="primary"
            size="large"
            :disabled="program?.status === 0"
            @click="openReferDialog"
          >
            立即推荐
          </el-button>
        </div>
      </div>

      <!-- Body: Two-column Layout -->
      <div class="rpd-body">
        <!-- Left Column -->
        <div class="rpd-main">
          <!-- Responsibilities -->
          <div class="rpd-section" v-if="responsibilitiesList.length">
            <h2 class="rpd-section-title">岗位职责</h2>
            <ul class="rpd-list">
              <li v-for="(item, idx) in responsibilitiesList" :key="idx">{{ item }}</li>
            </ul>
          </div>

          <!-- Requirements -->
          <div class="rpd-section" v-if="requirementsList.length">
            <h2 class="rpd-section-title">任职要求</h2>
            <ul class="rpd-list">
              <li v-for="(item, idx) in requirementsList" :key="idx">{{ item }}</li>
            </ul>
          </div>

          <!-- What We Offer -->
          <div class="rpd-section" v-if="offerItems.length">
            <h2 class="rpd-section-title">我们提供</h2>
            <ul class="rpd-list">
              <li v-for="(item, idx) in offerItems" :key="idx">{{ item }}</li>
            </ul>
          </div>

          <!-- Skills -->
          <div class="rpd-section" v-if="skillsList.length">
            <h2 class="rpd-section-title">技能要求</h2>
            <div class="rpd-skills">
              <el-tag
                v-for="(skill, idx) in skillsList"
                :key="idx"
                size="default"
                class="rpd-skill-tag"
              >
                {{ skill }}
              </el-tag>
            </div>
          </div>
        </div>

        <!-- Right Sidebar -->
        <div class="rpd-sidebar">
          <div class="rpd-sidebar-card">
            <h3 class="rpd-sidebar-title">报酬信息</h3>
            <div class="rpd-sidebar-item">
              <span class="rpd-sidebar-key">推荐奖金</span>
              <span class="rpd-sidebar-value rpd-sidebar-value-bold">
                ¥{{ formatBonus(programJobBonus) }}
              </span>
            </div>
            <div class="rpd-sidebar-item" v-for="(item, idx) in bonusStructureItems" :key="idx">
              <span class="rpd-sidebar-key">{{ item.label }}</span>
              <span class="rpd-sidebar-value rpd-sidebar-value-bold">¥{{ item.amount.toLocaleString('zh-CN') }}</span>
            </div>
            <div class="rpd-sidebar-item" v-if="salaryText">
              <span class="rpd-sidebar-key">薪资范围</span>
              <span class="rpd-sidebar-value rpd-sidebar-value-bold">{{ salaryText }}</span>
            </div>
          </div>

          <div class="rpd-sidebar-card">
            <h3 class="rpd-sidebar-title">职位信息</h3>
            <div class="rpd-sidebar-item" v-if="jobPosition?.location">
              <span class="rpd-sidebar-key">工作地点</span>
              <span class="rpd-sidebar-value">{{ jobPosition.location }}</span>
            </div>
            <div class="rpd-sidebar-item" v-if="experienceLabel">
              <span class="rpd-sidebar-key">经验要求</span>
              <span class="rpd-sidebar-value">{{ experienceLabel }}</span>
            </div>
            <div class="rpd-sidebar-item" v-if="educationLabel">
              <span class="rpd-sidebar-key">学历要求</span>
              <span class="rpd-sidebar-value">{{ educationLabel }}</span>
            </div>
            <div class="rpd-sidebar-item" v-if="positionTypeLabel">
              <span class="rpd-sidebar-key">职位类型</span>
              <span class="rpd-sidebar-value">{{ positionTypeLabel }}</span>
            </div>
          </div>

          <div class="rpd-sidebar-card">
            <h3 class="rpd-sidebar-title">所属计划</h3>
            <div class="rpd-sidebar-item">
              <span class="rpd-sidebar-key">计划名称</span>
              <span class="rpd-sidebar-value">{{ program?.title || '-' }}</span>
            </div>
            <div class="rpd-sidebar-item" v-if="program?.startDate">
              <span class="rpd-sidebar-key">计划周期</span>
              <span class="rpd-sidebar-value">{{ program.startDate }}{{ program.endDate ? ' 至 ' + program.endDate : ' 起长期有效' }}</span>
            </div>
            <div class="rpd-sidebar-item" v-if="program?.createBy">
              <span class="rpd-sidebar-key">创建人</span>
              <span class="rpd-sidebar-value">{{ program.createBy }}</span>
            </div>
          </div>

          <el-button
            type="primary"
            size="large"
            :disabled="program?.status === 0"
            @click="openReferDialog"
            class="rpd-sidebar-cta"
          >
            立即推荐此职位
          </el-button>
        </div>
      </div>
    </template>

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
        label-width="130px"
        label-position="right"
        :colon="false"
        @submit.prevent
      >
        <el-divider content-position="left">职位信息</el-divider>
        <el-form-item label="内推计划：">{{ program?.title || '-' }}</el-form-item>
        <el-form-item label="目标职位：">{{ detail?.programJob?.jobTitle || jobPosition?.title || '-' }}</el-form-item>
        <el-form-item label="所在部门：">{{ jobPosition?.departmentName || '-' }}</el-form-item>
        <el-form-item label="推荐奖金：">
          <span class="rpd-form-bonus">¥{{ formatBonus(programJobBonus) }}</span>
        </el-form-item>

        <el-divider content-position="left">候选人信息</el-divider>
        <el-form-item label="候选人ID：" prop="candidateId">
          <el-input
            v-model="referForm.candidateId"
            placeholder="请输入候选人ID"
            maxlength="20"
          />
        </el-form-item>
        <el-form-item label="候选人姓名：" prop="candidateName">
          <el-input
            v-model="referForm.candidateName"
            placeholder="请输入候选人姓名"
            maxlength="50"
          />
        </el-form-item>
        <el-form-item label="手机号：">
          <el-input
            v-model="referForm.candidatePhone"
            placeholder="选填"
            maxlength="20"
          />
        </el-form-item>
        <el-form-item label="邮箱：">
          <el-input
            v-model="referForm.candidateEmail"
            placeholder="选填"
            maxlength="100"
          />
        </el-form-item>
        <el-form-item label="与候选人关系：">
          <el-select v-model="referForm.relationship" placeholder="请选择关系" style="width: 100%;">
            <el-option label="同事" value="同事" />
            <el-option label="朋友" value="朋友" />
            <el-option label="前同事" value="前同事" />
            <el-option label="校友" value="校友" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>

        <el-divider content-position="left">推荐备注</el-divider>
        <el-form-item label="推荐备注：">
          <el-input
            v-model="referForm.referralNote"
            type="textarea"
            :rows="3"
            placeholder="选填，如推荐理由、候选人亮点等"
            maxlength="500"
            show-word-limit
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
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, WarningFilled } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import axios from 'axios'
import { createReferral } from '@/api/referral'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// ---- State ----
const loading = ref(true)
const error = ref<string | null>(null)
const detail = ref<Record<string, any> | null>(null)
const program = computed(() => detail.value?.program as Record<string, any> | null)
const programJob = computed(() => detail.value?.programJob as Record<string, any> | null)
const jobPosition = computed(() => detail.value?.jobPosition as Record<string, any> | null)

// ---- Computed ----
const programJobBonus = computed(() => {
  const pj = programJob.value
  return (pj?.bonusAmount as number) || (program.value?.bonusAmount as number) || 0
})

const salaryText = computed(() => {
  const min = (programJob.value?.minSalary ?? jobPosition.value?.minSalary) as number | undefined
  const max = (programJob.value?.maxSalary ?? jobPosition.value?.maxSalary) as number | undefined
  if (!min && !max) return ''
  if (min && max) return `¥${(min / 1000).toFixed(0)}K - ¥${(max / 1000).toFixed(0)}K`
  if (min) return `¥${(min / 1000).toFixed(0)}K 起`
  return `最高 ¥${(max! / 1000).toFixed(0)}K`
})

const positionTypeLabel = computed(() => {
  const map: Record<number, string> = { 1: '全职', 2: '兼职', 3: '实习', 4: '外包' }
  return map[jobPosition.value?.positionType as number] || ''
})

const experienceLabel = computed(() => {
  const map: Record<number, string> = { 1: '应届生', 2: '1-3年', 3: '3-5年', 4: '5-10年', 5: '10年以上' }
  return map[jobPosition.value?.experienceLevel as number] || ''
})

const educationLabel = computed(() => {
  const map: Record<number, string> = { 1: '大专', 2: '本科', 3: '硕士', 4: '博士', 5: '不限' }
  return map[jobPosition.value?.educationLevel as number] || ''
})

const responsibilitiesList = computed(() => parseJsonArray(detail.value?.jobPosition?.responsibilities))
const requirementsList = computed(() => parseJsonArray(detail.value?.jobPosition?.requirements))
const skillsList = computed(() => parseJsonArray(detail.value?.jobPosition?.skills))

const offerItems = computed(() => {
  const raw = jobPosition.value?.description as string
  if (!raw) return []
  return extractMarkdownSection(raw, '我们提供')
})

const bonusStructureItems = computed(() => {
  const raw = program.value?.bonusStructure
  if (!raw) return []
  let obj: Record<string, unknown>
  if (typeof raw === 'string') {
    try { obj = JSON.parse(raw) } catch { return [] }
  } else if (typeof raw === 'object') {
    obj = raw as Record<string, unknown>
  } else {
    return []
  }
  return Object.entries(obj)
    .filter(([, v]) => typeof v === 'number')
    .map(([label, amount]) => ({ label, amount: amount as number }))
})

// ---- Referral Dialog ----
const referDialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()

interface ReferForm {
  candidateId: string
  candidateName: string
  candidatePhone: string
  candidateEmail: string
  relationship: string
  referralNote: string
}

const referForm = reactive<ReferForm>({
  candidateId: '',
  candidateName: '',
  candidatePhone: '',
  candidateEmail: '',
  relationship: '',
  referralNote: '',
})

const referRules: FormRules<ReferForm> = {
  candidateId: [{ required: true, message: '请输入候选人ID', trigger: 'blur' }],
  candidateName: [{ required: true, message: '请输入候选人姓名', trigger: 'blur' }],
}

function resetReferForm() {
  referForm.candidateId = ''
  referForm.candidateName = ''
  referForm.candidatePhone = ''
  referForm.candidateEmail = ''
  referForm.relationship = ''
  referForm.referralNote = ''
}

function openReferDialog() {
  if (!userStore.isLoggedIn) {
    router.push({ name: 'PhoneLogin', query: { redirect: route.fullPath } })
    return
  }
  resetReferForm()
  referDialogVisible.value = true
}

async function handleRefer() {
  if (!formRef.value || !detail.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const pj = programJob.value
      const jp = jobPosition.value
      await createReferral({
        programId: Number(pj.programId),
        programJobId: Number(pj.id),
        referrerId: Number(userStore.userInfo?.id ?? 0),
        referrerName: userStore.userInfo?.realName || userStore.userInfo?.name || undefined,
        referrerDept: userStore.userInfo?.departmentName || undefined,
        candidateId: Number(referForm.candidateId),
        candidateName: referForm.candidateName || undefined,
        candidatePhone: referForm.candidatePhone || undefined,
        candidateEmail: referForm.candidateEmail || undefined,
        jobId: Number(pj.jobPositionId),
        jobTitle: pj.jobTitle || jp?.title || undefined,
        relationship: referForm.relationship || undefined,
        referralNote: referForm.referralNote || undefined,
        bonus: programJobBonus.value as number,
      })
      ElMessage.success('推荐成功！')
      referDialogVisible.value = false
    } catch {
      // Error shown by request interceptor
    } finally {
      submitting.value = false
    }
  })
}

// ---- API ----
async function fetchDetail() {
  const programJobId = route.params.programJobId as string
  if (!programJobId) {
    error.value = '缺少职位参数'
    loading.value = false
    return
  }

  loading.value = true
  error.value = null
  detail.value = null

  try {
    const res = await axios.get(`/api/v1/referrals/public/positions/${programJobId}/detail`)
    if (res.data?.code === 0 && res.data?.data) {
      detail.value = res.data.data
    } else {
      error.value = res.data?.message || '获取职位详情失败'
    }
  } catch (err: unknown) {
    error.value = err instanceof Error ? err.message : '获取职位详情失败'
  } finally {
    loading.value = false
  }
}

// ---- Helpers ----
function formatBonus(amount: number | null | undefined): string {
  if (amount == null || amount === 0) return '-'
  return amount.toLocaleString('zh-CN')
}

function parseJsonArray(raw: string | undefined): string[] {
  if (!raw) return []
  try {
    const parsed = JSON.parse(raw)
    if (Array.isArray(parsed)) return parsed.map(String)
  } catch {
    // Not JSON, return empty
  }
  return []
}

/** Extract list items under a ## heading from markdown text. */
function extractMarkdownSection(md: string, sectionTitle: string): string[] {
  const lines = md.split('\n')
  let inSection = false
  const items: string[] = []

  for (const line of lines) {
    const hMatch = line.match(/^##\s+(.+)/)
    if (hMatch) {
      inSection = hMatch[1].trim() === sectionTitle
      continue
    }
    if (!inSection) continue
    // Match list items: - xxx or 1. xxx
    const itemMatch = line.match(/^[\-\*\d+\.\、]\s+(.+)/)
    if (itemMatch) {
      items.push(itemMatch[1].trim())
    }
  }
  return items
}

onMounted(() => {
  fetchDetail()
})
</script>

<style scoped>
/* ---- Page ---- */
.rpd-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 0 40px;
}

/* ---- Top Bar ---- */
.rpd-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.rpd-back-btn {
  font-size: 14px;
  color: var(--c-text-secondary);
}

/* ---- Center Box (error/empty) ---- */
.rpd-center-box {
  text-align: center;
  padding: 80px 24px;
}

.rpd-center-title {
  font-size: 16px;
  color: var(--c-text);
  margin: 16px 0 8px;
}

.rpd-center-desc {
  font-size: 13px;
  color: var(--c-text-muted);
  margin-bottom: 20px;
}

/* ---- Hero ---- */
.rpd-hero {
  background: var(--c-card);
  border-radius: var(--c-radius-lg);
  box-shadow: var(--c-shadow-sm);
  border: 1px solid var(--c-border);
  padding: 28px 32px;
  margin-bottom: 24px;
  position: relative;
  overflow: hidden;
}

.rpd-hero::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, var(--c-primary), var(--c-primary-light));
}

.rpd-hero-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 16px;
  gap: 24px;
}

.rpd-hero-main {
  flex: 1;
  min-width: 0;
}

.rpd-hero-program {
  font-size: 13px;
  color: var(--c-text-muted);
  margin-bottom: 8px;
}

.rpd-hero-title {
  margin: 0 0 10px;
  font-size: 24px;
  font-weight: 700;
  color: var(--c-text);
  line-height: 1.3;
}

.rpd-hero-tags {
  display: flex;
  gap: 8px;
}

.rpd-hero-bonus {
  text-align: right;
  flex-shrink: 0;
}

.rpd-hero-bonus-val {
  font-size: 32px;
  font-weight: 700;
  color: var(--c-primary);
  line-height: 1.1;
}

.rpd-currency {
  font-size: 20px;
  font-weight: 500;
}

.rpd-hero-bonus-label {
  font-size: 12px;
  color: var(--c-text-muted);
  margin-top: 4px;
}

.rpd-hero-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  color: var(--c-text-secondary);
  font-size: 13px;
  margin-bottom: 20px;
}

.rpd-meta-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.rpd-meta-divider {
  color: var(--c-border);
}

.rpd-hero-actions {
  padding-top: 16px;
  border-top: 1px solid var(--c-border);
}

/* ---- Body Layout ---- */
.rpd-body {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 24px;
  align-items: start;
}

/* ---- Main Content ---- */
.rpd-main {
  min-width: 0;
}

.rpd-section {
  background: var(--c-card);
  border-radius: var(--c-radius-lg);
  box-shadow: var(--c-shadow-sm);
  border: 1px solid var(--c-border);
  padding: 24px;
  margin-bottom: 20px;
}

.rpd-section-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--c-text);
  margin: 0 0 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--c-border);
}


.rpd-list {
  margin: 0;
  padding: 0 0 0 18px;
}

.rpd-list li {
  font-size: 14px;
  color: var(--c-text-secondary);
  line-height: 1.8;
  margin-bottom: 6px;
}

.rpd-list li::marker {
  color: var(--c-primary);
}

.rpd-skills {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.rpd-skill-tag {
  font-size: 13px;
  font-weight: 500;
}

/* ---- Sidebar ---- */
.rpd-sidebar {
  position: sticky;
  top: 84px;
}

.rpd-sidebar-card {
  background: var(--c-card);
  border-radius: var(--c-radius-lg);
  box-shadow: var(--c-shadow-sm);
  border: 1px solid var(--c-border);
  padding: 20px;
  margin-bottom: 16px;
}

.rpd-sidebar-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--c-text);
  margin: 0 0 16px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--c-border);
}

.rpd-sidebar-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  font-size: 13px;
}

.rpd-sidebar-item:not(:last-child) {
  border-bottom: 1px solid var(--c-border-light);
}

.rpd-sidebar-key {
  color: var(--c-text-muted);
}

.rpd-sidebar-value {
  color: var(--c-text-secondary);
  text-align: right;
  max-width: 60%;
  word-break: break-word;
}

.rpd-sidebar-value-bold {
  font-weight: 600;
  color: var(--c-primary);
}

.rpd-sidebar-cta {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
}

/* ---- Form helpers ---- */
.rpd-form-bonus {
  font-weight: 600;
  color: var(--c-primary);
}

/* ---- Responsive ---- */
@media (max-width: 1024px) {
  .rpd-body {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .rpd-hero {
    padding: 20px 16px;
  }

  .rpd-hero-top {
    flex-direction: column;
  }

  .rpd-hero-bonus {
    text-align: left;
  }

  .rpd-hero-title {
    font-size: 20px;
  }
}
</style>

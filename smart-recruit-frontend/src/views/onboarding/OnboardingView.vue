<template>
  <div class="onboarding-page">
    <div class="sr-page-header">
      <h1>入职管理</h1>
      <p>跟踪和管理新员工入职流程</p>
    </div>

    <!-- Stats -->
    <div class="sr-stat-cards">
      <div class="sr-stat-card">
        <div class="stat-icon" style="background: #f0f9ff; color: #0284c7"><el-icon :size="20"><UserFilled /></el-icon></div>
        <div class="stat-info"><div class="stat-value">{{ stats.totalCount }}</div><div class="stat-label">入职总人数</div></div>
      </div>
      <div class="sr-stat-card">
        <div class="stat-icon" style="background: #f1f5f9; color: #64748b"><el-icon :size="20"><Clock /></el-icon></div>
        <div class="stat-info"><div class="stat-value">{{ stats.pendingCount }}</div><div class="stat-label">待入职</div></div>
      </div>
      <div class="sr-stat-card">
        <div class="stat-icon" style="background: #eef2ff; color: #4f46e5"><el-icon :size="20"><Tickets /></el-icon></div>
        <div class="stat-info"><div class="stat-value">{{ stats.activeCount }}</div><div class="stat-label">入职中</div></div>
      </div>
      <div class="sr-stat-card">
        <div class="stat-icon" style="background: #ecfdf5; color: #059669"><el-icon :size="20"><CircleCheck /></el-icon></div>
        <div class="stat-info"><div class="stat-value">{{ stats.doneCount }}</div><div class="stat-label">已入职</div></div>
      </div>
      <div class="sr-stat-card">
        <div class="stat-icon" style="background: #fef2f2; color: #dc2626"><el-icon :size="20"><WarningFilled /></el-icon></div>
        <div class="stat-info"><div class="stat-value">{{ stats.atRiskCount }}</div><div class="stat-label">有风险</div></div>
      </div>
    </div>

    <!-- Table -->
    <div class="sr-section">
      <div class="sr-toolbar" style="flex-wrap: wrap; gap: 8px;">
        <div class="sr-filter-bar" style="flex-wrap: wrap; gap: 8px;">
          <el-input v-model="filters.employeeNo" placeholder="员工编号" clearable style="width: 130px;" @clear="onFilterChange" @keyup.enter="onFilterChange" />
          <el-input v-model="filters.employeeName" placeholder="姓名" clearable style="width: 110px;" @clear="onFilterChange" @keyup.enter="onFilterChange" />
          <el-tree-select
            v-model="filters.departmentName"
            :data="deptTree"
            :props="{ label: 'name', value: 'name', children: 'children' }"
            placeholder="部门"
            clearable
            filterable
            check-strictly
            style="width: 150px;"
            popper-class="dept-filter-popper"
            @change="onFilterChange"
          />
          <el-select v-model="filters.positionTitle" placeholder="职位" clearable filterable style="width: 150px;" @change="onFilterChange">
            <el-option v-for="p in positionOptions" :key="p" :label="p" :value="p" />
          </el-select>
          <el-select v-model="filters.status" placeholder="状态" clearable style="width: 100px;" @change="onFilterChange">
            <el-option label="全部" value="" />
            <el-option label="待入职" value="0" />
            <el-option label="入职中" value="1" />
            <el-option label="已入职" value="2" />
            <el-option label="有风险" value="3" />
          </el-select>
          <el-select v-model="filters.employeeStatus" placeholder="员工状态" clearable style="width: 120px;" @change="onFilterChange">
            <el-option label="全部" value="" />
            <el-option label="待入职" value="0" />
            <el-option label="试用期" value="1" />
            <el-option label="正式" value="2" />
            <el-option label="已离职" value="3" />
          </el-select>
          <el-date-picker
            v-model="filters.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="入职开始日期"
            end-placeholder="入职结束日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            clearable
            style="width: 240px;"
            @change="onFilterChange"
          />
        </div>
        <el-button type="primary" :icon="Plus" @click="openCreateDialog">新建入职</el-button>
      </div>

      <el-table v-loading="loading" :data="onboardings" stripe empty-text="暂无入职数据">
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="employeeNo" label="员工编号" width="150" />
        <el-table-column prop="employeeName" label="姓名" width="100" />
        <el-table-column prop="level" label="职级" width="80" />
        <el-table-column prop="jobTitle" label="职位" min-width="150" />
        <el-table-column prop="departmentName" label="部门" width="120" />
        <el-table-column prop="onboardDate" label="入职日期" width="120" />
        <el-table-column label="进度" width="220">
          <template #default="{ row }">
            <div style="display: flex; align-items: center; gap: 8px;">
              <el-progress
                :percentage="(() => {
                  const totalDocs = row.totalDocumentsCount ?? 6
                  const doneDocs = row.completedDocumentsCount ?? 0
                  const totalUnits = totalDocs + 5
                  const doneUnits = doneDocs + Math.max(0, (row.currentStep || 1) - 1)
                  return totalUnits > 0 ? Math.round(doneUnits / totalUnits * 100) : 0
                })()"
                :color="row.riskLevel === 2 ? '#dc2626' : '#4f46e5'"
                :stroke-width="8" style="flex: 1;"
              />
              <span style="font-size: 12px; color: var(--c-text-secondary); white-space: nowrap;">
                {{ (() => {
                  const totalDocs = row.totalDocumentsCount ?? 6
                  const doneDocs = row.completedDocumentsCount ?? 0
                  return doneDocs + Math.max(0, (row.currentStep || 1) - 1)
                })() }}/{{ (row.totalDocumentsCount ?? 6) + 5 }}
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="风险等级" width="100">
          <template #default="{ row }">
            <el-tag :type="getRiskLevelType(row.riskLevel)" size="small">
              {{ getRiskLevelLabel(row.riskLevel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getOnboardingStatusType(row.status)" size="small">
              {{ getOnboardingStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="员工状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getEmployeeStatusType(row.employeeStatus)" size="small">
              {{ getEmployeeStatusLabel(row.employeeStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button size="small" text type="primary" @click="$router.push(`/onboarding/${row.id}`)">
              {{ row.status === 2 ? '详情' : '入职办理' }}
            </el-button>
            <el-button
              v-if="row.status === 0 || row.status === 3"
              size="small"
              text
              type="danger"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div style="display: flex; justify-content: flex-end; margin-top: 16px;">
        <el-pagination
          v-model:current-page="page" v-model:page-size="size" :total="total"
          :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange" @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- Create Dialog -->
    <el-dialog v-model="createDialogVisible" title="新建入职" width="600px" :close-on-click-modal="false">
      <el-form :model="createForm" label-width="90px" :rules="createRules" ref="createFormRef">
        <el-form-item label="选择Offer" prop="offerId">
          <el-select
            v-model="createForm.offerId"
            filterable
            placeholder="选择已接受的Offer"
            style="width: 100%;"
            :filter-method="filterOfferOptions"
            @change="onOfferSelect"
          >
            <el-option
              v-for="o in filteredOffers"
              :key="o.id"
              :label="`${o.offerNo || ''} — ${o.candidateName} — ${o.positionTitle}`"
              :value="o.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="候选人">
          <el-input :model-value="createForm.candidateName" disabled />
        </el-form-item>
        <el-form-item label="职位">
          <el-input :model-value="createForm.positionTitle" disabled />
        </el-form-item>
        <el-form-item label="职级">
          <el-input :model-value="createForm.level" disabled />
        </el-form-item>
        <el-form-item label="部门">
          <el-input :model-value="createForm.departmentName" disabled />
        </el-form-item>
        <el-form-item label="入职日期" prop="onboardDate">
          <el-date-picker
            v-model="createForm.onboardDate"
            type="date"
            placeholder="选择入职日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width: 100%;"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreate">确认创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Plus, Tickets, CircleCheck, WarningFilled, UserFilled, Clock } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { getOnboardings, getOnboardingStats, createOnboarding, deleteOnboarding } from '@/api/onboarding'
import { getOffers } from '@/api/offer'
import { getDepartments } from '@/api/system'
import { getJobs } from '@/api/job'
import {
  getOnboardingStatusLabel, getOnboardingStatusType,
  getRiskLevelLabel, getRiskLevelType,
  getEmployeeStatusLabel, getEmployeeStatusType,
} from '@/utils/format'
import type { OnboardingVO, OnboardingStatsVO, OfferVO, DepartmentTreeVO } from '@/types/models'

const loaded = ref(false)
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const onboardings = ref<OnboardingVO[]>([])
const filters = reactive({
  employeeNo: '',
  employeeName: '',
  departmentName: '',
  positionTitle: '',
  status: '',
  employeeStatus: '',
  dateRange: null as [string, string] | null,
})

const deptTree = ref<DepartmentTreeVO[]>([])
const positionOptions = ref<string[]>([])

const stats = reactive<OnboardingStatsVO>({
  totalCount: 0, pendingCount: 0, activeCount: 0, doneCount: 0,
  atRiskCount: 0, lowRiskCount: 0, mediumRiskCount: 0, highRiskCount: 0,
})

// ---- Create Dialog ----
const createDialogVisible = ref(false)
const creating = ref(false)
const createFormRef = ref<FormInstance>()
const acceptedOffers = ref<OfferVO[]>([])
const filteredOffers = ref<OfferVO[]>([])

function filterOfferOptions(query: string) {
  if (!query) {
    filteredOffers.value = acceptedOffers.value
    return
  }
  const lower = query.toLowerCase()
  filteredOffers.value = acceptedOffers.value.filter(o =>
    (o.offerNo && o.offerNo.toLowerCase().includes(lower)) ||
    (o.candidateName && o.candidateName.toLowerCase().includes(lower))
  )
}

const createForm = reactive({
  offerId: '',
  candidateId: '',
  candidateName: '',
  positionTitle: '',
  level: '',
  departmentName: '',
  onboardDate: '',
})

const createRules: FormRules = {
  offerId: [{ required: true, message: '请选择Offer', trigger: 'change' }],
  onboardDate: [{ required: true, message: '请选择入职日期', trigger: 'change' }],
}

function onOfferSelect(offerId: string | number) {
  const offer = acceptedOffers.value.find(o => String(o.id) === String(offerId))
  if (offer) {
    createForm.candidateId = String(offer.candidateId)
    createForm.candidateName = offer.candidateName
    createForm.positionTitle = offer.positionTitle
    createForm.departmentName = offer.departmentName || '-'
    createForm.level = offer.level || ''
    createForm.onboardDate = offer.expectedOnboardDate || ''
  }
}

async function openCreateDialog() {
  createForm.offerId = ''
  createForm.candidateId = ''
  createForm.candidateName = ''
  createForm.positionTitle = ''
  createForm.level = ''
  createForm.departmentName = ''
  createForm.onboardDate = ''
  try {
    // 获取已接受的 Offer（status=4）
    const res = await getOffers({ page: 1, size: 200, status: 4 })
    acceptedOffers.value = res.records
    filteredOffers.value = res.records
  } catch {
    acceptedOffers.value = []
    filteredOffers.value = []
  }
  createDialogVisible.value = true
}

async function handleCreate() {
  const valid = await createFormRef.value?.validate().catch(() => false)
  if (!valid) return
  creating.value = true
  try {
    await createOnboarding({
      offerId: createForm.offerId,
      candidateId: createForm.candidateId,
      onboardDate: createForm.onboardDate || undefined,
    })
    ElMessage.success('入职记录创建成功')
    createDialogVisible.value = false
    loadOnboardings()
  } catch {
    // 错误已由 HTTP 拦截器统一提示
  } finally {
    creating.value = false
  }
}

async function handleDelete(row: OnboardingVO) {
  try {
    await ElMessageBox.confirm(
      `确定删除 ${row.employeeName}（${row.employeeNo}）的入职记录吗？此操作不可恢复。`,
      '删除确认',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await deleteOnboarding(row.id)
    ElMessage.success('已删除')
    loadOnboardings()
  } catch {
    // 错误已由 HTTP 拦截器统一提示
  }
}

// ---- List ----
async function loadStats() {
  try {
    const s = await getOnboardingStats()
    Object.assign(stats, s)
  } catch {
    // stats remain at defaults
  }
}

async function loadOnboardings() {
  loading.value = true
  try {
    const res = await getOnboardings({
      page: page.value, size: size.value,
      employeeNo: filters.employeeNo || undefined,
      employeeName: filters.employeeName || undefined,
      departmentName: filters.departmentName || undefined,
      positionTitle: filters.positionTitle || undefined,
      status: filters.status || undefined,
      employeeStatus: filters.employeeStatus || undefined,
      startDate: filters.dateRange?.[0] || undefined,
      endDate: filters.dateRange?.[1] || undefined,
    })
    onboardings.value = res.records
    total.value = res.total
  } catch {
    onboardings.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
  loadStats()
}

function onFilterChange() {
  page.value = 1
  loadOnboardings()
}

function handlePageChange(p: number) { if (!loaded.value) return; page.value = p; loadOnboardings() }
function handleSizeChange(s: number) { if (!loaded.value) return; size.value = s; page.value = 1; loadOnboardings() }

async function loadDeptTree() {
  try {
    deptTree.value = await getDepartments()
  } catch {
    deptTree.value = []
  }
}

async function loadPositionOptions() {
  try {
    const res = await getJobs({ page: 1, size: 200 })
    const titles = new Set<string>()
    for (const job of res.records) {
      if (job.title) titles.add(job.title)
    }
    positionOptions.value = Array.from(titles).sort()
  } catch {
    positionOptions.value = []
  }
}

onMounted(async () => {
  await Promise.all([loadOnboardings(), loadDeptTree(), loadPositionOptions()])
  loaded.value = true
})
</script>

<style>
.dept-filter-popper {
  min-width: 150px !important;
}
</style>

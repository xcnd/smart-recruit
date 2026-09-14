<template>
  <div class="program-jobs-page">
    <!-- Top Bar -->
    <div style="display: flex; align-items: center; gap: 12px; margin-bottom: 16px;">
      <el-button :icon="ArrowLeft" text @click="$router.push('/referral')">返回</el-button>
      <span style="color: var(--c-text-muted);">/</span>
      <h2 style="margin: 0; font-size: 18px; font-weight: 600;">
        {{ programName || '加载中...' }}
      </h2>
      <el-tag v-if="programBonus" type="primary" effect="plain" size="small">
        默认奖金 ¥{{ programBonus.toLocaleString() }}
      </el-tag>
    </div>

    <!-- Loading -->
    <template v-if="loading">
      <div class="sr-section" style="padding: 24px;">
        <el-skeleton animated :count="3">
          <template #template>
            <div style="display: flex; gap: 16px; padding: 8px 0;">
              <el-skeleton-item variant="text" style="width: 30%;" />
              <el-skeleton-item variant="text" style="width: 15%;" />
              <el-skeleton-item variant="text" style="width: 15%;" />
              <el-skeleton-item variant="text" style="width: 10%;" />
            </div>
          </template>
        </el-skeleton>
      </div>
    </template>

    <!-- Error -->
    <div v-else-if="error" class="sr-section" style="text-align: center; padding: 80px 24px;">
      <el-icon :size="48" color="var(--c-danger)"><WarningFilled /></el-icon>
      <p style="margin-top: 16px; color: var(--c-text-secondary);">{{ error }}</p>
      <el-button type="primary" style="margin-top: 16px;" @click="fetchData">重试</el-button>
    </div>

    <!-- Content -->
    <template v-else>
      <div class="sr-section" style="margin-bottom: 16px;">
        <div class="flex-between">
          <span class="text-muted">
            已关联 <strong>{{ programJobs.length }}</strong> 个职位，
            其中 <strong>{{ enabledCount }}</strong> 个已启用
          </span>
          <div style="display: flex; gap: 12px;">
            <el-button type="primary" @click="openAddDialog">添加职位</el-button>
            <el-button :loading="batchSaving" @click="handleBatchSave">批量保存</el-button>
          </div>
        </div>
      </div>

      <!-- Empty state -->
      <div v-if="!programJobs.length" class="sr-section" style="text-align: center; padding: 80px 24px;">
        <el-empty description="暂无关联职位">
          <el-button type="primary" @click="openAddDialog">添加职位</el-button>
        </el-empty>
      </div>

      <!-- Jobs Table -->
      <div v-else class="sr-section" style="padding: 0;">
        <el-table :data="programJobs" stripe style="width: 100%;">
          <el-table-column type="index" label="序号" width="60" align="center" />

          <el-table-column label="职位名称" min-width="160">
            <template #default="{ row }">
              {{ getJobName(row.jobPositionId) }}
            </template>
          </el-table-column>

          <el-table-column label="部门" width="140">
            <template #default="{ row }">
              {{ getJobDept(row.jobPositionId) || '-' }}
            </template>
          </el-table-column>

          <el-table-column label="内推奖金" width="220" align="center">
            <template #default="{ row }">
              <el-input-number
                v-model="bonusEdits[String(row.id)]"
                :min="0"
                :step="500"
                :max="100000"
                size="small"
                controls-position="right"
                style="width: 130px;"
                :placeholder="`默认 ¥${programBonus}`"
              />
              <span style="margin-left: 4px; font-size: 12px; color: var(--c-text-secondary);">元</span>
            </template>
          </el-table-column>

          <el-table-column label="启用" width="70" align="center">
            <template #default="{ row }">
              <el-switch
                :model-value="row.isEnabled === 1"
                size="small"
                :loading="togglingMap[String(row.id)]"
                @change="(val: boolean) => handleToggleJob(row, val)"
              />
            </template>
          </el-table-column>

          <el-table-column label="标签" width="100" align="center">
            <template #default="{ row }">
              <el-select
                v-model="tagEdits[String(row.id)]"
                size="small"
                placeholder="无"
                clearable
                style="width: 90px;"
              >
                <el-option
                  v-for="(label, code) in ProgramJobTagLabels"
                  :key="Number(code)"
                  :label="label"
                  :value="Number(code)"
                />
              </el-select>
            </template>
          </el-table-column>

          <el-table-column label="创建时间" width="110" align="center">
            <template #default="{ row }">
              {{ formatDate(row.createTime) }}
            </template>
          </el-table-column>

          <el-table-column label="操作" width="80" align="center" fixed="right">
            <template #default="{ row }">
              <el-button
                type="danger"
                link
                size="small"
                @click="handleRemoveJob(row)"
              >
                移除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </template>

    <!-- Add Job Dialog -->
    <el-dialog
      v-model="addDialogVisible"
      title="添加内推职位"
      width="640px"
      :close-on-click-modal="false"
    >
      <div style="display: flex; gap: 12px; margin-bottom: 12px;">
        <el-tree-select
          v-model="selectedDept"
          :data="deptOptions"
          :props="{ label: 'name', value: 'id', children: 'children' }"
          placeholder="选择部门"
          clearable
          check-strictly
          style="width: 200px;"
          @change="jobSearch = ''"
        />
        <el-input
          v-model="jobSearch"
          placeholder="搜索职位名称..."
          clearable
          style="flex: 1;"
        />
      </div>
      <el-table
        :data="filteredAvailableJobs"
        stripe
        max-height="360"
        highlight-current-row
        @row-click="toggleJobSelection"
        @selection-change="onSelectionChange"
        ref="selectionTableRef"
      >
        <el-table-column type="selection" width="40" />
        <el-table-column prop="name" label="职位名称" min-width="160">
          <template #default="{ row }">
            <div>{{ row.name }}</div>
            <div style="font-size: 12px; color: var(--c-text-muted);">{{ row.dept }}</div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.statusCode === 1 ? 'success' : 'info'" size="small">
              {{ row.statusCode === 1 ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="selectedJobs.length === 0" @click="confirmAddJobs">
          添加选中职位 ({{ selectedJobs.length }})
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft, WarningFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getPrograms,
  getProgramJobs,
  batchSaveProgramJobs,
  deleteProgramJob,
  toggleProgramJob,
} from '@/api/referral'
import { getJobs } from '@/api/job'
import { getDepartments } from '@/api/system'
import type { ReferralProgramVO, RefProgramJobVO, JobVO, DepartmentTreeVO } from '@/types/models'
import { ProgramJobTagLabels } from '@/constants/enums'

const route = useRoute()
const programId = Number(route.params.id)

// ---- State ----
const loading = ref(true)
const error = ref<string | null>(null)
const program = ref<ReferralProgramVO | null>(null)
const programJobs = ref<RefProgramJobVO[]>([])
const allJobs = ref<JobVO[]>([])          // All published jobs from recruitment
const batchSaving = ref(false)

const bonusEdits = reactive<Record<string, number | null>>({})
const tagEdits = reactive<Record<string, number | null>>({})
const togglingMap = reactive<Record<string, boolean>>({})

// Add dialog
const addDialogVisible = ref(false)
const jobSearch = ref('')
const selectedDept = ref<string>('')
const selectedJobs = ref<{ id: number | string; name: string; dept: string; statusCode: number }[]>([])
const deptOptions = ref<DepartmentTreeVO[]>([])

// ---- Computed ----
const programName = computed(() => program.value?.title ?? '职位管理')
const programBonus = computed(() => program.value?.bonusAmount ?? 0)
const enabledCount = computed(() => programJobs.value.filter((j) => j.isEnabled === 1).length)

const filteredAvailableJobs = computed(() => {
  const keyword = jobSearch.value.trim().toLowerCase()
  const filterDept = selectedDept.value ? String(selectedDept.value) : ''
  const assignedIds = new Set(programJobs.value.map((j) => String(j.jobPositionId)))
  return allJobs.value
    .filter((j) => !assignedIds.has(String(j.id)) && (j.status as unknown as number) === 1)
    .filter((j) => !filterDept || String(j.departmentId) === filterDept)
    .filter((j) => !keyword || j.title.toLowerCase().includes(keyword))
    .map((j) => ({
      id: j.id,
      name: j.title,
      dept: j.departmentName || '-',
      statusCode: j.status as unknown as number,
    }))
})

// ---- Helpers ----
function getJobName(jobPositionId: string): string {
  const job = allJobs.value.find((j) => String(j.id) === String(jobPositionId))
  return job?.title ?? `职位 #${jobPositionId}`
}

function getJobDept(jobPositionId: string): string {
  const job = allJobs.value.find((j) => String(j.id) === String(jobPositionId))
  return job?.departmentName ?? ''
}

function formatDate(d: string | undefined): string {
  if (!d) return '-'
  const date = new Date(d)
  if (isNaN(date.getTime())) return '-'
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

function initEdits() {
  for (const job of programJobs.value) {
    const key = String(job.id)
    if (!(key in bonusEdits)) {
      bonusEdits[key] = job.bonusAmount
    }
    if (!(key in tagEdits)) {
      tagEdits[key] = job.tag
    }
  }
}

// ---- Add Dialog ----
function onSelectionChange(rows: { id: number | string }[]) {
  selectedJobs.value = rows as { id: number | string; name: string; dept: string; statusCode: number }[]
}

function toggleJobSelection(row: { id: number | string; name: string; dept: string; statusCode: number }) {
  const idx = selectedJobs.value.findIndex((j) => String(j.id) === String(row.id))
  if (idx >= 0) {
    selectedJobs.value.splice(idx, 1)
  } else {
    selectedJobs.value.push(row)
  }
}

function openAddDialog() {
  jobSearch.value = ''
  selectedDept.value = ''
  selectedJobs.value = []
  addDialogVisible.value = true
}

function confirmAddJobs() {
  // Add selected jobs as local entries (not saved until batch save)
  const now = new Date().toISOString()
  for (const selected of selectedJobs.value) {
    const tempId = -Date.now() - Math.random() // negative temp ID
    const newJob: RefProgramJobVO = {
      id: String(tempId),
      programId: String(programId),
      jobPositionId: String(selected.id),
      isEnabled: 1,
      bonusAmount: null,
      tag: null,
      createTime: now,
      updateTime: now,
    }
    programJobs.value.push(newJob)
    bonusEdits[String(tempId)] = null
    tagEdits[String(tempId)] = null
  }
  addDialogVisible.value = false
  ElMessage.success(`已添加 ${selectedJobs.value.length} 个职位，请点击批量保存生效`)
}

// ---- API Actions ----
async function fetchData() {
  loading.value = true
  error.value = null
  try {
    const [programsRes, jobsRes, allJobsRes, deptRes] = await Promise.all([
      getPrograms(),
      getProgramJobs(programId),
      getJobs({ page: 1, size: 200, status: 1 }),
      getDepartments(),
    ])
    const list = Array.isArray(programsRes) ? programsRes : []
    program.value = list.find((p) => String(p.id) === String(programId)) ?? null
    programJobs.value = Array.isArray(jobsRes) ? jobsRes : []
    allJobs.value = Array.isArray(allJobsRes?.records) ? allJobsRes.records : []
    deptOptions.value = Array.isArray(deptRes) ? deptRes : []
    initEdits()
  } catch (err: unknown) {
    error.value = err instanceof Error ? err.message : '加载失败'
  } finally {
    loading.value = false
  }
}

async function handleToggleJob(row: RefProgramJobVO, enabled: boolean) {
  // Only toggle server-side for existing (non-temp) jobs
  if (Number(row.id) < 0) {
    row.isEnabled = enabled ? 1 : 0
    return
  }
  const key = String(row.id)
  togglingMap[key] = true
  try {
    await toggleProgramJob(Number(row.id))
    row.isEnabled = enabled ? 1 : 0
  } catch {
    // ignore
  } finally {
    togglingMap[key] = false
  }
}

async function handleRemoveJob(row: RefProgramJobVO) {
  try {
    await ElMessageBox.confirm('确定要移除此职位吗？', '移除确认', {
      type: 'warning',
      confirmButtonText: '确认移除',
    })
  } catch {
    return
  }

  // For temp entries, just remove from list
  if (Number(row.id) < 0) {
    const idx = programJobs.value.findIndex((j) => j.id === row.id)
    if (idx >= 0) programJobs.value.splice(idx, 1)
    return
  }

  try {
    await deleteProgramJob(Number(row.id))
    programJobs.value = programJobs.value.filter((j) => j.id !== row.id)
    ElMessage.success('已移除')
  } catch {
    // error handled by interceptor
  }
}

async function handleBatchSave() {
  if (!programJobs.value.length) return
  batchSaving.value = true
  try {
    const data = programJobs.value.map((j) => ({
      id: Number(j.id) > 0 ? String(j.id) : undefined,
      jobPositionId: Number(j.jobPositionId),
      isEnabled: j.isEnabled,
      bonusAmount: bonusEdits[String(j.id)] ?? j.bonusAmount,
      tag: tagEdits[String(j.id)] ?? j.tag,
      jobTitle: getJobName(String(j.jobPositionId)),
    }))
    await batchSaveProgramJobs(programId, data)
    ElMessage.success('保存成功')
    await fetchData()
  } catch {
    // error handled
  } finally {
    batchSaving.value = false
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.text-muted {
  color: var(--c-text-muted);
  font-size: 13px;
}
</style>

<template>
  <div class="referral-home-page">
    <!-- Page Header -->
    <div class="sr-page-header">
      <div class="flex-between">
        <div>
          <h1>内推管理</h1>
          <p>管理和配置内推计划与奖金</p>
        </div>
        <div v-if="isAdmin" style="display: flex; gap: 12px;">
          <el-button type="primary" @click="openCreateDialog">
            新建计划
          </el-button>
          <el-button :loading="batchSaving" @click="handleBatchSaveAll">
            批量保存
          </el-button>
        </div>
      </div>
    </div>

    <!-- Loading State: Skeleton -->
    <template v-if="loading">
      <div class="sr-section" style="padding: 24px;">
        <el-skeleton animated :count="4">
          <template #template>
            <div style="display: flex; align-items: center; gap: 16px; padding: 8px 0;">
              <el-skeleton-item variant="text" style="width: 25%;" />
              <el-skeleton-item variant="text" style="width: 10%;" />
              <el-skeleton-item variant="text" style="width: 15%;" />
              <el-skeleton-item variant="text" style="width: 20%;" />
              <el-skeleton-item variant="text" style="width: 12%;" />
              <el-skeleton-item variant="text" style="width: 10%;" />
            </div>
          </template>
        </el-skeleton>
      </div>
    </template>

    <!-- Error State -->
    <div v-else-if="error" class="sr-section" style="text-align: center; padding: 80px 24px;">
      <el-icon :size="48" color="var(--c-danger)">
        <WarningFilled />
      </el-icon>
      <p style="margin-top: 16px; font-size: 16px; color: var(--c-text-secondary);">
        加载失败，请稍后重试
      </p>
      <p style="margin-top: 8px; font-size: 13px; color: var(--c-text-muted);">
        {{ error }}
      </p>
      <el-button type="primary" style="margin-top: 20px;" @click="fetchData">重新加载</el-button>
    </div>

    <!-- Empty State -->
    <div v-else-if="!programs.length" class="sr-section" style="text-align: center; padding: 80px 24px;">
      <el-empty description="暂无内推计划">
        <el-button type="primary" @click="openCreateDialog">创建第一个内推计划</el-button>
      </el-empty>
    </div>

    <!-- Data State -->
    <div v-else class="sr-section" style="padding: 0;">
      <el-table :data="programs" stripe style="width: 100%;">
        <el-table-column type="index" label="序号" width="60" align="center" />

        <el-table-column prop="title" label="计划名称" min-width="160">
          <template #default="{ row }">
            <router-link
              :to="`/referral/${row.id}`"
              class="program-title-link"
            >
              {{ row.title }}
            </router-link>
          </template>
        </el-table-column>

        <el-table-column v-if="isAdmin" label="启用状态" width="100" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              :loading="togglingMap[String(row.id)]"
              @change="(val: boolean) => handleToggle(row, val)"
            />
          </template>
        </el-table-column>

        <el-table-column v-if="isAdmin" label="奖金金额" width="140" align="center">
          <template #default="{ row }">
            <el-input-number
              v-model="bonusEdits[String(row.id)]"
              :min="0"
              :step="500"
              :max="100000"
              size="small"
              controls-position="right"
              style="width: 100px;"
            />
            <span style="margin-left: 4px; font-size: 12px; color: var(--c-text-secondary);">元</span>
          </template>
        </el-table-column>

        <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.description">{{ row.description }}</span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>

        <el-table-column label="开始日期" width="120" align="center">
          <template #default="{ row }">
            {{ formatDate(row.startDate) }}
          </template>
        </el-table-column>

        <el-table-column label="结束日期" width="120" align="center">
          <template #default="{ row }">
            {{ formatDate(row.endDate) }}
          </template>
        </el-table-column>

        <el-table-column label="创建时间" width="120" align="center">
          <template #default="{ row }">
            {{ formatDate(row.createTime) }}
          </template>
        </el-table-column>

        <el-table-column prop="createBy" label="创建人" width="100" align="center">
          <template #default="{ row }">
            {{ row.createBy || '-' }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="250" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              link
              size="small"
              @click="$router.push(`/referral/${row.id}?refer=1`)"
            >
              内推
            </el-button>
            <el-button
              v-if="isAdmin"
              type="primary"
              link
              size="small"
              @click="$router.push(`/referral/${row.id}/jobs`)"
            >
              职位
            </el-button>
            <el-button
              type="primary"
              link
              size="small"
              @click="$router.push(`/referral/${row.id}`)"
            >
              详情
            </el-button>
            <el-button
              v-if="isAdmin"
              type="primary"
              link
              size="small"
              @click="openEditDialog(row)"
            >
              编辑
            </el-button>
            <el-button
              v-if="isAdmin"
              type="danger"
              link
              size="small"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- Create / Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="formTitle"
      width="560px"
      :close-on-click-modal="false"
      @closed="resetForm"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
        label-position="right"
      >
        <el-form-item label="计划名称" prop="title">
          <el-input v-model="formData.title" placeholder="请输入计划名称" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="计划描述" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="3"
            placeholder="请输入计划描述"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="奖金金额" prop="bonusAmount">
          <el-input-number
            v-model="formData.bonusAmount"
            :min="0"
            :step="500"
            :max="100000"
            controls-position="right"
            style="width: 180px;"
          />
        </el-form-item>
        <el-form-item label="发放阶段">
          <div style="display: flex; flex-direction: column; gap: 8px; width: 100%;">
            <div
              v-for="(stage, idx) in bonusStages"
              :key="idx"
              style="display: flex; align-items: center; gap: 8px;"
            >
              <el-input
                v-model="stage.name"
                placeholder="阶段名称，如：入职发放"
                style="width: 200px;"
              />
              <el-input-number
                v-model="stage.amount"
                :min="0"
                :step="500"
                :max="100000"
                controls-position="right"
                placeholder="金额"
                style="width: 130px;"
              />
              <span style="font-size: 12px; color: var(--c-text-muted);">元</span>
              <el-button :icon="Delete" circle size="small" @click="bonusStages.splice(idx, 1)" />
            </div>
          </div>
          <el-button type="primary" link size="small" style="margin-top: 6px;" @click="bonusStages.push({ name: '', amount: 0 })">
            + 添加阶段
          </el-button>
        </el-form-item>
        <el-form-item label="开始日期" prop="startDate">
          <el-date-picker
            v-model="formData.startDate"
            type="date"
            placeholder="选择开始日期"
            value-format="YYYY-MM-DD"
            style="width: 100%;"
          />
        </el-form-item>
        <el-form-item label="结束日期" prop="endDate">
          <el-date-picker
            v-model="formData.endDate"
            type="date"
            placeholder="选填，留空表示长期有效"
            value-format="YYYY-MM-DD"
            style="width: 100%;"
          />
        </el-form-item>
        <el-form-item label="启用状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="formSaving" @click="handleFormSubmit">
          {{ editingProgram ? '保存修改' : '创建计划' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { WarningFilled, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  getPrograms,
  toggleProgram,
  batchSave,
  createProgram,
  updateProgram,
  deleteProgram,
} from '@/api/referral'
import type { ReferralProgramVO } from '@/types/models'
import { useUserStore } from '@/stores/user'

// ---- Permission ----
const userStore = useUserStore()
const isAdmin = computed(() => {
  const roleName = userStore.userInfo?.roleName || ''
  return roleName.includes('超级管理员') || roleName.includes('HR管理员')
})

// ---- State ----
const loading = ref(true)
const error = ref<string | null>(null)
const programs = ref<ReferralProgramVO[]>([])

// Bonus edit tracking: map id -> current edit value
const bonusEdits = reactive<Record<string, number>>({})
const togglingMap = reactive<Record<string, boolean>>({})
const batchSaving = ref(false)

// Dialog state
const dialogVisible = ref(false)
const formSaving = ref(false)
const editingProgram = ref<ReferralProgramVO | null>(null)
const formRef = ref<FormInstance>()
const formTitle = ref('新建内推计划')

interface BonusStage {
  name: string
  amount: number
}

interface ProgramFormData {
  title: string
  description: string
  bonusAmount: number
  startDate: string
  endDate: string
  status: number
}

const defaultFormData = (): ProgramFormData => ({
  title: '',
  description: '',
  bonusAmount: 5000,
  startDate: '',
  endDate: '',
  status: 1,
})

const bonusStages = reactive<BonusStage[]>([])

const formData = reactive<ProgramFormData>(defaultFormData())

const formRules: FormRules = {
  title: [{ required: true, message: '请输入计划名称', trigger: 'blur' }],
  bonusAmount: [{ required: true, message: '请输入奖金金额', trigger: 'blur' }],
  startDate: [{ required: true, message: '请选择开始日期', trigger: 'change' }],
}

// ---- Helpers ----
function formatDate(dateStr: string | undefined): string {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  if (isNaN(d.getTime())) return '-'
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function initBonusEdits() {
  for (const p of programs.value) {
    const key = String(p.id)
    if (!(key in bonusEdits)) {
      bonusEdits[key] = p.bonusAmount ?? 0
    }
  }
}

function openCreateDialog() {
  editingProgram.value = null
  formTitle.value = '新建内推计划'
  Object.assign(formData, defaultFormData())
  bonusStages.splice(0, bonusStages.length)
  dialogVisible.value = true
}

function openEditDialog(row: ReferralProgramVO) {
  editingProgram.value = row
  formTitle.value = '编辑内推计划'
  formData.title = row.title
  formData.description = row.description ?? ''
  formData.bonusAmount = row.bonusAmount ?? 0
  formData.startDate = row.startDate ? formatDate(row.startDate) : ''
  formData.endDate = row.endDate ? formatDate(row.endDate) : ''
  formData.status = row.status

  // Load existing bonus structure (handle JSON string from API)
  bonusStages.splice(0, bonusStages.length)
  let bs: unknown = row.bonusStructure
  if (typeof bs === 'string') {
    try { bs = JSON.parse(bs) } catch { bs = null }
  }
  if (bs && typeof bs === 'object' && !Array.isArray(bs)) {
    const obj = bs as Record<string, unknown>
    for (const [key, value] of Object.entries(obj)) {
      if (key === 'currency' || key === 'note' || key === 'remark') continue
      const amount = typeof value === 'number' ? value
        : (value && typeof value === 'object' ? ((value as Record<string, unknown>).amount as number) || 0 : 0)
      bonusStages.push({ name: key, amount })
    }
  }

  dialogVisible.value = true
}

function resetForm() {
  formRef.value?.resetFields()
  editingProgram.value = null
}

// ---- Helpers ----
function buildBonusStructure(): Record<string, number> | undefined {
  const valid = bonusStages.filter(s => s.name.trim())
  if (!valid.length) return undefined
  return Object.fromEntries(valid.map(s => [s.name.trim(), s.amount]))
}

// ---- API Actions ----
async function fetchData() {
  loading.value = true
  error.value = null
  try {
    const res = await getPrograms()
    programs.value = Array.isArray(res) ? res : []
    initBonusEdits()
  } catch (err: unknown) {
    error.value = err instanceof Error ? err.message : '网络请求失败'
    programs.value = []
  } finally {
    loading.value = false
  }
}

async function handleFormSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  formSaving.value = true
  try {
    const bonusStructure = buildBonusStructure()
    if (editingProgram.value) {
      // Update
      await updateProgram(Number(editingProgram.value.id), {
        title: formData.title,
        description: formData.description || undefined,
        bonusAmount: formData.bonusAmount,
        bonusStructure,
        startDate: formData.startDate,
        endDate: formData.endDate || undefined,
        status: formData.status,
      })
      ElMessage.success('计划更新成功')
    } else {
      // Create
      await createProgram({
        title: formData.title,
        description: formData.description || undefined,
        bonusAmount: formData.bonusAmount,
        bonusStructure,
        startDate: formData.startDate,
        endDate: formData.endDate || undefined,
        status: formData.status,
      })
      ElMessage.success('计划创建成功')
    }
    dialogVisible.value = false
    await fetchData()
  } catch {
    // Error already shown by interceptor
  } finally {
    formSaving.value = false
  }
}

async function handleDelete(row: ReferralProgramVO) {
  try {
    await ElMessageBox.confirm(
      `确定要删除计划「${row.title}」吗？此操作不可撤销。`,
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
  } catch {
    return // User cancelled
  }
  try {
    await deleteProgram(Number(row.id))
    ElMessage.success('计划已删除')
    await fetchData()
  } catch {
    // Error already shown by interceptor
  }
}

async function handleToggle(row: ReferralProgramVO, enabled: boolean) {
  const key = String(row.id)
  togglingMap[key] = true
  try {
    await toggleProgram(Number(row.id))
    row.status = enabled ? 1 : 0
    ElMessage.success(enabled ? '计划已启用' : '计划已停用')
  } catch {
    // Revert on failure: do nothing, switch will remain at original value
  } finally {
    togglingMap[key] = false
  }
}

async function handleBatchSaveAll() {
  if (!programs.value.length) return
  batchSaving.value = true
  try {
    const data = programs.value.map((p) => ({
      id: Number(p.id),
      title: p.title,
      status: p.status,
      bonusAmount: bonusEdits[String(p.id)] ?? p.bonusAmount,
      description: p.description,
      bonusStructure: p.bonusStructure,
      startDate: p.startDate,
      endDate: p.endDate,
      eligibleDeptIds: p.eligibleDeptIds,
    }))
    await batchSave(data)
    ElMessage.success('批量保存成功')
    // Refresh to get server-synced data
    await fetchData()
  } catch {
    // Error already shown by interceptor
  } finally {
    batchSaving.value = false
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.program-title-link {
  color: var(--c-primary);
  text-decoration: none;
  font-weight: 500;
  cursor: pointer;
}

.program-title-link:hover {
  text-decoration: underline;
}

.text-muted {
  color: var(--c-text-muted);
}
</style>

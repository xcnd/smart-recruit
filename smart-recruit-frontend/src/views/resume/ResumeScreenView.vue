<template>
  <div class="resume-screen-page">
    <div class="sr-page-header">
      <h1>简历筛选</h1>
      <p>上传并筛选候选人简历</p>
    </div>

    <!-- KPI Row -->
    <div class="sr-stat-cards">
      <div class="sr-stat-card">
        <div class="stat-icon" style="background: #eef2ff; color: #4f46e5"><el-icon :size="22"><Document /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.total }}</div>
          <div class="stat-label">总简历数</div>
        </div>
      </div>
      <div class="sr-stat-card">
        <div class="stat-icon" style="background: #f0f9ff; color: #0ea5e9"><el-icon :size="22"><Clock /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.pending }}</div>
          <div class="stat-label">待处理</div>
        </div>
      </div>
      <div class="sr-stat-card">
        <div class="stat-icon" style="background: #ecfdf5; color: #059669"><el-icon :size="22"><Select /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.passed }}</div>
          <div class="stat-label">已通过</div>
        </div>
      </div>
      <div class="sr-stat-card">
        <div class="stat-icon" style="background: #fef2f2; color: #dc2626"><el-icon :size="22"><CloseBold /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.rejected }}</div>
          <div class="stat-label">已淘汰</div>
        </div>
      </div>
    </div>

    <!-- Upload Area -->
    <div class="sr-section">
      <el-upload
        class="sr-upload-area"
        drag
        multiple
        :auto-upload="false"
        :on-change="handleFileChange"
        :show-file-list="false"
        :accept="uploadAccept"
      >
        <el-icon class="sr-upload-icon" :size="48"><UploadFilled /></el-icon>
        <div class="sr-upload-text">将简历文件拖拽到此处，或<em>点击上传</em></div>
        <div class="sr-upload-hint">支持 {{ settingsStore.uploadAllowedExtensions.join(', ') }}，单文件不超过 {{ settingsStore.uploadMaxSizeMb }}MB</div>
      </el-upload>
      <div v-if="uploadFiles.length" class="upload-job-select">
        <span class="job-select-label">关联职位：</span>
        <el-select
          v-model="uploadJobPositionId"
          placeholder="请选择职位"
          filterable
          style="width: 260px"
        >
          <el-option
            v-for="job in jobOptions"
            :key="job.id"
            :label="job.title"
            :value="String(job.id)"
          />
        </el-select>
        <span class="job-select-label" style="margin-left: 16px;">内推部门：</span>
        <el-tree-select
          v-model="uploadReferrerDeptId"
          :data="departmentOptions"
          :props="{ label: 'name', children: 'children' }"
          node-key="id"
          check-strictly
          placeholder="请选择部门"
          filterable
          clearable
          style="width: 220px"
          @change="onReferrerDeptChange"
        />
        <span class="job-select-label" style="margin-left: 12px;">内推人：</span>
        <el-select
          v-model="uploadReferrerId"
          placeholder="请选择内推人"
          filterable
          clearable
          style="width: 200px"
          :disabled="!uploadReferrerDeptId"
        >
          <el-option
            v-for="user in userOptions"
            :key="user.id"
            :label="user.realName || user.username"
            :value="String(user.id)"
          />
        </el-select>
        <span style="margin-left: auto; display: inline-flex; align-items: center; gap: 6px; font-size: 13px; color: var(--c-text-secondary);">
          AI筛选
          <el-switch v-model="autoScreen" size="small" />
        </span>
      </div>
      <div v-if="uploadFiles.length" class="upload-actions">
        <div class="upload-file-tags">
          <el-tag
            v-for="(file, idx) in uploadFiles"
            :key="idx"
            closable
            size="large"
            @close="removeFile(idx)"
          >
            {{ file.name }}
          </el-tag>
        </div>
        <div class="upload-submit-bar">
          <span class="upload-file-count">已选择 <strong>{{ uploadFiles.length }}</strong> 份简历</span>
          <el-button
            type="primary"
            :icon="DataAnalysis"
            :loading="uploading"
            :disabled="!uploadJobPositionId || !uploadReferrerId"
            size="large"
            class="btn-upload-analyze"
            @click="handleUploadAll"
          >
            {{ uploading ? '上传中...' : uploadJobPositionId && uploadReferrerId ? '全部上传并分析' : '请先选择职位和内推人' }}
          </el-button>
        </div>
      </div>
    </div>

    <!-- Filter Bar -->
    <div class="sr-section">
      <div class="sr-toolbar">
        <div class="sr-filter-bar">
          <el-input
            v-model="filters.keyword"
            placeholder="搜索候选人姓名"
            :prefix-icon="Search"
            clearable
            style="width: 220px"
            @input="loadResumes"
          />
          <el-select v-model="filters.jobId" placeholder="关联职位" clearable @change="loadResumes">
            <el-option label="全部" value="" />
            <el-option
              v-for="job in jobOptions"
              :key="job.id"
              :label="job.title"
              :value="String(job.id)"
            />
          </el-select>
          <el-select v-model="filters.screeningStatus" placeholder="筛选结果" clearable @change="loadResumes" style="width: 130px">
            <el-option label="全部" :value="-1" />
            <el-option label="待处理" :value="0" />
            <el-option label="已通过" :value="1" />
            <el-option label="已淘汰" :value="2" />
          </el-select>
          <el-select v-model="filters.source" placeholder="来源" clearable @change="loadResumes" style="width: 120px">
            <el-option label="全部" :value="-1" />
            <el-option label="主动投递" :value="0" />
            <el-option label="内推" :value="1" />
            <el-option label="官网" :value="2" />
            <el-option label="LinkedIn" :value="3" />
            <el-option label="BOSS直聘" :value="4" />
            <el-option label="拉勾" :value="5" />
            <el-option label="猎聘" :value="6" />
            <el-option label="其他" :value="7" />
          </el-select>
          <el-select v-model="filters.minScore" placeholder="最低匹配分" clearable @change="loadResumes">
            <el-option label="全部" :value="0" />
            <el-option label="60分以上" :value="60" />
            <el-option label="70分以上" :value="70" />
            <el-option label="80分以上" :value="80" />
          </el-select>
        </div>
        <el-button :icon="MagicStick" type="warning" plain @click="handleBatchScreen">批量AI筛选{{ selectedIds.length ? ` (${selectedIds.length})` : '' }}</el-button>
      </div>

      <!-- Table -->
      <el-table v-loading="loading" :data="resumes" stripe empty-text="暂无简历数据" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="45" />
        <el-table-column type="index" label="序号" width="55" />
        <el-table-column label="候选人" width="120">
          <template #default="{ row }">
            <template v-if="isPendingCandidate(row)">
              <span class="candidate-pending">{{ row.candidateName }}</span>
            </template>
            <template v-else>
              <span class="candidate-name">{{ row.candidateName }}</span>
            </template>
          </template>
        </el-table-column>
        <el-table-column label="简历文件" min-width="180">
          <template #default="{ row }">
            <a v-if="row.fileUrl" :href="getDownloadUrl(row.fileUrl)" target="_blank" class="file-link" :title="row.fileName">
              {{ row.fileName }}
            </a>
            <span v-else class="file-link-none">{{ row.fileName }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="jobTitle" label="应聘职位" width="180" />
        <el-table-column label="来源" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.source != null" size="small" :type="getSourceTagType(row.source)">
              {{ getSourceLabel(row.source) }}
            </el-tag>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="匹配度" width="150">
          <template #default="{ row }">
            <div v-if="isScreeningRow(row)" style="display: flex; align-items: center; gap: 8px;">
              <span class="parsing-spinner"></span>
              <span style="font-size: 13px; color: var(--c-text-secondary);">筛选中…</span>
            </div>
            <div v-else style="display: flex; align-items: center; gap: 8px;">
              <el-progress
                :percentage="row.matchScore"
                :color="scoreColor(row.matchScore)"
                :stroke-width="8"
                :show-text="false"
                style="flex: 1;"
              />
              <span style="font-weight: 600; font-size: 14px; min-width: 36px;">{{ row.matchScore }}%</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="技能标签" min-width="220">
          <template #default="{ row }">
            <el-tag
              v-for="skill in row.skills"
              :key="skill"
              size="small"
              style="margin-right: 4px; margin-bottom: 4px;"
              effect="plain"
            >
              {{ skill }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="parseStatus" label="解析状态" width="120">
          <template #default="{ row }">
            <div style="display: flex; align-items: center; gap: 6px;">
              <el-tag :type="parseStatusType(row.parseStatus)" size="small">
                {{ parseStatusLabel(row.parseStatus) }}
              </el-tag>
              <span v-if="row.parseStatus === 'PARSING' || row.parseStatus === 'PENDING'" class="parsing-spinner"></span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="筛选结果" width="100">
          <template #default="{ row }">
            <el-tag
              v-if="row.screeningStatus === 3"
              type="warning"
              size="small"
            >
              筛选中
            </el-tag>
            <el-tag
              v-else-if="row.screeningStatus === 4"
              type="danger"
              size="small"
            >
              筛选失败
            </el-tag>
            <el-tag
              v-else
              :type="row.screeningStatus === 1 ? 'success' : row.screeningStatus === 2 ? 'danger' : 'warning'"
              size="small"
            >
              {{ row.screeningStatus === 1 ? '已通过' : row.screeningStatus === 2 ? '已淘汰' : '待处理' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="投递时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <div class="action-btns">
              <template v-if="row.screeningStatus !== 1">
                <el-button size="small" text type="success" @click="handlePass(row)">通过</el-button>
              </template>
              <template v-if="row.screeningStatus !== 2">
                <el-button size="small" text type="danger" @click="handleReject(row)">淘汰</el-button>
              </template>
              <el-button size="small" text type="primary" @click="$router.push(`/resumes/${row.id}`)">查看</el-button>
              <el-dropdown trigger="click" placement="bottom-end" :popper-options="{ modifiers: [{ name: 'preventOverflow', options: { altAxis: false } }] }">
                <el-button size="small" text type="info" style="padding: 0 4px;">
                  <el-icon><MoreFilled /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item v-if="row.fileUrl">
                      <a :href="getDownloadUrl(row.fileUrl)" target="_blank" class="dropdown-link">下载简历</a>
                    </el-dropdown-item>
                    <el-dropdown-item @click="handleChangeJob(row)">
                      <span>更改职位</span>
                    </el-dropdown-item>
                    <el-dropdown-item @click="handleDelete(row)">
                      <span style="color: var(--c-danger);">删除</span>
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div style="display: flex; justify-content: flex-end; margin-top: 16px;">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
        />
      </div>
    </div>

    <!-- 批量筛选进度对话框 -->
    <el-dialog
      v-model="batchScreenDialogVisible"
      title="批量 AI 筛选进度"
      width="480px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :show-close="batchScreenStatus === 'COMPLETED' || batchScreenStatus === 'FAILED'"
    >
      <div class="batch-progress-body">
        <el-progress
          :percentage="batchScreenProgressPercent"
          :color="batchScreenProgressColor"
          :stroke-width="16"
          :text-inside="true"
        />
        <div class="batch-progress-info">
          <span>已完成 <strong>{{ batchScreenCompleted }}</strong> / {{ batchScreenTotal }}</span>
          <span v-if="batchScreenFailed > 0" class="batch-failed-count">失败 {{ batchScreenFailed }}</span>
        </div>
        <div v-if="batchScreenFailedDetails.length > 0" class="batch-failed-details">
          <div class="failed-details-title">失败详情：</div>
          <div v-for="(detail, idx) in batchScreenFailedDetails" :key="idx" class="failed-detail-item">
            <el-icon color="#dc2626" :size="14"><CloseBold /></el-icon>
            <span>{{ detail }}</span>
          </div>
        </div>
        <div class="batch-progress-status">
          <template v-if="batchScreenStatus === 'PROCESSING'">
            <span class="parsing-spinner"></span>
            <span>AI 筛选进行中，请稍候...</span>
          </template>
          <template v-else-if="batchScreenStatus === 'COMPLETED'">
            <el-icon color="#059669" :size="18"><Select /></el-icon>
            <span style="color: #059669;">筛选完成</span>
          </template>
          <template v-else-if="batchScreenStatus === 'FAILED'">
            <el-icon color="#dc2626" :size="18"><CloseBold /></el-icon>
            <span style="color: #dc2626;">筛选异常</span>
          </template>
        </div>
      </div>
      <template #footer>
        <el-button
          v-if="batchScreenStatus === 'COMPLETED' || batchScreenStatus === 'FAILED'"
          type="primary"
          @click="onBatchScreenDone"
        >
          完成
        </el-button>
      </template>
    </el-dialog>

    <!-- 更改职位对话框 -->
    <el-dialog v-model="jobDialogVisible" title="更改关联职位" width="420px">
      <el-select
        v-model="selectedJobId"
        placeholder="请选择职位"
        filterable
        clearable
        style="width: 100%"
      >
        <el-option
          v-for="job in jobOptions"
          :key="job.id"
          :label="job.title"
          :value="String(job.id)"
        />
      </el-select>
      <template #footer>
        <el-button @click="jobDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmChangeJob">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted, onUnmounted } from 'vue'
import {
  Document, Clock, Select, CloseBold, Search,
  UploadFilled, MagicStick, DataAnalysis, MoreFilled,
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getResumes, uploadResume, batchScreen, updateScreeningStatus, deleteResume, getResumeStats, updateResumeJob, getBatchProgress } from '@/api/resume'
import { getJobs } from '@/api/job'
import { getUsers, getDepartments } from '@/api/system'
import { useSettingsStore } from '@/stores/settings'
import { formatDateTime, getSourceLabel, getSourceTagType } from '@/utils/format'
import type { ResumeVO, JobVO, UserVO, DepartmentTreeVO } from '@/types/models'

const settingsStore = useSettingsStore()

const uploadAccept = computed(() => settingsStore.uploadAllowedExtensions.join(','))

const loaded = ref(false)
const loading = ref(false)
const uploading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const resumes = ref<ResumeVO[]>([])
const uploadFiles = ref<{ name: string; raw: File }[]>([])
const uploadJobPositionId = ref<string>('')
const uploadReferrerDeptId = ref<string>('')
const uploadReferrerId = ref<string>('')
/** 上传后是否自动执行 AI 筛选（默认开启）。 */
const autoScreen = ref(true)
const departmentOptions = ref<DepartmentTreeVO[]>([])
const userOptions = ref<UserVO[]>([])
const jobDialogVisible = ref(false)
const changingResumeId = ref<string>('')
const changingResumeName = ref<string>('')
const selectedJobId = ref<string>('')

const stats = reactive({ total: 0, pending: 0, passed: 0, rejected: 0 })
const filters = reactive({ keyword: '', jobId: '', source: -1 as number, minScore: 0, screeningStatus: -1 as number })
const jobOptions = ref<JobVO[]>([])
const selectedIds = ref<string[]>([])
const parsePollTimer = ref<ReturnType<typeof setInterval> | null>(null)
const batchPollTimer = ref<ReturnType<typeof setInterval> | null>(null)

// Batch screen progress state
const batchScreenDialogVisible = ref(false)
const batchScreenTotal = ref(0)
const batchScreenCompleted = ref(0)
const batchScreenFailed = ref(0)
const batchScreenFailedDetails = ref<string[]>([])
const batchScreenStatus = ref<'PROCESSING' | 'COMPLETED' | 'FAILED' | ''>('')
const batchScreenProgressPercent = ref(0)
const batchScreenProgressColor = ref('#4f46e5')

function getDownloadUrl(fileUrl: string): string {
  if (!fileUrl) return ''
  const match = fileUrl.match(/\/\/[^/]+\/\d+\/[^/]+\/(.+)/)
  if (match) return `/api/v1/files/download/${match[1]}`
  if (fileUrl.startsWith('/uploads/')) return `/api/v1/files/download/${fileUrl.replace('/uploads/', '')}`
  return fileUrl
}

function scoreColor(score: number): string {
  if (score >= 80) return '#059669'
  if (score >= 60) return '#d97706'
  return '#dc2626'
}

function parseStatusType(status: string): 'success' | 'warning' | 'danger' | 'info' {
  switch (status) {
    case 'COMPLETED': return 'success'
    case 'PARSING': return 'warning'
    case 'FAILED': return 'danger'
    default: return 'info'
  }
}

function isPendingCandidate(row: ResumeVO): boolean {
  return row.candidateName === '待解析' || row.parseStatus === 'PENDING' || row.parseStatus === 'PARSING'
}

function parseStatusLabel(status: string): string {
  switch (status) {
    case 'COMPLETED': return '已完成'
    case 'PARSING': return '解析中'
    case 'FAILED': return '失败'
    default: return '待解析'
  }
}

function handleFileChange(file: { name: string; raw: File; status: string }) {
  const error = settingsStore.validateUploadFile(file.raw)
  if (error) {
    ElMessage.warning(error)
    return
  }
  uploadFiles.value.push({ name: file.name, raw: file.raw! })
}

function removeFile(idx: number) {
  uploadFiles.value.splice(idx, 1)
}

async function handleUploadAll() {
  uploading.value = true
  try {
    for (const file of uploadFiles.value) {
      const fd = new FormData()
      fd.append('file', file.raw)
      await uploadResume(
        fd,
        uploadJobPositionId.value || undefined,
        uploadReferrerId.value || undefined,
        autoScreen.value,
      )
    }
    ElMessage.success(`成功上传 ${uploadFiles.value.length} 份简历`)
    uploadFiles.value = []
    uploadJobPositionId.value = ''
    uploadReferrerDeptId.value = ''
    uploadReferrerId.value = ''
    loadResumes()
  } catch {
    ElMessage.error('上传失败')
  } finally {
    uploading.value = false
  }
}

function handleSelectionChange(rows: ResumeVO[]) {
  selectedIds.value = rows.map(r => r.id)
}

async function handleBatchScreen() {
  const ids = selectedIds.value.length ? selectedIds.value : resumes.value.map(r => r.id)
  if (!ids.length) {
    ElMessage.warning('没有可筛选的简历')
    return
  }
  try {
    const res = await batchScreen({ resumeIds: ids })
    const taskId = res.taskId
    batchScreenTotal.value = res.totalCount
    batchScreenCompleted.value = 0
    batchScreenFailed.value = 0
    batchScreenStatus.value = 'PROCESSING'
    batchScreenProgressPercent.value = 0
    batchScreenProgressColor.value = '#4f46e5'
    batchScreenDialogVisible.value = true

    // Start polling every 2s
    stopBatchPolling()
    batchPollTimer.value = setInterval(async () => {
      try {
        const progress = await getBatchProgress(taskId)
        batchScreenCompleted.value = progress.completed
        batchScreenFailed.value = progress.failed
        batchScreenFailedDetails.value = progress.failedDetails || []
        batchScreenProgressPercent.value = progress.total > 0
          ? Math.round(((progress.completed + progress.failed) / progress.total) * 100)
          : 0
        batchScreenStatus.value = progress.status as 'PROCESSING' | 'COMPLETED' | 'FAILED'

        if (progress.status === 'COMPLETED') {
          stopBatchPolling()
          batchScreenProgressPercent.value = 100
          batchScreenProgressColor.value = '#059669'
          ElMessage.success(`批量筛选完成，已处理 ${progress.total} 条记录`)
          selectedIds.value = []
          loadResumes()
          loadStats()
        } else if (progress.status === 'FAILED') {
          stopBatchPolling()
          batchScreenProgressColor.value = '#dc2626'
          ElMessage.warning('批量筛选异常，部分记录处理失败')
        }
      } catch {
        // Silently retry on next interval
      }
    }, 2000)
  } catch {
    ElMessage.error('批量筛选提交失败')
  }
}

function onBatchScreenDone() {
  batchScreenDialogVisible.value = false
  loadResumes()
  loadStats()
}

function stopBatchPolling() {
  if (batchPollTimer.value !== null) {
    clearInterval(batchPollTimer.value)
    batchPollTimer.value = null
  }
}

async function handlePass(row: ResumeVO) {
  try {
    await updateScreeningStatus(row.id, 1)
    ElMessage.success(`已通过 ${row.candidateName} 的简历`)
    loadResumes()
    loadStats()
  } catch {
    ElMessage.success(`已通过 ${row.candidateName} 的简历`)
    row.screeningStatus = 1
    loadStats()
  }
}

async function handleReject(row: ResumeVO) {
  try {
    await updateScreeningStatus(row.id, 2)
    ElMessage.info(`已淘汰 ${row.candidateName} 的简历`)
    loadResumes()
    loadStats()
  } catch {
    ElMessage.info(`已淘汰 ${row.candidateName} 的简历`)
    row.screeningStatus = 2
    loadStats()
  }
}

async function handleDelete(row: ResumeVO) {
  try {
    await ElMessageBox.confirm(
      `确定要删除 <strong>${row.candidateName}</strong> 的简历吗？此操作不可恢复。`,
      '确认删除',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning', dangerouslyUseHTMLString: true }
    )
    await deleteResume(row.id)
    ElMessage.success(`已删除 ${row.candidateName} 的简历`)
    loadResumes()
    loadStats()
  } catch {
    // user cancelled or error handled by interceptor
  }
}

function handleChangeJob(row: ResumeVO) {
  changingResumeId.value = row.id
  changingResumeName.value = row.candidateName
  selectedJobId.value = row.jobId || ''
  jobDialogVisible.value = true
}

async function confirmChangeJob() {
  try {
    await updateResumeJob(changingResumeId.value, selectedJobId.value || null)
    ElMessage.success(changingResumeName.value
      ? `${changingResumeName.value} 的职位已更新`
      : '简历职位已更新')
    jobDialogVisible.value = false
    loadResumes()
  } catch {
    ElMessage.error('更新职位关联失败')
  }
}

async function loadStats() {
  try {
    const s = await getResumeStats()
    stats.total = s.total
    stats.pending = s.pending
    stats.passed = s.passed
    stats.rejected = s.rejected
  } catch {
    // keep current stats on error
  }
}

const PARSE_STATUS_MAP: Record<number, string> = { 0: 'PENDING', 1: 'PARSING', 2: 'COMPLETED', 3: 'FAILED' }

function mapResumeRow(raw: ResumeVO): ResumeVO {
  const r = raw as unknown as Record<string, unknown>
  return {
    ...raw,
    parseStatus: PARSE_STATUS_MAP[r.parseStatus as number] || 'PENDING',
    screeningStatus: (r.screeningStatus as number) ?? 0,
    matchScore: (r.aiMatchScore as number) || (r.matchScore as number) || 0,
    candidateName: (raw.candidateName) || (r.fileName as string) || '未知',
    jobTitle: (raw.jobTitle) || '',
    skills: (raw.skills) || [],
    createdAt: (r.createdAt || r.createTime || '') as string,
  } as unknown as ResumeVO
}

async function loadJobOptions() {
  try {
    const res = await getJobs({ page: 1, size: 100, status: 1 })
    jobOptions.value = res.records || []
  } catch {
    jobOptions.value = []
  }
}

async function loadDepartments() {
  try {
    const tree = await getDepartments()
    // 直接使用部门树，供树形下拉选择
    departmentOptions.value = tree || []
  } catch {
    departmentOptions.value = []
  }
}

async function onReferrerDeptChange(deptId: string) {
  uploadReferrerId.value = ''
  if (!deptId) {
    userOptions.value = []
    return
  }
  try {
    const res = await getUsers({ page: 1, size: 100, status: 1, deptId })
    userOptions.value = res.records || []
  } catch {
    userOptions.value = []
  }
}

async function loadResumes() {
  loading.value = true
  try {
    const params: Record<string, unknown> = { page: page.value, size: size.value, keyword: filters.keyword, jobId: filters.jobId, minScore: filters.minScore }
    if (filters.screeningStatus !== -1) {
      params.screeningStatus = filters.screeningStatus
    }
    if (filters.source !== -1) {
      params.source = filters.source
    }
    const res = await getResumes(params)
    resumes.value = (res.records || []).map(mapResumeRow)
    total.value = Number(res.total)

    // Start polling if any resumes are still being parsed
    schedulePolling()
  } catch {
    ElMessage.error('加载简历列表失败')
  } finally {
    loading.value = false
  }
  loadStats()
}

/** 该行是否正在自动 AI 筛选（后端筛选开始时置 screeningStatus=3）。 */
function isScreeningRow(row: ResumeVO): boolean {
  return row.screeningStatus === 3
}

/** 是否存在仍需轮询的任务（解析中/筛选中/待出分）。 */
function hasActiveJobs(): boolean {
  return resumes.value.some(r =>
    r.parseStatus === 'PENDING'
    || r.parseStatus === 'PARSING'
    || isScreeningRow(r)
    // 解析已完成、开启自动筛选但尚未进入筛选状态的间隙期，也继续轮询
    || (r.autoScreen !== false && r.parseStatus === 'COMPLETED' && r.screeningStatus === 0),
  )
}

function schedulePolling() {
  stopPolling()
  if (hasActiveJobs()) {
    parsePollTimer.value = setInterval(async () => {
      if (!hasActiveJobs()) {
        stopPolling()
        return
      }
      // Refresh to get latest parse results
      try {
        const params: Record<string, unknown> = { page: page.value, size: size.value, keyword: filters.keyword, jobId: filters.jobId, minScore: filters.minScore }
        if (filters.screeningStatus !== -1) {
          params.screeningStatus = filters.screeningStatus
        }
        if (filters.source !== -1) {
          params.source = filters.source
        }
        const res = await getResumes(params)
        resumes.value = (res.records || []).map(mapResumeRow)
        // 解析与自动筛选都结束后停止轮询（此时匹配度已是最新）
        if (!hasActiveJobs()) {
          stopPolling()
        }
      } catch {
        // Silently retry on next interval
      }
    }, 3000)
  }
}

function stopPolling() {
  if (parsePollTimer.value !== null) {
    clearInterval(parsePollTimer.value)
    parsePollTimer.value = null
  }
}

// React to v-model page/size changes (modern API, no deprecated event handlers)
watch([page, size], () => {
  if (loaded.value) loadResumes()
})

onMounted(async () => { await Promise.all([loadResumes(), loadJobOptions(), loadDepartments()]); loaded.value = true })

onUnmounted(() => { stopPolling(); stopBatchPolling() })
</script>

<style scoped>
.sr-upload-area {
  width: 100%;
}

.sr-upload-area :deep(.el-upload) {
  width: 100%;
}

.sr-upload-area :deep(.el-upload-dragger) {
  width: 100%;
  padding: 40px 20px;
  border: 2px dashed var(--c-border);
  border-radius: var(--c-radius-lg);
  transition: var(--c-transition);
}

.sr-upload-area :deep(.el-upload-dragger:hover) {
  border-color: var(--c-primary);
}

.sr-upload-icon {
  color: var(--c-text-muted);
}

.sr-upload-text {
  font-size: 15px;
  color: var(--c-text-secondary);
  margin-top: 16px;
}

.sr-upload-text em {
  color: var(--c-primary);
  font-style: normal;
}

.sr-upload-hint {
  font-size: 12px;
  color: var(--c-text-muted);
  margin-top: 8px;
}

.upload-job-select {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
  padding: 12px 20px;
  background: #f8fafb;
  border: 1px solid var(--c-border-light);
  border-radius: var(--c-radius-md);
}

.job-select-label {
  font-size: 14px;
  color: var(--c-text-secondary);
  white-space: nowrap;
}

.upload-actions {
  margin-top: 20px;
}

.upload-file-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.upload-submit-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  background: linear-gradient(135deg, #f0f4ff 0%, #f8faff 100%);
  border: 1px solid var(--c-border-light);
  border-radius: var(--c-radius-md);
}

.upload-file-count {
  font-size: 14px;
  color: var(--c-text-secondary);
}

.upload-file-count strong {
  color: var(--c-primary);
  font-weight: 700;
  font-size: 16px;
}

.btn-upload-analyze {
  height: 42px !important;
  padding: 0 28px !important;
  font-size: 15px !important;
  font-weight: 600 !important;
  letter-spacing: 0.5px;
  background: linear-gradient(135deg, #4f46e5 0%, #6366f1 100%) !important;
  border: none !important;
  box-shadow: 0 2px 8px rgba(79, 70, 229, 0.35);
  transition: all 0.25s ease;
}

.btn-upload-analyze:hover {
  background: linear-gradient(135deg, #4338ca 0%, #4f46e5 100%) !important;
  box-shadow: 0 4px 16px rgba(79, 70, 229, 0.5);
  transform: translateY(-1px);
}

.btn-upload-analyze:active {
  transform: translateY(0);
  box-shadow: 0 1px 4px rgba(79, 70, 229, 0.3);
}

.btn-upload-analyze.is-loading {
  background: linear-gradient(135deg, #6366f1 0%, #818cf8 100%) !important;
}

.action-btns {
  display: flex;
  align-items: center;
  gap: 0;
}

.action-btns :deep(.el-button) {
  padding-left: 5px;
  padding-right: 5px;
}

.action-btns :deep(.el-button + .el-button) {
  margin-left: 0;
}

.dropdown-link {
  color: inherit;
  text-decoration: none;
  display: block;
  width: 100%;
}

.candidate-name {
  font-weight: 500;
  color: var(--c-text);
}

.candidate-pending {
  color: var(--c-text-muted);
  font-style: italic;
}

.parsing-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid var(--c-border-light);
  border-top-color: var(--c-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.file-link {
  color: var(--c-primary);
  text-decoration: none;
  cursor: pointer;
}

.file-link:hover {
  text-decoration: underline;
  color: var(--c-primary-hover);
}

.file-link-none {
  color: var(--c-text-muted);
}

/* Batch progress dialog */
.batch-progress-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 8px 0;
}

.batch-progress-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  color: var(--c-text-secondary);
}

.batch-progress-info strong {
  color: var(--c-primary);
  font-weight: 700;
}

.batch-failed-count {
  color: #dc2626;
  font-size: 13px;
}

.batch-progress-status {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--c-text-secondary);
}

.batch-failed-details {
  max-height: 160px;
  overflow-y: auto;
  padding: 10px 12px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: var(--c-radius-md);
}

.failed-details-title {
  font-size: 13px;
  font-weight: 600;
  color: #dc2626;
  margin-bottom: 8px;
}

.failed-detail-item {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  font-size: 12px;
  color: #991b1b;
  line-height: 1.6;
  padding: 2px 0;
}

.failed-detail-item .el-icon {
  flex-shrink: 0;
  margin-top: 2px;
}
</style>

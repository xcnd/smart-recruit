<template>
  <div class="tasks-page">
    <div class="sr-page-header">
      <h1>待办事项</h1>
      <p>管理和处理所有待办任务，支持按类型、优先级、状态和关键字筛选。</p>
    </div>

    <!-- Filters -->
    <div class="filter-bar">
      <el-select v-model="filterType" placeholder="全部类型" clearable style="width: 140px" @change="search">
        <el-option v-for="opt in typeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
      <el-select v-model="filterPriority" placeholder="全部优先级" clearable style="width: 140px" @change="search">
        <el-option v-for="opt in priorityOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
      <el-select v-model="filterStatus" placeholder="全部状态" clearable style="width: 140px" @change="search">
        <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
      <el-input
        v-model="keyword"
        placeholder="搜索任务..."
        clearable
        style="width: 240px"
        @keyup.enter="search"
        @clear="search"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-button type="primary" @click="search">搜索</el-button>
    </div>

    <!-- List -->
    <div class="sr-section" v-loading="loading">
      <div v-if="tasks.length === 0 && !loading" class="empty-state">
        <el-empty description="暂无待办事项" />
      </div>
      <div
        v-for="(task, index) in tasks"
        :key="task.id"
        class="task-item"
        :class="{ 'is-clickable': task.relatedType && task.relatedId }"
        @click="navigateToDetail(task)"
      >
        <span class="task-index">{{ (page - 1) * size + index + 1 }}</span>
        <div class="task-type-icon" :class="getTaskClass(task.type ?? task.category ?? 5)">
          <el-icon :size="14"><component :is="getTaskIcon(task.type ?? task.category ?? 5)" /></el-icon>
        </div>
        <div class="task-content">
          <div class="task-header-line">
            <span class="activity-type-tag" :class="getTaskClass(task.type ?? task.category ?? 5)">
              {{ getTaskLabel(task.type ?? task.category ?? 5) }}
            </span>
            <div class="task-right-tags">
              <el-tag :type="getPriorityTagType(task.priority ?? task.urgency ?? 1)" size="small">
                {{ getPriorityLabel(task.priority ?? task.urgency ?? 1) }}
              </el-tag>
              <el-tag :type="getStatusTagType(0)" size="small" v-if="!(filterStatus !== undefined && filterStatus !== 0)">
                待办
              </el-tag>
              <span class="activity-time task-time" v-if="task.dueDate || task.scheduledAt">
                <el-icon :size="10"><Clock /></el-icon>
                {{ formatTimeAgo(task.dueDate) || task.scheduledAt }}
              </span>
            </div>
          </div>
          <div class="task-title">{{ task.title }}</div>
          <div class="task-meta" v-if="task.relatedPerson || task.candidateName">
            <el-icon :size="12"><UserFilled /></el-icon>
            <span>{{ task.relatedPerson || task.candidateName }}</span>
          </div>
          <div class="activity-desc" v-if="task.description">{{ task.description }}</div>
        </div>
        <div class="task-actions">
          <el-button size="small" type="success" :icon="Check" circle @click.stop="handleComplete(task)" />
          <el-button size="small" type="info" :icon="Close" circle @click.stop="handleDismiss(task)" />
        </div>
      </div>
    </div>

    <!-- Pagination -->
    <div class="sr-pagination-footer" v-if="total > 0">
      <span class="sr-pagination-info">共 <strong>{{ total }}</strong> 条待办</span>
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @size-change="loadPage"
        @current-change="loadPage"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Clock } from '@element-plus/icons-vue'
import {
  DocumentCopy, VideoCamera, Tickets, Van, CircleCheck, Promotion, UserFilled, Check, Close,
} from '@element-plus/icons-vue'
import { getTasksPage, completeTask as completeTaskApi, dismissTask as dismissTaskApi } from '@/api/dashboard'
import type { PendingTaskVO } from '@/types/models'
import { ElMessage } from 'element-plus'

const router = useRouter()
const loading = ref(false)
const tasks = ref<PendingTaskVO[]>([])
const page = ref(1)
const size = ref(20)
const total = ref(0)
const filterType = ref<number | undefined>(undefined)
const filterPriority = ref<number | undefined>(undefined)
const filterStatus = ref<number | undefined>(0)
const keyword = ref('')

const typeOptions = [
  { value: 0, label: '筛选简历' },
  { value: 1, label: '安排面试' },
  { value: 2, label: '审批Offer' },
  { value: 3, label: '办理入职' },
  { value: 4, label: '反馈评审' },
  { value: 5, label: '其他' },
]

const priorityOptions = [
  { value: 0, label: '高' },
  { value: 1, label: '中' },
  { value: 2, label: '低' },
]

const statusOptions = [
  { value: 0, label: '待办' },
  { value: 1, label: '已完成' },
  { value: 2, label: '已忽略' },
]

function getTaskIcon(type: number) {
  const map: Record<number, unknown> = {
    0: DocumentCopy, 1: VideoCamera, 2: Tickets,
    3: Van, 4: CircleCheck, 5: Promotion,
  }
  return map[type] || Promotion
}

function getTaskClass(type: number): string {
  const map: Record<number, string> = {
    0: 'APPLICATION', 1: 'INTERVIEW', 2: 'OFFER',
    3: 'ONBOARDING', 4: 'APPLICATION', 5: 'REFERRAL',
  }
  return map[type] || 'REFERRAL'
}

function getTaskLabel(type: number): string {
  const map: Record<number, string> = {
    0: '筛选简历', 1: '安排面试', 2: '审批Offer',
    3: '办理入职', 4: '反馈评审', 5: '其他',
  }
  return map[type] || '待办'
}

function getPriorityLabel(p: number): string {
  return p === 0 ? '高' : p === 1 ? '中' : '低'
}

function getPriorityTagType(p: number): 'danger' | 'warning' | 'info' {
  return p === 0 ? 'danger' : p === 1 ? 'warning' : 'info'
}

function getStatusTagType(s: number): 'success' | 'info' | 'danger' {
  return s === 0 ? 'info' : s === 1 ? 'success' : 'danger'
}

function formatTimeAgo(dateStr: string | null): string {
  if (!dateStr) return ''
  const now = Date.now()
  const then = new Date(dateStr).getTime()
  const diff = now - then
  if (diff < 0) return '刚刚'
  const seconds = Math.floor(diff / 1000)
  if (seconds < 5) return '刚刚'
  if (seconds < 60) return `${seconds}秒前`
  const minutes = Math.floor(seconds / 60)
  if (minutes < 60) return `${minutes}分钟前`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}小时前`
  const days = Math.floor(hours / 24)
  if (days < 30) return `${days}天前`
  const months = Math.floor(days / 30)
  return `${months}个月前`
}

function navigateToDetail(item: { relatedType?: string | null; relatedId?: number | string | null }) {
  const type = item.relatedType
  const id = item.relatedId
  if (!type || !id) return
  const routeMap: Record<string, string> = {
    CANDIDATE: `/candidates/${id}`,
    JOB: `/jobs/${id}`,
    APPLICATION: `/candidates/${id}`,
    INTERVIEW: `/interviews/${id}/report`,
    OFFER: `/offers/${id}`,
    ONBOARDING: `/onboarding/${id}`,
  }
  const path = routeMap[type.toUpperCase()]
  if (path) router.push(path)
}

async function loadPage() {
  loading.value = true
  try {
    const result = await getTasksPage({
      page: page.value,
      size: size.value,
      type: filterType.value,
      priority: filterPriority.value,
      status: filterStatus.value,
      keyword: keyword.value || undefined,
    })
    tasks.value = result.records
    total.value = result.total
  } catch {
    tasks.value = []
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  loadPage()
}

async function handleComplete(task: PendingTaskVO) {
  try {
    await completeTaskApi(task.id)
    ElMessage.success('任务已完成')
    loadPage()
  } catch {
    ElMessage.error('操作失败')
  }
}

async function handleDismiss(task: PendingTaskVO) {
  try {
    await dismissTaskApi(task.id)
    ElMessage.success('任务已忽略')
    loadPage()
  } catch {
    ElMessage.error('操作失败')
  }
}

onMounted(() => loadPage())
</script>

<style scoped>
.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  align-items: center;
}

.empty-state {
  padding: 40px 0;
}

.sr-pagination-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16px;
  padding: 12px 0 0;
  border-top: 1px solid var(--c-border);
}

.sr-pagination-info {
  font-size: 13px;
  color: var(--c-text-secondary);
}

.task-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 0;
  border-bottom: 1px solid var(--c-border-light);
}

.task-item.is-clickable {
  cursor: pointer;
  border-radius: var(--c-radius-sm);
  transition: background 0.15s;
}

.task-item.is-clickable:hover {
  background: var(--c-bg-hover, #f5f7fa);
}

.task-item:last-child { border-bottom: none; }

.task-index {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 12px;
  color: var(--c-text-disabled);
  margin-top: 2px;
}

.task-type-icon {
  width: 32px;
  height: 32px;
  border-radius: var(--c-radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: white;
  margin-top: 2px;
}

.task-type-icon.APPLICATION { background: var(--c-info); }
.task-type-icon.INTERVIEW { background: var(--c-warning); }
.task-type-icon.OFFER { background: var(--c-success); }
.task-type-icon.ONBOARDING { background: var(--c-primary); }
.task-type-icon.REFERRAL { background: #f59e0b; }

.task-content { flex: 1; min-width: 0; }

.task-header-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}

.task-right-tags {
  display: flex;
  align-items: center;
  gap: 6px;
}

.activity-type-tag {
  font-size: 10px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 10px;
  line-height: 1.6;
}

.activity-type-tag.APPLICATION { background: #e0f2fe; color: #0369a1; }
.activity-type-tag.INTERVIEW { background: #fef3c7; color: #b45309; }
.activity-type-tag.OFFER { background: #dcfce7; color: #15803d; }
.activity-type-tag.ONBOARDING { background: #ede9fe; color: #5b21b6; }
.activity-type-tag.REFERRAL { background: #fef9c3; color: #a16207; }

.task-title { font-size: 13px; color: var(--c-text); line-height: 1.5; margin-bottom: 4px; }

.task-meta {
  font-size: 12px;
  color: var(--c-text-muted);
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 4px;
}

.activity-desc {
  font-size: 12px;
  color: var(--c-text-muted);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.task-time {
  display: inline-flex;
  align-items: center;
  gap: 2px;
}

.activity-time { font-size: 11px; color: var(--c-text-disabled); flex-shrink: 0; }

.task-actions {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
  align-items: flex-start;
  margin-top: 2px;
}
</style>

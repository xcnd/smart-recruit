<template>
  <div class="activities-page">
    <div class="sr-page-header">
      <h1>最近动态</h1>
      <p>查看所有招聘活动动态，支持按类型和关键字筛选。</p>
    </div>

    <!-- Filters -->
    <div class="filter-bar">
      <el-select v-model="filterType" placeholder="全部类型" clearable style="width: 140px" @change="search">
        <el-option v-for="opt in typeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
      <el-input
        v-model="keyword"
        placeholder="搜索动态内容..."
        clearable
        style="width: 280px"
        @keyup.enter="search"
        @clear="search"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-button type="primary" @click="search">搜索</el-button>
    </div>

    <!-- List -->
    <div class="sr-section" v-loading="loading">
      <div v-if="activities.length === 0 && !loading" class="empty-state">
        <el-empty description="暂无动态" />
      </div>
      <div
        v-for="(activity, index) in activities"
        :key="activity.id"
        class="activity-item"
        :class="{ 'is-clickable': activity.relatedType && activity.relatedId }"
        @click="navigateToDetail(activity)"
      >
        <span class="activity-index">{{ (page - 1) * size + index + 1 }}</span>
        <div class="activity-icon" :class="getActivityClass(activity.type)">
          <el-icon :size="14"><component :is="getActivityIcon(activity.type)" /></el-icon>
        </div>
        <div class="activity-content">
          <div class="activity-header">
            <span class="activity-type-tag" :class="getActivityClass(activity.type)">{{ getActivityLabel(activity.type) }}</span>
            <span class="activity-time">{{ formatTimeAgo(activity.createdAt) }}</span>
          </div>
          <div class="activity-title">{{ activity.title }}</div>
          <div class="activity-desc" v-if="activity.description">{{ activity.description }}</div>
        </div>
      </div>
    </div>

    <!-- Pagination -->
    <div class="sr-pagination-footer" v-if="total > 0">
      <span class="sr-pagination-info">共 <strong>{{ total }}</strong> 条动态</span>
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
import { Search } from '@element-plus/icons-vue'
import {
  DocumentCopy, VideoCamera, Tickets, Van, Promotion, UserFilled,
} from '@element-plus/icons-vue'
import { getActivitiesPage } from '@/api/dashboard'
import type { RecentActivityVO } from '@/types/models'

const router = useRouter()
const loading = ref(false)
const activities = ref<RecentActivityVO[]>([])
const page = ref(1)
const size = ref(20)
const total = ref(0)
const filterType = ref<number | undefined>(undefined)
const keyword = ref('')

const typeOptions = [
  { value: 0, label: '投递' },
  { value: 1, label: '筛选' },
  { value: 2, label: '面试' },
  { value: 3, label: 'Offer' },
  { value: 4, label: '入职' },
  { value: 5, label: '内推' },
  { value: 6, label: '系统' },
]

function getActivityIcon(type: number) {
  const map: Record<number, unknown> = {
    0: DocumentCopy, 1: DocumentCopy, 2: VideoCamera,
    3: Tickets, 4: Van, 5: Promotion, 6: UserFilled,
  }
  return map[type] || UserFilled
}

function getActivityClass(type: number): string {
  const map: Record<number, string> = {
    0: 'APPLICATION', 1: 'APPLICATION', 2: 'INTERVIEW',
    3: 'OFFER', 4: 'ONBOARDING', 5: 'REFERRAL', 6: 'REFERRAL',
  }
  return map[type] || 'APPLICATION'
}

function getActivityLabel(type: number): string {
  const map: Record<number, string> = {
    0: '投递', 1: '筛选', 2: '面试', 3: 'Offer', 4: '入职', 5: '内推', 6: '系统',
  }
  return map[type] || '动态'
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
    const result = await getActivitiesPage({
      page: page.value,
      size: size.value,
      type: filterType.value,
      keyword: keyword.value || undefined,
    })
    activities.value = result.records
    total.value = result.total
  } catch {
    activities.value = []
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  loadPage()
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

.activity-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 0;
  border-bottom: 1px solid var(--c-border-light);
}

.activity-item.is-clickable {
  cursor: pointer;
  border-radius: var(--c-radius-sm);
  transition: background 0.15s;
}

.activity-item.is-clickable:hover {
  background: var(--c-bg-hover, #f5f7fa);
}

.activity-item:last-child { border-bottom: none; }

.activity-index {
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

.activity-icon {
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

.activity-icon.APPLICATION { background: var(--c-info); }
.activity-icon.INTERVIEW { background: var(--c-warning); }
.activity-icon.OFFER { background: var(--c-success); }
.activity-icon.ONBOARDING { background: var(--c-primary); }
.activity-icon.REFERRAL { background: #f59e0b; }

.activity-content { flex: 1; min-width: 0; }

.activity-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
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

.activity-title { font-size: 13px; color: var(--c-text); line-height: 1.5; margin-bottom: 6px; }

.activity-desc {
  font-size: 12px;
  color: var(--c-text-muted);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.activity-time { font-size: 11px; color: var(--c-text-disabled); flex-shrink: 0; }
</style>

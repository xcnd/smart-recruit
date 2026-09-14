<template>
  <div class="dashboard">
    <div class="sr-page-header">
      <h1>工作台</h1>
      <p>欢迎回来，{{ userStore.userName }}。以下是今日招聘数据概览。</p>
      <p class="header-date">{{ todayDate }}</p>
    </div>

    <!-- Daily Stats -->
    <div class="sr-section-title" style="margin-bottom: 16px;">每日数据</div>
    <div class="sr-stat-cards">
      <div
        v-for="card in dailyCards"
        :key="card.key"
        class="sr-stat-card daily-stat-card"
        :style="{ borderTop: `3px solid ${card.color}` }"
      >
        <div class="stat-info">
          <div class="stat-value" :style="{ color: card.color }">{{ card.value }}</div>
          <div class="stat-label">{{ card.label }}</div>
        </div>
        <el-icon :size="18" :color="card.color"><component :is="card.icon" /></el-icon>
      </div>
    </div>

    <!-- Process KPI Cards -->
    <div class="sr-section-title" style="margin: 24px 0 16px;">流程概览</div>
    <div class="sr-stat-cards">
      <div class="sr-stat-card" v-for="kpi in kpiCards" :key="kpi.key">
        <div class="stat-icon" :style="{ background: kpi.bgColor, color: kpi.color }">
          <el-icon :size="22"><component :is="kpi.icon" /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ kpi.value }}</div>
          <div class="stat-label">{{ kpi.label }}</div>
          <div class="stat-change" :class="kpi.trend">
            <el-icon :size="12"><component :is="kpi.trend === 'up' ? Top : Bottom" /></el-icon>
            {{ kpi.change }}
          </div>
        </div>
      </div>
    </div>

    <!-- Charts Section -->
    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 24px; margin-bottom: 24px;">
      <!-- Funnel Chart -->
      <div class="sr-section">
        <div class="sr-section-title">
          招聘漏斗
          <span class="section-subtitle">统计周期：全部候选人阶段分布</span>
        </div>
        <div style="height: 320px;">
          <canvas ref="funnelChartRef"></canvas>
        </div>
      </div>

      <!-- Department Progress -->
      <div class="sr-section">
        <div class="sr-section-title">
          部门招聘进度
          <span class="section-subtitle">统计周期：当前在招职位</span>
        </div>
        <div style="height: 320px;">
          <canvas ref="deptChartRef"></canvas>
        </div>
      </div>
    </div>

    <!-- Lists Section -->
    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 24px;">
      <!-- Recent Activities -->
      <div class="sr-section">
        <div class="sr-section-title">
          最近动态
          <router-link to="/dashboard/activities" class="view-all-link">查看全部</router-link>
        </div>
        <el-skeleton :loading="loading" animated :count="5">
          <template #default>
            <div
              v-for="activity in activities"
              :key="activity.id"
              class="activity-item"
              :class="{ 'is-clickable': activity.relatedType && activity.relatedId }"
              @click="navigateToDetail(activity)"
            >
              <div class="activity-icon" :class="getActivityTypeClass(activity.type)">
                <el-icon :size="14"><component :is="getActivityIcon(activity.type)" /></el-icon>
              </div>
              <div class="activity-content">
                <div class="activity-header">
                  <span class="activity-type-tag" :class="getActivityTypeClass(activity.type)">{{ getActivityTypeLabel(activity.type) }}</span>
                  <span class="activity-time">{{ activity.time || formatTimeAgo(activity.createdAt) }}</span>
                </div>
                <div class="activity-title">{{ activity.title }}</div>
                <div class="activity-desc" v-if="activity.description">{{ activity.description }}</div>
              </div>
            </div>
            <el-empty v-if="!activities.length" description="暂无动态" />
          </template>
        </el-skeleton>
      </div>

      <!-- Pending Tasks -->
      <div class="sr-section">
        <div class="sr-section-title">
          待办事项
          <router-link to="/dashboard/tasks" class="view-all-link">查看全部</router-link>
        </div>
        <el-skeleton :loading="loading" animated :count="5">
          <template #default>
            <div
              v-for="task in tasks"
              :key="task.id"
              class="task-item"
              :class="{ 'is-clickable': task.relatedType && task.relatedId }"
              @click="navigateToDetail(task)"
            >
              <div class="task-type-icon" :class="getTaskTypeClass(task.type ?? task.category ?? 5)">
                <el-icon :size="14"><component :is="getTaskTypeIcon(task.type ?? task.category ?? 5)" /></el-icon>
              </div>
              <div class="task-content">
                <div class="activity-header">
                  <span class="activity-type-tag" :class="getTaskTypeClass(task.type ?? task.category ?? 5)">{{ getTaskTypeLabel(task.type ?? task.category ?? 5) }}</span>
                  <div class="task-urgency-tags">
                    <el-tag
                      :type="getTaskPriorityType(task.priority ?? task.urgency ?? 1)"
                      size="small"
                      class="task-urgency"
                    >
                      {{ getTaskPriorityLabel(task.priority ?? task.urgency ?? 1) }}
                    </el-tag>
                    <span class="activity-time task-time" v-if="task.dueDate || task.scheduledAt">
                      <el-icon :size="10"><Clock /></el-icon>
                      {{ formatTimeAgo(task.dueDate) || task.scheduledAt }}
                    </span>
                  </div>
                </div>
                <div class="task-title">{{ task.title }}</div>
                <div class="activity-desc" v-if="task.description">{{ task.description }}</div>
              </div>
              <div class="task-actions">
                <el-button size="small" type="success" :icon="Check" circle @click.stop="handleCompleteTask(task)" />
                <el-button size="small" type="info" :icon="Close" circle @click.stop="handleDismissTask(task)" />
              </div>
            </div>
            <el-empty v-if="!tasks.length" description="暂无待办事项" />
          </template>
        </el-skeleton>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, markRaw, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { Chart, registerables } from 'chart.js'
import { Top, Bottom, Briefcase, DocumentCopy, VideoCamera, Tickets, User, Calendar, TrendCharts, DataLine } from '@element-plus/icons-vue'
import {
  UserFilled, Check, CircleCheck, Van, Promotion, Close, Clock,
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import {
  getKpi, getFunnel, getDepartmentProgress,
  getRecentActivities, getPendingTasks,
  completeTask as completeTaskApi, dismissTask as dismissTaskApi,
} from '@/api/dashboard'
import { ElMessage } from 'element-plus'
import type {
  DashboardKpiVO, FunnelStageVO, DepartmentProgressVO,
  RecentActivityVO, PendingTaskVO,
} from '@/types/models'

Chart.register(...registerables)

const userStore = useUserStore()
const router = useRouter()
const loading = ref(true)

const todayDate = new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric', month: 'long', day: 'numeric', weekday: 'long',
}).format(new Date())
const funnelChartRef = ref<HTMLCanvasElement>()
const deptChartRef = ref<HTMLCanvasElement>()
let funnelChart: Chart | null = null
let deptChart: Chart | null = null

const dailyCards = ref([
  { key: 'today', value: 0, label: '今日新增候选人', icon: markRaw(User), color: '#3b82f6' },
  { key: 'yesterday', value: 0, label: '昨日新增候选人', icon: markRaw(Calendar), color: '#06b6d4' },
  { key: 'month', value: 0, label: '本月新增候选人', icon: markRaw(TrendCharts), color: '#10b981' },
  { key: 'jobs', value: 0, label: '在招职位', icon: markRaw(DataLine), color: '#8b5cf6' },
])

const kpiCards = ref([
  { key: 'interviews', value: 0, label: '待面试', icon: markRaw(VideoCamera), color: '#d97706', bgColor: '#fffbeb', trend: 'up', change: '' },
  { key: 'offers', value: 0, label: '待发Offer', icon: markRaw(Tickets), color: '#059669', bgColor: '#ecfdf5', trend: 'up', change: '' },
  { key: 'hires', value: 0, label: '本月入职', icon: markRaw(Briefcase), color: '#4f46e5', bgColor: '#eef2ff', trend: 'up', change: '' },
  { key: 'avgDays', value: 0, label: '平均到岗天数', icon: markRaw(DocumentCopy), color: '#0ea5e9', bgColor: '#f0f9ff', trend: 'down', change: '' },
])

const activities = ref<RecentActivityVO[]>([])
const tasks = ref<PendingTaskVO[]>([])

function getActivityIcon(activityType: number) {
  const map: Record<number, unknown> = {
    0: DocumentCopy,   // APPLY
    1: DocumentCopy,   // SCREEN
    2: VideoCamera,    // INTERVIEW
    3: Tickets,        // OFFER
    4: Van,            // HIRE
    5: Promotion,      // REFERRAL
    6: UserFilled,     // SYSTEM
  }
  return map[activityType] || UserFilled
}

function getActivityTypeClass(activityType: number): string {
  const map: Record<number, string> = {
    0: 'APPLICATION',
    1: 'APPLICATION',
    2: 'INTERVIEW',
    3: 'OFFER',
    4: 'ONBOARDING',
    5: 'REFERRAL',
    6: 'REFERRAL',
  }
  return map[activityType] || 'APPLICATION'
}

function getActivityTypeLabel(activityType: number): string {
  const map: Record<number, string> = {
    0: '投递',
    1: '筛选',
    2: '面试',
    3: 'Offer',
    4: '入职',
    5: '内推',
    6: '系统',
  }
  return map[activityType] || '动态'
}

function getTaskPriorityLabel(priority: number): string {
  return priority === 0 ? '高' : priority === 1 ? '中' : '低'
}

function getTaskPriorityType(priority: number): 'danger' | 'warning' | 'info' {
  return priority === 0 ? 'danger' : priority === 1 ? 'warning' : 'info'
}

function getTaskTypeLabel(taskType: number): string {
  const map: Record<number, string> = {
    0: '筛选简历',
    1: '安排面试',
    2: '审批Offer',
    3: '办理入职',
    4: '反馈评审',
    5: '其他',
  }
  return map[taskType] || '待办'
}

function getTaskTypeIcon(taskType: number): unknown {
  const map: Record<number, unknown> = {
    0: DocumentCopy,
    1: VideoCamera,
    2: Tickets,
    3: Van,
    4: CircleCheck,
    5: Promotion,
  }
  return map[taskType] || Promotion
}

function getTaskTypeClass(taskType: number): string {
  const map: Record<number, string> = {
    0: 'APPLICATION',
    1: 'INTERVIEW',
    2: 'OFFER',
    3: 'ONBOARDING',
    4: 'APPLICATION',
    5: 'REFERRAL',
  }
  return map[taskType] || 'REFERRAL'
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

function formatDateTime(dateStr: string | null): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const pad = (n: number) => n < 10 ? `0${n}` : `${n}`
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

function formatDate(dateStr: string | null): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const pad = (n: number) => n < 10 ? `0${n}` : `${n}`
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

/** 根据 relatedType + relatedId 跳转到对应的详情页。 */
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
  if (path) {
    router.push(path)
  }
}

async function loadData() {
  try {
    const [kpiData, funnelData, deptData, activityData, taskData] = await Promise.all([
      getKpi(),
      getFunnel(),
      getDepartmentProgress(),
      getRecentActivities(),
      getPendingTasks(),
    ])

    // Map daily stats to cards
    dailyCards.value[0].value = kpiData.todayCandidateCount || 0
    dailyCards.value[1].value = kpiData.yesterdayCandidateCount || 0
    dailyCards.value[2].value = kpiData.applicationCount || 0
    dailyCards.value[3].value = kpiData.jobCount || 0

    // Map KPI data to process cards
    kpiCards.value[0].value = kpiData.pendingInterview || 0
    kpiCards.value[0].change = kpiData.pendingInterviewTrend || ''
    kpiCards.value[0].trend = 'up'
    kpiCards.value[1].value = kpiData.pendingOffer || 0
    kpiCards.value[1].change = kpiData.pendingOfferTrend || ''
    kpiCards.value[1].trend = 'up'
    kpiCards.value[2].value = kpiData.hiresThisMonth || 0
    kpiCards.value[2].change = kpiData.hiresThisMonthTrend || ''
    kpiCards.value[2].trend = (kpiData.hiresThisMonthTrend || '').startsWith('+') ? 'up' : 'down'
    kpiCards.value[3].value = kpiData.avgDaysToHire || 0
    kpiCards.value[3].change = kpiData.avgDaysToHireTrend || ''
    kpiCards.value[3].trend = 'down'

    // Map activity data
    activities.value = (activityData || []).map(a => ({
      ...a,
      user: a.candidateName || a.actorName || '',
      time: a.createdAt ? formatTimeAgo(a.createdAt) : '',
    }))

    // Map task data
    tasks.value = (taskData || []).map(t => ({
      ...t,
      candidateName: t.relatedPerson || t.candidateName,
      scheduledAt: t.dueDate ? formatTimeAgo(t.dueDate) : '',
      urgency: t.priority,
      type: t.category,
    }))

    renderCharts(funnelData || [], deptData || [])
  } catch (err) {
    console.warn('Dashboard data load failed, using mock data:', err)
    loadMockData()
  }
}

function loadMockData() {
  dailyCards.value[0].value = 5
  dailyCards.value[1].value = 12
  dailyCards.value[2].value = 86
  dailyCards.value[3].value = 24

  kpiCards.value[0].value = 47
  kpiCards.value[0].change = '+15% 较上月'
  kpiCards.value[1].value = 8
  kpiCards.value[1].change = '+3% 较上月'
  kpiCards.value[2].value = 12
  kpiCards.value[2].change = '+20% 较上月'
  kpiCards.value[3].value = 18.5
  kpiCards.value[3].change = '-0.5天'

  activities.value = [
    { id: 1, type: 2, title: '张明完成前端开发工程师技术面试', description: '', candidateName: '张明', jobTitle: '前端开发工程师', user: '王面试官', createdAt: new Date(Date.now() - 10 * 60000).toISOString(), relatedType: 'CANDIDATE', relatedId: '1' },
    { id: 2, type: 0, title: '李婷申请高级产品经理职位', description: '', candidateName: '李婷', jobTitle: '高级产品经理', user: '系统', createdAt: new Date(Date.now() - 25 * 60000).toISOString(), relatedType: 'CANDIDATE', relatedId: '2' },
    { id: 3, type: 3, title: '陈华接受了Java开发工程师Offer', description: '', candidateName: '陈华', jobTitle: 'Java开发工程师', user: '张伟', createdAt: new Date(Date.now() - 60 * 60000).toISOString(), relatedType: 'CANDIDATE', relatedId: '3' },
    { id: 4, type: 5, title: '赵丽通过内推推荐了2位候选人', description: '', candidateName: '赵丽', jobTitle: '', user: '赵丽', createdAt: new Date(Date.now() - 120 * 60000).toISOString(), relatedType: null, relatedId: null },
    { id: 5, type: 4, title: '孙晓已完成入职手续Step 3/6', description: '', candidateName: '孙晓', jobTitle: '', user: '系统', createdAt: new Date(Date.now() - 180 * 60000).toISOString(), relatedType: 'ONBOARDING', relatedId: '5' },
  ]

  tasks.value = [
    { id: 1, category: 0, title: '审核人工智能工程师面试报告', priority: 0, relatedPerson: '周杰', dueDate: '2026-07-28T14:00:00', candidateName: '周杰', scheduledAt: '2026-07-28 14:00', urgency: 0, createdAt: new Date(Date.now() - 2 * 3600000).toISOString(), relatedType: 'INTERVIEW', relatedId: '1' },
    { id: 2, category: 1, title: '审批大数据开发工程师Offer', priority: 0, relatedPerson: '吴强', dueDate: '2026-07-29T00:00:00', candidateName: '吴强', scheduledAt: '2026-07-29', urgency: 0, createdAt: new Date(Date.now() - 4 * 3600000).toISOString(), relatedType: 'OFFER', relatedId: '2' },
    { id: 3, category: 0, title: '筛选今日新投递简历 (15份)', priority: 1, relatedPerson: '', dueDate: null, scheduledAt: '', urgency: 1, createdAt: new Date(Date.now() - 6 * 3600000).toISOString(), relatedType: null, relatedId: null },
    { id: 4, category: 2, title: '确认刘晓入职体检报告', priority: 2, relatedPerson: '刘晓', dueDate: '2026-07-30T00:00:00', candidateName: '刘晓', scheduledAt: '2026-07-30', urgency: 2, createdAt: new Date(Date.now() - 8 * 3600000).toISOString(), relatedType: 'ONBOARDING', relatedId: '4' },
    { id: 5, category: 5, title: '发放赵丽内推奖金 (2人)', priority: 2, relatedPerson: '', dueDate: null, scheduledAt: '', urgency: 2, createdAt: new Date(Date.now() - 10 * 3600000).toISOString(), relatedType: null, relatedId: null },
  ]

  renderCharts([], [])
}

function renderCharts(funnelData: FunnelStageVO[], deptData: DepartmentProgressVO[]) {
  if (funnelChartRef.value) {
    if (funnelChart) funnelChart.destroy()
    const labels = funnelData.map((f) => f.label || `阶段${f.stage}`)
    funnelChart = new Chart(funnelChartRef.value, {
      type: 'bar',
      data: {
        labels,
        datasets: [{
          label: '候选人数量',
          data: funnelData.map((f) => f.count),
          backgroundColor: [
            '#3b82f6', '#06b6d4', '#10b981', '#f59e0b', '#8b5cf6', '#22c55e', '#64748b', '#ef4444',
          ],
          borderRadius: 8,
          borderSkipped: false,
        }],
      },
      options: {
        indexAxis: 'y',
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false } },
        scales: {
          x: { grid: { display: false }, ticks: { font: { size: 11 } } },
          y: { grid: { display: false }, ticks: { font: { size: 12 } } },
        },
      },
    })
  }

  if (deptChartRef.value) {
    if (deptChart) deptChart.destroy()
    const hiredVals = deptData.map((d) => d.hired ?? d.filled ?? 0)
    const plannedVals = deptData.map((d) => d.headcount ?? d.total ?? 0)
    deptChart = new Chart(deptChartRef.value, {
      type: 'bar',
      data: {
        labels: deptData.map((d) => d.departmentName),
        datasets: [
          {
            label: '已招',
            data: hiredVals,
            backgroundColor: '#1677ff',
            borderRadius: 6,
          },
          {
            label: '待招',
            data: plannedVals.map((total, i) => Math.max(0, total - hiredVals[i])),
            backgroundColor: '#e2e8f0',
            borderRadius: 6,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { position: 'bottom', labels: { usePointStyle: true, font: { size: 11 } } } },
        scales: {
          x: { stacked: true, grid: { display: false }, ticks: { font: { size: 11 } } },
          y: { stacked: true, grid: { display: false }, ticks: { font: { size: 12 } } },
        },
      },
    })
  }
}

async function handleCompleteTask(task: PendingTaskVO) {
  try {
    await completeTaskApi(task.id)
    ElMessage.success('任务已完成')
    tasks.value = tasks.value.filter(t => String(t.id) !== String(task.id))
  } catch {
    ElMessage.error('操作失败')
  }
}

async function handleDismissTask(task: PendingTaskVO) {
  try {
    await dismissTaskApi(task.id)
    ElMessage.success('任务已忽略')
    tasks.value = tasks.value.filter(t => String(t.id) !== String(task.id))
  } catch {
    ElMessage.error('操作失败')
  }
}

let refreshTimer: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  loadData().finally(() => { loading.value = false })
  // 每30秒自动刷新数据
  refreshTimer = setInterval(() => {
    loadData()
  }, 30000)
})

onBeforeUnmount(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  if (funnelChart) funnelChart.destroy()
  if (deptChart) deptChart.destroy()
})
</script>

<style scoped>
.dashboard {
  max-width: 1400px;
}

.header-date {
  margin-top: 4px;
  font-size: 13px;
  color: var(--c-text-muted);
}

.daily-stat-card {
  flex-direction: row-reverse;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
}

.daily-stat-card .stat-value {
  font-size: 28px;
  font-weight: 700;
}

.daily-stat-card .stat-label {
  font-size: 13px;
  color: var(--c-text-muted);
  margin-top: 2px;
}

.section-subtitle {
  font-size: 12px;
  font-weight: 400;
  color: var(--c-text-muted);
  margin-left: 12px;
}

.activity-item {
  display: flex;
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

.activity-item:last-child {
  border-bottom: none;
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

.activity-content {
  flex: 1;
  min-width: 0;
}

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

.activity-title {
  font-size: 13px;
  color: var(--c-text);
  line-height: 1.5;
  margin-bottom: 6px;
}

.activity-meta {
  font-size: 12px;
  color: var(--c-text-muted);
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 6px;
  flex-wrap: wrap;
}

.activity-meta .el-icon {
  flex-shrink: 0;
}

.meta-sep {
  margin: 0 2px;
  color: var(--c-text-disabled);
  user-select: none;
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

.activity-time {
  font-size: 11px;
  color: var(--c-text-disabled);
  flex-shrink: 0;
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

.task-item:last-child {
  border-bottom: none;
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

.task-urgency-tags {
  display: flex;
  align-items: center;
  gap: 8px;
}

.task-time {
  display: inline-flex;
  align-items: center;
  gap: 2px;
}

.task-urgency {
  flex-shrink: 0;
}

.task-content {
  flex: 1;
  min-width: 0;
}

.task-title {
  font-size: 13px;
  color: var(--c-text);
  line-height: 1.5;
  margin-bottom: 6px;
}

.task-actions {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
  align-items: flex-start;
  margin-top: 2px;
}

.view-all-link {
  font-size: 12px;
  font-weight: 400;
  color: var(--c-primary);
  text-decoration: none;
  margin-left: auto;
}

.view-all-link:hover {
  text-decoration: underline;
}
</style>

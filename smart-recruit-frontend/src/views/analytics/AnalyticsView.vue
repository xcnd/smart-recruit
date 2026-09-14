<template>
  <div class="analytics-page">
    <!-- Page Header -->
    <div class="sr-page-header">
      <h1>数据分析</h1>
      <p>基于招聘全流程真实数据的多维分析与洞察</p>
    </div>

    <!-- Toolbar -->
    <div class="toolbar">
      <el-radio-group v-model="timeRange" @change="loadData">
        <el-radio-button value="7d">近7天</el-radio-button>
        <el-radio-button value="30d">近30天</el-radio-button>
        <el-radio-button value="90d">近90天</el-radio-button>
        <el-radio-button value="year">本年度</el-radio-button>
      </el-radio-group>
      <div class="toolbar-right">
        <el-text v-if="overview" type="info" size="small">
          {{ overview.startDate }} ~ {{ overview.endDate }}
        </el-text>
        <el-button :icon="Download" :loading="exporting" @click="handleExport">
          导出报表
        </el-button>
      </div>
    </div>

    <!-- Load Error -->
    <el-alert
      v-if="loadError"
      title="数据加载失败，请确认后端服务已启动"
      type="error"
      :closable="false"
      class="load-error"
      show-icon
    >
      <template #default>
        <el-button size="small" text type="primary" :loading="loading" @click="loadData">
          重新加载
        </el-button>
      </template>
    </el-alert>

    <!-- KPI Cards -->
    <div v-loading="loading" class="kpi-grid">
      <div
        v-for="kpi in overview?.kpis || []"
        :key="kpi.key"
        class="kpi-card"
      >
        <div class="kpi-label">{{ kpi.label }}</div>
        <div class="kpi-value-row">
          <span class="kpi-value">{{ formatKpiValue(kpi) }}</span>
          <span class="kpi-unit">{{ kpi.unit }}</span>
        </div>
        <div class="kpi-trend" :class="trendClass(kpi)">
          {{ trendText(kpi) }}
        </div>
      </div>
    </div>

    <el-empty
      v-if="!loading && (!overview || overview.kpis.length === 0)"
      description="暂无统计数据，请先创建招聘业务数据"
      :image-size="80"
    />

    <!-- Charts Row 1 -->
    <div class="chart-grid">
      <section class="chart-card">
        <div class="chart-card-title">转化漏斗</div>
        <div class="chart-card-desc">当前周期内新增候选人在各招聘阶段的分布与转化</div>
        <div class="chart-body"><canvas ref="funnelRef"></canvas></div>
      </section>
      <section class="chart-card">
        <div class="chart-card-title">渠道分布</div>
        <div class="chart-card-desc">当前周期内候选人来源渠道占比</div>
        <div class="chart-body"><canvas ref="channelRef"></canvas></div>
      </section>
    </div>

    <!-- Charts Row 2 -->
    <div class="chart-grid">
      <section class="chart-card">
        <div class="chart-card-title">招聘趋势（近 12 个月）</div>
        <div class="chart-card-desc">月度新增候选人与完成入职对比</div>
        <div class="chart-body"><canvas ref="trendRef"></canvas></div>
      </section>
      <section class="chart-card">
        <div class="chart-card-title">Offer 趋势（近 12 个月）</div>
        <div class="chart-card-desc">月度发送/接受数量与平均确认周期</div>
        <div class="chart-body"><canvas ref="offerRef"></canvas></div>
      </section>
    </div>

    <!-- Charts Row 3 -->
    <div class="chart-grid">
      <section class="chart-card">
        <div class="chart-card-title">部门招聘进度</div>
        <div class="chart-card-desc">各部门计划编制与实际入职进度</div>
        <div class="chart-body"><canvas ref="deptRef"></canvas></div>
      </section>
      <section class="chart-card">
        <div class="chart-card-title">渠道 ROI 分析</div>
        <div class="chart-card-desc">成本为投放估算模型，ROI = 入职数 × 岗位年化价值 ÷ 总成本</div>
        <div class="chart-table">
          <el-table :data="overview?.channels || []" stripe size="small">
            <el-table-column prop="channel" label="渠道" min-width="90" />
            <el-table-column label="投递量" min-width="70" align="right">
              <template #default="{ row }">{{ row.candidateCount ?? 0 }}</template>
            </el-table-column>
            <el-table-column label="占比" min-width="70" align="right">
              <template #default="{ row }">{{ (row.percentage ?? 0).toFixed(1) }}%</template>
            </el-table-column>
            <el-table-column label="入职数" min-width="70" align="right">
              <template #default="{ row }">{{ row.hireCount ?? 0 }}</template>
            </el-table-column>
            <el-table-column label="转化率" min-width="80" align="right">
              <template #default="{ row }">{{ (row.conversionRate ?? 0).toFixed(1) }}%</template>
            </el-table-column>
            <el-table-column label="总成本" min-width="90" align="right">
              <template #default="{ row }">¥{{ formatMoney(row.totalCost) }}</template>
            </el-table-column>
            <el-table-column label="ROI" min-width="80" align="right">
              <template #default="{ row }">
                <el-tag v-if="row.roi !== null" :type="roiTagType(row.roi)" size="small">
                  {{ (row.roi ?? 0).toFixed(2) }}
                </el-tag>
                <el-tag v-else type="success" size="small">∞</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </section>
    </div>

    <!-- AI Insights -->
    <section class="insight-section">
      <div class="insight-title">
        <el-icon :size="18"><MagicStick /></el-icon>
        AI 洞察
      </div>
      <div class="insight-cards">
        <div
          v-for="insight in overview?.insights || []"
          :key="insight.id"
          class="insight-card"
          :class="{ 'insight-card-ai': insight.aiGenerated }"
        >
          <el-tag
            v-if="insight.aiGenerated"
            type="danger"
            size="small"
            effect="dark"
            round
            class="insight-ai-tag"
          >
            AI
          </el-tag>
          <div class="insight-card-header">
            <div class="insight-icon-badge" :class="iconToneClass(insight.icon)">
              <el-icon :size="17"><component :is="getInsightIcon(insight.icon)" /></el-icon>
            </div>
            <el-tag :type="insightTagType(insight.trend)" size="small" effect="light" round>
              {{ trendArrow(insight.trend) }} {{ insight.value }}
            </el-tag>
          </div>
          <div class="insight-card-title">{{ insight.title }}</div>
          <div class="insight-card-desc">{{ insight.description }}</div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { Download, TrendCharts, Connection, Coin, User, DataLine, Warning, Timer, CircleCheck, MagicStick } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { Chart, registerables } from 'chart.js'
import { getAnalyticsOverview, exportAnalyticsReport } from '@/api/analytics'
import { getDepartmentProgress } from '@/api/dashboard'
import { formatMoney } from '@/utils/format'
import type {
  AnalyticsOverviewVO,
  AnalyticsKpiVO,
  AnalyticsChannelVO,
  DepartmentProgressVO,
} from '@/types/models'

Chart.register(...registerables)

// ==================== 状态 ====================
const timeRange = ref<'7d' | '30d' | '90d' | 'year'>('30d')
const overview = ref<AnalyticsOverviewVO | null>(null)
const loading = ref(false)
const exporting = ref(false)
const loadError = ref(false)

const funnelRef = ref<HTMLCanvasElement>()
const channelRef = ref<HTMLCanvasElement>()
const trendRef = ref<HTMLCanvasElement>()
const offerRef = ref<HTMLCanvasElement>()
const deptRef = ref<HTMLCanvasElement>()

let funnelChart: Chart | null = null
let channelChart: Chart | null = null
let trendChart: Chart | null = null
let offerChart: Chart | null = null
let deptChart: Chart | null = null

/** AI 洞察异步生成后的一次性自动刷新（避免用户手动刷新页面）。 */
let aiInsightTimer: ReturnType<typeof setTimeout> | null = null
let aiInsightRefreshed = false

// ==================== 时间范围 ====================
const rangeParams = computed(() => {
  const end = new Date()
  const start = new Date()
  if (timeRange.value === '7d') start.setDate(end.getDate() - 6)
  else if (timeRange.value === '30d') start.setDate(end.getDate() - 29)
  else if (timeRange.value === '90d') start.setDate(end.getDate() - 89)
  else start.setMonth(0, 1)
  return {
    startDate: formatDate(start),
    endDate: formatDate(end),
  }
})

function formatDate(d: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

// ==================== 数据加载 ====================
async function loadData() {
  loading.value = true
  loadError.value = false
  aiInsightRefreshed = false
  try {
    const [data, depts] = await Promise.all([
      getAnalyticsOverview(rangeParams.value),
      getDepartmentProgress(),
    ])
    overview.value = data
    renderCharts(data, depts)
    scheduleAiInsightRefresh(data)
  } catch {
    overview.value = null
    loadError.value = true
  } finally {
    loading.value = false
  }
}

/** AI 洞察首次加载通常为规则版，8 秒后自动重拉一次升级为 AI 生成版。 */
function scheduleAiInsightRefresh(data: AnalyticsOverviewVO) {
  if (aiInsightRefreshed) return
  const hasAiInsight = (data.insights || []).some(i => i.aiGenerated)
  if (hasAiInsight) return
  if (aiInsightTimer) clearTimeout(aiInsightTimer)
  aiInsightTimer = setTimeout(async () => {
    aiInsightRefreshed = true
    try {
      const fresh = await getAnalyticsOverview(rangeParams.value)
      if (overview.value) {
        overview.value.insights = fresh.insights || overview.value.insights
      }
    } catch {
      // 刷新失败保持现有洞察
    }
  }, 8000)
}

function renderCharts(data: AnalyticsOverviewVO, depts: DepartmentProgressVO[]) {
  renderFunnel(data.funnel)
  renderChannel(data.channels)
  renderTrend(data.candidateTrend)
  renderOfferTrend(data.offerTrend)
  renderDepartment(depts)
}

// ==================== 图表渲染 ====================
function renderFunnel(funnel: AnalyticsOverviewVO['funnel']) {
  if (!funnelRef.value) return
  funnelChart?.destroy()
  funnelChart = new Chart(funnelRef.value, {
    type: 'bar',
    data: {
      labels: funnel.map(f => f.label || `阶段${f.sortOrder}`),
      datasets: [{
        label: '人数',
        data: funnel.map(f => f.count),
        backgroundColor: ['#818cf8', '#6366f1', '#4f46e5', '#4338ca', '#3730a3', '#312e81', '#94a3b8', '#cbd5e1'],
        borderRadius: 8,
        borderSkipped: false,
      }],
    },
    options: {
      indexAxis: 'y',
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: false },
        tooltip: {
          callbacks: {
            label: (ctx) => {
              const stage = funnel[ctx.dataIndex]
              const rate = stage.conversionRate != null && stage.sortOrder < funnel.length
                ? `，下一阶段转化率 ${stage.conversionRate}%` : ''
              return `人数 ${stage.count}${rate}`
            },
          },
        },
      },
      scales: {
        x: { grid: { display: false }, ticks: { precision: 0 } },
        y: { grid: { display: false } },
      },
    },
  })
}

function renderChannel(channels: AnalyticsChannelVO[]) {
  if (!channelRef.value) return
  channelChart?.destroy()
  channelChart = new Chart(channelRef.value, {
    type: 'doughnut',
    data: {
      labels: channels.map(c => c.channel),
      datasets: [{
        data: channels.map(c => c.candidateCount),
        backgroundColor: ['#4f46e5', '#0ea5e9', '#059669', '#d97706', '#dc2626', '#8b5cf6', '#64748b', '#14b8a6'],
        borderWidth: 0,
      }],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { position: 'bottom', labels: { usePointStyle: true, padding: 14, font: { size: 11 } } },
      },
    },
  })
}

function renderTrend(points: AnalyticsOverviewVO['candidateTrend']) {
  if (!trendRef.value) return
  trendChart?.destroy()
  trendChart = new Chart(trendRef.value, {
    type: 'line',
    data: {
      labels: points.map(p => p.month),
      datasets: [
        {
          label: '新增候选人',
          data: points.map(p => p.candidateCount),
          borderColor: '#4f46e5',
          backgroundColor: 'rgba(79, 70, 229, 0.08)',
          fill: true,
          tension: 0.4,
          pointRadius: 3,
        },
        {
          label: '完成入职',
          data: points.map(p => p.onboardCount),
          borderColor: '#059669',
          backgroundColor: 'rgba(5, 150, 105, 0.08)',
          fill: true,
          tension: 0.4,
          pointRadius: 3,
        },
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { position: 'bottom', labels: { usePointStyle: true, font: { size: 10 } } },
      },
      scales: {
        y: { beginAtZero: true, grid: { color: '#f1f5f9' }, ticks: { precision: 0 } },
        x: { grid: { display: false } },
      },
    },
  })
}

function renderOfferTrend(points: AnalyticsOverviewVO['offerTrend']) {
  if (!offerRef.value) return
  offerChart?.destroy()
  offerChart = new Chart(offerRef.value, {
    data: {
      labels: points.map(p => p.month),
      datasets: [
        {
          type: 'bar',
          label: '发送',
          data: points.map(p => p.sentCount),
          backgroundColor: '#818cf8',
          borderRadius: 4,
          order: 2,
        },
        {
          type: 'bar',
          label: '接受',
          data: points.map(p => p.acceptedCount),
          backgroundColor: '#34d399',
          borderRadius: 4,
          order: 2,
        },
        {
          type: 'line',
          label: '平均确认周期(天)',
          data: points.map(p => p.avgConfirmDays),
          borderColor: '#d97706',
          backgroundColor: 'rgba(217, 119, 6, 0.08)',
          yAxisID: 'y1',
          tension: 0.4,
          pointRadius: 3,
          order: 1,
        },
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { position: 'bottom', labels: { usePointStyle: true, font: { size: 10 } } },
      },
      scales: {
        y: { beginAtZero: true, grid: { color: '#f1f5f9' }, ticks: { precision: 0 } },
        y1: {
          beginAtZero: true,
          position: 'right',
          grid: { display: false },
        },
        x: { grid: { display: false } },
      },
    },
  })
}

function renderDepartment(depts: DepartmentProgressVO[]) {
  if (!deptRef.value) return
  deptChart?.destroy()
  const sorted = [...depts].sort((a, b) => (b.hired ?? 0) - (a.hired ?? 0)).slice(0, 8)
  deptChart = new Chart(deptRef.value, {
    type: 'bar',
    data: {
      labels: sorted.map(d => d.departmentName),
      datasets: [
        {
          label: '已入职',
          data: sorted.map(d => d.hired ?? 0),
          backgroundColor: '#1677ff',
          borderRadius: 4,
        },
        {
          label: '待招',
          data: sorted.map(d => Math.max(0, (d.headcount ?? 0) - (d.hired ?? 0))),
          backgroundColor: '#e2e8f0',
          borderRadius: 4,
        },
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { position: 'bottom', labels: { usePointStyle: true, font: { size: 10 } } },
      },
      scales: {
        x: { stacked: true, grid: { display: false } },
        y: { stacked: true, beginAtZero: true, grid: { color: '#f1f5f9' }, ticks: { precision: 0 } },
      },
    },
  })
}

// ==================== KPI 展示 ====================
function formatKpiValue(kpi: AnalyticsKpiVO): string {
  const value = kpi.value ?? 0
  if (kpi.key === 'acceptanceRate' || kpi.key === 'avgConfirmDays') {
    return value.toFixed(1)
  }
  return Math.round(value).toLocaleString('zh-CN')
}

function trendText(kpi: AnalyticsKpiVO): string {
  if (kpi.changePercent === null) return '环比 —'
  if (kpi.changePercent > 0) return `环比 ↑ ${kpi.changePercent.toFixed(1)}%`
  if (kpi.changePercent < 0) return `环比 ↓ ${Math.abs(kpi.changePercent).toFixed(1)}%`
  return '环比持平'
}

function trendClass(kpi: AnalyticsKpiVO): string {
  if (kpi.changePercent === null) return 'stable'
  return kpi.changePercent >= 0 ? 'up' : 'down'
}

// ==================== 洞察展示 ====================
function getInsightIcon(icon: string) {
  const map: Record<string, unknown> = {
    trend: TrendCharts,
    connection: Connection,
    money: Coin,
    coin: Coin,
    user: User,
    data: DataLine,
    warning: Warning,
    timer: Timer,
    'circle-check': CircleCheck,
  }
  return map[icon] || DataLine
}

function trendArrow(trend: string): string {
  return trend === 'UP' ? '↑' : trend === 'DOWN' ? '↓' : '→'
}

/** 图标徽章色调（不同洞察类型不同颜色，视觉上更专业）。 */
function iconToneClass(icon: string): string {
  const tones: Record<string, string> = {
    trend: 'tone-violet',
    connection: 'tone-blue',
    money: 'tone-amber',
    coin: 'tone-amber',
    user: 'tone-green',
    data: 'tone-cyan',
    warning: 'tone-red',
    timer: 'tone-purple',
    'circle-check': 'tone-green',
  }
  return tones[icon] || 'tone-cyan'
}

function insightTagType(trend: string): 'success' | 'danger' | 'info' {
  if (trend === 'UP') return 'success'
  if (trend === 'DOWN') return 'danger'
  return 'info'
}

function roiTagType(roi: number): 'success' | 'warning' | 'danger' {
  if (roi > 1) return 'success'
  if (roi > 0.5) return 'warning'
  return 'danger'
}

// ==================== 导出 ====================
async function handleExport() {
  exporting.value = true
  try {
    const blob = await exportAnalyticsReport(rangeParams.value)
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `smart-recruit-analytics-${rangeParams.value.startDate}-${rangeParams.value.endDate}.csv`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    // 延迟释放，确保浏览器完成下载
    setTimeout(() => URL.revokeObjectURL(url), 1000)
    ElMessage.success('报表已导出')
  } catch {
    // HTTP 拦截器统一提示
  } finally {
    exporting.value = false
  }
}

// ==================== 生命周期 ====================
onMounted(loadData)

onBeforeUnmount(() => {
  if (aiInsightTimer) {
    clearTimeout(aiInsightTimer)
    aiInsightTimer = null
  }
  [funnelChart, channelChart, trendChart, offerChart, deptChart].forEach(c => c?.destroy())
})
</script>

<style scoped>
.analytics-page {
  max-width: 1280px;
}

.sr-page-header {
  margin-bottom: 20px;
}

.sr-page-header h1 {
  font-size: 20px;
  font-weight: 700;
  color: var(--c-text);
  margin-bottom: 4px;
}

.sr-page-header p {
  font-size: 13px;
  color: var(--c-text-secondary);
}

/* Toolbar */
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 20px;
  padding: 14px 16px;
  background: var(--el-bg-color);
  border: 1px solid var(--c-border-light);
  border-radius: var(--c-radius-lg);
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-left: auto;
}

.load-error {
  margin-bottom: 16px;
}

/* KPI */
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
  min-height: 96px;
}

.kpi-card {
  min-width: 0;
  padding: 16px 18px;
  background: var(--el-bg-color);
  border: 1px solid var(--c-border-light);
  border-radius: var(--c-radius-lg);
  box-shadow: var(--c-shadow-sm);
}

.kpi-label {
  font-size: 12px;
  color: var(--c-text-secondary);
  margin-bottom: 8px;
}

.kpi-value-row {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.kpi-value {
  font-size: 26px;
  font-weight: 700;
  color: var(--c-text);
  line-height: 1.2;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.kpi-unit {
  font-size: 12px;
  color: var(--c-text-muted);
}

.kpi-trend {
  margin-top: 8px;
  font-size: 12px;
}

.kpi-trend.up { color: var(--c-success); }
.kpi-trend.down { color: var(--c-danger); }
.kpi-trend.stable { color: var(--c-text-muted); }

/* Charts */
.chart-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
}

.chart-card {
  min-width: 0;
  padding: 18px 20px;
  background: var(--el-bg-color);
  border: 1px solid var(--c-border-light);
  border-radius: var(--c-radius-lg);
  box-shadow: var(--c-shadow-sm);
  overflow: hidden;
}

.chart-card-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--c-text);
}

.chart-card-desc {
  margin: 4px 0 14px;
  font-size: 12px;
  color: var(--c-text-muted);
}

.chart-body {
  height: 280px;
  min-height: 280px;
  width: 100%;
  position: relative;
}

.chart-body :deep(canvas) {
  width: 100% !important;
  height: 100% !important;
}

.chart-table {
  width: 100%;
  overflow-x: auto;
}

.chart-table :deep(.el-table) {
  width: 100%;
}

/* Insights */
.insight-section {
  padding: 18px 20px;
  background: var(--el-bg-color);
  border: 1px solid var(--c-border-light);
  border-radius: var(--c-radius-lg);
  box-shadow: var(--c-shadow-sm);
  overflow: hidden;
}

.insight-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: var(--c-text);
  margin-bottom: 16px;
}

.insight-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 14px;
}

.insight-card {
  position: relative;
  min-width: 0;
  padding: 14px 16px;
  border: 1px solid var(--c-border);
  border-radius: var(--c-radius-md);
  transition: var(--c-transition);
}

.insight-card:hover { border-color: var(--c-primary); }

/* AI 生成洞察：紫色渐变描边 + 右上角 AI 标识 */
.insight-card-ai {
  border-color: #c7d2fe;
  background: linear-gradient(180deg, #fafaff 0%, #ffffff 100%);
}

/* AI 徽章占据右上角，给头部右侧留出空间，避免遮挡趋势数值 */
.insight-card-ai .insight-card-header {
  padding-right: 42px;
}

.insight-ai-tag {
  position: absolute;
  top: 10px;
  right: 10px;
  background: #1677ff !important;
  border: none;
  font-weight: 600;
}

.insight-card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.insight-icon-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 10px;
  flex: none;
}

.insight-icon-badge.tone-violet { background: #eef2ff; color: #6366f1; }
.insight-icon-badge.tone-blue   { background: #eff6ff; color: #3b82f6; }
.insight-icon-badge.tone-amber  { background: #fffbeb; color: #d97706; }
.insight-icon-badge.tone-green  { background: #ecfdf5; color: #059669; }
.insight-icon-badge.tone-cyan   { background: #ecfeff; color: #0891b2; }
.insight-icon-badge.tone-red    { background: #fef2f2; color: #dc2626; }
.insight-icon-badge.tone-purple { background: #faf5ff; color: #9333ea; }

.insight-card-header .el-tag {
  margin-left: auto;
}

.insight-card-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--c-text);
  margin-bottom: 6px;
}

.insight-card-desc {
  font-size: 13px;
  color: var(--c-text-secondary);
  line-height: 1.6;
}

@media (max-width: 900px) {
  .chart-grid {
    grid-template-columns: 1fr;
  }

  .toolbar {
    justify-content: flex-start;
  }
}
</style>

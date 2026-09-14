<template>
  <div class="interview-report-page">
    <el-button :icon="ArrowLeft" text @click="$router.back()" style="margin-bottom: 16px;">返回面试列表</el-button>

    <div v-loading="loading">
      <!-- AI 评估中提示 -->
      <el-alert
        v-if="report?.evaluationStatus === 'PROCESSING'"
        title="AI 评估生成中，页面将自动刷新..."
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 16px;"
      >
        <template #default>
          <span>AI 正在进行六维度评估分析，当前显示的是基于反馈评分的初步报告，稍后会自动更新完整的 AI 评估结果。</span>
        </template>
      </el-alert>

      <el-alert
        v-if="report?.evaluationStatus === 'FAILED'"
        title="AI 评估失败"
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom: 16px;"
      >
        <template #default>
          <span>AI 自动评估未能完成，当前显示的是基于反馈评分的评估报告。</span>
        </template>
      </el-alert>

      <!-- Header -->
      <div class="sr-section" v-if="report">
        <div class="report-header">
          <div>
            <h2 style="margin: 0; font-size: 20px;">面试报告</h2>
            <p style="margin: 4px 0 0; color: var(--c-text-secondary);">
              {{ report.candidateName || '-' }}
              <span v-if="report.candidateId" style="color: #94a3b8;">（ID: {{ report.candidateId }}）</span>
              · {{ report.jobTitle || '-' }} · {{ typeLabel(report.type) }}
              <span v-if="report.scheduledAt"> · {{ formatScheduledAt(report.scheduledAt) }}</span>
            </p>
          </div>
          <div style="display: flex; align-items: center; gap: 10px;">
            <el-button
              v-if="report.suggestion === 'ADVANCE' && !report.hasNextRound"
              type="primary"
              :icon="Plus"
              @click="goNextRound"
            >
              进入下一轮
            </el-button>
            <el-tag
              v-if="report.suggestion"
              :type="suggestionTagType(report.suggestion)"
              size="large"
              effect="dark"
            >
              {{ suggestionLabel(report.suggestion) }}
            </el-tag>
          </div>
        </div>
      </div>

      <!-- Candidate Info -->
      <div class="sr-section" v-if="report?.candidate" style="margin-bottom: 24px;">
        <div class="sr-section-title">候选人信息</div>
        <div class="candidate-info-grid">
          <div class="ci-item"><span class="ci-label">姓名</span><span class="ci-value">{{ report.candidateName || '-' }}</span></div>
          <div class="ci-item"><span class="ci-label">候选人ID</span><span class="ci-value">{{ report.candidateId || '-' }}</span></div>
          <div class="ci-item"><span class="ci-label">性别</span><span class="ci-value">{{ genderLabel(report.candidate.gender) }}</span></div>
          <div class="ci-item"><span class="ci-label">年龄</span><span class="ci-value">{{ report.candidate.age ?? '-' }}岁</span></div>
          <div class="ci-item"><span class="ci-label">学历</span><span class="ci-value">{{ report.candidate.education || '-' }}</span></div>
          <div class="ci-item"><span class="ci-label">毕业院校</span><span class="ci-value">{{ report.candidate.school || '-' }}</span></div>
          <div class="ci-item"><span class="ci-label">专业</span><span class="ci-value">{{ report.candidate.major || '-' }}</span></div>
          <div class="ci-item"><span class="ci-label">所在城市</span><span class="ci-value">{{ report.candidate.city || '-' }}</span></div>
          <div class="ci-item"><span class="ci-label">手机号</span><span class="ci-value">{{ report.candidate.phone || '-' }}</span></div>
          <div class="ci-item"><span class="ci-label">邮箱</span><span class="ci-value">{{ report.candidate.email || '-' }}</span></div>
          <div class="ci-item"><span class="ci-label">当前公司</span><span class="ci-value">{{ report.candidate.currentCompany || '-' }}</span></div>
          <div class="ci-item"><span class="ci-label">当前职位</span><span class="ci-value">{{ report.candidate.currentPosition || '-' }}</span></div>
          <div class="ci-item"><span class="ci-label">工作年限</span><span class="ci-value">{{ report.candidate.yearsOfExperience ? report.candidate.yearsOfExperience + '年' : '-' }}</span></div>
        </div>
      </div>

      <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 24px; margin-bottom: 24px;">
        <!-- Radar Chart -->
        <div class="sr-section">
          <div class="sr-section-title">六维度评估</div>
          <el-empty v-if="dimensionScores.length === 0" description="暂无评估数据" :image-size="60" />
          <div v-else style="height: 320px;">
            <canvas ref="radarChartRef"></canvas>
          </div>
        </div>

        <!-- Overall Score Gauge & Details -->
        <div class="sr-section">
          <div class="sr-section-title">总体评估</div>
          <el-empty v-if="dimensionScores.length === 0" description="暂无评估数据" :image-size="60" />
          <div v-else style="display: flex; flex-direction: column; align-items: center;">
            <!-- Score Gauge -->
            <div class="score-gauge">
              <svg viewBox="0 0 140 80" width="200" height="120">
                <path d="M 10 70 A 60 60 0 0 1 130 70" fill="none" stroke="#e2e8f0" stroke-width="12" stroke-linecap="round" />
                <path
                  d="M 10 70 A 60 60 0 0 1 130 70"
                  fill="none"
                  :stroke="gaugeColor(report?.overallScore || 0)"
                  stroke-width="12"
                  stroke-linecap="round"
                  :stroke-dasharray="`${(report?.overallScore || 0) * 1.884} 188.4`"
                />
              </svg>
              <div class="score-gauge-text">
                <span class="score-gauge-value">{{ report?.overallScore }}</span>
                <span class="score-gauge-label">分</span>
              </div>
            </div>

            <!-- Dimension Scores -->
            <div style="width: 100%; margin-top: 16px;">
              <div
                v-for="dim in dimensionScores"
                :key="dim.key"
                class="dim-score-item"
              >
                <span class="dim-score-label">{{ dim.label }}</span>
                <el-progress
                  :percentage="dim.percentage"
                  :color="dim.color"
                  :stroke-width="8"
                  style="flex: 1;"
                />
                <span class="dim-score-value">{{ dim.percentage }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Strengths & Weaknesses -->
      <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 24px; margin-bottom: 24px;">
        <div class="sr-section">
          <div class="sr-section-title" style="color: var(--c-success);">优势</div>
          <el-empty v-if="toArray(report?.strengths).length === 0" description="暂无评估数据" :image-size="40" />
          <ul v-else style="list-style: none; padding: 0;">
            <li v-for="(s, i) in toArray(report?.strengths)" :key="'s' + i" class="sw-item">
              <el-icon style="color: var(--c-success); margin-right: 8px;"><Check /></el-icon>
              {{ s }}
            </li>
          </ul>
        </div>
        <div class="sr-section">
          <div class="sr-section-title" style="color: var(--c-danger);">待改进</div>
          <el-empty v-if="toArray(report?.weaknesses).length === 0" description="暂无评估数据" :image-size="40" />
          <ul v-else style="list-style: none; padding: 0;">
            <li v-for="(w, i) in toArray(report?.weaknesses)" :key="'w' + i" class="sw-item">
              <el-icon style="color: var(--c-warning); margin-right: 8px;"><WarningFilled /></el-icon>
              {{ w }}
            </li>
          </ul>
        </div>
      </div>

      <!-- Dimension Details -->
      <div class="sr-section" v-if="report?.dimensions?.length">
        <div class="sr-section-title">维度详情</div>
        <el-table :data="report.dimensions" stripe>
          <el-table-column prop="name" label="评估维度" width="160" />
          <el-table-column prop="score" label="得分" width="100">
            <template #default="{ row }">
              <span style="font-weight: 600;">{{ row.score }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="weight" label="权重(%)" width="100">
            <template #default="{ row }">
              <span>{{ row.weight }}%</span>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- Summary -->
      <div class="sr-section" v-if="report?.feedback">
        <div class="sr-section-title">综合评语</div>
        <p style="font-size: 14px; color: var(--c-text-secondary); line-height: 1.8; white-space: pre-wrap;">{{ report.feedback }}</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Check, WarningFilled, Plus } from '@element-plus/icons-vue'
import { Chart, registerables } from 'chart.js'
import { ElMessage } from 'element-plus'
import { getReport } from '@/api/interview'
import type { InterviewReportVO } from '@/types/models'

Chart.register(...registerables)

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const report = ref<InterviewReportVO | null>(null)
const radarChartRef = ref<HTMLCanvasElement>()
let radarChart: Chart | null = null
const pollTimer = ref<ReturnType<typeof setInterval> | null>(null)
const pollCount = ref(0)
const pollMax = 40 // 最多轮询40次（120秒）

function toArray(val: unknown): string[] {
  if (!val) return []
  if (Array.isArray(val)) return val
  if (typeof val === 'string') return val.split('\n').filter(Boolean)
  return []
}

const dimColors: Record<string, string> = {
  '技术深度': '#4f46e5', '沟通表达': '#0ea5e9', '问题解决': '#059669',
  '团队协作': '#d97706', '抗压能力': '#dc2626', '学习能力': '#8b5cf6',
}

const dimensionScores = computed(() => {
  if (!report.value?.dimensions) return []
  return report.value.dimensions.map(d => ({
    key: d.name,
    label: d.name,
    score: (d.score || 0) / 10,       // 0-100 -> 0-10 for radar chart
    percentage: d.score || 0,          // 0-100 for progress bar
    color: dimColors[d.name] || '#6b7280',
  }))
})

function gaugeColor(score: number): string {
  if (score >= 80) return '#059669'
  if (score >= 60) return '#d97706'
  return '#dc2626'
}

function suggestionTagType(s: string): 'success' | 'warning' | 'danger' | 'info' {
  const map: Record<string, 'success' | 'warning' | 'danger' | 'info'> = {
    ADVANCE: 'success', RETEST: 'warning', REJECT: 'danger',
  }
  return map[s] || 'info'
}

function suggestionLabel(s: string): string {
  const map: Record<string, string> = {
    ADVANCE: '建议进入下一轮', RETEST: '建议重新评估', REJECT: '建议淘汰',
  }
  return map[s] || s
}

function typeLabel(t: number | undefined): string {
  const map: Record<number, string> = {
    0: '电话面试', 1: '视频面试', 2: '现场面试',
    3: 'AI面试', 4: '技术面试', 5: 'HR面试', 6: '领导面试',
  }
  return map[t ?? -1] || '面试'
}

function genderLabel(g: number | undefined): string {
  const map: Record<number, string> = { 0: '未知', 1: '男', 2: '女' }
  return map[g ?? 0] || '未知'
}

function formatScheduledAt(dateStr: string): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function goNextRound() {
  if (!report.value) return
  const nextRound = (report.value.round || 1) + 1
  router.push({
    path: '/interviews',
    query: {
      autoCreate: 'true',
      candidateId: String(report.value.candidateId),
      candidateName: report.value.candidateName || '',
      jobId: String(report.value.jobId || ''),
      jobTitle: report.value.jobTitle || '',
      round: String(nextRound),
    },
  })
}

function renderRadarChart() {
  if (!radarChartRef.value || !report.value) return
  if (radarChart) radarChart.destroy()

  const dims = dimensionScores.value
  radarChart = new Chart(radarChartRef.value, {
    type: 'radar',
    data: {
      labels: dims.map(d => d.label),
      datasets: [{
        label: '候选人得分',
        data: dims.map(d => d.score),
        backgroundColor: 'rgba(79, 70, 229, 0.15)',
        borderColor: '#4f46e5',
        borderWidth: 2,
        pointBackgroundColor: '#4f46e5',
        pointRadius: 5,
        pointBorderWidth: 2,
        pointBorderColor: '#fff',
      }],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      scales: {
        r: {
          min: 0,
          max: 10,
          ticks: { stepSize: 2, font: { size: 10 }, backdropColor: 'transparent' },
          pointLabels: { font: { size: 12 } },
        },
      },
      plugins: { legend: { display: false } },
    },
  })
}

function stopPolling() {
  if (pollTimer.value) {
    clearInterval(pollTimer.value)
    pollTimer.value = null
  }
}

function startPolling() {
  stopPolling()
  pollCount.value = 0
  pollTimer.value = setInterval(async () => {
    pollCount.value++
    if (pollCount.value > pollMax) {
      stopPolling()
      ElMessage.warning('AI评估处理时间较长，请稍后手动刷新页面')
      return
    }
    try {
      const interviewId = route.params.id as string
      const r = await getReport(interviewId)
      report.value = r
      if (r.evaluationStatus === 'COMPLETED' || r.evaluationStatus === 'FAILED') {
        stopPolling()
        if (r.evaluationStatus === 'FAILED') {
          ElMessage.warning('AI评估失败，显示为基于反馈的基础评估')
        }
        setTimeout(() => renderRadarChart(), 100)
      }
    } catch {
      // 轮询静默失败
    }
  }, 3000)
}

async function loadReport() {
  loading.value = true
  try {
    const interviewId = route.params.id as string
    report.value = await getReport(interviewId)
    if (report.value.evaluationStatus === 'PROCESSING') {
      // AI 评估中，启动轮询
      startPolling()
    } else if (report.value.evaluationStatus === 'FAILED') {
      ElMessage.warning('AI评估失败，显示为基于反馈的基础评估')
    }
    setTimeout(() => renderRadarChart(), 100)
  } catch {
    const interviewId = route.params.id as string
    report.value = {
      interviewId,
      candidateId: '10001',
      candidateName: '张明', jobTitle: '高级前端开发工程师', jobId: '20001', type: 4, round: 1,
      scheduledAt: '2026-07-22T10:00:00',
      candidate: {
        gender: 1, age: 28, education: '本科', school: '清华大学',
        major: '计算机科学与技术', city: '北京', phone: '138****6789',
        email: 'zhangming@example.com', currentCompany: '字节跳动',
        currentPosition: '高级前端工程师', yearsOfExperience: 5,
      },
      overallScore: 85, result: 0, suggestion: 'ADVANCE',
      feedback: '候选人张明整体面试表现优秀。技术基础扎实，在前端架构设计方面有深入的理解和实践经验。沟通表达清晰，能够准确阐述技术方案。虽然在高并发处理方面经验略有不足，但学习能力强，可快速成长。建议进入下一轮面试（总监面）。',
      dimensions: [
        { name: '技术深度', weight: 25, score: 90 },
        { name: '沟通表达', weight: 20, score: 80 },
        { name: '问题解决', weight: 20, score: 85 },
        { name: '团队协作', weight: 15, score: 75 },
        { name: '抗压能力', weight: 10, score: 70 },
        { name: '学习能力', weight: 10, score: 90 },
      ],
      strengths: '前端技术基础扎实，Vue和React双框架均有深入理解\n项目经验丰富，有大型复杂项目架构经验\n学习能力强，能快速掌握新技术栈',
      weaknesses: '后端知识储备不足，对数据库优化理解有限\n高并发场景实际经验较少',
      hasNextRound: false,
    }
    setTimeout(() => renderRadarChart(), 100)
  } finally {
    loading.value = false
  }
}

onMounted(() => loadReport())
onBeforeUnmount(() => {
  stopPolling()
  if (radarChart) radarChart.destroy()
})
</script>

<style scoped>
.candidate-info-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px 24px;
}
.ci-item {
  display: flex;
  align-items: center;
  padding: 6px 0;
}
.ci-label {
  font-size: 13px;
  color: var(--c-text-secondary);
  min-width: 64px;
}
.ci-value {
  font-size: 13px;
  color: var(--c-text);
  font-weight: 500;
}

.report-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.score-gauge {
  position: relative;
  display: flex;
  justify-content: center;
}

.score-gauge-text {
  position: absolute;
  bottom: -5px;
  text-align: center;
}

.score-gauge-value {
  font-size: 32px;
  font-weight: 700;
  color: var(--c-text);
}

.score-gauge-label {
  font-size: 14px;
  color: var(--c-text-secondary);
  margin-left: 2px;
}

.dim-score-item {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.dim-score-label {
  width: 70px;
  font-size: 13px;
  color: var(--c-text-secondary);
  text-align: right;
}

.dim-score-value {
  width: 30px;
  font-size: 13px;
  font-weight: 600;
  color: var(--c-text);
}

.sw-item {
  display: flex;
  align-items: flex-start;
  padding: 8px 0;
  font-size: 13px;
  color: var(--c-text-secondary);
  line-height: 1.6;
}

.sw-item .el-icon {
  margin-top: 3px;
  flex-shrink: 0;
}
</style>

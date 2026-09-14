<template>
  <div class="interview-page">
    <div class="sr-page-header">
      <h1>面试管理</h1>
      <p>安排和管理候选人面试</p>
    </div>

    <!-- Stats -->
    <div class="sr-stat-cards">
      <div class="sr-stat-card" v-for="stat in interviewStats" :key="stat.key"
        :style="{ borderLeft: `3px solid ${stat.color}` }">
        <div class="stat-icon" :style="{ background: stat.bgColor, color: stat.color }">
          <el-icon :size="20"><component :is="stat.icon" /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stat.value }}</div>
          <div class="stat-label">{{ stat.label }}</div>
        </div>
      </div>
    </div>

    <!-- Filter & Table -->
    <div class="sr-section">
      <div class="sr-toolbar">
        <div class="sr-filter-bar">
          <el-input v-model="filters.candidateId" placeholder="候选人ID" clearable style="width: 140px" @change="loadInterviews" />
          <el-input v-model="filters.candidateName" placeholder="候选人姓名" clearable style="width: 140px" @change="loadInterviews" />
          <el-input v-model="filters.interviewerName" placeholder="面试官" clearable style="width: 140px" @change="loadInterviews" />
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 240px"
            @change="onDateRangeChange"
          />
          <el-select v-model="filters.type" placeholder="面试类型" clearable @change="loadInterviews">
            <el-option label="全部" value="" />
            <el-option label="AI面试" value="AI" />
            <el-option label="技术面试" value="TECH" />
            <el-option label="HR面试" value="HR" />
            <el-option label="领导面试" value="LEADERSHIP" />
          </el-select>
          <el-select v-model="filters.status" placeholder="状态" clearable @change="loadInterviews">
            <el-option label="全部" value="" />
            <el-option label="已安排" value="SCHEDULED" />
            <el-option label="进行中" value="IN_PROGRESS" />
            <el-option label="已完成" value="COMPLETED" />
            <el-option label="已取消" value="CANCELLED" />
          </el-select>
        </div>
        <el-button type="primary" :icon="Plus" @click="openCreateDialog">安排面试</el-button>
      </div>

      <el-table v-loading="loading" :data="interviews" stripe empty-text="暂无面试安排" :row-class-name="getRowClassName">
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="candidateId" label="候选人ID" width="200" />
        <el-table-column prop="candidateName" label="候选人" width="120" />
        <el-table-column prop="jobTitle" label="应聘职位" min-width="160" />
        <el-table-column prop="round" label="轮次" width="90">
          <template #default="{ row }">
            第{{ row.round || 1 }}轮
            <el-tag v-if="row.isFinalRound === 1" type="warning" size="small" effect="dark" style="margin-left: 4px">终面</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="typeLabel" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.type === 3 ? 'success' : row.type === 4 ? 'primary' : 'info'" size="small">
              {{ row.typeLabel || 'HR面试' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="interviewerName" label="面试官" width="110" />
        <el-table-column prop="scheduledAt" label="面试时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.scheduledAt) }}</template>
        </el-table-column>
        <el-table-column prop="duration" label="时长(分钟)" width="100" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="({ 0: 'info', 1: 'warning', 2: 'success', 3: 'danger' } as Record<number, 'info' | 'warning' | 'success' | 'danger'>)[row.status as number] || 'info'" size="small">
              {{ row.statusLabel }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="结果" width="80">
          <template #default="{ row }">
            <template v-if="row.status === 2">
              <el-tag :type="row.result === 0 ? 'success' : row.result === 2 ? 'warning' : 'danger'" size="small">
                {{ row.result === 0 ? '通过' : row.result === 2 ? '待定' : '未通过' }}
              </el-tag>
            </template>
            <span v-else style="color: var(--c-text-disabled)">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 2 && row.result === 0 && row.isFinalRound === 1" size="small" type="success"
              @click="$router.push({ path: '/offers', query: { candidateId: row.candidateId, candidateName: row.candidateName, jobId: row.jobId, jobTitle: row.jobTitle } })">
              <el-icon style="margin-right:2px"><Select /></el-icon> 发起Offer
            </el-button>
            <el-button v-if="row.status === 2" size="small" text type="primary" @click="$router.push(`/interviews/${row.id}/report`)">
              查看报告
            </el-button>
            <el-button v-if="isManualInterview(row) && row.status === 0" size="small" text type="warning" @click="handleStartInterview(row)">开始面试</el-button>
            <el-button v-if="isManualInterview(row) && row.status === 1" size="small" text type="primary" @click="openResultDialog(row)">录入结果</el-button>
            <el-button size="small" text type="primary" @click="openQuestionsDialog(row)">题目</el-button>
            <el-button v-if="row.status !== 3 && row.status !== 2" size="small" text type="success" @click="openEditDialog(row)">编辑</el-button>
            <el-button v-if="row.status === 0" size="small" text type="warning" @click="handleCancel(row)">
              取消
            </el-button>
            <el-button v-if="row.status === 2 || row.status === 3" size="small" text type="danger" @click="handleDelete(row)">
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

    <!-- Result Entry Dialog (面试反馈表) -->
    <el-dialog v-model="resultDialogVisible" width="680px" :close-on-click-modal="false" class="feedback-dialog">
      <template #header>
        <div class="fd-header">
          <span class="fd-header-title">面试反馈表</span>
          <span class="fd-header-sub">请根据面试实际情况填写评价</span>
        </div>
      </template>

      <!-- Candidate Info Card -->
      <div class="fd-candidate-card" v-if="currentResultRow">
        <div class="fd-candidate-avatar">
          {{ (currentResultRow.candidateName || '?')[0] }}
        </div>
        <div class="fd-candidate-meta">
          <div class="fd-candidate-name">{{ currentResultRow.candidateName }}</div>
          <div class="fd-candidate-detail">
            <span>{{ currentResultRow.jobTitle }}</span>
            <span class="fd-dot">·</span>
            <span>{{ currentResultRow.typeLabel || '面试' }}</span>
            <span class="fd-dot">·</span>
            <span>第{{ currentResultRow.round || 1 }}轮</span>
          </div>
        </div>
        <div class="fd-candidate-score" v-if="overallScore > 0">
          <div class="fd-score-ring" :class="scoreLevelClass">
            <span class="fd-score-num">{{ overallScore }}</span>
            <span class="fd-score-unit">分</span>
          </div>
          <div class="fd-score-label">{{ scoreLevelLabel }}</div>
        </div>
        <div class="fd-candidate-score fd-candidate-score--empty" v-else>
          <div class="fd-score-ring empty">
            <span class="fd-score-num">-</span>
            <span class="fd-score-unit">待评</span>
          </div>
        </div>
      </div>

      <!-- Dimension Ratings -->
      <div class="fd-section">
        <div class="fd-section-title">维度评分</div>
        <div class="fd-dimensions">
          <div class="fd-dim-row" v-for="dim in ratingDimensions" :key="dim.key">
            <div class="fd-dim-header">
              <span class="fd-dim-label">{{ dim.label }}</span>
              <span class="fd-dim-score" :class="{ 'fd-dim-scored': feedbackForm[dim.key] > 0 }">
                {{ feedbackForm[dim.key] > 0 ? feedbackForm[dim.key] + ' / 5' : '未评' }}
              </span>
            </div>
            <div class="fd-dim-stars" @click="(e: Event) => handleStarClick(dim.key, e)">
              <span
                v-for="n in 5"
                :key="n"
                class="fd-star"
                :class="{ active: (feedbackForm[dim.key] || 0) >= n }"
                :data-value="n"
              >
                <el-icon :size="18"><StarFilled v-if="(feedbackForm[dim.key] || 0) >= n" /><Star v-else /></el-icon>
              </span>
            </div>
            <div class="fd-dim-bar">
              <div class="fd-dim-bar-fill" :style="{ width: ((feedbackForm[dim.key] || 0) / 5 * 100) + '%' }"></div>
            </div>
          </div>
        </div>
      </div>

      <!-- Hire Recommendation -->
      <div class="fd-section">
        <div class="fd-section-title">录用建议</div>
        <div class="fd-hire-options">
          <div
            v-for="opt in hireOptions"
            :key="opt.value"
            class="fd-hire-card"
            :class="{ selected: feedbackForm.hireRecommendation === opt.value }"
            @click="feedbackForm.hireRecommendation = opt.value"
          >
            <div class="fd-hire-icon">
              <el-icon :size="20"><component :is="opt.icon" /></el-icon>
            </div>
            <div class="fd-hire-text">
              <div class="fd-hire-label">{{ opt.label }}</div>
              <div class="fd-hire-desc">{{ opt.desc }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- Key Info: Salary & Onboard Date -->
      <div class="fd-section">
        <div class="fd-section-title">关键信息</div>
        <div class="fd-keyinfo">
          <div class="fd-keyinfo-row">
            <div class="fd-keyinfo-item">
              <span class="fd-keyinfo-label">期望月薪（最低）</span>
              <div class="fd-keyinfo-input-group">
                <el-input-number
                  v-model="feedbackForm.expectedSalaryMin"
                  :min="0" :max="200000" :step="1000"
                  placeholder="如 15000"
                  controls-position="right"
                  style="flex: 1"
                />
                <span class="fd-keyinfo-unit">元/月</span>
              </div>
            </div>
            <div class="fd-keyinfo-divider">—</div>
            <div class="fd-keyinfo-item">
              <span class="fd-keyinfo-label">期望月薪（最高）</span>
              <div class="fd-keyinfo-input-group">
                <el-input-number
                  v-model="feedbackForm.expectedSalaryMax"
                  :min="0" :max="200000" :step="1000"
                  placeholder="如 25000"
                  controls-position="right"
                  style="flex: 1"
                />
                <span class="fd-keyinfo-unit">元/月</span>
              </div>
            </div>
          </div>
          <div class="fd-keyinfo-row">
            <div class="fd-keyinfo-item fd-keyinfo-item--wide">
              <span class="fd-keyinfo-label">预计可入职日期</span>
              <el-date-picker
                v-model="feedbackForm.availableDate"
                type="date"
                placeholder="选择日期"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </div>
          </div>
          <div class="fd-keyinfo-row">
            <div class="fd-keyinfo-item fd-keyinfo-item--wide">
              <span class="fd-keyinfo-label">特殊要求</span>
              <el-input
                v-model="feedbackForm.specialRequirements"
                type="textarea"
                :rows="2"
                maxlength="500"
                show-word-limit
                placeholder="如：期望远程办公、弹性工作制、特殊福利、住房补贴、股权激励等..."
              />
            </div>
          </div>
        </div>
      </div>

      <!-- Comments -->
      <div class="fd-section">
        <div class="fd-section-title">
          <span>面试评语</span>
          <el-button link type="primary" size="small" class="fd-comment-example-btn" @click="fillSampleComment">
            填入Java高级工程师评语示例
          </el-button>
        </div>
        <el-input
          v-model="feedbackForm.comments"
          type="textarea"
          :rows="6"
          maxlength="5000"
          show-word-limit
          placeholder="请详细记录候选人的表现、亮点、不足及综合评价..."
          class="fd-textarea"
        />
      </div>

      <template #footer>
        <div class="fd-footer">
          <el-button @click="resultDialogVisible = false" class="fd-btn-cancel">取消</el-button>
          <el-button type="primary" :loading="resultSubmitting" @click="handleResultSubmit" class="fd-btn-submit">
            提交反馈
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- AI 面试题目 Dialog -->
    <el-dialog v-model="questionsDialogVisible" title="AI 面试题目" width="640px" :close-on-click-modal="false">
      <div v-if="questionsStatus === 1" style="margin-bottom: 12px;">
        <el-alert type="warning" :closable="false" show-icon
          title="AI 出题中，请稍后刷新查看" />
      </div>
      <div v-else-if="questionsStatus === 3" style="margin-bottom: 12px;">
        <el-alert type="error" :closable="false" show-icon
          title="AI 出题失败，当前展示的是内置模板题目" />
      </div>
      <div v-loading="questionsLoading" style="min-height: 80px;">
        <ol v-if="aiQuestions.length" class="sq-question-list">
          <li v-for="(q, idx) in aiQuestions" :key="idx" class="sq-question-item">
            <span class="sq-question-no">{{ idx + 1 }}.</span>
            <span>{{ q }}</span>
          </li>
        </ol>
        <el-empty v-else-if="!questionsLoading" description="暂无题目" :image-size="60" />
      </div>
      <template #footer>
        <el-button v-if="questionsStatus === 1" :loading="questionsLoading" @click="refreshQuestions">刷新</el-button>
        <el-button type="primary" @click="questionsDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- Create Dialog -->
    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑面试' : '安排面试'" width="620px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="候选人" prop="candidateId">
          <el-select v-model="form.candidateId" placeholder="请选择候选人" filterable style="width: 100%">
            <el-option v-for="c in candidates" :key="c.id" :label="`${c.name} - ${c.jobTitle || ''}`" :value="String(c.id)" />
          </el-select>
        </el-form-item>
        <el-form-item label="应聘职位" prop="jobId">
          <el-select v-model="form.jobId" placeholder="请选择职位" style="width: 100%">
            <el-option v-for="j in jobs" :key="j.id" :label="j.title" :value="String(j.id)" />
          </el-select>
        </el-form-item>
        <el-form-item label="面试类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio-button value="AI">AI面试</el-radio-button>
            <el-radio-button value="TECH">技术面试</el-radio-button>
            <el-radio-button value="HR">HR面试</el-radio-button>
            <el-radio-button value="LEADERSHIP">领导面试</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="面试轮次" prop="round">
              <el-select v-model="form.round" placeholder="选择轮次" style="width: 100%">
                <el-option v-for="r in 7" :key="r" :label="`第${r}轮`" :value="r"
                  :disabled="existingRounds.includes(r)" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否终面">
              <el-checkbox v-model="form.isFinalRound" :true-value="1" :false-value="0" :disabled="form.round >= 7">本轮为最终面试</el-checkbox>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="面试官" prop="interviewerId">
              <el-select v-model="form.interviewerId" placeholder="请选择面试官" filterable style="width: 100%">
                <el-option v-for="u in users" :key="u.id" :label="u.realName || u.name || u.username" :value="String(u.id)" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="面试时间" prop="scheduledAt">
              <el-date-picker
                v-model="form.scheduledAt" type="datetime" placeholder="选择日期时间"
                style="width: 100%" format="YYYY-MM-DD HH:mm" value-format="YYYY-MM-DD HH:mm:ss"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="面试时长" prop="duration">
              <el-select v-model="form.duration" placeholder="选择时长" style="width: 100%">
                <el-option label="30分钟" :value="30" />
                <el-option label="45分钟" :value="45" />
                <el-option label="60分钟" :value="60" />
                <el-option label="90分钟" :value="90" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">{{ editingId ? '保存修改' : '确认安排' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, nextTick, computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { getInterviews, createInterview, cancelInterview, updateInterview, deleteInterview, getInterviewStats, submitResult, startInterview, submitFeedback, getCandidateNextRound, getInterviewQuestions } from '@/api/interview'
import { getInterviewers } from '@/api/system'
import { getCandidates, updateCandidate } from '@/api/candidate'
import { getJobs } from '@/api/job'
import { formatDateTime } from '@/utils/format'
import type { InterviewVO, UserVO, InterviewStatsVO, CandidateVO, JobVO } from '@/types/models'
import { VideoCamera, Timer, Check, CircleClose, Star, StarFilled, Select, CloseBold, CircleCheckFilled, RemoveFilled } from '@element-plus/icons-vue'

const loaded = ref(false)
const route = useRoute()
const loading = ref(false)
const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const page = ref(1)
const size = ref(10)
const total = ref(0)
const interviews = ref<InterviewVO[]>([])
let aiPollTimer: ReturnType<typeof setInterval> | null = null
const users = ref<UserVO[]>([])
const candidates = ref<CandidateVO[]>([])
const jobs = ref<JobVO[]>([])
const editingId = ref<string | null>(null)

const filters = reactive({ type: '', status: '', candidateId: '', candidateName: '', interviewerName: '', startDate: '', endDate: '' })
const dateRange = ref<[string, string] | null>(null)
const existingRounds = ref<number[]>([])

function onDateRangeChange(val: [string, string] | null) {
  if (val && val.length === 2) {
    filters.startDate = val[0]
    filters.endDate = val[1]
  } else {
    filters.startDate = ''
    filters.endDate = ''
  }
  loadInterviews()
}

// ─── Result Entry Dialog (面试反馈表) ───
const resultDialogVisible = ref(false)
const resultSubmitting = ref(false)
const questionsDialogVisible = ref(false)
const questionsLoading = ref(false)
const aiQuestions = ref<string[]>([])
const questionsStatus = ref<number>(0)
let currentQuestionsId: string | null = null

/** 打开 AI 面试题目弹窗。 */
function openQuestionsDialog(row: InterviewVO) {
  currentQuestionsId = row.id
  questionsStatus.value = row.aiQuestionStatus ?? 0
  aiQuestions.value = []
  questionsDialogVisible.value = true
  void refreshQuestions()
}

/** 刷新/加载 AI 面试题目。 */
async function refreshQuestions() {
  if (!currentQuestionsId) return
  questionsLoading.value = true
  try {
    aiQuestions.value = await getInterviewQuestions(currentQuestionsId)
  } catch {
    ElMessage.error('加载面试题目失败')
  } finally {
    questionsLoading.value = false
  }
}
const currentResultRow = ref<InterviewVO | null>(null)
const feedbackForm = reactive<Record<string, any>>({
  techRating: 0,
  commRating: 0,
  solveRating: 0,
  learnRating: 0,
  teamRating: 0,
  comments: '',
  hireRecommendation: 2,
  expectedSalaryMin: undefined as number | undefined,
  expectedSalaryMax: undefined as number | undefined,
  availableDate: '' as string,
  specialRequirements: '' as string,
})

const ratingDimensions = [
  { key: 'techRating', label: '技术深度', desc: '专业知识掌握程度、技术广度与深度', weight: '25%' },
  { key: 'commRating', label: '沟通表达', desc: '逻辑清晰度、语言组织与表达力', weight: '20%' },
  { key: 'solveRating', label: '问题解决', desc: '分析问题、解决思路与应变能力', weight: '20%' },
  { key: 'teamRating', label: '团队协作', desc: '合作意识、冲突处理与影响力', weight: '15%' },
  { key: 'learnRating', label: '学习能力', desc: '新技术接受度、自我驱动成长', weight: '10%' },
]

const hireOptions = [
  { value: 0, label: '强烈推荐', desc: '非常优秀，建议优先录用', icon: CircleCheckFilled },
  { value: 1, label: '推荐录用', desc: '符合要求，可以录用', icon: Select },
  { value: 2, label: '待定', desc: '需要进一步比较或加面', icon: RemoveFilled },
  { value: 3, label: '不推荐', desc: '不符合岗位要求', icon: CloseBold },
]

const overallScore = computed(() => {
  const dims = [feedbackForm.techRating, feedbackForm.commRating, feedbackForm.solveRating, feedbackForm.learnRating, feedbackForm.teamRating]
  const rated = dims.filter((d: number) => d > 0)
  if (rated.length === 0) return 0
  return Math.round(rated.reduce((a: number, b: number) => a + b, 0) / rated.length * 20)
})

const scoreLevelClass = computed(() => {
  if (overallScore.value >= 80) return 'high'
  if (overallScore.value >= 60) return 'mid'
  if (overallScore.value > 0) return 'low'
  return ''
})

const scoreLevelLabel = computed(() => {
  if (overallScore.value >= 85) return '优秀'
  if (overallScore.value >= 70) return '良好'
  if (overallScore.value >= 50) return '一般'
  if (overallScore.value > 0) return '较差'
  return ''
})

function isManualInterview(row: InterviewVO): boolean {
  return row.type === 4 || row.type === 5 || row.type === 6 // 技术/HR/领导面试
}

function getRowClassName({ row }: { row: InterviewVO }): string {
  if (row.isFinalRound === 1) return 'final-round-row'
  return ''
}

function handleStarClick(dimKey: string, e: Event) {
  const target = (e.target as HTMLElement).closest('[data-value]') as HTMLElement | null
  if (target) {
    feedbackForm[dimKey] = parseInt(target.dataset.value || '0')
  }
}

function openResultDialog(row: InterviewVO) {
  currentResultRow.value = row
  feedbackForm.techRating = 0
  feedbackForm.commRating = 0
  feedbackForm.solveRating = 0
  feedbackForm.learnRating = 0
  feedbackForm.teamRating = 0
  feedbackForm.comments = ''
  feedbackForm.hireRecommendation = 2
  feedbackForm.expectedSalaryMin = undefined
  feedbackForm.expectedSalaryMax = undefined
  feedbackForm.availableDate = ''
  feedbackForm.specialRequirements = ''
  resultDialogVisible.value = true
}

function fillSampleComment() {
  feedbackForm.comments = [
    '【技术深度】',
    '候选人具备扎实的Java基础，对JVM内存模型、垃圾回收机制（G1/ZGC）、类加载机制有深入理解。熟悉Spring Boot/Spring Cloud Alibaba微服务架构体系，对分布式系统设计（CAP理论、分布式事务、服务治理）有实际项目经验。熟练使用MySQL，对索引优化、SQL调优、事务隔离级别与MVCC原理有清晰认知。掌握Redis缓存策略、消息队列（RocketMQ）使用场景及可靠性设计。了解DDD领域驱动设计，并在项目中实践过限界上下文划分。',
    '',
    '【问题解决】',
    '面对复杂技术问题时思路清晰，能从根因分析入手逐步拆解。在系统性能优化方面有丰富经验，熟练使用Arthas、JProfiler等工具进行性能诊断和调优。曾在项目中主导过慢SQL治理，将核心接口P99响应时间从800ms优化至120ms。',
    '',
    '【沟通表达】',
    '沟通表达条理清晰，能够用简洁的语言向非技术人员阐述技术方案。具备良好的技术文档编写能力，能有效推动跨团队技术方案的讨论与落地。',
    '',
    '【团队协作】',
    '有指导初中级工程师的经验，代码审查认真细致，注重团队知识沉淀和技术分享。在跨部门协作中表现出良好的协调能力，能主动承担边界模糊的工作。',
    '',
    '【学习能力】',
    '对新技术保持较高的学习热情，主动关注行业动态和技术趋势。曾在业余时间深入学习云原生技术栈（Kubernetes、Istio），并在团队内进行分享推广。',
    '',
    '【不足与风险】',
    '1. 对云原生技术的实践经验仍处于学习阶段，尚未在大型生产环境中主导落地；',
    '2. 在大流量高并发场景下的架构设计经验有待进一步加强；',
    '3. 英语技术文档阅读能力较好，但口语交流能力一般。',
    '',
    '【综合评价】',
    '候选人技术功底深厚，具备高级工程师应有的技术广度和深度，项目经验与岗位要求高度匹配。逻辑清晰，沟通顺畅，团队协作意识强。建议录用，定级P7/高级工程师，可安排交叉面试做最终确认。',
  ].join('\n')
}

async function handleStartInterview(row: InterviewVO) {
  try {
    await startInterview(row.id)
    ElMessage.success('面试已开始')
    loadInterviews()
    loadStats()
  } catch (e) {
    console.error('开始面试失败:', e)
    ElMessage.error('操作失败，请检查服务是否正常运行')
  }
}

async function handleResultSubmit() {
  if (!currentResultRow.value) return
  if (!feedbackForm.comments) {
    ElMessage.warning('请填写面试评语')
    return
  }
  resultSubmitting.value = true
  const row = currentResultRow.value
  try {
    // 1. 提交反馈表（维度评分 + 评语 + 录用建议 + 薪资/入职日期）
    await submitFeedback(row.id, {
      candidateId: row.candidateId,
      techRating: feedbackForm.techRating || undefined,
      commRating: feedbackForm.commRating || undefined,
      solveRating: feedbackForm.solveRating || undefined,
      learnRating: feedbackForm.learnRating || undefined,
      teamRating: feedbackForm.teamRating || undefined,
      comments: feedbackForm.comments,
      hireRecommendation: feedbackForm.hireRecommendation,
      expectedSalaryMin: feedbackForm.expectedSalaryMin,
      expectedSalaryMax: feedbackForm.expectedSalaryMax,
      availableDate: feedbackForm.availableDate || undefined,
      specialRequirements: feedbackForm.specialRequirements || undefined,
    })
    // 2. 持久化候选人薪资期望、可入职日期和特殊要求（供后续Offer环节使用）
    if (feedbackForm.expectedSalaryMin || feedbackForm.expectedSalaryMax || feedbackForm.availableDate || feedbackForm.specialRequirements) {
      const updateData: Record<string, any> = {}
      if (feedbackForm.expectedSalaryMin) updateData.expectedSalaryMin = feedbackForm.expectedSalaryMin
      if (feedbackForm.expectedSalaryMax) updateData.expectedSalaryMax = feedbackForm.expectedSalaryMax
      const remarkParts: string[] = []
      if (feedbackForm.availableDate) remarkParts.push(`预计可入职: ${feedbackForm.availableDate}`)
      if (feedbackForm.specialRequirements) remarkParts.push(`特殊要求: ${feedbackForm.specialRequirements}`)
      if (remarkParts.length > 0) updateData.remark = remarkParts.join('；')
      try {
        await updateCandidate(String(row.candidateId), updateData)
      } catch (e) {
        console.error('更新候选人信息失败:', e)
      }
    }
    // 3. 更新面试结果及综合评分
    const hireToResult: Record<number, number> = { 0: 0, 1: 0, 2: 2, 3: 1 }
    const result = hireToResult[feedbackForm.hireRecommendation] ?? 2
    const dims = [feedbackForm.techRating, feedbackForm.commRating, feedbackForm.solveRating, feedbackForm.learnRating, feedbackForm.teamRating]
    const rated = dims.filter((d: number) => d > 0)
    const avg = rated.length > 0 ? Math.round(rated.reduce((a: number, b: number) => a + b, 0) / rated.length * 20) : 0
    await submitResult(row.id, {
      result: String(result),
      score: avg,
      comment: feedbackForm.comments,
    })
    resultDialogVisible.value = false
    loadInterviews()
    loadStats()
    // 面试通过时处理Offer跳转
    const isPass = feedbackForm.hireRecommendation === 0 || feedbackForm.hireRecommendation === 1
    if (isPass) {
      const isFinal = row.isFinalRound === 1
      if (isFinal) {
        // 终面通过 → 自动跳转到 Offer 创建页，预填候选人信息
        ElMessage({
          message: '终面通过！即将跳转到 Offer 管理页面...',
          type: 'success',
          duration: 2000,
          showClose: true,
        })
        setTimeout(() => {
          window.open(
            `/offers?candidateId=${row.candidateId}&candidateName=${encodeURIComponent(row.candidateName)}&jobId=${row.jobId}&jobTitle=${encodeURIComponent(row.jobTitle)}`,
            '_blank'
          )
        }, 1500)
      } else {
        ElMessage({
          message: '面试通过！请继续安排下一轮面试或前往 Offer 管理创建 Offer',
          type: 'success',
          duration: 5000,
          showClose: true,
        })
      }
    } else {
      ElMessage.success('面试反馈已提交')
    }
  } catch (e) {
    console.error('提交面试反馈失败:', e)
    ElMessage.error('操作失败，请检查服务是否正常运行')
  } finally { resultSubmitting.value = false }
}

// ─── Form ───
const form = reactive({
  candidateId: null as string | null, jobId: null as string | null,
  type: 'AI', round: 1, isFinalRound: 0 as number, interviewerId: null as string | null,
  scheduledAt: '', duration: 45,
})

const formRules: FormRules = {
  candidateId: [{ required: true, message: '请选择候选人', trigger: 'change' }],
  jobId: [{ required: true, message: '请选择职位', trigger: 'change' }],
  type: [{ required: true, message: '请选择面试类型', trigger: 'change' }],
  interviewerId: [{ required: true, message: '请选择面试官', trigger: 'change' }],
  scheduledAt: [{ required: true, message: '请选择面试时间', trigger: 'change' }],
  duration: [{ required: true, message: '请选择时长', trigger: 'change' }],
}

// 选择候选人后自动回填职位和建议下一轮次
watch(() => form.candidateId, async (newId) => {
  if (editingId.value) return // 编辑模式下不自动回填
  if (!newId) { form.jobId = null; existingRounds.value = []; return }
  // 自动回填职位
  const c = candidates.value.find(c => String(c.id) === newId)
  if (c && c.jobId) {
    form.jobId = String(c.jobId)
  }
  // 自动查询建议下一轮次
  try {
    const res = await getCandidateNextRound(newId)
    existingRounds.value = res.existingRounds || []
    form.round = res.nextRound || 1
  } catch {
    existingRounds.value = []
    form.round = 1
  }
})

// 第7轮（末轮）时默认标记为终面
watch(() => form.round, (r) => {
  if (r >= 7) {
    form.isFinalRound = 1
  }
})

function getCandidateName(candidateId: string | null): string {
  if (!candidateId) return ''
  const c = candidates.value.find(c => String(c.id) === candidateId)
  return c?.name || ''
}

function getJobTitleById(jobId: string | null): string {
  if (!jobId) return ''
  const j = jobs.value.find(j => String(j.id) === jobId)
  return j?.title || ''
}

const stats = ref<InterviewStatsVO>({ total: 0, today: 0, passed: 0, cancelled: 0 })

const interviewStats = computed(() => [
  { key: 'total', label: '总面试', value: stats.value.total, color: '#4f46e5', bgColor: '#eef2ff', icon: VideoCamera },
  { key: 'today', label: '今日面试', value: stats.value.today, color: '#0ea5e9', bgColor: '#f0f9ff', icon: Timer },
  { key: 'passed', label: '已通过', value: stats.value.passed, color: '#059669', bgColor: '#ecfdf5', icon: Check },
  { key: 'cancelled', label: '已取消', value: stats.value.cancelled, color: '#dc2626', bgColor: '#fef2f2', icon: CircleClose },
])

function openCreateDialog() {
  editingId.value = null
  existingRounds.value = []
  if (users.value.length === 0) loadUsers()
  if (candidates.value.length === 0) loadCandidates()
  if (jobs.value.length === 0) loadJobs()
  dialogVisible.value = true
  nextTick(() => {
    formRef.value?.resetFields()
    form.isFinalRound = 0
    form.scheduledAt = defaultScheduledAt()
  })
}

function defaultScheduledAt(): string {
  const d = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} 10:00:00`
}

function openCreateDialogWithQuery() {
  const q = route.query
  editingId.value = null
  existingRounds.value = []
  // 如果URL指定了candidateId，先查询建议轮次
  if (q.candidateId) {
    getCandidateNextRound(String(q.candidateId)).then(res => {
      existingRounds.value = res.existingRounds || []
      if (!q.round) {
        form.round = res.nextRound || 1
      }
    }).catch(() => { existingRounds.value = [] })
  }
  dialogVisible.value = true
  nextTick(() => {
    formRef.value?.resetFields()
    form.candidateId = q.candidateId ? String(q.candidateId) : null
    form.jobId = q.jobId ? String(q.jobId) : null
    form.type = 'TECH'
    form.round = q.round ? Number(q.round) : 1
    form.isFinalRound = 0
    form.scheduledAt = defaultScheduledAt()
    form.duration = 45
  })
}

function openEditDialog(row: InterviewVO) {
  editingId.value = row.id
  existingRounds.value = []
  form.candidateId = row.candidateId || null
  form.jobId = row.jobId || null
  form.type = row.type === 3 ? 'AI' : row.type === 4 ? 'TECH' : row.type === 5 ? 'HR' : row.type === 6 ? 'LEADERSHIP' : 'AI'
  form.round = row.round || 1
  form.isFinalRound = row.isFinalRound || 0
  let interviewerId: string | null = null
  const ids = row.interviewerIds
  if (ids) {
    let parsed: string[] = []
    if (typeof ids === 'string') {
      try { parsed = JSON.parse(ids) } catch { /* ignore */ }
    } else if (Array.isArray(ids)) {
      parsed = ids.map(String)
    }
    if (parsed.length > 0) interviewerId = parsed[0]
  }
  form.interviewerId = interviewerId ? String(interviewerId) : null
  form.scheduledAt = row.scheduledAt
  form.duration = row.duration
  if (users.value.length === 0) loadUsers()
  if (candidates.value.length === 0) loadCandidates()
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      if (editingId.value) {
        await updateInterview(editingId.value, {
          candidateId: form.candidateId!, candidateName: getCandidateName(form.candidateId),
          jobId: form.jobId!, jobTitle: getJobTitleById(form.jobId),
          type: form.type, round: form.round, isFinalRound: form.isFinalRound,
          interviewerId: form.interviewerId ?? undefined,
          scheduledAt: form.scheduledAt, duration: form.duration,
        })
        ElMessage.success('面试已更新')
      } else {
        const created = await createInterview({
          candidateId: form.candidateId!, candidateName: getCandidateName(form.candidateId),
          jobId: form.jobId!, jobTitle: getJobTitleById(form.jobId),
          type: form.type, round: form.round, isFinalRound: form.isFinalRound,
          interviewerId: form.interviewerId ?? undefined,
          scheduledAt: form.scheduledAt, duration: form.duration,
        })
        ElMessage.success(created.aiQuestionStatus === 1 ? '面试已安排，AI 出题进行中' : '面试已安排')
        if (created.aiQuestionStatus === 1) {
          ensureAiQuestionPolling()
        }
      }
      dialogVisible.value = false
      loadInterviews()
      loadStats()
    } catch (e) {
      console.error('提交失败:', e)
      // Axios 拦截器已自动显示后端错误消息，此处不再重复提示
    } finally { submitting.value = false }
  })
}

async function handleCancel(row: InterviewVO) {
  await ElMessageBox.confirm('确定取消该面试吗？', '确认取消', { type: 'warning' })
  try {
    await cancelInterview(row.id)
    ElMessage.info('面试已取消')
    loadInterviews()
    loadStats()
  } catch (e) {
    console.error('取消面试失败:', e)
    ElMessage.error('操作失败，请检查服务是否正常运行')
  }
}

async function handleDelete(row: InterviewVO) {
  await ElMessageBox.confirm('确定删除该面试吗？此操作不可恢复。', '确认删除', { type: 'error' })
  try {
    await deleteInterview(row.id)
    ElMessage.success('面试已删除')
    loadInterviews()
    loadStats()
  } catch (e) {
    console.error('删除面试失败:', e)
    ElMessage.error('操作失败，请检查服务是否正常运行')
  }
}

async function loadInterviews() {
  loading.value = true
  try {
    const res = await getInterviews({ page: page.value, size: size.value, ...filters })
    interviews.value = res.records
    total.value = res.total
  } catch (e) {
    console.error('加载面试列表失败:', e)
    ElMessage.error('加载面试列表失败，请检查服务是否正常运行')
  } finally {
    loading.value = false
    // 若存在“AI出题中”的面试，自动轮询直到全部完成或超时
    if (interviews.value.some(r => r.aiQuestionStatus === 1)) {
      ensureAiQuestionPolling()
    }
  }
}

/**
 * 轮询刷新面试列表，直到没有“AI出题中”的记录或达到上限。
 */
function ensureAiQuestionPolling() {
  if (aiPollTimer) return
  let tries = 0
  aiPollTimer = setInterval(() => {
    tries += 1
    loadInterviews()
    const pending = interviews.value.some(r => r.aiQuestionStatus === 1)
    if (!pending || tries >= 24) {
      if (aiPollTimer) {
        clearInterval(aiPollTimer)
        aiPollTimer = null
      }
    }
  }, 5000)
}

onUnmounted(() => {
  if (aiPollTimer) {
    clearInterval(aiPollTimer)
    aiPollTimer = null
  }
})

function handlePageChange(p: number) { if (!loaded.value) return; page.value = p; loadInterviews() }
function handleSizeChange(s: number) { if (!loaded.value) return; size.value = s; page.value = 1; loadInterviews() }

async function loadUsers() {
  try {
    users.value = await getInterviewers()
  } catch { /* 加载用户列表失败静默处理 */ }
}

async function loadCandidates() {
  try {
    const res = await getCandidates({ page: 1, size: 200 })
    candidates.value = res.records
  } catch { /* 静默处理 */ }
}

async function loadJobs() {
  try {
    const res = await getJobs({ page: 1, size: 200, status: 1 })
    jobs.value = res.records
  } catch { /* 静默处理 */ }
}

async function loadStats() {
  try {
    const res = await getInterviewStats()
    stats.value = res
  } catch (e) {
    console.error('加载统计数据失败:', e)
  }
}

onMounted(async () => {
  await Promise.all([loadInterviews(), loadStats(), loadUsers(), loadCandidates(), loadJobs()])
  loaded.value = true
  // 从面试报告页面跳转过来的"进入下一轮"：自动打开创建对话框并预填候选人
  if (route.query.autoCreate === 'true') {
    nextTick(() => {
      openCreateDialogWithQuery()
    })
  }
})
</script>

<style scoped>
/* ================================================================
   Feedback Dialog — Professional Production-Grade Styling
   ================================================================ */

/* -- Dialog header -- */
.fd-header {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.fd-header-title {
  font-size: 17px;
  font-weight: 700;
  color: #0f172a;
}
.fd-header-sub {
  font-size: 12.5px;
  color: #94a3b8;
}

/* -- Candidate Info Card -- */
.fd-candidate-card {
  display: flex;
  align-items: center;
  gap: 14px;
  background: linear-gradient(135deg, #f8fafc 0%, #eff6ff 100%);
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 16px 20px;
  margin-bottom: 20px;
}
.fd-candidate-avatar {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: linear-gradient(135deg, #4f46e5, #7c3aed);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 700;
  flex-shrink: 0;
}
.fd-candidate-meta {
  flex: 1;
  min-width: 0;
}
.fd-candidate-name {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}
.fd-candidate-detail {
  font-size: 12.5px;
  color: #64748b;
  margin-top: 2px;
  display: flex;
  align-items: center;
  gap: 4px;
}
.fd-dot {
  color: #cbd5e1;
}
.fd-candidate-score {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex-shrink: 0;
}
.fd-score-ring {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 3px solid #22c55e;
  background: #f0fdf4;
}
.fd-score-ring.high {
  border-color: #22c55e;
  background: #f0fdf4;
}
.fd-score-ring.mid {
  border-color: #f59e0b;
  background: #fffbeb;
}
.fd-score-ring.low {
  border-color: #ef4444;
  background: #fef2f2;
}
.fd-score-ring.empty {
  border-color: #e2e8f0;
  background: #f8fafc;
}
.fd-score-num {
  font-size: 18px;
  font-weight: 800;
  color: #0f172a;
  line-height: 1;
}
.fd-score-unit {
  font-size: 10px;
  color: #94a3b8;
}
.fd-score-label {
  font-size: 11px;
  font-weight: 600;
  color: #64748b;
  margin-top: 2px;
}
.fd-candidate-score--empty .fd-score-num,
.fd-candidate-score--empty .fd-score-unit {
  color: #94a3b8;
}

/* -- Section -- */
.fd-section {
  margin-bottom: 20px;
}
.fd-section-title {
  font-size: 13px;
  font-weight: 700;
  color: #334155;
  margin-bottom: 10px;
  padding-left: 2px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.fd-section-title::before {
  content: '';
  width: 3px;
  height: 14px;
  background: #4f46e5;
  border-radius: 2px;
  display: inline-block;
}
.fd-comment-example-btn {
  font-size: 11px;
  margin-left: auto;
  color: #818cf8;
  padding: 0 4px;
}

/* -- Dimension Ratings -- */
.fd-dimensions {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 4px 0;
}
.fd-dim-row {
  padding: 10px 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid #f1f5f9;
}
.fd-dim-row:last-child {
  border-bottom: none;
}
.fd-dim-header {
  width: 84px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 1px;
}
.fd-dim-label {
  font-size: 12.5px;
  font-weight: 600;
  color: #334155;
}
.fd-dim-score {
  font-size: 11px;
  color: #94a3b8;
}
.fd-dim-score.fd-dim-scored {
  color: #6366f1;
  font-weight: 600;
}
.fd-dim-stars {
  display: flex;
  gap: 3px;
  cursor: pointer;
  flex-shrink: 0;
}
.fd-star {
  color: #cbd5e1;
  transition: color 0.15s, transform 0.15s;
  display: flex;
}
.fd-star:hover {
  transform: scale(1.15);
}
.fd-star.active {
  color: #f59e0b;
}
.fd-dim-bar {
  flex: 1;
  height: 4px;
  background: #e2e8f0;
  border-radius: 2px;
  overflow: hidden;
}
.fd-dim-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #6366f1, #a78bfa);
  border-radius: 2px;
  transition: width 0.3s ease;
}

/* -- Hire Recommendation Cards -- */
.fd-hire-options {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}
.fd-hire-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 14px 8px;
  border: 2px solid #e2e8f0;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
  text-align: center;
  background: #fff;
}
.fd-hire-card:hover {
  border-color: #a5b4fc;
  background: #f8fafc;
}
.fd-hire-card.selected {
  border-color: #4f46e5;
  background: #eef2ff;
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1);
}
.fd-hire-icon {
  color: #94a3b8;
  transition: color 0.2s;
}
.fd-hire-card.selected .fd-hire-icon {
  color: #4f46e5;
}
.fd-hire-card:nth-child(1).selected .fd-hire-icon { color: #22c55e; }
.fd-hire-card:nth-child(1).selected { border-color: #22c55e; background: #f0fdf4; box-shadow: 0 0 0 3px rgba(34, 197, 94, 0.1); }
.fd-hire-card:nth-child(2).selected .fd-hire-icon { color: #6366f1; }
.fd-hire-card:nth-child(2).selected { border-color: #6366f1; background: #eef2ff; box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1); }
.fd-hire-card:nth-child(3).selected .fd-hire-icon { color: #f59e0b; }
.fd-hire-card:nth-child(3).selected { border-color: #f59e0b; background: #fffbeb; box-shadow: 0 0 0 3px rgba(245, 158, 11, 0.1); }
.fd-hire-card:nth-child(4).selected .fd-hire-icon { color: #ef4444; }
.fd-hire-card:nth-child(4).selected { border-color: #ef4444; background: #fef2f2; box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.1); }
.fd-hire-label {
  font-size: 12.5px;
  font-weight: 600;
  color: #334155;
}
.fd-hire-card.selected .fd-hire-label {
  color: #4f46e5;
}
.fd-hire-card:nth-child(1).selected .fd-hire-label { color: #16a34a; }
.fd-hire-card:nth-child(2).selected .fd-hire-label { color: #4f46e5; }
.fd-hire-card:nth-child(3).selected .fd-hire-label { color: #d97706; }
.fd-hire-card:nth-child(4).selected .fd-hire-label { color: #dc2626; }
.fd-hire-desc {
  font-size: 10.5px;
  color: #94a3b8;
  line-height: 1.3;
}

/* -- Key Info: Salary & Onboard Date -- */
.fd-keyinfo {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 14px 16px;
}
.fd-keyinfo-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.fd-keyinfo-row + .fd-keyinfo-row {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f1f5f9;
}
.fd-keyinfo-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.fd-keyinfo-item--wide {
  flex: unset;
  width: 100%;
}
.fd-keyinfo-input-group {
  display: flex;
  align-items: center;
  gap: 8px;
}
.fd-keyinfo-label {
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
}
.fd-keyinfo-unit {
  font-size: 13px;
  font-weight: 500;
  color: #475569;
  white-space: nowrap;
  flex-shrink: 0;
}
.fd-keyinfo-divider {
  color: #cbd5e1;
  font-size: 14px;
  padding-top: 20px;
  flex-shrink: 0;
}
:deep(.fd-keyinfo .el-textarea__inner) {
  border-radius: 8px;
  border-color: #e2e8f0;
  font-size: 13px;
  resize: none;
}
:deep(.fd-keyinfo .el-textarea__inner:focus) {
  border-color: #4f46e5;
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.06);
}

/* -- Textarea -- */
:deep(.fd-textarea .el-textarea__inner) {
  border-radius: 10px;
  border-color: #e2e8f0;
  font-size: 13px;
  line-height: 1.6;
  resize: none;
  transition: border-color 0.2s, box-shadow 0.2s;
}
:deep(.fd-textarea .el-textarea__inner:focus) {
  border-color: #4f46e5;
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.08);
}
:deep(.fd-textarea .el-input__count) {
  font-size: 11px;
  color: #94a3b8;
}

/* -- Footer -- */
.fd-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
.fd-btn-cancel {
  border-color: #e2e8f0;
  color: #64748b;
  font-weight: 500;
  border-radius: 8px;
}
.fd-btn-submit {
  font-weight: 600;
  border-radius: 8px;
  padding: 0 24px;
}

/* -- Dialog override -- */
:deep(.feedback-dialog .el-dialog__header) {
  padding: 20px 24px 0;
}
:deep(.feedback-dialog .el-dialog__body) {
  padding: 16px 24px 4px;
}
:deep(.feedback-dialog .el-dialog__footer) {
  padding: 0 24px 20px;
}

/* -- 终面行高亮 -- */
:deep(.final-round-row) {
  background: linear-gradient(90deg, #fff7ed 0%, #fffbf0 100%) !important;
}
:deep(.final-round-row td) {
  border-bottom-color: #fcd34d !important;
}

/* -- AI 面试题目列表 -- */
.sq-question-list {
  margin: 0;
  padding-left: 4px;
  list-style: none;
}
.sq-question-item {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  padding: 8px 10px;
  border-radius: 8px;
  background: #f8fafc;
  margin-bottom: 8px;
  line-height: 1.6;
  color: #334155;
}
.sq-question-no {
  color: #4f46e5;
  font-weight: 600;
  flex-shrink: 0;
}
</style>

<template>
  <div class="assessment-page">
    <!-- ================================================================ -->
    <!-- TOP HEADER — Enterprise Logo + Assessment Title + Countdown -->
    <!-- ================================================================ -->
    <header class="app-header" v-if="step !== 'loading' && step !== 'error'">
      <div class="header-inner">
        <div class="header-brand">
          <svg class="brand-logo" width="36" height="36" viewBox="0 0 28 28" fill="none">
            <rect width="28" height="28" rx="6" fill="#1677ff"/>
            <path d="M8 20V8l6 8.5L20 8v12" stroke="#fff" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" fill="none"/>
          </svg>
          <span class="brand-divider" />
          <span class="brand-subtitle">在线测评</span>
        </div>
        <div class="header-timer" v-if="step === 'question' || step === 'confirm' || step === 'submitting'">
          <span class="timer-icon">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
          </span>
          <span class="timer-label">剩余时间</span>
          <span class="timer-value" :class="{ 'timer-warning': remainingSeconds <= 300, 'timer-danger': remainingSeconds <= 60 }">
            {{ formattedTime }}
          </span>
        </div>
      </div>
    </header>

    <!-- ================================================================ -->
    <!-- MAIN CONTENT AREA -->
    <!-- ================================================================ -->
    <main class="app-main" :class="{ 'has-header': step !== 'loading' && step !== 'error' }">
      <div class="content-container">

        <!-- ─── Loading ─── -->
        <div v-if="step === 'loading'" class="state-loading">
          <svg class="loading-logo" width="48" height="48" viewBox="0 0 28 28" fill="none">
            <rect width="28" height="28" rx="6" fill="#1677ff"/>
            <path d="M8 20V8l6 8.5L20 8v12" stroke="#fff" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" fill="none"/>
          </svg>
          <div class="loading-spinner" />
          <p class="loading-text">正在加载测评，请稍候...</p>
        </div>

        <!-- ─── Landing ─── -->
        <div v-else-if="step === 'landing'" class="state-landing">
          <div class="landing-card">
            <div class="landing-head">
              <h1 class="landing-title">{{ assessmentData?.jobTitle }}</h1>
              <span class="landing-type-badge" :style="{ background: typeColor }">{{ assessmentData?.typeLabel }}</span>
            </div>
            <div class="landing-body">
              <div class="landing-row">
                <span class="landing-label">候选人</span>
                <span class="landing-value"><strong>{{ assessmentData?.candidateName }}</strong></span>
              </div>
              <div class="landing-row">
                <span class="landing-label">邮箱</span>
                <span class="landing-value">{{ assessmentData?.candidateEmail }}</span>
              </div>
              <div class="landing-row">
                <span class="landing-label">题目数量</span>
                <span class="landing-value"><strong>{{ assessmentData?.questions?.length || 0 }}</strong> 题</span>
              </div>
              <div class="landing-row">
                <span class="landing-label">答题时长</span>
                <span class="landing-value"><strong>{{ assessmentData?.durationMinutes || 30 }}</strong> 分钟</span>
              </div>
              <div class="landing-row">
                <span class="landing-label">发送时间</span>
                <span class="landing-value">{{ assessmentData?.sentTime || '-' }}</span>
              </div>
            </div>
            <div class="landing-footer">
              <button class="start-btn" @click="startAssessment">
                开始测评
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="5" y1="12" x2="19" y2="12"/><polyline points="12 5 19 12 12 19"/></svg>
              </button>
              <p class="landing-disclaimer">请确保网络稳定，中途退出将丢失答题进度。测评限时完成，请合理分配时间。</p>
            </div>
          </div>
        </div>

        <!-- ─── Questions ─── -->
        <div v-else-if="step === 'question'" class="state-question">
          <!-- Progress bar -->
          <div class="q-progress-bar">
            <div class="q-progress-fill" :style="{ width: progressPercent + '%' }" />
          </div>
          <div class="q-progress-stats">
            <span>第 {{ currentIndex + 1 }}/{{ totalQuestions }} 题</span>
            <span class="q-answered-count">已答 {{ answeredCount }} 题</span>
          </div>

          <!-- Question body -->
          <div class="q-body">
            <div class="q-stem">
              <span class="q-stem-num">{{ currentIndex + 1 }}.</span>
              <span class="q-stem-text">{{ currentQuestion?.questionText }}</span>
              <span class="q-type-badge" :class="'q-type-' + (currentQuestion?.questionType || 'essay')">
                {{ questionTypeName(currentQuestion?.questionType) }}
              </span>
            </div>

            <!-- Essay / Short Answer -->
            <template v-if="currentQuestion?.questionType === 'essay'">
              <div class="q-essay">
                <textarea
                  class="q-textarea"
                  :value="answers[currentQuestion!.questionId] || ''"
                  @input="onEssayInput($event)"
                  placeholder="请输入您的回答..."
                  rows="8"
                />
                <div class="q-essay-hint">
                  请在此处输入您的回答。支持换行，字数不限。
                  <span class="q-essay-count" v-if="(answers[currentQuestion!.questionId] || '').length > 0">
                    已输入 {{ (answers[currentQuestion!.questionId] || '').length }} 字
                  </span>
                </div>
              </div>
            </template>

            <!-- Likert scale -->
            <template v-else-if="assessmentData?.type === 1">
              <div class="q-likert">
                <label
                  v-for="opt in currentQuestion?.options"
                  :key="opt.key"
                  class="q-likert-option"
                  :class="{ 'q-likert-selected': answers[currentQuestion!.questionId] === opt.key }"
                  @click="selectOption(opt.key)"
                >
                  <span class="q-likert-score">{{ opt.key }}</span>
                  <span class="q-likert-label">{{ opt.value }}</span>
                </label>
              </div>
            </template>

            <!-- MCQ: single_choice / multiple_choice / true_false -->
            <template v-else>
              <div class="q-options">
                <label
                  v-for="opt in currentQuestion?.options"
                  :key="opt.key"
                  class="q-option"
                  :class="{ 'q-option-selected': isOptionSelected(opt.key) }"
                  @click="handleOptionClick(opt.key)"
                >
                  <span
                    class="q-option-key"
                    :class="{ 'q-option-key-checkbox': currentQuestion?.questionType === 'multiple_choice' }"
                  >
                    <template v-if="currentQuestion?.questionType === 'multiple_choice' && isOptionSelected(opt.key)">
                      <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
                    </template>
                    <template v-else>{{ opt.key }}</template>
                  </span>
                  <span class="q-option-text">{{ opt.value }}</span>
                </label>
              </div>
              <div
                v-if="currentQuestion?.questionType === 'multiple_choice'"
                class="q-multi-hint"
              >
                多选题，请选择所有正确答案
              </div>
            </template>
          </div>

          <!-- Navigation -->
          <div class="q-nav">
            <button
              v-if="currentIndex > 0"
              class="nav-btn nav-btn-prev"
              @click="prevQuestion"
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6"/></svg>
              上一题
            </button>
            <div class="nav-spacer" />
            <div class="q-dots">
              <span
                v-for="(q, idx) in assessmentData?.questions"
                :key="q.questionId"
                class="q-dot"
                :class="{ 'q-dot-active': idx === currentIndex, 'q-dot-done': !!answers[q.questionId] }"
                @click="goToQuestion(idx)"
              >{{ idx + 1 }}</span>
            </div>
            <div class="nav-spacer" />
            <button
              v-if="currentIndex < totalQuestions - 1"
              class="nav-btn nav-btn-next"
              @click="nextQuestion"
            >
              下一题
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"/></svg>
            </button>
            <button
              v-else
              class="nav-btn nav-btn-submit"
              @click="showConfirm"
            >
              提交答卷
            </button>
          </div>
        </div>

        <!-- ─── Confirm ─── -->
        <div v-else-if="step === 'confirm'" class="state-confirm">
          <div class="confirm-card">
            <div class="confirm-icon">
              <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" style="color:#f59e0b;"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
            </div>
            <h2>确认提交答卷？</h2>
            <p class="confirm-summary">
              已答 <strong>{{ answeredCount }}</strong> / {{ totalQuestions }} 题
              <template v-if="unansweredCount > 0">
                ，<span class="text-danger">{{ unansweredCount }} 题未作答</span>
              </template>
            </p>
            <p class="confirm-hint">提交后将无法修改答案，请仔细检查</p>
            <div class="confirm-actions">
              <button class="btn-secondary" @click="goBack">返回检查</button>
              <button class="btn-primary" @click="doSubmit" :disabled="submitting">
                {{ submitting ? '提交中...' : '确认提交' }}
              </button>
            </div>
          </div>
        </div>

        <!-- ─── Result ─── -->
        <div v-else-if="step === 'result'" class="state-result">
          <div class="result-card">
            <div class="result-icon">
              <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="#2563eb" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
            </div>
            <h1>测评已完成</h1>
            <p class="result-meta">{{ resultData?.jobTitle }} — {{ resultData?.typeLabel }}</p>
            <div class="result-score">
              <span class="score-number">{{ resultData?.score }}</span>
              <span v-if="resultData?.correctCount !== undefined && resultData.correctCount !== null" class="score-detail">
                正确 {{ resultData.correctCount }}/{{ resultData.totalQuestions }} 题
              </span>
            </div>
            <p class="result-desc">{{ resultData?.resultDescription }}</p>
            <button class="btn-primary" @click="finish">关闭页面</button>
          </div>
        </div>

        <!-- ─── Error ─── -->
        <div v-else-if="step === 'error'" class="state-error">
          <div class="error-card">
            <div class="error-icon" :class="{ 'icon-warn': errorIcon === 'warning', 'icon-err': errorIcon === 'error' }">
              <svg v-if="errorIcon === 'warning'" width="56" height="56" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
              <svg v-else width="56" height="56" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
            </div>
            <h1>{{ errorTitle }}</h1>
            <p>{{ errorMessage }}</p>
            <button class="btn-primary" @click="finish">返回首页</button>
          </div>
        </div>

      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { fetchAssessmentByToken, submitAssessment } from '@/api/assessment'
import type { AssessmentPageVO, AssessmentQuestionItem, AssessResultVO } from '@/types/models'

const route = useRoute()
const router = useRouter()

const token = computed(() => route.query.token as string)

// ─── State ───
type Step = 'loading' | 'landing' | 'question' | 'confirm' | 'submitting' | 'result' | 'error'
const step = ref<Step>('loading')
const submitting = ref(false)
const assessmentData = ref<AssessmentPageVO | null>(null)
const resultData = ref<AssessResultVO | null>(null)
const currentIndex = ref(0)
const answers = ref<Record<number, string>>({})
const errorTitle = ref('')
const errorMessage = ref('')
const errorIcon = ref<'warning' | 'error'>('warning')

// ─── Countdown Timer ───
const remainingSeconds = ref(0)
let timerHandle: ReturnType<typeof setInterval> | null = null

const formattedTime = computed(() => {
  const m = Math.floor(remainingSeconds.value / 60)
  const s = remainingSeconds.value % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
})

function startTimer(minutes: number) {
  remainingSeconds.value = minutes * 60
  timerHandle = setInterval(() => {
    if (remainingSeconds.value > 0) {
      remainingSeconds.value--
    } else {
      // Time's up — auto-submit
      if (timerHandle) clearInterval(timerHandle)
      timerHandle = null
      ElMessage.warning('答题时间已到，系统将自动提交答卷')
      doSubmit()
    }
  }, 1000)
}

function stopTimer() {
  if (timerHandle) {
    clearInterval(timerHandle)
    timerHandle = null
  }
}

onBeforeUnmount(() => stopTimer())

// ─── Computed ───
const totalQuestions = computed(() => assessmentData.value?.questions?.length ?? 0)
const currentQuestion = computed(() => assessmentData.value?.questions?.[currentIndex.value] as AssessmentQuestionItem | undefined)
const progressPercent = computed(() =>
  totalQuestions.value > 0 ? Math.round(((currentIndex.value + 1) / totalQuestions.value) * 100) : 0
)
const answeredCount = computed(() => Object.keys(answers.value).filter(k => answers.value[Number(k)] !== '').length)
const unansweredCount = computed(() => totalQuestions.value - answeredCount.value)
const typeColor = computed(() => {
  const t = assessmentData.value?.type
  return t === 0 ? '#2563eb' : t === 1 ? '#059669' : '#d97706'
})

// ─── Lifecycle ───
onMounted(async () => {
  if (!token.value) {
    showError('error', '无效的测评链接', '缺少测评令牌，请通过邮件中的完整链接访问。')
    return
  }
  try {
    assessmentData.value = await fetchAssessmentByToken(token.value)
    step.value = 'landing'
  } catch (e: any) {
    const msg = e?.message || e?.response?.data?.message || ''
    if (msg.includes('已完成')) {
      showError('warning', '该测评已完成', msg || '感谢您的参与，测评结果已提交。')
    } else if (msg.includes('无效') || msg.includes('过期')) {
      showError('warning', '链接无效或已过期', msg || '请联系招聘负责人重新发送测评邀请。')
    } else {
      showError('warning', '无法加载测评', msg || '请稍后重试，或联系招聘负责人。')
    }
  }
})

// ─── Methods ───
function showError(icon: 'warning' | 'error', title: string, msg: string) {
  errorIcon.value = icon
  errorTitle.value = title
  errorMessage.value = msg
  step.value = 'error'
}

function startAssessment() {
  step.value = 'question'
  currentIndex.value = 0
  const duration = assessmentData.value?.durationMinutes || 30
  startTimer(duration)
}

const questionTypeNameMap: Record<string, string> = {
  single_choice: '单选题',
  multiple_choice: '多选题',
  true_false: '判断题',
  essay: '问答题',
  likert: '量表题',
}
function questionTypeName(qt?: string): string {
  return questionTypeNameMap[qt || ''] || ''
}

function isOptionSelected(optKey: string): boolean {
  if (!currentQuestion.value) return false
  const val = answers.value[currentQuestion.value.questionId] || ''
  if (currentQuestion.value.questionType === 'multiple_choice') {
    return val.split(',').includes(optKey)
  }
  return val === optKey
}

function handleOptionClick(optKey: string) {
  if (!currentQuestion.value) return
  const qid = currentQuestion.value.questionId
  const qt = currentQuestion.value.questionType
  if (qt === 'multiple_choice') {
    // 多选：toggle
    const selected = (answers.value[qid] || '').split(',').filter(Boolean)
    const idx = selected.indexOf(optKey)
    if (idx >= 0) {
      selected.splice(idx, 1)
    } else {
      selected.push(optKey)
    }
    answers.value[qid] = selected.join(',')
  } else {
    // 单选/判断：直接替换
    answers.value[qid] = optKey
  }
}

function selectOption(key: string) {
  if (!currentQuestion.value) return
  answers.value[currentQuestion.value.questionId] = key
}

function onEssayInput(e: Event) {
  if (!currentQuestion.value) return
  const target = e.target as HTMLTextAreaElement
  answers.value[currentQuestion.value.questionId] = target.value
}

function isSkipRequiredCheck(): boolean {
  const qt = currentQuestion.value?.questionType
  return qt === 'essay' || qt === 'multiple_choice'
}

function prevQuestion() {
  if (currentIndex.value > 0) currentIndex.value--
}

function nextQuestion() {
  const q = currentQuestion.value
  if (q && !answers.value[q.questionId] && !isSkipRequiredCheck()) {
    ElMessage.warning('请先回答当前题目')
    return
  }
  if (currentIndex.value < totalQuestions.value - 1) {
    currentIndex.value++
  }
}

function goToQuestion(idx: number) {
  currentIndex.value = idx
}

function showConfirm() {
  const q = currentQuestion.value
  if (q && !answers.value[q.questionId] && !isSkipRequiredCheck()) {
    ElMessage.warning('请先回答当前题目')
    return
  }
  step.value = 'confirm'
}

function goBack() {
  step.value = 'question'
}

async function doSubmit() {
  if (submitting.value) return
  submitting.value = true
  stopTimer()
  try {
    const answerItems = (assessmentData.value?.questions ?? []).map(q => ({
      questionId: q.questionId,
      selectedAnswer: answers.value[q.questionId] || '',
    }))
    resultData.value = await submitAssessment({
      token: token.value,
      answers: answerItems,
    })
    step.value = 'result'
  } catch (e: any) {
    const msg = e?.message || '提交失败，请稍后重试'
    ElMessage.error(msg)
    step.value = 'question'
    // Resume timer on failure
    const duration = assessmentData.value?.durationMinutes || 30
    startTimer(duration)
  } finally {
    submitting.value = false
  }
}

function finish() {
  window.close()
  setTimeout(() => {
    router.push('/')
  }, 500)
}
</script>

<style scoped>
/* ─── CSS Variables ─── */
.assessment-page {
  --brand-primary: #2563eb;
  --brand-primary-light: #eff6ff;
  --brand-gradient: linear-gradient(135deg, #2563eb, #3b82f6);
  --text-primary: #0f172a;
  --text-secondary: #475569;
  --text-tertiary: #94a3b8;
  --border-color: #e2e8f0;
  --bg-page: #f8fafc;
  --bg-white: #ffffff;
  --shadow-sm: 0 1px 3px rgba(0,0,0,0.06);
  --shadow-md: 0 4px 16px rgba(0,0,0,0.06);
  --radius-lg: 12px;
  --radius-md: 8px;
  --radius-sm: 6px;
}

.assessment-page {
  min-height: 100vh;
  background: var(--bg-page);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'PingFang SC', 'Microsoft YaHei', 'Helvetica Neue', sans-serif;
  color: var(--text-primary);
  display: flex;
  flex-direction: column;
}

/* ─── HEADER ─── */
.app-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  height: 56px;
  background: var(--bg-white);
  border-bottom: 1px solid var(--border-color);
  box-shadow: var(--shadow-sm);
}

.header-inner {
  max-width: 860px;
  margin: 0 auto;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32px;
}

.header-brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-logo {
  height: 28px;
  width: auto;
}

.brand-divider {
  width: 1px;
  height: 20px;
  background: var(--border-color);
}

.brand-subtitle {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-secondary);
  letter-spacing: 0.5px;
}

.header-timer {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--brand-primary-light);
  padding: 6px 14px;
  border-radius: 8px;
}

.timer-icon {
  display: flex;
  align-items: center;
  color: var(--brand-primary);
  flex-shrink: 0;
}

.timer-label {
  font-size: 12px;
  color: var(--text-secondary);
  white-space: nowrap;
}

.timer-value {
  font-size: 16px;
  font-weight: 700;
  color: var(--brand-primary);
  font-variant-numeric: tabular-nums;
  min-width: 50px;
  text-align: right;
}

.timer-value.timer-warning {
  color: #d97706;
}

.timer-value.timer-danger {
  color: #dc2626;
  animation: pulse 1s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

/* ─── MAIN CONTENT ─── */
.app-main {
  flex: 1;
  display: flex;
  justify-content: center;
}

.app-main.has-header {
  padding-top: 56px;
}

.content-container {
  width: 100%;
  max-width: 860px;
  padding: 32px;
  display: flex;
  flex-direction: column;
}

/* ─── LOADING ─── */
.state-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  gap: 20px;
}

.loading-logo {
  height: 36px;
  width: auto;
  opacity: 0.6;
}

.loading-spinner {
  width: 36px;
  height: 36px;
  border: 3px solid var(--border-color);
  border-top-color: var(--brand-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }

.loading-text {
  color: var(--text-tertiary);
  font-size: 14px;
}

/* ─── LANDING ─── */
.state-landing {
  padding-top: 48px;
}

.landing-card {
  background: var(--bg-white);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
  overflow: hidden;
}

.landing-head {
  padding: 32px 40px;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.landing-title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
}

.landing-type-badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 14px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
  color: #fff;
  white-space: nowrap;
  flex-shrink: 0;
}

.landing-body {
  padding: 8px 40px;
}

.landing-row {
  display: flex;
  align-items: center;
  padding: 14px 0;
  border-bottom: 1px solid #f1f5f9;
}

.landing-row:last-child {
  border-bottom: none;
}

.landing-label {
  width: 100px;
  flex-shrink: 0;
  font-size: 14px;
  color: var(--text-tertiary);
}

.landing-value {
  font-size: 14px;
  color: var(--text-primary);
}

.landing-footer {
  padding: 24px 40px 32px;
  text-align: left;
}

.start-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 36px;
  background: var(--brand-gradient);
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: box-shadow 0.2s, transform 0.15s;
}

.start-btn:hover {
  box-shadow: 0 4px 16px rgba(37, 99, 235, 0.35);
  transform: translateY(-1px);
}

.landing-disclaimer {
  margin: 16px 0 0;
  font-size: 12px;
  color: var(--text-tertiary);
  line-height: 1.6;
}

/* ─── QUESTIONS ─── */
.state-question {
  padding-top: 24px;
}

.q-progress-bar {
  height: 4px;
  background: #e2e8f0;
  border-radius: 2px;
  overflow: hidden;
  margin-bottom: 10px;
}

.q-progress-fill {
  height: 100%;
  background: var(--brand-primary);
  border-radius: 2px;
  transition: width 0.3s ease;
}

.q-progress-stats {
  display: flex;
  justify-content: space-between;
  margin-bottom: 32px;
  font-size: 13px;
  color: var(--text-tertiary);
}

.q-answered-count {
  color: var(--brand-primary);
  font-weight: 500;
}

.q-body {
  background: var(--bg-white);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
  padding: 36px 40px;
  margin-bottom: 24px;
}

.q-stem {
  display: flex;
  gap: 8px;
  font-size: 16px;
  line-height: 1.8;
  color: var(--text-primary);
  margin-bottom: 28px;
}

.q-stem-num {
  font-weight: 700;
  color: var(--brand-primary);
  flex-shrink: 0;
}

.q-stem-text {
  flex: 1;
}

.q-type-badge {
  display: inline-block;
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 4px;
  white-space: nowrap;
  flex-shrink: 0;
  align-self: flex-start;
  margin-top: 2px;
}
.q-type-single_choice { background: #eff6ff; color: #2563eb; }
.q-type-multiple_choice { background: #f5f3ff; color: #7c3aed; }
.q-type-true_false { background: #fff7ed; color: #ea580c; }
.q-type-essay { background: #f0fdf4; color: #16a34a; }
.q-type-likert { background: #fdf2f8; color: #db2777; }

.q-multi-hint {
  margin-top: 12px;
  font-size: 12px;
  color: #7c3aed;
  padding-left: 4px;
}

/* MCQ options */
.q-options {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.q-option {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 20px;
  border: 2px solid var(--border-color);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.15s;
  user-select: none;
}

.q-option:hover {
  border-color: #bfdbfe;
  background: #fafaff;
}

.q-option-selected {
  border-color: var(--brand-primary);
  background: var(--brand-primary-light);
}

.q-option-key {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: #f1f5f9;
  font-weight: 700;
  font-size: 13px;
  color: #475569;
  flex-shrink: 0;
  transition: all 0.15s;
}

.q-option-key.q-option-key-checkbox {
  border-radius: 6px;
  border: 2px solid #cbd5e1;
  background: transparent;
}

.q-option-selected .q-option-key {
  background: var(--brand-primary);
  color: #fff;
}

.q-option-selected .q-option-key.q-option-key-checkbox {
  background: var(--brand-primary);
  border-color: var(--brand-primary);
  color: #fff;
}

.q-option-text {
  font-size: 14px;
  color: #334155;
  line-height: 1.5;
}

/* Essay / Short Answer */
.q-essay {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.q-textarea {
  width: 100%;
  min-height: 200px;
  padding: 16px;
  border: 2px solid var(--border-color);
  border-radius: var(--radius-md);
  font-size: 15px;
  line-height: 1.8;
  color: var(--text-primary);
  background: #fafbfc;
  resize: vertical;
  font-family: inherit;
  transition: border-color 0.15s, box-shadow 0.15s;
  outline: none;
}

.q-textarea:focus {
  border-color: var(--brand-primary);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
  background: var(--bg-white);
}

.q-textarea::placeholder {
  color: var(--text-tertiary);
}

.q-essay-hint {
  font-size: 12px;
  color: var(--text-tertiary);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.q-essay-count {
  color: var(--brand-primary);
  font-weight: 500;
}

/* Likert options */
.q-likert {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.q-likert-option {
  flex: 1;
  min-width: 80px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 14px 8px;
  border: 2px solid var(--border-color);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.15s;
  user-select: none;
}

.q-likert-option:hover {
  border-color: #bfdbfe;
  background: #fafaff;
}

.q-likert-selected {
  border-color: var(--brand-primary);
  background: var(--brand-primary-light);
}

.q-likert-score {
  font-size: 20px;
  font-weight: 800;
  color: #475569;
  transition: color 0.15s;
}

.q-likert-selected .q-likert-score {
  color: var(--brand-primary);
}

.q-likert-label {
  font-size: 11px;
  color: #64748b;
  text-align: center;
  line-height: 1.3;
}

/* Navigation */
.q-nav {
  display: flex;
  align-items: center;
  gap: 12px;
}

.nav-spacer {
  flex: 1;
}

.nav-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 22px;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;
  border: 1px solid var(--border-color);
  background: var(--bg-white);
  color: var(--text-secondary);
}

.nav-btn:hover {
  border-color: var(--brand-primary);
  color: var(--brand-primary);
}

.nav-btn-prev, .nav-btn-next {
}

.nav-btn-submit {
  background: var(--brand-gradient);
  color: #fff;
  border: none;
  padding: 10px 28px;
}

.nav-btn-submit:hover {
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3);
}

.q-dots {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  justify-content: center;
}

.q-dot {
  width: 30px;
  height: 30px;
  border-radius: 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  background: #f1f5f9;
  color: #64748b;
  transition: all 0.15s;
}

.q-dot:hover {
  background: #e2e8f0;
}

.q-dot-active {
  background: var(--brand-primary);
  color: #fff;
}

.q-dot-done {
  background: #dbeafe;
  color: var(--brand-primary);
}

.q-dot-active.q-dot-done {
  background: var(--brand-primary);
  color: #fff;
}

/* ─── CONFIRM ─── */
.state-confirm {
  padding-top: 80px;
  display: flex;
  justify-content: center;
}

.confirm-card {
  background: var(--bg-white);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
  padding: 48px;
  text-align: center;
  max-width: 440px;
  width: 100%;
}

.confirm-icon {
  margin-bottom: 16px;
  display: flex;
  justify-content: center;
}

.confirm-card h2 {
  margin: 0 0 12px;
  font-size: 20px;
  color: var(--text-primary);
}

.confirm-summary {
  margin: 0 0 8px;
  color: var(--text-secondary);
  font-size: 15px;
}

.text-danger {
  color: #dc2626;
}

.confirm-hint {
  color: var(--text-tertiary);
  font-size: 13px;
  margin: 0 0 28px;
}

.confirm-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

/* ─── RESULT ─── */
.state-result {
  padding-top: 80px;
  display: flex;
  justify-content: center;
}

.result-card {
  background: var(--bg-white);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
  padding: 48px;
  text-align: center;
  max-width: 440px;
  width: 100%;
}

.result-icon {
  margin-bottom: 16px;
  display: flex;
  justify-content: center;
}

.result-card h1 {
  margin: 0 0 4px;
  font-size: 22px;
  color: var(--text-primary);
}

.result-meta {
  color: var(--text-tertiary);
  font-size: 14px;
  margin-bottom: 28px;
}

.result-score {
  background: var(--brand-primary-light);
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 20px;
}

.score-number {
  display: block;
  font-size: 40px;
  font-weight: 800;
  color: var(--brand-primary);
  line-height: 1.2;
}

.score-detail {
  display: block;
  margin-top: 6px;
  font-size: 14px;
  color: var(--text-secondary);
}

.result-desc {
  margin: 0 0 28px;
  color: var(--text-secondary);
  font-size: 14px;
  line-height: 1.8;
  text-align: left;
}

/* ─── ERROR ─── */
.state-error {
  padding-top: 120px;
  display: flex;
  justify-content: center;
}

.error-card {
  background: var(--bg-white);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
  padding: 48px;
  text-align: center;
  max-width: 440px;
  width: 100%;
}

.error-icon {
  margin-bottom: 16px;
  display: flex;
  justify-content: center;
}

.icon-warn { color: #f59e0b; }
.icon-err { color: #dc2626; }

.error-card h1 {
  margin: 0 0 12px;
  font-size: 20px;
  color: var(--text-primary);
}

.error-card p {
  color: var(--text-secondary);
  font-size: 14px;
  line-height: 1.6;
  margin: 0 0 28px;
}

/* ─── SHARED BUTTONS ─── */
.btn-primary {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 36px;
  background: var(--brand-gradient);
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: box-shadow 0.2s;
}

.btn-primary:hover {
  box-shadow: 0 4px 14px rgba(37, 99, 235, 0.3);
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-secondary {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 12px 36px;
  background: var(--bg-white);
  color: var(--text-secondary);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-secondary:hover {
  border-color: var(--brand-primary);
  color: var(--brand-primary);
}

/* ─── RESPONSIVE ─── */
@media (max-width: 640px) {
  .content-container {
    padding: 16px;
  }
  .landing-head,
  .landing-body,
  .landing-footer {
    padding-left: 24px;
    padding-right: 24px;
  }
  .q-body {
    padding: 24px;
  }
  .landing-head {
    flex-direction: column;
    align-items: flex-start;
  }
  .header-inner {
    padding: 0 16px;
  }
}
</style>

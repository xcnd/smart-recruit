<template>
  <div class="onboarding-detail-page">
    <!-- 返回按钮 -->
    <div class="detail-back">
      <el-button :icon="ArrowLeft" text @click="$router.back()">返回入职列表</el-button>
    </div>

    <div v-loading="loading">
      <!-- ==================== 员工信息卡片 ==================== -->
      <div v-if="onboarding" class="employee-card">
        <div class="employee-card-inner">
          <!-- 左侧：头像 + 基本信息 -->
          <div class="employee-main">
            <el-avatar :size="56" class="employee-avatar">
              {{ onboarding.employeeName?.charAt(0) || '?' }}
            </el-avatar>
            <div class="employee-info">
              <div class="employee-name-row">
                <span class="employee-name">{{ onboarding.employeeName }}</span>
                <span class="employee-no">{{ onboarding.employeeNo }}</span>
              </div>
              <div class="employee-meta">
                <span class="meta-item">
                  <el-icon :size="14"><Briefcase /></el-icon>
                  {{ onboarding.jobTitle }}
                  <el-tag v-if="onboarding.level" size="small" type="info" class="level-tag">
                    {{ onboarding.level }}
                  </el-tag>
                </span>
                <span class="meta-divider">·</span>
                <span class="meta-item">
                  <el-icon :size="14"><OfficeBuilding /></el-icon>
                  {{ onboarding.departmentName }}
                </span>
                <span class="meta-divider">·</span>
                <span class="meta-item">
                  <el-icon :size="14"><Calendar /></el-icon>
                  入职 {{ onboarding.onboardDate }}
                </span>
              </div>
            </div>
          </div>

          <!-- 右侧：状态标签 -->
          <div class="employee-tags">
            <el-tag
              :type="getOnboardingStatusType(onboarding.status)"
              size="default"
              class="status-main-tag"
            >
              {{ getOnboardingStatusLabel(onboarding.status) }}
            </el-tag>
            <el-tag
              v-if="onboarding.riskLevel !== undefined"
              :type="getRiskLevelType(onboarding.riskLevel)"
              size="default"
            >
              {{ getRiskLevelLabel(onboarding.riskLevel) }}
            </el-tag>
            <div class="employee-status-edit">
              <el-select v-model="employeeStatusDraft" size="small" style="width: 110px" placeholder="员工状态">
                <el-option label="待入职" :value="0" />
                <el-option label="试用期" :value="1" />
                <el-option label="正式" :value="2" />
                <el-option label="已离职" :value="3" />
              </el-select>
              <el-button size="small" type="primary" :loading="savingStatus" @click="saveEmployeeStatus">
                保存
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- ==================== 主体：左侧导航 + 右侧内容 ==================== -->
      <div v-if="onboarding" class="detail-layout">
        <!-- 左侧：步骤导航 -->
        <aside class="step-sidebar">
          <div class="step-sidebar-header">
            <div class="step-progress-label">入职流程</div>
            <div class="step-progress-count">{{ completedProgressCount }}/{{ totalProgressCount }} 完成 · {{ progressPercent }}%</div>
          </div>

          <nav class="step-nav">
            <button
              v-for="(name, idx) in STEP_NAMES"
              :key="idx"
              class="step-nav-item"
              :class="{
                'is-active': activeStep === idx,
                'is-done': idx < (onboarding.currentStep - 1),
                'is-current': idx === (onboarding.currentStep - 1),
              }"
              :disabled="idx > (onboarding.currentStep - 1) && idx !== activeStep"
              @click="selectStep(idx)"
            >
              <span class="step-nav-dot">
                <el-icon v-if="idx < (onboarding.currentStep - 1)" :size="14"><Check /></el-icon>
                <span v-else class="dot-fill" />
              </span>
              <span class="step-nav-label">{{ name }}</span>
              <span v-if="idx === 0 && missingDocumentsCount > 0" class="step-nav-missing-badge">{{ missingDocumentsCount }}</span>
              <span v-if="idx === (onboarding.currentStep - 1)" class="step-nav-current-badge">当前</span>
            </button>
          </nav>

          <!-- AI 预测详情（侧边栏内） -->
          <div v-if="retentionPrediction && retentionPrediction.status !== 'PENDING'" class="sidebar-ai-card">
            <div class="sidebar-ai-header">
              <el-icon :size="14"><TrendCharts /></el-icon>
              <span>AI 留任预测</span>
            </div>

            <!-- 双环仪表 -->
            <div class="sidebar-ai-gauges">
              <div class="sidebar-ai-gauge">
                <el-progress
                  type="circle"
                  :percentage="retentionPrediction.retentionScore6M"
                  :color="retentionScoreColor"
                  :width="58"
                  :stroke-width="6"
                />
                <span class="gauge-num">{{ retentionPrediction.retentionScore6M }}%</span>
                <span class="gauge-sub">6个月</span>
              </div>
              <div class="sidebar-ai-gauge">
                <el-progress
                  type="circle"
                  :percentage="retentionPrediction.retentionScore12M"
                  :color="retentionScore12Color"
                  :width="58"
                  :stroke-width="6"
                />
                <span class="gauge-num">{{ retentionPrediction.retentionScore12M }}%</span>
                <span class="gauge-sub">12个月</span>
              </div>
            </div>

            <div class="sidebar-ai-risk-line">
              <el-tag :type="retentionRiskTagType" size="small">{{ retentionRiskLabel }}</el-tag>
            </div>

            <!-- 风险因素 -->
            <div v-if="retentionPrediction.riskFactors && retentionPrediction.riskFactors.length" class="sidebar-ai-section">
              <div class="sidebar-ai-section-title risk-title">
                <el-icon :size="12"><WarningFilled /></el-icon>
                风险因素
              </div>
              <ul class="sidebar-ai-list">
                <li v-for="(f, i) in retentionPrediction.riskFactors" :key="i">{{ f }}</li>
              </ul>
            </div>

            <!-- 干预建议 -->
            <div v-if="retentionPrediction.interventions && retentionPrediction.interventions.length" class="sidebar-ai-section">
              <div class="sidebar-ai-section-title intervention-title">
                <el-icon :size="12"><MagicStick /></el-icon>
                干预建议
              </div>
              <ul class="sidebar-ai-list">
                <li v-for="(iv, i) in retentionPrediction.interventions" :key="i">{{ iv }}</li>
              </ul>
            </div>
          </div>
          <!-- 评估中 -->
          <div v-else-if="retentionLoading" class="sidebar-ai-card">
            <div class="sidebar-ai-header">
              <el-icon :size="14" class="is-loading"><Loading /></el-icon>
              <span>AI 留任预测</span>
            </div>
            <div class="sidebar-ai-loading">AI 正在评估留任风险，请稍候...</div>
          </div>
        </aside>

        <!-- 右侧：步骤内容 -->
        <main class="step-content">
          <!-- 步骤标题栏 -->
          <div class="step-content-header">
            <div class="step-content-title">
              <span class="step-badge">{{ activeStep + 1 }}</span>
              {{ STEP_NAMES[activeStep] }}
            </div>
          </div>

          <!-- 步骤面板（带切换动画） -->
          <div class="step-content-body">
            <Transition name="step-fade" mode="out-in">
              <div :key="activeStep" class="step-panel-wrapper">
                <Step1DocumentsPanel
                  v-if="activeStep === 0"
                  :onboarding="onboarding"
                  @updated="loadOnboarding"
                />
                <Step2EquipmentPanel
                  v-if="activeStep === 1"
                  :onboarding="onboarding"
                  @updated="loadOnboarding"
                />
                <Step3AccountPanel
                  v-if="activeStep === 2"
                  :onboarding="onboarding"
                  @updated="loadOnboarding"
                />
                <Step4WelcomePanel
                  v-if="activeStep === 3"
                  :onboarding="onboarding"
                  @updated="loadOnboarding"
                />
                <Step5MentorPanel
                  v-if="activeStep === 4"
                  :onboarding="onboarding"
                  @updated="loadOnboarding"
                />
                <Step6TrainingPanel
                  v-if="activeStep === 5"
                  :onboarding="onboarding"
                  @updated="loadOnboarding"
                />
              </div>
            </Transition>
          </div>

          <!-- 底部操作栏 -->
          <div class="step-actions" v-if="onboarding.currentStep <= STEP_NAMES.length">
            <div class="step-actions-left">
              <span class="step-current-hint">
                当前：<strong>{{ getOnboardingStepName(onboarding.currentStep) }}</strong>
              </span>
            </div>
            <div class="step-actions-right">
              <el-button
                v-if="onboarding.currentStep > 1"
                :disabled="advancing"
                @click="goToPreviousStep"
              >
                ← 上一步
              </el-button>
              <el-button
                v-if="onboarding.currentStep < STEP_NAMES.length"
                type="primary"
                :loading="advancing"
                :icon="ArrowRight"
                @click="handleAdvanceStep"
              >
                推进至：{{ getOnboardingStepName(onboarding.currentStep + 1) }}
              </el-button>
              <!-- 所有步骤完成 -->
              <template v-if="onboarding.currentStep >= STEP_NAMES.length">
                <el-tag v-if="onboarding.status === 2" type="success" size="default">已入职</el-tag>
                <el-button
                  v-else
                  type="success"
                  :loading="advancing"
                  @click="handleCompleteOnboarding"
                >
                  标记为已入职
                </el-button>
              </template>
            </div>
          </div>
        </main>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import {
  ArrowLeft, ArrowRight, Briefcase, Calendar, Check, MagicStick,
  OfficeBuilding, TrendCharts, WarningFilled, Loading,
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getOnboardingDetail, advanceStep, completeOnboarding, getRetentionPrediction, updateEmployeeStatus } from '@/api/onboarding'
import {
  getRiskLevelLabel, getRiskLevelType,
  getOnboardingStepName, getOnboardingStatusLabel, getOnboardingStatusType,
} from '@/utils/format'

import Step1DocumentsPanel from '@/components/onboarding/Step1DocumentsPanel.vue'
import Step2EquipmentPanel from '@/components/onboarding/Step2EquipmentPanel.vue'
import Step3AccountPanel from '@/components/onboarding/Step3AccountPanel.vue'
import Step4WelcomePanel from '@/components/onboarding/Step4WelcomePanel.vue'
import Step5MentorPanel from '@/components/onboarding/Step5MentorPanel.vue'
import Step6TrainingPanel from '@/components/onboarding/Step6TrainingPanel.vue'

import type { OnboardingDetailVO, RetentionPredictionVO } from '@/types/models'

const STEP_NAMES = ['资料收集', '设备发放', '账号开通', '欢迎页', '导师分配', '入职培训']

const route = useRoute()
const loading = ref(true)
const advancing = ref(false)
const onboarding = ref<OnboardingDetailVO | null>(null)
const retentionPrediction = ref<RetentionPredictionVO | null>(null)
const retentionLoading = ref(false)
let retentionPollTimer: ReturnType<typeof setInterval> | null = null
let retentionPollTries = 0
const activeStep = ref(0)
const employeeStatusDraft = ref<number | undefined>(undefined)
const savingStatus = ref(false)

const completedDocumentsCount = computed(() => {
  const docs = onboarding.value?.documents || []
  return docs.filter(d => d.status >= 1).length
})

const totalDocumentsCount = computed(() => {
  return (onboarding.value?.documents || []).length || 6
})

const missingDocumentsCount = computed(() => {
  return totalDocumentsCount.value - completedDocumentsCount.value
})

const completedStepsExtra = computed(() => {
  if (!onboarding.value) return 0
  return Math.max(0, (onboarding.value.currentStep || 1) - 1)
})

const totalProgressCount = computed(() => {
  // 6 documents + 5 other steps (设备发放 ~ 入职培训)
  return totalDocumentsCount.value + 5
})

const completedProgressCount = computed(() => {
  return completedDocumentsCount.value + completedStepsExtra.value
})

const progressPercent = computed(() => {
  if (totalProgressCount.value === 0) return 0
  return Math.round(completedProgressCount.value / totalProgressCount.value * 100)
})

const retentionScoreColor = computed(() => {
  const score = retentionPrediction.value?.retentionScore6M || 0
  return score >= 80 ? '#059669' : score >= 50 ? '#d97706' : '#dc2626'
})

const retentionScore12Color = computed(() => {
  const score = retentionPrediction.value?.retentionScore12M || 0
  return score >= 80 ? '#059669' : score >= 50 ? '#d97706' : '#dc2626'
})

const retentionRiskLabel = computed(() => {
  const score = retentionPrediction.value?.retentionScore6M || 0
  return score >= 80 ? '低风险' : score >= 50 ? '中等风险' : '高风险'
})

const retentionRiskTagType = computed<'success' | 'warning' | 'danger' | 'info' | ''>(() => {
  const score = retentionPrediction.value?.retentionScore6M || 0
  return score >= 80 ? 'success' : score >= 50 ? 'warning' : 'danger'
})

function selectStep(idx: number) {
  if (!onboarding.value) return
  // Allow navigating to completed steps and the current step only
  if (idx <= (onboarding.value.currentStep - 1)) {
    activeStep.value = idx
  }
}

function goToPreviousStep() {
  if (!onboarding.value) return
  const prev = (onboarding.value.currentStep || 1) - 2
  if (prev >= 0) {
    activeStep.value = prev
  }
}

async function handleAdvanceStep() {
  if (!onboarding.value) return
  advancing.value = true
  try {
    await advanceStep(onboarding.value.id)
    ElMessage.success('已推进至下一步')
    await loadOnboarding()
    if (onboarding.value) {
      activeStep.value = (onboarding.value.currentStep || 1) - 1
    }
  } catch {
    // HTTP 拦截器统一提示
  } finally {
    advancing.value = false
  }
}

async function handleCompleteOnboarding() {
  if (!onboarding.value) return
  advancing.value = true
  try {
    await completeOnboarding(onboarding.value.id)
    ElMessage.success('已标记为已入职')
    await loadOnboarding()
  } catch {
    // HTTP 拦截器统一提示
  } finally {
    advancing.value = false
  }
}

async function loadOnboarding() {
  try {
    const detail = await getOnboardingDetail(route.params.id as string)
    onboarding.value = detail
    employeeStatusDraft.value = detail.employeeStatus ?? 0
    // Only jump to current step if we're ahead of it
    if (activeStep.value > (detail.currentStep || 1) - 1) {
      activeStep.value = (detail.currentStep || 1) - 1
    }
  } catch {
    onboarding.value = null
  }

  // 留任预测异步加载：不阻塞页面（后端异步生成 LLM 预测，前端轮询结果）
  loadRetentionPrediction()
}

/** 异步加载留任预测：PENDING 时每 2s 轮询，最多 20 次（40s）。 */
function loadRetentionPrediction() {
  retentionLoading.value = true
  retentionPollTries = 0
  stopRetentionPolling()
  fetchRetentionPrediction()
  retentionPollTimer = setInterval(fetchRetentionPrediction, 2000)
}

function stopRetentionPolling() {
  if (retentionPollTimer) {
    clearInterval(retentionPollTimer)
    retentionPollTimer = null
  }
}

async function fetchRetentionPrediction() {
  if (!route.params.id) return
  retentionPollTries++
  try {
    const res = await getRetentionPrediction(route.params.id as string)
    if (res.status === 'COMPLETED' || res.status === undefined) {
      retentionPrediction.value = res
      retentionLoading.value = false
      stopRetentionPolling()
      return
    }
  } catch {
    // 轮询失败继续等待下一次
  }
  if (retentionPollTries >= 20) {
    retentionLoading.value = false
    stopRetentionPolling()
  }
}

/** 保存员工状态。 */
async function saveEmployeeStatus() {
  if (employeeStatusDraft.value === undefined) return
  savingStatus.value = true
  try {
    await updateEmployeeStatus(String(route.params.id), employeeStatusDraft.value)
    ElMessage.success('员工状态已更新')
    await loadOnboarding()
  } catch {
    ElMessage.error('员工状态更新失败')
  } finally {
    savingStatus.value = false
  }
}

onMounted(async () => {
  loading.value = true
  await loadOnboarding()
  if (onboarding.value) {
    activeStep.value = (onboarding.value.currentStep || 1) - 1
  }
  loading.value = false
})

onBeforeUnmount(() => {
  stopRetentionPolling()
})
</script>

<style scoped>
.onboarding-detail-page {
  max-width: 1400px;
}

/* ---- 返回 ---- */
.detail-back {
  margin-bottom: 16px;
}

/* ==============================
   员工信息卡片
   ============================== */
.employee-card {
  background: var(--c-card);
  border: 1px solid var(--c-border);
  border-radius: 12px;
  margin-bottom: 20px;
  box-shadow: var(--c-shadow-sm);
  overflow: hidden;
}

.employee-card-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  gap: 20px;
  flex-wrap: wrap;
}

.employee-main {
  display: flex;
  align-items: center;
  gap: 16px;
  flex: 1;
  min-width: 0;
}

.employee-avatar {
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #fff;
  font-size: 22px;
  font-weight: 700;
  flex-shrink: 0;
}

.employee-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.employee-name-row {
  display: flex;
  align-items: baseline;
  gap: 12px;
  flex-wrap: wrap;
}

.employee-name {
  font-size: 18px;
  font-weight: 700;
  color: var(--c-text);
  letter-spacing: -0.2px;
}

.employee-no {
  font-size: 13px;
  color: var(--c-text-muted);
  font-family: 'SF Mono', 'Cascadia Code', 'Consolas', monospace;
  background: var(--c-bg);
  padding: 2px 8px;
  border-radius: 4px;
}

.employee-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  font-size: 13px;
  color: var(--c-text-secondary);
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  white-space: nowrap;
}

.meta-divider {
  color: var(--c-border);
  font-weight: 300;
}

.level-tag {
  margin-left: 4px;
  font-size: 11px;
}

.employee-tags {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.status-main-tag {
  font-weight: 600;
}

.employee-status-edit {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: 8px;
}

/* ==============================
   主体布局：侧边栏 + 内容
   ============================== */
.detail-layout {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

/* ---- 左侧步骤导航 ---- */
.step-sidebar {
  width: 240px;
  flex-shrink: 0;
  position: sticky;
  top: calc(var(--c-topbar-height) + 24px);
}

.step-sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 4px 12px;
  margin-bottom: 4px;
  border-bottom: 1px solid var(--c-border-light);
}

.step-progress-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--c-text);
}

.step-progress-count {
  font-size: 12px;
  color: var(--c-text-muted);
  font-weight: 500;
}

.step-nav {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.step-nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 9px 10px;
  border: none;
  border-radius: 8px;
  background: transparent;
  cursor: pointer;
  font-size: 13px;
  color: var(--c-text-secondary);
  transition: background 0.15s, color 0.15s;
  text-align: left;
  position: relative;
}

.step-nav-item:hover:not(:disabled) {
  background: var(--c-bg);
  color: var(--c-text);
}

.step-nav-item:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.step-nav-item.is-active,
.step-nav-item.is-current {
  background: var(--c-primary-light-9, #eef2ff);
  color: #4f46e5;
  font-weight: 600;
}

.step-nav-item.is-done {
  color: var(--c-text);
}

.step-nav-dot {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 12px;
  transition: background 0.2s, border-color 0.2s;
}

/* 已完成 */
.step-nav-item.is-done .step-nav-dot {
  background: #ecfdf5;
  color: #059669;
}

/* 当前 */
.step-nav-item.is-current .step-nav-dot {
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #fff;
}

.step-nav-item.is-current .step-nav-dot .dot-fill {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #fff;
  animation: step-pulse 2s infinite;
}

@keyframes step-pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.6; transform: scale(0.85); }
}

/* 未开始 */
.step-nav-item:not(.is-done):not(.is-current) .step-nav-dot {
  border: 2px solid var(--c-border);
  background: transparent;
}

.step-nav-label {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.step-nav-missing-badge {
  font-size: 11px;
  font-weight: 700;
  color: #fff;
  background: #dc2626;
  min-width: 18px;
  height: 18px;
  line-height: 18px;
  text-align: center;
  border-radius: 9px;
  padding: 0 5px;
  margin-left: auto;
}

.step-nav-current-badge {
  font-size: 10px;
  font-weight: 600;
  padding: 1px 6px;
  border-radius: 10px;
  background: #c7d2fe;
  color: #4338ca;
  text-transform: uppercase;
  letter-spacing: 0.3px;
  flex-shrink: 0;
}

/* ---- 侧边栏 AI 预测（丰富版） ---- */
.sidebar-ai-card {
  margin-top: 16px;
  padding: 14px;
  border: 1px solid var(--c-border-light);
  border-radius: 10px;
  background: var(--c-card);
}

.sidebar-ai-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  color: var(--c-text-muted);
  margin-bottom: 12px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.sidebar-ai-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 72px;
  font-size: 13px;
  color: var(--c-text-secondary);
}

.sidebar-ai-gauges {
  display: flex;
  justify-content: space-around;
  gap: 4px;
  margin-bottom: 10px;
}

.sidebar-ai-gauge {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.gauge-num {
  font-size: 13px;
  font-weight: 700;
  color: var(--c-text);
  margin-top: 2px;
}

.gauge-sub {
  font-size: 10px;
  color: var(--c-text-muted);
  margin-top: -2px;
}

.sidebar-ai-risk-line {
  text-align: center;
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--c-border-light);
}

.sidebar-ai-section {
  margin-top: 10px;
}

.sidebar-ai-section + .sidebar-ai-section {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid var(--c-border-light);
}

.sidebar-ai-section-title {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  font-weight: 600;
  margin-bottom: 6px;
}

.sidebar-ai-section-title.risk-title {
  color: #d97706;
}

.sidebar-ai-section-title.intervention-title {
  color: #6366f1;
}

.sidebar-ai-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.sidebar-ai-list li {
  font-size: 11px;
  color: var(--c-text-secondary);
  line-height: 1.5;
  padding: 3px 0 3px 12px;
  position: relative;
}

.sidebar-ai-list li::before {
  content: '';
  position: absolute;
  left: 0;
  top: 9px;
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: var(--c-text-muted);
}

.sidebar-ai-section-title.risk-title + .sidebar-ai-list li::before {
  background: #d97706;
}

.sidebar-ai-section-title.intervention-title + .sidebar-ai-list li::before {
  background: #6366f1;
}

/* ---- 右侧内容区 ---- */
.step-content {
  flex: 1;
  min-width: 0;
}

.step-content-header {
  margin-bottom: 16px;
}

.step-content-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  font-weight: 600;
  color: var(--c-text);
}

.step-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: 6px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #fff;
  font-size: 13px;
  font-weight: 700;
}

.step-content-body {
  background: var(--c-card);
  border: 1px solid var(--c-border);
  border-radius: 12px;
  padding: 20px 24px;
  box-shadow: var(--c-shadow-sm);
}

/* 步骤切换动画 */
.step-fade-enter-active,
.step-fade-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}

.step-fade-enter-from {
  opacity: 0;
  transform: translateY(6px);
}

.step-fade-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

/* ---- 底部操作栏 ---- */
.step-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 16px;
  padding: 14px 18px;
  background: var(--c-card);
  border: 1px solid var(--c-border);
  border-radius: 10px;
  box-shadow: var(--c-shadow-sm);
}

.step-actions-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.step-current-hint {
  font-size: 13px;
  color: var(--c-text-secondary);
}

.step-actions-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

/* ---- 响应式 ---- */
@media (max-width: 768px) {
  .detail-layout {
    flex-direction: column;
  }

  .step-sidebar {
    width: 100%;
    position: static;
  }

  .step-nav {
    flex-direction: row;
    flex-wrap: wrap;
    gap: 4px;
  }

  .step-nav-item {
    width: auto;
    padding: 6px 10px;
    font-size: 12px;
  }

  .step-nav-current-badge {
    display: none;
  }

  .sidebar-ai-card {
    display: none;
  }

  .employee-card-inner {
    padding: 14px 16px;
  }

  .employee-meta {
    gap: 2px;
  }

  .meta-divider {
    display: none;
  }
}
</style>

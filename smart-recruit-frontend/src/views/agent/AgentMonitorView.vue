<template>
  <div class="orchestration-page">
    <!-- Architecture Diagram -->
    <div class="arch-card">
      <div class="arch-header">
        <h2>
          <el-icon><Connection /></el-icon>
          AgentScope Java 多智能体架构
        </h2>
        <span class="arch-badge">AgentScope v2.0 · Spring Boot 4.1</span>
      </div>
      <div class="arch-diagram">
        <!-- Orchestration Layer -->
        <div class="arch-layer layer-orchestration">
          <span class="layer-label">编排层 Orchestration</span>
          <div class="agent-nodes">
            <div
              v-for="agent in orchestrationAgents"
              :key="agent.id"
              class="agent-node"
              @click="scrollToAgent(agent.id)"
              :title="agent.description"
            >
              <div class="agent-icon" :style="{ background: agent.color ? gradient(agent.color) : 'linear-gradient(135deg, #8b5cf6, #a78bfa)' }">
                <el-icon :size="18"><component :is="agent.icon" /></el-icon>
              </div>
              <div class="agent-name">{{ agent.displayName }}</div>
              <div class="agent-model">{{ agent.model }}</div>
              <span class="agent-status" :class="healthClass(agent.health)">
                {{ healthLabel(agent.health) }} {{ agent.health }}%
              </span>
            </div>
          </div>
        </div>
        <div class="arch-bus">
          <span class="bus-line"></span>
          <el-icon><Bottom /></el-icon>
          AgentScope 消息总线
          <el-icon><Bottom /></el-icon>
          <span class="bus-line"></span>
        </div>
        <!-- Execution Layer -->
        <div class="arch-layer layer-execution">
          <span class="layer-label">执行层 Execution</span>
          <div class="agent-nodes">
            <div
              v-for="agent in executionAgents"
              :key="agent.id"
              class="agent-node"
              @click="scrollToAgent(agent.id)"
              :title="agent.description"
            >
              <div class="agent-icon" :style="{ background: gradient(agent.color) }">
                <el-icon :size="18"><component :is="agent.icon" /></el-icon>
              </div>
              <div class="agent-name">{{ agent.displayName }}</div>
              <div class="agent-model">{{ agent.model }}</div>
              <span class="agent-status" :class="healthClass(agent.health)">
                {{ healthLabel(agent.health) }} {{ agent.health }}%
              </span>
            </div>
          </div>
        </div>
        <div class="arch-bus">
          <span class="bus-line"></span>
          <el-icon><Bottom /></el-icon>
          结果回调 · 复盘触发
          <el-icon><Bottom /></el-icon>
          <span class="bus-line"></span>
        </div>
        <!-- Review Layer -->
        <div class="arch-layer layer-review">
          <span class="layer-label">复盘层 Review</span>
          <div class="agent-nodes">
            <div
              v-for="agent in reviewAgents"
              :key="agent.id"
              class="agent-node"
              @click="scrollToAgent(agent.id)"
              :title="agent.description"
            >
              <div class="agent-icon" :style="{ background: gradient(agent.color) }">
                <el-icon :size="18"><component :is="agent.icon" /></el-icon>
              </div>
              <div class="agent-name">{{ agent.displayName }}</div>
              <div class="agent-model">{{ agent.model }}</div>
              <span class="agent-status" :class="healthClass(agent.health)">
                {{ healthLabel(agent.health) }} {{ agent.health }}%
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 全流程编排（简历 → Offer） -->
    <div class="panel pipeline-panel">
      <div class="panel-header">
        <h3>
          <el-icon><Connection /></el-icon>
          全流程编排（简历 → Offer）
        </h3>
        <span class="panel-count">ResumeParser → SmartScreener → HumanReview → InterviewEvaluator → OfferPredictor</span>
      </div>
      <div class="pipeline-body">
        <div class="pipeline-form">
          <el-form label-position="top" size="default">
            <div class="pipeline-grid">
              <el-form-item label="候选人姓名">
                <el-input v-model="pipelineForm.candidateName" placeholder="如：张三" />
              </el-form-item>
              <el-form-item label="部门">
                <el-tree-select
                  v-model="pipelineDeptId"
                  :data="pipelineDepartments"
                  :props="{ label: 'name', children: 'children' }"
                  node-key="id"
                  check-strictly
                  placeholder="请选择部门"
                  filterable
                  clearable
                  style="width: 100%"
                  @change="onPipelineDeptChange"
                />
              </el-form-item>
              <el-form-item label="目标岗位">
                <el-select
                  v-model="pipelineJobId"
                  placeholder="请选择岗位"
                  filterable
                  clearable
                  style="width: 100%"
                  :disabled="!pipelineDeptId"
                  @change="onPipelineJobSelect"
                >
                  <el-option
                    v-for="job in filteredPipelineJobs"
                    :key="job.id"
                    :label="job.title"
                    :value="job.id"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="必备技能（选岗后自动带出，可修改）">
                <el-input v-model="pipelineForm.requiredSkills" placeholder="如：Java,Spring,MySQL" />
              </el-form-item>
              <el-form-item label="最低工作年限">
                <el-input-number v-model="pipelineForm.minYears" :min="0" :max="30" style="width: 100%" />
              </el-form-item>
              <el-form-item label="Offer 年薪总包（万元）">
                <el-input-number v-model="pipelineForm.offerPackage" :min="0" :max="1000" style="width: 100%" />
              </el-form-item>
            </div>
            <el-form-item label="简历文件（上传后自动解析技能/年限/学历）">
              <div style="display: flex; gap: 12px; align-items: center; flex-wrap: wrap;">
                <el-upload
                  :auto-upload="false"
                  :show-file-list="false"
                  :accept="pipelineUploadAccept"
                  :on-change="handlePipelineResumeFile"
                >
                  <el-button :loading="pipelineParsing" :icon="Upload">上传简历文件</el-button>
                </el-upload>
                <span v-if="pipelineParsedResume" class="pipeline-parse-summary">
                  已解析：
                  <template v-if="pipelineParsedResume.parsed?.name">{{ pipelineParsedResume.parsed.name }}</template>
                  <template v-else>未识别姓名</template>
                  <template v-if="pipelineParsedResume.parsed?.phone"> · {{ pipelineParsedResume.parsed.phone }}</template>
                  <template v-if="pipelineParsedResume.parsed?.education?.[0]">
                    ·
                    {{ pipelineParsedResume.parsed.education[0].school || '' }}
                    <template v-if="pipelineParsedResume.parsed.education[0].degree">
                      （{{ pipelineParsedResume.parsed.education[0].degree }}）
                    </template>
                  </template>
                </span>
              </div>
              <div v-if="pipelineParsedResume?.parsed?.skills?.length" style="margin-top: 8px;">
                <el-tag
                  v-for="s in pipelineParsedResume.parsed.skills"
                  :key="s"
                  size="small"
                  effect="plain"
                  style="margin: 0 6px 6px 0;"
                >
                  {{ s }}
                </el-tag>
              </div>
            </el-form-item>
            <el-form-item label="简历文本（上传后自动填入，可编辑）">
              <el-input v-model="pipelineForm.resumeText" type="textarea" :rows="4"
                placeholder="上传简历文件后自动填入解析文本，也可手动粘贴..." />
            </el-form-item>
            <el-form-item label="面试摘要（可选，提供后执行面试评估阶段）">
              <el-input v-model="pipelineForm.interviewSummary" type="textarea" :rows="3"
                placeholder="粘贴面试反馈摘要，例如：候选人技术基础扎实，沟通表达清晰..." />
            </el-form-item>
            <div class="pipeline-actions">
              <el-button type="primary" :icon="Promotion" :loading="pipelineRunning" @click="runPipeline">
                运行全流程编排
              </el-button>
              <el-button @click="resetPipeline">重置</el-button>
            </div>
          </el-form>
        </div>

        <div class="pipeline-result">
          <template v-if="pipelineResult">
            <div class="pipeline-summary">
              <div class="summary-item">
                <span class="summary-label">整体状态</span>
                <span class="summary-value" :class="pipelineResult.status === 'COMPLETED' ? 'ok' : 'fail'">
                  {{ pipelineResult.status === 'COMPLETED' ? '执行成功' : '部分失败' }}
                </span>
              </div>
              <div class="summary-item">
                <span class="summary-label">总耗时</span>
                <span class="summary-value">{{ pipelineResult.durationMs }}ms</span>
              </div>
              <div class="summary-item">
                <span class="summary-label">筛选评分</span>
                <span class="summary-value">{{ pipelineResult.screening?.overallScore ?? '-' }}</span>
              </div>
              <div class="summary-item">
                <span class="summary-label">Offer 接受概率</span>
                <span class="summary-value">{{ pipelineResult.offerPrediction?.acceptanceProbability ?? '-' }}%</span>
              </div>
            </div>
            <div class="pipeline-stages">
              <div v-for="s in pipelineResult.stages" :key="s.stage" class="stage-item">
                <div class="stage-icon" :class="s.status.toLowerCase()">
                  <el-icon v-if="s.status === 'COMPLETED'"><CircleCheck /></el-icon>
                  <el-icon v-else-if="s.status === 'FAILED'"><CircleClose /></el-icon>
                  <el-icon v-else><VideoPause /></el-icon>
                </div>
                <div class="stage-info">
                  <div class="stage-title">
                    {{ s.stageName }}
                    <span class="stage-status" :class="s.status.toLowerCase()">
                      {{ s.status === 'COMPLETED' ? '完成' : s.status === 'FAILED' ? '失败' : '跳过' }}
                    </span>
                  </div>
                  <div class="stage-message">{{ s.message }}</div>
                  <div class="stage-meta">
                    {{ s.agentName }} · {{ s.durationMs != null ? s.durationMs + 'ms' : '-' }}
                  </div>
                </div>
              </div>
            </div>
          </template>
          <el-empty v-else description="填写左侧表单，点击「运行全流程编排」开始" :image-size="80" />
        </div>
      </div>
    </div>

    <!-- Agent Detail Cards -->
    <div class="section-header">
      <h2>
        <el-icon><Service /></el-icon>
        Agent 运行状态
      </h2>
      <span class="stats-summary">
        调用总量 {{ totalCalls }}/h · 平均延迟 {{ avgLatency }}ms · 整体健康度 {{ avgHealth }}%
      </span>
    </div>
    <div class="agent-cards-grid">
      <div
        v-for="agent in agents"
        :key="agent.id"
        :id="`card-${agent.id}`"
        class="agent-card"
        :class="{ highlight: highlightedAgent === agent.id }"
      >
        <div class="card-header">
          <div class="card-info">
            <div class="card-icon" :style="{ background: gradient(agent.color) }">
              <el-icon :size="18"><component :is="agent.icon" /></el-icon>
            </div>
            <div class="card-name">
              {{ agent.name }}
              <span v-if="agent.status === 2" class="paused-tag">已暂停</span>
              <span v-else-if="agent.status === 3" class="error-tag">故障</span>
            </div>
            <div class="card-model">{{ agent.model }}</div>
          </div>
          <div class="health-bar-wrap">
            <span class="health-pct" :class="healthClass(agent.health)">{{ agent.health }}%</span>
            <div class="health-bar">
              <div class="fill" :class="healthClass(agent.health)" :style="{ width: agent.health + '%' }"></div>
            </div>
            <div class="health-label">健康度</div>
          </div>
        </div>
        <div class="metrics">
          <div class="metric-item">
            <div class="metric-value">{{ fmt(agent.callsPerHour) }}<span class="metric-unit">/h</span></div>
            <div class="metric-label">调用量</div>
          </div>
          <div class="metric-item">
            <div class="metric-value">{{ agent.latencyMs }}<span class="metric-unit">ms</span></div>
            <div class="metric-label">平均延迟</div>
          </div>
          <div class="metric-item">
            <div class="metric-value">{{ agent.accuracy }}<span class="metric-unit">%</span></div>
            <div class="metric-label">准确率</div>
          </div>
          <div class="metric-item">
            <div class="metric-value">{{ tokenK(agent.tokens24h) }}<span class="metric-unit">K</span></div>
            <div class="metric-label">Token/24h</div>
          </div>
        </div>
        <p class="card-description">{{ agent.description }}</p>
        <div class="card-actions">
          <el-button size="small" type="primary" @click="openLogModal(agent)">
            <el-icon><Document /></el-icon> 日志
          </el-button>
          <el-button size="small" @click="openRestartModal(agent)">
            <el-icon><SwitchButton /></el-icon> 重启
          </el-button>
          <el-button size="small" @click="pauseAgentAction(agent)" :type="agent.status === 2 ? 'success' : 'danger'" plain>
            <el-icon><VideoPause /></el-icon> {{ agent.status === 2 ? '恢复' : '暂停' }}
          </el-button>
        </div>
      </div>
    </div>

    <!-- Two Column: Tasks + Events -->
    <div class="two-col">
      <!-- Task Queue -->
      <div class="panel task-queue-panel">
        <div class="panel-header">
          <h3>
            <el-icon><List /></el-icon>
            Agent 任务队列
          </h3>
          <span class="panel-count">{{ tasks.length }} 个任务</span>
        </div>
        <div class="panel-body">
          <el-table :data="tasks" stripe size="small">
            <el-table-column label="任务 ID" width="100">
              <template #default="{ row }">
                <span class="task-id">{{ row.id }}</span>
              </template>
            </el-table-column>
            <el-table-column label="Agent" width="180">
              <template #default="{ row }">
                <span style="display: inline-flex; align-items: center; gap: 6px;">
                  <span class="agent-type-dot" :style="{ background: agentTypeColor(row.type) }"></span>
                  {{ row.agent }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="类型" min-width="110">
              <template #default="{ row }">
                <el-tag size="small" :style="agentTypeTagStyle(row.type)">{{ row.type }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="优先级" width="70">
              <template #default="{ row }">
                <span class="priority-tag" :class="row.priority.toLowerCase()">
                  {{ row.priority === 'HIGH' ? '高' : row.priority === 'MEDIUM' ? '中' : '低' }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <span class="status-tag" :class="row.status.toLowerCase()">
                  {{ statusLabel(row.status) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="耗时（毫秒）" width="90">
              <template #default="{ row }">
                {{ row.duration }}
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <!-- Real-time Event Stream -->
      <div class="panel event-stream-panel">
        <div class="panel-header">
          <h3>
            <el-icon><Lightning /></el-icon>
            实时事件流
          </h3>
          <span class="panel-count">{{ filteredEvents.length }} 条事件</span>
        </div>
        <div class="panel-body">
          <div class="event-filters">
            <button
              v-for="f in eventFilters"
              :key="f.key"
              class="event-filter"
              :class="{ active: activeEventFilter === f.key }"
              @click="activeEventFilter = f.key"
            >
              {{ f.label }}
            </button>
          </div>
          <div class="event-list">
            <div
              v-for="event in filteredEvents.slice(0, 30)"
              :key="event.id"
              class="event-item"
            >
              <div class="event-dot" :class="event.level.toLowerCase()"></div>
              <span class="event-time">{{ event.time }}</span>
              <span class="event-text">{{ event.text }}</span>
              <span class="event-agent">{{ event.agent }}</span>
            </div>
            <div v-if="!filteredEvents.length" class="event-empty">暂无事件</div>
          </div>
        </div>
      </div>
    </div>

    <!-- Token Chart -->
    <div class="panel">
      <div class="panel-header">
        <h3>
          <el-icon><PieChart /></el-icon>
          Token 消耗分布（近 24h）
        </h3>
        <span class="panel-count">总计 {{ totalTokens }} tokens</span>
      </div>
      <div class="panel-body">
        <div class="token-chart-wrap">
          <div class="token-chart-canvas">
            <canvas ref="tokenChartRef"></canvas>
          </div>
          <div class="token-legend">
            <div
              v-for="(item, i) in tokenLegendItems"
              :key="i"
              class="legend-item"
            >
              <div class="legend-color" :style="{ background: chartColors[i % chartColors.length] }"></div>
              <span class="legend-name">{{ item.name }}</span>
              <span class="legend-val">{{ item.value }}K ({{ item.pct }}%)</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Log Dialog -->
    <el-dialog v-model="logDialogVisible" :title="`运行日志 · ${currentLogAgent?.name}`" width="640px">
      <div class="log-block">{{ logContent }}</div>
      <template #footer>
        <el-button @click="downloadLogs">
          <el-icon><Download /></el-icon> 下载日志
        </el-button>
        <el-button type="primary" @click="logDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- Restart Dialog -->
    <el-dialog v-model="restartDialogVisible" :title="`重启 Agent`" width="480px" :close-on-click-modal="false">
      <p class="restart-info">正在重启 <strong>{{ restartingAgent?.name }}</strong>，期间该 Agent 将不可用。</p>
      <div class="progress-bar-wrap">
        <div class="progress-bar" :style="{ width: restartProgress + '%' }"></div>
      </div>
      <p class="restart-status-text">{{ restartStatusText }}</p>
      <template #footer>
        <el-button @click="cancelRestart" :disabled="restartProgress > 0 && restartProgress < 100">取消</el-button>
        <el-button type="primary" @click="confirmRestart" :disabled="restartStarted">
          确认重启
        </el-button>
      </template>
    </el-dialog>

    <!-- Add Agent Dialog -->
    <el-dialog v-model="addAgentDialogVisible" title="注册新 Agent" width="520px">
      <el-form :model="newAgent" label-position="top">
        <el-form-item label="Agent 名称">
          <el-input v-model="newAgent.name" placeholder="如: SkillExtractorAgent" />
        </el-form-item>
        <el-form-item label="所属层级">
          <el-select v-model="newAgent.layer" style="width: 100%">
            <el-option value="orchestration" label="编排层 (Orchestration)" />
            <el-option value="execution" label="执行层 (Execution)" />
            <el-option value="review" label="复盘层 (Review)" />
          </el-select>
        </el-form-item>
        <el-form-item label="绑定模型">
          <el-select v-model="newAgent.model" style="width: 100%">
            <el-option value="deepseek-v4" label="deepseek-v4（DeepSeek 旗舰）" />
            <el-option value="deepseek-v4-flash" label="deepseek-v4-flash（DeepSeek 轻量）" />
            <el-option value="qwen3-max" label="qwen3-max（通义千问旗舰）" />
            <el-option value="qwen-turbo" label="qwen-turbo（通义千问轻量）" />
            <el-option value="deepseek-chat" label="deepseek-chat（DeepSeek 通用）" />
            <el-option value="deepseek-reasoner" label="deepseek-reasoner（DeepSeek 推理）" />
          </el-select>
        </el-form-item>
        <el-form-item label="功能描述">
          <el-input v-model="newAgent.description" type="textarea" :rows="3" placeholder="描述该 Agent 的职责和核心能力..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addAgentDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="registerAgent">
          <el-icon><Check /></el-icon> 注册
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick, markRaw } from 'vue'
import {
  Connection, Service, List, Lightning, PieChart,
  Document, SwitchButton, VideoPause, Download, Check, Bottom,
  Cpu, Files, EditPen, ScaleToOriginal, QuestionFilled,
  Microphone, Star, TrendCharts, UserFilled,
  Setting, Promotion, CircleCheck, CircleClose, Upload
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { Chart, registerables } from 'chart.js'
import {
  getAgents,
  getTaskQueue,
  getAgentLogs,
  pauseAgent,
  resumeAgent,
  restartAgent,
  registerAgent as apiRegisterAgent,
  connectEventStream,
  runPipeline as apiRunPipeline,
} from '@/api/agent'
import { getJobs } from '@/api/job'
import { getDepartments } from '@/api/system'
import { parseResumeFile } from '@/api/resume'
import type { PipelineResumeParseVO } from '@/api/resume'
import type {
  AgentInfoVO,
  TaskQueueItemVO,
  AgentLogVO,
  AgentEventVO,
  PipelineResultVO,
  JobVO,
  DepartmentTreeVO,
} from '@/types/models'
import { formatDateTime } from '@/utils/format'

Chart.register(...registerables)

// ==================== TYPE DEFINITIONS ====================
interface AgentDisplay {
  id: string
  name: string
  displayName: string
  model: string
  layer: 'orchestration' | 'execution' | 'review'
  icon: ReturnType<typeof markRaw>
  description: string
  status: number // 0=RUNNING, 1=IDLE, 2=PAUSED, 3=ERROR
  callsPerHour: number
  latencyMs: number
  accuracy: number
  health: number
  tokens24h: number
  tasksCompleted: number
  color: string
}

interface EventItem {
  id: string
  level: 'INFO' | 'SUCCESS' | 'WARNING' | 'ERROR'
  agent: string
  text: string
  time: string
}

interface TaskItem {
  id: string
  agent: string
  type: string
  priority: 'HIGH' | 'MEDIUM' | 'LOW'
  status: 'QUEUED' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'RETRYING'
  time: string
  duration: number | string
}

// ==================== AGENT 元数据映射（图标/颜色，按 agentId） ====================
const AGENT_META: Record<string, {
  icon: ReturnType<typeof markRaw>
  color: string
  layer: 'orchestration' | 'execution' | 'review'
}> = {
  orchestrator: { icon: markRaw(Cpu), color: '#8b5cf6', layer: 'orchestration' },
  'resume-parser': { icon: markRaw(Files), color: '#3b82f6', layer: 'execution' },
  'smart-screener': { icon: markRaw(ScaleToOriginal), color: '#3b82f6', layer: 'execution' },
  'jd-generator': { icon: markRaw(EditPen), color: '#3b82f6', layer: 'execution' },
  'interview-question': { icon: markRaw(QuestionFilled), color: '#3b82f6', layer: 'execution' },
  'interview-evaluator': { icon: markRaw(Microphone), color: '#3b82f6', layer: 'execution' },
  'referral-matcher': { icon: markRaw(Star), color: '#3b82f6', layer: 'execution' },
  'talent-recommender': { icon: markRaw(Star), color: '#3b82f6', layer: 'execution' },
  'analytics-insights': { icon: markRaw(TrendCharts), color: '#6366f1', layer: 'execution' },
  'offer-predictor': { icon: markRaw(TrendCharts), color: '#10b981', layer: 'review' },
  'retention-predictor': { icon: markRaw(UserFilled), color: '#10b981', layer: 'review' },
}

/** 后端层级编码 → 前端层级。 */
const TYPE_TO_LAYER: Record<number, 'orchestration' | 'execution' | 'review'> = {
  0: 'orchestration',
  1: 'execution',
  2: 'review',
}

// ==================== REACTIVE STATE ====================
const agents = ref<AgentDisplay[]>([])
const tasks = ref<TaskItem[]>([])
const events = ref<EventItem[]>([])
const highlightedAgent = ref<string | null>(null)

const logDialogVisible = ref(false)
const currentLogAgent = ref<AgentDisplay | null>(null)
const logContent = ref('')

const restartDialogVisible = ref(false)
const restartingAgent = ref<AgentDisplay | null>(null)
const restartProgress = ref(0)
const restartStatusText = ref('等待执行...')
const restartStarted = ref(false)
let restartTimer: ReturnType<typeof setInterval> | null = null

const addAgentDialogVisible = ref(false)
const newAgent = ref({ name: '', layer: 'execution', model: 'deepseek-v4', description: '' })

// ==================== 全流程编排 ====================
const pipelineForm = reactive({
  candidateName: '',
  jobTitle: '',
  requiredSkills: '',
  minYears: 3,
  offerPackage: 50,
  resumeText: '',
  interviewSummary: '',
})
const pipelineRunning = ref(false)
const pipelineResult = ref<PipelineResultVO | null>(null)

/** 全流程编排：部门树 + 岗位选择 */
const pipelineDepartments = ref<DepartmentTreeVO[]>([])
const pipelineJobs = ref<JobVO[]>([])
const pipelineDeptId = ref('')
const pipelineJobId = ref('')

/** 按所选部门过滤岗位。 */
const filteredPipelineJobs = computed(() =>
  pipelineDeptId.value
    ? pipelineJobs.value.filter(j => String(j.departmentId) === String(pipelineDeptId.value))
    : pipelineJobs.value,
)

function onPipelineDeptChange() {
  pipelineJobId.value = ''
}

/** 选中岗位后自动带出职位名称、必备技能与最低年限。 */
function onPipelineJobSelect(jobId: string) {
  const job = filteredPipelineJobs.value.find(j => j.id === jobId)
  if (!job) return
  pipelineForm.jobTitle = job.title
  pipelineForm.requiredSkills = (job.skills || []).join(',')
  pipelineForm.minYears = estimateMinYears(job.experienceLevel)
}

/** 经验等级 → 最低工作年限估算。 */
function estimateMinYears(level: number): number {
  const map: Record<number, number> = { 0: 0, 1: 1, 2: 3, 3: 5, 4: 8, 5: 10 }
  return map[level] ?? 3
}

async function loadPipelineOptions() {
  try {
    const [jobsRes, deptTree] = await Promise.all([
      getJobs({ page: 1, size: 100, status: 1 }),
      getDepartments(),
    ])
    pipelineJobs.value = jobsRes?.records || []
    pipelineDepartments.value = deptTree || []
  } catch {
    pipelineJobs.value = []
    pipelineDepartments.value = []
  }
}

const pipelineParsing = ref(false)
const pipelineParsedResume = ref<PipelineResumeParseVO | null>(null)
const pipelineUploadAccept = '.pdf,.doc,.docx,.jpg,.jpeg,.png,.gif,.webp'

/** 上传简历文件 → 同步解析（文本/AI 视觉）→ 回填候选人姓名与简历文本。 */
async function handlePipelineResumeFile(uploadFile: { raw: File }) {
  if (!uploadFile?.raw) return
  pipelineParsing.value = true
  try {
    const result = await parseResumeFile(uploadFile.raw)
    pipelineParsedResume.value = result
    const name = result.parsed?.name?.trim()
    if (name && !pipelineForm.candidateName.trim()) {
      pipelineForm.candidateName = name
    }
    if (result.rawText) {
      pipelineForm.resumeText = result.rawText
    }
    ElMessage.success('简历解析完成，已自动填入表单')
  } catch {
    ElMessage.error('简历解析失败，请查看原因或手动粘贴文本')
  } finally {
    pipelineParsing.value = false
  }
}

/** 运行「简历 → Offer」全流程编排。 */
async function runPipeline() {
  if (!pipelineForm.candidateName.trim() || !pipelineForm.jobTitle.trim()) {
    ElMessage.warning('请填写候选人姓名和目标职位')
    return
  }
  pipelineRunning.value = true
  pipelineResult.value = null
  try {
    const result = await apiRunPipeline({
      candidateName: pipelineForm.candidateName.trim(),
      jobTitle: pipelineForm.jobTitle.trim(),
      resumeText: pipelineForm.resumeText || undefined,
      requiredSkills: pipelineForm.requiredSkills
        ? pipelineForm.requiredSkills.split(/[,，]/).map(s => s.trim()).filter(Boolean)
        : undefined,
      minYearsOfExperience: pipelineForm.minYears ?? undefined,
      offerTotalPackage: pipelineForm.offerPackage ? pipelineForm.offerPackage * 10000 : undefined,
      interviewSummary: pipelineForm.interviewSummary || undefined,
    })
    pipelineResult.value = result
    // 编排产生的任务/指标会实时更新监控数据
    refreshAgents()
    refreshTasks()
    ElMessage.success(`全流程编排执行完成，共 ${result.stages?.length ?? 0} 个阶段`)
  } catch {
    ElMessage.error('全流程编排执行失败，请检查输入后重试')
  } finally {
    pipelineRunning.value = false
  }
}

function resetPipeline() {
  Object.assign(pipelineForm, {
    candidateName: '',
    jobTitle: '',
    requiredSkills: '',
    minYears: 3,
    offerPackage: 50,
    resumeText: '',
    interviewSummary: '',
  })
  pipelineDeptId.value = ''
  pipelineJobId.value = ''
  pipelineParsedResume.value = null
  pipelineResult.value = null
}

const activeEventFilter = ref('ALL')
const tokenChartRef = ref<HTMLCanvasElement>()
let tokenChart: Chart | null = null
let taskTimer: ReturnType<typeof setInterval> | null = null
let agentTimer: ReturnType<typeof setInterval> | null = null
let eventSource: EventSource | null = null

/** 将后端 AgentInfoVO 映射为前端展示对象。 */
function toAgentDisplay(vo: AgentInfoVO): AgentDisplay {
  const meta = AGENT_META[vo.id] ?? {
    icon: markRaw(Setting),
    color: '#3b82f6',
    layer: TYPE_TO_LAYER[vo.type] ?? 'execution',
  }
  const config = vo.config ?? {}
  return {
    id: vo.id,
    name: vo.name,
    displayName: vo.name,
    model: (config.model as string) || vo.name,
    layer: meta.layer,
    icon: meta.icon,
    description: vo.description || vo.name,
    status: vo.status ?? 0,
    callsPerHour: Number(config.callsPerHour ?? 0),
    latencyMs: vo.metrics?.avgResponseTime ?? 0,
    accuracy: vo.metrics?.successRate ?? 0,
    health: vo.health ?? 0,
    tokens24h: Number(config.tokens24h ?? 0),
    tasksCompleted: vo.metrics?.tasksCompleted ?? 0,
    color: meta.color,
  }
}

/** 将后端事件 VO 映射为前端事件项。 */
function toEventItem(ev: AgentEventVO): EventItem {
  const levelMap: Record<number, EventItem['level']> = {
    0: 'INFO',
    1: 'WARNING',
    2: 'ERROR',
    3: 'SUCCESS',
  }
  const ts = ev.timestamp ? ev.timestamp : new Date().toISOString()
  return {
    id: ev.eventId,
    level: levelMap[ev.type] ?? 'INFO',
    agent: ev.agentName || ev.agentId || '未知',
    text: ev.message || 'Agent 事件',
    time: formatDateTime(ts),
  }
}

// ==================== COMPUTED ====================
const orchestrationAgents = computed(() => agents.value.filter(a => a.layer === 'orchestration'))
const executionAgents = computed(() => agents.value.filter(a => a.layer === 'execution'))
const reviewAgents = computed(() => agents.value.filter(a => a.layer === 'review'))

const totalCalls = computed(() => fmt(agents.value.reduce((s, a) => s + a.callsPerHour, 0)))
const avgLatency = computed(() => agents.value.length === 0
  ? 0
  : Math.round(agents.value.reduce((s, a) => s + a.latencyMs, 0) / agents.value.length))
const avgHealth = computed(() => agents.value.length === 0
  ? '0.0'
  : (agents.value.reduce((s, a) => s + a.health, 0) / agents.value.length).toFixed(1))
const totalTokens = computed(() => fmt(agents.value.reduce((s, a) => s + a.tokens24h, 0)))

const eventFilters = [
  { key: 'ALL', label: '全部' },
  { key: 'INFO', label: '信息' },
  { key: 'SUCCESS', label: '成功' },
  { key: 'WARNING', label: '警告' },
  { key: 'ERROR', label: '错误' },
]

const filteredEvents = computed(() => {
  if (activeEventFilter.value === 'ALL') return events.value
  return events.value.filter(e => e.level === activeEventFilter.value)
})

const chartColors = [
  '#8b5cf6', '#3b82f6', '#6366f1', '#60a5fa',
  '#818cf8', '#93c5fd', '#a5b4fc', '#10b981', '#34d399'
]

const tokenLegendItems = computed(() => {
  const total = agents.value.reduce((s, a) => s + a.tokens24h, 0)
  return agents.value.map((a, i) => ({
    name: a.displayName,
    value: (a.tokens24h / 1000).toFixed(0),
    pct: total > 0 ? ((a.tokens24h / total) * 100).toFixed(1) : '0.0',
    color: chartColors[i % chartColors.length],
  }))
})

// ==================== HELPERS ====================
function fmt(n: number): string {
  if (n >= 10000) return (n / 10000).toFixed(1) + '万'
  return n.toLocaleString()
}

function tokenK(n: number): string {
  return (n / 1000).toFixed(0)
}

function gradient(color: string): string {
  return `linear-gradient(135deg, ${color}, ${color}dd)`
}

function healthClass(h: number): string {
  if (h >= 95) return 'green'
  if (h >= 90) return 'yellow'
  return 'red'
}

function healthLabel(h: number): string {
  if (h >= 95) return '健康'
  if (h >= 90) return '注意'
  return '异常'
}

function statusLabel(s: string): string {
  switch (s) {
    case 'RUNNING': return '执行中'
    case 'QUEUED': return '排队中'
    case 'COMPLETED': return '已完成'
    case 'FAILED': return '失败'
    case 'RETRYING': return '重试中'
    default: return s
  }
}

// ==================== ACTIONS ====================
function scrollToAgent(id: string) {
  highlightedAgent.value = id
  const card = document.getElementById(`card-${id}`)
  if (card) {
    card.scrollIntoView({ behavior: 'smooth', block: 'center' })
    setTimeout(() => { highlightedAgent.value = null }, 2000)
  }
  const agent = agents.value.find(a => a.id === id)
  if (agent) ElMessage.info(`定位到 ${agent.displayName}`)
}

async function openLogModal(agent: AgentDisplay) {
  currentLogAgent.value = agent
  logContent.value = '正在加载真实运行日志...'
  logDialogVisible.value = true
  try {
    const logs = await getAgentLogs(agent.id)
    if (!logs || logs.length === 0) {
      logContent.value = '暂无运行日志（智能体尚未产生任务或事件）'
      return
    }
    const lines = logs.map((log: AgentLogVO) => {
      const ts = log.startedAt || log.completedAt
      const time = ts ? formatDateTime(ts) : '--'
      const status = statusLabel(mapLogStatus(log.status))
      const type = mapLogType(log.taskType)
      const duration = log.durationMs != null ? `${log.durationMs}ms` : '--'
      const parts = [`[${time}] [${status}] ${type} 耗时=${duration}`]
      if (log.inputSummary) parts.push(`  输入: ${log.inputSummary}`)
      if (log.outputSummary) parts.push(`  输出: ${log.outputSummary}`)
      return parts.join('\n')
    })
    logContent.value = lines.join('\n\n')
  } catch {
    logContent.value = '日志加载失败，请稍后重试'
  }
}

function mapLogStatus(status?: number): string {
  const map: Record<number, string> = { 0: 'QUEUED', 1: 'RUNNING', 2: 'COMPLETED', 3: 'FAILED', 4: 'RETRYING' }
  return map[status ?? 0] ?? 'UNKNOWN'
}

function mapLogType(taskType?: number): string {
  const map: Record<number, string> = {
    0: '简历解析', 1: '简历筛选', 2: '面试评估', 3: 'Offer预测',
    4: '内推匹配', 5: '题目生成', 6: 'JD生成', 7: '流程编排',
    8: '智能洞察',
  }
  return taskType != null ? (map[taskType] ?? '运维事件') : '运维事件'
}

/** Agent 任务类型 → 主题色（不同 Agent 用不同专业颜色区分）。 */
const AGENT_TYPE_COLORS: Record<string, string> = {
  '简历解析': '#1677ff',
  '简历筛选': '#13c2c2',
  '面试评估': '#722ed1',
  'Offer预测': '#fa8c16',
  '人才推荐': '#52c41a',
  '内推匹配': '#52c41a',
  '题目生成': '#eb2f96',
  'JD生成': '#faad14',
  '流程编排': '#2f54eb',
  '智能洞察': '#a0d911',
}

function agentTypeColor(type: string): string {
  return AGENT_TYPE_COLORS[type] ?? '#64748b'
}

function agentTypeTagStyle(type: string): Record<string, string> {
  const color = agentTypeColor(type)
  return {
    color,
    background: `${color}14`,
    borderColor: `${color}40`,
  }
}

function downloadLogs() {
  const blob = new Blob([logContent.value], { type: 'text/plain' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `agent-runtime-${new Date().toISOString().slice(0, 10)}.log`
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success('日志已下载')
}

function openRestartModal(agent: AgentDisplay) {
  restartingAgent.value = agent
  restartProgress.value = 0
  restartStatusText.value = '等待执行...'
  restartStarted.value = false
  restartDialogVisible.value = true
  if (restartTimer) clearInterval(restartTimer)
}

async function confirmRestart() {
  const agentId = restartingAgent.value?.id
  if (!agentId) return
  restartStarted.value = true
  restartStatusText.value = '正在通知 AgentScope Runtime 重启...'
  restartProgress.value = 30
  try {
    await restartAgent(agentId)
    restartProgress.value = 70
    restartStatusText.value = '运行时已重启，正在刷新状态...'
    await refreshAgents()
    restartProgress.value = 100
    restartStatusText.value = '重启完成！Agent 已恢复运行'
    ElMessage.success(`${restartingAgent.value?.name} 重启成功`)
  } catch {
    restartStatusText.value = '重启失败，请稍后重试'
    ElMessage.error(`${restartingAgent.value?.name} 重启失败`)
  } finally {
    restartStarted.value = false
    if (restartTimer) clearInterval(restartTimer)
    setTimeout(() => { restartDialogVisible.value = false }, 1200)
  }
}

function cancelRestart() {
  if (restartTimer) clearInterval(restartTimer)
  restartDialogVisible.value = false
}

async function pauseAgentAction(agent: AgentDisplay) {
  try {
    if (agent.status === 2) {
      await resumeAgent(agent.id)
      ElMessage.success(`Agent ${agent.displayName} 已恢复`)
    } else {
      await pauseAgent(agent.id)
      ElMessage.warning(`Agent ${agent.displayName} 已暂停，后续能力调用将被拒绝`)
    }
    await refreshAgents()
  } catch {
    ElMessage.error(`Agent ${agent.displayName} 操作失败`)
  }
}

async function registerAgent() {
  const displayName = newAgent.value.name.trim()
  if (!displayName) {
    ElMessage.warning('请输入 Agent 名称')
    return
  }
  const type = newAgent.value.layer === 'orchestration' ? 0 : newAgent.value.layer === 'review' ? 2 : 1
  try {
    const created = await apiRegisterAgent({
      displayName,
      type,
      model: newAgent.value.model,
      description: newAgent.value.description || undefined,
      config: { timeoutSeconds: 300, maxRetries: 3 },
    })
    addAgentDialogVisible.value = false
    newAgent.value = { name: '', layer: 'execution', model: 'deepseek-v4', description: '' }
    await refreshAgents()
    ElMessage.success(`Agent "${created.name}" 已注册并加入编排网络`)
  } catch {
    ElMessage.error('Agent 注册失败，请检查名称是否已存在')
  }
}

// ==================== 真实数据加载 ====================

/** 刷新 Agent 列表（注册表 + 最近指标聚合）。 */
async function refreshAgents() {
  try {
    const list = await getAgents()
    if (Array.isArray(list)) {
      agents.value = list.map(toAgentDisplay)
      renderTokenChart()
    }
  } catch {
    // 网络异常时保留旧数据，避免页面闪烁
  }
}

/** 刷新任务队列（真实活跃任务）。 */
async function refreshTasks() {
  try {
    const list = await getTaskQueue()
    tasks.value = (Array.isArray(list) ? list : []).map((t: TaskQueueItemVO) => ({
      id: t.id,
      agent: t.agentName || t.agentId,
      type: t.type,
      priority: t.priority as 'HIGH' | 'MEDIUM' | 'LOW',
      status: t.status as TaskItem['status'],
      time: t.createdAt ? formatDateTime(t.createdAt) : '--',
      duration: t.durationMs ?? '--',
    }))
  } catch {
    // 网络异常时保留旧数据
  }
}

/** 建立真实 SSE 事件流（读取 ai_event_log）。 */
function connectSse() {
  disconnectSse()
  try {
    eventSource = connectEventStream()
    eventSource.addEventListener('agent-event', (ev: MessageEvent) => {
      let payload: AgentEventVO | null = null
      try {
        payload = JSON.parse(ev.data as string) as AgentEventVO
      } catch {
        return
      }
      if (!payload) return
      events.value.unshift(toEventItem(payload))
      if (events.value.length > 100) events.value.splice(100)
    })
    eventSource.onerror = () => {
      // 浏览器 EventSource 会自动重连，这里仅做日志
      console.info('[AgentMonitor] SSE 连接中断，等待自动重连...')
    }
  } catch (e) {
    console.warn('[AgentMonitor] SSE 初始化失败:', e)
  }
}

function disconnectSse() {
  if (eventSource) {
    eventSource.close()
    eventSource = null
  }
}

// ==================== CHART ====================
function renderTokenChart() {
  if (!tokenChartRef.value) return
  if (tokenChart) tokenChart.destroy()
  const agentNames = agents.value.map(a => a.displayName)
  const tokenData = agents.value.map(a => a.tokens24h)
  tokenChart = new Chart(tokenChartRef.value, {
    type: 'doughnut',
    data: {
      labels: agentNames,
      datasets: [{
        data: tokenData,
        backgroundColor: chartColors.slice(0, agents.value.length),
        borderColor: '#fff',
        borderWidth: 3,
      }],
    },
    options: {
      responsive: true,
      maintainAspectRatio: true,
      cutout: '60%',
      plugins: {
        legend: { display: false },
        tooltip: {
          callbacks: {
            label: (ctx: { label?: string; raw: unknown; dataset: { data: number[] } }) => {
              const total = (ctx.dataset.data as number[]).reduce((a: number, b: number) => a + b, 0)
              const pct = ((ctx.raw as number / total) * 100).toFixed(1)
              return `${ctx.label}: ${((ctx.raw as number) / 1000).toFixed(0)}K (${pct}%)`
            },
          },
        },
      },
    },
  })
}

// ==================== LIFECYCLE ====================
async function loadData() {
  await Promise.all([refreshAgents(), refreshTasks()])
}

function startTimers() {
  taskTimer = setInterval(() => {
    refreshTasks()
  }, 8000)
  agentTimer = setInterval(() => {
    refreshAgents()
  }, 15000)
  connectSse()
}

function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape') {
    logDialogVisible.value = false
    restartDialogVisible.value = false
    addAgentDialogVisible.value = false
  }
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') {
    e.preventDefault()
    addAgentDialogVisible.value = true
  }
  if ((e.ctrlKey || e.metaKey) && e.key === 'r') {
    e.preventDefault()
    refreshAll()
  }
}

function refreshAll() {
  loadData()
  connectSse()
  ElMessage.info('已刷新所有数据')
}

onMounted(async () => {
  await loadData()
  await loadPipelineOptions()
  await nextTick()
  renderTokenChart()
  startTimers()
  document.addEventListener('keydown', onKeydown)
})

onBeforeUnmount(() => {
  if (taskTimer) clearInterval(taskTimer)
  if (agentTimer) clearInterval(agentTimer)
  if (restartTimer) clearInterval(restartTimer)
  disconnectSse()
  if (tokenChart) tokenChart.destroy()
  document.removeEventListener('keydown', onKeydown)
})
</script>

<style scoped>
/* ========== CSS VARIABLES ========== */
:root {
  --c-orchestration: #8b5cf6;
  --c-orchestration-bg: #f5f3ff;
  --c-execution: #3b82f6;
  --c-execution-bg: #eff6ff;
  --c-review: #10b981;
  --c-review-bg: #ecfdf5;
  --c-success: #059669;
  --c-success-bg: #ecfdf5;
  --c-warning: #d97706;
  --c-warning-bg: #fffbeb;
  --c-danger: #dc2626;
  --c-danger-bg: #fef2f2;
  --c-info: #0ea5e9;
  --c-info-bg: #f0f9ff;
}

.orchestration-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding-bottom: 24px;
}

/* ========== ARCHITECTURE DIAGRAM ========== */
.arch-card {
  background: var(--c-card-bg, #fff);
  border-radius: 12px;
  border: 1px solid var(--c-border, #e2e8f0);
  padding: 24px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
}

.arch-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.arch-header h2 {
  font-size: 16px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
}

.arch-badge {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 500;
  background: var(--c-primary-bg, #eef2ff);
  color: var(--c-primary, #4f46e5);
}

.arch-diagram {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.arch-layer {
  border: 2px dashed var(--c-border, #e2e8f0);
  border-radius: 12px;
  padding: 16px 20px;
  position: relative;
  transition: all 0.3s;
}

.arch-layer:hover {
  box-shadow: 0 4px 6px rgba(0,0,0,0.07);
}

.arch-layer .layer-label {
  position: absolute;
  top: -11px;
  left: 20px;
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 1px;
  padding: 2px 12px;
  border-radius: 20px;
  color: #fff;
}

.layer-orchestration {
  border-color: var(--c-orchestration);
  background: var(--c-orchestration-bg);
}
.layer-orchestration .layer-label { background: var(--c-orchestration); }

.layer-execution {
  border-color: var(--c-execution);
  background: var(--c-execution-bg);
}
.layer-execution .layer-label { background: var(--c-execution); }

.layer-review {
  border-color: var(--c-review);
  background: var(--c-review-bg);
}
.layer-review .layer-label { background: var(--c-review); }

.agent-nodes {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: center;
  margin-top: 8px;
}

.agent-node {
  background: #fff;
  border: 1.5px solid var(--c-border, #e2e8f0);
  border-radius: 12px;
  padding: 14px 16px;
  cursor: pointer;
  transition: all 0.15s;
  text-align: center;
  min-width: 130px;
  flex: 1;
  max-width: 180px;
  position: relative;
}

.agent-node:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(0,0,0,0.1);
  border-color: var(--c-primary, #4f46e5);
}

.agent-node .agent-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 8px;
  font-size: 18px;
  color: #fff;
}

.agent-node .agent-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--c-text, #0f172a);
  margin-bottom: 2px;
}

.agent-node .agent-model {
  font-size: 11px;
  color: var(--c-text-muted, #94a3b8);
}

.agent-node .agent-status {
  margin-top: 6px;
  font-size: 10px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 10px;
  display: inline-block;
}

.agent-status.green { background: var(--c-success-bg); color: var(--c-success); }
.agent-status.yellow { background: var(--c-warning-bg); color: var(--c-warning); }
.agent-status.red { background: var(--c-danger-bg); color: var(--c-danger); }

.arch-bus {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 8px;
  color: var(--c-text-muted, #94a3b8);
  font-size: 12px;
  font-weight: 500;
}

.arch-bus .bus-line {
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--c-primary-light, #6366f1), transparent);
}

/* ========== SECTION HEADER ========== */
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.section-header h2 {
  font-size: 16px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
}

.stats-summary {
  font-size: 13px;
  color: var(--c-text-secondary, #64748b);
}

/* ========== AGENT CARDS ========== */
.agent-cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

.agent-card {
  background: var(--c-card-bg, #fff);
  border-radius: 12px;
  border: 1px solid var(--c-border, #e2e8f0);
  padding: 20px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
  transition: all 0.15s;
  cursor: pointer;
}

.agent-card:hover {
  box-shadow: 0 4px 6px rgba(0,0,0,0.07);
  border-color: var(--c-primary-light, #6366f1);
}

.agent-card.highlight {
  border-color: var(--c-primary, #4f46e5);
  box-shadow: 0 0 0 3px var(--c-primary-bg, #eef2ff);
}

.card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 16px;
}

.card-info .card-icon {
  width: 42px;
  height: 42px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
  margin-bottom: 8px;
}

.card-name {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 2px;
}

.card-name .paused-tag,
.card-name .error-tag {
  display: inline-block;
  margin-left: 6px;
  padding: 1px 8px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 500;
  vertical-align: 2px;
}

.card-name .paused-tag {
  color: #b45309;
  background: #fffbeb;
  border: 1px solid #fcd34d;
}

.card-name .error-tag {
  color: #b91c1c;
  background: #fef2f2;
  border: 1px solid #fca5a5;
}

.card-model {
  font-size: 12px;
  color: var(--c-text-muted, #94a3b8);
}

.health-bar-wrap {
  text-align: right;
}

.health-pct {
  font-size: 20px;
  font-weight: 700;
}

.health-pct.green { color: var(--c-success); }
.health-pct.yellow { color: var(--c-warning); }
.health-pct.red { color: var(--c-danger); }

.health-bar {
  width: 60px;
  height: 6px;
  border-radius: 3px;
  background: #f1f5f9;
  margin-top: 6px;
  overflow: hidden;
}

.health-bar .fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.6s;
}

.health-bar .fill.green { background: var(--c-success); }
.health-bar .fill.yellow { background: var(--c-warning); }
.health-bar .fill.red { background: var(--c-danger); }

.health-label {
  font-size: 11px;
  color: var(--c-text-muted, #94a3b8);
  margin-top: 2px;
}

.metrics {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-bottom: 10px;
}

.metric-item {
  background: #f1f5f9;
  border-radius: 8px;
  padding: 8px 12px;
}

.metric-value {
  font-size: 18px;
  font-weight: 700;
  color: var(--c-text, #0f172a);
}

.metric-unit {
  font-size: 12px;
  color: var(--c-text-muted, #94a3b8);
  margin-left: 2px;
}

.metric-label {
  font-size: 11px;
  color: var(--c-text-muted, #94a3b8);
  margin-top: 2px;
}

.card-description {
  font-size: 12px;
  color: var(--c-text-muted, #94a3b8);
  margin: 10px 0;
  line-height: 1.5;
}

.card-actions {
  display: flex;
  gap: 8px;
  margin-top: 14px;
}

/* ========== TWO COLUMN ========== */
.two-col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

@media (max-width: 1200px) {
  .two-col {
    grid-template-columns: 1fr;
  }
  .agent-cards-grid {
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  }
  .agent-node {
    min-width: 100px;
    padding: 10px 8px;
  }
}

/* ========== PANEL ========== */
.panel {
  background: var(--c-card-bg, #fff);
  border-radius: 12px;
  border: 1px solid var(--c-border, #e2e8f0);
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
  overflow: hidden;
}

.panel-header {
  padding: 16px 20px;
  border-bottom: 1px solid var(--c-border, #e2e8f0);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.panel-header h3 {
  font-size: 13px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
}

.panel-count {
  font-size: 12px;
  color: var(--c-text-muted, #94a3b8);
}

.panel-body {
  padding: 16px 20px;
  max-height: 340px;
  overflow-y: auto;
}

/* 任务队列 + 实时事件流：两个窗口使用完全相同的固定高度与样式，
   不依赖网格拉伸；事件流内容超出时在窗口内部滚动 */
.task-queue-panel .panel-body,
.event-stream-panel .panel-body {
  height: 560px;
  max-height: none;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
}

/* 任务队列：表格按内容自适应高度，禁止出现垂直滚动条 */
.task-queue-panel .panel-body {
  overflow: hidden;
}

/* 实时事件流：内容超出窗口高度时内部滚动 */
.event-stream-panel .panel-body {
  overflow-y: auto;
}

/* ========== TASK TABLE ========== */
.task-id {
  font-family: 'SF Mono', 'Consolas', monospace;
  font-size: 12px;
  color: var(--c-primary, #4f46e5);
  font-weight: 600;
}

/* Agent 类型区分圆点 */
.agent-type-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
  display: inline-block;
}

.priority-tag {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 600;
}

.priority-tag.high { background: var(--c-danger-bg); color: var(--c-danger); }
.priority-tag.medium { background: var(--c-warning-bg); color: var(--c-warning); }
.priority-tag.low { background: var(--c-info-bg); color: var(--c-info); }

.status-tag {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 500;
}

.status-tag.running { background: #dbeafe; color: #2563eb; }
.status-tag.completed { background: var(--c-success-bg); color: var(--c-success); }
.status-tag.queued { background: var(--c-warning-bg); color: var(--c-warning); }
.status-tag.failed { background: var(--c-danger-bg); color: var(--c-danger); }

/* ========== EVENT STREAM ========== */
.event-filters {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.event-filter {
  font-size: 11px;
  padding: 3px 10px;
  border-radius: 12px;
  border: 1px solid var(--c-border, #e2e8f0);
  background: #fff;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.15s;
  color: var(--c-text-secondary, #64748b);
  font-family: inherit;
}

.event-filter:hover {
  border-color: var(--c-primary, #4f46e5);
  color: var(--c-primary, #4f46e5);
}

.event-filter.active {
  background: var(--c-primary, #4f46e5);
  color: #fff;
  border-color: var(--c-primary, #4f46e5);
}

.event-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.event-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 8px;
  background: #f1f5f9;
  font-size: 13px;
  animation: eventFadeIn 0.3s ease;
}

@keyframes eventFadeIn {
  from { opacity: 0; transform: translateY(-4px); }
  to { opacity: 1; transform: translateY(0); }
}

.event-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-top: 5px;
  flex-shrink: 0;
}

.event-dot.info { background: var(--c-info); }
.event-dot.success { background: var(--c-success); }
.event-dot.warning { background: var(--c-warning); }
.event-dot.error { background: var(--c-danger); }

.event-time {
  font-size: 11px;
  color: var(--c-text-muted, #94a3b8);
  white-space: nowrap;
  min-width: 140px;
}

.event-text {
  flex: 1;
  color: var(--c-text-secondary, #64748b);
}

.event-agent {
  font-weight: 600;
  color: var(--c-primary, #4f46e5);
  font-size: 11px;
  white-space: nowrap;
}

.event-empty {
  text-align: center;
  padding: 24px;
  color: var(--c-text-secondary, #64748b);
}

/* ========== TOKEN CHART ========== */
.token-chart-wrap {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 8px 0;
}

.token-chart-canvas {
  width: 200px;
  height: 200px;
  flex-shrink: 0;
}

.token-chart-canvas canvas {
  max-width: 200px;
  max-height: 200px;
}

.token-legend {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 12px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: space-between;
}

.legend-color {
  width: 10px;
  height: 10px;
  border-radius: 2px;
  flex-shrink: 0;
}

.legend-name {
  flex: 1;
  color: var(--c-text-secondary, #64748b);
}

.legend-val {
  color: var(--c-text, #0f172a);
  font-weight: 600;
}

/* ========== LOG MODAL ========== */
.log-block {
  background: #1e293b;
  color: #e2e8f0;
  padding: 16px;
  border-radius: 8px;
  font-family: 'SF Mono', 'Consolas', monospace;
  font-size: 12px;
  line-height: 1.8;
  max-height: 400px;
  overflow-y: auto;
  white-space: pre-wrap;
}

/* ========== RESTART MODAL ========== */
.restart-info {
  font-size: 14px;
  color: var(--c-text-secondary, #64748b);
  margin-bottom: 16px;
}

.restart-info strong {
  color: var(--c-text, #0f172a);
}

.progress-bar-wrap {
  background: #f1f5f9;
  border-radius: 6px;
  height: 8px;
  margin-bottom: 12px;
  overflow: hidden;
}

.progress-bar {
  height: 100%;
  background: linear-gradient(90deg, var(--c-primary, #4f46e5), var(--c-primary-light, #6366f1));
  border-radius: 6px;
  transition: width 0.3s;
}

.restart-status-text {
  font-size: 12px;
  color: var(--c-text-muted, #94a3b8);
  text-align: center;
  margin: 0;
}

/* ========== 全流程编排面板 ========== */
.pipeline-panel {
  margin-bottom: 24px;
}

.pipeline-body {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  padding: 16px 20px;
}

.pipeline-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 16px;
}

.pipeline-actions {
  display: flex;
  gap: 8px;
}

.pipeline-parse-summary {
  font-size: 13px;
  color: var(--c-text-secondary);
}

.pipeline-result {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 16px;
  min-height: 480px;
  max-height: none;
  overflow: visible;
}

.pipeline-summary {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}

.summary-item {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 10px 12px;
  text-align: center;
}

.summary-label {
  display: block;
  font-size: 12px;
  color: #94a3b8;
  margin-bottom: 4px;
}

.summary-value {
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
}

.summary-value.ok { color: #059669; }
.summary-value.fail { color: #dc2626; }

.pipeline-stages {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.stage-item {
  display: flex;
  gap: 12px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 10px 12px;
}

.stage-icon {
  flex: none;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
}

.stage-icon.completed { background: #ecfdf5; color: #059669; }
.stage-icon.failed { background: #fef2f2; color: #dc2626; }
.stage-icon.skipped { background: #f1f5f9; color: #94a3b8; }

.stage-info {
  flex: 1;
  min-width: 0;
}

.stage-title {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  display: flex;
  align-items: center;
  gap: 8px;
}

.stage-status {
  font-size: 11px;
  font-weight: 500;
  padding: 1px 8px;
  border-radius: 10px;
}

.stage-status.completed { color: #059669; background: #ecfdf5; }
.stage-status.failed { color: #dc2626; background: #fef2f2; }
.stage-status.skipped { color: #64748b; background: #f1f5f9; }

.stage-message {
  font-size: 12px;
  color: #475569;
  margin-top: 3px;
  word-break: break-all;
}

.stage-meta {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 3px;
}

@media (max-width: 960px) {
  .pipeline-body {
    grid-template-columns: 1fr;
  }
}
</style>

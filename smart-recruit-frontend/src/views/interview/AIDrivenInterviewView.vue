<template>
  <div class="ai-interview-page">
    <!-- ===== 页面头部 ===== -->
    <div class="sr-page-header">
      <div class="header-left">
        <h2 class="header-title">AI 智能面试</h2>
        <span class="ai-status-badge">
          <span class="pulse-dot"></span> AI 引擎运行中
        </span>
      </div>
      <div class="header-right">
        <span class="current-date">{{ formattedDate }}</span>
      </div>
    </div>

    <!-- ===== 统计卡片 ===== -->
    <div class="stat-row">
      <div class="stat-card" v-for="card in statsCards" :key="card.key">
        <div class="stat-icon" :style="{ background: card.bgColor, color: card.color }">
          <el-icon :size="22"><component :is="card.icon" /></el-icon>
        </div>
        <div class="stat-body">
          <div class="stat-value">{{ card.value }}</div>
          <div class="stat-label">{{ card.label }}</div>
        </div>
      </div>
    </div>

    <!-- ===== 双列布局：面试日程 + AI 智能出题 ===== -->
    <div class="two-col">
      <!-- 面试日程 -->
      <div class="card">
        <div class="card-header">
          <h2><el-icon><Calendar /></el-icon> 面试日程</h2>
          <div class="tab-switcher">
            <button
              class="tab-btn"
              :class="{ active: scheduleTab === 'day' }"
              @click="handleScheduleTabSwitch('day')"
            >日视图</button>
            <button
              class="tab-btn"
              :class="{ active: scheduleTab === 'week' }"
              @click="handleScheduleTabSwitch('week')"
            >周视图</button>
          </div>
        </div>
        <div class="card-body no-padding">
          <div v-if="scheduleLoading" class="schedule-loading">
            <el-skeleton animated :count="5" />
          </div>

          <!-- 日视图 -->
          <template v-else-if="scheduleTab === 'day'">
            <ul class="interview-list" v-if="scheduleList.length">
              <li
                v-for="item in scheduleList"
                :key="item.id"
                class="interview-item"
                :class="{
                  selected: selectedScheduleId === item.id,
                  's-pending': item.statusTag === 'pending',
                  's-ongoing': item.statusTag === 'ongoing',
                  's-done': item.statusTag === 'done',
                  's-cancelled': item.statusTag === 'cancelled',
                }"
                @click="selectedScheduleId = item.id"
              >
                <div class="time-slot">{{ item.startTime }}</div>
                <div class="item-body">
                  <div class="candidate-name">{{ item.candidateName }}</div>
                  <div class="item-meta">
                    <span>{{ item.jobTitle }}</span>
                    <span class="meta-divider"></span>
                    <span class="type-tag">{{ item.typeLabel }}</span>
                    <span class="meta-divider"></span>
                    <span>{{ item.interviewerName }}</span>
                  </div>
                </div>
                <span class="status-badge-sm" :class="'s-' + item.statusTag">
                  {{ statusTagLabel(item.statusTag) }}
                </span>
              </li>
            </ul>
            <div v-else class="empty-state">
              <el-icon :size="40"><Calendar /></el-icon>
              <p>暂无面试日程</p>
            </div>
          </template>

          <!-- 周视图 -->
          <template v-else>
            <div v-if="weekDays.length" class="week-schedule">
              <div v-for="day in weekDays" :key="day.date" class="week-day">
                <div class="week-day-header">
                  <span class="week-day-name">{{ day.label }}</span>
                  <span class="week-day-date">{{ day.dateStr }}</span>
                  <span class="week-day-count">{{ day.count }} 场</span>
                </div>
                <ul v-if="day.items.length" class="interview-list">
                  <li
                    v-for="item in day.items"
                    :key="item.id"
                    class="interview-item"
                    :class="{
                      selected: selectedScheduleId === item.id,
                      's-pending': item.statusTag === 'pending',
                      's-ongoing': item.statusTag === 'ongoing',
                      's-done': item.statusTag === 'done',
                      's-cancelled': item.statusTag === 'cancelled',
                    }"
                    @click="selectedScheduleId = item.id"
                  >
                    <div class="time-slot">{{ item.startTime }}</div>
                    <div class="item-body">
                      <div class="candidate-name">{{ item.candidateName }}</div>
                      <div class="item-meta">
                        <span>{{ item.jobTitle }}</span>
                        <span class="meta-divider"></span>
                        <span class="type-tag">{{ item.typeLabel }}</span>
                        <span class="meta-divider"></span>
                        <span>{{ item.interviewerName }}</span>
                      </div>
                    </div>
                    <span class="status-badge-sm" :class="'s-' + item.statusTag">
                      {{ statusTagLabel(item.statusTag) }}
                    </span>
                  </li>
                </ul>
                <div v-else class="week-day-empty">
                  <span>暂无面试安排</span>
                </div>
              </div>
            </div>
            <div v-else class="empty-state">
              <el-icon :size="40"><Calendar /></el-icon>
              <p>暂无面试日程</p>
            </div>
          </template>
        </div>
      </div>

      <!-- AI 智能出题 -->
      <div class="card">
        <div class="card-header">
          <h2><el-icon><MagicStick /></el-icon> AI 智能出题</h2>
        </div>
        <div class="card-body">
          <!-- 选择部门 -->
          <div class="form-group">
            <label class="form-label">选择部门</label>
            <el-tree-select
              v-model="questionDeptId"
              :data="questionDeptOptions"
              :props="{ label: 'name', value: 'id', children: 'children' }"
              placeholder="请选择部门"
              check-strictly
              clearable
              style="width: 100%"
              @change="handleQuestionDeptChange"
            />
          </div>
          <!-- 选择职位 -->
          <div class="form-group">
            <label class="form-label">选择职位</label>
            <el-select
              v-model="questionPosition"
              placeholder="请选择职位..."
              style="width: 100%"
              :disabled="!questionDeptId"
            >
              <el-option
                v-for="pos in positionOptions"
                :key="pos.value"
                :label="pos.label"
                :value="pos.value"
              />
            </el-select>
          </div>

          <!-- 出题控制参数 -->
          <div class="generate-controls">
            <!-- 难度级别 -->
            <div class="form-group">
              <label class="form-label">难度级别</label>
              <div class="radio-group">
                <label class="radio-item" v-for="d in difficultyOptions" :key="d.value">
                  <input type="radio" name="diffLevel" :value="d.value" v-model="questionDifficulty" />
                  {{ d.label }}
                </label>
              </div>
            </div>

            <!-- 题目分类 -->
            <div class="form-group">
              <label class="form-label">题目分类</label>
              <div class="checkbox-group">
                <label class="checkbox-item" v-for="c in categoryOptions" :key="c.value">
                  <input type="checkbox" :value="c.value" v-model="questionCategories" />
                  {{ c.label }}
                </label>
              </div>
            </div>

            <!-- 每类题目数量 -->
            <div class="form-group">
              <label class="form-label">
                每类题目数量
                <span class="form-hint">（共 {{ totalQuestionCount }} 道）</span>
              </label>
              <div v-for="cat in categoryOptions" :key="cat.value" class="count-row">
                <span class="count-row-label" v-show="questionCategories.includes(cat.value)">
                  {{ cat.label }}
                </span>
                <div
                  class="count-stepper"
                  v-show="questionCategories.includes(cat.value)"
                >
                  <button
                    class="stepper-btn"
                    :disabled="questionCounts[cat.value] <= 1"
                    @click="questionCounts[cat.value]--"
                  >
                    <el-icon><Remove /></el-icon>
                  </button>
                  <span class="stepper-value">{{ questionCounts[cat.value] }}</span>
                  <button
                    class="stepper-btn"
                    :disabled="questionCounts[cat.value] >= 50"
                    @click="questionCounts[cat.value]++"
                  >
                    <el-icon><Plus /></el-icon>
                  </button>
                  <el-slider
                    :model-value="questionCounts[cat.value]"
                    :min="1"
                    :max="50"
                    :step="1"
                    style="flex: 1; margin-left: 10px;"
                    @update:model-value="questionCounts[cat.value] = $event"
                  />
                </div>

                <!-- 技术基础题的题型分配 -->
                <div
                  v-if="cat.value === 'tech' && questionCategories.includes('tech')"
                  class="tech-type-sub"
                >
                  <span class="tech-type-hint">题型分配（合计 {{ techTypeTotal }} / {{ questionCounts.tech }}）</span>
                  <div v-for="tt in techTypeOptions" :key="tt.value" class="tech-type-row">
                    <span class="tech-type-label">{{ tt.label }}</span>
                    <div class="count-stepper count-stepper-sm">
                      <button
                        class="stepper-btn"
                        :disabled="techTypeCounts[tt.value] <= 0"
                        @click="techTypeCounts[tt.value]--"
                      >
                        <el-icon><Remove /></el-icon>
                      </button>
                      <span class="stepper-value">{{ techTypeCounts[tt.value] || 0 }}</span>
                      <button
                        class="stepper-btn"
                        :disabled="techTypeTotal >= questionCounts.tech"
                        @click="techTypeCounts[tt.value]++"
                      >
                        <el-icon><Plus /></el-icon>
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 生成按钮 — 全宽 -->
          <button
            class="btn btn-primary btn-block"
            :disabled="questionLoading"
            @click="handleGenerateQuestions"
          >
            <span v-if="questionLoading" class="spinner"></span>
            <el-icon v-else><MagicStick /></el-icon>
            {{ questionLoading ? 'AI 正在生成题目...' : 'AI 智能出题' }}
          </button>

          <!-- 结果区 -->
          <div id="questionsResult" style="margin-top: 16px;">
            <!-- 空状态 -->
            <div
              v-if="!questionLoading && !questionResult"
              class="empty-state"
            >
              <el-icon :size="24" style="display: block; margin: 0 auto 8px;"><Top /></el-icon>
              <p>选择职位后点击"AI 智能出题"生成面试题目</p>
            </div>

            <!-- 加载态 -->
            <div v-if="questionLoading" class="empty-state">
              <div class="spinner dark" style="width:32px;height:32px;display:block;margin:0 auto 12px;"></div>
              <p>AI 正在分析职位要求，生成针对性面试题目...</p>
            </div>

            <!-- 出题结果 -->
            <template v-if="questionResult && !questionLoading">
              <!-- 技术基础题 -->
              <div v-if="questionResult.techQuestions?.length">
                <div class="question-section-label">
                  <el-icon><Monitor /></el-icon> 技术基础题
                </div>
                <div
                  v-for="q in questionResult.techQuestions"
                  :key="'t' + q.number"
                  class="question-item-card"
                >
                  <div class="question-item-top">
                    <span class="q-num">{{ q.number }}.</span>
                    <span class="q-body-text">{{ q.question }}</span>
                    <span v-if="q.questionType" class="type-tag" :class="'type-' + q.questionType">
                      {{ techTypeLabels[q.questionType] || q.questionType }}
                    </span>
                    <span class="diff-tag" :class="diffClass(q.difficultyCode)">{{ q.difficulty }}</span>
                  </div>
                  <div
                    v-if="q.options?.length && q.questionType !== 'essay'"
                    class="question-options-inline"
                  >
                    <span
                      v-for="opt in q.options"
                      :key="opt"
                      class="q-opt-chip"
                    >{{ opt }}</span>
                  </div>
                </div>
              </div>

              <!-- 项目经验题 -->
              <div v-if="questionResult.projectQuestions?.length">
                <div class="question-section-label">
                  <el-icon><FolderOpened /></el-icon> 项目经验题
                </div>
                <div
                  v-for="q in questionResult.projectQuestions"
                  :key="'p' + q.number"
                  class="question-item-card"
                >
                  <span class="q-num">{{ q.number }}.</span>
                  <span>
                    {{ q.question }}
                    <span class="diff-tag" :class="diffClass(q.difficultyCode)">{{ q.difficulty }}</span>
                  </span>
                </div>
              </div>

              <!-- 行为面试题 -->
              <div v-if="questionResult.behavioralQuestions?.length">
                <div class="question-section-label">
                  <el-icon><UserFilled /></el-icon> 行为面试题
                </div>
                <div
                  v-for="q in questionResult.behavioralQuestions"
                  :key="'b' + q.number"
                  class="question-item-card"
                >
                  <span class="q-num">{{ q.number }}.</span>
                  <span>
                    {{ q.question }}
                    <span class="diff-tag" :class="diffClass(q.difficultyCode)">{{ q.difficulty }}</span>
                  </span>
                </div>
              </div>

              <!-- 操作按钮 -->
              <div class="btn-group" style="margin-top:12px;">
                <button class="btn btn-outline btn-sm" @click="copyQuestions">
                  <el-icon><DocumentCopy /></el-icon> 复制全部题目
                </button>
                <button class="btn btn-primary btn-sm" @click="showEmailDialog = true">
                  <el-icon><Promotion /></el-icon> 发送给面试官
                </button>
                <button class="btn btn-success btn-sm" @click="openCreateAssessmentFromQuestions">
                  <el-icon><Plus /></el-icon> 创建为测评
                </button>
                <button class="btn btn-outline btn-sm" @click="openBankDialog">
                  <el-icon><List /></el-icon> 加入面试题库
                </button>
              </div>
            </template>
          </div>
        </div>
      </div>
    </div>

    <!-- 发送邮件对话框 -->
    <el-dialog v-model="showEmailDialog" title="发送面试题给面试官" width="480px" :close-on-click-modal="false">
      <el-form :model="emailForm" label-width="100px">
        <el-form-item label="面试官邮箱" required>
          <el-input v-model="emailForm.email" placeholder="请输入面试官邮箱地址" />
        </el-form-item>
        <el-form-item label="面试官姓名">
          <el-input v-model="emailForm.interviewerName" placeholder="选填，用于邮件问候语" />
        </el-form-item>
        <el-form-item label="发送内容">
          <div class="email-preview">
            <div class="preview-row">
              <span class="preview-label">职位：</span>{{ selectedJobTitle }}
            </div>
            <div class="preview-row">
              <span class="preview-label">技术基础题：</span>{{ questionResult?.techQuestions?.length || 0 }} 道
            </div>
            <div class="preview-row" v-if="questionResult?.projectQuestions?.length">
              <span class="preview-label">项目经验题：</span>{{ questionResult.projectQuestions.length }} 道
            </div>
            <div class="preview-row" v-if="questionResult?.behavioralQuestions?.length">
              <span class="preview-label">行为面试题：</span>{{ questionResult.behavioralQuestions.length }} 道
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEmailDialog = false">取消</el-button>
        <el-button type="primary" :loading="emailSending" @click="handleSendEmail">
          {{ emailSending ? '发送中...' : '发送邮件' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- ===== AI 评估报告 ===== -->
    <div class="card">
      <div class="card-header">
        <h2><el-icon><DataAnalysis /></el-icon> AI 评估报告</h2>
        <div style="display: flex; gap: 8px;">
          <el-tree-select
            v-model="selectedDeptId"
            :data="deptOptions"
            :props="{ label: 'name', value: 'id', children: 'children' }"
            placeholder="请选择部门"
            check-strictly
            clearable
            style="width: 200px;"
            @change="handleDeptChange"
          />
          <el-select
            v-model="selectedCandidateId"
            placeholder="选择已评估的候选人..."
            style="width: 300px;"
            :disabled="!selectedDeptId"
            @change="handleLoadAssessment"
          >
            <el-option
              v-for="c in filteredCandidateOptions"
              :key="c.id"
              :label="c.name + ' - ' + c.jobTitle"
              :value="c.id"
            />
          </el-select>
        </div>
      </div>
      <div class="card-body">
        <!-- 空状态 -->
        <div v-if="!assessmentData && !assessmentLoading" class="empty-state">
          <el-icon :size="32"><DataAnalysis /></el-icon>
          <p>选择已完成的面试候选人查看 AI 评估报告</p>
        </div>

        <!-- 加载态 -->
        <div v-if="assessmentLoading" class="empty-state">
          <div class="spinner dark" style="width:32px;height:32px;display:block;margin:0 auto 12px;"></div>
          <p>正在加载评估报告...</p>
        </div>

        <!-- 报告内容 -->
        <div v-if="assessmentData && !assessmentLoading" class="report-grid">
          <!-- 左侧：评分 + 雷达图 -->
          <div>
            <div
              class="score-circle"
              :class="assessmentData.overallScore >= 80 ? 'high' : assessmentData.overallScore >= 60 ? 'medium' : 'low'"
            >
              <span class="score-num">{{ assessmentData.overallScore }}</span>
              <span class="score-total">/100</span>
            </div>
            <div class="radar-container">
              <canvas ref="radarChartRef"></canvas>
            </div>
          </div>

          <!-- 右侧：详细报告 -->
          <div class="report-detail">
            <h3>{{ assessmentData.candidateName }} - AI 综合评估报告</h3>
            <div class="summary-text">{{ assessmentData.overallComment }}</div>

            <!-- 关键时刻 -->
            <h4 class="key-moments-title">
              <el-icon><Clock /></el-icon> 关键时刻
            </h4>
            <ul class="key-moments">
              <li v-for="(m, i) in assessmentData.keyMoments" :key="i">
                <el-icon><CircleCheckFilled /></el-icon>
                <strong>{{ m.time }}</strong>&nbsp;{{ m.text }}
              </li>
              <li v-if="!assessmentData.keyMoments?.length">
                <span style="color: #94a3b8;">暂无关键时刻记录</span>
              </li>
            </ul>

            <!-- 录用建议横幅 -->
            <div
              class="suggested-action"
              :class="suggestedActionClass(assessmentData.suggestion)"
            >
              <el-icon v-if="assessmentData.suggestion <= 1"><CircleCheck /></el-icon>
              <el-icon v-else-if="assessmentData.suggestion === 3"><CircleClose /></el-icon>
              <el-icon v-else><RefreshRight /></el-icon>
              {{ assessmentData.suggestionLabel }}
            </div>

            <!-- 操作按钮 -->
            <div class="btn-group">
              <button class="btn btn-success" @click="handleUpdateResult(0)">
                <el-icon><CircleCheck /></el-icon> 通过
              </button>
              <button class="btn btn-danger" @click="handleUpdateResult(1)">
                <el-icon><CircleClose /></el-icon> 淘汰
              </button>
              <button class="btn btn-warning" @click="handleUpdateResult(2)">
                <el-icon><RefreshRight /></el-icon> 加试
              </button>
              <button class="btn btn-outline" @click="showFeedbackDialog = true">
                <el-icon><Edit /></el-icon> 面试反馈表
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ===== 面试反馈表弹窗 ===== -->
    <el-dialog
      v-model="showFeedbackDialog"
      title="面试反馈表"
      width="600px"
      :close-on-click-modal="false"
    >
      <!-- 候选人选择 -->
      <div class="form-group">
        <label class="form-label">候选人</label>
        <el-select
          v-model="selectedFeedbackCandidate"
          placeholder="选择候选人..."
          style="width: 100%"
        >
          <el-option
            v-for="c in candidateOptions"
            :key="c.id"
            :label="c.name + ' - ' + c.jobTitle"
            :value="c.id"
          />
        </el-select>
      </div>

      <!-- 五维评分 -->
      <div class="form-group" v-for="dim in ratingDimensions" :key="dim.key">
        <label class="form-label">{{ dim.label }}</label>
        <div class="star-rating" @click="(e: Event) => handleStarClick(dim.key, e)">
          <span
            v-for="n in 5"
            :key="n"
            class="star"
            :class="{ filled: (feedbackForm[dim.key] || 0) >= n }"
            :data-value="n"
          >
            <el-icon :size="22"><StarFilled v-if="(feedbackForm[dim.key] || 0) >= n" /><Star v-else /></el-icon>
          </span>
        </div>
      </div>

      <!-- 面试评语 -->
      <div class="form-group">
        <label class="form-label">面试评语</label>
        <el-input
          v-model="feedbackForm.comments"
          type="textarea"
          :rows="4"
          placeholder="请填写对候选人的综合评价..."
        />
      </div>

      <!-- 综合评价 -->
      <div class="form-group">
        <label class="form-label">综合评价</label>
        <div class="radio-group">
          <label class="radio-item" v-for="opt in hireOptions" :key="opt.value">
            <input type="radio" name="overallDecision" :value="opt.value" v-model="feedbackForm.hireRecommendation" />
            {{ opt.label }}
          </label>
        </div>
      </div>

      <template #footer>
        <el-button @click="showFeedbackDialog = false">取消</el-button>
        <el-button type="primary" :loading="feedbackSubmitting" @click="handleSubmitFeedback">
          <el-icon><Promotion /></el-icon> 提交反馈
        </el-button>
      </template>
    </el-dialog>

    <!-- ===== 加入面试题库弹窗 ===== -->
    <el-dialog
      v-model="bankDialogVisible"
      title="加入面试题库"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form :model="bankForm" label-width="96px">
        <el-form-item label="部门" required>
          <el-tree-select
            v-model="bankForm.departmentId"
            :data="bankDeptOptions"
            :props="{ label: 'name', value: 'id', children: 'children' }"
            placeholder="请选择部门"
            check-strictly
            clearable
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="职位" required>
          <el-input v-model="bankForm.jobTitle" placeholder="职位名称（默认取自AI出题职位）" />
        </el-form-item>
        <el-form-item label="套题名称" required>
          <el-input v-model="bankForm.bankName" placeholder="如：高级Java开发-AI生成题" />
        </el-form-item>
        <el-form-item label="套题类型">
          <el-select v-model="bankForm.questionType" style="width: 100%">
            <el-option label="技术面" :value="0" />
            <el-option label="项目面" :value="1" />
            <el-option label="行为/HR面" :value="2" />
            <el-option label="综合面" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="难度">
          <el-select v-model="bankForm.difficulty" style="width: 100%">
            <el-option label="简单" :value="1" />
            <el-option label="中等" :value="2" />
            <el-option label="困难" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="bankForm.description" type="textarea" :rows="2" placeholder="可选：套题说明" />
        </el-form-item>
        <div class="bank-preview-tip">将保存本次 AI 生成的 {{ bankQuestionCount }} 道题目到面试题库</div>
      </el-form>
      <template #footer>
        <el-button @click="bankDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingBank" @click="submitBank">确认加入</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount, computed, markRaw, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Chart, registerables } from 'chart.js'
import {
  MagicStick, Top, Calendar, Monitor, FolderOpened, UserFilled,
  DataAnalysis, Clock, CircleCheck, CircleClose, RefreshRight,
  DocumentCopy, Document, Edit, Plus, Promotion, List, CircleCheckFilled,
  StarFilled, Star, VideoCamera, TrendCharts, Loading,
  Warning, Medal, Trophy, Flag, Search, Remove,
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

import { getJobs } from '@/api/job'
import { getDepartments } from '@/api/system'
import { getCandidateById } from '@/api/candidate'
import {
  getQuestionBanks, getQuestionBank, createQuestionBank,
  type QuestionBankVO, type QuestionBankItemVO,
} from '@/api/questionBank'
import {
  getAiStats, getSchedule, generateQuestions, getGenerateTaskResult, sendQuestionsEmail,
  getInterviewAssessment, submitFeedback, updateResult,
  getOnlineAssessments, createOnlineAssessment, sendOnlineAssessment, updateOnlineAssessmentScore,
  getAssessedCandidates, getWeekSchedule, generateAssessmentQuestions,
  getQuestionGenerateResults, getAssessmentQuestions,
  saveAssessmentQuestions
} from '@/api/interview'

import type {
  AiStatsVO, InterviewScheduleVO, QuestionGenerateResult, QuestionGenerateTaskVO,
  AssessmentResponse, OnlineAssessmentVO, AssessedCandidateVO, DepartmentTreeVO
} from '@/types/models'

// Chart.js 注册
Chart.register(...registerables)

// ================================================================
// 当前日期
// ================================================================
const formattedDate = computed(() => {
  const now = new Date()
  const weekdays = ['日', '一', '二', '三', '四', '五', '六']
  return now.getFullYear() + '年' + (now.getMonth() + 1) + '月' +
    now.getDate() + '日 星期' + weekdays[now.getDay()]
})

// ================================================================
// 统计卡片
// ================================================================
const statsData = ref<AiStatsVO>({ todayInterviews: 0, completed: 0, aiAssessed: 0, passRate: 0 })
const statsCards = ref<{ key: string; label: string; value: string; color: string; bgColor: string; icon: any }[]>([])

function updateStatsCards() {
  const d = statsData.value
  statsCards.value = [
    { key: 'today', label: '今日面试', value: String(d.todayInterviews), color: '#4f46e5', bgColor: '#eef2ff', icon: markRaw(Calendar) },
    { key: 'completed', label: '已完成', value: String(d.completed), color: '#059669', bgColor: '#ecfdf5', icon: markRaw(CircleCheck) },
    { key: 'ai', label: 'AI 评估完成', value: String(d.aiAssessed), color: '#0ea5e9', bgColor: '#f0f9ff', icon: markRaw(MagicStick) },
    { key: 'passRate', label: '通过率', value: d.passRate + '%', color: '#d97706', bgColor: '#fffbeb', icon: markRaw(TrendCharts) },
  ]
}

async function loadStats() {
  try {
    statsData.value = await getAiStats()
  } catch {
    statsData.value = { todayInterviews: 0, completed: 0, aiAssessed: 0, passRate: 0 }
  }
  updateStatsCards()
}

// ================================================================
// 面试日程
// ================================================================
const scheduleLoading = ref(true)
const scheduleTab = ref('day')
const scheduleList = ref<InterviewScheduleVO[]>([])
const selectedScheduleId = ref<string | null>(null)
const weekScheduleList = ref<InterviewScheduleVO[]>([])

interface WeekDay {
  date: string
  dateStr: string
  label: string
  count: number
  items: InterviewScheduleVO[]
}

const weekDays = computed<WeekDay[]>(() => {
  const weekRange = getWeekRange()
  const weekDayNames = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  const map = new Map<string, InterviewScheduleVO[]>()
  for (const item of weekScheduleList.value) {
    const d = item.scheduledDate || ''
    if (!map.has(d)) map.set(d, [])
    map.get(d)!.push(item)
  }
  return weekRange.dates.map((date, i) => {
    const dt = new Date(date + 'T00:00:00')
    const items = map.get(date) || []
    return {
      date,
      dateStr: (dt.getMonth() + 1) + '月' + dt.getDate() + '日',
      label: weekDayNames[dt.getDay()],
      count: items.length,
      items,
    }
  })
})

function getWeekRange(): { dates: string[]; startDate: string; endDate: string } {
  const now = new Date()
  const day = now.getDay()
  const diff = day === 0 ? 6 : day - 1 // 周一起始
  const monday = new Date(now)
  monday.setDate(now.getDate() - diff)
  const dates: string[] = []
  for (let i = 0; i < 7; i++) {
    const d = new Date(monday)
    d.setDate(monday.getDate() + i)
    dates.push(d.toISOString().split('T')[0])
  }
  return { dates, startDate: dates[0], endDate: dates[6] }
}

async function loadSchedule() {
  scheduleLoading.value = true
  try {
    const today = new Date().toISOString().split('T')[0]
    scheduleList.value = await getSchedule(today)
  } catch {
    scheduleList.value = []
  }
  scheduleLoading.value = false
}

async function loadWeekSchedule() {
  scheduleLoading.value = true
  try {
    const { startDate, endDate } = getWeekRange()
    weekScheduleList.value = await getWeekSchedule(startDate, endDate)
  } catch {
    weekScheduleList.value = []
  }
  scheduleLoading.value = false
}

function handleScheduleTabSwitch(tab: string) {
  scheduleTab.value = tab
  selectedScheduleId.value = null
  if (tab === 'day') {
    loadSchedule()
  } else {
    loadWeekSchedule()
  }
}

function statusTagLabel(statusTag: string) {
  const map: Record<string, string> = {
    pending: '待开始',
    ongoing: '进行中',
    done: '已完成',
    cancelled: '已取消',
  }
  return map[statusTag] || statusTag
}

// ================================================================
// AI 智能出题
// ================================================================
const questionLoading = ref(false)
const questionPosition = ref<string | null>(null)
const questionDeptId = ref<string | null>(null)
const questionDeptOptions = ref<DepartmentTreeVO[]>([])
const questionResult = ref<QuestionGenerateResult | null>(null)
const selectedJobTitle = ref('')
const router = useRouter()
const positionOptions = ref<{ label: string; value: string; title: string }[]>([])

// 出题控制参数
const questionDifficulty = ref('mixed')
const questionCategories = ref<string[]>(['tech', 'project', 'behavioral'])
const questionCounts = reactive<Record<string, number>>({
  tech: 3,
  project: 3,
  behavioral: 3,
})
const difficultyOptions = [
  { label: '混合', value: 'mixed' },
  { label: '简单', value: 'easy' },
  { label: '中等', value: 'medium' },
  { label: '困难', value: 'hard' },
]
const categoryOptions = [
  { label: '技术基础', value: 'tech' },
  { label: '项目经验', value: 'project' },
  { label: '行为面试', value: 'behavioral' },
]
const techTypeOptions = [
  { label: '单选题', value: 'single_choice' },
  { label: '多选题', value: 'multiple_choice' },
  { label: '判断题', value: 'true_false' },
  { label: '问答题', value: 'essay' },
]
const techTypeCounts = reactive<Record<string, number>>({
  single_choice: 3,
  multiple_choice: 0,
  true_false: 0,
  essay: 0,
})
const techTypeLabels: Record<string, string> = {
  single_choice: '单选',
  multiple_choice: '多选',
  true_false: '判断',
  essay: '问答',
}
// 题型总数 — 不能超过"技术基础题"总数量
const techTypeTotal = computed(() => {
  return Object.values(techTypeCounts).reduce((sum, n) => sum + (n || 0), 0)
})
// 监听技术基础题总数变化，自动分发到题型
watch(
  () => questionCounts.tech,
  (newVal) => {
    const total = techTypeTotal.value
    if (total < newVal) {
      // 增加时补充到 single_choice
      techTypeCounts.single_choice += (newVal - total)
    } else if (total > newVal) {
      // 减少时从 essay → true_false → multiple_choice → single_choice 依次扣减
      let excess = total - newVal
      const order = ['essay', 'true_false', 'multiple_choice', 'single_choice'] as const
      for (const key of order) {
        if (excess <= 0) break
        const remove = Math.min(techTypeCounts[key], excess)
        techTypeCounts[key] -= remove
        excess -= remove
      }
    }
  }
)
const totalQuestionCount = computed(() => {
  return questionCategories.value.reduce((sum, cat) => sum + (questionCounts[cat] || 3), 0)
})

/**
 * 根据职位名称推导职位方向类型。
 * 0=前端, 1=后端, 2=AI/算法, 3=产品, 4=运维, 5=数据, 8=测试
 */
function inferPositionType(jobTitle: string): number | null {
  const title = jobTitle.toLowerCase()
  if (/前端|web|react|vue|angular|h5|ui|flutter|小程序|ios|android|移动端/i.test(title)) return 0
  if (/后端|java|golang|go|python|php|node|rust|c#|服务端|中间件/i.test(title)) return 1
  if (/ai|算法|机器学|深度学|nlp|自然语言|cv|计算机视觉|大模型|llm|人工智能|推荐/i.test(title)) return 2
  if (/产品|经理|pm/i.test(title)) return 3
  if (/运维|devops|sre|k8s|kubernetes|docker|ci|cd|云平台|基础设施|安全/i.test(title)) return 4
  if (/数据|etl|大数据|数仓|spark|hadoop|flink|分析/i.test(title)) return 5
  if (/测试|qa|质量|测开|自动化测试|接口测试|性能测试|软件测试/i.test(title)) return 8
  return null
}

/** 加载部门树（AI 出题两级联动第一步）。 */
async function loadQuestionDepts() {
  try {
    questionDeptOptions.value = await getDepartments()
  } catch {
    questionDeptOptions.value = []
  }
}

/** 切换部门：清空职位选择并加载该部门已发布职位。 */
async function handleQuestionDeptChange(deptId: string) {
  questionPosition.value = null
  positionOptions.value = []
  if (!deptId) return
  await loadPositionOptions(deptId)
}

/** 按部门加载已发布职位。 */
async function loadPositionOptions(departmentId?: string) {
  try {
    const params: Record<string, unknown> = { page: 1, size: 200, status: 1 }
    if (departmentId) params.departmentId = departmentId
    const res = await getJobs(params)
    const publishedJobs = res.records || []
    positionOptions.value = publishedJobs.map(job => ({
      label: job.title,
      value: job.id,
      title: job.title,
    }))
  } catch {
    positionOptions.value = []
  }
}

let pollTimer: ReturnType<typeof setInterval> | null = null

async function handleGenerateQuestions() {
  if (!questionPosition.value) {
    ElMessage.warning('请先选择职位')
    return
  }
  const selected = positionOptions.value.find(p => p.value === questionPosition.value)
  if (!selected) {
    ElMessage.warning('未找到所选职位信息')
    return
  }
  const posType = inferPositionType(selected.title)
  if (posType === null) {
    ElMessage.warning('无法识别所选职位的方向类型，请选择其他职位')
    return
  }

  // 清除之前的轮询和结果
  if (pollTimer) { clearInterval(pollTimer); pollTimer = null }
  questionResult.value = null
  questionLoading.value = true
  selectedJobTitle.value = selected.title

  try {
    // 构建请求体
    const reqBody: {
      positionType: number
      difficultyLevel?: string
      categories?: string[]
      categoryQuestionCounts?: Record<string, number>
      techQuestionTypes?: Record<string, number>
    } = {
      positionType: posType,
      difficultyLevel: questionDifficulty.value,
      categories: questionCategories.value.length ? questionCategories.value : undefined,
      categoryQuestionCounts: questionCategories.value.reduce((acc, cat) => {
        acc[cat] = questionCounts[cat] || 3
        return acc
      }, {} as Record<string, number>),
    }
    // 如果选中了技术基础题且题型合计 > 0，传递题型分配
    if (questionCategories.value.includes('tech') && techTypeTotal.value > 0) {
      reqBody.techQuestionTypes = {}
      techTypeOptions.forEach(tt => {
        if (techTypeCounts[tt.value] > 0) {
          reqBody.techQuestionTypes![tt.value] = techTypeCounts[tt.value]
        }
      })
    }

    // 1. 创建异步任务
    const { taskId } = await generateQuestions(reqBody)

    // 2. 轮询查询任务状态（每 2 秒一次）
    pollTimer = setInterval(async () => {
      try {
        const task: QuestionGenerateTaskVO = await getGenerateTaskResult(taskId)
        if (task.status === 'COMPLETED') {
          clearInterval(pollTimer!)
          pollTimer = null
          questionResult.value = task.result ?? null
          questionLoading.value = false
          ElMessage.success('题目生成完成！')
        } else if (task.status === 'FAILED') {
          clearInterval(pollTimer!)
          pollTimer = null
          questionLoading.value = false
          ElMessage.error(task.errorMessage || '题目生成失败，请稍后重试')
        } else if (task.status === 'NOT_FOUND') {
          clearInterval(pollTimer!)
          pollTimer = null
          questionLoading.value = false
          ElMessage.error('出题任务已过期，请重新生成')
        }
        // PENDING / PROCESSING — 继续轮询
      } catch {
        clearInterval(pollTimer!)
        pollTimer = null
        questionLoading.value = false
        ElMessage.error('查询出题状态失败，请稍后重试')
      }
    }, 2000)
  } catch {
    questionLoading.value = false
    ElMessage.error('创建出题任务失败，请稍后重试')
  }
}

function diffClass(code: number) {
  return code === 0 ? 'easy' : code === 1 ? 'medium' : 'hard'
}

function copyQuestions() {
  if (!questionResult.value) return
  const parts: string[] = ['AI 智能面试题目', '']
  const groups = [
    { label: '【技术基础题】', items: questionResult.value.techQuestions },
    { label: '【项目经验题】', items: questionResult.value.projectQuestions },
    { label: '【行为面试题】', items: questionResult.value.behavioralQuestions },
  ]
  groups.forEach(g => {
    if (g.items?.length) {
      parts.push(g.label)
      g.items.forEach(q => parts.push(`${q.number}. ${q.question} [${q.difficulty}]`))
      parts.push('')
    }
  })
  navigator.clipboard.writeText(parts.join('\n')).then(() => {
    ElMessage.success('全部题目已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败，请手动选择复制')
  })
}

// ================================================================
// 发送面试题邮件
// ================================================================
const showEmailDialog = ref(false)
const emailSending = ref(false)
const emailForm = reactive({
  email: '',
  interviewerName: '',
})

function handleSendEmail() {
  if (!emailForm.email.trim()) {
    ElMessage.warning('请输入面试官邮箱地址')
    return
  }
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailRegex.test(emailForm.email.trim())) {
    ElMessage.warning('请输入正确的邮箱格式')
    return
  }
  if (!questionResult.value) {
    ElMessage.warning('请先生成面试题')
    return
  }
  emailSending.value = true
  sendQuestionsEmail({
    email: emailForm.email.trim(),
    interviewerName: emailForm.interviewerName.trim() || undefined,
    positionLabel: selectedJobTitle.value,
    techQuestions: questionResult.value.techQuestions,
    projectQuestions: questionResult.value.projectQuestions,
    behavioralQuestions: questionResult.value.behavioralQuestions,
  }).then(() => {
    ElMessage.success('面试题邮件已发送！')
    showEmailDialog.value = false
    emailForm.email = ''
    emailForm.interviewerName = ''
  }).catch(() => {
    ElMessage.error('邮件发送失败，请稍后重试')
  }).finally(() => {
    emailSending.value = false
  })
}

// ================================================================
// AI 评估报告
// ================================================================
const assessmentLoading = ref(false)
const selectedCandidateId = ref<string | null>(null)
const assessmentData = ref<AssessmentResponse | null>(null)
const candidateOptions = ref<{ id: string; name: string; jobTitle: string; departmentId: number | null }[]>([])
const deptOptions = ref<DepartmentTreeVO[]>([])
const selectedDeptId = ref<string | null>(null)
let radarChart: Chart | null = null
const radarChartRef = ref<HTMLCanvasElement>()

/** 按所选部门过滤后的候选人列表（AI 评估报告专用）。 */
const filteredCandidateOptions = computed(() => {
  if (!selectedDeptId.value) return []
  return candidateOptions.value.filter(c => String(c.departmentId ?? '') === selectedDeptId.value)
})

async function loadDepartments() {
  try {
    deptOptions.value = await getDepartments()
  } catch {
    deptOptions.value = []
  }
}

/** 切换部门：清空候选人选择与已加载的评估报告。 */
function handleDeptChange() {
  selectedCandidateId.value = null
  assessmentData.value = null
  if (radarChart) {
    radarChart.destroy()
    radarChart = null
  }
}

async function loadCandidateOptions() {
  try {
    const list = await getAssessedCandidates()
    candidateOptions.value = list.map(c => ({
      id: String(c.interviewId),
      name: c.candidateName,
      jobTitle: c.jobTitle,
      departmentId: c.departmentId,
    }))
  } catch {
    candidateOptions.value = []
  }
}

async function handleLoadAssessment() {
  if (!selectedCandidateId.value) {
    assessmentData.value = null
    return
  }
  assessmentLoading.value = true
  assessmentData.value = null
  try {
    assessmentData.value = await getInterviewAssessment(selectedCandidateId.value)
  } catch {
    assessmentData.value = null
    ElMessage.warning('暂无评估数据')
  }
  assessmentLoading.value = false
  // 延迟确保 canvas 渲染后再画图
  setTimeout(() => renderRadarChart(), 100)
}

function renderRadarChart() {
  if (!radarChartRef.value || !assessmentData.value) return
  if (radarChart) radarChart.destroy()
  const d = assessmentData.value
  // Scale 1.0-5.0 → 0-100 for the chart
  const scoreTo100 = (v: any) => (Number(v) || 0) * 20
  radarChart = new Chart(radarChartRef.value, {
    type: 'radar',
    data: {
      labels: ['技术深度', '沟通表达', '问题解决', '学习能力', '团队协作'],
      datasets: [{
        label: '评分',
        data: [
          scoreTo100(d.technologyDepth),
          scoreTo100(d.communication),
          scoreTo100(d.problemSolving),
          scoreTo100(d.learningAbility),
          scoreTo100(d.teamwork),
        ],
        backgroundColor: 'rgba(79,70,229,0.15)',
        borderColor: '#4f46e5',
        borderWidth: 2,
        pointBackgroundColor: '#4f46e5',
        pointBorderColor: '#fff',
        pointBorderWidth: 2,
        pointRadius: 4,
        pointHoverRadius: 6,
      }],
    },
    options: {
      responsive: true,
      maintainAspectRatio: true,
      scales: {
        r: {
          beginAtZero: true,
          max: 100,
          min: 0,
          ticks: { stepSize: 20, display: false },
          pointLabels: { font: { size: 12 }, color: '#475569' },
          grid: { color: 'rgba(148,163,184,0.2)' },
          angleLines: { color: 'rgba(148,163,184,0.2)' },
        },
      },
      plugins: { legend: { display: false } },
    },
  })
}

function suggestedActionClass(suggestion: number) {
  return suggestion <= 1 ? 'next-round' : suggestion === 3 ? 'reject' : 'retest'
}

async function handleUpdateResult(result: number) {
  if (!selectedCandidateId.value) {
    ElMessage.warning('请先选择候选人')
    return
  }
  try {
    await updateResult(selectedCandidateId.value, result)
    const messages: Record<number, string> = { 0: '通过', 1: '淘汰', 2: '加试' }
    ElMessage.success(`已标记为"${messages[result] || '操作成功'}"`)
  } catch {
    ElMessage.error('更新失败')
  }
}

// ================================================================
// 在线测评管理
// ================================================================
const assessmentListLoading = ref(true)
const onlineAssessmentList = ref<OnlineAssessmentVO[]>([])
const assessmentTotal = ref(0)
const assessmentPage = reactive({ page: 1, size: 10 })
const assessmentFilter = reactive({
  candidateName: '' as string,
  candidateEmail: '' as string,
  status: null as number | null,
  type: null as number | null,
})

const hasAssessmentFilter = computed(() =>
  !!assessmentFilter.candidateName || !!assessmentFilter.candidateEmail
    || assessmentFilter.status !== null || assessmentFilter.type !== null
)

function resetAssessmentFilter() {
  assessmentFilter.candidateName = ''
  assessmentFilter.candidateEmail = ''
  assessmentFilter.status = null
  assessmentFilter.type = null
  assessmentPage.page = 1
  loadOnlineAssessments()
}

async function loadOnlineAssessments() {
  assessmentListLoading.value = true
  try {
    const res = await getOnlineAssessments({
      page: assessmentPage.page,
      size: assessmentPage.size,
      candidateName: assessmentFilter.candidateName || undefined,
      candidateEmail: assessmentFilter.candidateEmail || undefined,
      status: assessmentFilter.status !== null ? assessmentFilter.status : undefined,
      type: assessmentFilter.type !== null ? assessmentFilter.type : undefined,
    })
    onlineAssessmentList.value = res.records || []
    assessmentTotal.value = res.total
  } catch {
    onlineAssessmentList.value = []
  }
  assessmentListLoading.value = false
}

async function handleSendAssessment(id: string) {
  sendingAssessmentId.value = id
  try {
    await sendOnlineAssessment(id)
    ElMessage.success('测评已发送')
    loadOnlineAssessments()
  } catch {
    ElMessage.error('发送失败')
  } finally {
    sendingAssessmentId.value = null
  }
}

async function handleGenerateAssessmentQuestions(row: OnlineAssessmentVO) {
  const id = String(row.id)
  generatingQuestionsId.value = id
  try {
    await generateAssessmentQuestions(id)
    ElMessage.success('AI 题目已生成，可发送测评')
    loadOnlineAssessments()
  } catch {
    ElMessage.error('AI 题目生成失败，请稍后重试')
  } finally {
    generatingQuestionsId.value = null
  }
}

function typeClass(type: number) {
  return type === 0 ? 'coding' : type === 1 ? 'personality' : 'iq'
}

function typeIcon(type: number) {
  return type === 0 ? 'Monitor' : type === 1 ? 'UserFilled' : 'MagicStick'
}

function handleViewAssessment(row: OnlineAssessmentVO) {
  viewAssessmentData.value = row
  showViewAssessmentDialog.value = true
}

async function handleViewQuestions(row: OnlineAssessmentVO) {
  showQuestionsDialog.value = true
  questionsActiveTab.value = '全部'
  questionsLoading.value = true
  viewQuestions.value = []
  try {
    viewQuestions.value = await getAssessmentQuestions(String(row.id))
  } catch {
    ElMessage.error('加载题目失败')
  } finally {
    questionsLoading.value = false
  }
}

// ---- 测评报告分析计算 ----

interface ScoreParseResult { correct: number; total: number }
function parseScore(score: string | undefined): ScoreParseResult {
  if (!score) return { correct: 0, total: 1 }
  const m = score.match(/^(\d+)\s*\/\s*(\d+)$/)
  if (m) return { correct: parseInt(m[1]), total: parseInt(m[2]) }
  const n = parseInt(score)
  if (!isNaN(n)) return { correct: n, total: 100 }
  return { correct: 0, total: 1 }
}

const scorePercent = computed(() => {
  const s = parseScore(viewAssessmentData.value?.score)
  return Math.round((s.correct / s.total) * 100)
})

const scoreColor = computed(() => {
  if (scorePercent.value >= 80) return '#059669'
  if (scorePercent.value >= 60) return '#2563eb'
  return '#d97706'
})

const levelLabel = computed(() => {
  if (scorePercent.value >= 80) return '优秀'
  if (scorePercent.value >= 60) return '良好'
  return '需提升'
})

const levelClass = computed(() => {
  if (scorePercent.value >= 80) return 'level-excellent'
  if (scorePercent.value >= 60) return 'level-good'
  return 'level-improve'
})

const reportDescription = computed(() => {
  const d = viewAssessmentData.value
  if (!d) return ''
  if (d.type === 1) {
    return personalityDescriptions[d.score || ''] || '感谢完成本次性格测评。'
  }
  const pct = scorePercent.value
  const s = parseScore(d.score)
  if (pct >= 80) {
    return `正确率 ${pct}%，${s.correct}/${s.total} 题正确。表现优秀，展现出了扎实的专业知识和出色的解题能力，远超大多数候选人。`
  } else if (pct >= 60) {
    return `正确率 ${pct}%，${s.correct}/${s.total} 题正确。表现良好，具备较好的基础能力，部分知识点尚有提升空间。`
  }
  return `正确率 ${pct}%，${s.correct}/${s.total} 题正确。建议加强相关领域知识的学习和实践，重点补足薄弱环节。`
})

const reportStrengths = computed(() => {
  const d = viewAssessmentData.value
  if (!d || d.type === 1) return []
  const pct = scorePercent.value
  if (d.type === 0) {
    if (pct >= 80) return ['Java/编程基础扎实', '面向对象思想理解到位', 'SQL 与数据库知识掌握良好']
    if (pct >= 60) return ['基本编程概念掌握', '常见数据结构有一定理解']
    return ['已具备入门级编程基础']
  }
  // IQ 测试
  if (pct >= 80) return ['逻辑推理能力优秀', '数列规律识别准确', '图形分析能力突出']
  if (pct >= 60) return ['基本逻辑判断正确', '数字规律识别能力中等']
  return ['具备基础逻辑思维能力']
})

const reportWeaknesses = computed(() => {
  const d = viewAssessmentData.value
  if (!d || d.type === 1) return []
  const pct = scorePercent.value
  if (d.type === 0) {
    if (pct >= 80) return ['高级算法优化可进一步加强', '系统架构设计经验可积累']
    if (pct >= 60) return ['数据结构与算法需加强', '异常处理与边界情况考虑不足', 'SOLID 设计原则理解有待深入']
    return ['编程基础需系统性加强', 'SQL 查询与数据库操作需学习', '数据结构和常用算法需专项训练', '面向对象核心概念需重新梳理']
  }
  if (pct >= 80) return ['极复杂场景推理可继续挑战']
  if (pct >= 60) return ['图形空间推理需加强', '复杂数列推导需提升']
  return ['逻辑推理需系统性训练', '数字敏感度需提高', '空间想象力需加强']
})

// 性格测试信息
const personalityDescriptions: Record<string, string> = {
  '外向型': '您善于社交沟通，性格开朗外向，适合需要频繁协作的岗位，在团队中能发挥桥梁和润滑剂的作用。',
  '尽责型': '您做事认真负责、有条理，注重细节，适合需要高度自律和细节把控的工作，是团队中可靠的中坚力量。',
  '开放型': '您思维活跃、乐于创新，对新事物充满好奇，适合需要创造力和灵活性的岗位。',
  '平衡型': '您性格均衡，在不同场景下都能展现出良好的适应能力，是团队中多面手型的人才。',
}

const personalityTypeIcon = computed(() => {
  const t = viewAssessmentData.value?.score || ''
  return { '外向型': '&#x1F91D;', '尽责型': '&#x1F4CB;', '开放型': '&#x1F680;', '平衡型': '&#x2696;' }[t] || '&#x1F3AF;'
})

const personalityTraits = computed(() => {
  const t = viewAssessmentData.value?.score || ''
  const map: Record<string, string[]> = {
    '外向型': ['善于沟通表达', '团队协作意识强', '乐于助人、亲和力高', '主动性强、反应迅速'],
    '尽责型': ['做事严谨认真', '注重细节和计划', '责任心强、值得信赖', '自我驱动力高'],
    '开放型': ['思维灵活有创意', '乐于接受新事物', '学习能力强', '适应变化的能力突出'],
    '平衡型': ['性格均衡稳定', '多场景适应力好', '情商较高', '综合素质突出'],
  }
  return map[t] || ['综合表现良好']
})

const personalitySuitable = computed(() => {
  const t = viewAssessmentData.value?.score || ''
  const map: Record<string, string[]> = {
    '外向型': ['团队主管/项目经理', '客户经理/销售', '人力资源', '市场推广'],
    '尽责型': ['质量保障/测试', '财务/审计', '研究员/分析师', '行政/运营管理'],
    '开放型': ['产品经理/设计师', '研发/架构师', '创新业务负责人', '战略规划'],
    '平衡型': ['综合管理岗位', '创业合伙人', '技术管理', '咨询顾问'],
  }
  return map[t] || ['多岗位均可胜任']
})

const personalityAdvice = computed(() => {
  const t = viewAssessmentData.value?.score || ''
  const map: Record<string, string[]> = {
    '外向型': ['适当培养独处深度思考的习惯', '重要决策时先沉淀再行动'],
    '尽责型': ['尝试接受适度的不确定性', '加强跨部门沟通的频率'],
    '开放型': ['需要时做好计划和时间管理', '关注执行落地的细节'],
    '平衡型': ['选择一个方向深耕形成专长', '主动争取挑战性项目锻炼自己'],
  }
  return map[t] || ['持续学习，全面发展']
})

// 新建测评
const showCreateAssessmentDialog = ref(false)
const creatingAssessment = ref(false)
const sendingAssessmentId = ref<string | null>(null)
const generatingQuestionsId = ref<string | null>(null)
const autoGenerateQuestionsId = ref<string | null>(null)
const generateResultOptions = ref<{ taskId: string; positionLabel: string; positionType: number }[]>([])
const selectedGenerateResult = ref<string | null>(null)
const createAssessmentForm = reactive({
  candidateId: '',
  candidateName: '',
  candidateEmail: '',
  jobTitle: '',
  type: 0 as number,
})

/** 在线测评题目来源：none=默认系统题库, ai=AI出题结果, bank=面试题库。 */
const assessmentSource = ref<'none' | 'ai' | 'bank'>('none')
const bankOptions = ref<QuestionBankVO[]>([])
const selectedBankId = ref<string>('')

/** 加入面试题库弹窗。 */
const bankDialogVisible = ref(false)
const savingBank = ref(false)
const bankDeptOptions = ref<DepartmentTreeVO[]>([])
const bankForm = reactive({
  departmentId: '',
  jobTitle: '',
  bankName: '',
  questionType: 3,
  difficulty: 2,
  description: '',
})

/** 本次 AI 出题结果可入库的题目总数。 */
const bankQuestionCount = computed(() => {
  if (!questionResult.value) return 0
  return (questionResult.value.techQuestions?.length || 0)
    + (questionResult.value.projectQuestions?.length || 0)
    + (questionResult.value.behavioralQuestions?.length || 0)
})

const lookingUpCandidate = ref(false)

async function handleCandidateIdLookup() {
  const id = createAssessmentForm.candidateId.trim()
  if (!id) return
  lookingUpCandidate.value = true
  try {
    const candidate = await getCandidateById(id)
    createAssessmentForm.candidateName = candidate.name || ''
    createAssessmentForm.candidateEmail = candidate.email || ''
    createAssessmentForm.jobTitle = candidate.jobTitle || ''
  } catch {
    // 查不到候选人，不清空已填写内容
  } finally {
    lookingUpCandidate.value = false
  }
}

async function loadGenerateResults() {
  const results: { taskId: string; positionLabel: string; positionType: number }[] = []
  // 优先展示当前页面已生成的 AI 出题结果（即使后端任务已过期也能用）
  if (questionResult.value && selectedJobTitle.value) {
    results.push({
      taskId: '__current__',
      positionLabel: selectedJobTitle.value,
      positionType: questionResult.value.positionType ?? 0,
    })
  }
  try {
    const remote = await getQuestionGenerateResults()
    // 合并远程结果，去重（排除已添加的当前结果）
    for (const r of remote) {
      if (!results.some(x => x.positionLabel === r.positionLabel)) {
        results.push(r)
      }
    }
  } catch {
    // ignore
  }
  generateResultOptions.value = results
}

function onSelectGenerateResult(taskId: string | null) {
  if (!taskId) {
    // 清除选择，恢复空白
    if (autoGenerateQuestionsId.value === 'pending') {
      createAssessmentForm.jobTitle = selectedJobTitle.value
      createAssessmentForm.type = 0
    }
    return
  }
  const selected = generateResultOptions.value.find(r => r.taskId === taskId)
  if (selected) {
    createAssessmentForm.jobTitle = selected.positionLabel
    createAssessmentForm.type = 0
  }
}

/** 打开「加入面试题库」弹窗（预填职位与套题名）。 */
async function openBankDialog() {
  // 部门和职位取「生成题目之前用户选择的部门与职位」
  const jobTitle = selectedJobTitle.value || questionResult.value?.positionLabel || ''
  Object.assign(bankForm, {
    departmentId: questionDeptId.value || '',
    jobTitle,
    bankName: jobTitle ? `${jobTitle}-AI生成题` : 'AI生成面试题',
    questionType: 3,
    difficulty: 2,
    description: '由 AI 智能出题生成，加入面试题库沉淀复用。',
  })
  try {
    bankDeptOptions.value = await getDepartments()
  } catch {
    bankDeptOptions.value = []
  }
  bankDialogVisible.value = true
}

/** 将 AI 出题结果转换为面试题库题目格式。 */
function convertToBankQuestions(result: typeof questionResult.value): QuestionBankItemVO[] {
  if (!result) return []
  const items: QuestionBankItemVO[] = []
  const categories = [
    { label: '技术基础题', list: result.techQuestions },
    { label: '项目经验题', list: result.projectQuestions },
    { label: '行为面试题', list: result.behavioralQuestions },
  ]
  for (const cat of categories) {
    if (!cat.list) continue
    for (const item of cat.list) {
      const qt = item.questionType || 'essay'
      items.push({
        questionType: bankQuestionTypeCode(qt),
        question: `${cat.label}：${item.question}`,
        options: item.options?.length ? [...item.options] : [],
        answer: extractAnswerKey(qt, item.referenceAnswer || '', parseOptions(item.options || [])),
        explanation: item.referenceAnswer || '',
        difficulty: item.difficultyCode || 2,
      })
    }
  }
  return items
}

/** AI 题型字符串 → 题库题型编码：0=单选,1=多选,2=问答,3=判断。 */
function bankQuestionTypeCode(qt: string): number {
  if (qt === 'single_choice') return 0
  if (qt === 'multiple_choice') return 1
  if (qt === 'true_false') return 3
  return 2
}

/** 解析 "A. xxx" 选项为 {key, value}，复用答案提取逻辑。 */
function parseOptions(options: string[]): { key: string; value: string }[] {
  return options.map(opt => {
    const m = opt.match(/^([A-Ea-e])[\.\)、]\s*(.+)/)
    if (m) return { key: m[1].toUpperCase(), value: m[2] }
    return { key: '', value: opt }
  })
}

/** 确认将 AI 题目保存到面试题库。 */
async function submitBank() {
  if (!bankForm.departmentId) {
    ElMessage.warning('请选择部门')
    return
  }
  if (!bankForm.bankName?.trim() || !bankForm.jobTitle?.trim()) {
    ElMessage.warning('请填写套题名称与职位')
    return
  }
  savingBank.value = true
  try {
    await createQuestionBank({
      bankName: bankForm.bankName.trim(),
      departmentId: bankForm.departmentId,
      departmentName: findDeptName(bankForm.departmentId, bankDeptOptions.value),
      jobTitle: bankForm.jobTitle.trim(),
      questionType: bankForm.questionType,
      difficulty: bankForm.difficulty,
      description: bankForm.description,
      status: 1,
      items: convertToBankQuestions(questionResult.value),
    })
    ElMessage.success(`已加入面试题库，共 ${bankQuestionCount.value} 题`)
    bankDialogVisible.value = false
  } catch {
    ElMessage.error('保存失败，请稍后重试')
  } finally {
    savingBank.value = false
  }
}

function findDeptName(id: string, nodes: DepartmentTreeVO[]): string {
  if (!id) return ''
  for (const n of nodes) {
    if (String(n.id) === String(id)) return n.name
    if (n.children?.length) {
      const found = findDeptName(id, n.children)
      if (found) return found
    }
  }
  return ''
}

/** 题目来源切换：选择面试题库时加载套题列表。 */
async function onAssessmentSourceChange(source: string) {
  selectedBankId.value = ''
  if (source === 'bank') {
    try {
      const res = await getQuestionBanks({ size: 100 })
      bankOptions.value = res.records || []
    } catch {
      bankOptions.value = []
    }
  }
}

function onCreateAssessmentClosed() {
  autoGenerateQuestionsId.value = null
  assessmentSource.value = 'none'
  selectedBankId.value = ''
}

/** 面试题库题目 → 在线测评题目格式。 */
function convertBankToAssessmentQuestions(items: QuestionBankItemVO[]) {
  return items.map((it, idx) => {
    const qt = it.questionType === 1 ? 'multiple_choice'
      : it.questionType === 3 ? 'true_false'
        : it.questionType === 0 ? 'single_choice' : 'essay'
    return {
      questionId: idx + 1,
      type: it.questionType ?? 2,
      questionText: it.question,
      difficulty: it.difficulty === 3 ? '困难' : it.difficulty === 1 ? '简单' : '中等',
      questionType: qt,
      options: parseOptions(it.options || []),
      correctAnswer: it.answer || '',
      score: 10,
    }
  })
}

async function handleCreateAssessment() {
  if (!createAssessmentForm.candidateId) {
    ElMessage.warning('请输入候选人ID')
    return
  }
  creatingAssessment.value = true
  try {
    const vo = await createOnlineAssessment({
      candidateId: String(createAssessmentForm.candidateId),
      candidateName: createAssessmentForm.candidateName,
      candidateEmail: createAssessmentForm.candidateEmail,
      jobTitle: createAssessmentForm.jobTitle,
      type: createAssessmentForm.type,
    })
    showCreateAssessmentDialog.value = false

    // 按题目来源保存题目：AI 出题结果 / 面试题库 / AI 出题区域一键创建
    let attached = false
    try {
      if (assessmentSource.value === 'ai' && questionResult.value) {
        await saveAssessmentQuestions(String(vo.id),
          convertToAssessmentQuestions(questionResult.value))
        attached = true
      } else if (assessmentSource.value === 'bank' && selectedBankId.value) {
        const bank = await getQuestionBank(selectedBankId.value)
        await saveAssessmentQuestions(String(vo.id),
          convertBankToAssessmentQuestions(bank.items || []))
        attached = true
      } else if (autoGenerateQuestionsId.value !== null && questionResult.value) {
        await saveAssessmentQuestions(String(vo.id),
          convertToAssessmentQuestions(questionResult.value))
        attached = true
      }
    } catch {
      ElMessage.warning('测评已创建，但题目保存失败，可稍后手动处理')
    } finally {
      autoGenerateQuestionsId.value = null
    }
    ElMessage.success(attached ? '测评已创建，题目已保存' : '测评创建成功')
    loadOnlineAssessments()
  } catch {
    ElMessage.error('创建失败')
  }
  creatingAssessment.value = false
}

/**
 * 从 AI 返回的参考答案文本中提取客观题的答案键（A/B/C/D）。
 * AI 返回的 referenceAnswer 格式举例：
 * - 单选: "C，Redis 是缓存中间件，不是 JVM 垃圾收集器。"
 * - 多选: "A、B、C、D，消息队列不能替换数据库主键..."
 * - 判断: "正确，CAS 通过比较并交换实现无锁并发..."
 * - 问答: "关键得分点：1.慢调用比例 2.异常比例..."
 */
function extractAnswerKey(questionType: string, referenceAnswer: string, options: { key: string; value: string }[]): string {
  if (!referenceAnswer) return ''
  const ref = referenceAnswer.trim()

  if (questionType === 'true_false') {
    // "正确，xxx" → A, "错误，xxx" → B
    if (ref.startsWith('正确') || ref.toLowerCase().startsWith('true')) return 'A'
    if (ref.startsWith('错误') || ref.toLowerCase().startsWith('false')) return 'B'
    // 如果选项中有"正确"，匹配其 key
    const correctOpt = options.find(o => o.value.includes('正确') || o.value.toLowerCase().includes('true'))
    if (correctOpt) return correctOpt.key
    return ref.length <= 2 ? ref.toUpperCase() : ''
  }

  if (questionType === 'single_choice') {
    // "C，xxx" → C
    const singleMatch = ref.match(/^([A-Ea-e])[，,、\s]/)
    if (singleMatch) return singleMatch[1].toUpperCase()
    // 尝试匹配选项内容
    for (const opt of options) {
      if (ref.startsWith(opt.value) || opt.value && ref.includes(opt.key + '.')) return opt.key
    }
    return ref.length <= 2 ? ref.toUpperCase() : ref
  }

  if (questionType === 'multiple_choice') {
    // "A、B、C、D，xxx" → "A,B,C,D"
    const multiMatch = ref.match(/^([A-Ea-e][、,，\s]*)+/)
    if (multiMatch) {
      const keys = multiMatch[0].match(/[A-Ea-e]/g)
      if (keys) return keys.map(k => k.toUpperCase()).join(',')
    }
    return ref
  }

  // essay: 保留完整参考答案供 AI 评分对比
  return ref
}

/** 将 AI 智能出题结果转换为 AssessmentQuestionItem 列表 */
function convertToAssessmentQuestions(result: typeof questionResult.value) {
  if (!result) return []
  const questions: {
    questionId: number
    type: number
    questionText: string
    difficulty: string
    questionType: string
    options: { key: string; value: string }[]
    correctAnswer: string
    score: number
  }[] = []
  let qid = 1
  const categories = [
    { label: '技术基础题', items: result.techQuestions },
    { label: '项目经验题', items: result.projectQuestions },
    { label: '行为面试题', items: result.behavioralQuestions },
  ]
  for (const cat of categories) {
    if (cat.items) {
      for (const item of cat.items) {
        // 使用 AI 返回的实际题型
        const qt = item.questionType || 'essay'
        // 转换选项：string[] → {key, value}[]
        let opts: { key: string; value: string }[] = []
        if (item.options?.length && (qt === 'single_choice' || qt === 'multiple_choice' || qt === 'true_false')) {
          opts = item.options.map(opt => {
            // LLM 可能返回 "A. xxx" 或 "A) xxx" 格式
            const match = opt.match(/^([A-Ea-e])[\.\)、]\s*(.+)/)
            if (match) {
              return { key: match[1].toUpperCase(), value: match[2] }
            }
            return { key: '', value: opt }
          })
        } else if (qt === 'true_false') {
          // 判断题：给默认的"正确/错误"选项
          opts = [
            { key: 'A', value: '正确' },
            { key: 'B', value: '错误' },
          ]
        }
        questions.push({
          questionId: qid++,
          type: 0,
          questionText: `[${cat.label}] ${item.question}`,
          difficulty: item.difficulty || '中等',
          questionType: qt,
          options: opts,
          correctAnswer: extractAnswerKey(qt, item.referenceAnswer || '', opts),
          score: 0,
        })
      }
    }
  }
  return questions
}

function openCreateAssessmentFromQuestions() {
  // 将本次 AI 出题结果带到在线测评页，并在该页自动打开新建弹窗
  if (!questionResult.value) {
    ElMessage.warning('请先生成 AI 出题结果')
    return
  }
  sessionStorage.setItem('smartrecruit-ai-question-result', JSON.stringify(questionResult.value))
  router.push('/online-assessments?fromAi=1')
}

function openCreateAssessmentDialog() {
  selectedGenerateResult.value = null
  autoGenerateQuestionsId.value = null
  createAssessmentForm.jobTitle = ''
  createAssessmentForm.type = 0
  showCreateAssessmentDialog.value = true
}

watch(showCreateAssessmentDialog, (val) => {
  if (val) loadGenerateResults()
})

// 填分
const showScoreDialog = ref(false)
const fillAssessmentId = ref('')
const scoreForm = reactive({ score: '' })

// 查看测评报告
const showViewAssessmentDialog = ref(false)
const viewAssessmentData = ref<OnlineAssessmentVO | null>(null)

// 查看题目
const showQuestionsDialog = ref(false)
const questionsActiveTab = ref('全部')
const questionsLoading = ref(false)
const viewQuestions = ref<{
  questionId: number; questionText: string; difficulty?: string; questionType: string
  options: { key: string; value: string }[]; correctAnswer?: string
}[]>([])

const groupedViewQuestions = computed(() => {
  const categoryOrder = ['技术基础题', '项目经验题', '行为面试题']
  const categoryIcons: Record<string, string> = {
    '技术基础题': 'Monitor',
    '项目经验题': 'FolderOpened',
    '行为面试题': 'UserFilled',
  }
  const categoryMap = new Map<string, typeof viewQuestions.value>()
  for (const q of viewQuestions.value) {
    const match = q.questionText.match(/^\[(.+?)\]\s*(.*)/)
    const category = match ? match[1] : '其他'
    const text = match ? match[2] : q.questionText
    if (!categoryMap.has(category)) {
      categoryMap.set(category, [])
    }
    categoryMap.get(category)!.push({ ...q, questionText: text })
  }

  const groups: { category: string; icon: string; startNum: number; questions: typeof viewQuestions.value }[] = []
  let runningNum = 0
  // 按固定顺序排列，未识别的类别放末尾
  const orderedCategories = [
    ...categoryOrder.filter(c => categoryMap.has(c)),
    ...Array.from(categoryMap.keys()).filter(c => !categoryOrder.includes(c)),
  ]
  for (const category of orderedCategories) {
    const questions = categoryMap.get(category)!
    groups.push({
      category,
      icon: categoryIcons[category] || 'List',
      startNum: runningNum,
      questions,
    })
    runningNum += questions.length
  }
  return groups
})

const questionTypeLabelMap: Record<string, string> = {
  single_choice: '单选题',
  multiple_choice: '多选题',
  true_false: '判断题',
  essay: '问答题',
  likert: '量表题',
}
function typeLabel(qt: string): string {
  return questionTypeLabelMap[qt] || qt || '未知'
}
function correctLabel(q: { questionType: string; correctAnswer?: string }): string {
  if (!q.correctAnswer) return ''
  if (q.questionType === 'true_false') {
    // 判断题答案可能包含"正确"或"错误"关键词
    const ans = q.correctAnswer.trim()
    if (ans.includes('正确') || ans.toLowerCase().includes('true') || ans === 'A') return '答案：正确'
    if (ans.includes('错误') || ans.toLowerCase().includes('false') || ans === 'B') return '答案：错误'
    return '答案：' + ans
  }
  if (q.questionType === 'essay' || q.questionType === 'likert') return ''
  // MCQ：显示正确选项 key
  if (q.correctAnswer.length <= 2 && /^[A-E]$/i.test(q.correctAnswer)) {
    return '答案：' + q.correctAnswer.toUpperCase()
  }
  return '答案：' + q.correctAnswer
}
function isCorrectOption(q: { questionType: string; correctAnswer?: string }, optKey: string): boolean {
  if (!q.correctAnswer || q.questionType === 'essay' || q.questionType === 'likert') return false
  if (q.questionType === 'true_false') {
    const ans = q.correctAnswer.trim()
    const isCorrect = ans.includes('正确') || ans.toLowerCase().includes('true')
    return (isCorrect && optKey === 'A') || (!isCorrect && optKey === 'B')
  }
  // 单选/多选：匹配选项 key
  return q.correctAnswer
    .split(/[,，、\s]+/)
    .some(k => k.toUpperCase() === optKey.toUpperCase())
}

function handleFillScore(row: OnlineAssessmentVO) {
  fillAssessmentId.value = String(row.id)
  scoreForm.score = ''
  showScoreDialog.value = true
}

async function handleSubmitScore() {
  if (!scoreForm.score) {
    ElMessage.warning('请输入成绩')
    return
  }
  try {
    await updateOnlineAssessmentScore(fillAssessmentId.value, scoreForm.score)
    ElMessage.success('成绩已更新')
    showScoreDialog.value = false
    loadOnlineAssessments()
  } catch {
    ElMessage.error('更新失败')
  }
}

// ================================================================
// 反馈表
// ================================================================
const showFeedbackDialog = ref(false)
const feedbackSubmitting = ref(false)
const selectedFeedbackCandidate = ref<string>('')
const feedbackForm = reactive<Record<string, any>>({
  techRating: 0,
  commRating: 0,
  solveRating: 0,
  learnRating: 0,
  teamRating: 0,
  comments: '',
  hireRecommendation: 2,
})

const ratingDimensions = [
  { key: 'techRating', label: '技术深度' },
  { key: 'commRating', label: '沟通表达' },
  { key: 'solveRating', label: '问题解决' },
  { key: 'learnRating', label: '学习能力' },
  { key: 'teamRating', label: '团队协作' },
]

const hireOptions = [
  { value: 0, label: '强烈推荐录用' },
  { value: 1, label: '推荐录用' },
  { value: 2, label: '待定' },
  { value: 3, label: '不推荐' },
]

function handleStarClick(dimKey: string, e: Event) {
  const target = (e.target as HTMLElement).closest('[data-value]') as HTMLElement | null
  if (target) {
    feedbackForm[dimKey] = parseInt(target.dataset.value || '0')
  }
}

async function handleSubmitFeedback() {
  if (!feedbackForm.comments) {
    ElMessage.warning('请填写面试评语')
    return
  }
  feedbackSubmitting.value = true
  // Use either the feedback-specific candidate or the assessment candidate
  const cid = selectedFeedbackCandidate.value || selectedCandidateId.value
  if (!cid) {
    ElMessage.warning('请选择候选人')
    feedbackSubmitting.value = false
    return
  }
  try {
    await submitFeedback(cid, {
      candidateId: assessmentData.value?.candidateId || cid,
      techRating: feedbackForm.techRating || undefined,
      commRating: feedbackForm.commRating || undefined,
      solveRating: feedbackForm.solveRating || undefined,
      learnRating: feedbackForm.learnRating || undefined,
      teamRating: feedbackForm.teamRating || undefined,
      comments: feedbackForm.comments,
      hireRecommendation: feedbackForm.hireRecommendation,
    })
    ElMessage.success('反馈已提交')
    showFeedbackDialog.value = false
  } catch {
    ElMessage.error('提交失败')
  }
  feedbackSubmitting.value = false
}

// ================================================================
// 工具函数
// ================================================================
function formatDate(dateStr: string) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit',
  })
}

// ================================================================
// 生命周期
// ================================================================
onMounted(() => {
  loadStats()
  loadSchedule()
  loadQuestionDepts()
  loadCandidateOptions()
  loadDepartments()
})

onBeforeUnmount(() => {
  if (radarChart) radarChart.destroy()
  if (pollTimer) clearInterval(pollTimer)
})
</script>

<style scoped>
/* ================================================================
   General
   ================================================================ */
.ai-interview-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding: 24px 28px;
}

/* ---- 头部 ---- */
.sr-page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.header-title {
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}
.current-date {
  font-size: 13px;
  color: #64748b;
}
.ai-status-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: #ecfdf5;
  color: #059669;
  padding: 5px 14px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}
.pulse-dot {
  width: 8px;
  height: 8px;
  background: #059669;
  border-radius: 50%;
  position: relative;
}
.pulse-dot::after {
  content: '';
  position: absolute;
  top: -3px;
  left: -3px;
  width: 14px;
  height: 14px;
  background: #059669;
  border-radius: 50%;
  opacity: 0.3;
  animation: pulse 2s ease-out infinite;
}
@keyframes pulse {
  0% { transform: scale(1); opacity: 0.3; }
  70% { transform: scale(2.2); opacity: 0; }
  100% { transform: scale(1); opacity: 0; }
}

/* ---- 统计卡片 ---- */
.stat-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}
.stat-card {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  cursor: default;
  transition: box-shadow 150ms, transform 150ms;
}
.stat-card:hover {
  box-shadow: 0 4px 6px rgba(0,0,0,0.07);
  transform: translateY(-1px);
}
.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.stat-body { flex: 1; min-width: 0; }
.stat-value {
  font-size: 26px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.2;
}
.stat-label {
  font-size: 13px;
  color: #64748b;
  margin-top: 2px;
}

/* ---- Card / Panel ---- */
.card {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
  overflow: hidden;
}
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 20px;
  border-bottom: 1px solid #f1f5f9;
}
.card-header h2 {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
}
.card-body {
  padding: 16px 20px;
}
.card-body.no-padding {
  padding: 0;
}

/* ---- Two-column Layout ---- */
.two-col {
  display: grid;
  grid-template-columns: 1.5fr 1fr;
  gap: 20px;
}

/* ---- Tab Switcher ---- */
.tab-switcher {
  display: flex;
  background: #f1f5f9;
  border-radius: 8px;
  padding: 3px;
  gap: 2px;
}
.tab-btn {
  padding: 6px 16px;
  border-radius: 6px;
  border: none;
  background: transparent;
  color: #64748b;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 150ms;
  white-space: nowrap;
  font-family: inherit;
}
.tab-btn.active {
  background: #fff;
  color: #4f46e5;
  font-weight: 600;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
}

/* ---- Interview List ---- */
.interview-list {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 1px;
  background: #f1f5f9;
}
.interview-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  background: #fff;
  cursor: pointer;
  transition: background 150ms;
  border-left: 3px solid transparent;
}
.interview-item:hover {
  background: #eef2ff;
}
.interview-item.selected {
  background: #eef2ff;
  border-left-color: #4f46e5;
}
.time-slot {
  font-size: 13px;
  font-weight: 600;
  color: #4f46e5;
  white-space: nowrap;
  min-width: 48px;
}
.item-body {
  flex: 1;
  min-width: 0;
}
.candidate-name {
  font-size: 13px;
  font-weight: 600;
  color: #0f172a;
  line-height: 1.3;
}
.item-meta {
  font-size: 12px;
  color: #64748b;
  margin-top: 2px;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.meta-divider {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: #94a3b8;
}
.type-tag {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 500;
  background: #eef2ff;
  color: #4f46e5;
}
.status-badge-sm {
  font-size: 11px;
  padding: 3px 10px;
  border-radius: 100px;
  font-weight: 600;
  white-space: nowrap;
}
.status-badge-sm.s-pending { background: #fffbeb; color: #d97706; }
.status-badge-sm.s-ongoing { background: #f0f9ff; color: #0ea5e9; }
.status-badge-sm.s-done { background: #ecfdf5; color: #059669; }
.status-badge-sm.s-cancelled { background: #fef2f2; color: #dc2626; }

/* ---- Schedule Loading ---- */
.schedule-loading {
  padding: 16px 20px;
}

/* ---- Week Schedule ---- */
.week-schedule {
  display: flex;
  flex-direction: column;
}
.week-day {
  border-bottom: 1px solid #f1f5f9;
}
.week-day:last-child {
  border-bottom: none;
}
.week-day-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}
.week-day-name {
  font-size: 13px;
  font-weight: 700;
  color: #1e293b;
}
.week-day-date {
  font-size: 12px;
  color: #64748b;
  flex: 1;
}
.week-day-count {
  font-size: 11px;
  color: #94a3b8;
  background: #f1f5f9;
  padding: 2px 10px;
  border-radius: 10px;
  font-weight: 500;
}
.week-day-empty {
  padding: 16px;
  text-align: center;
  font-size: 12px;
  color: #94a3b8;
}

/* ---- Empty State ---- */
.empty-state {
  text-align: center;
  padding: 40px 20px;
  color: #94a3b8;
}
.empty-state p {
  font-size: 13px;
  margin-top: 8px;
}

.bank-preview-tip {
  font-size: 13px;
  color: var(--c-text-secondary);
  background: var(--c-primary-bg, #eef2ff);
  border-radius: 6px;
  padding: 8px 12px;
}

/* ---- Buttons ---- */
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 9px 18px;
  border-radius: 8px;
  border: 1px solid transparent;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 150ms;
  font-family: inherit;
  white-space: nowrap;
}
.btn-primary {
  background: #4f46e5;
  color: #fff;
  border-color: #4f46e5;
}
.btn-primary:hover { background: #6366f1; border-color: #6366f1; }
.btn-primary:disabled { opacity: 0.6; cursor: not-allowed; }
.btn-outline {
  background: #fff;
  color: #4f46e5;
  border-color: #4f46e5;
}
.btn-outline:hover { background: #eef2ff; }
.btn-success { background: #059669; color: #fff; }
.btn-success:hover { background: #047857; }
.btn-danger { background: #dc2626; color: #fff; }
.btn-danger:hover { background: #b91c1c; }
.btn-warning { background: #d97706; color: #fff; }
.btn-warning:hover { background: #b45309; }
.btn-sm { padding: 5px 12px; font-size: 12px; }
.btn-xs { padding: 3px 8px; font-size: 11px; }
.btn-block { width: 100%; justify-content: center; }
.btn-group { display: flex; gap: 8px; flex-wrap: wrap; }

/* ---- Spinner ---- */
.spinner {
  display: inline-block;
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}
.spinner.dark {
  border-color: rgba(79,70,229,0.2);
  border-top-color: #4f46e5;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* ---- Form Elements ---- */
.form-group {
  margin-bottom: 18px;
}
.form-label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: #0f172a;
  margin-bottom: 6px;
}
.form-hint {
  font-weight: 400;
  font-size: 11px;
  color: #94a3b8;
}

/* ---- Generate Controls ---- */
.generate-controls {
  padding: 14px 16px;
  background: #fafbfc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  margin-bottom: 16px;
}
.generate-controls .form-group {
  margin-bottom: 14px;
}
.generate-controls .form-group:last-child {
  margin-bottom: 0;
}

.checkbox-group {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.checkbox-item {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: #334155;
  cursor: pointer;
  padding: 5px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  transition: all 150ms;
}
.checkbox-item:hover {
  border-color: #4f46e5;
}
.checkbox-item:has(input:checked) {
  border-color: #4f46e5;
  background: #eef2ff;
  color: #4f46e5;
  font-weight: 500;
}
.checkbox-item input[type="checkbox"] {
  accent-color: #4f46e5;
}

.count-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.count-row:last-child {
  margin-bottom: 0;
}
.count-row-label {
  font-size: 12px;
  font-weight: 500;
  color: #475569;
  min-width: 60px;
  white-space: nowrap;
}
.count-stepper {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: 1;
}
.stepper-btn {
  width: 30px;
  height: 30px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: #fff;
  color: #4f46e5;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-family: inherit;
  transition: all 150ms;
}
.stepper-btn:hover:not(:disabled) {
  border-color: #4f46e5;
  background: #eef2ff;
}
.stepper-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.stepper-value {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
  min-width: 24px;
  text-align: center;
}

/* ---- AI Question Generation ---- */
.question-section-label {
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
  margin: 16px 0 8px 0;
  padding-bottom: 6px;
  border-bottom: 1px dashed #e2e8f0;
  display: flex;
  align-items: center;
  gap: 6px;
}
.question-item-card {
  padding: 8px 12px;
  background: #f1f5f9;
  border-radius: 8px;
  margin-bottom: 6px;
  font-size: 13px;
  color: #0f172a;
  display: flex;
  flex-direction: column;
  gap: 6px;
  line-height: 1.5;
}
.question-item-top {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  flex-wrap: wrap;
}
.q-body-text {
  flex: 1;
  min-width: 0;
}
.q-num {
  color: #4f46e5;
  font-weight: 700;
  flex-shrink: 0;
}
.question-options-inline {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  padding-left: 24px;
}
.q-opt-chip {
  display: inline-block;
  padding: 2px 8px;
  background: #e2e8f0;
  border-radius: 4px;
  font-size: 12px;
  color: #334155;
  white-space: nowrap;
}
.diff-tag {
  font-size: 10px;
  padding: 2px 7px;
  border-radius: 4px;
  font-weight: 600;
  white-space: nowrap;
  margin-left: 4px;
}
.diff-tag.easy { background: #ecfdf5; color: #059669; }
.diff-tag.medium { background: #fffbeb; color: #d97706; }
.diff-tag.hard { background: #fef2f2; color: #dc2626; }

/* Question type tag */
.type-tag {
  font-size: 10px;
  padding: 2px 7px;
  border-radius: 4px;
  font-weight: 600;
  white-space: nowrap;
  margin-left: 4px;
}
.type-tag.type-single_choice { background: #eff6ff; color: #2563eb; }
.type-tag.type-multiple_choice { background: #f5f3ff; color: #7c3aed; }
.type-tag.type-true_false { background: #fff7ed; color: #ea580c; }
.type-tag.type-essay { background: #f0fdf4; color: #16a34a; }

/* Tech question type sub-controls */
.tech-type-sub {
  margin-top: 8px;
  padding: 10px 12px;
  background: #f8fafc;
  border: 1px dashed #e2e8f0;
  border-radius: 6px;
  width: 100%;
}
.tech-type-hint {
  font-size: 12px;
  color: #64748b;
  margin-bottom: 6px;
  display: block;
}
.tech-type-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
}
.tech-type-label {
  font-size: 12px;
  color: #475569;
  width: 48px;
  flex-shrink: 0;
}
.count-stepper-sm .count-stepper {
  gap: 4px;
}
.count-stepper-sm .stepper-btn {
  width: 24px;
  height: 24px;
  font-size: 12px;
}
.count-stepper-sm .stepper-value {
  width: 24px;
  font-size: 13px;
}

/* ---- Assessment Report ---- */
.report-grid {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 24px;
  align-items: start;
}
.score-circle {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  margin: 0 auto 12px;
}
.score-circle.high { background: #ecfdf5; color: #059669; border: 3px solid #059669; }
.score-circle.medium { background: #fffbeb; color: #d97706; border: 3px solid #d97706; }
.score-circle.low { background: #fef2f2; color: #dc2626; border: 3px solid #dc2626; }
.score-num { font-size: 28px; line-height: 1; }
.score-total { font-size: 10px; opacity: 0.7; }
.radar-container {
  background: #f1f5f9;
  border-radius: 8px;
  padding: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.radar-container canvas { max-width: 240px; max-height: 240px; }

.report-detail h3 {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 10px;
}
.summary-text {
  font-size: 13px;
  color: #64748b;
  line-height: 1.7;
  margin-bottom: 16px;
}
.key-moments-title {
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.key-moments {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}
.key-moments li {
  font-size: 12px;
  color: #64748b;
  padding: 6px 10px;
  background: #f1f5f9;
  border-radius: 8px;
  display: flex;
  align-items: flex-start;
  gap: 8px;
  line-height: 1.5;
}
.key-moments li .el-icon {
  color: #4f46e5;
  margin-top: 2px;
  font-size: 11px;
  flex-shrink: 0;
}
.suggested-action {
  padding: 12px 16px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.suggested-action.next-round { background: #ecfdf5; color: #059669; }
.suggested-action.reject { background: #fef2f2; color: #dc2626; }
.suggested-action.retest { background: #fffbeb; color: #d97706; }

/* ---- Online Assessment Table ---- */
.assessment-table :deep(.el-table__header th) {
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  background: #f1f5f9;
  border-bottom: 1px solid #e2e8f0;
}
.assessment-type {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}
.assessment-type.coding { background: #eef2ff; color: #4f46e5; }
.assessment-type.personality { background: #f0f9ff; color: #0ea5e9; }
.assessment-type.iq { background: #fffbeb; color: #d97706; }

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding: 12px 20px;
}

/* ---- Star Rating ---- */
.star-rating {
  display: flex;
  gap: 6px;
  font-size: 24px;
  color: #cbd5e1;
  cursor: pointer;
}
.star-rating .star {
  transition: color 150ms;
}
.star-rating .star.filled {
  color: #f59e0b;
}

/* ---- Radio Group ---- */
.radio-group {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.radio-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #0f172a;
  cursor: pointer;
  padding: 8px 14px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  transition: all 150ms;
}
.radio-item:hover {
  border-color: #4f46e5;
}
.radio-item input[type="radio"] {
  accent-color: #4f46e5;
}
.radio-item:has(input:checked) {
  border-color: #4f46e5;
  background: #eef2ff;
}

/* ---- Responsive ---- */
@media (max-width: 1400px) {
  .two-col {
    grid-template-columns: 1fr;
  }
  .report-grid {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 1200px) {
  .stat-row {
    grid-template-columns: repeat(2, 1fr);
  }
}
@media (max-width: 768px) {
  .stat-row {
    grid-template-columns: 1fr;
  }
}

/* ---- View Assessment Report Dialog ---- */
.view-report-body {
  padding: 4px 0;
}

.report-summary {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  background: #f8fafc;
  border-radius: 12px;
  margin-bottom: 20px;
}
.summary-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: 700;
  flex-shrink: 0;
}
.summary-info {
  flex: 1;
  min-width: 0;
}
.summary-name {
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
  margin-bottom: 2px;
}
.summary-meta {
  font-size: 13px;
  color: #64748b;
  margin-bottom: 2px;
}
.summary-time {
  font-size: 12px;
  color: #94a3b8;
}

.report-result {
  text-align: center;
}

/* Score ring */
.score-ring-wrap {
  display: inline-block;
  margin-bottom: 12px;
}
.score-ring {
  width: 140px;
  height: 140px;
}
.ring-percent {
  font-size: 28px;
  font-weight: 700;
  fill: #0f172a;
}
.ring-sub {
  font-size: 12px;
  fill: #94a3b8;
}

.level-badge {
  display: inline-block;
  padding: 4px 18px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 10px;
}
.level-excellent {
  background: #ecfdf5;
  color: #059669;
}
.level-good {
  background: #eff6ff;
  color: #2563eb;
}
.level-improve {
  background: #fffbeb;
  color: #d97706;
}

.report-desc {
  font-size: 13px;
  color: #475569;
  line-height: 1.7;
  max-width: 420px;
  margin: 0 auto 20px;
}

.report-analysis {
  text-align: left;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 0 4px;
}
.analysis-section h4 {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 6px;
}
.analysis-section ul {
  margin: 0;
  padding: 0 0 0 20px;
}
.analysis-section li {
  font-size: 13px;
  color: #475569;
  line-height: 1.8;
}

/* Personality type */
.personality-type-display {
  padding: 20px 0;
}
.pt-icon {
  font-size: 48px;
  margin-bottom: 8px;
}
.pt-label {
  font-size: 24px;
  font-weight: 700;
  color: #2563eb;
}

/* 邮件发送对话框预览 */
.email-preview {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 12px 16px;
  font-size: 13px;
}
.email-preview .preview-row {
  padding: 4px 0;
  color: #475569;
  line-height: 1.6;
}
.email-preview .preview-label {
  color: #94a3b8;
}

/* ---- 测评列表搜索栏 ---- */
.filter-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}
.btn-text {
  background: none;
  border: none;
  color: #64748b;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  padding: 4px 8px;
  border-radius: 4px;
}
.btn-text:hover {
  color: #4f46e5;
  background: #eef2ff;
}

/* ---- 查看题目弹窗 ---- */
.questions-body {
  max-height: 500px;
  overflow-y: auto;
}
.qd-tabs {
  display: flex;
  gap: 6px;
  margin-bottom: 14px;
  padding-bottom: 10px;
  border-bottom: 1px solid #e2e8f0;
}
.qd-tab {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 14px;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
  background: #fff;
  color: #64748b;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 150ms;
  font-family: inherit;
}
.qd-tab:hover {
  border-color: #4f46e5;
  color: #4f46e5;
}
.qd-tab.active {
  background: #4f46e5;
  border-color: #4f46e5;
  color: #fff;
}
.qd-category-header {
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
  padding: 10px 0 6px 0;
  margin-bottom: 4px;
  border-bottom: 1px dashed #e2e8f0;
  display: flex;
  align-items: center;
  gap: 6px;
}
.qd-category-count {
  font-size: 11px;
  font-weight: 500;
  color: #94a3b8;
  margin-left: auto;
}
.question-detail-item {
  padding: 12px 16px;
  margin-bottom: 10px;
  background: #f8fafc;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}
.qd-header {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 6px;
}
.qd-num {
  font-weight: 700;
  color: #4f46e5;
  font-size: 15px;
}
.qd-type {
  font-size: 11px;
  color: #64748b;
  background: #e2e8f0;
  padding: 1px 8px;
  border-radius: 4px;
}
.qd-type-tag { font-weight: 600; }
.qd-type-single_choice { background: #eff6ff; color: #2563eb; }
.qd-type-multiple_choice { background: #f5f3ff; color: #7c3aed; }
.qd-type-true_false { background: #fff7ed; color: #ea580c; }
.qd-type-essay { background: #f0fdf4; color: #16a34a; }
.qd-type-likert { background: #fdf2f8; color: #db2777; }
.qd-diff {
  font-size: 11px;
  padding: 1px 8px;
  border-radius: 4px;
  font-weight: 600;
}
.qd-diff-简单 { background: #f0fdf4; color: #16a34a; }
.qd-diff-中等 { background: #fff7ed; color: #ea580c; }
.qd-diff-困难 { background: #fef2f2; color: #dc2626; }
.qd-text-inline {
  font-size: 14px;
  color: #1e293b;
  line-height: 1.6;
  margin-left: 6px;
}
.qd-answer {
  font-size: 12px;
  color: #059669;
  font-weight: 600;
  margin-left: auto;
}
.qd-text {
  font-size: 14px;
  color: #1e293b;
  line-height: 1.6;
  margin-bottom: 8px;
}
.qd-options {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.qd-option {
  font-size: 13px;
  color: #475569;
  background: #fff;
  border: 1px solid #e2e8f0;
  padding: 4px 12px;
  border-radius: 6px;
}
.qd-option.is-correct {
  border-color: #059669;
  background: #ecfdf5;
  color: #059669;
  font-weight: 500;
}
.qd-reference {
  margin-top: 8px;
  padding: 10px 14px;
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  border-radius: 6px;
}
.qd-reference-label {
  font-size: 12px;
  font-weight: 600;
  color: #059669;
  margin-bottom: 4px;
}
.qd-reference-text {
  font-size: 13px;
  color: #374151;
  line-height: 1.7;
}
</style>

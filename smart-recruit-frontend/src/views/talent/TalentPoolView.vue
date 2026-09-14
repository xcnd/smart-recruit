<template>
  <div class="talent-pool-page">
    <div class="sr-page-header">
      <h1>人才库</h1>
      <p>管理和搜索储备人才资源</p>
    </div>

    <!-- AI Recommendation Section -->
    <div class="sr-section">
      <div class="talent-recs-header">
        <div class="sr-section-title">AI人才推荐</div>
        <el-select
          v-model="selectedJobId"
          placeholder="请选择招聘岗位"
          @change="onJobSelect"
          clearable
          filterable
          style="width: 280px;"
        >
          <el-option
            v-for="job in publishedJobs"
            :key="job.id"
            :label="job.title"
            :value="job.id"
          />
        </el-select>
      </div>

      <div v-if="!selectedJobId" class="talent-empty-prompt">
        <el-empty description="请选择招聘岗位以获取AI人才推荐" :image-size="80" />
      </div>

      <template v-else>
        <el-alert
          type="info"
          :closable="false"
          show-icon
          style="margin-bottom: 16px;"
        >
          <template #title>
            正在为【{{ selectedJobTitle }}】匹配人才
          </template>
          <template #default>
            <span v-if="recommending">正在匹配中，请稍候...</span>
            <span v-else-if="recommendError" class="recommend-error">{{ recommendError }}</span>
            <span v-else>系统为您找到 <strong>{{ recommendTotal }}</strong> 位高度匹配的候选人，可考虑主动联系。</span>
          </template>
        </el-alert>

        <div v-if="recommending" class="talent-loading">
          <div class="recommend-progress">
            <div class="recommend-progress-header">
              <el-icon class="is-loading"><Loading /></el-icon>
              <span>AI 正在为【{{ selectedJobTitle }}】匹配人才</span>
            </div>
            <div class="recommend-steps">
              <div
                v-for="(step, idx) in recommendSteps"
                :key="idx"
                class="recommend-step"
                :class="{ done: idx < recommendStage, active: idx === recommendStage }"
              >
                <el-icon v-if="idx < recommendStage"><CircleCheck /></el-icon>
                <el-icon v-else-if="idx === recommendStage" class="is-loading"><Loading /></el-icon>
                <span v-else class="step-dot"></span>
                <span>{{ step }}</span>
              </div>
            </div>
            <el-progress
              :percentage="Math.min(100, Math.round(recommendStage / 4 * 100))"
              :stroke-width="8"
              :show-text="false"
              color="#4f46e5"
            />
          </div>
        </div>

        <div v-if="!recommending && recommendations.length" class="recommend-batch-bar">
          <div class="recommend-batch-info">
            第 <strong>{{ recommendPage }}</strong> / {{ recommendTotalPages }} 批
          </div>
          <el-button
            type="primary"
            plain
            :loading="loadingBatch"
            :disabled="recommendTotal <= 8"
            @click="nextRecommendBatch"
          >
            <el-icon style="margin-right: 4px;"><RefreshRight /></el-icon>
            换一批
          </el-button>
        </div>

        <div class="talent-recs" v-if="!recommending && recommendations.length">
          <el-card
            v-for="talent in recommendations"
            :key="talent.id"
            class="talent-card"
            shadow="hover"
            @click="showDetail(talent)"
          >
            <div class="talent-card-header">
              <el-avatar :size="40" style="background: var(--c-primary);">{{ talent.candidateName?.charAt(0) }}</el-avatar>
              <div>
                <div class="talent-card-name">{{ talent.candidateName }}</div>
                <div class="talent-card-role">{{ talent.lastPosition }}</div>
              </div>
              <el-tooltip placement="top" effect="dark">
                <template #content>
                  <div>技能匹配 · 学历匹配 · 经验匹配</div>
                  <div>职位匹配 · 地点匹配</div>
                </template>
                <template #default>
                  <div class="talent-match-score">{{ talent.matchScore }}%</div>
                </template>
              </el-tooltip>
              <el-tag
                v-if="talent.matchDimensions"
                type="danger"
                size="small"
                effect="plain"
                class="ai-tag"
              >AI 匹配</el-tag>
            </div>
            <div class="talent-card-body">
              <div class="talent-card-info">
                <span>{{ talent.education }}</span>
                <span>{{ talent.experience }}年经验</span>
              </div>
              <div class="talent-card-skills">
                <el-tag
                  v-for="s in talent.skills?.slice(0, 3)"
                  :key="s"
                  size="small"
                  effect="plain"
                  style="margin-right: 4px;"
                >
                  {{ s }}
                </el-tag>
              </div>
              <div v-if="talent.matchDimensions" class="talent-match-dims">
                <div
                  v-for="(label, key) in dimLabels"
                  :key="key"
                  class="dim-row"
                  v-show="(talent.matchDimensions || {})[key] !== undefined"
                >
                  <span class="dim-label">{{ label }}</span>
                  <div class="dim-bar">
                    <div
                      class="dim-fill"
                      :style="{ width: Math.min(100, (talent.matchDimensions || {})[key] || 0) + '%' }"
                    ></div>
                  </div>
                  <span class="dim-value">{{ Math.round((talent.matchDimensions || {})[key] || 0) }}</span>
                </div>
              </div>
            </div>
          </el-card>
        </div>
        <el-empty v-else-if="!recommending" description="该岗位暂无匹配的候选人" :image-size="80" />
      </template>

      <!-- Talent Table -->
      <div class="sr-section-title" style="margin-top: 24px;">全部人才</div>
      <div class="sr-toolbar" style="margin-bottom: 12px;">
        <div class="sr-filter-bar">
          <el-input
            v-model="filters.keyword"
            placeholder="搜索姓名、技能、公司..."
            :prefix-icon="Search"
            clearable
            style="width: 300px"
            @input="handleSearch"
          />
          <el-select v-model="filters.skills" multiple filterable placeholder="技能筛选" clearable style="width: 240px">
            <el-option v-for="s in skillOptions" :key="s" :label="s" :value="s" />
          </el-select>
          <el-select v-model="filters.education" placeholder="学历" clearable @change="handleSearch">
            <el-option label="全部" value="" />
            <el-option label="本科" value="本科" />
            <el-option label="硕士" value="硕士" />
            <el-option label="博士" value="博士" />
          </el-select>
          <el-select v-model="filters.experience" placeholder="工作年限" clearable @change="handleSearch">
            <el-option label="全部" value="" />
            <el-option label="应届生" value="FRESH" />
            <el-option label="1-3年" value="JUNIOR" />
            <el-option label="3-5年" value="MIDDLE" />
            <el-option label="5-10年" value="SENIOR" />
            <el-option label="10年以上" value="EXPERT" />
          </el-select>
        </div>
      </div>
      <el-table v-loading="loading" :data="talents" stripe empty-text="暂无人才数据">
        <el-table-column prop="candidateName" label="姓名" width="100">
          <template #default="{ row }">
            <el-link type="primary" @click="showDetail(row)">{{ row.candidateName }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="lastPosition" label="最近职位" min-width="150" />
        <el-table-column prop="currentCompany" label="最近公司" width="180" />
        <el-table-column label="技能" min-width="180">
          <template #default="{ row }">
            <el-tag v-for="s in row.skills" :key="s" size="small" effect="plain" style="margin-right: 4px; margin-bottom: 4px;">
              {{ s }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="匹配度" width="120">
          <template #default="{ row }">
            <el-progress
              :percentage="row.matchScore"
              :color="row.matchScore >= 80 ? '#059669' : row.matchScore >= 60 ? '#d97706' : '#dc2626'"
              :stroke-width="6"
            />
          </template>
        </el-table-column>
        <el-table-column label="标签" width="150">
          <template #default="{ row }">
            <el-tag v-for="t in row.tags" :key="t" size="small" style="margin-right: 4px;">{{ t }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : row.status === 1 ? 'warning' : 'info'" size="small">
              {{ row.status === 0 ? '可联系' : row.status === 1 ? '已联系' : '已沟通' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="showDetail(row)">详情</el-button>
            <el-button link type="success" size="small" @click="handleContact(row)">联系</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- Talent Detail Drawer -->
      <el-drawer
        v-model="drawerVisible"
        :title="currentTalent?.candidateName || '人才详情'"
        size="480px"
        direction="rtl"
      >
        <template v-if="currentTalent">
          <div class="detail-section">
            <div class="detail-header">
              <el-avatar :size="56" style="background: var(--c-primary); font-size: 22px;">
                {{ currentTalent.candidateName?.charAt(0) }}
              </el-avatar>
              <div>
                <div class="detail-name">{{ currentTalent.candidateName }}</div>
                <div class="detail-role">{{ currentTalent.lastPosition || '-' }}</div>
              </div>
              <el-tag :type="currentTalent.matchScore >= 80 ? 'success' : currentTalent.matchScore >= 60 ? 'warning' : 'danger'" effect="dark">
                {{ currentTalent.matchScore }}% 匹配
              </el-tag>
            </div>
          </div>

          <el-divider />

          <div class="detail-section">
            <h4>基本信息</h4>
            <div class="detail-grid">
              <div class="detail-item">
                <span class="detail-label">邮箱</span>
                <span class="detail-value">{{ currentTalent.email || '-' }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">电话</span>
                <span class="detail-value">{{ currentTalent.phone || '-' }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">学历</span>
                <span class="detail-value">{{ currentTalent.education || '-' }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">经验</span>
                <span class="detail-value">{{ currentTalent.experience }}年</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">最近公司</span>
                <span class="detail-value">{{ currentTalent.currentCompany || '-' }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">来源</span>
                <span class="detail-value">{{ sourceLabel(currentTalent.source) }}</span>
              </div>
            </div>
          </div>

          <el-divider />

          <div class="detail-section">
            <h4>技能标签</h4>
            <div class="detail-tags">
              <el-tag v-for="s in currentTalent.skills" :key="s" effect="plain" style="margin: 2px 4px 2px 0;">{{ s }}</el-tag>
            </div>
          </div>

          <div class="detail-section" v-if="currentTalent.tags?.length">
            <h4>人才标签</h4>
            <div class="detail-tags">
              <el-tag v-for="t in currentTalent.tags" :key="t" type="warning" effect="light" style="margin: 2px 4px 2px 0;">{{ t }}</el-tag>
            </div>
          </div>

          <div class="detail-section" v-if="currentTalent.aiTags?.length">
            <h4>AI标签</h4>
            <div class="detail-tags">
              <el-tag v-for="t in currentTalent.aiTags" :key="t" type="success" effect="light" style="margin: 2px 4px 2px 0;">{{ t }}</el-tag>
            </div>
          </div>

          <el-divider />

          <div class="detail-section">
            <h4>简历 ({{ resumes.length }})</h4>
            <div v-if="loadingResumes" style="text-align: center; padding: 16px;">
              <el-icon class="is-loading"><Loading /></el-icon>
              <span style="margin-left: 8px; color: var(--c-text-secondary); font-size: 13px;">加载中...</span>
            </div>
            <el-empty v-else-if="!resumes.length" description="暂无简历" :image-size="40" />
            <div v-else class="resume-list">
              <div v-for="r in resumes" :key="r.id" class="resume-item">
                <div class="resume-item-main">
                  <el-icon color="var(--c-primary)" style="margin-right: 6px;"><Document /></el-icon>
                  <span class="resume-filename">{{ r.fileName }}</span>
                  <el-tag size="small" :type="parseStatusType(r.parseStatus)" effect="light" style="margin-left: 8px;">
                    {{ parseStatusLabel(r.parseStatus) }}
                  </el-tag>
                </div>
                <div class="resume-item-meta">
                  <span>投递: {{ r.jobTitle || '-' }}</span>
                  <span>匹配: {{ r.matchScore }}%</span>
                  <el-tag size="small" :type="screeningType(r.screeningStatus)" effect="plain">
                    {{ screeningLabel(r.screeningStatus) }}
                  </el-tag>
                </div>
                <div class="resume-item-actions">
                  <el-link type="primary" :href="r.fileUrl" target="_blank" :disabled="!r.fileUrl">
                    <el-icon><View /></el-icon> 查看简历
                  </el-link>
                </div>
              </div>
            </div>
          </div>

          <el-divider />

          <div class="detail-section">
            <h4>求职意向</h4>
            <div class="detail-grid">
              <div class="detail-item">
                <span class="detail-label">期望职位</span>
                <span class="detail-value">{{ currentTalent.expectedPosition || '-' }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">期望地点</span>
                <span class="detail-value">{{ currentTalent.expectedLocation || '-' }}</span>
              </div>
              <div class="detail-item" v-if="currentTalent.expectedSalaryMin || currentTalent.expectedSalaryMax">
                <span class="detail-label">期望薪资</span>
                <span class="detail-value">
                  {{ currentTalent.expectedSalaryMin || '-' }}k ~ {{ currentTalent.expectedSalaryMax || '-' }}k
                </span>
              </div>
            </div>
          </div>

          <el-divider />

          <div class="detail-section">
            <h4>匹配状态</h4>
            <div class="detail-grid">
              <div class="detail-item">
                <span class="detail-label">人才池类型</span>
                <span class="detail-value">{{ poolTypeLabel(currentTalent.poolType) }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">技能等级</span>
                <span class="detail-value">{{ skillLevelLabel(currentTalent.skillLevel) }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">入池时间</span>
                <span class="detail-value">{{ currentTalent.createdAt }}</span>
              </div>
              <div class="detail-item" v-if="currentTalent.lastContactAt">
                <span class="detail-label">最近联系</span>
                <span class="detail-value">{{ currentTalent.lastContactAt }}</span>
              </div>
            </div>
          </div>
        </template>

        <template #footer>
          <el-button @click="drawerVisible = false">关闭</el-button>
          <el-button type="primary" @click="handleContact(currentTalent)">联系候选人</el-button>
        </template>
      </el-drawer>

      <div style="display: flex; justify-content: flex-end; margin-top: 16px;">
        <el-pagination
          v-model:current-page="page" v-model:page-size="size" :total="total"
          :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange" @current-change="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount, computed } from 'vue'
import { Search, Document, View, Loading, CircleCheck, RefreshRight } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getTalentPool, startAsyncRecommendation, getAsyncRecommendation } from '@/api/talent'
import { getJobs } from '@/api/job'
import { getResumes } from '@/api/resume'
import type { TalentPoolVO, JobVO, ResumeVO } from '@/types/models'

const loaded = ref(false)
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const talents = ref<TalentPoolVO[]>([])
const recommendations = ref<TalentPoolVO[]>([])
const recommending = ref(false)
const recommendStage = ref(0)
const recommendError = ref('')
const recommendTaskId = ref('')
const recommendTotal = ref(0)
const recommendPage = ref(1)
const loadingBatch = ref(false)
const RECOMMEND_PAGE_SIZE = 8
let recommendPollTimer: ReturnType<typeof setInterval> | null = null

const recommendTotalPages = computed(() =>
  Math.max(1, Math.ceil(recommendTotal.value / RECOMMEND_PAGE_SIZE)),
)

const recommendSteps = ['解析岗位要求', '召回候选人才', 'AI 语义匹配', '生成匹配报告']
const dimLabels: Record<string, string> = {
  skillMatch: '技能',
  roleMatch: '角色',
  experienceMatch: '经验',
  educationMatch: '学历',
  semanticMatch: '语义',
}

const drawerVisible = ref(false)
const currentTalent = ref<TalentPoolVO | null>(null)
const resumes = ref<ResumeVO[]>([])
const loadingResumes = ref(false)

const selectedJobId = ref<string>('')
const publishedJobs = ref<JobVO[]>([])
const selectedJobTitle = computed(() => {
  const job = publishedJobs.value.find(j => j.id === selectedJobId.value)
  return job?.title || ''
})

const filters = reactive({
  keyword: '',
  skills: [] as string[],
  education: '',
  experience: '',
})

const skillOptions = ['Java', 'Python', 'React', 'Vue', 'TypeScript', 'Go', 'Spring Boot', 'Docker', 'Kubernetes', '机器学习', '大数据', '产品规划', '数据分析', 'UI设计']

async function loadTalents() {
  loading.value = true
  try {
    const res = await getTalentPool({ page: page.value, size: size.value, ...filters })
    talents.value = res.records
    total.value = res.total
  } catch {
    talents.value = [
      { id: '1', candidateName: '林峰', email: 'linfeng@email.com', phone: '138****1111', skills: ['Go', 'Docker', 'Kubernetes', '微服务'], education: '硕士', experience: 8, lastPosition: '高级后端工程师', currentCompany: '某科技公司', matchScore: 88, tags: ['技术大牛', '内推优质'], status: 0 as const, lastContactAt: '', createdAt: '2026-03-15' },
      { id: '2', candidateName: '黄薇', email: 'huangwei@email.com', phone: '139****2222', skills: ['产品规划', '数据分析', '用户研究'], education: '硕士', experience: 6, lastPosition: '高级产品经理', currentCompany: '某互联网公司', matchScore: 82, tags: ['产品专家'], status: 1 as const, lastContactAt: '2026-06-10', createdAt: '2026-02-20' },
      { id: '3', candidateName: '郑凯', email: 'zhengkai@email.com', phone: '136****3333', skills: ['React', 'TypeScript', 'Node.js'], education: '本科', experience: 4, lastPosition: '前端开发工程师', currentCompany: '某电商平台', matchScore: 75, tags: [], status: 0 as const, lastContactAt: '', createdAt: '2026-05-08' },
      { id: '4', candidateName: '钱芳', email: 'qianfang@email.com', phone: '137****4444', skills: ['Python', '机器学习', 'NLP', 'PyTorch'], education: '博士', experience: 3, lastPosition: 'AI研究员', currentCompany: '某AI实验室', matchScore: 90, tags: ['AI人才', '高潜'], status: 2 as const, lastContactAt: '2026-06-15', createdAt: '2026-04-12' },
    ]
    total.value = 128
  } finally { loading.value = false }
}

async function loadPublishedJobs() {
  try {
    const res = await getJobs({ status: 1, size: 100 })
    publishedJobs.value = res.records.filter(
      (j: JobVO) => j.status === 1
    )
  } catch {
    publishedJobs.value = []
  }
}

async function onJobSelect() {
  if (!selectedJobId.value) {
    recommendations.value = []
    return
  }
  stopPolling()
  recommendations.value = []
  recommendError.value = ''
  recommendStage.value = 0
  recommendTotal.value = 0
  recommendPage.value = 1
  recommending.value = true
  try {
    const { taskId } = await startAsyncRecommendation(Number(selectedJobId.value))
    recommendTaskId.value = taskId
    startPolling()
  } catch {
    recommendations.value = []
    recommending.value = false
    recommendError.value = '推荐任务启动失败，请稍后重试'
  }
}

/** 轮询异步推荐任务。 */
function startPolling() {
  stopPolling()
  pollRecommendation()
  recommendPollTimer = setInterval(pollRecommendation, 1500)
}

function stopPolling() {
  if (recommendPollTimer) {
    clearInterval(recommendPollTimer)
    recommendPollTimer = null
  }
}

async function pollRecommendation() {
  if (!recommendTaskId.value) return
  try {
    const task = await getAsyncRecommendation(recommendTaskId.value, {
      page: recommendPage.value,
      size: RECOMMEND_PAGE_SIZE,
    })
    if (task.status === 'RUNNING' || task.status === 'PENDING') {
      // 匹配中：阶段随轮询推进，营造流程感
      recommendStage.value = Math.min(3, recommendStage.value + 1)
      return
    }
    if (task.status === 'COMPLETED') {
      recommendations.value = task.results || []
      recommendTotal.value = task.total || 0
      recommendPage.value = task.page || 1
      recommendStage.value = 4
      recommending.value = false
      stopPolling()
      return
    }
    if (task.status === 'FAILED') {
      recommendError.value = task.message || 'AI 匹配失败，请稍后重试'
      recommending.value = false
      stopPolling()
    }
  } catch {
    // 轮询失败继续等待下一次
  }
}

/** 换一批：加载下一批推荐结果，到末尾后回到第一批。 */
async function nextRecommendBatch() {
  const next = recommendPage.value >= recommendTotalPages.value
    ? 1
    : recommendPage.value + 1
  loadingBatch.value = true
  try {
    const task = await getAsyncRecommendation(recommendTaskId.value, {
      page: next,
      size: RECOMMEND_PAGE_SIZE,
    })
    if (task.status === 'COMPLETED') {
      recommendations.value = task.results || []
      recommendPage.value = task.page || next
      recommendTotal.value = task.total || 0
      recommendStage.value = 4
    }
  } catch {
    ElMessage.warning('加载失败，请稍后重试')
  } finally {
    loadingBatch.value = false
  }
}

async function showDetail(talent: TalentPoolVO) {
  currentTalent.value = talent
  resumes.value = []
  drawerVisible.value = true
  await loadResumes(talent.candidateId)
}

async function loadResumes(candidateId: string | number) {
  loadingResumes.value = true
  try {
    const res = await getResumes({ candidateId, size: 10 })
    resumes.value = res.records
  } catch {
    resumes.value = []
  } finally {
    loadingResumes.value = false
  }
}

async function handleContact(talent: TalentPoolVO | null) {
  if (!talent) return
  try {
    await import('@/api/talent').then(m => m.contactCandidate(String(talent.id)))
    ElMessage.success(`已标记联系 ${talent.candidateName}`)
  } catch {
    ElMessage.warning('操作失败，请重试')
  }
}

function sourceLabel(s: number | undefined): string {
  if (s === 0) return '主动投递'
  if (s === 1) return '内推'
  if (s === 2) return '猎头'
  if (s === 3) return '校招'
  return '-'
}

function poolTypeLabel(t: number | undefined): string {
  if (t === 0) return '通用人才池'
  if (t === 1) return '技术人才池'
  if (t === 2) return '管理人才池'
  if (t === 3) return '实习人才池'
  return '-'
}

function skillLevelLabel(l: number | undefined): string {
  if (l === 0) return '初级'
  if (l === 1) return '中级'
  if (l === 2) return '高级'
  if (l === 3) return '专家'
  return '-'
}

function parseStatusLabel(s: string): string {
  const map: Record<string, string> = { PENDING: '待解析', PARSING: '解析中', COMPLETED: '已完成', FAILED: '解析失败' }
  return map[s] || s || '-'
}

function parseStatusType(s: string): 'info' | 'success' | 'warning' | 'danger' {
  const map: Record<string, 'info' | 'success' | 'warning' | 'danger'> = {
    PENDING: 'info', PARSING: 'warning', COMPLETED: 'success', FAILED: 'danger',
  }
  return map[s] || 'info'
}

function screeningLabel(s: number): string {
  if (s === 0) return '待处理'
  if (s === 1) return '已通过'
  if (s === 2) return '已淘汰'
  return '-'
}

function screeningType(s: number): 'info' | 'success' | 'danger' {
  if (s === 0) return 'info'
  if (s === 1) return 'success'
  return 'danger'
}

function handleSearch() { page.value = 1; loadTalents() }
function handlePageChange(p: number) { if (!loaded.value) return; page.value = p; loadTalents() }
function handleSizeChange(s: number) { if (!loaded.value) return; size.value = s; page.value = 1; loadTalents() }

onMounted(async () => {
  await loadTalents()
  await loadPublishedJobs()
  loaded.value = true
})

onBeforeUnmount(() => {
  stopPolling()
})
</script>

<style scoped>
.talent-recs-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.talent-recs-header .sr-section-title {
  margin-bottom: 0;
}

.talent-recs {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
  margin-bottom: 8px;
}

.recommend-batch-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  padding: 10px 16px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}

.recommend-batch-info {
  font-size: 13px;
  color: #475569;
}

.recommend-batch-info strong {
  color: #4f46e5;
  font-size: 15px;
}

.talent-empty-prompt {
  margin: 16px 0;
  padding: 24px;
  background: var(--c-bg-secondary, #f8f9fa);
  border-radius: 8px;
}

.talent-card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.talent-card-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--c-text);
}

.talent-card-role {
  font-size: 12px;
  color: var(--c-text-secondary);
}

.talent-card-body {
  font-size: 12px;
}

.talent-card-info {
  display: flex;
  gap: 12px;
  color: var(--c-text-secondary);
  margin-bottom: 8px;
}

.talent-card-skills {
  display: flex;
  flex-wrap: wrap;
}

/* -- Detail Drawer -- */
.detail-header {
  display: flex;
  align-items: center;
  gap: 14px;
}

.detail-name {
  font-size: 18px;
  font-weight: 600;
  color: var(--c-text);
}

.detail-role {
  font-size: 13px;
  color: var(--c-text-secondary);
  margin-top: 2px;
}

.detail-section h4 {
  font-size: 14px;
  font-weight: 600;
  color: var(--c-text);
  margin: 0 0 10px 0;
}

.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.detail-label {
  font-size: 12px;
  color: var(--c-text-secondary);
}

.detail-value {
  font-size: 13px;
  color: var(--c-text);
}

.detail-tags {
  display: flex;
  flex-wrap: wrap;
}

.resume-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.resume-item {
  padding: 10px 12px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 6px;
  background: var(--c-bg-secondary, #f8f9fa);
}

.resume-item-main {
  display: flex;
  align-items: center;
  margin-bottom: 6px;
}

.resume-filename {
  font-size: 13px;
  font-weight: 500;
  color: var(--c-text);
}

.resume-item-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: var(--c-text-secondary);
  margin-bottom: 6px;
}

.resume-item-actions {
  display: flex;
  gap: 12px;
}

/* ==============================
   AI 推荐：匹配中专业效果
   ============================== */
.talent-loading {
  padding: 8px 4px;
}

.recommend-progress {
  background: linear-gradient(135deg, #eef2ff, #f5f3ff);
  border: 1px solid #e0e7ff;
  border-radius: 12px;
  padding: 20px 24px;
}

.recommend-progress-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #4338ca;
  margin-bottom: 16px;
}

.recommend-steps {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.recommend-step {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 13px;
  color: #94a3b8;
  background: #fff;
  border: 1px solid #e2e8f0;
}

.recommend-step.done {
  color: #059669;
  border-color: #a7f3d0;
  background: #ecfdf5;
}

.recommend-step.active {
  color: #4f46e5;
  border-color: #c7d2fe;
  background: #eef2ff;
  font-weight: 600;
}

.step-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #cbd5e1;
}

.recommend-error {
  color: #dc2626;
}

.ai-tag {
  flex: none;
}

.talent-match-score {
  font-size: 18px;
  font-weight: 700;
  color: #059669;
}

.talent-match-dims {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.dim-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.dim-label {
  width: 34px;
  font-size: 12px;
  color: #64748b;
  flex: none;
}

.dim-bar {
  flex: 1;
  height: 6px;
  border-radius: 3px;
  background: #f1f5f9;
  overflow: hidden;
}

.dim-fill {
  height: 100%;
  border-radius: 3px;
  background: linear-gradient(90deg, #6366f1, #8b5cf6);
}

.dim-value {
  width: 28px;
  text-align: right;
  font-size: 12px;
  font-weight: 600;
  color: #334155;
  flex: none;
}
</style>

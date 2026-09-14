<template>
  <div class="candidate-center-page">
    <div class="sr-page-header">
      <h1>候选人中心</h1>
      <p>管理候选人在招聘流程中的阶段</p>
    </div>

    <!-- Stage Stats -->
    <div class="sr-stat-cards">
      <div class="sr-stat-card" v-for="stat in statsData" :key="stat.key" :style="{ borderLeft: `3px solid ${stat.color}` }">
        <div class="stat-info">
          <div class="stat-value">{{ stat.count }}</div>
          <div class="stat-label">{{ stat.label }}</div>
        </div>
      </div>
    </div>

    <!-- Filter Bar -->
    <div class="sr-section">
      <div class="sr-toolbar">
        <div class="sr-filter-bar">
          <el-input
            v-model="filters.keyword"
            placeholder="搜索姓名/邮箱/手机"
            :prefix-icon="Search"
            clearable
            style="width: 240px"
            @input="onFilterChange"
          />
          <el-select v-model="filters.source" placeholder="来源" clearable style="width: 120px" @change="onFilterChange">
            <el-option label="主动投递" :value="0" />
            <el-option label="内推" :value="1" />
            <el-option label="官网" :value="2" />
            <el-option label="LinkedIn" :value="3" />
            <el-option label="BOSS直聘" :value="4" />
            <el-option label="拉勾" :value="5" />
            <el-option label="猎聘" :value="6" />
            <el-option label="其他" :value="7" />
          </el-select>
          <el-select v-model="filters.education" placeholder="学历" clearable style="width: 110px" @change="onFilterChange">
            <el-option label="高中" :value="0" />
            <el-option label="大专" :value="1" />
            <el-option label="本科" :value="2" />
            <el-option label="硕士" :value="3" />
            <el-option label="博士" :value="4" />
          </el-select>
          <el-date-picker
            v-model="filters.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width: 260px"
            @change="onFilterChange"
          />
        </div>
      </div>
    </div>

    <!-- Tabs -->
    <el-tabs v-model="activeTab" class="candidate-tabs" @tab-change="onTabChange">
      <el-tab-pane v-for="tab in tabs" :key="tab.stage" :label="tab.label" :name="tab.stage">
        <!-- Table -->
        <div v-loading="loading" class="tab-content">
          <el-table :data="candidates" stripe empty-text="暂无候选人数据">
            <el-table-column type="index" label="序号" width="55" />
            <el-table-column prop="id" label="候选人ID" width="170">
              <template #default="{ row }">
                <span class="cell-text" style="font-family: monospace; font-size: 12px;">{{ row.id }}</span>
              </template>
            </el-table-column>
            <el-table-column label="候选人" width="120">
              <template #default="{ row }">
                <div class="candidate-cell">
                  <el-avatar :size="28" :style="{ background: row.avatarColor || 'var(--c-primary)', fontSize: '12px' }">
                    {{ row.name?.charAt(0) }}
                  </el-avatar>
                  <span class="candidate-cell-name">{{ row.name }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="邮箱" width="180">
              <template #default="{ row }"><span class="cell-text">{{ row.email }}</span></template>
            </el-table-column>
            <el-table-column label="手机" width="130">
              <template #default="{ row }"><span class="cell-text">{{ row.phone || '-' }}</span></template>
            </el-table-column>
            <el-table-column label="应聘职位" width="150">
              <template #default="{ row }"><span class="cell-text">{{ row.jobTitle || '-' }}</span></template>
            </el-table-column>
            <el-table-column label="来源" width="90">
              <template #default="{ row }"><span class="cell-text">{{ getSourceLabel(row.source) }}</span></template>
            </el-table-column>
            <el-table-column label="学历" width="70">
              <template #default="{ row }"><span class="cell-text">{{ getEducationLabel(row.education) }}</span></template>
            </el-table-column>
            <el-table-column label="AI匹配分" width="140">
              <template #default="{ row }">
                <div style="display: flex; align-items: center; gap: 8px;">
                  <el-progress :percentage="row.matchScore || 0" :color="scoreColor(row.matchScore || 0)" :stroke-width="8" :show-text="false" style="flex: 1;" />
                  <span class="match-score-text">{{ row.matchScore || 0 }}%</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="技能标签" min-width="140">
              <template #default="{ row }">
                <el-tag
                  v-for="skill in (row.skills || []).slice(0, 4)"
                  :key="skill" size="small" effect="plain"
                  style="margin-right: 4px; margin-bottom: 4px;"
                >{{ skill }}</el-tag>
                <el-tag v-if="(row.skills || []).length > 4" size="small" type="info" effect="plain">+{{ row.skills.length - 4 }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="投递时间" width="170">
              <template #default="{ row }">{{ row.createdAt?.replace('T', ' ').substring(0, 19) || '-' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="210" fixed="right">
              <template #default="{ row }">
                <div class="action-btns">
                  <el-button size="small" text type="primary" @click="openCandidateDetail(row)">详情</el-button>
                  <el-button size="small" text type="warning" @click="handleAdvance(row)">推进</el-button>
                  <el-button size="small" text type="success" @click="handleAddToPool(row)">入库</el-button>
                  <el-button size="small" text type="danger" @click="handleReject(row)">淘汰</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>

          <!-- Pagination -->
          <div class="sr-pagination-footer">
            <el-pagination
              v-model:current-page="currentPage.page"
              v-model:page-size="currentPage.size"
              :total="currentPage.total"
              :page-sizes="[20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="loadCandidates"
              @current-change="loadCandidates"
            />
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCandidates, getCandidateStats, updateStage } from '@/api/candidate'
import { addToPool } from '@/api/talent'
import { getSourceLabel, getEducationLabel, getCandidateStageLabel, kanbanStageToCode } from '@/utils/format'
import { mapCandidateRow } from '@/utils/candidateAdapter'
import type { CandidateVO } from '@/types/models'

const router = useRouter()
const candidates = ref<CandidateVO[]>([])
const loading = ref(false)

// ─── Tabs ───
const tabs = [
  { label: '新候选人', stage: 0 },
  { label: '初筛中', stage: 1 },
  { label: '面试中', stage: 3 },
  { label: 'Offer阶段', stage: 4 },
]

const activeTab = ref(0)

const stageOrder = [0, 1, 3, 4]

// per-tab pagination
const tabPages = reactive<Record<number, { page: number; size: number; total: number }>>({
  0: { page: 1, size: 20, total: 0 },
  1: { page: 1, size: 20, total: 0 },
  3: { page: 1, size: 20, total: 0 },
  4: { page: 1, size: 20, total: 0 },
})

const currentPage = computed(() => tabPages[activeTab.value])

// ─── Filters (shared, no stage — stage comes from active tab) ───
const filters = reactive({
  keyword: '',
  source: undefined as number | undefined,
  education: undefined as number | undefined,
  dateRange: null as [string, string] | null,
})

// ─── Stats (loaded separately) ───
const statsData = reactive([
  { key: 'new', label: '新候选人', count: 0, color: '#0ea5e9' },
  { key: 'screening', label: '初筛中', count: 0, color: '#d97706' },
  { key: 'interview', label: '面试中', count: 0, color: '#4f46e5' },
  { key: 'offer', label: 'Offer阶段', count: 0, color: '#059669' },
])

// ─── Utilities ───
function scoreColor(score: number): string {
  if (score >= 80) return '#059669'
  if (score >= 60) return '#d97706'
  return '#dc2626'
}

// ─── Data Loading ───
async function loadCandidates() {
  loading.value = true
  try {
    const params: Record<string, unknown> = {
      page: currentPage.value.page,
      size: currentPage.value.size,
      stage: activeTab.value,
      screeningStatus: 1, // 只显示简历筛选通过的候选人
    }
    if (filters.keyword) params.keyword = filters.keyword
    if (filters.source != null) params.source = filters.source
    if (filters.education != null) params.education = filters.education
    if (filters.dateRange) {
      params.applyDateStart = filters.dateRange[0]
      params.applyDateEnd = filters.dateRange[1]
    }
    const res = await getCandidates(params)
    currentPage.value.total = Number(res.total)
    candidates.value = ((res.records || []) as unknown as Record<string, unknown>[])
      .map(mapCandidateRow)
  } catch {
    candidates.value = []
    currentPage.value.total = 0
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  try {
    const stats = await getCandidateStats()
    statsData[0].count = stats.newCount
    statsData[1].count = stats.screeningCount
    statsData[2].count = stats.interviewingCount
    statsData[3].count = stats.offeredCount
  } catch { /* ignore */ }
}

function onFilterChange() {
  currentPage.value.page = 1
  loadCandidates()
}

function onTabChange() {
  loadCandidates()
}

// ─── Actions ───
function openCandidateDetail(candidate: CandidateVO) {
  const route = router.resolve(`/candidates/${candidate.id}`)
  window.open(route.href, '_blank')
}

async function handleAdvance(row: CandidateVO) {
  const currentIdx = stageOrder.indexOf(activeTab.value)
  if (currentIdx < 0 || currentIdx >= stageOrder.length - 1) {
    ElMessage.warning('当前阶段无法继续推进')
    return
  }
  const nextStage = stageOrder[currentIdx + 1]
  try {
    await updateStage(String(row.id), { stage: String(nextStage) })
    ElMessage.success(`${row.name} 已推进至${tabs[currentIdx + 1].label}`)
    loadCandidates()
    loadStats()
  } catch {
    ElMessage.error('操作失败')
  }
}

async function handleReject(row: CandidateVO) {
  try {
    await ElMessageBox.confirm(`确定淘汰 ${row.name} 吗？`, '确认淘汰', {
      confirmButtonText: '确定淘汰', cancelButtonText: '取消', type: 'warning',
    })
  } catch { return }
  const rejectCode = kanbanStageToCode('REJECTED')
  try {
    await updateStage(String(row.id), { stage: String(rejectCode) })
    ElMessage.info(`${row.name} 已淘汰`)
    loadCandidates()
    loadStats()
  } catch {
    ElMessage.error('操作失败')
  }
}

async function handleAddToPool(row: CandidateVO) {
  try {
    await addToPool({ candidateId: String(row.id) })
    ElMessage.success(`${row.name} 已添加至人才库`)
  } catch {
    ElMessage.error('添加失败')
  }
}

onMounted(() => {
  loadCandidates()
  loadStats()
})
</script>

<style scoped>
.candidate-tabs {
  margin-top: 4px;
}
.candidate-tabs :deep(.el-tabs__header) {
  margin-bottom: 16px;
}
.tab-content {
  min-height: 300px;
}

.candidate-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}
.candidate-cell-name {
  font-weight: 600;
  color: var(--c-text);
  font-size: 14px;
}
.cell-text {
  color: var(--c-text-secondary);
  font-size: 13px;
}
.match-score-text {
  font-weight: 600;
  font-size: 13px;
  min-width: 36px;
}
.action-btns {
  display: flex;
  align-items: center;
  gap: 0;
}
.action-btns .el-button {
  padding-left: 4px;
  padding-right: 4px;
}

.sr-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}
.sr-filter-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}
.sr-pagination-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>

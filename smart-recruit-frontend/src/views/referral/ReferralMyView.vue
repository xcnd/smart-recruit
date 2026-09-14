<template>
  <div class="rr-page">
    <!-- Page Header -->
    <div class="sr-page-header">
      <h1>内推记录</h1>
      <p>管理和查询所有内推投递记录</p>
    </div>

    <!-- Filter Section -->
    <div class="rr-filter-bar">
      <el-form :model="query" inline size="default">
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="候选人姓名/邮箱/职位"
            clearable
            style="width: 220px"
            @keyup.enter="search"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 140px">
            <el-option
              v-for="(label, value) in ReferralStatusLabels"
              :key="value"
              :label="label"
              :value="Number(value)"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="内推计划">
          <el-select v-model="query.programId" placeholder="全部计划" clearable style="width: 180px">
            <el-option
              v-for="p in programs"
              :key="p.id"
              :label="p.title"
              :value="Number(p.id)"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- Content Area -->
    <div class="rr-section">
      <!-- Loading -->
      <template v-if="loading">
        <el-skeleton :rows="8" animated />
      </template>

      <!-- Error -->
      <div v-else-if="error" class="rr-state-error">
        <el-icon :size="40"><WarningFilled /></el-icon>
        <p>{{ error }}</p>
        <el-button type="primary" @click="fetchRecords">重新加载</el-button>
      </div>

      <!-- Empty -->
      <div v-else-if="pageData.records.length === 0" class="rr-state-empty">
        <el-empty description="暂无内推记录" :image-size="100" />
      </div>

      <!-- Data Table -->
      <template v-else>
        <el-table
          :data="pageData.records"
          stripe
          style="width: 100%"
          row-key="id"
          @expand-change="handleExpand"
        >
          <!-- Expand: Bonus Stages -->
          <el-table-column type="expand">
            <template #default="{ row }">
              <div class="rr-expand-bonus">
                <h4>奖金发放明细</h4>
                <div v-if="bonusLoading[row.id]" class="rr-bonus-loading">
                  <el-icon class="is-loading" :size="20"><Loading /></el-icon>
                  <span>加载中...</span>
                </div>
                <div v-else-if="!bonusMap[row.id] || bonusMap[row.id].length === 0" class="rr-no-bonus">
                  暂无分阶段奖金记录
                </div>
                <div v-else class="rr-bonus-stages">
                  <div
                    v-for="(stage, idx) in bonusMap[row.id]"
                    :key="stage.id"
                    class="rr-bonus-card"
                    :class="{ 'is-paid': stage.status === 1, 'is-cancelled': stage.status === 2 }"
                  >
                    <div class="rr-bonus-card-hd">
                      <span class="rr-stage-num">第{{ idx + 1 }}期</span>
                      <el-tag
                        :type="stage.status === 1 ? 'success' : stage.status === 2 ? 'danger' : 'warning'"
                        size="small"
                      >
                        {{ stage.status === 1 ? '已发放' : stage.status === 2 ? '已取消' : '待发放' }}
                      </el-tag>
                    </div>
                    <div class="rr-bonus-card-body">
                      <span class="rr-stage-name">{{ stage.stageName }}</span>
                      <span class="rr-stage-amount">&yen;{{ stage.amount.toLocaleString('zh-CN') }}</span>
                    </div>
                    <div v-if="stage.paidTime" class="rr-stage-time">
                      发放时间：{{ formatDateTime(stage.paidTime) }}
                    </div>
                  </div>
                </div>
              </div>
            </template>
          </el-table-column>

          <el-table-column type="index" label="序号" width="60" align="center" />

          <el-table-column label="用户ID" width="90" align="center">
            <template #default="{ row }">
              {{ row.candidateId }}
            </template>
          </el-table-column>

          <el-table-column label="用户名" min-width="120">
            <template #default="{ row }">
              <div v-if="row.candidateName">
                <div style="font-weight: 600; font-size: 14px;">{{ row.candidateName }}</div>
                <div v-if="row.candidateUsername" style="font-size: 12px; color: #909399;">{{ row.candidateUsername }}</div>
              </div>
              <span v-else>-</span>
            </template>
          </el-table-column>

          <el-table-column label="计划关联职位" min-width="150" show-overflow-tooltip>
            <template #default="{ row }">
              {{ row.programJobTitle || '-' }}
            </template>
          </el-table-column>

          <el-table-column label="目标职位" min-width="140" show-overflow-tooltip>
            <template #default="{ row }">
              {{ row.jobTitle || '-' }}
            </template>
          </el-table-column>

          <el-table-column label="部门" min-width="110" show-overflow-tooltip>
            <template #default="{ row }">
              {{ row.departmentName || '-' }}
            </template>
          </el-table-column>

          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="getStatusTagType(row.status)" size="small" disable-transitions>
                {{ ReferralStatusLabels[row.status] || '未知' }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column label="奖金" width="110" align="right">
            <template #default="{ row }">
              <span class="rr-amount">&yen;{{ formatAmount(row.bonusAmount) }}</span>
            </template>
          </el-table-column>

          <el-table-column label="发放状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag
                v-if="row.bonusStatus != null"
                :type="getBonusStatusTagType(row.bonusStatus)"
                size="small"
                disable-transitions
              >
                {{ BonusStatusLabels[row.bonusStatus] || '未知' }}
              </el-tag>
              <span v-else class="rr-dash">-</span>
            </template>
          </el-table-column>

          <el-table-column label="已发放" width="110" align="right">
            <template #default="{ row }">
              <span class="rr-amount">&yen;{{ formatAmount(row.bonusPaid) }}</span>
            </template>
          </el-table-column>

          <el-table-column label="提交时间" width="170">
            <template #default="{ row }">
              {{ formatDateTime(row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column label="AI 匹配" width="120" align="center">
            <template #default="{ row }">
              <el-button size="small" text type="primary" @click="openMatch(row)">
                查看推荐
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- Pagination -->
        <div class="rr-pagination">
          <el-pagination
            v-model:current-page="query.page"
            v-model:page-size="query.size"
            :page-sizes="[10, 20, 50]"
            :total="pageData.total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="fetchRecords"
            @current-change="fetchRecords"
          />
        </div>
      </template>
    </div>

    <!-- AI 智能匹配推荐 -->
    <el-dialog v-model="matchDialogVisible" title="AI 智能匹配推荐" width="640px">
      <div v-loading="matchLoading" class="rr-match-list">
        <el-empty
          v-if="!matchLoading && matchList.length === 0"
          description="暂无匹配推荐，点击下方按钮重新匹配"
          :image-size="72"
        />
        <div v-for="m in matchList" :key="m.id" class="rr-match-item">
          <div class="rr-match-head">
            <span class="rr-match-rank">#{{ m.rankNo }}</span>
            <span class="rr-match-title">{{ m.matchedJobTitle || `职位 #${m.matchedJobId}` }}</span>
            <el-tag :type="matchScoreType(m.matchScore)" size="small">
              {{ Number(m.matchScore ?? 0).toFixed(0) }}%
            </el-tag>
            <el-tag v-if="m.source === 'AI'" size="small" type="info" effect="plain">AI 引擎</el-tag>
            <el-tag v-else size="small" type="warning" effect="plain">本地估算</el-tag>
          </div>
          <div v-if="m.recommendation" class="rr-match-desc">{{ m.recommendation }}</div>
          <div v-if="m.suggestedApproach" class="rr-match-desc muted">{{ m.suggestedApproach }}</div>
        </div>
      </div>
      <template #footer>
        <el-button :loading="matchLoading" @click="refreshMatch">重新匹配</el-button>
        <el-button type="primary" @click="matchDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { WarningFilled, Loading } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  getRecords as getRecordsApi,
  getBonusRecords,
  getPrograms,
  getReferralMatches,
  refreshReferralMatches,
} from '@/api/referral'
import { ReferralStatusLabels, BonusStatusLabels } from '@/constants/enums'
import { formatDateTime } from '@/utils/format'
import type { ReferralRecordVO, ReferralProgramVO, BonusRecordVO, ReferralMatchVO } from '@/types/models'
import type { PageResult } from '@/types/api'

// ---- Helpers ----

function getStatusTagType(status: number): 'success' | 'warning' | 'danger' | 'info' | 'primary' | '' {
  const map: Record<number, 'success' | 'warning' | 'danger' | 'info' | 'primary' | ''> = {
    0: 'warning',
    1: 'warning',
    2: 'primary',
    3: 'success',
    4: 'danger',
    5: 'info',
  }
  return map[status] || 'info'
}

function getBonusStatusTagType(bs: number): 'success' | 'warning' | 'danger' | 'info' | 'primary' | '' {
  const map: Record<number, 'success' | 'warning' | 'danger' | 'info' | 'primary' | ''> = {
    0: 'warning',
    1: 'warning',
    2: 'success',
  }
  return map[bs] || 'info'
}

function formatAmount(val: number | null | undefined): string {
  if (val == null || val === 0) return '0'
  return val.toLocaleString('zh-CN')
}

// ---- State ----

const loading = ref(false)
const error = ref('')

// ---- AI 智能匹配 ----
const matchDialogVisible = ref(false)
const matchLoading = ref(false)
const matchList = ref<ReferralMatchVO[]>([])
let currentRecordId = ''

async function openMatch(row: ReferralRecordVO) {
  currentRecordId = row.id
  matchDialogVisible.value = true
  matchList.value = []
  await loadMatch()
}

async function loadMatch() {
  matchLoading.value = true
  try {
    matchList.value = await getReferralMatches(currentRecordId)
  } catch {
    matchList.value = []
  } finally {
    matchLoading.value = false
  }
}

async function refreshMatch() {
  matchLoading.value = true
  try {
    matchList.value = await refreshReferralMatches(currentRecordId)
    ElMessage.success('智能匹配完成')
  } catch {
    // HTTP 拦截器统一提示
  } finally {
    matchLoading.value = false
  }
}

function matchScoreType(score: number): 'success' | 'warning' | 'danger' {
  if (score >= 80) return 'success'
  if (score >= 60) return 'warning'
  return 'danger'
}
const programs = ref<ReferralProgramVO[]>([])
const bonusMap = ref<Record<string, BonusRecordVO[]>>({})
const bonusLoading = ref<Record<string, boolean>>({})

const query = reactive({
  page: 1,
  size: 20,
  keyword: '' as string,
  status: undefined as number | undefined,
  programId: undefined as number | undefined,
})

const pageData = ref<PageResult<ReferralRecordVO>>({
  records: [],
  total: 0,
  size: 20,
  current: 1,
  pages: 0,
})

// ---- Methods ----

async function fetchRecords() {
  loading.value = true
  error.value = ''
  try {
    const res = await getRecordsApi({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      status: query.status,
      programId: query.programId,
    })
    pageData.value = res
    bonusMap.value = {}
  } catch (e: any) {
    console.error('Failed to load referral records:', e)
    error.value = e?.message || '加载内推记录失败'
    pageData.value = { records: [], total: 0, size: query.size, current: query.page, pages: 0 }
  } finally {
    loading.value = false
  }
}

async function fetchPrograms() {
  try {
    programs.value = await getPrograms()
  } catch {
    // Silently ignore, programs dropdown will just be empty
  }
}

function search() {
  query.page = 1
  fetchRecords()
}

function reset() {
  query.keyword = ''
  query.status = undefined
  query.programId = undefined
  query.page = 1
  fetchRecords()
}

async function handleExpand(row: ReferralRecordVO, expandedRows: ReferralRecordVO[]) {
  // Only load bonus records when expanding (not collapsing)
  const isExpanding = expandedRows.some((r) => r.id === row.id)
  if (!isExpanding) return

  if (bonusMap.value[row.id]) return // Already loaded

  bonusLoading.value = { ...bonusLoading.value, [row.id]: true }
  try {
    const data = await getBonusRecords(row.id)
    bonusMap.value = { ...bonusMap.value, [row.id]: data || [] }
  } catch {
    bonusMap.value = { ...bonusMap.value, [row.id]: [] }
  } finally {
    bonusLoading.value = { ...bonusLoading.value, [row.id]: false }
  }
}

// ---- Lifecycle ----

onMounted(() => {
  fetchRecords()
  fetchPrograms()
})
</script>

<style scoped>
.rr-match-list {
  min-height: 120px;
}

.rr-match-item {
  padding: 12px 14px;
  border: 1px solid var(--c-border-light);
  border-radius: var(--c-radius-md);
  margin-bottom: 10px;
  background: var(--el-bg-color);
}

.rr-match-head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.rr-match-rank {
  font-size: 12px;
  font-weight: 700;
  color: var(--c-primary);
}

.rr-match-title {
  flex: 1;
  font-size: 14px;
  font-weight: 600;
  color: var(--c-text);
}

.rr-match-desc {
  margin-top: 6px;
  font-size: 13px;
  color: var(--c-text-secondary);
  line-height: 1.6;
}

.rr-match-desc.muted {
  color: var(--c-text-muted);
}

.rr-page {
  animation: fade-in 0.3s ease;
}

@keyframes fade-in {
  from { opacity: 0; transform: translateY(6px); }
  to   { opacity: 1; transform: translateY(0); }
}

/* ---- Filter Bar ---- */
.rr-filter-bar {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px 4px;
  margin-bottom: 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,.04);
  border: 1px solid #eee;
}

/* ---- Section ---- */
.rr-section {
  background: #fff;
  border-radius: 12px;
  padding: 16px 24px;
  box-shadow: 0 1px 3px rgba(0,0,0,.04);
  border: 1px solid #eee;
}

/* ---- States ---- */
.rr-state-error,
.rr-state-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 0;
  text-align: center;
  color: #999;
  gap: 12px;
}

.rr-state-error {
  color: #f56c6c;
}

/* ---- Amount ---- */
.rr-amount {
  font-variant-numeric: tabular-nums;
  font-weight: 500;
}

.rr-dash {
  color: #ccc;
}

/* ---- Pagination ---- */
.rr-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

/* ---- Expand: Bonus ---- */
.rr-expand-bonus {
  padding: 16px 24px;
  background: #fafafa;
}

.rr-expand-bonus h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.rr-no-bonus {
  color: #999;
  font-size: 13px;
}

.rr-bonus-loading {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #1677ff;
  font-size: 13px;
}

.rr-bonus-stages {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.rr-bonus-card {
  flex: 1;
  min-width: 180px;
  max-width: 240px;
  border: 1px solid #e8ebf1;
  border-radius: 8px;
  padding: 12px 14px;
  background: #fff;
  transition: border-color .2s;
}

.rr-bonus-card.is-paid {
  border-color: #67c23a;
  background: #f0f9eb;
}

.rr-bonus-card.is-cancelled {
  border-color: #f56c6c;
  background: #fef0f0;
}

.rr-bonus-card-hd {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.rr-stage-num {
  font-size: 12px;
  color: #666;
  font-weight: 500;
}

.rr-bonus-card-body {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
}

.rr-stage-name {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.rr-stage-amount {
  font-size: 16px;
  font-weight: 700;
  color: #1677ff;
}

.rr-bonus-card.is-paid .rr-stage-amount {
  color: #67c23a;
}

.rr-stage-time {
  margin-top: 6px;
  font-size: 11px;
  color: #999;
}
</style>

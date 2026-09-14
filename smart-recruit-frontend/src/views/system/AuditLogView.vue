<template>
  <div class="audit-log-page">
    <div class="sr-page-header">
      <h1>审计日志查询</h1>
      <p>查看系统关键操作记录，支持多维筛选与详情追溯</p>
    </div>

    <!-- 统计概览 -->
    <div class="sr-stat-cards">
      <div class="sr-stat-card" style="border-left: 3px solid #4f46e5">
        <div class="stat-icon" style="background: #eef2ff; color: #4f46e5">
          <el-icon :size="20"><Document /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ fmt(stats.total) }}</div>
          <div class="stat-label">日志总量</div>
        </div>
      </div>
      <div class="sr-stat-card" style="border-left: 3px solid #0ea5e9">
        <div class="stat-icon" style="background: #f0f9ff; color: #0ea5e9">
          <el-icon :size="20"><Calendar /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ fmt(stats.todayCount) }}</div>
          <div class="stat-label">今日新增</div>
        </div>
      </div>
      <div class="sr-stat-card" style="border-left: 3px solid #059669">
        <div class="stat-icon" style="background: #ecfdf5; color: #059669">
          <el-icon :size="20"><CircleCheck /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.successRate }}%</div>
          <div class="stat-label">成功率（成功 {{ fmt(stats.successCount) }} / 失败 {{ fmt(stats.failCount) }}）</div>
        </div>
      </div>
      <div class="sr-stat-card" style="border-left: 3px solid #d97706">
        <div class="stat-icon" style="background: #fffbeb; color: #d97706">
          <el-icon :size="20"><Timer /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.avgDurationMs }}ms</div>
          <div class="stat-label">平均耗时</div>
        </div>
      </div>
    </div>

    <!-- 筛选工具栏 -->
    <div class="sr-section">
      <div class="sr-toolbar">
        <div class="sr-filter-bar">
          <el-input v-model="filters.username" placeholder="操作人用户名" clearable style="width: 150px" @keyup.enter="handleSearch" />
          <el-select v-model="filters.module" placeholder="模块" clearable style="width: 140px" @change="handleSearch">
            <el-option v-for="(label, code) in moduleOptions" :key="code" :label="label" :value="Number(code)" />
          </el-select>
          <el-select v-model="filters.action" placeholder="操作动作" clearable style="width: 130px" @change="handleSearch">
            <el-option v-for="(label, code) in actionOptions" :key="code" :label="label" :value="Number(code)" />
          </el-select>
          <el-select v-model="filters.result" placeholder="执行结果" clearable style="width: 120px" @change="handleSearch">
            <el-option label="成功" :value="1" />
            <el-option label="失败" :value="0" />
          </el-select>
          <el-date-picker
            v-model="filters.dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 260px"
            @change="handleSearch"
          />
          <el-input v-model="filters.keyword" placeholder="关键字（用户名/路径/描述）" :prefix-icon="Search" clearable style="width: 220px" @keyup.enter="handleSearch" />
        </div>
        <div>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="resetFilters">重置</el-button>
          <el-button :icon="RefreshRight" @click="reload">刷新</el-button>
        </div>
      </div>
    </div>

    <!-- 日志列表 -->
    <div class="sr-section">
      <el-table :data="records" v-loading="loading" stripe size="default">
        <el-table-column label="序号" width="70" align="center">
          <template #default="{ $index }">
            {{ (filters.page - 1) * filters.size + $index + 1 }}
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="操作时间" width="165" />
        <el-table-column prop="username" label="操作人" width="110">
          <template #default="{ row }">{{ row.username || '-' }}</template>
        </el-table-column>
        <el-table-column prop="moduleLabel" label="模块" width="110" />
        <el-table-column prop="actionLabel" label="动作" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="actionTagType(row.action)">{{ row.actionLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="targetType" label="目标类型" width="130" />
        <el-table-column prop="description" label="操作描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="requestMethod" label="方法" width="80">
          <template #default="{ row }">
            <span class="method-tag" :class="row.requestMethod?.toLowerCase()">{{ row.requestMethod || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="requestUri" label="请求路径" min-width="200" show-overflow-tooltip />
        <el-table-column label="结果" width="90">
          <template #default="{ row }">
            <span class="result-tag" :class="isSuccess(row) ? 'ok' : 'fail'">
              {{ isSuccess(row) ? '成功' : '失败' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="durationMs" label="耗时" width="90">
          <template #default="{ row }">{{ row.durationMs != null ? `${row.durationMs}ms` : '-' }}</template>
        </el-table-column>
        <el-table-column prop="clientIp" label="IP" width="120" />
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="View" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="filters.page"
          v-model:page-size="filters.size"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSearch"
          @current-change="loadLogs"
        />
      </div>
    </div>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="审计日志详情" width="760px" top="6vh">
      <template v-if="detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="操作时间">{{ detail.createTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="操作人">{{ detail.username || '-' }}（{{ detail.userId || '-' }}）</el-descriptions-item>
          <el-descriptions-item label="模块">{{ detail.moduleLabel || '-' }}</el-descriptions-item>
          <el-descriptions-item label="动作">
            <el-tag size="small" :type="actionTagType(detail.action)">{{ detail.actionLabel || '-' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="目标类型">{{ detail.targetType || '-' }}</el-descriptions-item>
          <el-descriptions-item label="目标 ID">{{ detail.targetId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="操作描述" :span="2">{{ detail.description || '-' }}</el-descriptions-item>
          <el-descriptions-item label="请求方法">{{ detail.requestMethod || '-' }}</el-descriptions-item>
          <el-descriptions-item label="请求路径">{{ detail.requestUri || '-' }}</el-descriptions-item>
          <el-descriptions-item label="响应状态">
            <span class="result-tag" :class="isSuccess(detail) ? 'ok' : 'fail'">
              {{ detail.responseStatus ?? '-' }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="客户端 IP">{{ detail.clientIp || '-' }}</el-descriptions-item>
          <el-descriptions-item label="执行耗时">{{ detail.durationMs != null ? `${detail.durationMs}ms` : '-' }}</el-descriptions-item>
          <el-descriptions-item label="链路 ID">{{ detail.traceId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="User-Agent" :span="2">
            <div class="detail-mono">{{ detail.userAgent || '-' }}</div>
          </el-descriptions-item>
          <el-descriptions-item label="请求参数" :span="2">
            <pre class="detail-mono detail-params">{{ detail.requestParams || '无' }}</pre>
          </el-descriptions-item>
          <el-descriptions-item v-if="detail.errorMsg" label="错误信息" :span="2">
            <div class="detail-error">{{ detail.errorMsg }}</div>
          </el-descriptions-item>
        </el-descriptions>
      </template>
      <template #footer>
        <el-button type="primary" @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Search, Refresh, RefreshRight, View, Document, Calendar, CircleCheck, Timer } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getAuditLogs, getAuditLogStats, getAuditLogDetail, type AuditLogQuery } from '@/api/audit'
import type { AuditLogVO, AuditLogDetailVO, AuditLogStatsVO } from '@/types/models'

const moduleOptions: Record<number, string> = {
  0: '系统管理',
  1: '职位管理',
  2: '候选人管理',
  3: '面试管理',
  4: 'Offer管理',
  5: '入职管理',
  6: '人才库',
  7: '内推管理',
  8: 'AI引擎',
}

const actionOptions: Record<number, string> = {
  0: '创建',
  1: '更新',
  2: '删除',
  3: '导出',
  4: '导入',
  5: '登录',
}

const filters = reactive<{
  page: number
  size: number
  username?: string
  module?: number
  action?: number
  result?: number
  dateRange?: [string, string] | null
  keyword?: string
}>({
  page: 1,
  size: 20,
  username: undefined,
  module: undefined,
  action: undefined,
  result: undefined,
  dateRange: null,
  keyword: undefined,
})

const loading = ref(false)
const records = ref<AuditLogVO[]>([])
const total = ref(0)
const stats = reactive<AuditLogStatsVO>({
  total: 0,
  todayCount: 0,
  successCount: 0,
  failCount: 0,
  successRate: 0,
  avgDurationMs: 0,
})

const detailVisible = ref(false)
const detail = ref<AuditLogDetailVO | null>(null)

function fmt(n: number): string {
  if (n >= 10000) return (n / 10000).toFixed(1) + '万'
  return n.toLocaleString()
}

function buildQuery(): AuditLogQuery {
  const q: AuditLogQuery = {
    page: filters.page,
    size: filters.size,
  }
  if (filters.username) q.username = filters.username
  if (filters.module !== undefined) q.module = filters.module
  if (filters.action !== undefined) q.action = filters.action
  if (filters.result !== undefined) q.result = filters.result
  if (filters.dateRange?.length === 2) {
    q.startDate = filters.dateRange[0]
    q.endDate = filters.dateRange[1]
  }
  if (filters.keyword) q.keyword = filters.keyword
  return q
}

async function loadLogs() {
  loading.value = true
  try {
    const res = await getAuditLogs(buildQuery())
    records.value = res.records ?? []
    total.value = res.total ?? 0
  } catch {
    ElMessage.error('审计日志加载失败')
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  try {
    const s = await getAuditLogStats()
    Object.assign(stats, s)
  } catch {
    // 统计失败不阻塞列表
  }
}

function handleSearch() {
  filters.page = 1
  loadLogs()
}

function resetFilters() {
  Object.assign(filters, {
    page: 1,
    size: 20,
    username: undefined,
    module: undefined,
    action: undefined,
    result: undefined,
    dateRange: null,
    keyword: undefined,
  })
  loadLogs()
  loadStats()
}

function reload() {
  loadLogs()
  loadStats()
}

async function openDetail(row: AuditLogVO) {
  detail.value = null
  detailVisible.value = true
  try {
    detail.value = await getAuditLogDetail(row.id)
  } catch {
    ElMessage.error('审计日志详情加载失败')
    detailVisible.value = false
  }
}

function isSuccess(row: AuditLogVO | AuditLogDetailVO): boolean {
  if (row.errorMsg) return false
  if (row.responseStatus == null) return true
  return row.responseStatus < 400
}

function actionTagType(action?: number): 'success' | 'warning' | 'danger' | 'info' {
  if (action === 0) return 'success' // 创建
  if (action === 1) return 'warning' // 更新
  if (action === 2) return 'danger' // 删除
  return 'info'
}

onMounted(() => {
  loadLogs()
  loadStats()
})
</script>

<style scoped>
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.method-tag {
  display: inline-block;
  padding: 1px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
  background: #f1f5f9;
  color: #475569;
}

.method-tag.get { background: #ecfdf5; color: #059669; }
.method-tag.post { background: #eff6ff; color: #2563eb; }
.method-tag.put { background: #fffbeb; color: #d97706; }
.method-tag.delete { background: #fef2f2; color: #dc2626; }

.result-tag {
  display: inline-block;
  padding: 1px 10px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 500;
}

.result-tag.ok { color: #059669; background: #ecfdf5; }
.result-tag.fail { color: #dc2626; background: #fef2f2; }

.detail-mono {
  font-family: 'SF Mono', Menlo, Consolas, monospace;
  font-size: 12px;
  color: #334155;
  word-break: break-all;
}

.detail-params {
  max-height: 220px;
  overflow: auto;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 8px 10px;
  margin: 0;
  white-space: pre-wrap;
}

.detail-error {
  color: #dc2626;
  word-break: break-all;
}
</style>

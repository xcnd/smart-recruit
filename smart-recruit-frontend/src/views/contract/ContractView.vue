<template>
  <div class="contract-page">
    <div class="sr-page-header">
      <h1>合同管理</h1>
      <p>管理招聘环节的 Offer 合同：创建、审批、发送签署、归档与打印</p>
    </div>

    <!-- 统计概览 -->
    <div class="sr-stat-cards">
      <div class="sr-stat-card" style="border-left: 3px solid #4f46e5" @click="filterByStatus(null)">
        <div class="stat-info">
          <div class="stat-value">{{ stats.total }}</div>
          <div class="stat-label">合同总数</div>
        </div>
      </div>
      <div class="sr-stat-card" style="border-left: 3px solid #94a3b8" @click="filterByStatus(0)">
        <div class="stat-info">
          <div class="stat-value">{{ stats.draftCount }}</div>
          <div class="stat-label">草稿</div>
        </div>
      </div>
      <div class="sr-stat-card" style="border-left: 3px solid #d97706" @click="filterByStatus(1)">
        <div class="stat-info">
          <div class="stat-value">{{ stats.pendingCount }}</div>
          <div class="stat-label">待审批</div>
        </div>
      </div>
      <div class="sr-stat-card" style="border-left: 3px solid #0ea5e9" @click="filterByStatus(3)">
        <div class="stat-info">
          <div class="stat-value">{{ stats.sentCount }}</div>
          <div class="stat-label">待签署</div>
        </div>
      </div>
      <div class="sr-stat-card" style="border-left: 3px solid #059669" @click="filterByStatus(4)">
        <div class="stat-info">
          <div class="stat-value">{{ stats.signedCount }}</div>
          <div class="stat-label">已签署</div>
        </div>
      </div>
      <div class="sr-stat-card" style="border-left: 3px solid #0d9488" @click="filterByStatus(7)">
        <div class="stat-info">
          <div class="stat-value">{{ stats.effectiveCount }}</div>
          <div class="stat-label">生效中</div>
        </div>
      </div>
    </div>

    <!-- 筛选工具栏 -->
    <div class="sr-section">
      <div class="sr-toolbar">
        <div class="sr-filter-bar">
          <el-input v-model="filters.keyword" placeholder="合同编号/候选人/职位" clearable style="width: 220px" @keyup.enter="handleSearch" />
          <el-select v-model="filters.status" placeholder="状态" clearable style="width: 130px" @change="handleSearch">
            <el-option v-for="(label, code) in statusOptions" :key="code" :label="label" :value="Number(code)" />
          </el-select>
          <el-date-picker v-model="filters.dateRange" type="daterange" value-format="YYYY-MM-DD"
                          range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期"
                          style="width: 260px" @change="handleSearch" />
        </div>
        <el-button type="primary" :icon="Plus" @click="openCreateDialog">从 Offer 创建合同</el-button>
      </div>
    </div>

    <!-- 合同列表 -->
    <div class="sr-section">
      <el-table :data="records" v-loading="loading" stripe size="default">
        <el-table-column label="序号" width="64" align="center">
          <template #default="{ $index }">{{ (filters.page - 1) * filters.size + $index + 1 }}</template>
        </el-table-column>
        <el-table-column prop="contractNo" label="合同编号" width="140" />
        <el-table-column prop="candidateName" label="候选人" width="100" />
        <el-table-column prop="jobTitle" label="职位" min-width="130" show-overflow-tooltip />
        <el-table-column prop="departmentName" label="部门" width="120" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="statusTagType(row.status)">{{ row.statusLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="voidReason" label="作废原因" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.voidReason || '-' }}</template>
        </el-table-column>
        <el-table-column prop="signedByHr" label="HR签署" width="90">
          <template #default="{ row }">{{ row.signedByHr || '-' }}</template>
        </el-table-column>
        <el-table-column prop="signedByCandidate" label="候选人签署" width="100">
          <template #default="{ row }">{{ row.signedByCandidate || '-' }}</template>
        </el-table-column>
        <el-table-column prop="signTime" label="签署时间" width="160" />
        <el-table-column prop="createBy" label="创建人" width="90" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goDetail(row.id)">详情</el-button>
            <el-button v-if="row.status === 0" link type="warning" @click="submitApproval(row)">提交审批</el-button>
            <el-button v-if="row.status === 1" link type="success" @click="approve(row)">审批</el-button>
            <el-button
              v-if="row.status === 2"
              link
              type="primary"
              :loading="sendingId === row.id"
              :disabled="sendingId !== null"
              @click="send(row)"
            >
              {{ sendingId === row.id ? '发送中...' : '发送' }}
            </el-button>
            <el-button v-if="[2, 3, 4].includes(row.status)" link type="success" @click="openHrSign(row)">HR签署</el-button>
            <el-button v-if="[4, 7].includes(row.status)" link type="danger" @click="voidIt(row)">作废</el-button>
            <el-button v-if="[0, 5].includes(row.status)" link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="filters.page"
          v-model:page-size="filters.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSearch"
          @current-change="loadData"
        />
      </div>
    </div>

    <!-- 从 Offer 创建合同 -->
    <el-dialog v-model="createVisible" title="从 Offer 创建合同" width="540px">
      <el-form label-position="top">
        <el-form-item label="选择 Offer">
          <el-select
            v-model="createOfferId"
            filterable
            remote
            :remote-method="searchOffers"
            :loading="offerLoading"
            placeholder="输入 Offer 编号或候选人姓名搜索"
            style="width: 100%"
          >
            <el-option
              v-for="o in offerOptions"
              :key="o.id"
              :label="`${o.offerNo} · ${o.candidateName}${o.positionTitle ? '（' + o.positionTitle + '）' : ''}`"
              :value="o.id"
            >
              <div class="offer-option">
                <span class="offer-no">{{ o.offerNo }}</span>
                <span class="offer-name">{{ o.candidateName }}</span>
                <span class="offer-position">{{ o.positionTitle || '-' }}</span>
                <el-tag size="small" type="success" effect="plain">{{ o.statusLabel }}</el-tag>
              </div>
            </el-option>
            <el-option v-if="!offerLoading && offerOptions.length === 0" :value="''" disabled>
              未找到可创建合同的 Offer（仅已审批/已发送/已接受）
            </el-option>
          </el-select>
        </el-form-item>
        <div class="field-hint">仅展示已审批/已发送/已接受的 Offer，选择后系统自动根据 Offer 信息生成合同草稿。</div>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="doCreate">创建合同</el-button>
      </template>
    </el-dialog>

    <!-- HR 签署 -->
    <el-dialog v-model="hrSignVisible" title="HR 签署合同" width="420px">
      <el-form label-position="top">
        <el-form-item label="签署人姓名">
          <el-input v-model="hrSignName" placeholder="如：李娜" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="hrSignVisible = false">取消</el-button>
        <el-button type="primary" :loading="signing" @click="doHrSign">确认签署</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  getContracts,
  getContractStats,
  createContract,
  submitContractApproval,
  approveContract,
  sendContract,
  hrSignContract,
  voidContract,
  deleteContract,
} from '@/api/contract'
import { getOfferOptions } from '@/api/offer'
import type { ContractVO, ContractStatsVO } from '@/types/models'
import type { OfferOptionVO } from '@/types/models'

const router = useRouter()

const statusOptions: Record<number, string> = {
  0: '草稿', 1: '待审批', 2: '已审批', 3: '待签署', 4: '已签署', 5: '已拒绝', 7: '生效中', 8: '已作废',
}

const filters = reactive<{
  page: number
  size: number
  keyword?: string
  status?: number
  dateRange?: [string, string] | null
}>({ page: 1, size: 20, keyword: undefined, status: undefined, dateRange: null })

const loading = ref(false)
const records = ref<ContractVO[]>([])
const total = ref(0)
const stats = reactive<ContractStatsVO>({
  total: 0, draftCount: 0, pendingCount: 0, sentCount: 0, signedCount: 0, archivedCount: 0, effectiveCount: 0,
})

const createVisible = ref(false)
const createOfferId = ref<number | undefined>(undefined)
const creating = ref(false)
const offerOptions = ref<OfferOptionVO[]>([])
const offerLoading = ref(false)
const sendingId = ref<string | null>(null)

const hrSignVisible = ref(false)
const hrSignName = ref('')
const signing = ref(false)
let signingContract: ContractVO | null = null

function buildQuery() {
  const q: Record<string, unknown> = { page: filters.page, size: filters.size }
  if (filters.keyword) q.keyword = filters.keyword
  if (filters.status !== undefined) q.status = filters.status
  if (filters.dateRange?.length === 2) {
    q.startDate = filters.dateRange[0]
    q.endDate = filters.dateRange[1]
  }
  return q
}

async function loadData() {
  loading.value = true
  try {
    const res = await getContracts(buildQuery())
    records.value = res.records ?? []
    total.value = res.total ?? 0
  } catch {
    ElMessage.error('合同列表加载失败')
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  try {
    Object.assign(stats, await getContractStats())
  } catch {
    // 统计失败不阻塞列表
  }
}

function handleSearch() {
  filters.page = 1
  loadData()
}

function filterByStatus(status: number | null) {
  filters.status = status ?? undefined
  handleSearch()
}

function goDetail(id: string) {
  router.push(`/contracts/${id}`)
}

function openCreateDialog() {
  createVisible.value = true
  createOfferId.value = undefined
  offerOptions.value = []
  searchOffers('')
}

async function searchOffers(keyword: string) {
  offerLoading.value = true
  try {
    offerOptions.value = await getOfferOptions(keyword || undefined)
  } catch {
    offerOptions.value = []
  } finally {
    offerLoading.value = false
  }
}

async function doCreate() {
  if (!createOfferId.value) {
    ElMessage.warning('请输入 Offer ID')
    return
  }
  creating.value = true
  try {
    const created = await createContract(createOfferId.value)
    ElMessage.success(`合同 ${created.contractNo} 创建成功`)
    createVisible.value = false
    handleSearch()
    loadStats()
  } catch {
    ElMessage.error('合同创建失败，请确认 Offer 存在')
  } finally {
    creating.value = false
  }
}

async function submitApproval(row: ContractVO) {
  await ElMessageBox.confirm(`确认提交合同 ${row.contractNo} 进入审批？`, '提交审批', { type: 'warning' })
  await submitContractApproval(row.id)
  ElMessage.success('已提交审批')
  handleSearch()
}

async function approve(row: ContractVO) {
  await ElMessageBox.confirm(`确认审批通过合同 ${row.contractNo}？`, '审批通过', { type: 'warning' })
  await approveContract(row.id)
  ElMessage.success('审批通过')
  handleSearch()
}

async function send(row: ContractVO) {
  await ElMessageBox.confirm(`确认发送合同 ${row.contractNo} 给候选人签署？`, '发送签署', { type: 'warning' })
  sendingId.value = row.id
  try {
    await sendContract(row.id)
    ElMessage.success('已发送签署邮件至候选人邮箱')
    handleSearch()
  } catch {
    ElMessage.error('发送失败，请稍后重试')
  } finally {
    sendingId.value = null
  }
}

function openHrSign(row: ContractVO) {
  signingContract = row
  hrSignName.value = ''
  hrSignVisible.value = true
}

async function doHrSign() {
  if (!hrSignName.value.trim()) {
    ElMessage.warning('请输入签署人姓名')
    return
  }
  if (!signingContract) return
  signing.value = true
  try {
    await hrSignContract(signingContract.id, hrSignName.value.trim())
    ElMessage.success('HR 签署成功')
    hrSignVisible.value = false
    handleSearch()
  } catch {
    ElMessage.error('签署失败')
  } finally {
    signing.value = false
  }
}

async function remove(row: ContractVO) {
  await ElMessageBox.confirm(`确认删除合同 ${row.contractNo}？`, '删除', { type: 'warning' })
  await deleteContract(row.id)
  ElMessage.success('已删除')
  handleSearch()
  loadStats()
}

async function voidIt(row: ContractVO) {
  try {
    const { value } = await ElMessageBox.prompt('请填写作废原因', `作废合同 ${row.contractNo}`, {
      confirmButtonText: '确认作废',
      cancelButtonText: '取消',
      inputPlaceholder: '请输入作废原因（必填）',
      inputValidator: (v: string) => (v && v.trim() ? true : '作废原因不能为空'),
      type: 'warning',
    })
    await voidContract(row.id, value.trim())
    ElMessage.success('合同已作废')
    handleSearch()
    loadStats()
  } catch {
    // 用户取消或校验未通过
  }
}

function statusTagType(status?: number): 'primary' | 'success' | 'warning' | 'danger' | 'info' {
  if (status === 4) return 'primary'
  if (status === 6 || status === 7) return 'success'
  if (status === 1 || status === 2) return 'warning'
  if (status === 5 || status === 8) return 'danger'
  return 'info'
}

onMounted(() => {
  loadData()
  loadStats()
})
</script>

<style scoped>
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.field-hint {
  font-size: 12px;
  color: #94a3b8;
  line-height: 1.6;
}

.offer-option {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}

.offer-option .offer-no {
  font-weight: 600;
  color: #1e293b;
  min-width: 130px;
}

.offer-option .offer-name {
  color: #334155;
  min-width: 70px;
}

.offer-option .offer-position {
  flex: 1;
  color: #94a3b8;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>

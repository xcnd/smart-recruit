<template>
  <div class="approval-detail-page">
    <!-- Loading -->
    <el-skeleton v-if="loading" :rows="10" animated />

    <!-- Error: Offer not found -->
    <el-result
      v-else-if="loadError"
      icon="error"
      title="Offer 不存在或已被删除"
      sub-title="请确认链接是否正确"
    >
      <template #extra>
        <el-button type="primary" @click="$router.push('/offers/approval')">
          返回审批列表
        </el-button>
      </template>
    </el-result>

    <!-- Main Content -->
    <template v-else-if="offer">
      <!-- ====== Header ====== -->
      <div class="page-header">
        <div class="header-left">
          <el-button :icon="ArrowLeft" @click="goBack">返回</el-button>
          <h1 class="page-title">Offer 审批详情</h1>
          <el-tag :type="statusTagType(offer.status)" size="large" class="status-tag">
            {{ statusLabel(offer.status) }}
          </el-tag>
        </div>
        <span class="offer-no" v-if="offer.offerNo">{{ offer.offerNo }}</span>
      </div>

      <!-- ====== Cannot Approve Alert ====== -->
      <el-alert
        v-if="offer.status !== 1"
        :title="offer.status === 2 ? '该Offer已审批通过' : offer.status === 0 ? '该Offer尚未提交审批' : '该Offer当前状态不可审批'"
        :type="offer.status === 2 ? 'success' : 'warning'"
        :closable="false"
        show-icon
        style="margin-bottom: 20px;"
      />
      <el-alert
        v-else-if="!isCurrentApprover"
        title="您不是当前阶段的审批人，无法操作"
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom: 20px;"
      >
        <template v-if="currentStageName">
          当前阶段：<strong>{{ currentStageName }}</strong>
          <span v-if="currentStageApprovers">，审批人：{{ currentStageApprovers }}</span>
        </template>
      </el-alert>

      <!-- ====== Offer Card ====== -->
      <el-card class="info-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span>Offer 信息</span>
            <el-tag v-if="offer.offerNo" size="small">{{ offer.offerNo }}</el-tag>
          </div>
        </template>
        <div class="info-grid">
          <div class="info-item">
            <span class="info-label">候选人</span>
            <span class="info-value">{{ offer.candidateName }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">职位</span>
            <span class="info-value">{{ offer.positionTitle }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">部门</span>
            <span class="info-value">{{ offer.departmentName }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">职级</span>
            <span class="info-value">{{ offer.level || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">基本月薪</span>
            <span class="info-value salary">¥{{ formatMoney(offer.baseSalary) }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">年终奖月数</span>
            <span class="info-value">{{ offer.bonusMonths || '-' }} 个月</span>
          </div>
          <div class="info-item">
            <span class="info-label">期权</span>
            <span class="info-value">{{ offer.stockOptions != null ? formatMoney(offer.stockOptions) : '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">签约奖金</span>
            <span class="info-value">{{ offer.signOnBonus != null ? '¥' + formatMoney(offer.signOnBonus) : '-' }}</span>
          </div>
          <div class="info-item full-width">
            <span class="info-label">年薪总包</span>
            <span class="info-value total-package">¥{{ formatMoney(offer.totalPackage) }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">期望入职日期</span>
            <span class="info-value">{{ offer.expectedOnboardDate || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">Offer有效期</span>
            <span class="info-value">{{ offer.validUntil || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">创建时间</span>
            <span class="info-value">{{ offer.createTime || '-' }}</span>
          </div>
        </div>
      </el-card>

      <!-- ====== Approval Flow Progress ====== -->
      <el-card class="flow-card" shadow="never" v-if="stages.length > 0">
        <template #header>
          <span>审批流程</span>
        </template>
        <div class="flow-progress">
          <div
            v-for="(stage, si) in stages"
            :key="si"
            class="flow-node"
            :class="{
              done: stageLevel > stage.level,
              active: stageLevel === stage.level,
              rejected: stageLevel === stage.level && isRejected,
            }"
          >
            <div class="flow-node-circle">
              <el-icon v-if="stageLevel > stage.level"><Check /></el-icon>
              <el-icon v-else-if="stageLevel === stage.level && isRejected"><Close /></el-icon>
              <span v-else>{{ stage.level }}</span>
            </div>
            <div class="flow-node-content">
              <div class="flow-node-name">{{ stage.nodeName }}</div>
              <div class="flow-node-approvers">
                <template v-if="stageLevel > stage.level">
                  已审批
                </template>
                <template v-else-if="stageLevel === stage.level && isRejected">
                  驳回人：{{ offer.approvals?.find(a => a.status === 2)?.approverName || '-' }}
                </template>
                <template v-else-if="stage.approvers?.length">
                  {{ stage.approvers.map(a => a.approverName).join('、') }}
                </template>
                <template v-else>
                  待分配
                </template>
              </div>
            </div>
            <div class="flow-node-tag">
              <el-tag v-if="stageLevel > stage.level" size="small" type="success">已通过</el-tag>
              <el-tag v-else-if="stageLevel === stage.level && isRejected" size="small" type="danger">已驳回</el-tag>
              <el-tag v-else-if="stageLevel === stage.level" size="small" type="warning">当前</el-tag>
              <el-tag v-else size="small" type="info">待审批</el-tag>
            </div>
            <div v-if="si < stages.length - 1" class="flow-connector" :class="{ done: stageLevel > stage.level }" />
          </div>
        </div>
      </el-card>

      <!-- ====== Approval History ====== -->
      <el-card class="history-card" shadow="never" v-if="offer.approvals?.length">
        <template #header>
          <span>审批记录</span>
        </template>
        <el-timeline>
          <el-timeline-item
            v-for="a in sortedApprovals"
            :key="a.id"
            :type="a.status === 1 ? 'success' : a.status === 2 ? 'danger' : 'info'"
            :timestamp="a.approveTime || ''"
            placement="top"
          >
            <div class="approval-record">
              <div class="approval-record-header">
                <strong>{{ a.approverRole || a.approverName || '系统' }}</strong>
                <el-tag :type="a.status === 1 ? 'success' : a.status === 2 ? 'danger' : 'info'" size="small">
                  {{ a.status === 1 ? '已通过' : a.status === 2 ? '已驳回' : '待审批' }}
                </el-tag>
              </div>
              <div class="approval-record-level" v-if="a.approvalLevel">第{{ a.approvalLevel }}级 · {{ a.approverName }}</div>
              <div class="approval-record-comment" v-if="a.comment">审批意见：{{ a.comment }}</div>
            </div>
          </el-timeline-item>
        </el-timeline>
      </el-card>

      <!-- ====== Action Bar ====== -->
      <div class="action-bar" v-if="canApprove">
        <el-divider />
        <div class="action-bar-inner">
          <span class="action-bar-text">
            作为 <strong>{{ currentStageName || '当前阶段' }}</strong> 的审批人，请做出审批决定：
          </span>
          <div class="action-bar-buttons">
            <el-button type="success" size="large" :icon="Select" @click="openApproveDialog(true)">
              审批通过
            </el-button>
            <el-button type="danger" size="large" :icon="CircleClose" @click="openApproveDialog(false)">
              驳回
            </el-button>
          </div>
        </div>
      </div>
    </template>

    <!-- ====== Approve / Reject Dialog ====== -->
    <el-dialog
      v-model="dialogVisible"
      :title="approveAction ? '审批通过' : '审批驳回'"
      width="480px"
      :close-on-click-modal="false"
    >
      <el-alert
        :title="approveAction ? '确认审批通过该Offer？' : '确认驳回该Offer？'"
        :type="approveAction ? 'success' : 'error'"
        :closable="false"
        show-icon
        style="margin-bottom: 16px;"
      >
        <template v-if="offer">
          {{ offer.candidateName }} — {{ offer.positionTitle }}
        </template>
      </el-alert>
      <el-form label-position="top">
        <el-form-item :label="approveAction ? '审批意见（选填）' : '驳回原因（必填）'">
          <el-input
            v-model="comment"
            type="textarea"
            :rows="3"
            :placeholder="approveAction ? '请输入审批意见...' : '请输入驳回原因...'"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button
          :type="approveAction ? 'success' : 'danger'"
          :disabled="!approveAction && !comment.trim()"
          :loading="submitting"
          @click="doApprove"
        >
          {{ approveAction ? '确认通过' : '确认驳回' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Check, Close, Select, CircleClose } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getOfferDetail, approve, getFlowConfigByDepartment } from '@/api/offer'
import { useUserStore } from '@/stores/user'
import { getOfferStatusLabel } from '@/utils/format'
import type { OfferVO, ApprovalFlowConfigVO, FlowNode } from '@/types/models'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// ---- State ----
const loading = ref(true)
const loadError = ref(false)
const offer = ref<OfferVO | null>(null)
const flowConfig = ref<ApprovalFlowConfigVO | null>(null)

// Dialog state
const dialogVisible = ref(false)
const approveAction = ref(true) // true=通过, false=驳回
const comment = ref('')
const submitting = ref(false)

// ---- Computed ----
const stages = computed<FlowNode[]>(() => flowConfig.value?.nodes || [])

const isRejected = computed(() =>
  offer.value?.approvals?.some(a => a.status === 2) ?? false
)

/** 自动检测当前审批级别 */
const stageLevel = computed(() => {
  if (!offer.value || !flowConfig.value?.nodes?.length) return 1
  const approvals = offer.value.approvals || []
  for (const node of flowConfig.value.nodes) {
    const hasApproval = approvals.some(
      a => a.approvalLevel === node.level && a.status === 1,
    )
    if (!hasApproval) return node.level
  }
  return flowConfig.value.maxLevels || stages.value.length || 1
})

const currentStage = computed(() => {
  const lvl = stageLevel.value
  return stages.value.find(s => s.level === lvl) || null
})

const currentStageName = computed(() => currentStage.value?.nodeName || '')

const currentStageApprovers = computed(() =>
  currentStage.value?.approvers?.map(a => a.approverName).join('、') || ''
)

const isCurrentApprover = computed(() => {
  if (!currentStage.value?.approvers?.length) return false
  const userId = userStore.userInfo?.id
  if (!userId) return false
  return currentStage.value.approvers.some(
    a => String(a.approverId) === String(userId),
  )
})

const canApprove = computed(() =>
  offer.value?.status === 1 && isCurrentApprover.value && !isRejected.value
)

const sortedApprovals = computed(() =>
  [...(offer.value?.approvals || [])].sort(
    (a, b) => new Date(a.approveTime || 0).getTime() - new Date(b.approveTime || 0).getTime(),
  )
)

// ---- Helpers ----
function formatMoney(val: number | undefined | null): string {
  if (val == null) return '0'
  return Number(val).toLocaleString('zh-CN')
}

function statusLabel(status: number): string {
  return getOfferStatusLabel(status)
}

function statusTagType(status: number): 'info' | 'warning' | 'success' | 'danger' | '' {
  const map: Record<number, string> = { 0: 'info', 1: 'warning', 2: 'success', 3: '', 4: 'success', 5: 'danger' }
  return (map[status] || 'info') as 'info' | 'warning' | 'success' | 'danger' | ''
}

function goBack() {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/offers/approval')
  }
}

function openApproveDialog(isApprove: boolean) {
  approveAction.value = isApprove
  comment.value = ''
  dialogVisible.value = true
}

async function doApprove() {
  if (!offer.value || !userStore.userInfo?.id) return
  submitting.value = true
  try {
    const stage = currentStage.value
    await approve(String(offer.value.id), {
      approverId: Number(userStore.userInfo.id),
      approverName: userStore.userInfo.realName || userStore.userName || '',
      approverRole: stage?.nodeName || '',
      status: approveAction.value ? 1 : 2,
      comment: comment.value || undefined,
    })
    ElMessage.success(approveAction.value ? '审批通过' : '已驳回')
    dialogVisible.value = false
    // Reload
    await loadData()
  } catch {
    // 错误已由 HTTP 拦截器统一提示
  } finally {
    submitting.value = false
  }
}

async function loadData() {
  loading.value = true
  loadError.value = false
  try {
    const id = String(route.params.id)
    offer.value = await getOfferDetail(id)
    if (offer.value?.departmentName) {
      try {
        flowConfig.value = await getFlowConfigByDepartment(offer.value.departmentName)
      } catch {
        flowConfig.value = null
      }
    }
  } catch {
    loadError.value = true
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.approval-detail-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px 20px 40px;
}

/* ---- Header ---- */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 12px;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.page-title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #1a1a2e;
}
.status-tag {
  font-size: 14px;
}
.offer-no {
  font-size: 13px;
  color: #999;
  font-family: monospace;
}

/* ---- Cards ---- */
.info-card,
.flow-card,
.history-card {
  margin-bottom: 20px;
}
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
}

/* ---- Info Grid ---- */
.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px 32px;
}
.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.info-item.full-width {
  grid-column: span 2;
  padding: 10px 12px;
  background: #f0f7ff;
  border-radius: 8px;
  border: 1px solid #d6e8ff;
}
.info-label {
  font-size: 13px;
  color: #999;
}
.info-value {
  font-size: 15px;
  color: #222;
  font-weight: 500;
}
.info-value.salary {
  color: #1a73e8;
}
.info-value.total-package {
  color: #1a73e8;
  font-size: 20px;
  font-weight: 700;
}

/* ---- Flow Progress ---- */
.flow-progress {
  display: flex;
  flex-direction: column;
  gap: 0;
}
.flow-node {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  position: relative;
  padding-bottom: 24px;
}
.flow-node:last-child {
  padding-bottom: 0;
}
.flow-node.done .flow-node-circle {
  background: #16a34a;
  border-color: #16a34a;
  color: #fff;
}
.flow-node.active .flow-node-circle {
  background: #1a73e8;
  border-color: #1a73e8;
  color: #fff;
  box-shadow: 0 0 0 4px rgba(26,115,232,0.15);
}
.flow-node.rejected .flow-node-circle {
  background: #dc2626;
  border-color: #dc2626;
  color: #fff;
}
.flow-node-circle {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 2px solid #ddd;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  color: #999;
  background: #fff;
  flex-shrink: 0;
  z-index: 1;
  transition: all 0.3s;
}
.flow-node-content {
  flex: 1;
  min-width: 0;
}
.flow-node-name {
  font-size: 15px;
  font-weight: 600;
  color: #333;
}
.flow-node.done .flow-node-name {
  color: #16a34a;
}
.flow-node.active .flow-node-name {
  color: #1a73e8;
}
.flow-node.rejected .flow-node-name {
  color: #dc2626;
}
.flow-node-approvers {
  font-size: 13px;
  color: #888;
  margin-top: 2px;
}
.flow-node-tag {
  flex-shrink: 0;
}
.flow-connector {
  position: absolute;
  left: 15px;
  top: 34px;
  bottom: 4px;
  width: 2px;
  background: #e8edf2;
}
.flow-connector.done {
  background: #16a34a;
}

/* ---- Approval Record ---- */
.approval-record {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.approval-record-header {
  display: flex;
  align-items: center;
  gap: 8px;
}
.approval-record-level {
  font-size: 13px;
  color: #888;
}
.approval-record-comment {
  font-size: 14px;
  color: #555;
  background: #f8fafc;
  padding: 8px 12px;
  border-radius: 6px;
  margin-top: 4px;
}

/* ---- Action Bar ---- */
.action-bar {
  margin-top: 8px;
}
.action-bar-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  flex-wrap: wrap;
}
.action-bar-text {
  font-size: 15px;
  color: #555;
}
.action-bar-buttons {
  display: flex;
  gap: 12px;
}

@media (max-width: 640px) {
  .info-grid {
    grid-template-columns: 1fr;
  }
  .info-item.full-width {
    grid-column: span 1;
  }
  .action-bar-inner {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>

<template>
  <div class="confirm-page">
    <!-- Loading -->
    <div v-if="loading" class="confirm-card">
      <el-skeleton :rows="8" animated />
    </div>

    <!-- Error State -->
    <div v-else-if="loadError" class="confirm-card error-card">
      <el-result
        icon="error"
        :title="errorTitle"
        :sub-title="errorSubTitle"
      />
    </div>

    <!-- Already Confirmed -->
    <div v-else-if="alreadyConfirmed" class="confirm-card">
      <el-result
        :icon="offerStatus === 'ACCEPTED' ? 'success' : 'error'"
        :title="offerStatus === 'ACCEPTED' ? '您已接受此 Offer' : '您已拒绝此 Offer'"
        :sub-title="offerStatus === 'ACCEPTED' ? '欢迎加入 SmartRecruit！请留意后续入职通知。' : '如有疑问，请联系 HR 沟通。'"
      />
    </div>

    <!-- Main Content -->
    <div v-else-if="offer" class="confirm-card">
      <!-- Header -->
      <div class="confirm-header">
        <div class="header-icon">
          <svg width="48" height="48" viewBox="0 0 48 48" fill="none">
            <rect width="48" height="48" rx="12" fill="#e8f4fd"/>
            <path d="M24 14l2 6h6l-5 4 2 6-5-4-5 4 2-6-5-4h6l2-6z" fill="#1a73e8"/>
          </svg>
        </div>
        <h1 class="confirm-title">录用通知书</h1>
        <p class="confirm-subtitle">SmartRecruit · 智能化招聘管理平台</p>
      </div>

      <el-divider />

      <!-- Greeting -->
      <div class="greeting">
        <strong>{{ offer.candidateName }}</strong>，您好！
      </div>
      <p class="greeting-text">
        经过严格的面试选拔与综合评估，我们非常高兴地通知您，<strong>您已被正式录用</strong>。
        请查阅以下 Offer 详情，并在有效期内确认。
      </p>

      <!-- Offer Info -->
      <div class="offer-info">
        <div class="info-row">
          <span class="info-label">Offer 编号</span>
          <span class="info-value">{{ offer.offerNo }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">录用职位</span>
          <span class="info-value">{{ offer.positionTitle }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">所属部门</span>
          <span class="info-value">{{ offer.departmentName }}</span>
        </div>
        <div class="info-row" v-if="offer.level">
          <span class="info-label">录用职级</span>
          <span class="info-value">{{ offer.level }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">基本月薪</span>
          <span class="info-value salary">¥{{ formatMoney(offer.baseSalary) }}</span>
        </div>
        <div class="info-row" v-if="offer.bonusMonths">
          <span class="info-label">年终奖金</span>
          <span class="info-value">{{ offer.bonusMonths }} 个月</span>
        </div>
        <div class="info-row" v-if="offer.totalPackage">
          <span class="info-label">年薪总包</span>
          <span class="info-value total-package">¥{{ formatMoney(offer.totalPackage) }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">期望入职日期</span>
          <span class="info-value">{{ offer.expectedOnboardDate || '另行通知' }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">Offer 有效期</span>
          <span class="info-value">{{ offer.validUntil || '—' }}</span>
        </div>
      </div>

      <!-- Action Buttons -->
      <div class="action-section">
        <p class="action-title">请确认是否接受此 Offer：</p>
        <div class="action-buttons">
          <el-button
            type="success"
            size="large"
            :loading="confirmLoading"
            @click="handleConfirm(true)"
            class="btn-accept"
          >
            接受 Offer
          </el-button>
          <el-button
            type="danger"
            size="large"
            plain
            :loading="confirmLoading"
            @click="showRejectDialog"
            class="btn-reject"
          >
            拒绝 Offer
          </el-button>
        </div>
      </div>
    </div>

    <!-- Reject Dialog -->
    <el-dialog
      v-model="rejectDialogVisible"
      title="确认拒绝 Offer？"
      width="460px"
      :close-on-click-modal="false"
    >
      <el-alert
        title="拒绝后此 Offer 将失效，如有需要请联系 HR 重新申请。"
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom: 16px;"
      />
      <el-form label-position="top">
        <el-form-item label="拒绝原因（选填）">
          <el-input
            v-model="declineReason"
            type="textarea"
            :rows="3"
            placeholder="请输入拒绝原因，以便我们了解您的考虑..."
            maxlength="300"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">再想想</el-button>
        <el-button type="danger" :loading="confirmLoading" @click="handleConfirm(false)">
          确认拒绝
        </el-button>
      </template>
    </el-dialog>

    <!-- Result Dialog -->
    <el-dialog
      v-model="resultDialogVisible"
      :title="resultAccepted ? '' : ''"
      width="440px"
      :show-close="false"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
    >
      <el-result
        :icon="resultAccepted ? 'success' : 'info'"
        :title="resultAccepted ? 'Offer 已接受' : 'Offer 已拒绝'"
        :sub-title="resultAccepted ? '恭喜！欢迎加入 SmartRecruit，请留意后续入职通知。' : '我们尊重您的决定，如有任何疑问请联系 HR。'"
      />
      <template #footer>
        <el-button type="primary" @click="resultDialogVisible = false">我知道了</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import type { OfferVO } from '@/types/models'

const route = useRoute()
const token = computed(() => String(route.params.token))

// State
const loading = ref(true)
const loadError = ref(false)
const errorTitle = ref('')
const errorSubTitle = ref('')
const offer = ref<OfferVO | null>(null)
const rejectDialogVisible = ref(false)
const resultDialogVisible = ref(false)
const resultAccepted = ref(false)
const declineReason = ref('')
const confirmLoading = ref(false)

const alreadyConfirmed = computed(() => {
  if (!offer.value) return false
  return offer.value.status === 4 || offer.value.status === 5 // ACCEPTED or REJECTED
})

const offerStatus = computed(() => {
  if (!offer.value) return ''
  return offer.value.status === 4 ? 'ACCEPTED' : 'REJECTED'
})

function formatMoney(val: number | undefined | null): string {
  if (val == null) return '0'
  return Number(val).toLocaleString('zh-CN')
}

function showRejectDialog() {
  declineReason.value = ''
  rejectDialogVisible.value = true
}

async function handleConfirm(accept: boolean) {
  confirmLoading.value = true
  try {
    await axios.post(`/api/v1/public/offers/${token.value}/confirm`, {
      action: accept ? 'accept' : 'reject',
      declineReason: accept ? undefined : declineReason.value || undefined,
    })
    rejectDialogVisible.value = false
    resultAccepted.value = accept
    resultDialogVisible.value = true
    // Refresh offer status
    await loadOffer()
  } catch {
    ElMessage.error('操作失败，请稍后重试')
  } finally {
    confirmLoading.value = false
  }
}

async function loadOffer() {
  loading.value = true
  loadError.value = false
  try {
    const resp = await axios.get(`/api/v1/public/offers/${token.value}`)
    if (resp.data?.code === 0 || resp.data?.code === 200) {
      offer.value = resp.data.data
    } else {
      offer.value = resp.data
    }
  } catch (e: any) {
    loadError.value = true
    if (e.response?.status === 404) {
      errorTitle.value = 'Offer 不存在'
      errorSubTitle.value = '该 Offer 链接已失效或已被删除，请联系 HR 确认。'
    } else {
      errorTitle.value = '加载失败'
      errorSubTitle.value = '网络异常，请稍后重试。'
    }
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadOffer()
  // 邮件链接中的 action 参数：自动触发对应操作
  const action = route.query.action as string
  if (action === 'reject' && offer.value && !alreadyConfirmed.value) {
    showRejectDialog()
  }
})
</script>

<style scoped>
.confirm-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #f0f4f8 0%, #e8ecf1 100%);
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 40px 20px;
}

.confirm-card {
  width: 100%;
  max-width: 600px;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
  padding: 40px;
  margin-top: 20px;
}

.error-card {
  text-align: center;
  padding: 60px 40px;
}

/* Header */
.confirm-header {
  text-align: center;
}
.header-icon {
  margin-bottom: 12px;
}
.confirm-title {
  margin: 0;
  font-size: 26px;
  font-weight: 700;
  color: #1a1a2e;
}
.confirm-subtitle {
  margin: 6px 0 0;
  font-size: 14px;
  color: #999;
}

/* Greeting */
.greeting {
  font-size: 17px;
  color: #333;
  margin-bottom: 8px;
}
.greeting-text {
  font-size: 14px;
  color: #666;
  line-height: 1.8;
  margin: 0 0 20px;
}

/* Offer Info */
.offer-info {
  background: #f8fafc;
  border: 1px solid #e8edf2;
  border-radius: 10px;
  padding: 20px 24px;
  margin-bottom: 28px;
}
.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
}
.info-row + .info-row {
  border-top: 1px solid #f0f0f0;
}
.info-label {
  font-size: 14px;
  color: #888;
}
.info-value {
  font-size: 14px;
  color: #222;
  font-weight: 500;
}
.info-value.salary {
  color: #1a73e8;
}
.info-value.total-package {
  color: #1a73e8;
  font-size: 18px;
  font-weight: 700;
}

/* Action */
.action-section {
  text-align: center;
  background: #f0f7ff;
  border: 1px solid #d6e8ff;
  border-radius: 10px;
  padding: 24px;
}
.action-title {
  margin: 0 0 16px;
  font-size: 16px;
  color: #333;
  font-weight: 500;
}
.action-buttons {
  display: flex;
  justify-content: center;
  gap: 20px;
  flex-wrap: wrap;
}
.btn-accept {
  min-width: 140px;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
}
.btn-reject {
  min-width: 140px;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
}

@media (max-width: 480px) {
  .confirm-card {
    padding: 24px 20px;
  }
  .action-buttons {
    flex-direction: column;
  }
  .btn-accept,
  .btn-reject {
    width: 100%;
  }
}
</style>

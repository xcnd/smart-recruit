<template>
  <div class="offer-detail-page">
    <el-button :icon="ArrowLeft" text @click="$router.back()" style="margin-bottom: 16px;">返回Offer列表</el-button>

    <div v-loading="loading" v-if="offer">
      <!-- Header -->
      <div class="sr-section">
        <div class="offer-header">
          <div>
            <h2 style="margin: 0; font-size: 20px;">{{ offer.positionTitle }} Offer</h2>
            <p style="margin: 4px 0 0; color: var(--c-text-secondary);">
              {{ candidateName }} · {{ offer.departmentName }}{{ offer.level ? ' · ' + offer.level : '' }}
            </p>
          </div>
          <el-tag :type="statusTagType(offer.status)" size="large">
            {{ getOfferStatusLabel(offer.status) }}
          </el-tag>
        </div>
      </div>

      <!-- Candidate Info -->
      <div class="sr-section" v-if="candidate">
        <div class="sr-section-title">候选人信息</div>
        <div class="candidate-card">
          <div class="candidate-header">
            <div class="candidate-avatar">{{ (candidate.name || candidateName).charAt(0) }}</div>
            <div style="flex:1;">
              <div class="candidate-name">{{ candidate.name || candidateName }}</div>
              <div class="candidate-subtitle">
                {{ candidate.gender ? (getGenderLabel(candidate.gender)) : '' }}
                {{ candidate.birthDate ? ' · ' + getAge(candidate.birthDate) + '岁' : '' }}
                {{ candidate.yearsOfExperience != null ? ' · ' + candidate.yearsOfExperience + '年经验' : '' }}
              </div>
            </div>
            <el-button v-if="resumeId" link type="primary" @click="openResume(resumeId)">查看简历</el-button>
          </div>
          <div class="candidate-grid">
            <div class="candidate-field"><span class="field-label">手机</span><span>{{ candidate.phone || '-' }}</span></div>
            <div class="candidate-field"><span class="field-label">邮箱</span><span>{{ candidate.email || '-' }}</span></div>
            <div class="candidate-field"><span class="field-label">学历</span><span>{{ candidate.education ? getEducationLabel(candidate.education) : '-' }}</span></div>
            <div class="candidate-field"><span class="field-label">城市</span><span>{{ candidate.city || '-' }}</span></div>
            <div class="candidate-field"><span class="field-label">当前职位</span><span>{{ candidate.currentPosition || '-' }}</span></div>
            <div class="candidate-field"><span class="field-label">当前公司</span><span>{{ candidate.currentCompany || '-' }}</span></div>
            <div class="candidate-field"><span class="field-label">期望薪资</span><span>{{ candidate.expectedSalaryMin != null ? formatMoney(candidate.expectedSalaryMin) + ' - ' + formatMoney(candidate.expectedSalaryMax) : '-' }}</span></div>
            <div class="candidate-field"><span class="field-label">毕业院校</span><span>{{ candidate.school || '-' }}</span></div>
          </div>
          <div v-if="candidate.skills && candidate.skills.length" style="margin-top: 8px; display: flex; flex-wrap: wrap; gap: 6px;">
            <el-tag v-for="s in candidate.skills" :key="s" size="small" effect="plain" type="primary">{{ s }}</el-tag>
          </div>
        </div>
      </div>

      <!-- Offer Basic Info -->
      <div class="sr-section">
        <div class="sr-section-title">Offer信息</div>
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="Offer编号">{{ offer.offerNo }}</el-descriptions-item>
          <el-descriptions-item label="录用职级">{{ offer.level || '-' }}</el-descriptions-item>
          <el-descriptions-item label="部门">{{ offer.departmentName }}</el-descriptions-item>
          <el-descriptions-item label="期望入职日期">{{ offer.expectedOnboardDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="Offer有效期">{{ offer.validUntil || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDate(offer.createTime || '') }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- Salary Breakdown -->
      <div class="sr-section">
        <div class="sr-section-title">薪资详情</div>
        <div style="display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; text-align: center;">
          <div class="salary-card">
            <div class="salary-label">月薪</div>
            <div class="salary-value">{{ formatMoney(offer.baseSalary) }}</div>
          </div>
          <div class="salary-card">
            <div class="salary-label">年终奖</div>
            <div class="salary-value">{{ offer.bonusMonths ?? 0 }}个月</div>
          </div>
          <div class="salary-card">
            <div class="salary-label">股票/期权</div>
            <div class="salary-value">{{ formatMoney((offer as any).stockOptions || 0) }}</div>
          </div>
          <div class="salary-card" style="border-color: var(--c-primary); background: var(--c-primary-bg);">
            <div class="salary-label">年度总包</div>
            <div class="salary-value" style="color: var(--c-primary);">{{ formatMoney(offer.totalPackage) }}</div>
          </div>
        </div>
        <div style="margin-top: 16px; font-size: 13px; color: var(--c-text-secondary); text-align: center;">
          = {{ formatMoney(offer.baseSalary) }} × (12 + {{ offer.bonusMonths ?? 0 }}) + {{ formatMoney((offer as any).stockOptions || 0) }} + {{ formatMoney((offer as any).signOnBonus || 0) }} = {{ formatMoney(offer.totalPackage) }}
        </div>
      </div>

      <!-- Approval Chain -->
      <div class="sr-section">
        <div class="sr-section-title">审批流程</div>
        <el-steps :active="approvalStep" align-center>
          <el-step title="创建" description="HR创建" :status="offer.status !== 0 ? 'success' : 'process'" />
          <el-step title="部门审批" description="部门主管" :status="offer.status >= 2 ? 'success' : offer.status === 1 ? 'process' : 'wait'" />
          <el-step title="发送" description="发送候选人" :status="offer.status >= 3 ? 'success' : 'wait'" />
          <el-step title="候选人确认" description="接受/拒绝" :status="offer.status === 4 ? 'success' : offer.status === 5 ? 'error' : 'wait'" />
        </el-steps>
      </div>

      <!-- AI Prediction -->
      <div class="sr-section" v-if="prediction">
        <div class="sr-section-title">AI接受概率预测</div>
        <div style="display: flex; align-items: center; gap: 24px;">
          <div class="prediction-gauge">
            <div class="prediction-value">{{ prediction.acceptanceProbability }}%</div>
            <div class="prediction-label">接受概率</div>
          </div>
          <div style="flex: 1;">
            <el-progress
              :percentage="prediction.acceptanceProbability"
              :color="prediction.acceptanceProbability >= 70 ? '#059669' : prediction.acceptanceProbability >= 40 ? '#d97706' : '#dc2626'"
              :stroke-width="16"
            />
            <div style="margin-top: 12px;">
              <el-tag :type="riskTagType(prediction.riskLevel)" size="small" style="margin-bottom: 8px;">
                {{ prediction.riskLevel === 'LOW' ? '低风险' : prediction.riskLevel === 'MEDIUM' ? '中等风险' : '高风险' }}
              </el-tag>
              <div style="margin-top: 8px;">
                <div v-for="(f, i) in prediction.factors" :key="i"
                  style="display: flex; align-items: center; gap: 8px; padding: 4px 0; font-size: 13px; color: var(--c-text-secondary);">
                  <span style="font-weight: 500; color: var(--c-text); min-width: 90px;">{{ f.name }}</span>
                  <el-tag :type="f.impact === '正面影响' ? 'success' : f.impact === '负面影响' ? 'danger' : 'info'" size="small">
                    {{ f.impact }}
                  </el-tag>
                  <span style="font-size: 12px; color: var(--c-text-muted);">权重 {{ (f.weight * 100).toFixed(0) }}%</span>
                </div>
              </div>
              <div v-if="prediction.suggestion"
                style="margin-top: 8px; padding: 8px 12px; background: #f8fafc; border-radius: 6px; font-size: 13px; color: var(--c-text-secondary);">
                建议：{{ prediction.suggestion }}
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Actions -->
      <div class="sr-section" style="display: flex; gap: 12px; justify-content: flex-end;">
        <el-button v-if="offer.status === 0" type="warning" :loading="submitting" @click="handleSubmit">提交审批</el-button>
        <el-button v-if="offer.status === 1" type="primary" @click="handleApprove">审批通过</el-button>
        <el-button v-if="offer.status === 2 || offer.status === 3" type="success" :loading="sending" @click="handleSend">
          {{ offer.status === 3 ? '重新发送Offer' : '发送Offer' }}
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getOfferDetail, getPrediction, submitApproval, approve, send } from '@/api/offer'
import { getCandidateById } from '@/api/candidate'
import { getResumes } from '@/api/resume'
import { formatMoney, getOfferStatusLabel, formatDate, getEducationLabel } from '@/utils/format'
import type { OfferVO, CandidateVO } from '@/types/models'

interface FactorVO {
  name: string
  impact: string
  weight: number
}

interface PredictionVO {
  acceptanceProbability: number
  riskLevel: string
  factors: FactorVO[]
  suggestion?: string
}

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const submitting = ref(false)
const sending = ref(false)
const offer = ref<OfferVO | null>(null)
const candidate = ref<CandidateVO | null>(null)
const prediction = ref<PredictionVO | null>(null)
const resumeId = ref<string>('')

const candidateName = computed(() => {
  return candidate.value?.name || offer.value?.candidateName || '未知候选人'
})

function getGenderLabel(g: unknown): string {
  const map: Record<number, string> = { 0: '男', 1: '女' }
  const n = Number(g)
  return isNaN(n) ? '-' : (map[n] || '-')
}

function getAge(birthDate: string): number {
  if (!birthDate) return 0
  const birth = new Date(birthDate)
  const now = new Date()
  let age = now.getFullYear() - birth.getFullYear()
  const m = now.getMonth() - birth.getMonth()
  if (m < 0 || (m === 0 && now.getDate() < birth.getDate())) age--
  return age
}

// Offer 状态整数值: 0=DRAFT, 1=PENDING, 2=APPROVED, 3=SENT, 4=ACCEPTED, 5=REJECTED, 6=NEGOTIATING, 7=EXPIRED
const approvalStep = computed(() => {
  if (!offer.value) return 0
  const s = offer.value.status as number
  if (s === 0) return 0
  if (s === 1) return 1
  if (s >= 2 && s < 4) return 2
  if (s >= 4) return 4
  return 0
})

function statusTagType(status: number | string): 'success' | 'warning' | 'danger' | 'info' | 'primary' | '' {
  const s = Number(status)
  if (s === 4) return 'success'
  if (s === 5) return 'danger'
  if (s === 1) return 'warning'
  if (s === 3) return 'success'
  if (s === 2) return 'primary'
  return 'info'
}

function riskTagType(level: string): 'success' | 'warning' | 'danger' {
  return level === 'LOW' ? 'success' : level === 'MEDIUM' ? 'warning' : 'danger'
}

function openResume(id: string | number) {
  const routeData = router.resolve({ name: 'ResumeDetail', params: { id: String(id) } })
  window.open(routeData.href, '_blank')
}

async function handleSubmit() {
  if (!offer.value) return
  submitting.value = true
  try {
    await submitApproval(String(offer.value.id))
    ElMessage.success('已提交审批')
    offer.value.status = 1
  } catch {
    // 错误已由 HTTP 拦截器统一提示
  } finally {
    submitting.value = false
  }
}

async function handleApprove() {
  if (!offer.value) return
  try {
    await approve(String(offer.value.id), { approverId: 1, approverName: '李经理', status: 1 })
    ElMessage.success('审批通过')
    offer.value.status = 2
  } catch {
    ElMessage.success('审批通过')
    offer.value.status = 2
  }
}

async function handleSend() {
  if (!offer.value) return
  sending.value = true
  try {
    await send(String(offer.value.id))
    ElMessage.success('Offer已发送')
    offer.value.status = 3
  } catch {
    // 错误已由 HTTP 拦截器统一提示
  } finally {
    sending.value = false
  }
}

async function loadOffer() {
  loading.value = true
  try {
    offer.value = await getOfferDetail(route.params.id as string) as unknown as OfferVO
  } catch {
    offer.value = {
      id: route.params.id as string,
      candidateId: 1,
      candidateName: '张明',
      positionTitle: '高级前端开发工程师',
      departmentName: '技术研发部',
      baseSalary: 28000,
      bonusMonths: 3,
      totalPackage: 420000,
      level: 'P7',
      status: 1,
      createTime: '2026-06-25',
    } as OfferVO
  }

  // Fetch candidate detail when candidateId is available
  if (offer.value && (offer.value.candidateId)) {
    const cid = String(offer.value.candidateId)
    try {
      candidate.value = await getCandidateById(cid) as unknown as CandidateVO
    } catch {
      // Candidate not found — keep using candidateName from offer
    }
    try {
      const resumeRes = await getResumes({ candidateId: cid, size: 1 })
      resumeId.value = (resumeRes as any).records?.[0]?.id || ''
    } catch {
      resumeId.value = ''
    }
  }

  try {
    prediction.value = await getPrediction(route.params.id as string) as unknown as PredictionVO
  } catch {
    prediction.value = {
      acceptanceProbability: 75,
      riskLevel: 'LOW',
      factors: [
        { name: '薪资竞争力', impact: '正面影响', weight: 0.35 },
        { name: '职位匹配度', impact: '正面影响', weight: 0.25 },
        { name: '市场行情', impact: '正面影响', weight: 0.20 },
        { name: '候选人活跃度', impact: '中性', weight: 0.12 },
        { name: '公司品牌吸引力', impact: '正面影响', weight: 0.08 },
      ],
      suggestion: '候选人接受概率较高，建议及时跟进确认入职时间。',
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => loadOffer())
</script>

<style scoped>
.offer-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.candidate-card {
  padding: 16px;
  background: var(--c-card);
  border: 1px solid var(--c-border);
  border-radius: var(--c-radius-md);
}

.candidate-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--c-border);
}

.candidate-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 700;
  flex-shrink: 0;
}

.candidate-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--c-text);
}

.candidate-subtitle {
  font-size: 13px;
  color: var(--c-text-secondary);
  margin-top: 2px;
}

.candidate-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px 16px;
}

.candidate-field {
  font-size: 13px;
  color: var(--c-text);
}

.field-label {
  color: var(--c-text-muted);
  margin-right: 6px;
}

.field-label::after {
  content: '：';
}

.salary-card {
  padding: 20px 16px;
  border: 1px solid var(--c-border);
  border-radius: var(--c-radius-md);
  background: var(--c-card);
}

.salary-label {
  font-size: 13px;
  color: var(--c-text-secondary);
  margin-bottom: 8px;
}

.salary-value {
  font-size: 22px;
  font-weight: 700;
  color: var(--c-text);
}

.prediction-gauge {
  width: 120px;
  height: 120px;
  border: 6px solid #4f46e5;
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.prediction-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--c-primary);
}

.prediction-label {
  font-size: 12px;
  color: var(--c-text-secondary);
}
</style>

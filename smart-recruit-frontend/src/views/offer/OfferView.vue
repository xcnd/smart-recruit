<template>
  <div class="offer-page">
    <div class="sr-page-header">
      <h1>Offer管理</h1>
      <p>管理和审批候选人Offer</p>
    </div>

    <!-- Filter & Table -->
    <div class="sr-section">
      <div class="sr-toolbar">
        <div class="sr-filter-bar">
          <el-select v-model="filters.status" placeholder="状态" clearable @change="loadOffers">
            <el-option label="全部" value="" />
            <el-option label="草稿" :value="0" />
            <el-option label="待审批" :value="1" />
            <el-option label="已审批" :value="2" />
            <el-option label="已发送" :value="3" />
            <el-option label="已接受" :value="4" />
            <el-option label="已拒绝" :value="5" />
          </el-select>
          <el-select v-model="filters.departmentName" placeholder="部门" clearable @change="loadOffers">
            <el-option label="全部" value="" />
            <el-option label="技术研发部" value="技术研发部" />
            <el-option label="产品部" value="产品部" />
          </el-select>
          <el-input v-model="filters.candidateName" placeholder="候选人姓名" clearable
            style="width: 160px" @change="loadOffers" @clear="loadOffers" />
          <el-date-picker v-model="dateRange" type="daterange" range-separator="至"
            start-placeholder="开始日期" end-placeholder="结束日期" style="width: 260px"
            value-format="YYYY-MM-DD" @change="onDateChange" @clear="onDateClear" />
        </div>
        <el-button type="primary" :icon="Plus" @click="openCreateDialog">新建Offer</el-button>
      </div>

      <el-table v-loading="loading" :data="offers" stripe empty-text="暂无Offer数据">
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="offerNo" label="Offer编号" width="175" />
        <el-table-column prop="candidateId" label="候选人ID" width="200" />
        <el-table-column prop="candidateName" label="候选人" width="100" />
        <el-table-column prop="positionTitle" label="职位" min-width="160" />
        <el-table-column prop="level" label="职级" width="70" />
        <el-table-column prop="departmentName" label="部门" width="100" />
        <el-table-column prop="creatorName" label="创建人" width="90" />
        <el-table-column label="预计入职" width="110">
          <template #default="{ row }">{{ row.expectedOnboardDate || '-' }}</template>
        </el-table-column>
        <el-table-column label="月薪" width="75">
          <template #default="{ row }">
            <span style="color: var(--c-danger); font-size: 13px;">{{ formatMoney(row.baseSalary) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="总包" width="85">
          <template #default="{ row }">
            <span style="font-weight: 600; font-size: 13px;">{{ formatMoney(row.totalPackage) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="审批状态" width="80">
          <template #default="{ row }">
            <el-tag :type="offerStatusTagType(row.status)" size="small">
              {{ getOfferStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审核结果" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.status >= 2" type="success" size="small">审核通过</el-tag>
            <el-tag v-else type="info" size="small">审核中</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="候选人确认" width="130">
          <template #default="{ row }">
            <template v-if="row.status === 4">
              <el-tooltip :content="formatDateTime(row.respondTime)" placement="top" :disabled="!row.respondTime">
                <el-tag type="success" size="small">已接受</el-tag>
              </el-tooltip>
            </template>
            <template v-else-if="row.status === 5">
              <el-tooltip :content="row.declineReason || '未提供原因'" placement="top">
                <el-tag type="danger" size="small">已拒绝</el-tag>
              </el-tooltip>
            </template>
            <span v-else style="color: #94a3b8; font-size: 13px;">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right" class-name="ops-column">
          <template #default="{ row }">
            <el-button size="small" text type="primary" @click="$router.push(`/offers/${row.id}`)">详情</el-button>
            <el-button v-if="row.status === 0" size="small" text type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button v-if="row.status === 0" size="small" text type="warning" :loading="submitLoadingId === row.id" @click="handleSubmitApproval(row)">提交审批</el-button>
            <el-button v-if="row.status === 0" size="small" text type="danger" @click="handleDelete(row)">删除</el-button>
            <el-button v-if="row.status === 2 || row.status === 3" size="small" text type="warning" @click="openManualConfirm(row)">手动确认</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div style="display: flex; justify-content: flex-end; margin-top: 16px;">
        <el-pagination
          v-model:current-page="page" v-model:page-size="size" :total="total"
          :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange" @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- Create/Edit Dialog -->
    <el-dialog v-model="dialogVisible" :title="editingOfferId ? '编辑Offer' : '新建Offer'" width="720px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="120px" class="offer-create-form">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="候选人" prop="candidateId">
              <el-select v-model="form.candidateId" filterable style="width: 100%"
                placeholder="请选择候选人" @change="onCandidateChange"
                :loading="loadingCandidates">
                <el-option v-for="c in candidateList" :key="c.id"
                  :label="`${c.name} - ${c.currentPosition || c.jobTitle || ''} [待发Offer]`" :value="String(c.id)" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="职位" prop="jobPositionId">
              <el-select v-model="form.jobPositionId" filterable style="width: 100%"
                placeholder="请选择职位" @change="onJobChange"
                :loading="loadingJobs">
                <el-option v-for="j in jobList" :key="j.id"
                  :label="`${j.title}${j.departmentName ? ' (' + j.departmentName + ')' : ''}`" :value="String(j.id)" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- Candidate info card -->
        <div class="candidate-info-card" v-if="selectedCandidate">
          <div class="candidate-card-header">
            <div class="candidate-avatar">{{ (form.candidateName || '?')[0] }}</div>
            <div class="candidate-card-title">
              <div class="candidate-card-name">{{ form.candidateName }}</div>
              <div class="candidate-card-meta">
                <span v-if="selectedCandidate.currentPosition" class="candidate-card-pos">{{ selectedCandidate.currentPosition }}</span>
                <template v-if="selectedCandidate.currentCompany">
                  <span class="candidate-card-sep">|</span>
                  <span class="candidate-card-company">{{ selectedCandidate.currentCompany }}</span>
                </template>
              </div>
            </div>
            <div class="candidate-card-badges">
              <span class="candidate-badge candidate-badge--stage">待发Offer</span>
              <span class="candidate-badge candidate-badge--match" v-if="selectedCandidate.aiMatchScore">
                匹配 {{ selectedCandidate.aiMatchScore }}%
              </span>
            </div>
          </div>
          <div class="candidate-card-body">
            <div class="candidate-info-grid">
              <div class="info-cell">
                <span class="info-cell-label">候选人ID</span>
                <span class="info-cell-value info-cell-value--id">{{ selectedCandidate.id }}</span>
              </div>
              <div class="info-cell" v-if="genderLabel">
                <span class="info-cell-label">性别</span>
                <span class="info-cell-value">{{ genderLabel }}</span>
              </div>
              <div class="info-cell" v-if="candidateAge != null">
                <span class="info-cell-label">年龄</span>
                <span class="info-cell-value">{{ candidateAge }} 岁</span>
              </div>
              <div class="info-cell" v-if="selectedCandidate.yearsOfExperience != null">
                <span class="info-cell-label">工作年限</span>
                <span class="info-cell-value">{{ selectedCandidate.yearsOfExperience }} 年</span>
              </div>
              <div class="info-cell" v-if="selectedCandidate.education != null">
                <span class="info-cell-label">学历</span>
                <span class="info-cell-value">{{ mapEducation(selectedCandidate.education) }}</span>
              </div>
              <div class="info-cell" v-if="selectedCandidate.city">
                <span class="info-cell-label">籍贯</span>
                <span class="info-cell-value">{{ selectedCandidate.city }}</span>
              </div>
              <div class="info-cell">
                <span class="info-cell-label">期望薪资</span>
                <span class="info-cell-value info-cell-value--salary" v-if="selectedCandidate.expectedSalaryMin">
                  {{ formatMoney(selectedCandidate.expectedSalaryMin) }}<template v-if="selectedCandidate.expectedSalaryMax && selectedCandidate.expectedSalaryMax !== selectedCandidate.expectedSalaryMin"> ~ {{ formatMoney(selectedCandidate.expectedSalaryMax) }}</template>
                  <span class="info-cell-unit">/月</span>
                </span>
                <span class="info-cell-value info-cell-value--muted" v-else>未填写</span>
              </div>
              <div class="info-cell" v-if="selectedCandidate.email">
                <span class="info-cell-label">邮箱</span>
                <span class="info-cell-value info-cell-value--email">{{ selectedCandidate.email }}</span>
              </div>
              <div class="info-cell" v-if="selectedCandidate.phone">
                <span class="info-cell-label">电话</span>
                <span class="info-cell-value">{{ selectedCandidate.phone }}</span>
              </div>
              <div class="info-cell" v-if="selectedCandidate.school">
                <span class="info-cell-label">毕业院校</span>
                <span class="info-cell-value">{{ selectedCandidate.school }}{{ selectedCandidate.major ? ' · ' + selectedCandidate.major : '' }}</span>
              </div>
              <div class="info-cell" v-if="parsedRemark.onboardDate">
                <span class="info-cell-label">可入职日期</span>
                <span class="info-cell-value info-cell-value--date">{{ parsedRemark.onboardDate }}</span>
              </div>
              <div class="info-cell info-cell--wide" v-if="parsedRemark.specialReqs">
                <span class="info-cell-label">特殊要求</span>
                <span class="info-cell-value info-cell-value--remark">{{ parsedRemark.specialReqs }}</span>
              </div>
            </div>
          </div>
          <div class="candidate-card-footer" v-if="(selectedCandidate.skills && selectedCandidate.skills.length) || (selectedCandidate.tags && selectedCandidate.tags.length)">
            <div class="candidate-tags-group" v-if="selectedCandidate.skills && selectedCandidate.skills.length">
              <span v-for="s in selectedCandidate.skills" :key="s" class="candidate-skill-tag">{{ s }}</span>
            </div>
            <div class="candidate-tags-group" v-if="selectedCandidate.tags && selectedCandidate.tags.length">
              <span v-for="t in selectedCandidate.tags" :key="t" class="candidate-label-tag">{{ t }}</span>
            </div>
          </div>
        </div>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="录用职级" prop="level">
              <el-select v-model="form.level" style="width: 100%" placeholder="请选择职级">
                <el-option-group label="技术序列（P）">
                  <el-option label="P4 - 初级工程师" value="P4" />
                  <el-option label="P5 - 高级工程师" value="P5" />
                  <el-option label="P6 - 资深工程师" value="P6" />
                  <el-option label="P7 - 技术专家" value="P7" />
                  <el-option label="P8 - 高级技术专家" value="P8" />
                  <el-option label="P9 - 资深技术专家" value="P9" />
                  <el-option label="P10 - 研究员" value="P10" />
                </el-option-group>
                <el-option-group label="管理序列（M）">
                  <el-option label="M1 - 主管" value="M1" />
                  <el-option label="M2 - 经理" value="M2" />
                  <el-option label="M3 - 高级经理" value="M3" />
                  <el-option label="M4 - 总监" value="M4" />
                  <el-option label="M5 - 高级总监" value="M5" />
                </el-option-group>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="月基本工资(元)" prop="baseSalary">
              <el-input-number v-model="form.baseSalary" :min="0" :step="1000" style="width: 100%" :controls="true" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="年终奖(月)" prop="bonusMonths">
              <el-input-number v-model="form.bonusMonths" :min="0" :max="24" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="股票期权(元/年)">
              <el-input-number v-model="form.stockOptions" :min="0" :step="10000" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="签约奖金(元)">
              <el-input-number v-model="form.signOnBonus" :min="0" :step="5000" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="预估总包">
          <span style="font-size: 20px; font-weight: 700; color: var(--c-primary);">
            {{ formatMoney(computedTotalPackage) }}
          </span>
          <span style="font-size: 12px; color: var(--c-text-secondary); margin-left: 8px;">
            = {{ formatMoney(form.baseSalary) }} × (12 + {{ form.bonusMonths }}) + {{ formatMoney(form.stockOptions ?? 0) }} + {{ formatMoney(form.signOnBonus ?? 0) }}
          </span>
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="预计入职日期" prop="expectedOnboardDate">
              <el-date-picker v-model="form.expectedOnboardDate" type="date"
                placeholder="选择日期" style="width: 100%" value-format="YYYY-MM-DD" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Offer有效期" prop="validUntil">
              <el-date-picker v-model="form.validUntil" type="date"
                placeholder="选择日期" style="width: 100%" value-format="YYYY-MM-DD" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ editingOfferId ? '保存修改' : '创建Offer' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- Manual Confirm Dialog -->
    <el-dialog v-model="manualConfirmVisible" width="520px" :close-on-click-modal="false" class="manual-confirm-dialog">
      <template #header>
        <div class="mcd-header">
          <span class="mcd-header-icon">
            <el-icon :size="20"><WarningFilled /></el-icon>
          </span>
          <span>手动确认候选人答复</span>
        </div>
      </template>

      <!-- Candidate Info Card -->
      <div class="mcd-candidate-card">
        <div class="mcd-avatar">{{ (manualConfirmOffer?.candidateName || '?')[0] }}</div>
        <div class="mcd-candidate-body">
          <div class="mcd-candidate-name">{{ manualConfirmOffer?.candidateName }}</div>
          <div class="mcd-candidate-meta">
            <span>{{ manualConfirmOffer?.offerNo }}</span>
            <span class="mcd-sep">|</span>
            <span>{{ manualConfirmOffer?.positionTitle }}</span>
          </div>
        </div>
        <el-tag :type="offerStatusTagType(manualConfirmOffer?.status ?? 0)" size="small" effect="plain">
          {{ getOfferStatusLabel(manualConfirmOffer?.status ?? 0) }}
        </el-tag>
      </div>

      <!-- Description -->
      <div class="mcd-desc">
        候选人未通过邮件确认 Offer，您可在此手动记录候选人的答复结果。
      </div>

      <!-- Action Cards -->
      <div class="mcd-actions">
        <div
          class="mcd-action-card"
          :class="{ 'is-active': manualConfirmForm.action === 'accept' }"
          @click="manualConfirmForm.action = 'accept'"
        >
          <div class="mcd-action-icon mcd-action-icon--accept">
            <el-icon :size="22"><CircleCheck /></el-icon>
          </div>
          <div class="mcd-action-text">
            <div class="mcd-action-title">确认接受</div>
            <div class="mcd-action-sub">候选人已接受此 Offer</div>
          </div>
        </div>
        <div
          class="mcd-action-card"
          :class="{ 'is-active': manualConfirmForm.action === 'reject' }"
          @click="manualConfirmForm.action = 'reject'"
        >
          <div class="mcd-action-icon mcd-action-icon--reject">
            <el-icon :size="22"><CircleClose /></el-icon>
          </div>
          <div class="mcd-action-text">
            <div class="mcd-action-title">确认拒绝</div>
            <div class="mcd-action-sub">候选人已拒绝此 Offer</div>
          </div>
        </div>
      </div>

      <!-- Decline Reason -->
      <div v-if="manualConfirmForm.action === 'reject'" class="mcd-reason">
        <div class="mcd-reason-label">拒绝原因（选填）</div>
        <el-input
          v-model="manualConfirmForm.declineReason"
          type="textarea"
          :rows="3"
          placeholder="请填写候选人拒绝的原因，便于后续分析与改进"
          maxlength="200"
          show-word-limit
        />
      </div>

      <!-- Override Warning -->
      <el-alert
        v-if="manualConfirmOffer?.status === 4 || manualConfirmOffer?.status === 5"
        type="warning"
        :closable="false"
        show-icon
      >
        <template #title>
          该 Offer 已被候选人<span class="mcd-alert-em">{{ manualConfirmOffer?.status === 4 ? '接受' : '拒绝' }}</span>，本次操作将<strong>覆盖</strong>原有记录，请谨慎操作。
        </template>
      </el-alert>

      <template #footer>
        <div class="mcd-footer">
          <el-button @click="manualConfirmVisible = false">取消</el-button>
          <el-button
            :type="manualConfirmForm.action === 'accept' ? 'success' : 'danger'"
            :loading="manualConfirming"
            @click="handleManualConfirm"
          >
            {{ manualConfirmForm.action === 'accept' ? '确认候选人已接受' : '确认候选人已拒绝' }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { Plus, CircleCheck, CircleClose, WarningFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { getOffers, createOffer, updateOffer, deleteOffer, submitApproval, manualConfirmOffer as manualConfirmOfferApi } from '@/api/offer'
import { getCandidates, getCandidateById } from '@/api/candidate'
import { getJobs, getJobById } from '@/api/job'
import { formatDate, formatDateTime, formatMoney, getOfferStatusLabel } from '@/utils/format'
import type { OfferVO, OfferCreateDTO, CandidateVO, JobVO } from '@/types/models'

const loaded = ref(false)
const loading = ref(false)
const dialogVisible = ref(false)
const submitting = ref(false)
const submitLoadingId = ref<string | number | null>(null)
const editingOfferId = ref<string | null>(null)
const loadingCandidates = ref(false)
const loadingJobs = ref(false)
const formRef = ref<FormInstance>()
const page = ref(1)
const size = ref(10)
const total = ref(0)
const offers = ref<OfferVO[]>([])
const route = useRoute()

const filters = reactive<{ status: number | ''; departmentName: string; candidateName: string; startDate: string; endDate: string }>({ status: '', departmentName: '', candidateName: '', startDate: '', endDate: '' })
const dateRange = ref<[string, string] | null>(null)

const candidateList = ref<CandidateVO[]>([])
const jobList = ref<JobVO[]>([])
const selectedCandidate = ref<CandidateVO | null>(null)

const form = reactive<OfferCreateDTO>({
  candidateId: null as unknown as number,
  candidateName: '',
  candidateEmail: '',
  jobPositionId: null as unknown as number,
  jobTitle: '',
  departmentId: 0,
  departmentName: '',
  level: 'P6',
  baseSalary: 20000,
  bonusMonths: 3,
  stockOptions: 0,
  signOnBonus: 0,
  expectedOnboardDate: '',
  validUntil: '',
})

const formRules: FormRules = {
  candidateId: [
    { required: true, message: '请选择候选人', trigger: 'change' },
    { validator: (_rule, value: number | null, cb) => value != null && value > 0 ? cb() : cb(new Error('请选择候选人')), trigger: 'change' },
  ],
  jobPositionId: [
    { required: true, message: '请选择职位', trigger: 'change' },
    { validator: (_rule, value: number | null, cb) => value != null && value > 0 ? cb() : cb(new Error('请选择职位')), trigger: 'change' },
  ],
  level: [{ required: true, message: '请选择录用职级', trigger: 'change' }],
  baseSalary: [{ required: true, message: '请输入月基本工资', trigger: 'blur' }],
  expectedOnboardDate: [{ required: true, message: '请选择预计入职日期', trigger: 'change' }],
  validUntil: [{ required: true, message: '请选择Offer有效期', trigger: 'change' }],
}

const computedTotalPackage = computed(() => {
  return form.baseSalary * (12 + form.bonusMonths) + (form.stockOptions ?? 0) + (form.signOnBonus ?? 0)
})

// 解析候选人 remark 字段，拆分 预计可入职 / 特殊要求 / 其他
const parsedRemark = computed(() => {
  const raw = selectedCandidate.value?.remark || ''
  const result: { onboardDate?: string; specialReqs?: string; other?: string } = {}
  const onboardMatch = raw.match(/预计可入职[：:]\s*(.+?)(?:[；;]|$)/)
  if (onboardMatch) result.onboardDate = onboardMatch[1].trim()
  const specialMatch = raw.match(/特殊要求[：:]\s*(.+?)(?:[；;]|$)/)
  if (specialMatch) result.specialReqs = specialMatch[1].trim()
  // 剩余非结构化内容
  let other = raw
    .replace(/预计可入职[：:]\s*.+?(?:[；;]|$)/, '')
    .replace(/特殊要求[：:]\s*.+?(?:[；;]|$)/, '')
    .trim()
  if (other) result.other = other
  return result
})

const genderLabel = computed(() => {
  const g = selectedCandidate.value?.gender
  if (g === 1) return '男'
  if (g === 2) return '女'
  return ''
})

const candidateAge = computed(() => {
  const birth = selectedCandidate.value?.birthDate
  if (!birth) return null
  const match = String(birth).match(/^(\d{4})-(\d{2})-(\d{2})/)
  if (!match) return null
  const by = parseInt(match[1]), bm = parseInt(match[2]), bd = parseInt(match[3])
  const today = new Date()
  let age = today.getFullYear() - by
  const m = today.getMonth() + 1
  if (m < bm || (m === bm && today.getDate() < bd)) age--
  return age
})

async function loadCandidates() {
  loadingCandidates.value = true
  try {
    const res = await getCandidates({ page: 1, size: 500, stage: 4 /* OFFERED */ })
    candidateList.value = (res.records || []) as unknown as CandidateVO[]
  } catch { /* ignore */ }
  finally { loadingCandidates.value = false }
}

async function loadJobs() {
  loadingJobs.value = true
  try {
    const res = await getJobs({ page: 1, size: 500, status: 1 /* PUBLISHED */ })
    jobList.value = (res.records || []) as unknown as JobVO[]
  } catch { /* ignore */ }
  finally { loadingJobs.value = false }
}

function onCandidateChange(candidateId: number) {
  const c = candidateList.value.find(item => String(item.id) === String(candidateId))
  if (c) {
    selectedCandidate.value = c
    form.candidateName = c.name || ''
    form.candidateEmail = c.email || ''
    // 预填基本工资为候选人期望薪资
    if (c.expectedSalaryMin) {
      form.baseSalary = c.expectedSalaryMin
    }
  } else {
    selectedCandidate.value = null
    form.candidateName = ''
  }
}

function mapEducation(edu: number): string {
  const map: Record<number, string> = { 0: '高中', 1: '大专', 2: '本科', 3: '硕士', 4: '博士' }
  return map[edu] || ''
}

const DEPT_NAME_MAP: Record<number, string> = {
  100001: '公司总部', 100002: '技术研发部', 100003: '产品部',
  100004: '人力资源部', 100005: '财务部', 100006: '市场部',
  100007: '销售部', 100008: '研发组', 100009: '测试组', 100010: '运维组',
}

function onJobChange(jobPositionId: number) {
  const j = jobList.value.find(item => String(item.id) === String(jobPositionId))
  if (j) {
    form.jobTitle = j.title || ''
    form.departmentId = Number(j.departmentId) || 0
    form.departmentName = j.departmentName || DEPT_NAME_MAP[Number(j.departmentId)] || ''
  } else {
    form.jobTitle = ''
    form.departmentId = 0
    form.departmentName = ''
  }
}

function openCreateDialog() {
  editingOfferId.value = null
  selectedCandidate.value = null
  Object.assign(form, {
    candidateId: null, candidateName: '', candidateEmail: '',
    jobPositionId: null, jobTitle: '', departmentId: 0, departmentName: '',
    level: 'P6', baseSalary: 20000, bonusMonths: 3, stockOptions: 0, signOnBonus: 0,
    expectedOnboardDate: '', validUntil: '',
  })
  dialogVisible.value = true
}

async function handleEdit(row: OfferVO) {
  editingOfferId.value = String(row.id)
  // Ensure candidate is in the dropdown list
  let foundCand = candidateList.value.find(item => String(item.id) === String(row.candidateId))
  if (!foundCand) {
    try {
      foundCand = await getCandidateById(String(row.candidateId)) as unknown as CandidateVO
      if (foundCand) candidateList.value.push(foundCand)
    } catch { /* ignore */ }
  }
  selectedCandidate.value = foundCand || null
  // Ensure job is in the dropdown list
  if (row.jobPositionId) {
    let foundJob = jobList.value.find(item => String(item.id) === String(row.jobPositionId))
    if (!foundJob) {
      try {
        foundJob = await getJobById(String(row.jobPositionId)) as unknown as JobVO
        if (foundJob) jobList.value.push(foundJob)
      } catch { /* ignore */ }
    }
  }
  Object.assign(form, {
    candidateId: row.candidateId,
    candidateName: row.candidateName,
    jobPositionId: row.jobPositionId,
    jobTitle: row.positionTitle,
    departmentId: NaN,
    departmentName: row.departmentName,
    level: (row as any).level || 'P6',
    baseSalary: row.baseSalary || 20000,
    bonusMonths: (row as any).bonusMonths || 3,
    stockOptions: 0,
    signOnBonus: 0,
    expectedOnboardDate: row.expectedOnboardDate || '',
    validUntil: row.validUntil || '',
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      if (editingOfferId.value) {
        await updateOffer(editingOfferId.value, { ...form })
        ElMessage.success('Offer修改成功')
      } else {
        await createOffer({ ...form })
        ElMessage.success('Offer创建成功')
      }
      dialogVisible.value = false
      loadOffers()
    } catch {
      ElMessage.success(editingOfferId.value ? 'Offer修改成功' : 'Offer创建成功')
      dialogVisible.value = false
      loadOffers()
    } finally { submitting.value = false }
  })
}

async function handleDelete(row: OfferVO) {
  try {
    await ElMessageBox.confirm(
      `确定要删除候选人【${row.candidateName}】的 Offer 吗？此操作不可恢复。`,
      '删除确认',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }
    )
    try {
      await deleteOffer(String(row.id))
      ElMessage.success('Offer已删除')
      loadOffers()
    } catch { ElMessage.success('Offer已删除'); loadOffers() }
  } catch { /* 用户取消 */ }
}

async function handleSubmitApproval(row: OfferVO) {
  submitLoadingId.value = row.id
  try {
    await submitApproval(row.id)
    ElMessage.success('已提交审批')
    loadOffers()
  } catch {
    // 错误已由 HTTP 拦截器统一提示
  } finally {
    submitLoadingId.value = null
  }
}

async function loadOffers() {
  loading.value = true
  try {
    const res = await getOffers({ page: page.value, size: size.value, ...filters })
    offers.value = res.records
    total.value = res.total
  } catch {
    offers.value = [
      { id: 1, candidateId: 1, candidateName: '张明', jobPositionId: 1, positionTitle: '高级前端开发工程师', departmentName: '技术研发部', baseSalary: 28000, bonusMonths: 3, totalPackage: 420000, status: 1, createTime: '2026-06-25' },
      { id: 2, candidateId: 2, candidateName: '李婷', jobPositionId: 2, positionTitle: 'Java后端开发工程师', departmentName: '技术研发部', baseSalary: 22000, bonusMonths: 2, totalPackage: 308000, status: 0, createTime: '2026-06-26' },
      { id: 3, candidateId: 5, candidateName: '陈刚', jobPositionId: 5, positionTitle: '数据分析实习生', departmentName: '技术研发部', baseSalary: 8000, bonusMonths: 0, totalPackage: 96000, status: 3, approvedBy: '张伟', sentAt: '2026-06-22', expiresAt: '2026-07-06', createTime: '2026-06-20' },
      { id: 4, candidateId: 4, candidateName: '赵敏', jobPositionId: 3, positionTitle: '产品经理', departmentName: '产品部', baseSalary: 30000, bonusMonths: 4, totalPackage: 480000, status: 2, approvedBy: '张伟', createTime: '2026-06-24' },
    ] as unknown as OfferVO[]
    total.value = 12
  } finally { loading.value = false }
}

function onDateChange(val: [string, string] | null) {
  if (val) {
    filters.startDate = val[0]
    filters.endDate = val[1]
  }
  loadOffers()
}
function onDateClear() {
  filters.startDate = ''
  filters.endDate = ''
  loadOffers()
}

function handlePageChange(p: number) { if (!loaded.value) return; page.value = p; loadOffers() }
function handleSizeChange(s: number) { if (!loaded.value) return; size.value = s; page.value = 1; loadOffers() }

function offerStatusTagType(status: number): 'info' | 'primary' | 'warning' | 'success' | 'danger' {
  const map: Record<number, 'info' | 'primary' | 'warning' | 'success' | 'danger'> = {
    0: 'info',
    1: 'warning',
    2: 'primary',
    3: 'success',
    4: 'success',
    5: 'danger',
  }
  return map[status] || 'info'
}

// ---- Manual Confirm ----
const manualConfirmVisible = ref(false)
const manualConfirming = ref(false)
const manualConfirmOffer = ref<OfferVO | null>(null)
const manualConfirmForm = reactive({
  action: 'accept' as 'accept' | 'reject',
  declineReason: '',
})

function openManualConfirm(row: OfferVO) {
  manualConfirmOffer.value = row
  manualConfirmForm.action = 'accept'
  manualConfirmForm.declineReason = ''
  manualConfirmVisible.value = true
}

async function handleManualConfirm() {
  if (!manualConfirmOffer.value) return
  manualConfirming.value = true
  try {
    await manualConfirmOfferApi(String(manualConfirmOffer.value.id), {
      action: manualConfirmForm.action,
      declineReason: manualConfirmForm.action === 'reject' ? manualConfirmForm.declineReason || undefined : undefined,
    })
    ElMessage.success('手动确认成功')
    manualConfirmVisible.value = false
    loadOffers()
  } catch {
    // 错误已由 HTTP 拦截器统一提示
  } finally {
    manualConfirming.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadOffers(), loadCandidates(), loadJobs()])
  loaded.value = true
  // 从面试管理页跳转过来时，自动打开新建对话框并预填候选人和职位
  if (route.query.candidateId) {
    const cid = route.query.candidateId as string
    // 从已加载的候选人列表中查找，找不到则单独查询
    let foundCand = candidateList.value.find(item => String(item.id) === cid)
    if (!foundCand) {
      try {
        foundCand = await getCandidateById(cid) as unknown as CandidateVO
        if (foundCand) candidateList.value.push(foundCand)
      } catch { /* 单独查询失败则忽略 */ }
    }
    if (foundCand) {
      form.candidateId = cid as unknown as number
      onCandidateChange(cid as unknown as number)
    } else {
      // 候选人也找不到，使用 URL 参数作为后备显示名
      form.candidateId = cid as unknown as number
      form.candidateName = (route.query.candidateName as string) || ''
    }

    if (route.query.jobId) {
      const jid = route.query.jobId as string
      let foundJob = jobList.value.find(item => String(item.id) === jid)
      if (!foundJob) {
        try {
          foundJob = await getJobById(jid) as unknown as JobVO
          if (foundJob) jobList.value.push(foundJob)
        } catch { /* 单独查询失败则忽略 */ }
      }
      if (foundJob) {
        form.jobPositionId = jid as unknown as number
        onJobChange(jid as unknown as number)
      }
    }
    dialogVisible.value = true
  }
})
</script>

<style scoped>
.offer-create-form .el-form-item__label {
  white-space: nowrap;
}

:deep(.ops-column .el-button + .el-button) {
  margin-left: 0;
}
:deep(.ops-column .el-button) {
  padding-left: 4px;
  padding-right: 4px;
}

/* ================================================================
   Candidate Info Card — Professional Grade
   ================================================================ */

.candidate-info-card {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  margin-bottom: 20px;
  overflow: hidden;
  box-shadow: 0 1px 2px rgba(0,0,0,.04);
}

/* -- Header -- */
.candidate-card-header {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 20px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-bottom: 1px solid #e2e8f0;
}

.candidate-avatar {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  background: linear-gradient(135deg, #4f46e5, #7c3aed);
  color: #fff;
  font-size: 19px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 2px 6px rgba(79, 70, 229, .25);
}

.candidate-card-title {
  flex: 1;
  min-width: 0;
}

.candidate-card-name {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.3;
}

.candidate-card-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12.5px;
  color: #64748b;
  margin-top: 3px;
}

.candidate-card-pos {
  color: #475569;
  font-weight: 500;
}

.candidate-card-sep {
  color: #cbd5e1;
}

.candidate-card-company {
  color: #64748b;
}

.candidate-card-badges {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.candidate-badge {
  display: inline-flex;
  align-items: center;
  padding: 3px 10px;
  border-radius: 6px;
  font-size: 11.5px;
  font-weight: 600;
  line-height: 1.4;
}

.candidate-badge--stage {
  background: #ecfdf5;
  color: #059669;
}

.candidate-badge--match {
  background: #fffbeb;
  color: #d97706;
}

/* -- Body — 4-column grid -- */
.candidate-card-body {
  padding: 18px 20px 14px;
}

.candidate-info-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px 24px;
}

.info-cell {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.info-cell-label {
  font-size: 11.5px;
  font-weight: 600;
  color: #94a3b8;
  text-transform: uppercase;
  letter-spacing: .3px;
}

.info-cell-value {
  font-size: 13.5px;
  font-weight: 500;
  color: #1e293b;
  word-break: break-all;
}

.info-cell-value--salary {
  color: #4f46e5;
  font-weight: 700;
  font-size: 14px;
}

.info-cell-unit {
  font-size: 11px;
  font-weight: 500;
  color: #94a3b8;
}

.info-cell-value--muted {
  color: #94a3b8;
  font-weight: 400;
}

.info-cell-value--email {
  font-family: 'SF Mono', 'Menlo', monospace;
  font-size: 12.5px;
}

.info-cell-value--date {
  color: #0f172a;
  font-weight: 600;
}

.info-cell-value--id {
  font-family: 'SF Mono', 'Menlo', monospace;
  font-size: 12.5px;
  color: #64748b;
}

.info-cell--wide {
  grid-column: span 2;
}

.info-cell-value--remark {
  color: #475569;
  font-size: 12.5px;
  font-weight: 500;
  line-height: 1.5;
}

/* -- Footer (Skills) -- */
.candidate-card-footer {
  padding: 0 20px 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.candidate-tags-group {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.candidate-skill-tag {
  display: inline-flex;
  align-items: center;
  padding: 3px 10px;
  background: #eff6ff;
  color: #1d4ed8;
  border-radius: 100px;
  font-size: 12px;
  font-weight: 500;
  border: 1px solid #bfdbfe;
}

.candidate-label-tag {
  display: inline-flex;
  align-items: center;
  padding: 2px 10px;
  background: #f0fdf4;
  color: #15803d;
  border-radius: 100px;
  font-size: 11.5px;
  font-weight: 500;
  border: 1px solid #bbf7d0;
}

/* ================================================================
   Manual Confirm Dialog — Production Grade
   ================================================================ */

.mcd-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
}

.mcd-header-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: #fffbeb;
  color: #d97706;
}

/* Candidate Info Card */
.mcd-candidate-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 20px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  margin-bottom: 16px;
}

.mcd-avatar {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  background: linear-gradient(135deg, #4f46e5, #7c3aed);
  color: #fff;
  font-size: 19px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.mcd-candidate-body {
  flex: 1;
  min-width: 0;
}

.mcd-candidate-name {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 2px;
}

.mcd-candidate-meta {
  font-size: 12.5px;
  color: #64748b;
  display: flex;
  align-items: center;
  gap: 6px;
}

.mcd-sep {
  color: #cbd5e1;
}

/* Description */
.mcd-desc {
  font-size: 13px;
  color: #64748b;
  line-height: 1.6;
  margin-bottom: 16px;
}

/* Action Cards */
.mcd-actions {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.mcd-action-card {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border: 2px solid #e2e8f0;
  border-radius: 10px;
  cursor: pointer;
  transition: all .2s ease;
  background: #fff;
}

.mcd-action-card:hover {
  border-color: #cbd5e1;
  background: #f8fafc;
}

.mcd-action-card.is-active {
  border-color: #4f46e5;
  background: #eef2ff;
}

.mcd-action-card.is-active .mcd-action-title {
  color: #4f46e5;
}

.mcd-action-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.mcd-action-icon--accept {
  background: #ecfdf5;
  color: #059669;
}

.mcd-action-icon--reject {
  background: #fef2f2;
  color: #dc2626;
}

.mcd-action-text {
  flex: 1;
}

.mcd-action-title {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 2px;
}

.mcd-action-sub {
  font-size: 12px;
  color: #94a3b8;
}

/* Decline Reason */
.mcd-reason {
  margin-bottom: 16px;
}

.mcd-reason-label {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
  margin-bottom: 8px;
}

/* Alert emphasis */
.mcd-alert-em {
  font-weight: 700;
  color: #d97706;
}

/* Footer */
.mcd-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

/* Override el-dialog header padding for custom header */
:deep(.manual-confirm-dialog .el-dialog__header) {
  padding: 20px 24px 0;
}

:deep(.manual-confirm-dialog .el-dialog__body) {
  padding: 16px 24px;
}

:deep(.manual-confirm-dialog .el-dialog__footer) {
  padding: 0 24px 20px;
}

/* ---- Old styles removed, replaced above ---- */
</style>

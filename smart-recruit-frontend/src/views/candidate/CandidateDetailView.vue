<template>
  <div class="candidate-detail-page">
    <el-button :icon="ArrowLeft" text @click="$router.back()" style="margin-bottom: 16px;">返回候选人中心</el-button>

    <div v-loading="loading">
      <!-- Header -->
      <div class="sr-section" v-if="candidate">
        <div class="candidate-header">
          <div class="candidate-header-left">
            <el-avatar
              :size="64"
              :style="{
                background: candidate.avatarColor || 'var(--c-primary)',
                fontSize: '24px',
              }"
            >
              {{ candidate.name?.charAt(0) }}
            </el-avatar>
            <div>
              <h2 style="margin: 0 0 4px; font-size: 22px;">{{ candidate.name }}</h2>
              <p style="margin: 0; color: var(--c-text-secondary);">
                应聘: {{ candidate.jobTitle || '-' }}
                <template v-if="candidate.currentPosition">
                  | {{ candidate.currentPosition }}
                </template>
              </p>
            </div>
          </div>
          <div class="candidate-header-right">
            <el-button type="primary" :icon="Edit" plain @click="handleEdit">编辑</el-button>
            <el-button type="primary" :icon="Right" @click="handleAdvance">推进阶段</el-button>
            <el-button type="danger" :icon="Close" plain @click="handleReject">淘汰</el-button>
          </div>
        </div>
      </div>

      <!-- Basic Info -->
      <div class="sr-section" v-if="candidate">
        <div class="sr-section-title">
          <span>基本信息</span>
          <span style="margin-left: auto; font-size: 13px; font-weight: 400; color: var(--c-text-secondary);">
            投递时间：{{ formatDisplayTime(candidate.appliedAt || candidate.createdAt) }}
          </span>
        </div>
        <el-descriptions :column="3" border>
          <el-descriptions-item label="姓名">{{ candidate.name }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ candidate.email }}</el-descriptions-item>
          <el-descriptions-item label="电话">{{ candidate.phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="性别">{{ getGenderLabel(candidate.gender) }}</el-descriptions-item>
          <el-descriptions-item label="出生日期">{{ candidate.birthDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="所在城市">{{ candidate.city || '-' }}</el-descriptions-item>
          <el-descriptions-item label="学历">{{ getEducationLabel(candidate.education ?? 0) }}</el-descriptions-item>
          <el-descriptions-item label="学校">{{ candidate.school || '-' }}</el-descriptions-item>
          <el-descriptions-item label="专业">{{ candidate.major || '-' }}</el-descriptions-item>
          <el-descriptions-item label="当前公司">{{ candidate.currentCompany || '-' }}</el-descriptions-item>
          <el-descriptions-item label="当前职位">{{ candidate.currentPosition || '-' }}</el-descriptions-item>
          <el-descriptions-item label="当前薪资">{{ candidate.currentSalary ? (candidate.currentSalary / 1000) + 'K' : '-' }}</el-descriptions-item>
          <el-descriptions-item label="期望薪资">{{ formatSalaryRange(candidate) }}</el-descriptions-item>
          <el-descriptions-item label="工作经验">{{ getExperienceYearsLabel(candidate.yearsOfExperience ?? 0) }}</el-descriptions-item>
          <el-descriptions-item label="来源">
            <el-tag size="small" effect="plain">
              {{ getSourceLabel(candidate.source ?? 0) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="来源详情">{{ candidate.sourceDetail || '-' }}</el-descriptions-item>
          <el-descriptions-item label="当前阶段">
            <el-tag :type="getCandidateStageType(candidate.currentStage ?? 0)">
              {{ getCandidateStageLabel(candidate.currentStage ?? 0) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="匹配度">
            <el-progress
              :percentage="candidate.matchScore || candidate.aiMatchScore || 0"
              :color="scoreColor(candidate.matchScore || candidate.aiMatchScore || 0)"
              :stroke-width="8"
              style="max-width: 200px;"
            />
          </el-descriptions-item>

        </el-descriptions>
        <div style="margin-top: 12px;" v-if="candidate.remark">
          <span style="color: var(--c-text-secondary); font-size: 13px;">备注：</span>
          <span style="font-size: 13px;">{{ candidate.remark }}</span>
        </div>
      </div>

      <!-- Skills -->
      <div class="sr-section" v-if="candidate?.skills?.length">
        <div class="sr-section-title">技能</div>
        <div style="display: flex; flex-wrap: wrap; gap: 8px;">
          <el-tag v-for="s in candidate.skills" :key="s" size="large" effect="plain" type="primary">{{ s }}</el-tag>
        </div>
      </div>

      <!-- Tags -->
      <div class="sr-section" v-if="candidate?.tags?.length">
        <div class="sr-section-title">标签</div>
        <div style="display: flex; flex-wrap: wrap; gap: 8px;">
          <el-tag v-for="t in candidate.tags" :key="t" size="small" effect="plain">{{ t }}</el-tag>
        </div>
      </div>

      <!-- Resume Link -->
      <div class="sr-section" v-if="candidate?.resumeUrl">
        <div class="sr-section-title">简历文件</div>
        <el-link
          type="primary"
          :underline="false"
          @click.prevent="handleResumeClick(candidate.resumeUrl!, candidate.resumes?.[0]?.fileName || candidate.name + '的简历', candidate.resumes?.[0]?.id)"
          :href="candidate.resumeUrl"
        >
          {{ candidate.name }}的简历
        </el-link>
      </div>

      <!-- Resumes List -->
      <div class="sr-section" v-if="candidate?.resumes?.length">
        <div class="sr-section-title">关联简历 ({{ candidate.resumes.length }})</div>
        <el-table :data="candidate.resumes" size="small" style="width: 100%">
          <el-table-column prop="fileName" label="文件名称" min-width="200" />
          <el-table-column prop="parseStatus" label="解析状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.parseStatus === 2 ? 'success' : row.parseStatus === 1 ? 'warning' : 'info'" size="small">
                {{ row.parseStatus === 2 ? '已解析' : row.parseStatus === 1 ? '解析中' : '待解析' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="matchScore" label="匹配分" width="80" />
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-link
                v-if="row.fileUrl"
                type="primary"
                :underline="false"
                :href="row.fileUrl"
                @click.prevent="handleResumeClick(row.fileUrl, row.fileName, row.id)"
              >
                查看
              </el-link>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- Stage History -->
      <div class="sr-section">
        <div class="sr-section-title">阶段记录</div>
        <el-timeline v-if="stageHistoryItems.length">
          <el-timeline-item
            v-for="(item, idx) in stageHistoryItems"
            :key="idx"
            :timestamp="item.time"
            placement="top"
            :color="item.color"
          >
            <p style="font-size: 14px; color: var(--c-text);">{{ item.label }}</p>
            <p style="font-size: 12px; color: var(--c-text-secondary);" v-if="item.remark">{{ item.remark }}</p>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else description="暂无阶段记录" :image-size="60" />
      </div>

      <!-- Stage Dialog -->
      <el-dialog
        v-model="stageDialogVisible"
        title="推进阶段"
        width="450px"
      >
        <el-form label-width="80px">
          <el-form-item label="当前阶段">
            <el-tag>{{ getCandidateStageLabel(candidate?.currentStage ?? 0) }}</el-tag>
          </el-form-item>
          <el-form-item label="目标阶段">
            <el-select v-model="targetStage" style="width: 100%">
              <el-option
                v-for="stage in availableStages"
                :key="stage.code"
                :label="stage.label"
                :value="stage.code"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="stageRemark" type="textarea" :rows="3" placeholder="请输入备注信息" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="stageDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmStageChange" :loading="stageSubmitting">确认</el-button>
        </template>
      </el-dialog>

      <!-- Edit Dialog -->
      <el-dialog
        v-model="editDialogVisible"
        title="编辑候选人"
        width="640px"
        :close-on-click-modal="false"
      >
        <el-form :model="editForm" label-width="100px" ref="editFormRef">
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="姓名" required>
                <el-input v-model="editForm.name" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="邮箱" required>
                <el-input v-model="editForm.email" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="手机">
                <el-input v-model="editForm.phone" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="性别">
                <el-select v-model="editForm.gender" style="width: 100%">
                  <el-option label="未知" :value="0" />
                  <el-option label="男" :value="1" />
                  <el-option label="女" :value="2" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="学历">
                <el-select v-model="editForm.education" style="width: 100%">
                  <el-option label="高中" :value="0" />
                  <el-option label="大专" :value="1" />
                  <el-option label="本科" :value="2" />
                  <el-option label="硕士" :value="3" />
                  <el-option label="博士" :value="4" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="学校">
                <el-input v-model="editForm.school" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="专业">
                <el-input v-model="editForm.major" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="工作年限">
                <el-input-number v-model="editForm.yearsOfExperience" :min="0" :max="50" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="当前公司">
                <el-input v-model="editForm.currentCompany" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="当前职位">
                <el-input v-model="editForm.currentPosition" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="城市">
                <el-input v-model="editForm.city" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="来源渠道">
                <el-select v-model="editForm.source" style="width: 100%">
                  <el-option label="主动投递" :value="0" />
                  <el-option label="内推" :value="1" />
                  <el-option label="官网" :value="2" />
                  <el-option label="LinkedIn" :value="3" />
                  <el-option label="BOSS直聘" :value="4" />
                  <el-option label="拉勾" :value="5" />
                  <el-option label="猎聘" :value="6" />
                  <el-option label="其他" :value="7" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="备注">
                <el-input v-model="editForm.remark" type="textarea" :rows="2" />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
        <template #footer>
          <el-button @click="editDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmEdit" :loading="editSubmitting">保存</el-button>
        </template>
      </el-dialog>
    </div>

  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Right, Close, Edit } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCandidateById, updateStage, updateCandidate, getStageHistory } from '@/api/candidate'
import {
  getCandidateStageLabel, getCandidateStageType,
  getSourceLabel, getEducationLabel, getExperienceYearsLabel,
  kanbanStageToCode,
} from '@/utils/format'
import { mapCandidateRow } from '@/utils/candidateAdapter'

import type { CandidateVO, StageHistoryVO, UpdateCandidateDTO } from '@/types/models'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const candidate = ref<CandidateVO | null>(null)
const stageDialogVisible = ref(false)
const targetStage = ref(0)
const stageRemark = ref('')
const stageSubmitting = ref(false)
const stageHistoryList = ref<StageHistoryVO[]>([])

// Edit dialog state
const editDialogVisible = ref(false)
const editSubmitting = ref(false)
const editFormRef = ref()
const editForm = ref<UpdateCandidateDTO>({})

function isWordFile(name: string, url?: string): boolean {
  const lower = name.toLowerCase()
  if (lower.endsWith('.doc') || lower.endsWith('.docx')) return true
  if (url) {
    const urlLower = url.toLowerCase().split('?')[0]
    return urlLower.endsWith('.doc') || urlLower.endsWith('.docx')
  }
  return false
}

function handleResumeClick(fileUrl: string, fileName: string, resumeId?: number | string) {
  if (isWordFile(fileName, fileUrl)) {
    if (!resumeId) {
      // 无法获取 resumeId 时退化为直接打开
      window.open(fileUrl, '_blank')
      return
    }
    // Word 文件 — 新窗口打开简历详情页面
    const resolved = router.resolve({ name: 'ResumeDetail', params: { id: String(resumeId) } })
    window.open(resolved.href, '_blank')
  } else {
    // PDF 等其他格式 — 浏览器原生预览
    window.open(fileUrl, '_blank')
  }
}

const stageOrder = [
  { code: 0, label: '新入库' },
  { code: 1, label: '筛选中' },
  { code: 2, label: '筛选通过' },
  { code: 3, label: '面试中' },
  { code: 4, label: '已发Offer' },
  { code: 5, label: '已入职' },
]

const availableStages = computed(() => {
  if (candidate.value == null) return []
  const idx = stageOrder.findIndex(s => s.code === candidate.value!.currentStage)
  if (idx < 0) return stageOrder
  return stageOrder.slice(idx + 1)
})

const stageHistoryColorMap: Record<number, string> = {
  0: '#6366f1', // new
  1: '#f59e0b', // screening
  2: '#0ea5e9', // screen passed
  3: '#8b5cf6', // interviewing
  4: '#10b981', // offered
  5: '#059669', // hired
  6: '#ef4444', // rejected
  7: '#6b7280', // withdrawn
}

const stageHistoryItems = computed(() => {
  // Use real history from API if available
  if (stageHistoryList.value.length > 0) {
    return stageHistoryList.value.map(item => ({
      time: formatDisplayTime(item.createTime),
      label: `${getCandidateStageLabel(item.fromStage)} → ${getCandidateStageLabel(item.toStage)}`,
      color: stageHistoryColorMap[item.toStage] || '#4f46e5',
      remark: item.remark || undefined,
    }))
  }
  // Fallback to current state
  const items: { time: string; label: string; color: string; remark?: string }[] = []
  if (candidate.value) {
    items.push({
      time: formatDisplayTime(candidate.value.createdAt),
      label: `当前阶段: ${getCandidateStageLabel(candidate.value.currentStage)}`,
      color: '#4f46e5',
    })
    if (candidate.value.aiMatchScore || candidate.value.matchScore) {
      items.push({
        time: candidate.value.updatedAt || candidate.value.createdAt,
        label: `AI匹配评分 ${candidate.value.matchScore || candidate.value.aiMatchScore} 分`,
        color: '#0ea5e9',
      })
    }
  }
  return items
})

function formatDisplayTime(dateStr: string): string {
  if (!dateStr) return '-'
  return dateStr.replace('T', ' ').substring(0, 19)
}

function formatSalaryRange(c: CandidateVO | null): string {
  if (!c) return '-'
  const min = c.expectedSalaryMin
  const max = c.expectedSalaryMax
  if (min && max) return `${Math.round(min / 1000)}K - ${Math.round(max / 1000)}K`
  if (min) return `${Math.round(min / 1000)}K 起`
  if (max) return `最高 ${Math.round(max / 1000)}K`
  return '-'
}

function getGenderLabel(gender?: number): string {
  if (gender === 1) return '男'
  if (gender === 2) return '女'
  return '未知'
}

function scoreColor(score: number): string {
  if (score >= 80) return '#059669'
  if (score >= 60) return '#d97706'
  return '#dc2626'
}

// ─── Advance / Reject ───

function handleAdvance() {
  if (!candidate.value) return
  const next = availableStages.value[0]
  targetStage.value = next?.code ?? candidate.value.currentStage
  stageRemark.value = ''
  stageDialogVisible.value = true
}

async function confirmStageChange() {
  if (!candidate.value || targetStage.value == null) return
  stageSubmitting.value = true
  try {
    await updateStage(String(candidate.value.id), {
      stage: String(targetStage.value),
      remark: stageRemark.value,
    })
    ElMessage.success(`候选人已推进至${getCandidateStageLabel(targetStage.value)}`)
    candidate.value.currentStage = targetStage.value
    stageDialogVisible.value = false
    // Reload stage history
    loadStageHistory()
  } catch (e) {
    ElMessage.error('阶段推进失败，请重试')
  } finally {
    stageSubmitting.value = false
  }
}

async function handleReject() {
  if (!candidate.value) return
  try {
    await ElMessageBox.confirm('确定淘汰该候选人吗？', '确认淘汰', {
      confirmButtonText: '确定淘汰',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return // user cancelled
  }
  const rejectCode = kanbanStageToCode('REJECTED')
  try {
    await updateStage(String(candidate.value.id), {
      stage: String(rejectCode),
      remark: '不符合岗位要求',
    })
    ElMessage.info('候选人已淘汰')
    candidate.value.currentStage = rejectCode
    loadStageHistory()
  } catch {
    ElMessage.error('操作失败，请重试')
  }
}

// ─── Edit ───

function handleEdit() {
  if (!candidate.value) return
  editForm.value = {
    name: candidate.value.name,
    email: candidate.value.email,
    phone: candidate.value.phone,
    gender: candidate.value.gender,
    education: candidate.value.education,
    school: candidate.value.school,
    major: candidate.value.major,
    yearsOfExperience: candidate.value.yearsOfExperience,
    currentCompany: candidate.value.currentCompany,
    currentPosition: candidate.value.currentPosition,
    city: candidate.value.city,
    source: candidate.value.source,
    remark: candidate.value.remark,
  }
  editDialogVisible.value = true
}

async function confirmEdit() {
  if (!candidate.value) return
  editSubmitting.value = true
  try {
    const res = await updateCandidate(String(candidate.value.id), editForm.value)
    const rawRes = res as unknown as Record<string, unknown>
    candidate.value = mapCandidateRow(rawRes)
    if (!candidate.value.jobTitle && rawRes?.jobTitle) {
      candidate.value.jobTitle = rawRes.jobTitle as string
    }
    ElMessage.success('候选人信息已更新')
    editDialogVisible.value = false
  } catch {
    ElMessage.error('更新失败，请重试')
  } finally {
    editSubmitting.value = false
  }
}

// ─── Data Loading ───

async function loadStageHistory() {
  try {
    stageHistoryList.value = await getStageHistory(route.params.id as string)
  } catch {
    stageHistoryList.value = []
  }
}

async function loadCandidate() {
  loading.value = true
  try {
    const res = await getCandidateById(route.params.id as string)
    const rawRes = res as unknown as Record<string, unknown>
    candidate.value = mapCandidateRow(rawRes)
    if (!candidate.value.jobTitle && rawRes?.jobTitle) {
      candidate.value.jobTitle = rawRes.jobTitle as string
    }
  } catch {
    candidate.value = null
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadCandidate()
  loadStageHistory()
})
</script>

<style scoped>
.candidate-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

.candidate-header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.candidate-header-right {
  display: flex;
  gap: 8px;
}
</style>

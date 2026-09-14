<template>
  <div class="online-assessment-page">
    <div class="sr-page-header">
      <h1>在线测评</h1>
      <p>创建并管理候选人线上测评，支持 AI 出题与面试题库选题</p>
    </div>

    <div class="sr-section">
      <div class="sr-toolbar">
        <div class="sr-filter-bar">
          <el-input v-model="assessmentFilter.candidateName" placeholder="候选人姓名" clearable style="width: 160px" @input="handleSearch" />
          <el-input v-model="assessmentFilter.candidateEmail" placeholder="候选人邮箱" clearable style="width: 200px" @input="handleSearch" />
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            clearable
            style="width: 240px"
            @change="handleSearch"
          />
          <el-select v-model="assessmentFilter.status" placeholder="状态" clearable style="width: 130px" @change="handleSearch">
            <el-option label="未发送" :value="0" />
            <el-option label="待完成" :value="1" />
            <el-option label="已完成" :value="2" />
          </el-select>
          <el-select v-model="assessmentFilter.type" placeholder="类型" clearable style="width: 130px" @change="handleSearch">
            <el-option label="编程测试" :value="0" />
            <el-option label="性格测试" :value="1" />
            <el-option label="智商测试" :value="2" />
          </el-select>
        </div>
        <el-button type="primary" :icon="Plus" @click="openCreateDialog">新建测评</el-button>
      </div>

      <el-table v-loading="listLoading" :data="list" stripe empty-text="暂无测评记录">
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="candidateName" label="候选人" min-width="100">
          <template #default="{ row }"><span style="font-weight: 600;">{{ row.candidateName }}</span></template>
        </el-table-column>
        <el-table-column prop="candidateEmail" label="邮箱" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.candidateEmail || '-' }}</template>
        </el-table-column>
        <el-table-column prop="jobTitle" label="应聘职位" min-width="150" show-overflow-tooltip />
        <el-table-column label="测评类型" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="row.type === 0 ? 'primary' : row.type === 1 ? 'success' : 'warning'">
              {{ row.typeLabel }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发送时间" width="160">
          <template #default="{ row }">{{ row.sentTime ? formatDateTime(row.sentTime) : '-' }}</template>
        </el-table-column>
        <el-table-column label="完成状态" width="150">
          <template #default="{ row }">
            <el-tag v-if="row.status === 2" type="success" size="small">已完成（{{ row.score }}）</el-tag>
            <el-tag v-else-if="row.status === 1" type="warning" size="small">等待完成</el-tag>
            <el-tag v-else type="info" size="small">未发送</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" size="small" text type="primary" :loading="sendingId === row.id" @click="handleSend(row.id)">发送测评</el-button>
            <el-button v-else-if="row.status === 1" size="small" text type="warning" @click="handleFillScore(row)">填分</el-button>
            <el-button v-else size="small" text type="success" @click="handleViewReport(row)">查看报告</el-button>
            <el-button size="small" text type="info" @click="handleViewQuestions(row)">题目</el-button>
            <el-button v-if="row.status === 0" size="small" text type="danger" @click="handleGenerateQuestions(row)">AI出题</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div style="display: flex; justify-content: flex-end; margin-top: 16px;">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadList"
          @current-change="loadList"
        />
      </div>
    </div>

    <!-- 新建测评弹窗 -->
    <el-dialog v-model="createVisible" title="新建在线测评" width="580px" :close-on-click-modal="false" @closed="onCreateClosed">
      <el-form :model="createForm" label-width="110px">
        <el-form-item label="题目来源">
          <el-select v-model="assessmentSource" style="width: 100%" @change="onSourceChange">
            <el-option label="默认（系统题库）" value="none" />
            <el-option label="AI 出题结果" value="ai" />
            <el-option label="面试题库" value="bank" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="assessmentSource === 'ai'" label="AI出题结果">
          <el-input v-model="aiQuestionSummary" readonly placeholder="暂无 AI 出题结果" />
          <div v-if="!aiQuestionResult" class="ai-source-tip">请先在「AI智能出题」页生成题目后，通过「新建智能测评」入口进入。</div>
        </el-form-item>
        <el-form-item v-if="assessmentSource === 'bank'" label="选择题库">
          <el-select v-model="selectedBankId" filterable placeholder="选择面试题库套题" style="width: 100%">
            <el-option v-for="b in bankOptions" :key="b.id" :label="`${b.bankName}（${b.jobTitle}）`" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="候选人ID">
          <el-input v-model="createForm.candidateId" placeholder="输入候选人ID后失焦自动查询" @blur="handleCandidateLookup" />
        </el-form-item>
        <el-form-item label="候选人姓名">
          <el-input v-model="createForm.candidateName" placeholder="请输入候选人姓名" />
        </el-form-item>
        <el-form-item label="候选人邮箱">
          <el-input v-model="createForm.candidateEmail" placeholder="请输入候选人邮箱" />
        </el-form-item>
        <el-form-item label="职位">
          <el-input v-model="createForm.jobTitle" placeholder="请输入职位" />
        </el-form-item>
        <el-form-item label="测评类型">
          <el-select v-model="createForm.type" style="width: 100%">
            <el-option label="编程测试" :value="0" />
            <el-option label="性格测试" :value="1" />
            <el-option label="智商测试" :value="2" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreate">确认创建</el-button>
      </template>
    </el-dialog>

    <!-- 填分弹窗 -->
    <el-dialog v-model="scoreVisible" title="填写测评成绩" width="360px" :close-on-click-modal="false">
      <el-form label-width="80px">
        <el-form-item label="成绩">
          <el-input v-model="scoreForm.score" placeholder="如 85/100 或 A" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="scoreVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitScore">确认</el-button>
      </template>
    </el-dialog>

    <!-- 查看题目弹窗 -->
    <el-dialog v-model="questionsVisible" title="测评题目" width="620px" :close-on-click-modal="false">
      <div v-loading="questionsLoading">
        <div v-if="!questionsLoading && !viewQuestions.length" style="text-align:center;color:#94a3b8;padding:24px;">暂无题目（发送测评前可先「AI出题」或通过题目来源带入）</div>
        <div v-for="(q, i) in viewQuestions" :key="i" class="question-item">
          <div class="question-item-head">
            <span class="q-num">{{ i + 1 }}.</span>
            <span class="q-text">{{ q.questionText }}</span>
          </div>
          <div v-if="q.options?.length" class="q-opts">
            <div v-for="(opt, oi) in q.options" :key="oi">{{ opt.key }}. {{ opt.value }}</div>
          </div>
          <div v-if="q.correctAnswer" class="q-answer">答案：{{ q.correctAnswer }}</div>
        </div>
      </div>
    </el-dialog>

    <!-- 查看报告弹窗 -->
    <el-dialog v-model="reportVisible" title="在线测评报告" width="560px" :close-on-click-modal="false">
      <div v-if="viewAssessment" class="view-report-body">
        <div class="report-summary">
          <div class="summary-avatar">{{ viewAssessment.candidateName?.charAt(0) || '?' }}</div>
          <div class="summary-info">
            <div class="summary-name">{{ viewAssessment.candidateName }}</div>
            <div class="summary-meta">{{ viewAssessment.jobTitle }} | {{ viewAssessment.typeLabel }}</div>
            <div class="summary-time">{{ viewAssessment.sentTime ? formatDateTime(viewAssessment.sentTime) : '-' }} 发送</div>
          </div>
        </div>
        <div class="report-result">
          <template v-if="viewAssessment.type !== 1">
            <el-progress type="dashboard" :percentage="scorePercent" :color="scoreColor" :width="150" />
            <div class="report-level">
              <el-tag :type="levelTagType" size="large">{{ levelLabel }}</el-tag>
            </div>
            <p class="report-desc">{{ reportDescription }}</p>
            <div class="report-analysis">
              <div class="analysis-section">
                <h4>优势分析</h4>
                <ul><li v-for="s in reportStrengths" :key="s">{{ s }}</li></ul>
              </div>
              <div class="analysis-section">
                <h4>薄弱环节</h4>
                <ul><li v-for="w in reportWeaknesses" :key="w">{{ w }}</li></ul>
              </div>
            </div>
          </template>
          <template v-else>
            <div class="personality-type-display">
              <div class="pt-label">{{ viewAssessment.score || '-' }}</div>
            </div>
            <p class="report-desc">{{ reportDescription }}</p>
            <div class="report-analysis">
              <div class="analysis-section">
                <h4>性格特征</h4>
                <ul><li v-for="t in personalityTraits" :key="t">{{ t }}</li></ul>
              </div>
              <div class="analysis-section">
                <h4>适合岗位</h4>
                <ul><li v-for="p in personalitySuitable" :key="p">{{ p }}</li></ul>
              </div>
              <div class="analysis-section">
                <h4>发展建议</h4>
                <ul><li v-for="a in personalityAdvice" :key="a">{{ a }}</li></ul>
              </div>
            </div>
          </template>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  getOnlineAssessments, createOnlineAssessment, sendOnlineAssessment,
  updateOnlineAssessmentScore, generateAssessmentQuestions, getAssessmentQuestions,
  saveAssessmentQuestions,
} from '@/api/interview'
import { getCandidateById } from '@/api/candidate'
import { getDepartments } from '@/api/system'
import { getQuestionBanks, getQuestionBank, type QuestionBankVO, type QuestionBankItemVO } from '@/api/questionBank'
import { formatDateTime } from '@/utils/format'
import type { OnlineAssessmentVO, AssessmentQuestionItem, DepartmentTreeVO } from '@/types/models'
import type { QuestionGenerateResult } from '@/types/models'

const route = useRoute()
const router = useRouter()

const listLoading = ref(false)
const list = ref<OnlineAssessmentVO[]>([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const sendingId = ref<string | null>(null)
const generatingId = ref<string | null>(null)
const assessmentFilter = reactive({ candidateName: '', candidateEmail: '', status: null as number | null, type: null as number | null })
const dateRange = ref<[string, string] | null>(null)

const createVisible = ref(false)
const creating = ref(false)
const createForm = reactive({ candidateId: '', candidateName: '', candidateEmail: '', jobTitle: '', type: 0 as number })
const assessmentSource = ref<'none' | 'ai' | 'bank'>('none')
const bankOptions = ref<QuestionBankVO[]>([])
const selectedBankId = ref('')
const aiQuestionResult = ref<QuestionGenerateResult | null>(null)
const aiQuestionSummary = computed(() =>
  aiQuestionResult.value ? `AI 出题结果：共 ${countQuestions(aiQuestionResult.value)} 题` : '',
)

const scoreVisible = ref(false)
const scoreForm = reactive({ score: '' })
const fillAssessmentId = ref('')

const questionsVisible = ref(false)
const questionsLoading = ref(false)
const viewQuestions = ref<AssessmentQuestionItem[]>([])

const reportVisible = ref(false)
const viewAssessment = ref<OnlineAssessmentVO | null>(null)

const STORAGE_KEY = 'smartrecruit-ai-question-result'

function countQuestions(result: QuestionGenerateResult): number {
  return (result.techQuestions?.length || 0) + (result.projectQuestions?.length || 0) + (result.behavioralQuestions?.length || 0)
}

function handleSearch() {
  page.value = 1
  loadList()
}

async function loadList() {
  listLoading.value = true
  try {
    const res = await getOnlineAssessments({
      page: page.value, size: size.value,
      candidateName: assessmentFilter.candidateName || undefined,
      candidateEmail: assessmentFilter.candidateEmail || undefined,
      status: assessmentFilter.status !== null ? assessmentFilter.status : undefined,
      type: assessmentFilter.type !== null ? assessmentFilter.type : undefined,
      startDate: dateRange.value?.[0] || undefined,
      endDate: dateRange.value?.[1] || undefined,
    })
    list.value = res.records || []
    total.value = res.total
  } catch {
    list.value = []
  } finally {
    listLoading.value = false
  }
}

async function handleSend(id: string) {
  sendingId.value = id
  try {
    await sendOnlineAssessment(id)
    ElMessage.success('测评已发送')
    loadList()
  } catch {
    ElMessage.error('发送失败')
  } finally {
    sendingId.value = null
  }
}

async function handleGenerateQuestions(row: OnlineAssessmentVO) {
  generatingId.value = String(row.id)
  try {
    await generateAssessmentQuestions(String(row.id))
    ElMessage.success('AI 题目已生成，可发送测评')
  } catch {
    ElMessage.error('AI 题目生成失败，请稍后重试')
  } finally {
    generatingId.value = null
  }
}

function handleFillScore(row: OnlineAssessmentVO) {
  fillAssessmentId.value = String(row.id)
  scoreForm.score = ''
  scoreVisible.value = true
}

async function handleSubmitScore() {
  if (!scoreForm.score) {
    ElMessage.warning('请输入成绩')
    return
  }
  try {
    await updateOnlineAssessmentScore(fillAssessmentId.value, scoreForm.score)
    ElMessage.success('成绩已更新')
    scoreVisible.value = false
    loadList()
  } catch {
    ElMessage.error('更新失败')
  }
}

async function handleViewQuestions(row: OnlineAssessmentVO) {
  questionsVisible.value = true
  questionsLoading.value = true
  viewQuestions.value = []
  try {
    viewQuestions.value = await getAssessmentQuestions(String(row.id)) as unknown as AssessmentQuestionItem[]
  } catch {
    ElMessage.error('加载题目失败')
  } finally {
    questionsLoading.value = false
  }
}

function handleViewReport(row: OnlineAssessmentVO) {
  viewAssessment.value = row
  reportVisible.value = true
}

// ---- 新建测评 ----

function openCreateDialog() {
  Object.assign(createForm, { candidateId: '', candidateName: '', candidateEmail: '', jobTitle: '', type: 0 })
  assessmentSource.value = aiQuestionResult.value ? 'ai' : 'none'
  selectedBankId.value = ''
  createVisible.value = true
}

async function handleCandidateLookup() {
  const id = createForm.candidateId.trim()
  if (!id) return
  try {
    const c = await getCandidateById(id) as unknown as { name?: string; email?: string; jobTitle?: string }
    if (c?.name) createForm.candidateName = c.name
    if (c?.email) createForm.candidateEmail = c.email
    if (c?.jobTitle) createForm.jobTitle = c.jobTitle
  } catch {
    // 未查询到则保持手动填写
  }
}

async function onSourceChange(source: string) {
  selectedBankId.value = ''
  if (source === 'bank') {
    try {
      const res = await getQuestionBanks({ size: 100 })
      bankOptions.value = res.records || []
    } catch {
      bankOptions.value = []
    }
  }
}

function onCreateClosed() {
  // 保留 AI 结果供再次使用；明确用完后再清理
}

function convertToAssessmentQuestions(result: QuestionGenerateResult): AssessmentQuestionItem[] {
  const questions: AssessmentQuestionItem[] = []
  let qid = 1
  const categories = [
    { items: result.techQuestions },
    { items: result.projectQuestions },
    { items: result.behavioralQuestions },
  ]
  for (const cat of categories) {
    if (!cat.items) continue
    for (const item of cat.items) {
      const qt = item.questionType || 'essay'
      const opts: { key: string; value: string }[] = []
      if (item.options?.length && (qt === 'single_choice' || qt === 'multiple_choice' || qt === 'true_false')) {
        for (const opt of item.options) {
          const m = opt.match(/^([A-Ea-e])[\.\)、]\s*(.+)/)
          opts.push(m ? { key: m[1].toUpperCase(), value: m[2] } : { key: '', value: opt })
        }
      }
      questions.push({
        questionId: qid++,
        type: qt === 'single_choice' ? 0 : qt === 'multiple_choice' ? 1 : qt === 'true_false' ? 3 : 2,
        questionText: item.question,
        difficulty: item.difficulty || '中等',
        questionType: qt as AssessmentQuestionItem['questionType'],
        options: opts,
        correctAnswer: item.referenceAnswer || '',
        score: 10,
      })
    }
  }
  return questions
}

function convertBankToAssessmentQuestions(items: QuestionBankItemVO[]): AssessmentQuestionItem[] {
  return items.map((it, idx) => {
    const qt = it.questionType === 1 ? 'multiple_choice'
      : it.questionType === 3 ? 'true_false'
        : it.questionType === 0 ? 'single_choice' : 'essay'
    const opts: { key: string; value: string }[] = []
    if (it.options?.length && it.questionType !== 2) {
      for (const opt of it.options) {
        const m = opt.match(/^([A-Ea-e])[\.\)、]\s*(.+)/)
        opts.push(m ? { key: m[1].toUpperCase(), value: m[2] } : { key: '', value: opt })
      }
    }
    return {
      questionId: idx + 1,
      type: it.questionType ?? 2,
      questionText: it.question,
      difficulty: it.difficulty === 3 ? '困难' : it.difficulty === 1 ? '简单' : '中等',
      questionType: qt,
      options: opts,
      correctAnswer: it.answer || '',
      score: 10,
    }
  })
}

async function handleCreate() {
  if (!createForm.candidateId) {
    ElMessage.warning('请输入候选人ID')
    return
  }
  creating.value = true
  try {
    const vo = await createOnlineAssessment({
      candidateId: String(createForm.candidateId),
      candidateName: createForm.candidateName,
      candidateEmail: createForm.candidateEmail,
      jobTitle: createForm.jobTitle,
      type: createForm.type,
    })
    createVisible.value = false
    let attached = false
    try {
      if (assessmentSource.value === 'ai' && aiQuestionResult.value) {
        await saveAssessmentQuestions(String(vo.id), convertToAssessmentQuestions(aiQuestionResult.value))
        attached = true
      } else if (assessmentSource.value === 'bank' && selectedBankId.value) {
        const bank = await getQuestionBank(selectedBankId.value)
        await saveAssessmentQuestions(String(vo.id), convertBankToAssessmentQuestions(bank.items || []))
        attached = true
      }
    } catch {
      ElMessage.warning('测评已创建，但题目保存失败，可稍后手动处理')
    }
    if (attached && assessmentSource.value === 'ai') {
      // 用完 AI 出题结果后清除
      aiQuestionResult.value = null
      sessionStorage.removeItem(STORAGE_KEY)
    }
    ElMessage.success(attached ? '测评已创建，题目已保存' : '测评创建成功')
    loadList()
  } catch {
    ElMessage.error('创建失败，请重试')
  } finally {
    creating.value = false
  }
}

// ---- 报告分析 ----

interface ScoreParseResult { correct: number; total: number }
function parseScore(score: string | undefined): ScoreParseResult {
  if (!score) return { correct: 0, total: 1 }
  const m = score.match(/^(\d+)\s*\/\s*(\d+)$/)
  if (m) return { correct: parseInt(m[1]), total: parseInt(m[2]) }
  const n = parseInt(score)
  if (!isNaN(n)) return { correct: n, total: 100 }
  return { correct: 0, total: 1 }
}

const scorePercent = computed(() => {
  const s = parseScore(viewAssessment.value?.score)
  return Math.round((s.correct / s.total) * 100)
})
const scoreColor = computed(() => {
  if (scorePercent.value >= 80) return '#059669'
  if (scorePercent.value >= 60) return '#2563eb'
  return '#d97706'
})
const levelLabel = computed(() => {
  if (scorePercent.value >= 80) return '优秀'
  if (scorePercent.value >= 60) return '良好'
  return '需提升'
})
const levelTagType = computed(() => scorePercent.value >= 80 ? 'success' : scorePercent.value >= 60 ? 'primary' : 'warning')

const reportDescription = computed(() => {
  const d = viewAssessment.value
  if (!d) return ''
  if (d.type === 1) {
    return personalityDescriptions[d.score || ''] || '感谢完成本次性格测评。'
  }
  const pct = scorePercent.value
  const s = parseScore(d.score)
  if (pct >= 80) return `正确率 ${pct}%，${s.correct}/${s.total} 题正确。表现优秀，展现出了扎实的专业知识和出色的解题能力。`
  if (pct >= 60) return `正确率 ${pct}%，${s.correct}/${s.total} 题正确。表现良好，具备较好的基础能力，部分知识点尚有提升空间。`
  return `正确率 ${pct}%，${s.correct}/${s.total} 题正确。建议加强相关领域知识的学习和实践。`
})

const reportStrengths = computed(() => {
  const d = viewAssessment.value
  if (!d || d.type === 1) return []
  const pct = scorePercent.value
  if (d.type === 0) {
    if (pct >= 80) return ['Java/编程基础扎实', '面向对象思想理解到位', 'SQL 与数据库知识掌握良好']
    if (pct >= 60) return ['基本编程概念掌握', '常见数据结构有一定理解']
    return ['已具备入门级编程基础']
  }
  if (pct >= 80) return ['逻辑推理能力优秀', '数列规律识别准确']
  if (pct >= 60) return ['基本逻辑判断正确', '数字规律识别能力中等']
  return ['具备基础逻辑思维能力']
})

const reportWeaknesses = computed(() => {
  const d = viewAssessment.value
  if (!d || d.type === 1) return []
  const pct = scorePercent.value
  if (d.type === 0) {
    if (pct >= 80) return ['高级算法优化可进一步加强', '系统架构设计经验可积累']
    if (pct >= 60) return ['数据结构与算法需加强', '异常处理与边界情况考虑不足']
    return ['编程基础需系统性加强', 'SQL 查询与数据库操作需学习', '数据结构与常用算法需专项训练']
  }
  if (pct >= 80) return ['极复杂场景推理可继续挑战']
  if (pct >= 60) return ['图形空间推理需加强', '复杂数列推导需提升']
  return ['逻辑推理需系统性训练', '数字敏感度需提高']
})

const personalityDescriptions: Record<string, string> = {
  '外向型': '您善于社交沟通，性格开朗外向，适合需要频繁协作的岗位，在团队中能发挥桥梁和润滑剂的作用。',
  '尽责型': '您做事认真负责、有条理，注重细节，适合需要高度自律和细节把控的工作，是团队中可靠的中坚力量。',
  '开放型': '您思维活跃、乐于创新，对新事物充满好奇，适合需要创造力和灵活性的岗位。',
  '平衡型': '您性格均衡，在不同场景下都能展现出良好的适应能力，是团队中多面手型的人才。',
}

const personalityTraits = computed(() => {
  const t = viewAssessment.value?.score || ''
  const map: Record<string, string[]> = {
    '外向型': ['善于沟通表达', '团队协作意识强', '主动性强、反应迅速'],
    '尽责型': ['做事严谨认真', '注重细节和计划', '责任心强、值得信赖'],
    '开放型': ['思维灵活有创意', '乐于接受新事物', '学习能力强'],
    '平衡型': ['性格均衡稳定', '多场景适应力好', '综合素质突出'],
  }
  return map[t] || ['综合表现良好']
})

const personalitySuitable = computed(() => {
  const t = viewAssessment.value?.score || ''
  const map: Record<string, string[]> = {
    '外向型': ['团队主管/项目经理', '客户经理/销售', '人力资源', '市场推广'],
    '尽责型': ['质量保障/测试', '财务/审计', '研究员/分析师', '行政/运营管理'],
    '开放型': ['产品经理/设计师', '研发/架构师', '创新业务负责人'],
    '平衡型': ['综合管理岗位', '技术管理', '咨询顾问'],
  }
  return map[t] || ['多岗位均可胜任']
})

const personalityAdvice = computed(() => {
  const t = viewAssessment.value?.score || ''
  const map: Record<string, string[]> = {
    '外向型': ['适当培养独处深度思考的习惯', '重要决策时先沉淀再行动'],
    '尽责型': ['尝试接受适度的不确定性', '加强跨部门沟通的频率'],
    '开放型': ['需要时做好计划和时间管理', '关注执行落地的细节'],
    '平衡型': ['选择一个方向深耕形成专长', '主动争取挑战性项目锻炼自己'],
  }
  return map[t] || ['持续学习，全面发展']
})

// ---- 初始化：接收 AI 智能出题页传入的题目 ----

onMounted(async () => {
  await loadList()
  // 仅当从「AI智能出题 → 创建为测评」带 fromAi=1 跳转而来时才自动打开新建弹窗
  if (route.query.fromAi !== '1') {
    return
  }
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY)
    if (raw) {
      const parsed = JSON.parse(raw)
      if (parsed?.positionLabel) {
        aiQuestionResult.value = parsed
        createForm.jobTitle = parsed.positionLabel
        createForm.type = 0
        assessmentSource.value = 'ai'
        createVisible.value = true
        // 消费完跳转标记，避免刷新页面再次自动弹窗
        router.replace('/online-assessments')
      }
    }
  } catch {
    aiQuestionResult.value = null
  }
})
</script>

<style scoped>
.ai-source-tip {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 4px;
}
.question-item {
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  padding: 10px 12px;
  margin-bottom: 10px;
}
.question-item-head {
  display: flex;
  gap: 6px;
  font-size: 14px;
}
.q-num {
  font-weight: 600;
  color: var(--c-primary, #4f46e5);
}
.q-opts {
  margin: 6px 0 0 20px;
  color: var(--c-text-secondary);
  font-size: 13px;
  line-height: 1.7;
}
.q-answer {
  margin: 6px 0 0 20px;
  color: #059669;
  font-size: 13px;
  font-weight: 500;
}
.view-report-body { padding: 4px; }
.report-summary {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.summary-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: var(--c-primary, #4f46e5);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 600;
}
.summary-name { font-size: 16px; font-weight: 600; }
.summary-meta { font-size: 13px; color: var(--c-text-secondary); }
.summary-time { font-size: 12px; color: #94a3b8; }
.report-result { text-align: center; }
.report-level { margin: 12px 0; }
.report-desc { font-size: 13px; color: var(--c-text-secondary); line-height: 1.7; text-align: left; margin: 12px 0; }
.report-analysis {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  text-align: left;
}
.analysis-section {
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  padding: 10px 12px;
}
.analysis-section h4 { margin: 0 0 8px; font-size: 13px; }
.analysis-section ul { margin: 0; padding-left: 18px; font-size: 13px; color: var(--c-text-secondary); line-height: 1.8; }
.personality-type-display { padding: 12px 0; }
.pt-label { font-size: 26px; font-weight: 700; color: var(--c-primary, #4f46e5); }
</style>

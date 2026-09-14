<template>
  <div class="question-bank-page">
    <div class="sr-page-header">
      <h1>面试题库</h1>
      <p>按部门与职位维护多套面试题，支持查看题目、选项、答案与解读</p>
    </div>

    <div class="sr-section">
      <div class="sr-toolbar">
        <div class="sr-filter-bar">
          <el-tree-select
            v-model="filters.departmentId"
            :data="deptOptions"
            :props="{ label: 'name', value: 'id', children: 'children' }"
            placeholder="请选择部门"
            check-strictly
            clearable
            style="width: 200px;"
            @change="handleDeptFilterChange"
          />
          <el-select
            v-model="filters.jobTitle"
            placeholder="选择职位"
            clearable
            filterable
            style="width: 200px;"
            :disabled="!filters.departmentId"
            @change="handleSearch"
          >
            <el-option v-for="j in jobFilterOptions" :key="j.id" :label="j.title" :value="j.title" />
          </el-select>
          <el-input
            v-model="filters.keyword"
            placeholder="套题名称"
            clearable
            style="width: 160px;"
            @input="handleSearch"
          />
          <el-select
            v-model="filters.questionType"
            placeholder="套题类型"
            clearable
            style="width: 130px;"
            @change="handleSearch"
          >
            <el-option label="技术面" :value="0" />
            <el-option label="项目面" :value="1" />
            <el-option label="行为/HR面" :value="2" />
            <el-option label="综合面" :value="3" />
          </el-select>
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            clearable
            style="width: 240px;"
            @change="handleSearch"
          />
        </div>
        <el-button type="primary" :icon="Plus" @click="openCreateDialog">新建套题</el-button>
      </div>

      <el-table v-loading="loading" :data="banks" stripe style="width: 100%">
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="bankName" label="套题名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="departmentName" label="部门" width="120" />
        <el-table-column prop="jobTitle" label="职位" min-width="180" show-overflow-tooltip />
        <el-table-column label="套题类型" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="bankTypeType(row.questionType)">{{ bankTypeLabel(row.questionType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="难度" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="difficultyType(row.difficulty)">{{ difficultyLabel(row.difficulty) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="questionCount" label="题数" width="70" align="center" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : row.status === 0 ? 'info' : 'warning'">
              {{ row.status === 1 ? '启用' : row.status === 0 ? '草稿' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" text type="primary" @click="openDetail(row)">查看</el-button>
            <el-button size="small" text type="warning" @click="openEditDialog(row)">编辑</el-button>
            <el-popconfirm title="确定删除该套题吗？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button size="small" text type="danger">删除</el-button>
              </template>
            </el-popconfirm>
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
          @size-change="loadBanks"
          @current-change="loadBanks"
        />
      </div>
    </div>

    <!-- 查看详情抽屉 -->
    <el-drawer v-model="detailVisible" :title="detail?.bankName || '套题详情'" size="640px">
      <template v-if="detail">
        <div class="bank-meta">
          <el-tag size="small">{{ detail.departmentName }}</el-tag>
          <el-tag size="small" type="info">{{ detail.jobTitle }}</el-tag>
          <el-tag size="small" type="warning">{{ bankTypeLabel(detail.questionType) }}</el-tag>
          <el-tag size="small" :type="difficultyType(detail.difficulty)">{{ difficultyLabel(detail.difficulty) }}</el-tag>
          <el-tag size="small" :type="detail.status === 1 ? 'success' : 'info'">
            {{ detail.status === 1 ? '启用' : '草稿' }}
          </el-tag>
        </div>
        <p v-if="detail.description" class="bank-desc">{{ detail.description }}</p>
        <el-divider content-position="left">题目（{{ detail.items?.length || 0 }}）</el-divider>
        <div v-if="!detail.items?.length" class="empty-tip">该套题暂无题目</div>
        <div v-for="(item, idx) in detail.items" :key="item.id" class="question-card">
          <div class="question-head">
            <span class="q-index">{{ idx + 1 }}</span>
            <span class="q-type">{{ itemTypeLabel(item.questionType) }}</span>
            <span class="q-diff">{{ difficultyLabel(item.difficulty) }}</span>
          </div>
          <div class="q-block q-block-question">
            <div class="q-block-label">问题</div>
            <div class="q-block-content">{{ item.question }}</div>
          </div>
          <div v-if="item.options?.length" class="q-block q-block-options">
            <div class="q-block-label">选项</div>
            <div class="q-options">
              <div v-for="(opt, oi) in item.options" :key="oi" class="q-option">{{ opt }}</div>
            </div>
          </div>
          <div v-if="item.answer" class="q-block q-block-answer">
            <div class="q-block-label">答案</div>
            <div class="q-block-content">
              <div v-if="answerOf(item.answer).header" class="answer-header">
                {{ answerOf(item.answer).header }}：
              </div>
              <div
                v-for="(line, li) in answerOf(item.answer).lines"
                :key="li"
                class="answer-line"
              >{{ line }}</div>
            </div>
          </div>
          <div v-if="item.explanation" class="q-block q-block-explanation">
            <div class="q-block-label">解读</div>
            <div class="q-block-content">
              <div v-if="explanationOf(item.explanation).header" class="explanation-header">
                {{ explanationOf(item.explanation).header }}：
              </div>
              <div
                v-for="(line, li) in explanationOf(item.explanation).lines"
                :key="li"
                class="explanation-line"
              >{{ line }}</div>
            </div>
          </div>
        </div>
      </template>
    </el-drawer>

    <!-- 新建/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑套题' : '新建套题'"
      width="820px"
      :close-on-click-modal="false"
      destroy-on-close
      @closed="handleDialogClosed"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="96px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="套题名称" prop="bankName">
              <el-input v-model="form.bankName" placeholder="如：Java后端-技术基础面" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="部门" prop="departmentId">
              <el-tree-select
                v-model="form.departmentId"
                :data="deptOptions"
                :props="{ label: 'name', value: 'id', children: 'children' }"
                placeholder="请选择部门"
                check-strictly
                style="width: 100%;"
                @change="handleDeptChange"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="职位" prop="jobTitle">
              <el-select
                v-model="form.jobTitle"
                filterable
                allow-create
                default-first-option
                placeholder="选择或输入职位"
                style="width: 100%;"
              >
                <el-option v-for="j in jobOptions" :key="j.id" :label="j.title" :value="j.title" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="套题类型">
              <el-select v-model="form.questionType" style="width: 100%;">
                <el-option label="技术面" :value="0" />
                <el-option label="项目面" :value="1" />
                <el-option label="行为/HR面" :value="2" />
                <el-option label="综合面" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="难度">
              <el-select v-model="form.difficulty" style="width: 100%;">
                <el-option label="简单" :value="1" />
                <el-option label="中等" :value="2" />
                <el-option label="困难" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="form.status" style="width: 100%;">
                <el-option label="启用" :value="1" />
                <el-option label="草稿" :value="0" />
                <el-option label="停用" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="套题说明">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="套题适用范围与考察重点（可选）" />
        </el-form-item>

        <div class="ai-switch-row">
          <el-switch v-model="aiEnabled" :disabled="aiGenerating" />
          <span class="ai-switch-label">AI 智能出题</span>
          <span class="ai-switch-tip">勾选后可按下方参数自动生成题目并填充到题目列表</span>
        </div>

        <div v-if="aiEnabled" class="ai-params">
          <el-row :gutter="12">
            <el-col :span="8">
              <el-form-item label="难度">
                <el-select v-model="aiParams.difficulty" style="width: 100%">
                  <el-option label="简单" value="easy" />
                  <el-option label="中等" value="medium" />
                  <el-option label="困难" value="hard" />
                  <el-option label="混合" value="mixed" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="16">
              <el-form-item label="题量分配">
                <div class="ai-count-grid">
                  <div class="ai-count-item">
                    <span>技术基础题</span>
                    <el-input-number v-model="aiParams.tech" :min="0" :max="10" size="small" />
                  </div>
                  <div class="ai-count-item">
                    <span>项目经验题</span>
                    <el-input-number v-model="aiParams.project" :min="0" :max="10" size="small" />
                  </div>
                  <div class="ai-count-item">
                    <span>行为面试题</span>
                    <el-input-number v-model="aiParams.behavioral" :min="0" :max="10" size="small" />
                  </div>
                </div>
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="技术题题型">
            <div class="ai-tech-types">
              <div v-for="tt in techTypeOptions" :key="tt.value" class="ai-tech-type">
                <el-checkbox v-model="tt.checked" />
                <span class="ai-tech-label">{{ tt.label }}</span>
                <el-input-number v-model="tt.count" :min="0" :max="10" size="small" :disabled="!tt.checked" />
              </div>
            </div>
          </el-form-item>
        </div>

        <div class="ai-generate-header">
          <span class="ai-generate-title">题目列表（{{ form.items.length }}）</span>
          <template v-if="aiEnabled">
            <el-button v-if="!aiGenerating" type="primary" plain :icon="MagicStick" @click="handleAiGenerate">
              AI 智能出题
            </el-button>
            <div v-else class="ai-progress">
              <div class="ai-progress-steps">
                <div
                  v-for="(step, i) in aiSteps"
                  :key="i"
                  class="ai-step"
                  :class="aiStepClass(i)"
                >
                  <el-icon><component :is="aiStepIcon(i)" /></el-icon>
                  <span>{{ step }}</span>
                </div>
              </div>
              <el-progress
                :percentage="aiProgress"
                :stroke-width="8"
                :status="aiProgress >= 100 ? 'success' : undefined"
                style="width: 240px;"
              />
              <div class="ai-progress-meta">
                <span>{{ aiPhaseText() }}</span>
                <span>已耗时 {{ aiElapsed }}s</span>
              </div>
            </div>
          </template>
        </div>
        <div v-for="(item, idx) in form.items" :key="idx" class="item-editor">
          <div class="item-editor-head">
            <span class="q-index">{{ idx + 1 }}</span>
            <el-select v-model="item.questionType" style="width: 110px;">
              <el-option label="单选" :value="0" />
              <el-option label="多选" :value="1" />
              <el-option label="判断" :value="3" />
              <el-option label="问答" :value="2" />
            </el-select>
            <el-select v-model="item.difficulty" style="width: 90px;">
              <el-option label="简单" :value="1" />
              <el-option label="中等" :value="2" />
              <el-option label="困难" :value="3" />
            </el-select>
            <el-button text type="danger" @click="removeItem(idx)">删除</el-button>
          </div>
          <el-input v-model="item.question" type="textarea" :rows="2" placeholder="请输入题目内容" />
          <template v-if="item.questionType !== 2">
            <div v-for="(opt, oi) in item.options" :key="oi" class="option-row">
              <span class="option-label">{{ optionLetter(oi) }}</span>
              <el-input v-model="item.options![oi]" placeholder="请输入选项内容" />
              <el-button text type="danger" @click="removeOption(item, oi)">移除</el-button>
            </div>
            <el-button size="small" text type="primary" @click="addOption(item)">+ 添加选项</el-button>
            <el-form-item label="答案" style="margin: 8px 0 0;">
              <el-input v-model="item.answer" :placeholder="item.questionType === 1 ? '多个答案用逗号分隔，如 A,B' : '如 A'" />
            </el-form-item>
          </template>
          <template v-else>
            <el-form-item label="参考要点" style="margin: 8px 0 0;">
              <el-input v-model="item.answer" type="textarea" :rows="2" placeholder="答案参考要点" />
            </el-form-item>
          </template>
          <el-form-item label="解读" style="margin: 8px 0 0;">
            <el-input v-model="item.explanation" type="textarea" :rows="2" placeholder="题目解读 / 考察点说明" />
          </el-form-item>
        </div>
        <el-button type="primary" plain :icon="Plus" @click="addItem">添加题目</el-button>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">{{ editingId ? '保存修改' : '创建套题' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Plus, MagicStick, CircleCheck, Loading, Clock } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { getDepartments } from '@/api/system'
import { getJobs } from '@/api/job'
import { generateQuestions, getGenerateTaskResult } from '@/api/interview'
import {
  getQuestionBanks, getQuestionBank, createQuestionBank, updateQuestionBank, deleteQuestionBank,
  type QuestionBankVO, type QuestionBankItemVO,
} from '@/api/questionBank'
import { formatDateTime } from '@/utils/format'
import type { DepartmentTreeVO, JobVO, QuestionGenerateResult } from '@/types/models'

const loading = ref(false)
const submitting = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const banks = ref<QuestionBankVO[]>([])
const deptOptions = ref<DepartmentTreeVO[]>([])
const jobOptions = ref<JobVO[]>([])
const jobFilterOptions = ref<JobVO[]>([])
const dateRange = ref<[string, string] | null>(null)
const filters = reactive<Record<string, unknown>>({
  departmentId: undefined,
  jobTitle: undefined,
  keyword: undefined,
  questionType: undefined,
})

const detailVisible = ref(false)
const detail = ref<QuestionBankVO | null>(null)
const dialogVisible = ref(false)
const editingId = ref<string | null>(null)
const formRef = ref<FormInstance>()

const form = reactive({
  bankName: '',
  departmentId: '',
  jobTitle: '',
  questionType: 0,
  difficulty: 2,
  description: '',
  status: 1,
  items: [] as QuestionBankItemVO[],
})

const formRules: FormRules = {
  bankName: [{ required: true, message: '请输入套题名称', trigger: 'blur' }],
  departmentId: [{ required: true, message: '请选择部门', trigger: 'change' }],
  jobTitle: [{ required: true, message: '请选择或输入职位', trigger: 'change' }],
}

const aiEnabled = ref(false)
const aiGenerating = ref(false)
const aiProgress = ref(0)
const aiElapsed = ref(0)
/** 0=创建任务, 1=AI 生成中, 2=解析结果。 */
const aiPhase = ref(0)
const aiSteps = ['创建任务', 'AI 生成中', '解析结果']
let aiPollTimer: ReturnType<typeof setInterval> | null = null
let aiTickTimer: ReturnType<typeof setInterval> | null = null

const aiParams = reactive({
  difficulty: 'medium',
  tech: 3,
  project: 2,
  behavioral: 2,
})

const techTypeOptions = reactive([
  { label: '单选题', value: 'single_choice', checked: true, count: 2 },
  { label: '多选题', value: 'multiple_choice', checked: true, count: 2 },
  { label: '判断题', value: 'true_false', checked: true, count: 1 },
  { label: '问答题', value: 'essay', checked: false, count: 0 },
])

function bankTypeLabel(t: number): string {
  return { 0: '技术面', 1: '项目面', 2: '行为/HR面', 3: '综合面' }[t] || '综合面'
}
function bankTypeType(t: number): 'primary' | 'success' | 'warning' | 'info' {
  return ({ 0: 'primary', 1: 'success', 2: 'warning', 3: 'info' } as Record<number, 'primary' | 'success' | 'warning' | 'info'>)[t] || 'info'
}
function itemTypeLabel(t: number): string {
  return { 0: '单选', 1: '多选', 2: '问答', 3: '判断' }[t] || '单选'
}
function difficultyLabel(d: number): string {
  return { 1: '简单', 2: '中等', 3: '困难' }[d] || '中等'
}
function difficultyType(d: number): 'success' | 'warning' | 'danger' | 'info' {
  return ({ 1: 'success', 2: 'warning', 3: 'danger' } as Record<number, 'success' | 'warning' | 'danger' | 'info'>)[d] || 'info'
}
function optionLetter(i: number): string {
  return String.fromCharCode(65 + i)
}

/** 把 "；1." / "，2、" 等编号要点转为换行。 */
function splitPoints(text: string | undefined): string {
  return (text || '').replace(/[；;，,]\s*(?=\d+[.、])/g, '\n')
}

/** 答案内容解析：提取「参考答案要点」等标题，并将编号要点转为独立行。 */
function answerOf(text: string | undefined): { header: string; lines: string[] } {
  let t = (text || '').trim()
  if (!t) return { header: '', lines: [] }
  // 简单答案（单选/多选选项键，如 B、A,B,C,D）原样展示，不做要点拆分
  if (/^[A-Ea-e](,[A-Ea-e])*$/.test(t)) {
    return { header: '', lines: [t] }
  }
  let header = ''
  const m = t.match(/^(参考答案要点|参考要点|答案要点|关键得分点|参考答案)[：:]\s*/)
  if (m) {
    header = m[1]
    t = t.slice(m[0].length)
  }
  return {
    header,
    lines: splitPoints(t).split('\n').map(s => s.trim()).filter(Boolean),
  }
}

/** 解读内容解析：提取「关键得分点」标题与逐条要点。 */
function explanationOf(text: string | undefined): { header: string; lines: string[] } {
  let t = (text || '').trim()
  let header = ''
  const m = t.match(/^关键得分点[：:]\s*/)
  if (m) {
    header = '关键得分点'
    t = t.slice(m[0].length)
  }
  return {
    header,
    lines: splitPoints(t).split('\n').map(s => s.trim()).filter(Boolean),
  }
}

async function loadBanks() {
  loading.value = true
  try {
    const res = await getQuestionBanks({
      page: page.value, size: size.value,
      ...filters,
      startDate: dateRange.value?.[0] || undefined,
      endDate: dateRange.value?.[1] || undefined,
    })
    banks.value = res.records
    total.value = res.total
  } catch {
    banks.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  loadBanks()
}

/** 切换部门筛选：清空职位选择并加载该部门职位，然后查询。 */
async function handleDeptFilterChange(deptId: string) {
  filters.jobTitle = undefined
  jobFilterOptions.value = []
  page.value = 1
  if (deptId) {
    try {
      const res = await getJobs({ size: 100, departmentId: deptId } as Record<string, unknown>)
      jobFilterOptions.value = res.records || []
    } catch {
      jobFilterOptions.value = []
    }
  }
  loadBanks()
}

async function loadDepartments() {
  try {
    deptOptions.value = await getDepartments()
  } catch {
    deptOptions.value = []
  }
}

async function handleDeptChange(deptId: string) {
  form.jobTitle = ''
  jobOptions.value = []
  if (!deptId) return
  try {
    const res = await getJobs({ size: 100, departmentId: deptId } as Record<string, unknown>)
    jobOptions.value = res.records || []
  } catch {
    jobOptions.value = []
  }
}

function findDeptName(id: string, nodes: DepartmentTreeVO[] = deptOptions.value): string {
  if (!id) return ''
  for (const n of nodes) {
    if (String(n.id) === String(id)) return n.name
    if (n.children?.length) {
      const found = findDeptName(id, n.children)
      if (found) return found
    }
  }
  return ''
}

async function openDetail(row: QuestionBankVO) {
  detail.value = null
  detailVisible.value = true
  try {
    detail.value = await getQuestionBank(row.id)
  } catch {
    detail.value = row
  }
}

function openCreateDialog() {
  editingId.value = null
  Object.assign(form, {
    bankName: '', departmentId: '', jobTitle: '', questionType: 0,
    difficulty: 2, description: '', status: 1, items: [newItem()],
  })
  jobOptions.value = []
  dialogVisible.value = true
}

async function openEditDialog(row: QuestionBankVO) {
  editingId.value = row.id
  const data = await getQuestionBank(row.id)
  Object.assign(form, {
    bankName: data.bankName,
    departmentId: String(data.departmentId),
    jobTitle: data.jobTitle,
    questionType: data.questionType ?? 0,
    difficulty: data.difficulty ?? 2,
    description: data.description || '',
    status: data.status ?? 1,
    items: (data.items || []).map(it => ({
      questionType: it.questionType ?? 0,
      question: it.question,
      options: it.options ? [...it.options] : [],
      answer: it.answer || '',
      explanation: it.explanation || '',
      difficulty: it.difficulty ?? 2,
    })),
  })
  await handleDeptChange(String(data.departmentId))
  dialogVisible.value = true
}

function newItem(): QuestionBankItemVO {
  return { questionType: 0, question: '', options: ['', '', '', ''], answer: '', explanation: '', difficulty: 2 }
}
function addItem() {
  form.items.push(newItem())
}
function removeItem(idx: number) {
  form.items.splice(idx, 1)
}
function addOption(item: QuestionBankItemVO) {
  if (!item.options) item.options = []
  item.options.push('')
}
function removeOption(item: QuestionBankItemVO, idx: number) {
  item.options?.splice(idx, 1)
}

/** 关闭弹窗时清理 AI 轮询定时器。 */
function handleDialogClosed() {
  clearAiTimers()
  aiGenerating.value = false
  aiProgress.value = 0
  aiElapsed.value = 0
  aiPhase.value = 0
}

function clearAiTimers() {
  if (aiPollTimer) {
    clearInterval(aiPollTimer)
    aiPollTimer = null
  }
  if (aiTickTimer) {
    clearInterval(aiTickTimer)
    aiTickTimer = null
  }
}

function aiStepClass(i: number): string {
  if (aiPhase.value === 2) return 'done'
  if (i < aiPhase.value) return 'done'
  if (i === aiPhase.value) return 'active'
  return 'pending'
}

function aiStepIcon(i: number) {
  if (aiPhase.value === 2 || i < aiPhase.value) return CircleCheck
  if (i === aiPhase.value) return Loading
  return Clock
}

function aiPhaseText(): string {
  if (aiPhase.value === 2) return '解析完成，正在填充题目'
  if (aiPhase.value === 1) return 'AI 正在生成题目...'
  return '正在创建出题任务...'
}

/** 点击「AI 智能出题」：按参数异步生成题目并填充到下方题目列表。 */
async function handleAiGenerate() {
  if (!form.departmentId) {
    ElMessage.warning('请先选择部门')
    return
  }
  if (!form.jobTitle?.trim()) {
    ElMessage.warning('请先填写职位')
    return
  }
  const posType = inferPositionType(form.jobTitle)
  if (posType === null) {
    ElMessage.warning('无法识别职位方向（前端/后端/算法/产品/运维/数据/测试），请调整职位名称')
    return
  }

  const categories: string[] = []
  const counts: Record<string, number> = {}
  if (aiParams.tech > 0) { categories.push('tech'); counts.tech = aiParams.tech }
  if (aiParams.project > 0) { categories.push('project'); counts.project = aiParams.project }
  if (aiParams.behavioral > 0) { categories.push('behavioral'); counts.behavioral = aiParams.behavioral }
  if (!categories.length) {
    ElMessage.warning('请至少设置一类题量')
    return
  }

  const techQuestionTypes: Record<string, number> = {}
  techTypeOptions.forEach(tt => {
    if (tt.checked && tt.count > 0) techQuestionTypes[tt.value] = tt.count
  })

  aiGenerating.value = true
  aiProgress.value = 8
  aiPhase.value = 0
  aiElapsed.value = 0
  try {
    const { taskId } = await generateQuestions({
      positionType: posType,
      difficultyLevel: aiParams.difficulty,
      categories,
      categoryQuestionCounts: counts,
      ...(Object.keys(techQuestionTypes).length ? { techQuestionTypes } : {}),
    })
    aiPhase.value = 1
    aiProgress.value = 35
    aiTickTimer = setInterval(() => { aiElapsed.value += 1 }, 1000)
    aiPollTimer = setInterval(async () => {
      aiProgress.value = Math.min(85, aiProgress.value + 8)
      try {
        const task = await getGenerateTaskResult(taskId)
        if (task.status === 'COMPLETED') {
          clearAiTimers()
          aiPhase.value = 2
          aiProgress.value = 100
          const items = convertToBankQuestions(task.result)
          if (!items.length) {
            aiGenerating.value = false
            ElMessage.warning('AI 未生成有效题目，请重试')
            return
          }
          form.items = items
          ElMessage.success(`AI 生成 ${items.length} 道题目，可修改后保存`)
          setTimeout(() => { aiGenerating.value = false }, 700)
        } else if (task.status === 'FAILED' || task.status === 'NOT_FOUND') {
          clearAiTimers()
          aiGenerating.value = false
          aiProgress.value = 0
          ElMessage.error(task.errorMessage || 'AI 出题失败，请稍后重试')
        }
      } catch {
        clearAiTimers()
        aiGenerating.value = false
        aiProgress.value = 0
        ElMessage.error('查询出题状态失败，请稍后重试')
      }
    }, 2000)
  } catch {
    aiGenerating.value = false
    aiProgress.value = 0
    ElMessage.error('创建出题任务失败，请稍后重试')
  }
}

/** 根据职位名称推导职位方向类型：0=前端,1=后端,2=AI/算法,3=产品,4=运维,5=数据,8=测试。 */
function inferPositionType(jobTitle: string): number | null {
  const title = jobTitle.toLowerCase()
  if (/前端|web|react|vue|angular|h5|ui|flutter|小程序|ios|android|移动端/i.test(title)) return 0
  if (/后端|java|golang|go|python|php|node|rust|c#|服务端|中间件/i.test(title)) return 1
  if (/ai|算法|机器学|深度学|nlp|自然语言|cv|计算机视觉|大模型|llm|人工智能|推荐/i.test(title)) return 2
  if (/产品|经理|pm/i.test(title)) return 3
  if (/运维|devops|sre|k8s|kubernetes|docker|ci|cd|云平台|基础设施|安全/i.test(title)) return 4
  if (/数据|etl|大数据|数仓|spark|hadoop|flink|分析/i.test(title)) return 5
  if (/测试|qa|质量|测开|自动化测试|接口测试|性能测试|软件测试/i.test(title)) return 8
  return null
}

/** 将 AI 出题结果转换为题库题目（技术基础/项目经验/行为面试三类）。 */
function convertToBankQuestions(result: QuestionGenerateResult | null | undefined): QuestionBankItemVO[] {
  if (!result) return []
  const items: QuestionBankItemVO[] = []
  const categories = [
    { label: '技术基础题', list: result.techQuestions },
    { label: '项目经验题', list: result.projectQuestions },
    { label: '行为面试题', list: result.behavioralQuestions },
  ]
  for (const cat of categories) {
    if (!cat.list) continue
    for (const item of cat.list) {
      const qt = item.questionType || 'essay'
      items.push({
        questionType: bankQuestionTypeCode(qt),
        question: `${cat.label}：${item.question}`,
        options: item.options?.length ? [...item.options] : [],
        answer: extractAnswerKey(qt, item.referenceAnswer || '', parseOptions(item.options || [])),
        explanation: item.referenceAnswer || '',
        difficulty: item.difficultyCode || 2,
      })
    }
  }
  return items
}

/** AI 题型字符串 → 题库题型编码：0=单选,1=多选,2=问答,3=判断。 */
function bankQuestionTypeCode(qt: string): number {
  if (qt === 'single_choice') return 0
  if (qt === 'multiple_choice') return 1
  if (qt === 'true_false') return 3
  return 2
}

/** 解析 "A. xxx" 选项为 {key, value}，供答案提取使用。 */
function parseOptions(options: string[]): { key: string; value: string }[] {
  return options.map(opt => {
    const m = opt.match(/^([A-Ea-e])[\.\)、]\s*(.+)/)
    if (m) return { key: m[1].toUpperCase(), value: m[2] }
    return { key: '', value: opt }
  })
}

/** 从 AI 参考答案中提取标准答案（判断题/单选/多选/问答）。 */
function extractAnswerKey(questionType: string, referenceAnswer: string, options: { key: string; value: string }[]): string {
  if (!referenceAnswer) return ''
  const ref = referenceAnswer.trim()

  if (questionType === 'true_false') {
    if (ref.startsWith('正确') || ref.toLowerCase().startsWith('true')) return 'A'
    if (ref.startsWith('错误') || ref.toLowerCase().startsWith('false')) return 'B'
    const correctOpt = options.find(o => o.value.includes('正确') || o.value.toLowerCase().includes('true'))
    if (correctOpt) return correctOpt.key
    return ref.length <= 2 ? ref.toUpperCase() : ''
  }

  if (questionType === 'single_choice') {
    const singleMatch = ref.match(/^([A-Ea-e])[，,、\s]/)
    if (singleMatch) return singleMatch[1].toUpperCase()
    for (const opt of options) {
      if (ref.startsWith(opt.value) || opt.value && ref.includes(opt.key + '.')) return opt.key
    }
    return ref.length <= 2 ? ref.toUpperCase() : ref
  }

  if (questionType === 'multiple_choice') {
    const multiMatch = ref.match(/^([A-Ea-e][、,，\s]*)+/)
    if (multiMatch) {
      const keys = multiMatch[0].match(/[A-Ea-e]/g)
      if (keys) return keys.map(k => k.toUpperCase()).join(',')
    }
    return ref
  }
  return ref
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const payload = {
        ...form,
        departmentName: findDeptName(form.departmentId),
        items: form.items
          .filter(it => it.question?.trim())
          .map(it => ({ ...it, options: it.questionType === 2 ? undefined : (it.options || []).filter(o => o.trim()) })),
      }
      if (editingId.value) {
        await updateQuestionBank(editingId.value, payload)
        ElMessage.success('套题已更新')
      } else {
        await createQuestionBank(payload)
        ElMessage.success('套题已创建')
      }
      dialogVisible.value = false
      loadBanks()
    } catch {
      ElMessage.error('保存失败，请检查题目是否填写完整')
    } finally {
      submitting.value = false
    }
  })
}

async function handleDelete(id: string) {
  try {
    await deleteQuestionBank(id)
    ElMessage.success('删除成功')
    loadBanks()
  } catch {
    ElMessage.error('删除失败')
  }
}

onMounted(async () => {
  await Promise.all([loadBanks(), loadDepartments()])
})
</script>

<style scoped>
.bank-meta {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}
.bank-desc {
  color: var(--c-text-secondary);
  font-size: 13px;
  margin: 4px 0 0;
}
.empty-tip {
  color: var(--c-text-muted);
  text-align: center;
  padding: 24px 0;
}
.question-card {
  border: 1px solid var(--c-border-light);
  border-radius: 8px;
  padding: 12px 14px;
  margin-bottom: 12px;
}
.question-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.q-index {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: var(--c-primary-bg, #eef2ff);
  color: var(--c-primary, #4f46e5);
  font-size: 12px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.q-type {
  font-size: 12px;
  color: var(--c-primary);
  background: var(--c-primary-bg, #eef2ff);
  padding: 1px 8px;
  border-radius: 10px;
}
.q-diff {
  font-size: 12px;
  color: var(--c-text-muted);
}
/* ---- 问题/选项/答案/解读 分区块 ---- */
.q-block {
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  padding: 8px 10px;
  margin-top: 8px;
}
.q-block-label {
  display: inline-block;
  font-size: 12px;
  font-weight: 600;
  padding: 1px 9px;
  border-radius: 10px;
  margin-bottom: 6px;
}
.q-block-content {
  font-size: 13px;
  color: var(--c-text);
  line-height: 1.7;
  white-space: pre-wrap;
}
.q-block-question {
  background: #f5f7ff;
  border-color: #e0e7ff;
}
.q-block-question .q-block-label {
  background: #eef2ff;
  color: #4f46e5;
}
.q-block-options {
  background: #f8fafc;
}
.q-block-options .q-block-label {
  background: #e2e8f0;
  color: #475569;
}
.q-block-options .q-option {
  font-size: 13px;
  color: var(--c-text-secondary);
  line-height: 1.8;
}
.q-block-answer {
  background: #ecfdf5;
  border-color: #a7f3d0;
}
.q-block-answer .q-block-label {
  background: #d1fae5;
  color: #059669;
}
.q-block-answer .answer-text {
  color: #047857;
  font-weight: 500;
}
.q-block-answer .answer-header {
  font-weight: 600;
  color: #047857;
  margin-bottom: 2px;
}
.q-block-answer .answer-line {
  color: #047857;
  line-height: 1.8;
}
.q-block-explanation {
  background: #fffbeb;
  border-color: #fde68a;
}
.q-block-explanation .q-block-label {
  background: #fef3c7;
  color: #b45309;
}
.q-block-explanation .explanation-header {
  font-weight: 600;
  color: #b45309;
  margin-bottom: 2px;
}
.q-block-explanation .explanation-line {
  color: #92400e;
  line-height: 1.8;
}
.item-editor {
  border: 1px dashed var(--c-border-light);
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;
}
.item-editor-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.ai-generate-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 18px 0 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--c-border-light);
}
.ai-generate-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}
.ai-switch-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 4px 0 12px;
  padding: 10px 12px;
  background: #f8fafc;
  border: 1px solid var(--c-border-light);
  border-radius: 8px;
}
.ai-switch-label {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
}
.ai-switch-tip {
  font-size: 12px;
  color: #94a3b8;
}
.ai-params {
  margin-bottom: 12px;
  padding: 4px 12px 0;
  background: #f8fafc;
  border: 1px solid var(--c-border-light);
  border-radius: 8px;
}
.ai-count-grid {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}
.ai-count-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #475569;
}
.ai-tech-types {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}
.ai-tech-type {
  display: flex;
  align-items: center;
  gap: 4px;
}
.ai-tech-label {
  font-size: 13px;
  color: #475569;
}
.ai-progress {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
}
.ai-progress-steps {
  display: flex;
  align-items: center;
  gap: 14px;
}
.ai-step {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #94a3b8;
}
.ai-step.active {
  color: #3b82f6;
  font-weight: 600;
}
.ai-step.done {
  color: #10b981;
}
.ai-progress-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 240px;
  font-size: 12px;
  color: #64748b;
}
.option-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 6px 0;
}
.option-label {
  width: 20px;
  font-weight: 600;
  color: var(--c-text-secondary);
}
</style>

<template>
  <div class="jd-page" v-loading="loading">
    <!-- Top Bar -->
    <div class="jd-topbar">
      <el-button text :icon="ArrowLeft" @click="$router.back()" class="jd-back-btn">返回职位列表</el-button>
      <div class="jd-topbar-actions">
        <el-button :icon="Edit" @click="$router.push('/jobs')" v-if="jobData">编辑职位</el-button>
      </div>
    </div>

    <div v-if="jobData" class="jd-content">
      <!-- Header Card -->
      <div class="jd-header-card">
        <div class="jd-header-top">
          <div class="jd-title-row">
            <h1 class="jd-title">{{ jobData.title }}</h1>
            <el-tag :type="jobData.urgency === 1 || jobData.urgency === 2 ? 'danger' : ''" size="large" effect="dark" v-if="jobData.urgency === 1 || jobData.urgency === 2">
              <el-icon style="margin-right: 4px;"><WarningFilled /></el-icon>急聘
            </el-tag>
          </div>
          <el-tag :type="statusTagType" size="large" effect="plain" class="jd-status-tag">{{ statusLabel }}</el-tag>
        </div>

        <div class="jd-meta-row">
          <span class="jd-meta-item"><el-icon><OfficeBuilding /></el-icon>{{ jobData.departmentName || getDepartmentLabel(jobData.departmentId) }}</span>
          <span class="jd-meta-divider">·</span>
          <span class="jd-meta-item"><el-icon><Location /></el-icon>{{ jobData.location }}</span>
          <span class="jd-meta-divider">·</span>
          <span class="jd-meta-item"><el-icon><Money /></el-icon>{{ formatSalary(jobData.salaryMin) }}-{{ formatSalary(jobData.salaryMax) }}</span>
          <span class="jd-meta-divider">·</span>
          <span class="jd-meta-item"><el-icon><Clock /></el-icon>{{ getExperienceLabel(jobData.level) }}</span>
          <span class="jd-meta-divider">·</span>
          <span class="jd-meta-item"><el-icon><Briefcase /></el-icon>{{ getJobTypeLabel(jobData.type) }}</span>
        </div>

        <!-- Quick Stats -->
        <div class="jd-stats-row">
          <div class="jd-stat-card">
            <div class="jd-stat-value">{{ jobData.headCount || '-' }}</div>
            <div class="jd-stat-label">招聘人数</div>
          </div>
          <div class="jd-stat-card">
            <div class="jd-stat-value">{{ jobData.applicationCount || 0 }}</div>
            <div class="jd-stat-label">已申请</div>
          </div>
          <div class="jd-stat-card">
            <div class="jd-stat-value">{{ formatDate(jobData.publishedAt) }}</div>
            <div class="jd-stat-label">发布时间</div>
          </div>
          <div class="jd-stat-card">
            <div class="jd-stat-value">{{ formatDate(jobData.updatedAt) }}</div>
            <div class="jd-stat-label">最近更新</div>
          </div>
        </div>
      </div>

      <!-- Body: Description + Sidebar -->
      <div class="jd-body">
        <!-- Main: Markdown Description -->
        <div class="jd-main">
          <div class="jd-section">
            <h2 class="jd-section-title">职位详情</h2>
            <div class="jd-description" v-html="renderedDescription"></div>
          </div>

          <!-- Skills -->
          <div class="jd-section" v-if="jobData.skills && jobData.skills.length">
            <h2 class="jd-section-title">技能要求</h2>
            <div class="jd-skills">
              <el-tag v-for="sk in jobData.skills" :key="sk" size="large" effect="plain" class="jd-skill-tag">{{ sk }}</el-tag>
            </div>
          </div>
        </div>

        <!-- Sidebar -->
        <div class="jd-sidebar">
          <div class="jd-sidebar-card">
            <h3 class="jd-sidebar-title">职位信息</h3>
            <div class="jd-sidebar-item">
              <span class="jd-sidebar-key">部门</span>
              <span class="jd-sidebar-val">{{ jobData.departmentName || getDepartmentLabel(jobData.departmentId) }}</span>
            </div>
            <div class="jd-sidebar-item">
              <span class="jd-sidebar-key">工作地点</span>
              <span class="jd-sidebar-val">{{ jobData.location }}</span>
            </div>
            <div class="jd-sidebar-item">
              <span class="jd-sidebar-key">经验要求</span>
              <span class="jd-sidebar-val">{{ getExperienceLabel(jobData.level) }}</span>
            </div>
            <div class="jd-sidebar-item" v-if="jobData.educationRequired != null">
              <span class="jd-sidebar-key">学历要求</span>
              <span class="jd-sidebar-val">{{ getEducationLabel(jobData.educationRequired) }}</span>
            </div>
            <div class="jd-sidebar-item" v-if="jobData.ageMin != null || jobData.ageMax != null">
              <span class="jd-sidebar-key">年龄要求</span>
              <span class="jd-sidebar-val">{{ formatAgeRange(jobData.ageMin, jobData.ageMax) }}</span>
            </div>
            <div class="jd-sidebar-item">
              <span class="jd-sidebar-key">工作类型</span>
              <span class="jd-sidebar-val">{{ getJobTypeLabel(jobData.type) }}</span>
            </div>
            <div class="jd-sidebar-item">
              <span class="jd-sidebar-key">薪资范围</span>
              <span class="jd-sidebar-val salary-val">{{ formatSalary(jobData.salaryMin) }}-{{ formatSalary(jobData.salaryMax) }}</span>
            </div>
            <div class="jd-sidebar-item">
              <span class="jd-sidebar-key">招聘人数</span>
              <span class="jd-sidebar-val">{{ jobData.headCount || '-' }}人</span>
            </div>
            <div class="jd-sidebar-item">
              <span class="jd-sidebar-key">申请人数</span>
              <span class="jd-sidebar-val">{{ jobData.applicationCount || 0 }}人</span>
            </div>
            <div class="jd-sidebar-item">
              <span class="jd-sidebar-key">发布时间</span>
              <span class="jd-sidebar-val">{{ formatDate(jobData.publishedAt) }}</span>
            </div>
            <div class="jd-sidebar-item">
              <span class="jd-sidebar-key">职位状态</span>
              <span class="jd-sidebar-val">
                <el-tag :type="statusTagType" size="small">{{ statusLabel }}</el-tag>
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Empty State -->
    <div v-if="!jobData && !loading" class="jd-empty">
      <el-empty description="职位不存在或已删除" />
      <el-button type="primary" @click="$router.push('/jobs')">返回职位列表</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowLeft, Edit, OfficeBuilding, Location, Money, Clock,
  Briefcase, WarningFilled,
} from '@element-plus/icons-vue'
import {
  getDepartmentLabel, getExperienceLabel, getJobTypeLabel,
  formatDate as fmtDate,
} from '@/utils/format'
import { getJobById } from '@/api/job'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const jobData = ref<Record<string, unknown> | null>(null)

const STATUS_LABELS: Record<number, string> = { 0: '草稿', 1: '已发布', 2: '已暂停', 3: '已关闭' }
const STATUS_TYPES: Record<number, string> = { 0: 'info', 1: 'success', 2: 'warning', 3: 'info' }

const statusLabel = computed(() => {
  const s = jobData.value?.status as number
  return STATUS_LABELS[s] || String(s)
})
const statusTagType = computed(() => {
  const s = jobData.value?.status as number
  return (STATUS_TYPES[s] || 'info') as 'success' | 'warning' | 'danger' | 'info' | ''
})

function formatDate(date: unknown): string {
  if (!date) return '-'
  return fmtDate(String(date))
}

function formatSalary(val: unknown): string {
  const n = Number(val)
  if (!n) return '-'
  if (n >= 1000) return `${(n / 1000).toFixed(0)}K`
  return `${n}`
}

const EDU_LABELS: Record<number, string> = { 0: '高中及以上', 1: '大专及以上', 2: '本科及以上', 3: '硕士及以上', 4: '博士' }
function getEducationLabel(val: unknown): string {
  const code = Number(val)
  return EDU_LABELS[code] || '-'
}

function formatAgeRange(min: unknown, max: unknown): string {
  const m = Number(min) || null
  const x = Number(max) || null
  if (m && x) return `${m}-${x}岁`
  if (m) return `${m}岁以上`
  if (x) return `${x}岁以下`
  return '-'
}

function renderMarkdown(md: string): string {
  if (!md) return ''
  const lines = md.split('\n')
  const blocks: string[] = []
  let i = 0

  while (i < lines.length) {
    const trimmed = lines[i].trim()
    if (!trimmed) { i++; continue }

    // Heading
    if (trimmed.startsWith('## ')) {
      blocks.push(`<h3>${escapeHtml(trimmed.slice(3))}</h3>`)
      i++
      continue
    }

    // Ordered list (consecutive numbered items)
    if (/^\d+\.\s/.test(trimmed)) {
      let olItems = ''
      while (i < lines.length) {
        const t = lines[i].trim()
        const m = t.match(/^(\d+)\.\s+(.*)/)
        if (m) {
          olItems += `<li>${escapeHtml(m[2])}</li>`
          i++
        } else if (!t) {
          i++
        } else {
          break
        }
      }
      blocks.push(`<ol>${olItems}</ol>`)
      continue
    }

    // Unordered list (consecutive dash items)
    if (trimmed.startsWith('- ')) {
      let ulItems = ''
      while (i < lines.length) {
        const t = lines[i].trim()
        if (t.startsWith('- ')) {
          ulItems += `<li>${escapeHtml(t.slice(2))}</li>`
          i++
        } else if (!t) {
          i++
        } else {
          break
        }
      }
      blocks.push(`<ul>${ulItems}</ul>`)
      continue
    }

    // Paragraph
    blocks.push(`<p>${escapeHtml(trimmed)}</p>`)
    i++
  }

  return blocks.join('\n')
}

function escapeHtml(text: string): string {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
}

const renderedDescription = computed(() => {
  const desc = (jobData.value?.description as string) || ''
  return renderMarkdown(desc)
})

async function loadDetail() {
  loading.value = true
  try {
    const id = String(route.params.id)
    const detail = await getJobById(id) as Record<string, unknown>
    jobData.value = detail
  } catch {
    jobData.value = null
  } finally {
    loading.value = false
  }
}

onMounted(() => { loadDetail() })
</script>

<style scoped>
.jd-page {
  max-width: 1100px;
  margin: 0;
  padding: 0 0 40px;
}

/* Top Bar */
.jd-topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  margin-bottom: 8px;
}
.jd-back-btn {
  font-size: 14px;
  color: #606266;
}
.jd-back-btn:hover {
  color: #409eff;
}

/* Header Card */
.jd-header-card {
  background: #fff;
  border-radius: 12px;
  padding: 32px 36px 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  margin-bottom: 20px;
}
.jd-header-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}
.jd-title-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}
.jd-title {
  font-size: 26px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0;
  line-height: 1.3;
  letter-spacing: -0.5px;
}
.jd-status-tag {
  flex-shrink: 0;
  margin-top: 4px;
}

/* Meta Row */
.jd-meta-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
  color: #606266;
  font-size: 14px;
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
}
.jd-meta-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.jd-meta-item .el-icon {
  font-size: 15px;
}
.jd-meta-divider {
  margin: 0 6px;
  color: #d0d0d0;
}

/* Stats Row */
.jd-stats-row {
  display: flex;
  gap: 32px;
}
.jd-stat-card {
  text-align: center;
}
.jd-stat-value {
  font-size: 22px;
  font-weight: 600;
  color: #1a1a1a;
  line-height: 1.4;
}
.jd-stat-label {
  font-size: 12px;
  color: #999;
  margin-top: 2px;
}

/* Body Layout */
.jd-body {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}
.jd-main {
  flex: 1;
  min-width: 0;
}
.jd-sidebar {
  width: 300px;
  flex-shrink: 0;
}

/* Section */
.jd-section {
  background: #fff;
  border-radius: 12px;
  padding: 24px 28px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  margin-bottom: 16px;
}
.jd-section-title {
  font-size: 17px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 16px;
  padding-bottom: 12px;
  border-bottom: 2px solid #409eff;
  display: inline-block;
}

/* Description (rendered markdown) */
.jd-description :deep(h3) {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 20px 0 10px;
}
.jd-description :deep(p) {
  margin: 8px 0;
  line-height: 1.7;
  color: #444;
  font-size: 14px;
}
.jd-description :deep(ul),
.jd-description :deep(ol) {
  margin: 8px 0;
  padding-left: 20px;
}
.jd-description :deep(li) {
  margin-bottom: 6px;
  line-height: 1.7;
  color: #444;
  font-size: 14px;
}

/* Skills */
.jd-skills {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.jd-skill-tag {
  font-size: 13px;
  padding: 6px 14px;
  border-radius: 6px;
}

/* Sidebar Card */
.jd-sidebar-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}
.jd-sidebar-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 16px;
  padding-bottom: 10px;
  border-bottom: 1px solid #f0f0f0;
}
.jd-sidebar-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  font-size: 13px;
}
.jd-sidebar-item + .jd-sidebar-item {
  border-top: 1px solid #f8f8f8;
}
.jd-sidebar-key {
  color: #999;
}
.jd-sidebar-val {
  color: #333;
  font-weight: 500;
}
.salary-val {
  color: #f56c6c;
  font-weight: 600;
}

/* Empty State */
.jd-empty {
  text-align: center;
  padding: 80px 0;
}

/* Responsive */
@media (max-width: 900px) {
  .jd-body {
    flex-direction: column;
  }
  .jd-sidebar {
    width: 100%;
  }
}
</style>

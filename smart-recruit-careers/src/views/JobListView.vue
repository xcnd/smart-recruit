<template>
  <div class="job-list-page">
    <!-- Hero -->
    <section class="jl-hero">
      <div class="container">
        <router-link to="/" class="back-link">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <line x1="19" y1="12" x2="5" y2="12" />
            <polyline points="12 19 5 12 12 5" />
          </svg>
          返回首页
        </router-link>

        <div class="jl-hero-content">
          <span class="jl-hero-label">Open Positions</span>
          <h1 class="jl-hero-title">{{ sectionTitle }}</h1>
          <p class="jl-hero-desc">{{ sectionSubtitle }}</p>
        </div>
      </div>
    </section>

    <!-- Content -->
    <section class="jl-content">
      <div class="container">
        <!-- Search Bar -->
        <JobSearchBar v-model:filter="currentFilter" v-model:keyword="searchKeyword" :categories="filterCategories" />

        <!-- Job Cards -->
        <div v-if="filteredJobs.length > 0" class="job-list">
          <JobCard
            v-for="job in pagedJobs"
            :key="job.id"
            :job="job"
            @click="handleCardClick(job.id)"
          />
        </div>

        <div v-else class="empty-state">
          <p class="empty-text">{{ emptyText }}</p>
        </div>

        <!-- Pagination -->
        <div v-if="filteredJobs.length > pageSize" class="pagination-wrapper">
          <el-pagination
            v-model:current-page="currentPage"
            :page-size="pageSize"
            :total="filteredJobs.length"
            background
            layout="prev, pager, next"
          />
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import JobSearchBar from '@/components/JobSearchBar.vue'
import JobCard from '@/components/JobCard.vue'
import { getCareersConfig } from '@/api/config'
import { getCareersPublicJobs } from '@/api/careers'

const router = useRouter()

const currentFilter = ref('全部职位')
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = 12
const sectionTitle = ref('热招职位')
const sectionSubtitle = ref('多个团队正在寻找优秀的你，投递简历开启你的 SmartRecruit 之旅。')
const emptyText = ref('暂无匹配的职位，请尝试其他筛选条件')
const filterCategories = ref<string[]>(['全部职位', '技术研发', '产品 & 设计', '市场 & 销售', '数据 & AI', '运营 & 职能'])

interface JobTag {
  text: string
  cls: string
}

interface SampleJob {
  id: number
  title: string
  dept: string
  location: string
  exp: string
  salary: string
  category: string
  date: string
  tags: JobTag[]
}

const sampleJobs = ref<SampleJob[]>([
  {
    id: 1,
    title: '资深后端开发工程师（Java）',
    dept: '技术研发部 · 核心平台组',
    location: '北京',
    exp: '5-10年',
    salary: '40-70K·16薪',
    category: 'tech',
    date: '2026-07-28',
    tags: [
      { text: '热招', cls: 'hot' },
      { text: 'Java', cls: 'tech-tag' },
      { text: 'Spring Boot', cls: 'tech-tag' },
    ],
  },
  {
    id: 2,
    title: 'AI 算法工程师（NLP/CV）',
    dept: 'AI 研究院 · 算法团队',
    location: '北京 / 上海',
    exp: '3-8年',
    salary: '50-90K·16薪',
    category: 'data',
    date: '2026-07-26',
    tags: [
      { text: '热招', cls: 'hot' },
      { text: 'NLP', cls: 'tech-tag' },
      { text: 'LLM', cls: 'tech-tag' },
    ],
  },
  {
    id: 3,
    title: '高级前端开发工程师（React/Vue）',
    dept: '技术研发部 · 前端团队',
    location: '上海',
    exp: '3-7年',
    salary: '35-60K·16薪',
    category: 'tech',
    date: '2026-07-25',
    tags: [
      { text: 'React', cls: 'tech-tag' },
      { text: 'Vue', cls: 'tech-tag' },
    ],
  },
  {
    id: 4,
    title: '高级产品经理（SaaS 方向）',
    dept: '产品部 · 招聘产品线',
    location: '北京',
    exp: '5-8年',
    salary: '35-55K·16薪',
    category: 'product',
    date: '2026-07-24',
    tags: [
      { text: 'B端', cls: 'tech-tag' },
      { text: 'SaaS', cls: 'tech-tag' },
    ],
  },
  {
    id: 5,
    title: '资深 UI/UX 设计师',
    dept: '设计部 · 体验设计团队',
    location: '北京 / 深圳',
    exp: '3-6年',
    salary: '30-50K·16薪',
    category: 'product',
    date: '2026-07-22',
    tags: [{ text: 'B端设计', cls: 'tech-tag' }],
  },
  {
    id: 6,
    title: '大客户销售经理（HR Tech）',
    dept: '销售部 · 大客户团队',
    location: '北京 / 上海 / 深圳',
    exp: '5-10年',
    salary: '25-45K·16薪',
    category: 'market',
    date: '2026-07-20',
    tags: [{ text: '热招', cls: 'hot' }],
  },
  {
    id: 7,
    title: '数据平台开发工程师',
    dept: '数据与 AI 平台部',
    location: '杭州',
    exp: '3-7年',
    salary: '35-60K·16薪',
    category: 'data',
    date: '2026-07-18',
    tags: [
      { text: '大数据', cls: 'tech-tag' },
      { text: 'Flink', cls: 'tech-tag' },
    ],
  },
  {
    id: 8,
    title: '安全合规工程师',
    dept: '基础架构部 · 安全团队',
    location: '北京',
    exp: '5-8年',
    salary: '40-65K·16薪',
    category: 'tech',
    date: '2026-07-16',
    tags: [
      { text: '安全', cls: 'tech-tag' },
      { text: '合规', cls: 'tech-tag' },
    ],
  },
  {
    id: 9,
    title: '校园招聘 HR',
    dept: '人力资源部 · 招聘团队',
    location: '北京',
    exp: '2-5年',
    salary: '18-30K·16薪',
    category: 'operation',
    date: '2026-07-15',
    tags: [
      { text: '校招', cls: 'new' },
      { text: '雇主品牌', cls: 'tech-tag' },
    ],
  },
])

const categoryMap: Record<string, string> = {
  '技术研发': 'tech',
  '产品 & 设计': 'product',
  '市场 & 销售': 'market',
  '数据 & AI': 'data',
  '运营 & 职能': 'operation',
}

const filteredJobs = computed<SampleJob[]>(() => {
  let jobs = [...sampleJobs.value]

  if (currentFilter.value !== '全部职位') {
    const mappedCat = categoryMap[currentFilter.value]
    if (mappedCat) {
      jobs = jobs.filter((j) => j.category === mappedCat)
    } else {
      return []
    }
  }

  const keyword = searchKeyword.value.trim().toLowerCase()
  if (keyword) {
    jobs = jobs.filter(
      (j) =>
        j.title.toLowerCase().includes(keyword) ||
        j.dept.toLowerCase().includes(keyword) ||
        j.tags.some((t) => t.text.toLowerCase().includes(keyword))
    )
  }

  // Sort by date descending (newest first)
  jobs.sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime())

  return jobs
})

const pagedJobs = computed<SampleJob[]>(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredJobs.value.slice(start, start + pageSize)
})

onMounted(async () => {
  try {
    const cfg = await getCareersConfig()
    if (cfg.careers_hot_title) sectionTitle.value = cfg.careers_hot_title
    if (cfg.careers_hot_subtitle) sectionSubtitle.value = cfg.careers_hot_subtitle
    if (cfg.careers_hot_empty_text) emptyText.value = cfg.careers_hot_empty_text
    if (cfg.careers_hot_filters) {
      try { filterCategories.value = JSON.parse(cfg.careers_hot_filters) } catch { }
    }
    try {
      const apiJobs = await getCareersPublicJobs('HOT')
      if (apiJobs && apiJobs.length > 0) {
        sampleJobs.value = (apiJobs as any[]).map((j: any) => ({
          ...j,
          date: formatApiDate(j.createTime || j.date),
        })) as SampleJob[]
      }
    } catch { /* keep hardcoded defaults */ }
  } catch { /* keep defaults */ }
})

function handleCardClick(jobId: number) {
  router.push('/jobs/' + jobId)
}

function formatApiDate(dateStr: string): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}
</script>

<style scoped>
.job-list-page {
  min-height: 100vh;
  background: var(--color-bg);
}

/* ================================================================
   Hero
   ================================================================ */
.jl-hero {
  padding: 100px 0 64px;
  background: linear-gradient(160deg, #0f172a 0%, #1e293b 40%, #1e3a5f 80%, #1b1b3a 100%);
  position: relative;
  overflow: hidden;
  isolation: isolate;
}

.jl-hero::before {
  content: '';
  position: absolute;
  inset: 0;
  z-index: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.04) 1px, transparent 1px);
  background-size: 80px 80px;
  mask-image: radial-gradient(ellipse 65% 55% at 50% 38%, rgba(0, 0, 0, 1) 0%, rgba(0, 0, 0, 0.3) 60%, transparent 100%);
  -webkit-mask-image: radial-gradient(ellipse 65% 55% at 50% 38%, rgba(0, 0, 0, 1) 0%, rgba(0, 0, 0, 0.3) 60%, transparent 100%);
  pointer-events: none;
}

.jl-hero::after {
  content: '';
  position: absolute;
  inset: 0;
  z-index: 0;
  background:
    radial-gradient(ellipse 70% 50% at 70% 30%, rgba(22, 119, 255, 0.08) 0%, transparent 60%),
    radial-gradient(ellipse 60% 50% at 30% 70%, rgba(139, 92, 246, 0.06) 0%, transparent 60%);
  pointer-events: none;
}

.jl-hero-content {
  position: relative;
  z-index: 1;
  text-align: center;
}

/* Back Link */
.back-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.4);
  margin-bottom: 28px;
  transition: color var(--transition);
}

.back-link:hover {
  color: rgba(255, 255, 255, 0.75);
}

.jl-hero-label {
  display: inline-block;
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: rgba(22, 119, 255, 0.85);
  background: rgba(22, 119, 255, 0.1);
  border: 1px solid rgba(22, 119, 255, 0.15);
  border-radius: var(--radius-full);
  padding: 4px 14px;
  margin-bottom: 16px;
}

.jl-hero-title {
  font-size: clamp(26px, 4.5vw, 40px);
  font-weight: 800;
  letter-spacing: -0.8px;
  line-height: 1.2;
  margin-bottom: 14px;
  color: #ffffff;
}

.jl-hero-desc {
  font-size: 15px;
  color: rgba(255, 255, 255, 0.5);
  max-width: 520px;
  margin: 0 auto;
}

/* ================================================================
   Content
   ================================================================ */
.jl-content {
  padding: 48px 0 96px;
  position: relative;
  z-index: 1;
}

/* ================================================================
   Job List
   ================================================================ */
.job-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ================================================================
   Empty State
   ================================================================ */
.empty-state {
  padding: 80px 0;
  text-align: center;
}

.empty-text {
  font-size: 15px;
  color: var(--color-text-muted);
}

/* ================================================================
   Pagination
   ================================================================ */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 48px;
  padding: 16px 0;
}

/* Ensure Element Plus pagination uses our tokens where possible */
.pagination-wrapper :deep(.el-pagination) {
  --el-pagination-bg-color: var(--color-bg);
  --el-pagination-hover-color: var(--color-primary);
  --el-pagination-button-bg-color: var(--color-bg);
}

.pagination-wrapper :deep(.el-pagination .el-pager li.is-active) {
  background-color: var(--color-primary);
  color: #fff;
}

.pagination-wrapper :deep(.el-pagination .el-pager li:hover) {
  color: var(--color-primary);
}

.pagination-wrapper :deep(.el-pagination button:hover) {
  color: var(--color-primary);
}

/* ================================================================
   Responsive
   ================================================================ */
@media (max-width: 768px) {
  .jl-hero {
    padding: 80px 0 48px;
  }

  .jl-content {
    padding: 32px 0 72px;
  }

  .jl-hero-desc {
    font-size: 14px;
  }

  .job-list {
    gap: 12px;
  }
}
</style>

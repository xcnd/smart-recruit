<template>
  <section id="jobs" class="section section-alt">
    <div class="container">
      <!-- Section Header -->
      <div class="section-header">
        <span class="section-label">Open Positions</span>
        <h2 class="section-title">{{ sectionTitle }}</h2>
        <p class="section-desc">{{ sectionSubtitle }}</p>
      </div>

      <!-- Search Bar -->
      <JobSearchBar v-model:filter="currentFilter" v-model:keyword="searchKeyword" :categories="filterCategories" />

      <!-- Job Cards -->
      <div v-if="displayedJobs.length > 0" class="job-list">
        <JobCard
          v-for="job in displayedJobs"
          :key="job.id"
          :job="job"
          @click="handleCardClick(job.id)"
        />
      </div>

      <div v-else class="empty-state">
        <p class="empty-text">{{ emptyText }}</p>
      </div>

      <!-- Show More / View All -->
      <div class="job-view-more">
        <button
          v-if="hasMore"
          class="view-more-btn"
          @click="showCount += 6"
        >
          加载更多职位
        </button>
        <router-link to="/jobs" class="view-all-link">
          查看全部职位
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <line x1="5" y1="12" x2="19" y2="12" />
            <polyline points="12 5 19 12 12 19" />
          </svg>
        </router-link>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import JobSearchBar from '@/components/JobSearchBar.vue'
import JobCard from '@/components/JobCard.vue'
import { getCareersPublicJobs } from '@/api/careers'
import { getCareersConfig } from '@/api/config'

const router = useRouter()

const currentFilter = ref('全部职位')
const searchKeyword = ref('')
const showCount = ref(6)

let refreshTimer: ReturnType<typeof setInterval> | null = null

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

const FALLBACK_JOBS: SampleJob[] = [
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
]

const sampleJobs = ref<SampleJob[]>([])
const filterCategories = ref<string[]>(['全部职位', '技术研发', '产品 & 设计', '市场 & 销售', '数据 & AI', '运营 & 职能'])
const sectionTitle = ref('热招职位')
const sectionSubtitle = ref('多个团队正在寻找优秀的你，投递简历开启你的 SmartRecruit 之旅。')
const emptyText = ref('暂无匹配的职位，请尝试其他筛选条件')

async function loadJobs() {
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
      } else {
        sampleJobs.value = [...FALLBACK_JOBS]
      }
    } catch {
      sampleJobs.value = [...FALLBACK_JOBS]
    }
  } catch {
    sampleJobs.value = [...FALLBACK_JOBS]
  }
}

function formatApiDate(dateStr: string): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  if (isNaN(d.getTime())) return dateStr
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

onMounted(() => {
  loadJobs()
  refreshTimer = setInterval(loadJobs, 5 * 60 * 1000) // 每 5 分钟刷新
})

onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
})

const filteredJobs = computed<SampleJob[]>(() => {
  let jobs = [...sampleJobs.value]

  if (currentFilter.value !== '全部职位') {
    jobs = jobs.filter((j) => j.category === currentFilter.value)
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

  return jobs
})

const displayedJobs = computed<SampleJob[]>(() => {
  return filteredJobs.value.slice(0, showCount.value)
})

const hasMore = computed(() => {
  return showCount.value < filteredJobs.value.length
})

function handleCardClick(jobId: number) {
  router.push('/jobs/' + jobId)
}
</script>

<style scoped>
.section {
  padding: 100px 0;
}

.section-alt {
  background: var(--color-bg-alt);
}

/* ================================================================
   Section Header
   ================================================================ */
.section-header {
  text-align: center;
  margin-bottom: 48px;
}

.section-label {
  display: inline-block;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-primary);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  margin-bottom: 16px;
  padding: 4px 14px;
  background: var(--color-primary-bg);
  border-radius: var(--radius-full);
}

.section-title {
  font-size: clamp(28px, 3.5vw, 42px);
  font-weight: 800;
  letter-spacing: -0.8px;
  line-height: 1.2;
  margin-bottom: 16px;
  color: var(--color-text);
}

.section-desc {
  font-size: 16px;
  color: var(--color-text-secondary);
  max-width: 560px;
  margin: 0 auto;
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
  padding: 64px 0;
  text-align: center;
}

.empty-text {
  font-size: 15px;
  color: var(--color-text-muted);
}

/* ================================================================
   View More / View All
   ================================================================ */
.job-view-more {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  margin-top: 40px;
}

.view-more-btn {
  padding: 12px 32px;
  font-size: 15px;
  font-weight: 600;
  color: var(--color-primary);
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-full);
  background: transparent;
  cursor: pointer;
  transition: all var(--transition);
}

.view-more-btn:hover {
  border-color: var(--color-primary);
  background: var(--color-primary-bg);
}

.view-all-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 32px;
  font-size: 15px;
  font-weight: 600;
  color: var(--color-primary);
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-full);
  transition: all var(--transition);
}

.view-all-link:hover {
  border-color: var(--color-primary);
  background: var(--color-primary-bg);
}

/* ================================================================
   Responsive
   ================================================================ */
@media (max-width: 768px) {
  .section {
    padding: 72px 0;
  }

  .section-header {
    margin-bottom: 36px;
  }

  .section-desc {
    font-size: 14px;
  }
}
</style>

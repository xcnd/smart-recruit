<template>
  <div class="social-recruitment-page">
    <!-- Navigation -->
    <NavBar />

    <!-- ============================================================
         Hero Banner
         ============================================================ -->
    <section class="social-hero">
      <!-- Glow orbs -->
      <div class="social-hero__orb social-hero__orb--1" aria-hidden="true" />
      <div class="social-hero__orb social-hero__orb--2" aria-hidden="true" />
      <div class="social-hero__orb social-hero__orb--3" aria-hidden="true" />

      <!-- Grid pattern overlay -->
      <div class="social-hero__grid" aria-hidden="true" />

      <!-- Content -->
      <div class="social-hero__content">
        <!-- Badge -->
        <div class="social-hero__badge">
          <span class="social-hero__badge-dot" aria-hidden="true" />
          <span>{{ heroBadge }}</span>
        </div>

        <!-- Title -->
        <h1 class="social-hero__title">{{ heroTitle }}</h1>

        <!-- Description -->
        <p class="social-hero__desc">{{ heroDesc }}</p>

        <!-- Stats -->
        <div class="social-hero__stats">
          <div v-for="(stat, idx) in heroStats" :key="idx" class="social-hero__stat">
            <div class="social-hero__stat-value">{{ stat.number }}</div>
            <div class="social-hero__stat-label">{{ stat.label }}</div>
          </div>
        </div>
      </div>

      <!-- Bottom fade gradient -->
      <div class="social-hero__bottom-fade" aria-hidden="true" />
    </section>

    <!-- ============================================================
         Job List Section
         ============================================================ -->
    <section class="social-jobs-section">
      <div class="container">
        <!-- Section Header -->
        <div class="section-header">
          <span class="section-label">Social Recruitment</span>
          <h2 class="section-title">{{ sectionTitle }}</h2>
          <p class="section-desc">{{ sectionSubtitle }}</p>
        </div>

        <!-- Search Bar -->
        <JobSearchBar
          v-model:filter="currentFilter"
          v-model:keyword="searchKeyword"
          :categories="filterCategories"
          placeholder="搜索职位名称、部门、关键词..."
        />

        <!-- Job Cards -->
        <div v-if="filteredJobs.length > 0" class="social-jobs-list">
          <JobCard
            v-for="job in pagedJobs"
            :key="job.id"
            :job="job"
            @click="handleCardClick(job.id)"
          />
        </div>

        <div v-else class="empty-state">
          <svg
            class="empty-icon"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="1.5"
            stroke-linecap="round"
            stroke-linejoin="round"
            width="48"
            height="48"
          >
            <circle cx="11" cy="11" r="8" />
            <line x1="21" y1="21" x2="16.65" y2="16.65" />
          </svg>
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

    <!-- Footer -->
    <FooterSection />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import NavBar from '@/components/NavBar.vue'
import FooterSection from '@/components/FooterSection.vue'
import JobSearchBar from '@/components/JobSearchBar.vue'
import JobCard from '@/components/JobCard.vue'
import { getCareersConfig } from '@/api/config'
import { getCareersPublicJobs } from '@/api/careers'

const router = useRouter()

// -- Hero reactive vars --
const heroBadge = ref('社会招聘')
const heroTitle = ref('寻找经验丰富的专业人才')
const heroDesc = ref('加入 SmartRecruit 核心团队，与行业顶尖人才一起，\n用 AI 重塑招聘的未来。我们提供具有竞争力的薪酬和开放的成长空间。')
const heroStats = ref<{ number: string; label: string }[]>([
  { number: '30+', label: '在招职位' },
  { number: '5', label: '办公城市' },
  { number: '16 薪', label: '薪酬保障' },
])

// -- Filter / Search / Pagination State --
const currentFilter = ref('全部职位')
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = 6
const sectionTitle = ref('社会招聘职位')
const sectionSubtitle = ref('我们正在寻找经验丰富的专业人才，加入核心团队，共同打造下一代智能招聘平台。')
const emptyText = ref('暂无匹配的职位，请尝试其他筛选条件')

// -- Filter categories --
const filterCategories = ref<string[]>([
  '全部职位',
  '技术研发',
  '产品 & 设计',
  '市场 & 销售',
  '数据 & AI',
  '运营 & 职能',
  '管理',
])

// -- Category mapping from filter chip to data category field --
const categoryMap: Record<string, string> = {
  '技术研发': 'tech',
  '产品 & 设计': 'product',
  '市场 & 销售': 'market',
  '数据 & AI': 'data',
  '运营 & 职能': 'operation',
}

// -- Interfaces --
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

// -- Mock Data: 10 senior-level social recruitment positions --
const socialJobs = ref<SampleJob[]>([
  {
    id: 101,
    title: '技术总监（AI 平台）',
    dept: '技术研发部·AI平台组',
    location: '北京',
    exp: '10-15年',
    salary: '80-120K·16薪',
    category: 'tech',
    date: '2026-07-25',
    tags: [
      { text: '热招', cls: 'hot' },
      { text: '管理岗', cls: 'tech-tag' },
      { text: 'AI', cls: 'tech-tag' },
    ],
  },
  {
    id: 102,
    title: '资深后端架构师（Java）',
    dept: '技术研发部·核心平台组',
    location: '北京/上海',
    exp: '8-12年',
    salary: '60-90K·16薪',
    category: 'tech',
    date: '2026-07-24',
    tags: [
      { text: '热招', cls: 'hot' },
      { text: 'Java', cls: 'tech-tag' },
      { text: '架构', cls: 'tech-tag' },
    ],
  },
  {
    id: 103,
    title: '高级产品总监（SaaS 平台）',
    dept: '产品部·招聘产品线',
    location: '北京',
    exp: '10-15年',
    salary: '70-100K·16薪',
    category: 'product',
    date: '2026-07-23',
    tags: [
      { text: 'B端', cls: 'tech-tag' },
      { text: 'SaaS', cls: 'tech-tag' },
      { text: '管理岗', cls: 'tech-tag' },
    ],
  },
  {
    id: 104,
    title: '资深 AI 研究员（LLM）',
    dept: 'AI 研究院',
    location: '北京/上海',
    exp: '5-12年',
    salary: '60-100K·16薪',
    category: 'data',
    date: '2026-07-22',
    tags: [
      { text: 'LLM', cls: 'tech-tag' },
      { text: 'NLP', cls: 'tech-tag' },
      { text: '博士优先', cls: 'new' },
    ],
  },
  {
    id: 105,
    title: '大客户销售总监',
    dept: '销售部·大客户团队',
    location: '北京/上海/深圳',
    exp: '8-15年',
    salary: '50-80K·16薪',
    category: 'market',
    date: '2026-07-21',
    tags: [
      { text: '热招', cls: 'hot' },
      { text: '管理岗', cls: 'tech-tag' },
    ],
  },
  {
    id: 106,
    title: '资深安全架构师',
    dept: '基础架构部·安全团队',
    location: '北京',
    exp: '8-12年',
    salary: '55-85K·16薪',
    category: 'tech',
    date: '2026-07-20',
    tags: [
      { text: '安全', cls: 'tech-tag' },
      { text: '架构', cls: 'tech-tag' },
    ],
  },
  {
    id: 107,
    title: '高级财务总监',
    dept: '财务部',
    location: '北京',
    exp: '10-15年',
    salary: '60-90K·16薪',
    category: 'operation',
    date: '2026-07-19',
    tags: [
      { text: '管理岗', cls: 'tech-tag' },
      { text: 'CPA', cls: 'tech-tag' },
    ],
  },
  {
    id: 108,
    title: '资深 DevOps 工程师',
    dept: '基础架构部·工程效能组',
    location: '杭州',
    exp: '5-10年',
    salary: '45-70K·16薪',
    category: 'tech',
    date: '2026-07-18',
    tags: [
      { text: 'K8s', cls: 'tech-tag' },
      { text: 'CI/CD', cls: 'tech-tag' },
    ],
  },
  {
    id: 109,
    title: '高级数据平台架构师',
    dept: '数据与AI平台部',
    location: '北京/杭州',
    exp: '8-12年',
    salary: '55-85K·16薪',
    category: 'data',
    date: '2026-07-17',
    tags: [
      { text: '大数据', cls: 'tech-tag' },
      { text: 'Flink', cls: 'tech-tag' },
      { text: '架构', cls: 'tech-tag' },
    ],
  },
  {
    id: 110,
    title: 'HR 副总裁',
    dept: '人力资源部',
    location: '北京',
    exp: '10-20年',
    salary: '80-120K·16薪',
    category: 'operation',
    date: '2026-07-16',
    tags: [
      { text: '管理岗', cls: 'tech-tag' },
      { text: 'HR', cls: 'tech-tag' },
    ],
  },
])

// -- Computed: filtered jobs --
const filteredJobs = computed<SampleJob[]>(() => {
  let jobs = [...socialJobs.value]

  // Filter by category
  if (currentFilter.value !== '全部职位') {
    const catValue = categoryMap[currentFilter.value]
    if (catValue) {
      jobs = jobs.filter((j) => j.category === catValue)
    } else {
      // Category with no mapping (e.g., '管理') - return empty
      return []
    }
  }

  // Filter by search keyword (title, dept, tags)
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

// -- Computed: paged jobs --
const pagedJobs = computed<SampleJob[]>(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredJobs.value.slice(start, start + pageSize)
})

onMounted(async () => {
  try {
    const cfg = await getCareersConfig()
    if (cfg.careers_social_hero_badge) heroBadge.value = cfg.careers_social_hero_badge
    if (cfg.careers_social_hero_title) heroTitle.value = cfg.careers_social_hero_title
    if (cfg.careers_social_hero_desc) heroDesc.value = cfg.careers_social_hero_desc
    if (cfg.careers_social_hero_stats) {
      try { heroStats.value = JSON.parse(cfg.careers_social_hero_stats) } catch { }
    }
    if (cfg.careers_social_title) sectionTitle.value = cfg.careers_social_title
    if (cfg.careers_social_subtitle) sectionSubtitle.value = cfg.careers_social_subtitle
    if (cfg.careers_social_empty_text) emptyText.value = cfg.careers_social_empty_text
    if (cfg.careers_social_filters) {
      try { filterCategories.value = JSON.parse(cfg.careers_social_filters) } catch { }
    }
    try {
      const apiJobs = await getCareersPublicJobs('SOCIAL')
      if (apiJobs && apiJobs.length > 0) {
        socialJobs.value = (apiJobs as any[]).map((j: any) => ({
          ...j,
          date: formatApiDate(j.createTime || j.date),
        })) as SampleJob[]
      }
    } catch { /* keep hardcoded defaults */ }
  } catch { /* keep defaults */ }
})

function formatApiDate(dateStr: string): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

// -- Card click handler --
function handleCardClick(jobId: number) {
  router.push('/jobs/' + jobId)
}
</script>

<style scoped>
/* ================================================================
   Page Container
   ================================================================ */
.social-recruitment-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

/* ================================================================
   Hero Section
   ================================================================ */
.social-hero {
  position: relative;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(160deg, #0f172a 0%, #1e293b 30%, #1e3a5f 60%, #1b1b3a 100%);
  overflow: hidden;
  padding: 140px 32px 100px;
  isolation: isolate;
}

/* -- Glow orbs -- */
.social-hero__orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.4;
  pointer-events: none;
  will-change: transform;
}

.social-hero__orb--1 {
  width: 420px;
  height: 420px;
  background: radial-gradient(circle, rgba(99, 102, 241, 0.7) 0%, transparent 70%);
  top: -10%;
  left: -5%;
  animation: social-orb-float-1 12s ease-in-out infinite alternate;
}

.social-hero__orb--2 {
  width: 360px;
  height: 360px;
  background: radial-gradient(circle, rgba(244, 63, 94, 0.55) 0%, transparent 70%);
  top: 45%;
  right: -8%;
  animation: social-orb-float-2 10s ease-in-out infinite alternate;
}

.social-hero__orb--3 {
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(139, 92, 246, 0.6) 0%, transparent 70%);
  bottom: -10%;
  left: 35%;
  animation: social-orb-float-3 8s ease-in-out infinite alternate;
}

@keyframes social-orb-float-1 {
  0% { transform: translate(0, 0) scale(1); }
  100% { transform: translate(60px, 40px) scale(1.08); }
}

@keyframes social-orb-float-2 {
  0% { transform: translate(0, 0) scale(1); }
  100% { transform: translate(-50px, -30px) scale(1.12); }
}

@keyframes social-orb-float-3 {
  0% { transform: translate(0, 0) scale(1); }
  100% { transform: translate(30px, -50px) scale(1.05); }
}

/* -- Grid pattern overlay -- */
.social-hero__grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.04) 1px, transparent 1px);
  background-size: 80px 80px;
  mask-image: radial-gradient(ellipse 65% 55% at 50% 38%, rgba(0, 0, 0, 1) 0%, rgba(0, 0, 0, 0.3) 60%, transparent 100%);
  -webkit-mask-image: radial-gradient(ellipse 65% 55% at 50% 38%, rgba(0, 0, 0, 1) 0%, rgba(0, 0, 0, 0.3) 60%, transparent 100%);
  pointer-events: none;
}

/* -- Content -- */
.social-hero__content {
  position: relative;
  z-index: 2;
  max-width: var(--max-width, 1280px);
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

/* -- Badge -- */
.social-hero__badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 18px;
  font-size: 14px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.85);
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: var(--radius-full, 9999px);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  margin-bottom: 28px;
}

.social-hero__badge-dot {
  display: block;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--color-success, #10b981);
  box-shadow: 0 0 10px rgba(16, 185, 129, 0.6);
  animation: social-pulse-dot 2s ease-in-out infinite;
}

@keyframes social-pulse-dot {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(1.4); }
}

/* -- Title -- */
.social-hero__title {
  font-size: clamp(40px, 5.5vw, 68px);
  font-weight: 800;
  color: #ffffff;
  letter-spacing: -1.5px;
  line-height: 1.15;
  margin-bottom: 24px;
}

/* -- Description -- */
.social-hero__desc {
  font-size: clamp(16px, 2vw, 19px);
  color: rgba(255, 255, 255, 0.6);
  line-height: 1.75;
  max-width: 640px;
  margin: 0 auto 56px;
}

/* -- Stats -- */
.social-hero__stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0;
  width: 100%;
  max-width: 720px;
}

.social-hero__stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px 16px;
  position: relative;
}

.social-hero__stat:not(:last-child)::after {
  content: '';
  position: absolute;
  right: 0;
  top: 16%;
  height: 68%;
  width: 1px;
  background: rgba(255, 255, 255, 0.1);
}

.social-hero__stat-value {
  font-size: 40px;
  font-weight: 800;
  color: #ffffff;
  line-height: 1.1;
  letter-spacing: -1px;
  margin-bottom: 6px;
}

.social-hero__stat-label {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.45);
  font-weight: 400;
  letter-spacing: 0.3px;
}

/* -- Bottom fade -- */
.social-hero__bottom-fade {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 120px;
  background: linear-gradient(to top, rgba(15, 13, 35, 0.5), transparent);
  pointer-events: none;
  z-index: 1;
}

/* ================================================================
   Job List Section
   ================================================================ */
.social-jobs-section {
  background: var(--color-bg);
  padding: 100px 0;
}

/* Section Header */
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

/* Job List */
.social-jobs-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 32px;
}

/* ================================================================
   Empty State
   ================================================================ */
.empty-state {
  padding: 80px 0;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.empty-icon {
  color: var(--color-text-muted);
  opacity: 0.4;
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
   Responsive — Tablet
   ================================================================ */
@media (max-width: 768px) {
  .social-hero {
    padding: 120px 20px 80px;
  }

  .social-hero__title {
    font-size: clamp(34px, 8vw, 48px);
  }

  .social-hero__desc {
    font-size: 16px;
    max-width: 480px;
    margin-bottom: 48px;
  }

  .social-hero__stats {
    max-width: 480px;
  }

  .social-hero__stat-value {
    font-size: 34px;
  }

  .social-hero__stat-label {
    font-size: 12px;
  }

  /* Adjust orb sizes for tablet */
  .social-hero__orb--1 {
    width: 320px;
    height: 320px;
  }

  .social-hero__orb--2 {
    width: 260px;
    height: 260px;
  }

  .social-hero__orb--3 {
    width: 220px;
    height: 220px;
  }

  /* Job section */
  .social-jobs-section {
    padding: 72px 0;
  }

  .section-header {
    margin-bottom: 36px;
  }

  .section-desc {
    font-size: 14px;
  }

  .social-jobs-list {
    gap: 12px;
  }
}

/* ================================================================
   Responsive — Mobile
   ================================================================ */
@media (max-width: 480px) {
  .social-hero {
    padding: 110px 16px 64px;
  }

  .social-hero__badge {
    font-size: 12px;
    padding: 5px 14px;
  }

  .social-hero__title {
    font-size: clamp(28px, 10vw, 40px);
  }

  .social-hero__desc {
    font-size: 15px;
    line-height: 1.65;
    margin-bottom: 40px;
  }

  .social-hero__stats {
    grid-template-columns: repeat(3, 1fr);
    gap: 12px 0;
    max-width: 360px;
  }

  .social-hero__stat {
    padding: 16px 8px;
  }

  .social-hero__stat-value {
    font-size: 28px;
  }

  .social-hero__stat-label {
    font-size: 11px;
  }

  /* Further reduce orb sizes */
  .social-hero__orb--1 {
    width: 240px;
    height: 240px;
  }

  .social-hero__orb--2 {
    width: 200px;
    height: 200px;
  }

  .social-hero__orb--3 {
    width: 160px;
    height: 160px;
  }

  /* Job section */
  .social-jobs-section {
    padding: 56px 0;
  }
}
</style>

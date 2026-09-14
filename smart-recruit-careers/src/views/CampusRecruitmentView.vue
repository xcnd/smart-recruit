<template>
  <div class="campus-page">
    <!-- ================================================================
         NavBar
         ================================================================ -->
    <NavBar />

    <!-- ================================================================
         Campus Hero Banner
         ================================================================ -->
    <section class="campus-hero">
      <!-- Glow orbs -->
      <div class="campus-hero__orb campus-hero__orb--1" aria-hidden="true" />
      <div class="campus-hero__orb campus-hero__orb--2" aria-hidden="true" />
      <div class="campus-hero__orb campus-hero__orb--3" aria-hidden="true" />

      <!-- Grid pattern overlay -->
      <div class="campus-hero__grid" aria-hidden="true" />

      <!-- Content -->
      <div class="campus-hero__content">
        <!-- Badge -->
        <div class="campus-hero__badge">
          <span class="campus-hero__badge-dot" aria-hidden="true" />
          <span>{{ heroBadge }}</span>
        </div>

        <!-- Title -->
        <h1 class="campus-hero__title">{{ heroTitle }}</h1>

        <!-- Description -->
        <p class="campus-hero__desc">{{ heroDesc }}</p>

        <!-- CTA -->
        <a
          href="#campus-positions"
          class="campus-hero__cta"
          @click.prevent="scrollToPositions"
        >
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <circle cx="11" cy="11" r="8" />
            <line x1="21" y1="21" x2="16.65" y2="16.65" />
          </svg>
          {{ heroCta }}
        </a>

        <!-- Stat counters -->
        <div class="campus-hero__stats">
          <div v-for="(stat, idx) in heroStats" :key="idx" class="campus-hero__stat">
            <div class="campus-hero__stat-number">{{ stat.number }}</div>
            <div class="campus-hero__stat-label">{{ stat.label }}</div>
          </div>
        </div>
      </div>

      <!-- Bottom fade gradient -->
      <div class="campus-hero__bottom-fade" aria-hidden="true" />
    </section>

    <!-- ================================================================
         Recruitment Process Timeline
         ================================================================ -->
    <section class="campus-process section">
      <div class="container">
        <div class="section-header">
          <span class="section-label">Recruitment Process</span>
          <h2 class="section-title">{{ processTitle }}</h2>
          <p class="section-desc">{{ processSubtitle }}</p>
        </div>

        <div class="process-timeline">
          <div
            v-for="(step, index) in processSteps"
            :key="index"
            class="process-step"
          >
            <div class="process-step__badge">
              <span class="process-step__number">{{ index + 1 }}</span>
            </div>
            <div class="process-step__body">
              <h3 class="process-step__title">{{ step.title }}</h3>
              <p class="process-step__desc">{{ step.description }}</p>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ================================================================
         Job List Section
         ================================================================ -->
    <section id="campus-positions" class="campus-jobs section section-alt">
      <div class="container">
        <div class="section-header">
          <span class="section-label">Campus Positions</span>
          <h2 class="section-title">{{ sectionTitle }}</h2>
          <p class="section-desc">{{ sectionSubtitle }}</p>
        </div>

        <!-- Search Bar -->
        <JobSearchBar
          v-model:filter="currentFilter"
          v-model:keyword="searchKeyword"
          :categories="campusCategories"
        />

        <!-- Job Cards -->
        <div v-if="filteredCampusJobs.length > 0" class="job-list">
          <JobCard
            v-for="job in pagedCampusJobs"
            :key="job.id"
            :job="job"
            @click="handleCardClick(job.id)"
          />
        </div>

        <div v-else class="empty-state">
          <p class="empty-text">{{ emptyText }}</p>
        </div>

        <!-- Pagination -->
        <div v-if="filteredCampusJobs.length > pageSize" class="pagination-wrapper">
          <el-pagination
            v-model:current-page="currentPage"
            :page-size="pageSize"
            :total="filteredCampusJobs.length"
            background
            layout="prev, pager, next"
          />
        </div>
      </div>
    </section>

    <!-- ================================================================
         Campus Talks Schedule
         ================================================================ -->
    <section class="campus-talks section">
      <div class="container">
        <div class="section-header">
          <span class="section-label">Campus Talks</span>
          <h2 class="section-title">{{ talksTitle }}</h2>
          <p class="section-desc">{{ talksSubtitle }}</p>
        </div>

        <div class="talks-grid">
          <article
            v-for="(talk, index) in campusTalks"
            :key="index"
            class="talk-card"
          >
            <!-- Date block -->
            <div class="talk-card__date">
              <span class="talk-card__month">{{ talk.month }}月</span>
              <span class="talk-card__day">{{ talk.day }}</span>
            </div>

            <!-- Info -->
            <div class="talk-card__info">
              <h3 class="talk-card__school">{{ talk.school }}</h3>
              <p class="talk-card__venue">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                  <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z" />
                  <circle cx="12" cy="10" r="3" />
                </svg>
                {{ talk.venue }}
              </p>
              <p class="talk-card__time">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                  <circle cx="12" cy="12" r="10" />
                  <polyline points="12 6 12 12 16 14" />
                </svg>
                {{ talk.timeSlot }}
              </p>
            </div>

            <!-- Status badge -->
            <span
              class="talk-card__status"
              :class="talk.status === 'upcoming' ? 'talk-card__status--upcoming' : 'talk-card__status--ended'"
            >
              {{ talk.status === 'upcoming' ? '即将开始' : '已结束' }}
            </span>
          </article>
        </div>
      </div>
    </section>

    <!-- ================================================================
         Footer Section
         ================================================================ -->
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
const heroBadge = ref('校园招聘')
const heroTitle = ref('开启你的职业生涯')
const heroDesc = ref('面向应届毕业生及在校实习生，我们提供完善的培训体系与快速成长路径。\n在这里，你将与行业顶尖人才并肩工作，在真实项目中历练成长。')
const heroCta = ref('查看校招职位')
const heroStats = ref<{ number: string; label: string }[]>([
  { number: '50+', label: '校招职位' },
  { number: '20+', label: '目标院校' },
  { number: '6 个月', label: '培养计划' },
])

// -- Process section --
const processTitle = ref('招聘流程')
const processSubtitle = ref('清晰透明的流程，让你每一步都心中有数')

// -- Filter / search / pagination state --
const currentFilter = ref('全部职位')
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = 6
const sectionTitle = ref('校招职位')
const sectionSubtitle = ref('面向 2026/2027 届毕业生及在校实习生')
const emptyText = ref('暂无匹配的校招职位，请尝试其他筛选条件')

// -- Talks section --
const talksTitle = ref('宣讲会行程')
const talksSubtitle = ref('期待在校园与你相遇')

const campusCategories = ref<string[]>([
  '全部职位',
  '技术研发',
  '产品 & 设计',
  '市场 & 营销',
  '数据 & AI',
  '运营 & 职能',
  '实习',
])

// -- Process steps data --
interface ProcessStep {
  title: string
  description: string
}

const processSteps = ref<ProcessStep[]>([
  { title: '网申投递', description: '在线提交简历与作品集' },
  { title: '简历筛选', description: 'HR 团队 3-5 个工作日内反馈' },
  { title: '线上笔试', description: '技术岗 90 分钟在线编程测试' },
  { title: '技术面试', description: '2-3 轮视频/线下面试' },
  { title: 'HR 面试', description: '沟通价值观与发展意向' },
  { title: '发放 Offer', description: '1 周内确认并发出聘用通知' },
])

// -- Campus talks mock data --
interface CampusTalk {
  month: number
  day: number
  school: string
  venue: string
  timeSlot: string
  status: 'upcoming' | 'ended'
}

const campusTalks = ref<CampusTalk[]>([
  {
    month: 9,
    day: 15,
    school: '清华大学',
    venue: '职业发展中心·天一厅',
    timeSlot: '14:00-16:00',
    status: 'upcoming',
  },
  {
    month: 9,
    day: 18,
    school: '北京大学',
    venue: '英杰交流中心·月光厅',
    timeSlot: '14:00-16:00',
    status: 'upcoming',
  },
  {
    month: 9,
    day: 22,
    school: '浙江大学',
    venue: '玉泉校区·永谦活动中心',
    timeSlot: '18:30-20:30',
    status: 'upcoming',
  },
  {
    month: 9,
    day: 25,
    school: '上海交通大学',
    venue: '闵行校区·铁生馆',
    timeSlot: '14:00-16:00',
    status: 'upcoming',
  },
])

// -- Job data interfaces --
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

// -- Campus mock job data (10 positions, IDs 201-210) --
const campusJobs = ref<SampleJob[]>([
  {
    id: 201,
    title: '前端开发工程师（2026 届）',
    dept: '技术研发部·前端团队',
    location: '上海',
    exp: '应届生',
    salary: '20-35K·16薪',
    category: 'tech',
    date: '2026-07-30',
    tags: [
      { text: '校招', cls: 'new' },
      { text: 'React', cls: 'tech-tag' },
      { text: 'Vue', cls: 'tech-tag' },
    ],
  },
  {
    id: 202,
    title: '后端开发工程师（2026 届）',
    dept: '技术研发部·核心平台组',
    location: '北京/杭州',
    exp: '应届生',
    salary: '22-38K·16薪',
    category: 'tech',
    date: '2026-07-30',
    tags: [
      { text: '校招', cls: 'new' },
      { text: 'Java', cls: 'tech-tag' },
      { text: 'Go', cls: 'tech-tag' },
    ],
  },
  {
    id: 203,
    title: '算法工程师（2026 届）',
    dept: 'AI 研究院',
    location: '北京',
    exp: '应届生',
    salary: '28-45K·16薪',
    category: 'data',
    date: '2026-07-29',
    tags: [
      { text: '校招', cls: 'new' },
      { text: 'NLP', cls: 'tech-tag' },
      { text: '硕士优先', cls: 'tech-tag' },
    ],
  },
  {
    id: 204,
    title: '产品经理（2026 届）',
    dept: '产品部',
    location: '北京/深圳',
    exp: '应届生',
    salary: '18-30K·16薪',
    category: 'product',
    date: '2026-07-28',
    tags: [
      { text: '校招', cls: 'new' },
      { text: 'B端', cls: 'tech-tag' },
    ],
  },
  {
    id: 205,
    title: '后端开发实习生',
    dept: '技术研发部',
    location: '北京/上海/杭州',
    exp: '在校生',
    salary: '8-12K',
    category: 'tech',
    date: '2026-07-27',
    tags: [
      { text: '实习', cls: 'new' },
      { text: '可转正', cls: 'hot' },
      { text: 'Java', cls: 'tech-tag' },
    ],
  },
  {
    id: 206,
    title: '前端开发实习生',
    dept: '技术研发部·前端团队',
    location: '上海',
    exp: '在校生',
    salary: '8-12K',
    category: 'tech',
    date: '2026-07-26',
    tags: [
      { text: '实习', cls: 'new' },
      { text: 'React', cls: 'tech-tag' },
      { text: '可转正', cls: 'hot' },
    ],
  },
  {
    id: 207,
    title: '数据分析师（2026 届）',
    dept: '数据与AI平台部',
    location: '北京/杭州',
    exp: '应届生',
    salary: '18-30K·16薪',
    category: 'data',
    date: '2026-07-25',
    tags: [
      { text: '校招', cls: 'new' },
      { text: 'SQL', cls: 'tech-tag' },
      { text: 'Python', cls: 'tech-tag' },
    ],
  },
  {
    id: 208,
    title: '人力资源管培生（2026 届）',
    dept: '人力资源部',
    location: '北京',
    exp: '应届生',
    salary: '15-25K·16薪',
    category: 'operation',
    date: '2026-07-24',
    tags: [
      { text: '校招', cls: 'new' },
      { text: '管培生', cls: 'tech-tag' },
    ],
  },
  {
    id: 209,
    title: 'AI 算法实习生',
    dept: 'AI 研究院',
    location: '北京',
    exp: '在校生',
    salary: '10-15K',
    category: 'data',
    date: '2026-07-23',
    tags: [
      { text: '实习', cls: 'new' },
      { text: 'LLM', cls: 'tech-tag' },
      { text: '硕博优先', cls: 'tech-tag' },
    ],
  },
  {
    id: 210,
    title: 'UI/UX 设计师（2026 届）',
    dept: '设计部·体验设计团队',
    location: '北京/深圳',
    exp: '应届生',
    salary: '18-28K·16薪',
    category: 'product',
    date: '2026-07-22',
    tags: [
      { text: '校招', cls: 'new' },
      { text: 'Figma', cls: 'tech-tag' },
    ],
  },
])

// -- Category filter mapping (Chinese label -> internal category key) --
const categoryMap: Record<string, string> = {
  '技术研发': 'tech',
  '产品 & 设计': 'product',
  '市场 & 营销': 'market',
  '数据 & AI': 'data',
  '运营 & 职能': 'operation',
}

// -- Filtered jobs computed --
const filteredCampusJobs = computed<SampleJob[]>(() => {
  let jobs = [...campusJobs.value]

  const filter = currentFilter.value

  if (filter !== '全部职位') {
    if (filter === '实习') {
      // Filter by intern/在校生 exp or title containing 实习
      jobs = jobs.filter((j) => j.exp === '在校生' || j.title.includes('实习'))
    } else {
      const mappedCategory = categoryMap[filter]
      if (mappedCategory) {
        jobs = jobs.filter((j) => j.category === mappedCategory)
      }
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

  return jobs
})

// -- Paginated jobs computed --
const pagedCampusJobs = computed<SampleJob[]>(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredCampusJobs.value.slice(start, start + pageSize)
})

onMounted(async () => {
  try {
    const cfg = await getCareersConfig()
    if (cfg.careers_campus_hero_badge) heroBadge.value = cfg.careers_campus_hero_badge
    if (cfg.careers_campus_hero_title) heroTitle.value = cfg.careers_campus_hero_title
    if (cfg.careers_campus_hero_desc) heroDesc.value = cfg.careers_campus_hero_desc
    if (cfg.careers_campus_hero_cta) heroCta.value = cfg.careers_campus_hero_cta
    if (cfg.careers_campus_hero_stats) {
      try { heroStats.value = JSON.parse(cfg.careers_campus_hero_stats) } catch { }
    }
    if (cfg.careers_campus_process_title) processTitle.value = cfg.careers_campus_process_title
    if (cfg.careers_campus_process_subtitle) processSubtitle.value = cfg.careers_campus_process_subtitle
    if (cfg.careers_campus_process_steps) {
      try { processSteps.value = JSON.parse(cfg.careers_campus_process_steps) } catch { }
    }
    if (cfg.careers_campus_title) sectionTitle.value = cfg.careers_campus_title
    if (cfg.careers_campus_subtitle) sectionSubtitle.value = cfg.careers_campus_subtitle
    if (cfg.careers_campus_empty_text) emptyText.value = cfg.careers_campus_empty_text
    if (cfg.careers_campus_filters) {
      try { campusCategories.value = JSON.parse(cfg.careers_campus_filters) } catch { }
    }
    try {
      const apiJobs = await getCareersPublicJobs('CAMPUS')
      if (apiJobs && apiJobs.length > 0) {
        campusJobs.value = (apiJobs as any[]).map((j: any) => ({
          ...j,
          date: formatApiDate(j.createTime || j.date),
        })) as SampleJob[]
      }
    } catch { /* keep hardcoded defaults */ }
    if (cfg.careers_campus_talks_title) talksTitle.value = cfg.careers_campus_talks_title
    if (cfg.careers_campus_talks_subtitle) talksSubtitle.value = cfg.careers_campus_talks_subtitle
    if (cfg.careers_campus_talks) {
      try { campusTalks.value = JSON.parse(cfg.careers_campus_talks) } catch { }
    }
  } catch { /* keep defaults */ }
})

// -- Handlers --
function formatApiDate(dateStr: string): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function handleCardClick(jobId: number) {
  router.push('/jobs/' + jobId)
}

function scrollToPositions() {
  const el = document.getElementById('campus-positions')
  el?.scrollIntoView({ behavior: 'smooth' })
}
</script>

<style scoped>
/* ================================================================
   Page wrapper
   ================================================================ */
.campus-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

/* ================================================================
   Campus Hero Banner
   ================================================================ */
.campus-hero {
  position: relative;
  width: 100%;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(160deg, #1677ff 0%, #06b6d4 40%, #0ea5e9 70%, #2563eb 100%);
  overflow: hidden;
  padding: 120px 32px 80px;
  isolation: isolate;
}

/* -- Glow orbs -- */
.campus-hero__orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.5;
  pointer-events: none;
  will-change: transform;
}

.campus-hero__orb--1 {
  width: 400px;
  height: 400px;
  background: rgba(34, 211, 238, 0.5);
  top: -8%;
  left: -6%;
  animation: campus-orb-float-1 10s ease-in-out infinite alternate;
}

.campus-hero__orb--2 {
  width: 360px;
  height: 360px;
  background: rgba(22, 119, 255, 0.6);
  top: 50%;
  right: -10%;
  animation: campus-orb-float-2 12s ease-in-out infinite alternate;
}

.campus-hero__orb--3 {
  width: 320px;
  height: 320px;
  background: rgba(16, 185, 129, 0.4);
  bottom: -8%;
  left: 30%;
  animation: campus-orb-float-3 8s ease-in-out infinite alternate;
}

@keyframes campus-orb-float-1 {
  0% { transform: translate(0, 0) scale(1); }
  100% { transform: translate(50px, 30px) scale(1.1); }
}

@keyframes campus-orb-float-2 {
  0% { transform: translate(0, 0) scale(1); }
  100% { transform: translate(-40px, -20px) scale(1.08); }
}

@keyframes campus-orb-float-3 {
  0% { transform: translate(0, 0) scale(1); }
  100% { transform: translate(20px, -40px) scale(1.06); }
}

/* -- Grid pattern overlay -- */
.campus-hero__grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.06) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.06) 1px, transparent 1px);
  background-size: 80px 80px;
  mask-image: radial-gradient(ellipse 65% 55% at 50% 38%, rgba(0, 0, 0, 1) 0%, rgba(0, 0, 0, 0.3) 60%, transparent 100%);
  -webkit-mask-image: radial-gradient(ellipse 65% 55% at 50% 38%, rgba(0, 0, 0, 1) 0%, rgba(0, 0, 0, 0.3) 60%, transparent 100%);
  pointer-events: none;
}

/* -- Content -- */
.campus-hero__content {
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
.campus-hero__badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 18px;
  font-size: 14px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.9);
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: var(--radius-full, 9999px);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  margin-bottom: 28px;
}

.campus-hero__badge-dot {
  display: block;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #3b82f6;
  box-shadow: 0 0 10px rgba(59, 130, 246, 0.6);
  animation: campus-pulse-dot 2s ease-in-out infinite;
}

@keyframes campus-pulse-dot {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.6; transform: scale(1.3); }
}

/* -- Title -- */
.campus-hero__title {
  font-size: clamp(40px, 5.5vw, 64px);
  font-weight: 800;
  color: #ffffff;
  letter-spacing: -1.5px;
  line-height: 1.2;
  margin-bottom: 24px;
}

/* -- Description -- */
.campus-hero__desc {
  font-size: clamp(16px, 2vw, 19px);
  color: rgba(255, 255, 255, 0.75);
  line-height: 1.75;
  max-width: 620px;
  margin: 0 auto 40px;
}

/* -- CTA button -- */
.campus-hero__cta {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 50px;
  padding: 0 32px;
  font-size: 16px;
  font-weight: 600;
  color: #1677ff;
  text-decoration: none;
  background: #ffffff;
  border-radius: var(--radius-full, 9999px);
  box-shadow: 0 6px 24px rgba(22, 119, 255, 0.3);
  transition:
    transform var(--transition, 0.2s ease),
    box-shadow var(--transition, 0.2s ease);
  margin-bottom: 72px;
}

.campus-hero__cta:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 32px rgba(22, 119, 255, 0.45);
}

/* -- Stats -- */
.campus-hero__stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0;
  width: 100%;
  max-width: 700px;
}

.campus-hero__stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px 16px;
  position: relative;
}

.campus-hero__stat:not(:last-child)::after {
  content: '';
  position: absolute;
  right: 0;
  top: 16%;
  height: 68%;
  width: 1px;
  background: rgba(255, 255, 255, 0.15);
}

.campus-hero__stat-number {
  font-size: 44px;
  font-weight: 800;
  color: #ffffff;
  line-height: 1.1;
  letter-spacing: -1px;
  margin-bottom: 6px;
}

.campus-hero__stat-label {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
  font-weight: 400;
  letter-spacing: 0.3px;
}

/* -- Bottom fade -- */
.campus-hero__bottom-fade {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 120px;
  background: linear-gradient(to top, rgba(22, 119, 255, 0.25), transparent);
  pointer-events: none;
  z-index: 1;
}

/* ================================================================
   Section Styles (shared)
   ================================================================ */
.section {
  padding: 100px 0;
}

.section-alt {
  background: var(--color-bg-alt);
}

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
   Process Timeline
   ================================================================ */
.process-timeline {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 0;
  max-width: var(--max-width);
  margin: 0 auto;
}

.process-step {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 24px 12px;
  position: relative;
}

/* Connecting line between steps */
.process-step:not(:last-child)::after {
  content: '';
  position: absolute;
  top: 44px;
  left: calc(50% + 20px);
  width: calc(100% - 40px);
  height: 2px;
  background: linear-gradient(90deg, var(--color-primary-light), var(--color-border));
  z-index: 0;
}

/* -- Step badge -- */
.process-step__badge {
  position: relative;
  z-index: 1;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #1677ff 0%, #4f46e5 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.3);
  flex-shrink: 0;
}

.process-step__number {
  font-size: 16px;
  font-weight: 700;
  color: #ffffff;
}

/* -- Step body -- */
.process-step__body {
  position: relative;
  z-index: 1;
}

.process-step__title {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 8px;
}

.process-step__desc {
  font-size: 13px;
  color: var(--color-text-muted);
  line-height: 1.5;
}

/* ================================================================
   Job List Section
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
   Campus Talks Schedule
   ================================================================ */
.talks-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 24px;
  max-width: var(--max-width);
  margin: 0 auto;
}

.talk-card {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 24px;
  background: var(--color-bg-card);
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border-light);
  box-shadow: var(--shadow-sm);
  transition:
    transform var(--transition-slow),
    box-shadow var(--transition-slow),
    border-color var(--transition-slow);
}

.talk-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-lg);
  border-color: rgba(79, 70, 229, 0.15);
}

/* -- Date block -- */
.talk-card__date {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 72px;
  background: linear-gradient(135deg, #1677ff 0%, #06b6d4 100%);
  border-radius: var(--radius-md);
  color: #fff;
  flex-shrink: 0;
}

.talk-card__month {
  font-size: 13px;
  font-weight: 500;
  opacity: 0.85;
  line-height: 1.3;
}

.talk-card__day {
  font-size: 26px;
  font-weight: 800;
  line-height: 1.1;
}

/* -- Info -- */
.talk-card__info {
  flex: 1;
  min-width: 0;
}

.talk-card__school {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 6px;
}

.talk-card__venue,
.talk-card__time {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--color-text-muted);
  line-height: 1.5;
}

.talk-card__venue svg,
.talk-card__time svg {
  flex-shrink: 0;
}

/* -- Status badge -- */
.talk-card__status {
  display: inline-block;
  padding: 4px 12px;
  font-size: 12px;
  font-weight: 600;
  border-radius: var(--radius-full);
  flex-shrink: 0;
  white-space: nowrap;
}

.talk-card__status--upcoming {
  background: rgba(16, 185, 129, 0.1);
  color: var(--color-success);
}

.talk-card__status--ended {
  background: rgba(148, 163, 184, 0.1);
  color: var(--color-text-muted);
}

/* ================================================================
   Responsive — max-width 768px
   ================================================================ */
@media (max-width: 768px) {
  .campus-hero {
    padding: 100px 20px 60px;
    min-height: auto;
  }

  .campus-hero__title {
    font-size: clamp(34px, 8vw, 48px);
  }

  .campus-hero__desc {
    font-size: 16px;
    max-width: 480px;
  }

  .campus-hero__cta {
    width: 100%;
    justify-content: center;
    height: 48px;
    font-size: 15px;
    margin-bottom: 56px;
  }

  .campus-hero__stats {
    grid-template-columns: repeat(3, 1fr);
    max-width: 480px;
  }

  .campus-hero__stat-number {
    font-size: 34px;
  }

  .campus-hero__stat-label {
    font-size: 12px;
  }

  /* Adjust orb sizes */
  .campus-hero__orb--1 {
    width: 260px;
    height: 260px;
  }

  .campus-hero__orb--2 {
    width: 240px;
    height: 240px;
  }

  .campus-hero__orb--3 {
    width: 200px;
    height: 200px;
  }

  /* Section padding */
  .section {
    padding: 72px 24px;
  }

  .section-header {
    margin-bottom: 36px;
  }

  .section-desc {
    font-size: 14px;
  }

  /* Process timeline -> vertical stack */
  .process-timeline {
    grid-template-columns: 1fr;
    gap: 0;
  }

  .process-step {
    flex-direction: row;
    align-items: center;
    gap: 16px;
    text-align: left;
    padding: 16px 0;
  }

  .process-step__badge {
    margin-bottom: 0;
    width: 36px;
    height: 36px;
  }

  .process-step__number {
    font-size: 14px;
  }

  /* Vertical connecting line */
  .process-step:not(:last-child)::after {
    top: auto;
    left: auto;
    right: auto;
    top: 36px;
    bottom: 0;
    left: 18px;
    width: 2px;
    height: auto;
  }

  .process-step__title {
    font-size: 14px;
    margin-bottom: 4px;
  }

  .process-step__desc {
    font-size: 12px;
  }

  /* Job list */
  .job-list {
    gap: 12px;
  }

  /* Talks grid -> single column */
  .talks-grid {
    grid-template-columns: 1fr;
  }
}

/* ================================================================
   Responsive — max-width 480px
   ================================================================ */
@media (max-width: 480px) {
  .campus-hero {
    padding: 90px 16px 48px;
  }

  .campus-hero__badge {
    font-size: 12px;
    padding: 5px 14px;
  }

  .campus-hero__title {
    font-size: clamp(28px, 10vw, 40px);
  }

  .campus-hero__desc {
    font-size: 15px;
    line-height: 1.65;
  }

  .campus-hero__stats {
    gap: 12px 0;
    max-width: 320px;
  }

  .campus-hero__stat {
    padding: 16px 10px;
  }

  .campus-hero__stat-number {
    font-size: 28px;
  }

  .campus-hero__stat-label {
    font-size: 11px;
  }

  .section {
    padding: 56px 24px;
  }

  .talk-card {
    flex-wrap: wrap;
    gap: 12px;
  }

  .talk-card__date {
    width: 52px;
    height: 60px;
  }

  .talk-card__day {
    font-size: 22px;
  }

  .talk-card__status {
    margin-left: auto;
  }
}
</style>

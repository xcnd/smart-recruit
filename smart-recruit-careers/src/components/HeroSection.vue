<template>
  <section ref="sectionRef" class="hero">
    <!-- Glow orbs -->
    <div class="hero__orb hero__orb--1" aria-hidden="true" />
    <div class="hero__orb hero__orb--2" aria-hidden="true" />
    <div class="hero__orb hero__orb--3" aria-hidden="true" />

    <!-- Grid pattern overlay -->
    <div class="hero__grid" aria-hidden="true" />

    <!-- Content -->
    <div class="hero__content">
      <!-- Badge -->
      <div ref="badgeRef" class="hero__badge fade-up">
        <span class="hero__badge-dot" aria-hidden="true" />
        <span>{{ badgeText }}</span>
      </div>

      <!-- Title -->
      <h1 ref="titleRef" class="hero__title fade-up">
        <span class="hero__title-main">{{ titleMain }}</span>
        <span class="hero__title-gradient">{{ titleGradient }}</span>
      </h1>

      <!-- Description -->
      <p ref="descRef" class="hero__desc fade-up">
        {{ description }}
      </p>

      <!-- CTA buttons -->
      <div ref="ctaRef" class="hero__ctas fade-up">
        <router-link to="/jobs" class="hero__cta-primary">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <circle cx="11" cy="11" r="8" />
            <line x1="21" y1="21" x2="16.65" y2="16.65" />
          </svg>
          {{ ctaPrimaryLabel }}
        </router-link>
        <a
          :href="isHomePage ? '#culture' : '/#culture'"
          class="hero__cta-outline"
          @click.prevent="handleLearnMore"
        >
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <polygon points="5 3 19 12 5 21 5 3" />
          </svg>
          {{ ctaOutlineLabel }}
        </a>
      </div>

      <!-- Stat counters -->
      <div ref="statsRef" class="hero__stats fade-up">
        <div
          v-for="(stat, index) in stats"
          :key="index"
          class="hero__stat"
        >
          <div class="hero__stat-number">
            <span class="hero__stat-value">{{ displayedValues[index] }}</span>
            <span v-if="stat.suffix" class="hero__stat-suffix">{{ stat.suffix }}</span>
          </div>
          <div class="hero__stat-label">{{ stat.label }}</div>
        </div>
      </div>
    </div>

    <!-- Bottom fade gradient -->
    <div class="hero__bottom-fade" aria-hidden="true" />
  </section>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getCareersConfig } from '@/api/config'

const route = useRoute()
const router = useRouter()

// -- Configurable content (with defaults) --
const badgeText = ref('2026 社会招聘 & 校园招聘同步开启')
const titleMain = ref('加入 SmartRecruit')
const titleGradient = ref('用 AI 重塑招聘的未来')
const description = ref('我们正在寻找对技术充满热情的你，一起打造下一代智能招聘平台。无论你是经验丰富的工程师，还是刚刚走出校园的优秀毕业生，这里都有属于你的舞台。')
const ctaPrimaryLabel = ref('查看热招职位')
const ctaOutlineLabel = ref('了解 SmartRecruit')

// -- Stat data --
interface StatItem {
  number: number
  suffix: string
  label: string
}

const DEFAULT_STATS: StatItem[] = [
  { number: 1200, suffix: '', label: '全球员工' },
  { number: 18, suffix: '', label: '全球办公城市' },
  { number: 500, suffix: '万+', label: '服务企业客户' },
  { number: 96, suffix: '%', label: '员工推荐率' },
]

const stats = ref<StatItem[]>([...DEFAULT_STATS])
const statsLoaded = ref(false)

const displayedValues = ref<number[]>(DEFAULT_STATS.map(() => 0))

// -- is home page --
const isHomePage = computed(() => route.name === 'Home')

// -- Learn more click --
function handleLearnMore() {
  if (isHomePage.value) {
    const el = document.getElementById('culture')
    el?.scrollIntoView({ behavior: 'smooth' })
  } else {
    router.push({ path: '/', hash: '#culture' })
  }
}

// -- Entrance animation (IntersectionObserver) --
const sectionRef = ref<HTMLElement | null>(null)
const badgeRef = ref<HTMLElement | null>(null)
const titleRef = ref<HTMLElement | null>(null)
const descRef = ref<HTMLElement | null>(null)
const ctaRef = ref<HTMLElement | null>(null)
const statsRef = ref<HTMLElement | null>(null)

let entranceObserver: IntersectionObserver | null = null

function setupEntrance() {
  const elements = [badgeRef.value, titleRef.value, descRef.value, ctaRef.value, statsRef.value].filter(Boolean) as HTMLElement[]

  entranceObserver = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add('visible')
        }
      })
    },
    { threshold: 0.15 }
  )

  elements.forEach((el) => entranceObserver!.observe(el))
}

// -- Animate counters --
let animationStarted = false
let statsObserver: IntersectionObserver | null = null

function easeOutCubic(t: number): number {
  return 1 - Math.pow(1 - t, 3)
}

function animateValue(index: number, target: number, duration: number = 2000) {
  const startTime = performance.now()

  function step(currentTime: number) {
    const elapsed = currentTime - startTime
    const raw = Math.min(elapsed / duration, 1)
    const eased = easeOutCubic(raw)
    const current = Math.round(eased * target)
    displayedValues.value[index] = current

    if (raw < 1) {
      requestAnimationFrame(step)
    } else {
      displayedValues.value[index] = target
    }
  }

  requestAnimationFrame(step)
}

function setupStatsObserver() {
  if (!statsRef.value) return

  statsObserver = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting && !animationStarted) {
          animationStarted = true
          nextTick(() => {
            // Stagger the counter animations slightly
            stats.value.forEach((stat, index) => {
              setTimeout(() => {
                animateValue(index, stat.number)
              }, index * 120)
            })
          })
        }
      })
    },
    { threshold: 0.3 }
  )

  statsObserver.observe(statsRef.value)
}

// -- Load config --
async function loadConfig() {
  try {
    const cfg = await getCareersConfig()
    if (cfg.careers_hero_badge) badgeText.value = cfg.careers_hero_badge
    if (cfg.careers_hero_title_main) titleMain.value = cfg.careers_hero_title_main
    if (cfg.careers_hero_title_gradient) titleGradient.value = cfg.careers_hero_title_gradient
    if (cfg.careers_hero_desc) description.value = cfg.careers_hero_desc
    if (cfg.careers_hero_cta_primary_label) ctaPrimaryLabel.value = cfg.careers_hero_cta_primary_label
    if (cfg.careers_hero_cta_outline_label) ctaOutlineLabel.value = cfg.careers_hero_cta_outline_label
    if (cfg.careers_hero_stats) {
      const parsed = JSON.parse(cfg.careers_hero_stats) as StatItem[]
      if (parsed.length > 0) {
        stats.value = parsed
        displayedValues.value = parsed.map(() => 0)
        if (animationStarted) {
          animationStarted = false
        }
      }
    }
  } catch { /* keep defaults */ }
}

// -- Lifecycle --
onMounted(() => {
  loadConfig()
  setupEntrance()
  setupStatsObserver()
})

onUnmounted(() => {
  if (entranceObserver) {
    entranceObserver.disconnect()
    entranceObserver = null
  }
  if (statsObserver) {
    statsObserver.disconnect()
    statsObserver = null
  }
})
</script>

<style scoped>
/* ================================================================
   HeroSection — full-width dark gradient hero
   ================================================================ */
.hero {
  position: relative;
  width: 100%;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(160deg, #0f172a 0%, #1e293b 30%, #1e3a5f 60%, #334155 100%);
  overflow: hidden;
  padding: 120px 32px 80px;
  isolation: isolate;
}

/* -- Glow orbs -- */
.hero__orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.4;
  pointer-events: none;
  will-change: transform;
}

.hero__orb--1 {
  width: 420px;
  height: 420px;
  background: radial-gradient(circle, rgba(99, 102, 241, 0.7) 0%, transparent 70%);
  top: -10%;
  left: -5%;
  animation: orb-float-1 12s ease-in-out infinite alternate;
}

.hero__orb--2 {
  width: 360px;
  height: 360px;
  background: radial-gradient(circle, rgba(244, 63, 94, 0.55) 0%, transparent 70%);
  top: 45%;
  right: -8%;
  animation: orb-float-2 10s ease-in-out infinite alternate;
}

.hero__orb--3 {
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(139, 92, 246, 0.6) 0%, transparent 70%);
  bottom: -10%;
  left: 35%;
  animation: orb-float-3 8s ease-in-out infinite alternate;
}

@keyframes orb-float-1 {
  0% { transform: translate(0, 0) scale(1); }
  100% { transform: translate(60px, 40px) scale(1.08); }
}

@keyframes orb-float-2 {
  0% { transform: translate(0, 0) scale(1); }
  100% { transform: translate(-50px, -30px) scale(1.12); }
}

@keyframes orb-float-3 {
  0% { transform: translate(0, 0) scale(1); }
  100% { transform: translate(30px, -50px) scale(1.05); }
}

/* -- Grid pattern overlay -- */
.hero__grid {
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
.hero__content {
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
.hero__badge {
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

.hero__badge-dot {
  display: block;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--color-success, #10b981);
  box-shadow: 0 0 10px rgba(16, 185, 129, 0.6);
  animation: pulse-dot 2s ease-in-out infinite;
}

@keyframes pulse-dot {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(1.4); }
}

/* -- Title -- */
.hero__title {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  margin-bottom: 24px;
  line-height: 1.15;
}

.hero__title-main {
  font-size: clamp(40px, 5.5vw, 68px);
  font-weight: 800;
  color: #ffffff;
  letter-spacing: -1.5px;
}

.hero__title-gradient {
  font-size: clamp(40px, 5.5vw, 68px);
  font-weight: 800;
  letter-spacing: -1.5px;
  background: linear-gradient(135deg, #818cf8 0%, #c084fc 35%, #f472b6 70%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

/* -- Description -- */
.hero__desc {
  font-size: clamp(16px, 2vw, 19px);
  color: rgba(255, 255, 255, 0.6);
  line-height: 1.75;
  max-width: 640px;
  margin: 0 auto 40px;
}

/* -- CTA buttons -- */
.hero__ctas {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 72px;
  flex-wrap: wrap;
  justify-content: center;
}

.hero__cta-primary {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 50px;
  padding: 0 32px;
  font-size: 16px;
  font-weight: 600;
  color: #ffffff;
  text-decoration: none;
  background: linear-gradient(135deg, #1677ff 0%, #4f46e5 100%);
  border-radius: var(--radius-full, 9999px);
  box-shadow: 0 6px 24px rgba(99, 102, 241, 0.4);
  transition:
    transform var(--transition, 0.2s ease),
    box-shadow var(--transition, 0.2s ease);
}

.hero__cta-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 32px rgba(99, 102, 241, 0.55);
}

.hero__cta-outline {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 50px;
  padding: 0 32px;
  font-size: 16px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  text-decoration: none;
  background: transparent;
  border: 1.5px solid rgba(255, 255, 255, 0.2);
  border-radius: var(--radius-full, 9999px);
  transition:
    background var(--transition, 0.2s ease),
    border-color var(--transition, 0.2s ease),
    transform var(--transition, 0.2s ease);
}

.hero__cta-outline:hover {
  background: rgba(255, 255, 255, 0.06);
  border-color: rgba(255, 255, 255, 0.35);
  transform: translateY(-2px);
}

/* -- Stats -- */
.hero__stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 0;
  width: 100%;
  max-width: 900px;
}

.hero__stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px 16px;
  position: relative;
}

.hero__stat:not(:last-child)::after {
  content: '';
  position: absolute;
  right: 0;
  top: 16%;
  height: 68%;
  width: 1px;
  background: rgba(255, 255, 255, 0.1);
}

.hero__stat-number {
  display: flex;
  align-items: baseline;
  margin-bottom: 6px;
}

.hero__stat-value {
  font-size: 44px;
  font-weight: 800;
  color: #ffffff;
  line-height: 1.1;
  letter-spacing: -1px;
}

.hero__stat-suffix {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-primary-light, #818cf8);
  margin-left: 3px;
}

.hero__stat-label {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.45);
  font-weight: 400;
  letter-spacing: 0.3px;
}

/* -- Bottom fade -- */
.hero__bottom-fade {
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
   Entrance animation (fade-up)
   ================================================================ */
.fade-up {
  opacity: 0;
  transform: translateY(32px);
  transition:
    opacity 0.7s cubic-bezier(0.4, 0, 0.2, 1),
    transform 0.7s cubic-bezier(0.4, 0, 0.2, 1);
}

.fade-up.visible {
  opacity: 1;
  transform: translateY(0);
}

/* Staggered delays for fade-up children */
.fade-up:nth-child(1) { transition-delay: 0.00s; }
.fade-up:nth-child(2) { transition-delay: 0.08s; }
.fade-up:nth-child(3) { transition-delay: 0.16s; }
.fade-up:nth-child(4) { transition-delay: 0.24s; }
.fade-up:nth-child(5) { transition-delay: 0.32s; }

/* ================================================================
   Responsive
   ================================================================ */
@media (max-width: 768px) {
  .hero {
    padding: 100px 20px 60px;
  }

  .hero__title-main,
  .hero__title-gradient {
    font-size: clamp(34px, 8vw, 48px);
  }

  .hero__desc {
    font-size: 16px;
    max-width: 480px;
  }

  .hero__ctas {
    flex-direction: column;
    width: 100%;
    margin-bottom: 56px;
  }

  .hero__cta-primary,
  .hero__cta-outline {
    width: 100%;
    justify-content: center;
    height: 48px;
    font-size: 15px;
  }

  .hero__stats {
    grid-template-columns: repeat(2, 1fr);
    max-width: 400px;
  }

  .hero__stat:nth-child(2)::after {
    display: none;
  }

  .hero__stat:nth-child(odd)::after {
    content: '';
  }

  .hero__stat-value {
    font-size: 36px;
  }

  .hero__stat-suffix {
    font-size: 18px;
  }

  /* Adjust orb sizes for mobile */
  .hero__orb--1 {
    width: 280px;
    height: 280px;
  }

  .hero__orb--2 {
    width: 240px;
    height: 240px;
  }

  .hero__orb--3 {
    width: 200px;
    height: 200px;
  }
}

@media (max-width: 480px) {
  .hero {
    padding: 90px 16px 48px;
  }

  .hero__badge {
    font-size: 12px;
    padding: 5px 14px;
  }

  .hero__title-main,
  .hero__title-gradient {
    font-size: clamp(28px, 10vw, 40px);
  }

  .hero__desc {
    font-size: 15px;
    line-height: 1.65;
  }

  .hero__stats {
    gap: 16px 0;
  }

  .hero__stat {
    padding: 16px 12px;
  }

  .hero__stat-value {
    font-size: 32px;
  }

  .hero__stat-suffix {
    font-size: 16px;
  }

  .hero__stat-label {
    font-size: 12px;
  }
}
</style>

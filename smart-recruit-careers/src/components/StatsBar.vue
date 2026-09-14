<template>
  <section class="stats-bar">
    <div class="container">
      <div class="stats-grid">
        <div
          v-for="(stat, index) in statsData"
          :key="index"
          class="stat-item"
        >
          <div class="stat-number">
            <span class="stat-value">{{ displayedValues[index] }}</span>
            <span class="stat-suffix">{{ stat.suffix }}</span>
          </div>
          <div class="stat-label">{{ stat.label }}</div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getCareersConfig } from '@/api/config'

interface Stat {
  number: number
  suffix: string
  label: string
}

const props = withDefaults(
  defineProps<{
    stats?: Stat[]
  }>(),
  {
    stats: () => [
      { number: 1200, suffix: '', label: '员工' },
      { number: 18, suffix: '', label: '城市' },
      { number: 500, suffix: '万+', label: '企业客户' },
      { number: 96, suffix: '%', label: '推荐率' }
    ]
  }
)

const statsData = ref<Stat[]>([...props.stats])

const displayedValues = ref<number[]>(statsData.value.map(() => 0))

function easeOutCubic(t: number): number {
  return 1 - Math.pow(1 - t, 3)
}

function animateValue(index: number, target: number, duration: number = 1600) {
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

function startAnimation() {
  statsData.value.forEach((stat, index) => {
    animateValue(index, stat.number)
  })
}

onMounted(async () => {
  try {
    const cfg = await getCareersConfig()
    if (cfg.careers_stats_items) {
      const parsed = JSON.parse(cfg.careers_stats_items) as Stat[]
      if (parsed.length > 0) statsData.value = parsed
    }
  } catch { /* keep defaults */ }
  startAnimation()
})
</script>

<style scoped>
.stats-bar {
  padding: 60px 0;
  background: var(--color-bg);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 0;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 32px 16px;
  position: relative;
}

.stat-item:not(:last-child)::after {
  content: '';
  position: absolute;
  right: 0;
  top: 20%;
  height: 60%;
  width: 1px;
  background: var(--color-border-light);
}

.stat-number {
  display: flex;
  align-items: baseline;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 48px;
  font-weight: 700;
  color: var(--color-primary);
  line-height: 1.1;
  letter-spacing: -1px;
}

.stat-suffix {
  font-size: 24px;
  font-weight: 600;
  color: var(--color-primary);
  margin-left: 2px;
}

.stat-label {
  font-size: 14px;
  color: var(--color-text-muted);
  font-weight: 400;
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .stat-item:nth-child(2)::after {
    display: none;
  }

  .stat-item:nth-child(1)::after,
  .stat-item:nth-child(3)::after {
    right: 0;
    top: 20%;
    height: 60%;
    width: 1px;
    background: var(--color-border-light);
  }

  .stat-value {
    font-size: 36px;
  }

  .stat-suffix {
    font-size: 20px;
  }
}

@media (max-width: 480px) {
  .stat-value {
    font-size: 30px;
  }
}
</style>

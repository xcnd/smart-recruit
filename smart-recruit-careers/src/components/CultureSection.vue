<template>
  <section id="culture" class="culture-section">
    <div class="container">
      <div class="section-header">
        <h2 class="section-title">{{ sectionTitle }}</h2>
        <p class="section-subtitle">{{ sectionSubtitle }}</p>
      </div>
      <div class="culture-grid">
        <div
          v-for="(card, index) in cultureCards"
          :key="index"
          class="culture-card"
        >
          <div class="card-gradient-bar"></div>
          <div class="card-icon" :class="card.iconBg">
            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="1.8"
              stroke-linecap="round"
              stroke-linejoin="round"
              width="22"
              height="22"
              v-html="card.iconPath"
            ></svg>
          </div>
          <h3 class="card-title">{{ card.title }}</h3>
          <p class="card-desc">{{ card.description }}</p>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getCareersConfig } from '@/api/config'

interface CultureCard {
  title: string
  description: string
  iconBg: string
  iconPath: string
}

const ICON_PATH_MAP: Record<string, string> = {
  'icon-purple':  '<path d="M12 2L2 7l10 5 10-5-10-5z"/><path d="M2 17l10 5 10-5"/><path d="M2 12l10 5 10-5"/>',
  'icon-rose':    '<polyline points="22 7 13.5 15.5 8.5 10.5 2 17"/><polyline points="16 7 22 7 22 13"/>',
  'icon-amber':   '<path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/>',
  'icon-emerald': '<line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/><line x1="2" y1="20" x2="22" y2="20"/>',
  'icon-cyan':    '<rect x="2" y="3" width="20" height="14" rx="2" ry="2"/><line x1="8" y1="21" x2="16" y2="21"/><line x1="12" y1="17" x2="12" y2="21"/>',
  'icon-indigo':  '<polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/>',
}

const DEFAULT_CARDS: CultureCard[] = [
  {
    title: '技术驱动创新',
    description: '深度投入 AI / LLM 前沿技术领域，每年技术投入占比营收 25%+，让工程师站在技术浪潮之巅。',
    iconBg: 'icon-purple',
    iconPath: ICON_PATH_MAP['icon-purple']
  },
  {
    title: '高速成长通道',
    description: '双阶梯晋升体系（管理 + 专家），每半年一次晋升窗口，优秀人才不受年限破格提拔。',
    iconBg: 'icon-rose',
    iconPath: ICON_PATH_MAP['icon-rose']
  },
  {
    title: '开放包容文化',
    description: '扁平化组织，坦诚清晰沟通，CEO 定期全员 AMA。多元背景人才汇聚，尊重每一种声音。',
    iconBg: 'icon-amber',
    iconPath: ICON_PATH_MAP['icon-amber']
  },
  {
    title: '数据驱动决策',
    description: 'A/B 实验文化深入骨髓，从产品功能到内部流程，一切以数据说话，拒绝拍脑袋。',
    iconBg: 'icon-emerald',
    iconPath: ICON_PATH_MAP['icon-emerald']
  },
  {
    title: '顶级工具与资源',
    description: 'MacBook Pro + 4K 显示器标配，正版 IDE / AI 工具全报销。提供丰富技术会议与培训资源。',
    iconBg: 'icon-cyan',
    iconPath: ICON_PATH_MAP['icon-cyan']
  },
  {
    title: '创业精神永续',
    description: '保持 Day 1 心态，鼓励内部创业与孵化项目。优秀内部项目可获得种子投资与独立运营机会。',
    iconBg: 'icon-indigo',
    iconPath: ICON_PATH_MAP['icon-indigo']
  }
]

const cultureCards = ref<CultureCard[]>(DEFAULT_CARDS)
const sectionTitle = ref('我们的文化')
const sectionSubtitle = ref('六大核心价值观驱动我们不断前进')

function enrichCard(card: { title: string; description: string; iconBg: string }): CultureCard {
  return {
    title: card.title,
    description: card.description,
    iconBg: card.iconBg,
    iconPath: ICON_PATH_MAP[card.iconBg] || ICON_PATH_MAP['icon-purple'],
  }
}

onMounted(async () => {
  try {
    const cfg = await getCareersConfig()
    if (cfg.careers_culture_title) sectionTitle.value = cfg.careers_culture_title
    if (cfg.careers_culture_subtitle) sectionSubtitle.value = cfg.careers_culture_subtitle
    if (cfg.careers_culture_cards) {
      const parsed = JSON.parse(cfg.careers_culture_cards) as Array<{ title: string; description: string; iconBg: string }>
      if (parsed.length > 0) cultureCards.value = parsed.map(enrichCard)
    }
  } catch { /* keep defaults */ }
})
</script>

<style scoped>
.culture-section {
  padding: 80px 0;
  background: var(--color-bg-alt);
}

.section-header {
  text-align: center;
  margin-bottom: 48px;
}

.section-title {
  font-size: 32px;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: 12px;
}

.section-subtitle {
  font-size: 16px;
  color: var(--color-text-secondary);
}

.culture-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

.culture-card {
  background: var(--color-bg-card);
  border-radius: var(--radius-lg);
  padding: 32px 28px;
  position: relative;
  overflow: hidden;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--color-border-light);
  transition: transform var(--transition-slow), box-shadow var(--transition-slow), border-color var(--transition-slow);
}

.culture-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
  border-color: var(--color-border);
}

.card-gradient-bar {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, var(--color-primary), var(--color-accent));
  transform: scaleX(0);
  transform-origin: left;
  transition: transform var(--transition-slow);
}

.culture-card:hover .card-gradient-bar {
  transform: scaleX(1);
}

.card-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
}

.card-icon svg {
  display: block;
}

.icon-purple {
  background: rgba(99, 102, 241, 0.1);
  color: var(--color-primary);
}

.icon-rose {
  background: rgba(244, 63, 94, 0.1);
  color: #f43f5e;
}

.icon-amber {
  background: rgba(245, 158, 11, 0.1);
  color: #f59e0b;
}

.icon-emerald {
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
}

.icon-cyan {
  background: rgba(6, 182, 212, 0.1);
  color: #06b6d4;
}

.icon-indigo {
  background: rgba(99, 102, 241, 0.12);
  color: var(--color-primary);
}

.card-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: 8px;
}

.card-desc {
  font-size: 14px;
  color: var(--color-text-secondary);
  line-height: 1.7;
}

@media (max-width: 1024px) {
  .culture-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .culture-grid {
    grid-template-columns: 1fr;
  }

  .culture-section {
    padding: 60px 0;
  }

  .section-title {
    font-size: 26px;
  }
}
</style>

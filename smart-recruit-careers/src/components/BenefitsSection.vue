<template>
  <section id="benefits" class="benefits-section">
    <div class="container">
      <div class="section-header">
        <h2 class="section-title">{{ sectionTitle }}</h2>
        <p class="section-subtitle">{{ sectionSubtitle }}</p>
      </div>
      <div class="benefits-grid">
        <div
          v-for="(benefit, index) in benefits"
          :key="index"
          class="benefit-item"
        >
          <div class="benefit-icon" :class="benefit.colorClass">
            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="1.8"
              stroke-linecap="round"
              stroke-linejoin="round"
              width="20"
              height="20"
              v-html="benefit.iconPath"
            ></svg>
          </div>
          <div class="benefit-content">
            <h3 class="benefit-title">{{ benefit.title }}</h3>
            <p class="benefit-desc">{{ benefit.description }}</p>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getCareersConfig } from '@/api/config'

interface Benefit {
  title: string
  description: string
  colorClass: string
  iconPath: string
}

const DEFAULT_ICON_PATHS: string[] = [
  '<circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="16"/><line x1="8" y1="12" x2="16" y2="12"/>',
  '<path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>',
  '<circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>',
  '<path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"/><path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"/>',
  '<line x1="12" y1="1" x2="12" y2="23"/><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/>',
  '<path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/>',
  '<polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>',
  '<circle cx="12" cy="12" r="10"/><line x1="2" y1="12" x2="22" y2="12"/><path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"/>',
]

const DEFAULT_BENEFITS: Benefit[] = [
  { title: '具有竞争力的薪酬', description: '16 薪起 + 年度调薪，优秀人才享签字费与签约奖金，薪资水平对标一线大厂。', colorClass: 'c1', iconPath: DEFAULT_ICON_PATHS[0] },
  { title: '全方位健康保障', description: '六险一金（含补充商业保险），年度高端体检，家属体检折扣，EAP 心理援助计划。', colorClass: 'c2', iconPath: DEFAULT_ICON_PATHS[1] },
  { title: '弹性工作与假期', description: '弹性上下班 + 混合办公（每周可选 2 天远程），12 天带薪年假起，带薪病假不限额。', colorClass: 'c3', iconPath: DEFAULT_ICON_PATHS[2] },
  { title: '成长学习基金', description: '年度 8,000 元学习基金，覆盖课程 / 书籍 / 会议。内部技术分享 + 外部专家讲座常态化。', colorClass: 'c4', iconPath: DEFAULT_ICON_PATHS[3] },
  { title: '股权激励计划', description: '核心岗位授予期权 / RSU，与公司共享成长红利。每年新增授予以持续激励。', colorClass: 'c5', iconPath: DEFAULT_ICON_PATHS[4] },
  { title: '安居乐业支持', description: '安家补贴 + 租房补贴，首次入职异地搬迁全额报销。购房免息借款助力安家。', colorClass: 'c6', iconPath: DEFAULT_ICON_PATHS[5] },
  { title: '专利与论文激励', description: '专利申请奖 10,000 元起，顶级会议论文奖金 20,000 元。鼓励技术创新与学术贡献。', colorClass: 'c7', iconPath: DEFAULT_ICON_PATHS[6] },
  { title: '全球轮岗机会', description: '北京 / 上海 / 深圳 / 杭州等城市自由轮岗，未来开放海外办公室短期交换机会。', colorClass: 'c8', iconPath: DEFAULT_ICON_PATHS[7] },
]

const benefits = ref<Benefit[]>(DEFAULT_BENEFITS)
const sectionTitle = ref('薪酬福利')
const sectionSubtitle = ref('我们提供行业领先的薪酬与福利体系，关爱每一位伙伴')

function enrichBenefit(item: { title: string; description: string; colorClass: string }, idx: number): Benefit {
  return {
    title: item.title,
    description: item.description,
    colorClass: item.colorClass,
    iconPath: DEFAULT_ICON_PATHS[idx % DEFAULT_ICON_PATHS.length],
  }
}

onMounted(async () => {
  try {
    const cfg = await getCareersConfig()
    if (cfg.careers_benefits_title) sectionTitle.value = cfg.careers_benefits_title
    if (cfg.careers_benefits_subtitle) sectionSubtitle.value = cfg.careers_benefits_subtitle
    if (cfg.careers_benefits_items) {
      const parsed = JSON.parse(cfg.careers_benefits_items) as Array<{ title: string; description: string; colorClass: string }>
      if (parsed.length > 0) benefits.value = parsed.map(enrichBenefit)
    }
  } catch { /* keep defaults */ }
})
</script>

<style scoped>
.benefits-section {
  padding: 80px 0;
  background: var(--color-bg);
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

.benefits-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.benefit-item {
  display: flex;
  gap: 16px;
  padding: 28px 28px;
  border-right: 1px solid var(--color-border-light);
  border-bottom: 1px solid var(--color-border-light);
  transition: background var(--transition);
}

.benefit-item:nth-child(2n) {
  border-right: none;
}

.benefit-item:nth-child(7),
.benefit-item:nth-child(8) {
  border-bottom: none;
}

.benefit-item:hover {
  background: var(--color-bg-alt);
}

.benefit-icon {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.benefit-icon svg {
  display: block;
}

/* Color classes c1-c8 */
.c1 { background: rgba(22,119,255,0.1); color: #1677ff; }
.c2 { background: rgba(244, 63, 94, 0.1); color: #f43f5e; }
.c3 { background: rgba(245, 158, 11, 0.1); color: #f59e0b; }
.c4 { background: rgba(16, 185, 129, 0.1); color: #10b981; }
.c5 { background: rgba(22,119,255,0.12); color: #1677ff; }
.c6 { background: rgba(6, 182, 212, 0.1); color: #06b6d4; }
.c7 { background: rgba(168, 85, 247, 0.1); color: #a855f7; }
.c8 { background: rgba(34, 197, 94, 0.1); color: #22c55e; }

.benefit-content {
  flex: 1;
  min-width: 0;
}

.benefit-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: 6px;
}

.benefit-desc {
  font-size: 13px;
  color: var(--color-text-muted);
  line-height: 1.7;
}

@media (max-width: 1024px) {
  .benefits-grid {
    grid-template-columns: 1fr;
  }

  .benefit-item {
    border-right: none;
  }

  .benefit-item:nth-child(7) {
    border-bottom: 1px solid var(--color-border-light);
  }

  .benefit-item:nth-child(8) {
    border-bottom: none;
  }
}

@media (max-width: 640px) {
  .benefits-section {
    padding: 60px 0;
  }

  .section-title {
    font-size: 26px;
  }
}
</style>

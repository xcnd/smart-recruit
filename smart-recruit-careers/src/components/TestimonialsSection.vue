<template>
  <section class="testimonials-section">
    <div class="container">
      <div class="section-header">
        <h2 class="section-title">{{ sectionTitle }}</h2>
        <p class="section-subtitle">{{ sectionSubtitle }}</p>
      </div>
      <div class="testimonials-grid">
        <div
          v-for="(testimonial, index) in testimonials"
          :key="index"
          class="testimonial-card"
        >
          <blockquote class="testimonial-quote">
            "{{ testimonial.quote }}"
          </blockquote>
          <div class="testimonial-author">
            <div class="author-avatar" :class="testimonial.avatarClass">
              {{ testimonial.initials }}
            </div>
            <div class="author-info">
              <div class="author-name">{{ testimonial.name }}</div>
              <div class="author-role">{{ testimonial.role }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getCareersConfig } from '@/api/config'

interface Testimonial {
  quote: string
  name: string
  initials: string
  role: string
  avatarClass: string
}

const DEFAULT_TESTIMONIALS: Testimonial[] = [
  { quote: '加入 SmartRecruit 三年，从一名普通工程师成长为 AI 算法负责人。这里不仅有顶尖的技术氛围，更重要的是给了我充分的自主权和试错空间。每年两次的晋升窗口让成长路径非常清晰。', name: '张明远', initials: '张', role: 'AI 算法专家 · 2023 年加入', avatarClass: 'ta1' },
  { quote: '作为两个孩子的妈妈，我最看重的就是工作与生活的平衡。SmartRecruit 的弹性工作制和混合办公政策让我既能全心投入工作，又不错过孩子的成长。公司对女性员工的关怀非常到位。', name: '林晓萌', initials: '林', role: '产品总监 · 2022 年加入', avatarClass: 'ta2' },
  { quote: '在上一家公司做了五年螺丝钉，来到 SmartRecruit 最大的感受是「被看见」。你的想法会被认真对待，做出的成果会被认可。扁平化的组织让新人也有机会直接跟 VP 级别的前辈交流。', name: '陈浩然', initials: '陈', role: '高级后端工程师 · 2024 年加入', avatarClass: 'ta3' },
]

const testimonials = ref<Testimonial[]>(DEFAULT_TESTIMONIALS)
const sectionTitle = ref('员工心声')
const sectionSubtitle = ref('听听我们的伙伴怎么说')

onMounted(async () => {
  try {
    const cfg = await getCareersConfig()
    if (cfg.careers_testimonials_title) sectionTitle.value = cfg.careers_testimonials_title
    if (cfg.careers_testimonials_subtitle) sectionSubtitle.value = cfg.careers_testimonials_subtitle
    if (cfg.careers_testimonials) {
      const parsed = JSON.parse(cfg.careers_testimonials) as Testimonial[]
      if (parsed.length > 0) testimonials.value = parsed
    }
  } catch { /* keep defaults */ }
})
</script>

<style scoped>
.testimonials-section {
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

.testimonials-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

.testimonial-card {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-lg);
  padding: 28px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  transition: box-shadow var(--transition), transform var(--transition);
}

.testimonial-card:hover {
  box-shadow: var(--shadow-lg);
  transform: translateY(-2px);
}

.testimonial-quote {
  font-size: 14px;
  color: var(--color-text-secondary);
  line-height: 1.8;
  margin-bottom: 24px;
  padding-left: 16px;
  border-left: 3px solid var(--color-primary);
  font-style: normal;
}

.testimonial-author {
  display: flex;
  align-items: center;
  gap: 12px;
}

.author-avatar {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-full);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 700;
  color: #fff;
  flex-shrink: 0;
}

.ta1 { background: linear-gradient(135deg, #1677ff, #4f46e5); }
.ta2 { background: linear-gradient(135deg, #f43f5e, #fb7185); }
.ta3 { background: linear-gradient(135deg, #10b981, #34d399); }

.author-info {
  min-width: 0;
}

.author-name {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: 2px;
}

.author-role {
  font-size: 12px;
  color: var(--color-text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

@media (max-width: 1024px) {
  .testimonials-grid {
    grid-template-columns: 1fr;
    max-width: 600px;
    margin: 0 auto;
  }
}

@media (max-width: 640px) {
  .testimonials-section {
    padding: 60px 0;
  }

  .section-title {
    font-size: 26px;
  }
}
</style>

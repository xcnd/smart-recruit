<template>
  <section id="faq" class="faq-section">
    <div class="container">
      <div class="section-header">
        <h2 class="section-title">{{ sectionTitle }}</h2>
        <p class="section-subtitle">{{ sectionSubtitle }}</p>
      </div>
      <div class="faq-list">
        <div
          v-for="(item, index) in faqItems"
          :key="index"
          class="faq-item"
          :class="{ active: activeIndex === index }"
        >
          <button
            class="faq-question"
            @click="toggle(index)"
          >
            <span>{{ item.question }}</span>
            <svg
              class="faq-arrow"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
              width="20"
              height="20"
            >
              <polyline points="6 9 12 15 18 9"/>
            </svg>
          </button>
          <div class="faq-answer-wrapper" ref="faqAnswerWrappers">
            <div class="faq-answer">
              <p>{{ item.answer }}</p>
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

interface FaqItem {
  question: string
  answer: string
}

const DEFAULT_FAQ: FaqItem[] = [
  {
    question: '招聘流程是怎样的？',
    answer: '简历投递 → 简历筛选（1-3 个工作日）→ 技术面试（2-3 轮，通常含编程考察与系统设计）→ HR 面试（文化匹配与职业规划）→ Offer 沟通 → 正式入职。整体流程通常 2-3 周内完成。'
  },
  {
    question: '是否支持远程办公？',
    answer: '我们采用混合办公模式，每周可选 2 天远程办公，核心协作时间（如重要会议、团队站会）需到岗参与。部分岗位（如标注、客服）支持全远程。'
  },
  {
    question: '技术面试主要考察什么？',
    answer: '我们不考八股文，更关注计算机基础（数据结构、算法复杂度）、工程能力（代码质量、系统设计、调试思路）以及领域深度。面试过程是双向交流，也欢迎你反向考察团队。'
  },
  {
    question: '应届生有培训体系吗？',
    answer: '有的。\u201c星火计划\u201d是我们为新人和应届生设计的系统培养体系，包括：① 1 对 1 Mentor 带教；② 6 个月轮岗了解公司核心业务；③ 定期的技术沙龙与软技能培训；④ 毕业答辩 + 定岗定级。'
  },
  {
    question: '可以同时投递多个岗位吗？',
    answer: '最多同时投递 3 个岗位。我们建议根据自身兴趣与能力精准投递，HR 也会在筛选阶段结合你的背景推荐最合适的岗位方向。'
  }
]

const faqItems = ref<FaqItem[]>([...DEFAULT_FAQ])
const sectionTitle = ref('常见问题')
const sectionSubtitle = ref('关于招聘的常见疑问，这里都有答案')

const activeIndex = ref<number | null>(null)

function toggle(index: number) {
  if (activeIndex.value === index) {
    activeIndex.value = null
  } else {
    activeIndex.value = index
  }
}

onMounted(async () => {
  try {
    const cfg = await getCareersConfig()
    if (cfg.careers_faq_title) sectionTitle.value = cfg.careers_faq_title
    if (cfg.careers_faq_subtitle) sectionSubtitle.value = cfg.careers_faq_subtitle
    if (cfg.careers_faq_items) {
      const parsed = JSON.parse(cfg.careers_faq_items) as FaqItem[]
      if (parsed.length > 0) faqItems.value = parsed
    }
  } catch { /* keep defaults */ }
})
</script>

<style scoped>
.faq-section {
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

.faq-list {
  max-width: 760px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.faq-item {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-md);
  overflow: hidden;
  transition: border-color var(--transition), box-shadow var(--transition);
}

.faq-item.active {
  border-color: var(--color-primary);
  box-shadow: var(--shadow-md);
}

.faq-question {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 20px 24px;
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
  text-align: left;
  background: none;
  border: none;
  cursor: pointer;
  transition: color var(--transition);
}

.faq-item.active .faq-question {
  color: var(--color-primary);
}

.faq-arrow {
  flex-shrink: 0;
  color: var(--color-text-muted);
  transition: transform var(--transition-slow);
}

.faq-item.active .faq-arrow {
  transform: rotate(180deg);
  color: var(--color-primary);
}

.faq-answer-wrapper {
  display: grid;
  grid-template-rows: 0fr;
  transition: grid-template-rows var(--transition-slow);
}

.faq-item.active .faq-answer-wrapper {
  grid-template-rows: 1fr;
}

.faq-answer {
  overflow: hidden;
  padding: 0 24px;
}

.faq-item.active .faq-answer {
  padding-bottom: 20px;
}

.faq-answer p {
  font-size: 14px;
  color: var(--color-text-secondary);
  line-height: 1.8;
}

@media (max-width: 640px) {
  .faq-section {
    padding: 60px 0;
  }

  .section-title {
    font-size: 26px;
  }

  .faq-question {
    padding: 16px 20px;
    font-size: 14px;
  }

  .faq-item.active .faq-answer {
    padding: 0 20px 16px;
  }
}
</style>

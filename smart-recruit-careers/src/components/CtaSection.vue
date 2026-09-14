<template>
  <section class="cta-section">
    <!-- Glow orb -->
    <div class="cta-orb"></div>
    <div class="container cta-content">
      <h2 class="cta-title">{{ title }}</h2>
      <p class="cta-description">{{ desc }}</p>
      <router-link :to="btnLink" class="cta-button">
        <span>{{ btnLabel }}</span>
        <svg
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
          width="18"
          height="18"
        >
          <line x1="5" y1="12" x2="19" y2="12"/>
          <polyline points="12 5 19 12 12 19"/>
        </svg>
      </router-link>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getCareersConfig } from '@/api/config'

const title = ref('准备好加入我们了吗？')
const desc = ref('和一群优秀的人，做一些有意义的事。你的下一段职业生涯，从这里开始。')
const btnLabel = ref('查看热招职位')
const btnLink = ref('/jobs')

onMounted(async () => {
  try {
    const cfg = await getCareersConfig()
    if (cfg.careers_cta_title) title.value = cfg.careers_cta_title
    if (cfg.careers_cta_desc) desc.value = cfg.careers_cta_desc
    if (cfg.careers_cta_btn_label) btnLabel.value = cfg.careers_cta_btn_label
    if (cfg.careers_cta_btn_link) btnLink.value = cfg.careers_cta_btn_link
  } catch { /* keep defaults */ }
})
</script>

<style scoped>
.cta-section {
  position: relative;
  background: linear-gradient(135deg, #0f172a 0%, #1e3a5f 50%, #0f172a 100%);
  padding: 100px 0 110px;
  overflow: hidden;
  text-align: center;
}

/* Glow orb */
.cta-orb {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 500px;
  height: 500px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(99, 102, 241, 0.25) 0%, transparent 70%);
  pointer-events: none;
  animation: orb-pulse 4s ease-in-out infinite;
}

@keyframes orb-pulse {
  0%, 100% {
    transform: translate(-50%, -50%) scale(1);
    opacity: 0.6;
  }
  50% {
    transform: translate(-50%, -50%) scale(1.15);
    opacity: 0.9;
  }
}

.cta-content {
  position: relative;
  z-index: 1;
}

.cta-title {
  font-size: clamp(28px, 5vw, 44px);
  font-weight: 700;
  color: #fff;
  margin-bottom: 16px;
  line-height: 1.2;
}

.cta-description {
  font-size: 17px;
  color: rgba(255, 255, 255, 0.55);
  margin-bottom: 36px;
  max-width: 500px;
  margin-left: auto;
  margin-right: auto;
  line-height: 1.6;
}

.cta-button {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 15px 36px;
  background: #fff;
  color: var(--color-primary);
  font-size: 16px;
  font-weight: 600;
  border-radius: var(--radius-full);
  text-decoration: none;
  transition: transform var(--transition), box-shadow var(--transition-slow);
  box-shadow: 0 4px 20px rgba(99, 102, 241, 0.3);
}

.cta-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 32px rgba(99, 102, 241, 0.45);
}

.cta-button svg {
  transition: transform var(--transition);
}

.cta-button:hover svg {
  transform: translateX(3px);
}

@media (max-width: 640px) {
  .cta-section {
    padding: 70px 0 80px;
  }

  .cta-button {
    padding: 13px 30px;
    font-size: 15px;
  }
}
</style>

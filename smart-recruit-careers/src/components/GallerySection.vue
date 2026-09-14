<template>
  <section id="life" class="gallery-section">
    <div class="container">
      <div class="section-header">
        <h2 class="section-title">{{ sectionTitle }}</h2>
        <p class="section-subtitle">{{ sectionSubtitle }}</p>
      </div>
    </div>
    <div class="gallery-scroll" ref="scrollContainer">
      <div class="gallery-track">
        <div
          v-for="(item, index) in galleryItems"
          :key="index"
          class="gallery-card"
        >
          <div class="gallery-image" :class="item.gradientClass">
            <img
              v-if="item.image"
              v-show="!imgBroken[index]"
              :src="item.image"
              :alt="item.caption"
              class="gallery-photo"
              loading="lazy"
              @error="onImgError(index)"
              @load="onImgLoad(index)"
            />
            <div class="gallery-overlay">
              <h3 class="gallery-caption">{{ item.caption }}</h3>
              <p class="gallery-subcaption">{{ item.subcaption }}</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getCareersConfig } from '@/api/config'

interface GalleryItem {
  caption: string
  subcaption: string
  gradientClass: string
  image?: string
}

const DEFAULT_GALLERY: GalleryItem[] = [
  {
    caption: '黑客马拉松 2026',
    subcaption: '48 小时极限编程挑战',
    gradientClass: 'g1',
    image: 'https://picsum.photos/id/60/680/520',
  },
  {
    caption: '团队 Offsite 团建',
    subcaption: '在山水间凝聚团队力量',
    gradientClass: 'g2',
    image: 'https://picsum.photos/id/1/680/520',
  },
  {
    caption: '开放式办公环境',
    subcaption: '激发创造力的灵感空间',
    gradientClass: 'g3',
    image: 'https://picsum.photos/id/10/680/520',
  },
  {
    caption: 'AI 技术分享会',
    subcaption: '与大咖共话技术前沿',
    gradientClass: 'g4',
    image: 'https://picsum.photos/id/20/680/520',
  },
  {
    caption: '年会盛典',
    subcaption: '年度高光时刻',
    gradientClass: 'g5',
    image: 'https://picsum.photos/id/26/680/520',
  },
  {
    caption: '新员工训练营',
    subcaption: '星火计划 · 融入之旅',
    gradientClass: 'g1',
    image: 'https://picsum.photos/id/30/680/520',
  },
]

const galleryItems = ref<GalleryItem[]>([...DEFAULT_GALLERY])
const sectionTitle = ref('我们的团队')
const sectionSubtitle = ref('记录每一个精彩瞬间，一起创造美好回忆')
const scrollContainer = ref<HTMLElement | null>(null)

// Track which images failed to load → fall back to gradient
const imgBroken = reactive<Record<number, boolean>>({})

function onImgError(index: number) {
  imgBroken[index] = true
}

function onImgLoad(index: number) {
  imgBroken[index] = false
}

onMounted(async () => {
  try {
    const cfg = await getCareersConfig()
    if (cfg.careers_gallery_title) sectionTitle.value = cfg.careers_gallery_title
    if (cfg.careers_gallery_subtitle) sectionSubtitle.value = cfg.careers_gallery_subtitle
    if (cfg.careers_gallery_items) {
      const parsed = JSON.parse(cfg.careers_gallery_items) as GalleryItem[]
      if (parsed.length > 0) galleryItems.value = parsed
    }
  } catch { /* keep defaults */ }
})
</script>

<style scoped>
.gallery-section {
  padding: 80px 0 100px;
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

.gallery-scroll {
  overflow-x: auto;
  scroll-snap-type: x mandatory;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: none;
  padding: 0 calc((100vw - var(--max-width)) / 2);
}

.gallery-scroll::-webkit-scrollbar {
  display: none;
}

.gallery-track {
  display: flex;
  gap: 20px;
  padding: 0 32px;
}

.gallery-card {
  min-width: 340px;
  width: 340px;
  border-radius: var(--radius-lg);
  overflow: hidden;
  scroll-snap-align: start;
  cursor: pointer;
  box-shadow: var(--shadow-md);
  transition: transform var(--transition-slow);
}

.gallery-card:hover {
  transform: scale(1.02);
}

.gallery-image {
  height: 260px;
  position: relative;
  overflow: hidden;
  transition: transform var(--transition-slow);
}

.gallery-card:hover .gallery-image {
  transform: scale(1.06);
}

/* Gradient backgrounds — fallback when no image */
.g1 { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }
.g2 { background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); }
.g3 { background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%); }
.g4 { background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%); }
.g5 { background: linear-gradient(135deg, #fa709a 0%, #fee140 100%); }

/* Photo <img> tag — sits above gradient */
.gallery-photo {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  z-index: 0;
}

.gallery-overlay {
  position: absolute;
  inset: 0;
  z-index: 1;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.6) 0%, rgba(0, 0, 0, 0) 50%);
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding: 24px;
}

.gallery-caption {
  font-size: 18px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 4px;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
}

.gallery-subcaption {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.75);
}

@media (max-width: 768px) {
  .gallery-section {
    padding: 60px 0 80px;
  }

  .gallery-track {
    padding: 0 20px;
  }

  .gallery-card {
    min-width: 280px;
    width: 280px;
  }

  .gallery-image {
    height: 220px;
  }

  .section-title {
    font-size: 26px;
  }
}
</style>

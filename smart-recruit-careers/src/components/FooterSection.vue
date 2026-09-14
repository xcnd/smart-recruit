<template>
  <footer class="footer">
    <div class="container">
      <div class="footer-grid">
        <!-- Brand column -->
        <div class="footer-col footer-brand">
          <div class="footer-logo">
            <svg
              viewBox="0 0 32 32"
              fill="none"
              width="32"
              height="32"
            >
              <rect width="32" height="32" rx="8" fill="#1677ff"/>
              <path d="M9 23V9l7 9.5L23 9v14" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" fill="none"/>
            </svg>
            <span class="logo-text">SmartRecruit</span>
          </div>
          <p class="footer-desc">{{ brandDesc }}</p>
        </div>

        <!-- Job categories -->
        <div class="footer-col">
          <h4 class="footer-col-title">{{ categoryTitle }}</h4>
          <ul class="footer-links">
            <li v-for="link in categoryLinks" :key="link.href">
              <a :href="link.href">{{ link.label }}</a>
            </li>
          </ul>
        </div>

        <!-- About us -->
        <div class="footer-col">
          <h4 class="footer-col-title">{{ aboutTitle }}</h4>
          <ul class="footer-links">
            <li v-for="link in aboutLinks" :key="link.label">
              <a :href="link.href">{{ link.label }}</a>
            </li>
          </ul>
        </div>

        <!-- Contact -->
        <div class="footer-col">
          <h4 class="footer-col-title">联系我们</h4>
          <ul class="footer-links footer-contact">
            <li>
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="14" height="14">
                <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
                <polyline points="22,6 12,13 2,6"/>
              </svg>
              <a :href="'mailto:' + contactEmail">{{ contactEmail }}</a>
            </li>
            <li v-for="(city, index) in contactCities" :key="index">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="14" height="14">
                <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/>
                <circle cx="12" cy="10" r="3"/>
              </svg>
              <span>{{ city.label }}</span>
            </li>
          </ul>
        </div>
      </div>

      <!-- Bottom bar -->
      <div class="footer-bottom">
        <span class="footer-copyright">{{ copyright }}</span>
        <div class="footer-legal">
          <a href="#">隐私政策</a>
          <a href="#">用户协议</a>
          <a href="#">Cookie 政策</a>
        </div>
      </div>
    </div>
  </footer>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getCareersConfig } from '@/api/config'

interface LinkItem { label: string; href: string }
interface ContactItem { label: string; icon: string }

const brandDesc = ref('AI 驱动的智能招聘平台，致力于用技术让招聘更精准、更高效、更人性化。服务超过 500 万家企业客户，累计处理 50 亿+ 简历。')
const copyright = ref('© 2026 SmartRecruit. All rights reserved.')
const contactEmail = ref('hr@smartrecruit.com')
const contactCities = ref<ContactItem[]>([
  { label: '北京 · 海淀区', icon: 'location' },
  { label: '上海 · 浦东新区', icon: 'location' },
  { label: '深圳 · 南山区', icon: 'location' },
  { label: '杭州 · 余杭区', icon: 'location' },
])

const categoryTitle = ref('职位类别')
const categoryLinks = ref<LinkItem[]>([
  { label: '技术研发', href: '/jobs?cat=tech' },
  { label: '产品与设计', href: '/jobs?cat=product' },
  { label: '数据与 AI', href: '/jobs?cat=data' },
  { label: '市场与销售', href: '/jobs?cat=market' },
  { label: '运营与职能', href: '/jobs?cat=ops' },
])

const aboutTitle = ref('关于我们')
const aboutLinks = ref<LinkItem[]>([
  { label: '企业文化', href: '#' },
  { label: '薪酬福利', href: '#' },
  { label: '工作生活', href: '#' },
  { label: '常见问题', href: '#' },
  { label: '联系我们', href: '#' },
])

onMounted(async () => {
  try {
    const cfg = await getCareersConfig()
    if (cfg.careers_footer_desc) brandDesc.value = cfg.careers_footer_desc
    if (cfg.careers_footer_copyright) copyright.value = cfg.careers_footer_copyright
    if (cfg.careers_footer_contact_email) contactEmail.value = cfg.careers_footer_contact_email
    if (cfg.careers_footer_contact_cities) {
      try { const p = JSON.parse(cfg.careers_footer_contact_cities); if (p.length > 0) contactCities.value = p } catch { }
    }
    if (cfg.careers_footer_category_title) categoryTitle.value = cfg.careers_footer_category_title
    if (cfg.careers_footer_category_links) {
      try { const p = JSON.parse(cfg.careers_footer_category_links); if (p.length > 0) categoryLinks.value = p } catch { }
    }
    if (cfg.careers_footer_about_title) aboutTitle.value = cfg.careers_footer_about_title
    if (cfg.careers_footer_about_links) {
      try { const p = JSON.parse(cfg.careers_footer_about_links); if (p.length > 0) aboutLinks.value = p } catch { }
    }
  } catch { /* keep defaults */ }
})
</script>

<style scoped>
.footer {
  background: #0f172a;
  padding: 60px 0 0;
  color: #fff;
}

.footer-grid {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr 1.2fr;
  gap: 40px;
  padding-bottom: 40px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.footer-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}

.logo-text {
  font-size: 20px;
  font-weight: 700;
  color: #fff;
}

.footer-desc {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.45);
  line-height: 1.7;
  max-width: 340px;
}

.footer-col-title {
  font-size: 14px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 16px;
}

.footer-links {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.footer-links li,
.footer-links a {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.5);
  transition: color var(--transition);
}

.footer-links a:hover {
  color: var(--color-primary-light);
}

.footer-contact li {
  display: flex;
  align-items: center;
  gap: 8px;
}

.footer-contact svg {
  flex-shrink: 0;
  color: rgba(255, 255, 255, 0.3);
}

.footer-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24px 0;
}

.footer-copyright {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.35);
}

.footer-legal {
  display: flex;
  gap: 20px;
}

.footer-legal a {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.35);
  transition: color var(--transition);
}

.footer-legal a:hover {
  color: var(--color-primary-light);
}

@media (max-width: 1024px) {
  .footer-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .footer-grid {
    grid-template-columns: 1fr;
    gap: 32px;
  }

  .footer-bottom {
    flex-direction: column;
    gap: 12px;
  }
}
</style>

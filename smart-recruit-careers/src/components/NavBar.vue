<template>
  <header class="navbar" :class="{ 'navbar--scrolled': scrolled }">
    <div class="navbar__inner">
      <!-- Logo -->
      <router-link to="/" class="navbar__logo" aria-label="SmartRecruit 首页">
        <span class="navbar__logo-icon">
          <svg viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg">
            <rect width="40" height="40" rx="10" fill="#1677ff"/>
            <path d="M12 28V12l8 12L28 12v16" stroke="#fff" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" fill="none"/>
          </svg>
        </span>
        <span class="navbar__logo-text">{{ logoText }}</span>
      </router-link>

      <!-- Desktop Nav Links -->
      <nav class="navbar__links" aria-label="主导航">
        <template v-for="link in navLinks" :key="link.key">
          <router-link
            v-if="link.type === 'route'"
            :to="link.route!"
            class="navbar__link"
            :class="{ 'navbar__link--active': activeSection === link.key }"
            @click="closeMobile"
          >
            {{ link.label }}
          </router-link>
          <a
            v-else
            :href="isHomePage ? `#${link.hash}` : `/#${link.hash}`"
            class="navbar__link"
            :class="{ 'navbar__link--active': activeSection === link.key }"
            @click.prevent="handleNavClick(link.hash!)"
          >
            {{ link.label }}
          </a>
        </template>
      </nav>

      <!-- Desktop Actions -->
      <div class="navbar__actions">
        <template v-if="userStore.isLoggedIn">
          <router-link to="/jobs" class="navbar__cta-primary" @click="closeMobile">
            {{ navCtaLabel }}
          </router-link>
          <el-dropdown v-if="displayName" trigger="click" popper-class="navbar__dropdown-popper" @command="handleDropdownCommand">
            <span class="navbar__user-phone">
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none" class="navbar__user-phone-icon">
                <rect x="2" y="1" width="12" height="14" rx="2" stroke="currentColor" stroke-width="1.3"/>
                <circle cx="8" cy="10.5" r="1.5" fill="currentColor"/>
              </svg>
              {{ displayName }}
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="applications">我的投递</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <router-link to="/jobs" class="navbar__cta" @click="closeMobile">
            {{ navCtaLabel }}
          </router-link>
        </template>
      </div>

      <!-- Mobile Hamburger -->
      <button
        class="navbar__hamburger"
        :class="{ 'navbar__hamburger--open': mobileOpen }"
        @click="toggleMobile"
        aria-label="切换菜单"
        :aria-expanded="mobileOpen"
      >
        <span class="navbar__hamburger-line" />
        <span class="navbar__hamburger-line" />
        <span class="navbar__hamburger-line" />
      </button>
    </div>

    <!-- Mobile Menu -->
    <transition name="mobile-slide">
      <div v-if="mobileOpen" class="navbar__mobile" role="menu">
        <nav class="navbar__mobile-links">
          <template v-for="link in navLinks" :key="link.key">
            <router-link
              v-if="link.type === 'route'"
              :to="link.route!"
              class="navbar__mobile-link"
              :class="{ 'navbar__mobile-link--active': activeSection === link.key }"
              @click="closeMobile"
              role="menuitem"
            >
              {{ link.label }}
            </router-link>
            <a
              v-else
              :href="isHomePage ? `#${link.hash}` : `/#${link.hash}`"
              class="navbar__mobile-link"
              :class="{ 'navbar__mobile-link--active': activeSection === link.key }"
              @click.prevent="handleNavClick(link.hash!)"
              role="menuitem"
            >
              {{ link.label }}
            </a>
          </template>
          <router-link
            to="/jobs"
            class="navbar__mobile-link navbar__mobile-link--cta"
            @click="closeMobile"
            role="menuitem"
          >
            查看全部职位
          </router-link>
          <template v-if="userStore.isLoggedIn">
            <div class="navbar__mobile-divider" />
            <div class="navbar__mobile-user">
              <span class="navbar__mobile-user-avatar">
                <svg viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <circle cx="16" cy="16" r="16" fill="#e8edf2"/>
                  <circle cx="16" cy="11" r="5.5" fill="#94a3b8"/>
                  <path d="M5 28c0-5.523 4.925-10 11-10s11 4.477 11 10" fill="#94a3b8"/>
                </svg>
              </span>
              <span class="navbar__mobile-user-name">{{ displayName }}</span>
            </div>
            <router-link
              to="/my-applications"
              class="navbar__mobile-link"
              @click="closeMobile"
              role="menuitem"
            >
              我的投递
            </router-link>
            <button
              class="navbar__mobile-link navbar__mobile-link--logout"
              @click="handleLogout"
              role="menuitem"
            >
              退出登录
            </button>
          </template>
        </nav>
      </div>
    </transition>

    <!-- Mobile overlay backdrop -->
    <transition name="fade">
      <div
        v-if="mobileOpen"
        class="navbar__overlay"
        @click="closeMobile"
        aria-hidden="true"
      />
    </transition>
  </header>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getCareersConfig } from '@/api/config'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// -- scroll state --
const scrolled = ref(false)
function onScroll() {
  scrolled.value = window.scrollY > 20

  if (route.name === 'Home') {
    const hashLinks = navLinks.value.filter(l => l.type === 'hash')
    for (let i = hashLinks.length - 1; i >= 0; i--) {
      const el = document.getElementById(hashLinks[i].hash!)
      if (el) {
        const rect = el.getBoundingClientRect()
        if (rect.top <= 160) {
          activeSection.value = hashLinks[i].key
          break
        }
      }
    }
  }
}

onMounted(() => window.addEventListener('scroll', onScroll, { passive: true }))
onUnmounted(() => window.removeEventListener('scroll', onScroll))

// -- config-driven reactive refs --
const logoText = ref('SmartRecruit')
const navCtaLabel = ref('投递简历')

// -- nav links --
interface NavLink {
  type: 'hash' | 'route'
  key: string
  label: string
  hash?: string
  route?: string
}

const DEFAULT_NAV_LINKS: NavLink[] = [
  { type: 'hash', key: 'culture', label: '企业文化', hash: 'culture' },
  { type: 'route', key: 'social', label: '社会招聘', route: '/social-recruitment' },
  { type: 'route', key: 'campus', label: '校园招聘', route: '/campus-recruitment' },
  { type: 'hash', key: 'benefits', label: '薪酬福利', hash: 'benefits' },
  { type: 'hash', key: 'life', label: '工作生活', hash: 'life' },
  { type: 'hash', key: 'faq', label: '常见问题', hash: 'faq' },
]

const navLinks = ref<NavLink[]>([...DEFAULT_NAV_LINKS])

const isHomePage = computed(() => route.name === 'Home')
const activeSection = ref('')

const routeKeyMap: Record<string, string> = {
  SocialRecruitment: 'social',
  CampusRecruitment: 'campus',
}

watch(
  () => route.name,
  (name) => {
    const key = typeof name === 'string' ? routeKeyMap[name] || '' : ''
    if (key) {
      activeSection.value = key
    } else if (name !== 'Home') {
      activeSection.value = ''
    }
  },
  { immediate: true }
)

onMounted(async () => {
  try {
    const cfg = await getCareersConfig()
    if (cfg.careers_nav_logo_text) logoText.value = cfg.careers_nav_logo_text
    if (cfg.careers_nav_cta_label) navCtaLabel.value = cfg.careers_nav_cta_label
    if (cfg.careers_nav_links) {
      try { const p = JSON.parse(cfg.careers_nav_links) as NavLink[]; if (p.length > 0) navLinks.value = p } catch { }
    }
  } catch { /* keep defaults */ }
})

function scrollToHash(hash: string, retries = 0) {
  const el = document.getElementById(hash)
  if (el) {
    const offset = 80
    const top = el.getBoundingClientRect().top + window.scrollY - offset
    window.scrollTo({ top, behavior: 'smooth' })
  } else if (retries < 20) {
    requestAnimationFrame(() => scrollToHash(hash, retries + 1))
  }
}

function handleNavClick(hash: string) {
  closeMobile()
  activeSection.value = hash

  if (isHomePage.value) {
    scrollToHash(hash)
  } else {
    router.push({ path: '/', hash: `#${hash}` }).then(() => {
      nextTick(() => scrollToHash(hash))
    })
  }
}

// -- mobile menu --
const mobileOpen = ref(false)
function toggleMobile() {
  mobileOpen.value = !mobileOpen.value
}
function closeMobile() {
  mobileOpen.value = false
}

// -- user display --
const displayName = computed(() => {
  const u = userStore.userInfo
  if (!u) return ''
  const phone = u.mobile || u.username || ''
  if (phone.length >= 11) return phone.slice(0, 3) + '****' + phone.slice(7)
  return phone
})

async function doLogout() {
  userStore.logout()
  ElMessage.success('已退出登录')
  router.push('/')
}

function handleDropdownCommand(command: string) {
  if (command === 'applications') {
    router.push('/my-applications')
  } else if (command === 'logout') {
    ElMessageBox.confirm(
      '确定要退出登录吗？',
      '退出登录',
      {
        confirmButtonText: '确定退出',
        cancelButtonText: '取消',
        type: 'warning',
      }
    ).then(() => {
      doLogout()
    }).catch(() => {})
  }
}

function handleLogout() {
  closeMobile()
  ElMessageBox.confirm(
    '确定要退出登录吗？',
    '退出登录',
    {
      confirmButtonText: '确定退出',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(() => {
    doLogout()
  }).catch(() => {})
}
</script>

<style scoped>
/* ================================================================
   NavBar — fixed top 64px with glass morphism
   ================================================================ */
.navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  height: 64px;
  display: flex;
  align-items: center;
  background: #ffffff;
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-bottom: 1px solid transparent;
  transition:
    background var(--transition-slow, 0.35s ease),
    box-shadow var(--transition-slow, 0.35s ease),
    border-color var(--transition-slow, 0.35s ease);
}

.navbar--scrolled {
  background: rgba(255, 255, 255, 0.98);
  border-bottom-color: var(--color-border-light, #f3f4f6);
  box-shadow: var(--shadow-sm, 0 1px 2px rgba(0,0,0,0.05));
}

.navbar__inner {
  display: flex;
  align-items: center;
  max-width: var(--max-width, 1280px);
  width: 100%;
  margin: 0 auto;
  padding: 0 32px;
  height: 100%;
}

/* -- Logo -- */
.navbar__logo {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  flex-shrink: 0;
  margin-right: 48px;
}

.navbar__logo-icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.navbar__logo-icon svg {
  display: block;
}

.navbar__logo-text {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text, #1a1a2e);
  letter-spacing: -0.3px;
  white-space: nowrap;
}

.navbar--scrolled .navbar__logo-text {
  color: var(--color-text, #1a1a2e);
}

/* -- Nav Links -- */
.navbar__links {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-right: auto;
}

.navbar__link {
  display: inline-flex;
  align-items: center;
  padding: 6px 16px;
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-secondary, #6b7280);
  text-decoration: none;
  border-radius: var(--radius-sm, 8px);
  transition:
    color var(--transition, 0.2s ease),
    background var(--transition, 0.2s ease);
  white-space: nowrap;
}

.navbar__link:hover {
  color: var(--color-text, #1a1a2e);
  background: rgba(0, 0, 0, 0.03);
}

.navbar__link--active {
  color: #1677ff !important;
  background: rgba(22, 119, 255, 0.08) !important;
}

/* -- Actions container -- */
.navbar__actions {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-left: auto;
}

/* -- CTA Button -- */
.navbar__cta {
  font-size: 13px;
  font-weight: 500;
  color: #1677ff;
  text-decoration: none;
  padding: 5px 14px;
  border-radius: 6px;
  border: 1px solid #1677ff;
  transition: all 0.15s;
  white-space: nowrap;
  flex-shrink: 0;
}

.navbar__cta:hover {
  background: #1677ff;
  color: #fff;
}

/* -- Primary CTA (gradient button) -- */
.navbar__cta-primary {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 32px;
  padding: 0 18px;
  font-size: 13px;
  font-weight: 600;
  color: #ffffff;
  text-decoration: none;
  background: linear-gradient(135deg, #1677ff 0%, #4f46e5 100%);
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.3);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  white-space: nowrap;
  flex-shrink: 0;
}

.navbar__cta-primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(99, 102, 241, 0.4);
}

/* -- User phone -- */
.navbar__user-phone {
  font-size: 13px;
  font-weight: 500;
  color: #4a4f5c;
  display: flex;
  align-items: center;
  gap: 6px;
  white-space: nowrap;
  cursor: pointer;
}

.navbar__user-phone:hover {
  color: #1677ff;
}

.navbar__user-phone-icon {
  color: #1677ff;
  flex-shrink: 0;
}

/* -- Logout button -- */
.navbar__logout-btn {
  font-size: 13px;
  font-weight: 500;
  color: #4a4f5c;
  background: none;
  border: 1px solid #eaecf2;
  border-radius: 6px;
  padding: 5px 14px;
  cursor: pointer;
  transition: all 0.15s;
  font-family: inherit;
  white-space: nowrap;
  flex-shrink: 0;
}

.navbar__logout-btn:hover {
  color: #f56c6c;
  border-color: #f56c6c;
}

/* -- Hamburger button -- */
.navbar__hamburger {
  display: none;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 5px;
  width: 40px;
  height: 40px;
  padding: 0;
  border: none;
  background: transparent;
  cursor: pointer;
  z-index: 1001;
  margin-left: 8px;
}

.navbar__hamburger-line {
  display: block;
  width: 22px;
  height: 2px;
  border-radius: 2px;
  background: var(--color-text, #1a1a2e);
  transition:
    transform var(--transition, 0.2s ease),
    opacity var(--transition, 0.2s ease);
  transform-origin: center;
}

.navbar__hamburger--open .navbar__hamburger-line:nth-child(1) {
  transform: translateY(7px) rotate(45deg);
}

.navbar__hamburger--open .navbar__hamburger-line:nth-child(2) {
  opacity: 0;
}

.navbar__hamburger--open .navbar__hamburger-line:nth-child(3) {
  transform: translateY(-7px) rotate(-45deg);
}

/* -- Mobile Overlay -- */
.navbar__overlay {
  position: fixed;
  inset: 0;
  top: 64px;
  background: rgba(0, 0, 0, 0.35);
  z-index: 998;
}

/* -- Mobile Menu -- */
.navbar__mobile {
  position: fixed;
  top: 64px;
  left: 0;
  right: 0;
  background: #ffffff;
  box-shadow: var(--shadow-lg, 0 10px 25px -3px rgba(0, 0, 0, 0.08));
  z-index: 999;
  padding: 24px;
  max-height: calc(100vh - 64px);
  overflow-y: auto;
}

.navbar__mobile-links {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.navbar__mobile-link {
  display: flex;
  align-items: center;
  padding: 14px 16px;
  font-size: 15px;
  font-weight: 500;
  color: var(--color-text, #1a1a2e);
  text-decoration: none;
  border-radius: var(--radius-sm, 8px);
  transition: background var(--transition, 0.2s ease);
}

.navbar__mobile-link:hover {
  background: var(--color-primary-bg, rgba(99, 102, 241, 0.06));
}

.navbar__mobile-link--active {
  color: #1677ff;
  background: rgba(22, 119, 255, 0.08);
}

.navbar__mobile-link--cta {
  background: linear-gradient(135deg, #1677ff 0%, #4f46e5 100%);
  color: #ffffff;
  justify-content: center;
  margin-top: 8px;
}

.navbar__mobile-link--cta:hover {
  background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%);
}

.navbar__mobile-link--logout {
  background: transparent;
  width: 100%;
  text-align: left;
  font-family: inherit;
  color: var(--color-accent, #f43f5e);
}

.navbar__mobile-link--logout:hover {
  background: rgba(244, 63, 94, 0.06);
}

.navbar__mobile-divider {
  height: 1px;
  background: var(--color-border-light, #f3f4f6);
  margin: 8px 0;
}

.navbar__mobile-user {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
}

.navbar__mobile-user-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
}

.navbar__mobile-user-avatar svg {
  width: 100%;
  height: 100%;
  display: block;
}

.navbar__mobile-user-name {
  font-size: 15px;
  font-weight: 500;
  color: var(--color-text, #1a1a2e);
}

/* -- Transitions -- */
.mobile-slide-enter-active {
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}
.mobile-slide-leave-active {
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}
.mobile-slide-enter-from {
  opacity: 0;
  transform: translateY(-8px);
}
.mobile-slide-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* -- Dropdown popper -- */
</style>

<style>
.navbar__dropdown-popper {
  margin-top: 6px !important;
  border-radius: 10px !important;
  border: 1px solid rgba(0, 0, 0, 0.06) !important;
  box-shadow:
    0 4px 6px -1px rgba(0, 0, 0, 0.05),
    0 10px 30px -8px rgba(0, 0, 0, 0.12) !important;
  padding: 4px !important;
  min-width: 120px !important;
}

.navbar__dropdown-popper .el-dropdown-menu {
  border: none !important;
  box-shadow: none !important;
}

.navbar__dropdown-popper .el-dropdown-menu__item {
  border-radius: 8px !important;
  margin: 1px 2px !important;
  padding: 10px 14px !important;
  font-size: 14px !important;
  color: #334155 !important;
}

.navbar__dropdown-popper .el-dropdown-menu__item:hover {
  background: rgba(22, 119, 255, 0.06) !important;
  color: #1677ff !important;
}
</style>

<style scoped>
/* ================================================================
   Responsive
   ================================================================ */
@media (max-width: 1024px) {
  .navbar__links {
    gap: 2px;
  }

  .navbar__link {
    padding: 6px 12px;
    font-size: 13px;
  }
}

@media (max-width: 860px) {
  .navbar__links {
    display: none;
  }

  .navbar__actions {
    margin-left: auto;
  }

  .navbar__cta {
    padding: 4px 12px;
    font-size: 13px;
  }

  .navbar__user-phone {
    font-size: 12px;
  }

  .navbar__logout-btn {
    padding: 4px 10px;
    font-size: 12px;
  }

  .navbar__hamburger {
    display: flex;
  }
}

@media (max-width: 480px) {
  .navbar__inner {
    padding: 0 16px;
  }

  .navbar__logo {
    margin-right: auto;
  }

  .navbar__logo-text {
    font-size: 16px;
  }

  .navbar__cta,
  .navbar__cta-primary {
    display: none;
  }

  .navbar__user-phone {
    font-size: 12px;
  }

  .navbar__logout-btn {
    padding: 4px 10px;
    font-size: 12px;
  }
}
</style>

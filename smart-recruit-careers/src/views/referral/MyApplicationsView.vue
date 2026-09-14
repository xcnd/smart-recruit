<template>
  <div class="ma-page" v-loading="loading">
    <NavBar />

    <!-- HERO -->
    <section class="ma-hero">
      <div class="ma-hero__orb ma-hero__orb--1" aria-hidden="true" />
      <div class="ma-hero__orb ma-hero__orb--2" aria-hidden="true" />
      <div class="ma-hero__orb ma-hero__orb--3" aria-hidden="true" />
      <div class="ma-hero__grid" aria-hidden="true" />

      <div class="container ma-hero__content">
        <router-link to="/" class="ma-back-link">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <line x1="19" y1="12" x2="5" y2="12" />
            <polyline points="12 19 5 12 12 5" />
          </svg>
          返回首页
        </router-link>
        <h1 class="ma-hero-title">我的投递</h1>
        <p class="ma-hero-sub">跟踪您投递的简历状态与进度</p>
      </div>

    </section>

    <!-- CONTENT -->
    <section class="ma-content">
      <div class="container">
        <!-- Login required -->
        <div v-if="!userStore.isLoggedIn && !loading" class="ma-empty-state">
          <div class="ma-empty-card">
            <div class="ma-empty-icon">
              <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                <path d="M7 11V7a5 5 0 0 1 10 0v4" />
              </svg>
            </div>
            <h2 class="ma-empty-title">请先登录</h2>
            <p class="ma-empty-desc">登录后可查看您的投递记录</p>
            <button class="ma-btn-primary" @click="goToLogin">去登录</button>
          </div>
        </div>

        <!-- Empty -->
        <div v-else-if="!loading && applications.length === 0" class="ma-empty-state">
          <div class="ma-empty-card">
            <div class="ma-empty-icon">
              <svg width="64" height="64" viewBox="0 0 64 64" fill="none">
                <rect x="10" y="8" width="44" height="50" rx="4" stroke="currentColor" stroke-width="2" />
                <path d="M22 24h20M22 32h16M22 40h12" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
              </svg>
            </div>
            <h2 class="ma-empty-title">暂无投递记录</h2>
            <p class="ma-empty-desc">去看看有哪些热门职位正在招聘</p>
            <button class="ma-btn-primary" @click="goToJobs">浏览职位</button>
          </div>
        </div>

        <!-- Application List -->
        <div v-else class="ma-list">
          <article
            v-for="app in applications"
            :key="app.id"
            class="ma-card"
          >
            <div class="ma-card-header">
              <div class="ma-card-avatar" :class="avatarClass(app.status)">
                <span class="ma-card-avatar-letter">{{ avatarLetter(app) }}</span>
              </div>
              <div class="ma-card-info">
                <h3 class="ma-card-title">
                  {{ app.jobTitle || app.programJobTitle || '职位名称' }}
                  <span v-if="app.departmentName" class="ma-card-dept">{{ app.departmentName }}</span>
                </h3>
                <div class="ma-card-meta">
                  <span class="ma-tag" :class="statusClass(app.status)">{{ statusLabel(app.status) }}</span>
                  <span class="ma-card-date">{{ formatDate(app.createTime) }}</span>
                </div>
              </div>
            </div>

            <div class="ma-card-progress">
              <div class="ma-timeline">
                <div class="ma-timeline-bg"></div>
                <div class="ma-timeline-fill" :style="{ width: progressPercent(app.status) + '%' }" :class="progressClass(app.status)"></div>
                <div class="ma-timeline-steps">
                  <div
                    v-for="(step, idx) in steps"
                    :key="idx"
                    class="ma-tl-step"
                    :class="{
                      'is-done': isStepDone(app.status, idx),
                      'is-active': isStepActive(app.status, idx),
                      'is-rejected': app.status === 5 && idx > 0
                    }"
                  >
                    <div class="ma-tl-circle">
                      <span class="ma-tl-icon-inner" v-html="stepIconHTML(idx, isStepDone(app.status, idx), app.status === 5)"></span>
                    </div>
                    <span class="ma-tl-label">{{ step }}</span>
                  </div>
                </div>
              </div>
            </div>
          </article>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/api/request'
import { getCareersMyApplications } from '@/api/careers'
import { useUserStore } from '@/stores/user'
import NavBar from '@/components/NavBar.vue'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(true)
const applications = ref<any[]>([])

const steps = ['投递简历', '简历筛选', '面试评估', '录用通知', '正式入职']

const stepIcons: Record<number, string> = {
  0: `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>`,
  1: `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"/></svg>`,
  2: `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>`,
  3: `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>`,
  4: `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="7" width="20" height="14" rx="2" ry="2"/><path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"/></svg>`,
}

const checkIcon = `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>`

const rejectIcon = `<svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>`

function stepIconHTML(idx: number, done: boolean, isRejected: boolean): string {
  if (isRejected && idx > 0) return rejectIcon
  if (done) return checkIcon
  return stepIcons[idx] || ''
}

const statusMap: Record<number, string> = {
  0: '待处理',
  1: '筛选中',
  2: '面试中',
  3: '已录用',
  4: '已入职',
  5: '已淘汰',
}

function statusLabel(status: number): string {
  return statusMap[status] || '未知'
}

function statusClass(status: number): string {
  if (status === 4) return 'ma-tag--success'
  if (status === 5) return 'ma-tag--danger'
  if (status >= 2) return 'ma-tag--primary'
  if (status === 1) return 'ma-tag--warn'
  return 'ma-tag--default'
}

function avatarClass(status: number): string {
  if (status === 4) return 'ma-avatar--success'
  if (status === 5) return 'ma-avatar--danger'
  if (status >= 2) return 'ma-avatar--primary'
  if (status === 1) return 'ma-avatar--warn'
  return 'ma-avatar--default'
}

function avatarLetter(app: any): string {
  const title = app.jobTitle || app.programJobTitle || '职位'
  return title.charAt(0)
}

function progressPercent(status: number): number {
  if (status === 5) return 100
  if (status >= 4) return 100
  if (status >= 3) return 75
  if (status >= 2) return 50
  if (status >= 1) return 25
  return 10
}

function progressClass(status: number): string {
  if (status === 5) return 'ma-progress-fill--danger'
  if (status >= 4) return 'ma-progress-fill--success'
  return ''
}

function isStepActive(status: number, idx: number): boolean {
  const mapping: Record<number, number> = { 0: 0, 1: 1, 2: 2, 3: 3, 4: 4, 5: -1 }
  return mapping[status] === idx
}

function isStepDone(status: number, idx: number): boolean {
  const mapping: Record<number, number> = { 0: -1, 1: 0, 2: 1, 3: 2, 4: 3, 5: -1 }
  return (mapping[status] ?? -1) >= idx
}

function formatDate(dateStr: string): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  if (isNaN(d.getTime())) return dateStr
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const h = String(d.getHours()).padStart(2, '0')
  const min = String(d.getMinutes()).padStart(2, '0')
  const s = String(d.getSeconds()).padStart(2, '0')
  return `${y}-${m}-${day} ${h}:${min}:${s}`
}

function goToLogin() {
  router.push({ name: 'PhoneLogin', query: { redirect: router.currentRoute.value.fullPath } })
}

function goToJobs() {
  router.push('/jobs')
}

async function loadApplications() {
  loading.value = true
  try {
    if (!userStore.isLoggedIn) {
      loading.value = false
      return
    }
    const [referralData, careersData] = await Promise.allSettled([
      request.get('/referrals/my-applications') as Promise<any[]>,
      loadCareersApplications(),
    ])

    const merged: any[] = []
    if (referralData.status === 'fulfilled' && referralData.value) {
      merged.push(...(referralData.value as any[]))
    }
    if (careersData.status === 'fulfilled' && careersData.value) {
      merged.push(...careersData.value)
    }

    merged.sort((a, b) => {
      const ta = a.createTime ? new Date(a.createTime).getTime() : 0
      const tb = b.createTime ? new Date(b.createTime).getTime() : 0
      return tb - ta
    })

    applications.value = merged
  } catch {
    // silently fail
  } finally {
    loading.value = false
  }
}

async function loadCareersApplications(): Promise<any[]> {
  const info = userStore.userInfo as Record<string, unknown> | null
  const uid = (info?.id as number) || 0
  const email = (info?.email as string) || ''
  if (!uid && !email) return []
  try {
    return (await getCareersMyApplications(uid, email)) as any[]
  } catch {
    return []
  }
}

onMounted(() => {
  loadApplications()
})
</script>

<style scoped>
.ma-page {
  min-height: 100vh;
  background: linear-gradient(160deg, #0f172a 0%, #1e293b 30%, #1e3a5f 60%, #1b1b3a 100%);
}

/* ================================================================
   Hero Section
   ================================================================ */
.ma-hero {
  position: relative;
  width: 100%;
  padding: 120px 0 56px;
  overflow: hidden;
  isolation: isolate;
}

/* -- Glow orbs -- */
.ma-hero__orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.35;
  pointer-events: none;
  will-change: transform;
}

.ma-hero__orb--1 {
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(99, 102, 241, 0.6) 0%, transparent 70%);
  top: -15%;
  right: -5%;
  animation: ma-orb-float-1 12s ease-in-out infinite alternate;
}

.ma-hero__orb--2 {
  width: 340px;
  height: 340px;
  background: radial-gradient(circle, rgba(244, 63, 94, 0.45) 0%, transparent 70%);
  bottom: -10%;
  left: -8%;
  animation: ma-orb-float-2 10s ease-in-out infinite alternate;
}

.ma-hero__orb--3 {
  width: 280px;
  height: 280px;
  background: radial-gradient(circle, rgba(139, 92, 246, 0.5) 0%, transparent 70%);
  top: 30%;
  left: 50%;
  animation: ma-orb-float-3 8s ease-in-out infinite alternate;
}

@keyframes ma-orb-float-1 {
  0% { transform: translate(0, 0) scale(1); }
  100% { transform: translate(-40px, 30px) scale(1.08); }
}

@keyframes ma-orb-float-2 {
  0% { transform: translate(0, 0) scale(1); }
  100% { transform: translate(50px, -20px) scale(1.1); }
}

@keyframes ma-orb-float-3 {
  0% { transform: translate(0, 0) scale(1); }
  100% { transform: translate(-30px, -40px) scale(1.06); }
}

/* -- Grid pattern overlay -- */
.ma-hero__grid {
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

/* -- Hero content -- */
.ma-hero__content {
  position: relative;
  z-index: 2;
}

.ma-back-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.5);
  margin-bottom: 24px;
  transition: color var(--transition);
}

.ma-back-link:hover {
  color: rgba(255, 255, 255, 0.8);
}

.ma-hero-title {
  font-size: clamp(24px, 5vw, 36px);
  font-weight: 800;
  letter-spacing: -0.5px;
  line-height: 1.25;
  color: #ffffff;
  margin: 0 0 8px;
}

.ma-hero-sub {
  font-size: 15px;
  color: rgba(255, 255, 255, 0.6);
  margin: 0;
}

/* ================================================================
   Content
   ================================================================ */
.ma-content {
  position: relative;
  z-index: 0;
  padding: 48px 0 96px;
}

/* ================================================================
   Empty States
   ================================================================ */
.ma-empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 64px 0;
}

.ma-empty-card {
  text-align: center;
  padding: 64px 48px;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-xl);
  max-width: 480px;
  width: 100%;
}

.ma-empty-icon {
  color: var(--color-text-muted);
  margin-bottom: 24px;
  display: flex;
  justify-content: center;
  opacity: 0.5;
}

.ma-empty-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-text);
  margin: 0 0 8px;
}

.ma-empty-desc {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin: 0 0 28px;
}

.ma-btn-primary {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 12px 28px;
  font-size: 15px;
  font-weight: 600;
  color: #fff;
  background: #1677ff;
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition);
  font-family: inherit;
  box-shadow: 0 2px 8px rgba(22, 119, 255, 0.3);
}

.ma-btn-primary:hover {
  opacity: 0.92;
  box-shadow: 0 4px 14px rgba(22, 119, 255, 0.4);
  transform: translateY(-1px);
}

/* ================================================================
   Application List
   ================================================================ */
.ma-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ================================================================
   Card
   ================================================================ */
.ma-card {
  background: var(--color-bg-card);
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border-light);
  box-shadow: var(--shadow-sm);
  padding: 24px;
  transition: transform var(--transition-slow), box-shadow var(--transition-slow), border-color var(--transition-slow);
}

.ma-card:hover {
  transform: translateX(4px);
  box-shadow: var(--shadow-lg);
  border-color: rgba(22, 119, 255, 0.2);
}

.ma-card-header {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 20px;
}

/* -- Avatar -- */
.ma-card-avatar {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.ma-card-avatar-letter {
  font-size: 18px;
  font-weight: 700;
  color: #fff;
}

.ma-avatar--default {
  background: linear-gradient(135deg, #94a3b8, #64748b);
}

.ma-avatar--warn {
  background: linear-gradient(135deg, #f59e0b, #d97706);
}

.ma-avatar--primary {
  background: #1677ff;
}

.ma-avatar--success {
  background: linear-gradient(135deg, #10b981, #059669);
}

.ma-avatar--danger {
  background: linear-gradient(135deg, #ef4444, #dc2626);
}

/* -- Card info -- */
.ma-card-info {
  flex: 1;
  min-width: 0;
}

.ma-card-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
  margin: 0 0 8px;
  line-height: 1.4;
}

.ma-card-dept {
  display: inline-block;
  font-size: 12px;
  font-weight: 500;
  color: #1677ff;
  background: rgba(22, 119, 255, 0.08);
  border-radius: 4px;
  padding: 2px 10px;
  margin-left: 8px;
  vertical-align: middle;
}

.ma-card-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.ma-card-date {
  font-size: 12px;
  color: var(--color-text-muted);
}

/* -- Tags -- */
.ma-tag {
  display: inline-flex;
  align-items: center;
  padding: 2px 10px;
  border-radius: var(--radius-full);
  font-size: 12px;
  font-weight: 500;
  line-height: 1.6;
}

.ma-tag--default {
  background: rgba(148, 163, 184, 0.15);
  color: #64748b;
}

.ma-tag--warn {
  background: rgba(245, 158, 11, 0.14);
  color: #d97706;
}

.ma-tag--primary {
  background: rgba(22, 119, 255, 0.12);
  color: #1677ff;
}

.ma-tag--success {
  background: rgba(5, 150, 105, 0.12);
  color: #059669;
}

.ma-tag--danger {
  background: rgba(244, 63, 94, 0.12);
  color: #f43f5e;
}

/* ================================================================
   Timeline Progress
   ================================================================ */
.ma-card-progress {
  padding-top: 6px;
}

.ma-timeline {
  position: relative;
  padding: 0 4px;
}

/* Connecting line (background) */
.ma-timeline-bg {
  position: absolute;
  top: 18px;
  left: 28px;
  right: 28px;
  height: 2px;
  background: var(--color-border);
  border-radius: 1px;
}

/* Connecting line (filled) */
.ma-timeline-fill {
  position: absolute;
  top: 18px;
  left: 28px;
  height: 2px;
  background: #1677ff;
  border-radius: 1px;
  transition: width 0.6s ease;
}

.ma-timeline-fill--success {
  background: linear-gradient(90deg, #1677ff, #10b981);
}

.ma-timeline-fill--danger {
  background: linear-gradient(90deg, #1677ff, #f87171);
}

/* Steps */
.ma-timeline-steps {
  display: flex;
  justify-content: space-between;
  position: relative;
}

.ma-tl-step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  flex: 1;
  position: relative;
}

/* Circle */
.ma-tl-circle {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg-card);
  border: 2px solid var(--color-border);
  transition: all 0.3s ease;
  z-index: 1;
  position: relative;
}

.ma-tl-icon-inner {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-muted);
  transition: color 0.3s ease;
}

.ma-tl-icon-inner :deep(svg) {
  display: block;
}

/* Done state */
.ma-tl-step.is-done .ma-tl-circle {
  background: #1677ff;
  border-color: #1677ff;
}

.ma-tl-step.is-done .ma-tl-icon-inner {
  color: #fff;
}

/* Active state */
.ma-tl-step.is-active .ma-tl-circle {
  background: #1677ff;
  border-color: #1677ff;
  animation: ma-pulse-ring 2s ease-in-out infinite;
}

.ma-tl-step.is-active .ma-tl-icon-inner {
  color: #fff;
  animation: ma-pulse-icon 2s ease-in-out infinite;
}

@keyframes ma-pulse-ring {
  0%, 100% { box-shadow: 0 0 0 4px rgba(22, 119, 255, 0.2); }
  50% { box-shadow: 0 0 0 10px rgba(22, 119, 255, 0.05); }
}

@keyframes ma-pulse-icon {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.12); }
}

/* Rejected state */
.ma-tl-step.is-rejected .ma-tl-circle {
  background: var(--color-bg-card);
  border-color: #f87171;
}

.ma-tl-step.is-rejected .ma-tl-icon-inner {
  color: #f87171;
}

/* Label */
.ma-tl-label {
  font-size: 12px;
  color: var(--color-text-muted);
  transition: color 0.3s;
  white-space: nowrap;
}

.ma-tl-step.is-done .ma-tl-label,
.ma-tl-step.is-active .ma-tl-label {
  color: #1677ff;
  font-weight: 600;
}

.ma-tl-step.is-rejected .ma-tl-label {
  color: var(--color-text-muted);
}

/* ================================================================
   Responsive
   ================================================================ */
@media (max-width: 768px) {
  .ma-hero {
    padding: 100px 0 48px;
  }

  .ma-hero__orb--1 {
    width: 220px;
    height: 220px;
  }

  .ma-hero__orb--2 {
    width: 180px;
    height: 180px;
  }

  .ma-hero__orb--3 {
    width: 160px;
    height: 160px;
  }

  .ma-content {
    padding: 32px 0 72px;
  }

  .ma-card {
    padding: 20px;
    border-radius: var(--radius-md);
  }

  .ma-card-header {
    flex-direction: column;
    gap: 12px;
  }

  .ma-card-avatar {
    width: 40px;
    height: 40px;
  }

  .ma-card-title {
    font-size: 15px;
  }

  .ma-timeline-steps {
    flex-wrap: wrap;
    gap: 8px;
  }

  .ma-tl-circle {
    width: 30px;
    height: 30px;
  }

  .ma-timeline-bg,
  .ma-timeline-fill {
    top: 15px;
    left: 21px;
    right: 21px;
  }

  .ma-empty-card {
    padding: 40px 24px;
  }
}

@media (max-width: 480px) {
  .ma-hero__content {
    padding: 0 20px;
  }
}
</style>

<template>
  <div class="ma-page" v-loading="loading">
    <!-- HEADER -->
    <header class="ma-header">
      <div class="ma-header-inner">
        <a href="/" class="ma-header-brand" title="SmartRecruit 智能招聘">
          <svg width="28" height="28" viewBox="0 0 28 28" fill="none">
            <rect width="28" height="28" rx="6" fill="#1677ff"/>
            <path d="M8 20V8l6 8.5L20 8v12" stroke="#fff" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" fill="none"/>
          </svg>
          <span class="ma-brand-text">SmartRecruit</span>
        </a>
        <div class="ma-header-right">
          <span v-if="userStore.isLoggedIn && maskedPhone" class="ma-user-phone">
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none" class="ma-user-icon">
              <rect x="2" y="1" width="12" height="14" rx="2" stroke="currentColor" stroke-width="1.3"/>
              <circle cx="8" cy="10.5" r="1.5" fill="currentColor"/>
            </svg>
            {{ maskedPhone }}
          </span>
          <a @click.prevent="goToJobList" class="ma-header-btn" style="cursor:pointer">投递简历</a>
          <button v-if="userStore.isLoggedIn" class="ma-logout-btn" @click="handleLogout">退出</button>
        </div>
      </div>
    </header>

    <!-- HERO -->
    <div class="ma-hero">
      <div class="ma-hero-inner">
        <h1 class="ma-hero-title">我的投递</h1>
        <p class="ma-hero-sub">跟踪您投递的简历状态与进度</p>
      </div>
    </div>

    <!-- CONTENT -->
    <div class="ma-content">
      <div class="ma-content-inner">
        <!-- Login required -->
        <div v-if="!userStore.isLoggedIn && !loading" class="ma-empty-state">
          <el-empty description="请先登录后查看投递记录" :image-size="80" />
          <el-button type="primary" size="large" round @click="goToLogin">去登录</el-button>
        </div>

        <!-- Empty -->
        <div v-else-if="!loading && applications.length === 0" class="ma-empty-state">
          <div class="ma-empty-icon">
            <svg width="64" height="64" viewBox="0 0 64 64" fill="none"><rect x="10" y="8" width="44" height="50" rx="4" stroke="#bfbfbf" stroke-width="2"/><path d="M22 24h20M22 32h16M22 40h12" stroke="#bfbfbf" stroke-width="2" stroke-linecap="round"/></svg>
          </div>
          <p class="ma-empty-title">暂无投递记录</p>
          <p class="ma-empty-desc">去看看有哪些热门职位正在招聘</p>
          <el-button type="primary" size="large" round @click="goToHome">浏览职位</el-button>
        </div>

        <!-- Application List -->
        <div v-else class="ma-list">
          <div v-for="app in applications" :key="app.id" class="ma-card">
            <div class="ma-card-top">
              <div class="ma-card-info">
                <h3 class="ma-card-job">
                  {{ app.jobTitle || app.programJobTitle || '职位名称' }}
                  <span v-if="app.departmentName" class="ma-job-dept">{{ app.departmentName }}</span>
                </h3>
                <div class="ma-card-meta">
                  <span class="ma-tag" :class="statusClass(app.status)">{{ statusLabel(app.status) }}</span>
                </div>
              </div>
              <div class="ma-card-date">{{ formatDate(app.createTime) }}</div>
            </div>
            <div class="ma-card-progress">
              <div class="ma-progress-bar">
                <div class="ma-progress-fill" :style="{ width: progressPercent(app.status) + '%' }"></div>
              </div>
              <div class="ma-progress-steps">
                <span v-for="(step, idx) in steps" :key="idx"
                  class="ma-step"
                  :class="{ 'is-active': isStepActive(app.status, idx), 'is-done': isStepDone(app.status, idx) }">
                  <span class="ma-step-dot"></span>
                  <span class="ma-step-label">{{ step }}</span>
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import request from '@/api/request'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loading = ref(true)
const applications = ref<any[]>([])

const backUrl = computed(() => {
  const ref = route.query.ref as string | undefined
  if (ref) {
    // If ref points to a job detail page, strip back to the program page
    return ref.replace(/\/job\/\d+$/, '')
  }
  return ''
})

async function goToJobList() {
  const url = backUrl.value
  if (url) {
    router.push(url)
    return
  }
  // No ref — fetch programs and redirect to the first one
  try {
    const res = await axios.get('/api/v1/referrals/public/programs')
    if (res.data?.code === 0 && res.data.data?.length) {
      const firstWithToken = res.data.data.find((p: any) => p.shareToken)
      if (firstWithToken) {
        router.push(`/p/${firstWithToken.shareToken}`)
        return
      }
    }
  } catch { /* ignore */ }
  ElMessage.warning('暂无可用职位')
}

const steps = ['投递', '筛选中', '面试', '录用', '入职']

const maskedPhone = computed(() => {
  const mobile = userStore.userInfo?.mobile
  if (!mobile || mobile.length !== 11) return null
  return mobile.slice(0, 3) + '****' + mobile.slice(7)
})

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

function progressPercent(status: number): number {
  if (status === 5) return 100 // rejected: show full bar but different color
  if (status >= 4) return 100
  if (status >= 3) return 75
  if (status >= 2) return 50
  if (status >= 1) return 25
  return 10
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

function goToHome() {
  router.push('/')
}

function handleLogout() {
  userStore.logout()
  ElMessage.success('已退出登录')
  router.push({ name: 'PhoneLogin' })
}

async function loadApplications() {
  loading.value = true
  try {
    if (!userStore.isLoggedIn) {
      loading.value = false
      return
    }
    const data = await request.get('/referrals/my-applications') as any[]
    applications.value = data || []
  } catch {
    // silently fail, handled by interceptor
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadApplications()
})
</script>

<style scoped>
/* ========== Page ========== */
.ma-page {
  min-height: 100vh;
  background: #f5f7fa;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

/* ========== Header ========== */
.ma-header {
  background: rgba(255,255,255,.88);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom: 1px solid #eaecf2;
  position: sticky;
  top: 0;
  z-index: 100;
}
.ma-header-inner {
  max-width: 960px;
  margin: 0 auto;
  padding: 0 24px;
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.ma-header-brand {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
}
.ma-brand-text {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a2e;
  letter-spacing: -0.2px;
}
.ma-header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}
.ma-user-phone {
  font-size: 13px;
  font-weight: 500;
  color: #4a4f5c;
  display: flex;
  align-items: center;
  gap: 6px;
}
.ma-user-icon {
  color: #1677ff;
  flex-shrink: 0;
}
.ma-header-btn {
  font-size: 13px;
  font-weight: 500;
  color: #1677ff;
  text-decoration: none;
  padding: 5px 14px;
  border-radius: 6px;
  border: 1px solid #1677ff;
  transition: all 0.15s;
}
.ma-header-btn:hover {
  background: #1677ff;
  color: #fff;
}
.ma-logout-btn {
  font-size: 13px;
  font-weight: 500;
  color: #4a4f5c;
  background: none;
  border: 1px solid #eaecf2;
  border-radius: 6px;
  padding: 5px 14px;
  cursor: pointer;
  transition: all 0.15s;
}
.ma-logout-btn:hover {
  color: #f56c6c;
  border-color: #f56c6c;
}

/* ========== Hero ========== */
.ma-hero {
  background: linear-gradient(135deg, #1677ff 0%, #0958d9 100%);
  padding: 40px 0;
}
.ma-hero-inner {
  max-width: 960px;
  margin: 0 auto;
  padding: 0 24px;
}
.ma-hero-title {
  font-size: 28px;
  font-weight: 700;
  color: #fff;
  margin: 0 0 6px;
}
.ma-hero-sub {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.75);
  margin: 0;
}

/* ========== Content ========== */
.ma-content {
  padding: 32px 0 64px;
}
.ma-content-inner {
  max-width: 960px;
  margin: 0 auto;
  padding: 0 24px;
}

/* ========== Empty ========== */
.ma-empty-state {
  text-align: center;
  padding: 80px 0;
}
.ma-empty-icon {
  margin-bottom: 16px;
  opacity: 0.5;
}
.ma-empty-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 0 0 8px;
}
.ma-empty-desc {
  font-size: 13px;
  color: #999;
  margin: 0 0 24px;
}

/* ========== List ========== */
.ma-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ========== Card ========== */
.ma-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  border: 1px solid #eef1f5;
  transition: box-shadow 0.2s;
}
.ma-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}
.ma-card-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}
.ma-card-info {
  flex: 1;
}
.ma-card-job {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0 0 8px;
}
.ma-job-dept {
  display: inline-block;
  font-size: 12px;
  font-weight: 400;
  color: #8c8c8c;
  background: #f5f5f5;
  border-radius: 4px;
  padding: 1px 8px;
  margin-left: 8px;
  vertical-align: middle;
}
.ma-card-meta {
  display: flex;
  gap: 8px;
  align-items: center;
}
.ma-card-date {
  font-size: 12px;
  color: #999;
  white-space: nowrap;
  margin-top: 2px;
}

/* ========== Tags ========== */
.ma-tag {
  display: inline-block;
  font-size: 12px;
  padding: 2px 10px;
  border-radius: 4px;
  line-height: 1.6;
}
.ma-tag--dept {
  background: #f0f5ff;
  color: #2f54eb;
}
.ma-tag--default {
  background: #f5f5f5;
  color: #999;
}
.ma-tag--warn {
  background: #fff7e6;
  color: #d48806;
}
.ma-tag--primary {
  background: #e6f4ff;
  color: #1677ff;
}
.ma-tag--success {
  background: #f6ffed;
  color: #389e0d;
}
.ma-tag--danger {
  background: #fff1f0;
  color: #cf1322;
}

/* ========== Progress ========== */
.ma-card-progress {
  margin-top: 4px;
}
.ma-progress-bar {
  height: 4px;
  background: #f0f0f0;
  border-radius: 2px;
  margin-bottom: 12px;
  overflow: hidden;
}
.ma-progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #1677ff, #52c41a);
  border-radius: 2px;
  transition: width 0.6s ease;
}
.ma-progress-steps {
  display: flex;
  justify-content: space-between;
}
.ma-step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  flex: 1;
}
.ma-step-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #e0e0e0;
  transition: all 0.3s;
}
.ma-step.is-done .ma-step-dot {
  background: #1677ff;
}
.ma-step.is-active .ma-step-dot {
  background: #1677ff;
  box-shadow: 0 0 0 4px rgba(22, 119, 255, 0.2);
}
.ma-step-label {
  font-size: 11px;
  color: #bbb;
  transition: color 0.3s;
}
.ma-step.is-done .ma-step-label,
.ma-step.is-active .ma-step-label {
  color: #1677ff;
  font-weight: 600;
}

/* ========== Responsive ========== */
@media (max-width: 640px) {
  .ma-hero-title {
    font-size: 22px;
  }
  .ma-card-top {
    flex-direction: column;
  }
  .ma-card-date {
    margin-top: 8px;
  }
  .ma-progress-steps {
    flex-wrap: wrap;
    gap: 8px;
  }
}
</style>

<template>
  <div class="sr-layout" :class="{ 'sidebar-collapsed': appStore.sidebarCollapsed }">
    <!-- 全局防截图水印（系统设置开关控制） -->
    <Watermark />
    <!-- Sidebar -->
    <aside class="sr-sidebar" :class="{ collapsed: appStore.sidebarCollapsed }">
      <el-tooltip
        :content="appStore.sidebarCollapsed ? '展开菜单' : '收起菜单'"
        placement="right"
        :disabled="!appStore.sidebarCollapsed"
      >
        <div class="sr-sidebar-logo" @click="appStore.toggleSidebar()">
          <template v-if="!appStore.sidebarCollapsed">
            <div class="sr-sidebar-logo-icon">
              <svg viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg">
                <rect width="40" height="40" rx="10" fill="#1677ff"/>
                <path d="M12 28V12l8 12L28 12v16" stroke="#fff" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" fill="none"/>
              </svg>
            </div>
            <div class="sr-sidebar-logo-text">
              <span class="sr-sidebar-logo-name">{{ settingsStore.systemName }}</span>
              <span class="sr-sidebar-logo-tagline">AI智能招聘平台</span>
            </div>
          </template>
          <div v-else class="sr-sidebar-logo-icon">
            <svg viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg">
              <rect width="40" height="40" rx="10" fill="#1677ff"/>
              <path d="M12 28V12l8 12L28 12v16" stroke="#fff" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" fill="none"/>
            </svg>
          </div>
        </div>
      </el-tooltip>

      <nav class="sr-sidebar-nav">
        <div
          v-for="group in visibleNavGroups"
          :key="group.key"
          class="sr-nav-group"
          :class="{ collapsed: !appStore.sidebarCollapsed && !isGroupExpanded(group.key) }"
        >
          <div
            class="sr-nav-group-title"
            :class="{ clickable: !appStore.sidebarCollapsed }"
            @click="toggleGroup(group.key)"
          >
            <template v-if="!appStore.sidebarCollapsed">
              <span>{{ group.title }}</span>
              <el-icon class="sr-group-arrow" :class="{ expanded: isGroupExpanded(group.key) }">
                <ArrowDown />
              </el-icon>
            </template>
          </div>
          <ul class="sr-nav-list" v-show="appStore.sidebarCollapsed || isGroupExpanded(group.key)">
            <li
              v-for="item in group.items"
              :key="item.route"
              class="sr-nav-item"
              :class="{ active: isActive(item.route) }"
            >
              <el-tooltip
                :content="item.label"
                placement="right"
                :disabled="!appStore.sidebarCollapsed"
              >
                <router-link :to="item.route" class="sr-nav-link">
                  <el-icon v-if="item.icon" class="sr-nav-icon">
                    <component :is="item.icon" />
                  </el-icon>
                  <span v-if="!appStore.sidebarCollapsed" class="sr-nav-label">{{ item.label }}</span>
                  <el-badge
                    v-if="item.badge && !appStore.sidebarCollapsed"
                    :value="item.badge"
                    class="sr-nav-badge"
                  />
                </router-link>
              </el-tooltip>
            </li>
          </ul>
        </div>
      </nav>
    </aside>

    <!-- Main Content -->
    <div class="sr-main-wrapper">
      <!-- Topbar -->
      <header class="sr-topbar">
        <div class="sr-topbar-left">
          <el-tooltip
            :content="appStore.sidebarCollapsed ? '展开菜单' : '收起菜单'"
            placement="bottom"
          >
            <el-button
              class="sr-toggle-btn"
              :icon="appStore.sidebarCollapsed ? Expand : Fold"
              text
              @click="appStore.toggleSidebar()"
            />
          </el-tooltip>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="route.meta.title">{{ route.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="sr-topbar-right">
          <!-- System Status Indicator -->
          <el-tooltip content="系统运行正常" placement="bottom">
            <span class="sr-status-indicator">
              <span class="sr-status-dot online"></span>
              系统正常
            </span>
          </el-tooltip>

          <!-- Notifications -->
          <el-popover
            placement="bottom"
            :width="340"
            trigger="click"
            v-model:visible="notifPopoverVisible"
          >
            <template #reference>
              <el-badge
                :value="notificationStore.unreadCount"
                :max="99"
                :hidden="!notificationStore.unreadCount"
                class="sr-notification-btn"
              >
                <el-button :icon="Bell" circle />
              </el-badge>
            </template>
            <div class="sr-notification-list">
              <div class="sr-notification-header">
                <span>消息通知</span>
                <div class="sr-notification-actions">
                  <el-button
                    v-if="notificationStore.unreadCount > 0"
                    size="small"
                    text
                    type="primary"
                    @click="handleMarkAllRead"
                  >
                    全部已读
                  </el-button>
                  <el-button size="small" text @click="goNotifications">查看全部</el-button>
                </div>
              </div>
              <div
                v-for="notif in notificationStore.recent"
                :key="notif.id"
                class="sr-notification-item"
                :class="{ unread: notif.read === 0 }"
                @click="handleNotificationClick(notif)"
              >
                <span v-if="notif.read === 0" class="sr-notification-dot"></span>
                <div class="sr-notification-body">
                  <div class="sr-notification-title">{{ notif.title }}</div>
                  <div class="sr-notification-time">{{ formatRelativeTime(notif.createdAt) }}</div>
                </div>
              </div>
              <el-empty v-if="!notificationStore.recent.length" description="暂无通知" :image-size="48" />
            </div>
          </el-popover>

          <!-- 消息详情 Dialog -->
          <el-dialog v-model="notifDetailVisible" title="消息详情" width="520px" :close-on-click-modal="false">
            <div v-if="currentNotif" class="sr-notif-detail">
              <div class="sr-notif-detail-title">{{ currentNotif.title }}</div>
              <div class="sr-notif-detail-time">{{ formatRelativeTime(currentNotif.createdAt) }}</div>
              <div class="sr-notif-detail-content">{{ currentNotif.content || '暂无内容' }}</div>
              <div v-if="currentNotif.actionUrl" class="sr-notif-detail-url">
                关联页面：{{ currentNotif.actionUrl }}
              </div>
            </div>
            <template #footer>
              <el-button v-if="currentNotif?.actionUrl" type="primary" @click="jumpToNotifAction">前往处理</el-button>
              <el-button @click="notifDetailVisible = false">关闭</el-button>
            </template>
          </el-dialog>

          <!-- User Dropdown -->
          <el-dropdown trigger="click" @command="handleUserCommand">
            <span class="sr-user-info">
              <el-avatar :size="32" :src="userStore.userAvatar">{{ userStore.userName?.charAt(0) || 'U' }}</el-avatar>
              <span class="sr-user-name">{{ userStore.userName || '用户名' }}</span>
              <el-tag v-if="userStore.userInfo?.roleName" size="small" type="info" class="sr-user-role">{{ userStore.userInfo.roleName }}</el-tag>
              <el-icon class="sr-user-arrow"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon> 个人信息
                </el-dropdown-item>
                <el-dropdown-item command="settings">
                  <el-icon><Setting /></el-icon> 系统设置
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon> 退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <!-- Page Content -->
      <div class="sr-content" :style="{ padding: '24px' }">
        <slot />
      </div>

      <!-- Footer -->
      <footer class="sr-footer">
        <span v-html="settingsStore.copyrightText"></span>
      </footer>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, onMounted, onBeforeUnmount, ref, computed, markRaw, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Bell, Expand, Fold, ArrowDown, User, Setting, SwitchButton, Tools,
  DataAnalysis, Briefcase, Document, UserFilled, VideoCamera,
  Star, Collection, Tickets, List, Money, OfficeBuilding,
  Avatar, DataLine, Monitor, Connection, Promotion, MagicStick, Checked, Clock, School, Trophy,
  DocumentChecked
} from '@element-plus/icons-vue'
import { useAppStore } from '@/stores/app'
import Watermark from '@/components/Watermark.vue'
import { useNotificationStore } from '@/stores/notification'
import { useUserStore } from '@/stores/user'
import { useSettingsStore } from '@/stores/settings'
import { formatRelativeTime, navigateToActionUrl } from '@/utils/notification'
import { getResumeStats } from '@/api/resume'
import type { NotificationVO } from '@/types/models'
import { getJobStats } from '@/api/job'
import { getCandidateStats } from '@/api/candidate'
import { getApprovalStats } from '@/api/offer'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const userStore = useUserStore()
const settingsStore = useSettingsStore()
const notificationStore = useNotificationStore()

const notifPopoverVisible = ref(false)
const notifDetailVisible = ref(false)
const currentNotif = ref<NotificationVO | null>(null)

let badgeTimer: ReturnType<typeof setInterval> | null = null

interface NavItem {
  route: string
  label: string
  icon?: unknown
  badge?: number
  /** 访问该菜单所需的权限码（缺省表示不限制）。 */
  perm?: string
}

interface NavGroup {
  key: string
  title: string
  items: NavItem[]
}

const navGroups = reactive<NavGroup[]>([
  {
    key: 'main',
      title: '主菜单',
      items: [
        { route: '/dashboard', label: '工作台', icon: markRaw(DataAnalysis), perm: 'dashboard' },
        { route: '/jobs', label: '职位管理', icon: markRaw(Briefcase), perm: 'recruitment:job' },
        { route: '/resumes', label: '简历筛选', icon: markRaw(Document), perm: 'recruitment:resume' },
        { route: '/candidates', label: '候选人中心', icon: markRaw(UserFilled), perm: 'candidate:view' },
        { route: '/interviews', label: '面试管理', icon: markRaw(VideoCamera), perm: 'recruitment:interview' },
        { route: '/interviews/ai', label: 'AI智能出题', icon: markRaw(MagicStick), perm: 'recruitment:interview' },
        { route: '/question-banks', label: '面试题库', icon: markRaw(List), perm: 'recruitment:interview' },
        { route: '/online-assessments', label: '在线测评', icon: markRaw(Tickets), perm: 'recruitment:interview' },
        { route: '/talent-pool', label: '人才库', icon: markRaw(Collection), perm: 'talent' },
        { route: '/analytics', label: '数据分析', icon: markRaw(DataLine), perm: 'analytics' },
      ],
  },
  {
    key: 'process',
    title: '招聘流程',
    items: [
      { route: '/offers', label: 'Offer管理', icon: markRaw(Money), perm: 'recruitment:offer' },
      { route: '/offers/approval', label: 'Offer审批', icon: markRaw(Checked), perm: 'offer:approve' },
      { route: '/onboarding', label: '入职管理', icon: markRaw(Tickets), perm: 'onboarding' },
      { route: '/contracts', label: '合同管理', icon: markRaw(DocumentChecked), perm: 'recruitment:contract' },
    ],
  },
  {
    key: 'careers',
    title: '招聘官网',
    items: [
      { route: '/careers/config', label: '官网配置', icon: markRaw(Monitor), perm: 'careers:config' },
      { route: '/careers/hot-jobs', label: '热门职位', icon: markRaw(Briefcase), perm: 'careers:hot-jobs' },
      { route: '/careers/social', label: '社会招聘', icon: markRaw(UserFilled), perm: 'careers:social' },
      { route: '/careers/campus', label: '校园招聘', icon: markRaw(School), perm: 'careers:campus' },
    ],
  },
  {
    key: 'referral',
    title: '工作内推',
    items: [
      { route: '/referral', label: '内推管理', icon: markRaw(Promotion), perm: 'referral' },
      { route: '/referral/records', label: '内推记录', icon: markRaw(Star), perm: 'referral' },
      { route: '/referral/leaderboard', label: '内推排行榜', icon: markRaw(Trophy), perm: 'referral' },
    ],
  },
  {
    key: 'system',
    title: '系统管理',
    items: [
      { route: '/system/users', label: '用户管理', icon: markRaw(Avatar), perm: 'system:user' },
      { route: '/system/roles', label: '角色管理', icon: markRaw(Monitor), perm: 'system:role' },
      { route: '/system/departments', label: '部门管理', icon: markRaw(OfficeBuilding), perm: 'system:dept' },
      { route: '/system/permissions', label: '权限管理', icon: markRaw(List), perm: 'system:permission' },
      { route: '/system/logs', label: '审计日志查询', icon: markRaw(Document), perm: 'system:log' },
      { route: '/notifications', label: '消息通知', icon: markRaw(Bell), perm: 'notification:view' },
      { route: '/system/settings', label: '系统设置', icon: markRaw(Tools), perm: 'system:settings' },
    ],
  },
  {
    key: 'ai',
    title: 'AI引擎',
    items: [
      { route: '/agents', label: 'AI智能编排', icon: markRaw(Connection), perm: 'ai' },
    ],
  },
])

/**
 * 按当前用户权限过滤后的可见菜单分组。
 * 无权限码的菜单项视为公开；空分组自动隐藏。
 */
const visibleNavGroups = computed<NavGroup[]>(() =>
  navGroups
    .map(group => ({
      ...group,
      items: group.items.filter(item => !item.perm || userStore.hasPermission(item.perm)),
    }))
    .filter(group => group.items.length > 0),
)

// 默认展开：主菜单、招聘流程；工作内推、招聘官网等目录默认折叠
const defaultExpanded = new Set(['main', 'process'])
const collapsedGroups = reactive(
  new Set<string>(visibleNavGroups.value.filter(g => !defaultExpanded.has(g.key)).map(g => g.key)),
)

function toggleGroup(key: string) {
  if (appStore.sidebarCollapsed) return
  if (collapsedGroups.has(key)) {
    collapsedGroups.delete(key)
  } else {
    collapsedGroups.add(key)
  }
}

function isGroupExpanded(key: string): boolean {
  return !collapsedGroups.has(key)
}

// 路由变化时自动展开包含当前激活菜单项的目录
watch(() => route.path, (path) => {
  for (const group of visibleNavGroups.value) {
    if (group.items.some(item => path === item.route || path.startsWith(item.route + '/'))) {
      collapsedGroups.delete(group.key)
      break
    }
  }
}, { immediate: true })

async function loadJobBadge() {
  try {
    const stats = await getJobStats()
    navGroups[0].items[1].badge = stats.published
  } catch {
    navGroups[0].items[1].badge = undefined
  }
}

async function loadResumeBadge() {
  try {
    const stats = await getResumeStats()
    navGroups[0].items[2].badge = stats.pending > 0 ? stats.pending : undefined
  } catch {
    navGroups[0].items[2].badge = undefined
  }
}

async function loadCandidateBadge() {
  try {
    const stats = await getCandidateStats()
    navGroups[0].items[3].badge = stats.totalCount > 0 ? stats.totalCount : undefined
  } catch {
    navGroups[0].items[3].badge = undefined
  }
}

async function loadApprovalBadge() {
  try {
    const stats = await getApprovalStats()
    navGroups[1].items[1].badge = stats.pendingCount > 0 ? stats.pendingCount : undefined
  } catch {
    navGroups[1].items[1].badge = undefined
  }
}

/** 统一刷新四个菜单角标。 */
async function loadMenuBadges() {
  // 只刷新当前用户有权限的菜单角标，避免无权限用户也去调用各业务统计接口
  const tasks: Promise<void>[] = []
  if (userStore.hasPermission('recruitment:job')) tasks.push(loadJobBadge())
  if (userStore.hasPermission('recruitment:resume')) tasks.push(loadResumeBadge())
  if (userStore.hasPermission('candidate:view')) tasks.push(loadCandidateBadge())
  if (userStore.hasPermission('offer:approve')) tasks.push(loadApprovalBadge())
  await Promise.allSettled(tasks)
}

onMounted(() => {
  loadMenuBadges()
  // 每 60 秒刷新一次菜单角标（页面可见时），保证数量实时
  badgeTimer = setInterval(() => {
    if (document.visibilityState === 'visible') {
      void loadMenuBadges()
    }
  }, 60_000)
  notificationStore.refresh()
  notificationStore.startPolling()
})

onBeforeUnmount(() => {
  if (badgeTimer) {
    clearInterval(badgeTimer)
    badgeTimer = null
  }
  notificationStore.stopPolling()
})

async function handleNotificationClick(notif: NotificationVO) {
  if (notif.read === 0) {
    await notificationStore.markRead(notif)
  }
  notifPopoverVisible.value = false
  currentNotif.value = notif
  notifDetailVisible.value = true
}

/** 从消息详情跳转到关联业务页面。 */
function jumpToNotifAction() {
  if (!currentNotif.value?.actionUrl) return
  notifDetailVisible.value = false
  const result = navigateToActionUrl(router, currentNotif.value.actionUrl)
  if (result === 'none') {
    ElMessage.warning('该消息暂无可跳转的页面')
  } else if (result === 'same') {
    ElMessage.info('已在该页面')
  }
}

async function handleMarkAllRead() {
  await notificationStore.markAllRead()
}

function goNotifications() {
  notifPopoverVisible.value = false
  router.push('/notifications')
}

const topLevelRoutes = computed(() =>
  new Set(visibleNavGroups.value.flatMap(g => g.items.map(i => i.route)))
)

function isActive(routePath: string): boolean {
  if (route.path === routePath) return true
  if (route.path.startsWith(routePath + '/')) {
    // 如果当前路径自身也是顶级菜单项，不触发前缀激活（避免同时高亮两个菜单）
    if (topLevelRoutes.value.has(route.path)) return false
    return true
  }
  return route.path.startsWith(routePath + '?')
}

function handleUserCommand(command: string) {
  if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  } else if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'settings') {
    router.push('/system/settings')
  }
}
</script>

<style scoped>
.sr-layout {
  display: flex;
  min-height: 100vh;
}

/* Sidebar */
.sr-sidebar {
  width: var(--c-sidebar-width);
  background: var(--c-sidebar-bg);
  color: var(--c-sidebar-text);
  transition: var(--c-transition-slow);
  display: flex;
  flex-direction: column;
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  z-index: 100;
  overflow-y: auto;
  overflow-x: hidden;
}

.sr-sidebar.collapsed {
  width: var(--c-sidebar-collapsed-width);
}

.sr-sidebar-logo {
  height: 60px;
  display: flex;
  align-items: center;
  padding: 8px 20px 0;
  gap: 12px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.1);
  cursor: pointer;
}

.sr-sidebar-logo-icon {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
}

.sr-sidebar-logo-icon svg {
  width: 100%;
  height: 100%;
  display: block;
}

.sr-sidebar-logo-text {
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-width: 0;
}

.sr-sidebar-logo-name {
  font-size: 15px;
  font-weight: 700;
  color: #ffffff;
  letter-spacing: -0.3px;
  line-height: 1.25;
  white-space: nowrap;
}

.sr-sidebar-logo-tagline {
  font-size: 12px;
  font-weight: 500;
  color: #cbd5e1;
  white-space: nowrap;
}

.sr-sidebar-nav {
  flex: 1;
  padding: 8px 0;
}

.sr-nav-group {
  margin-bottom: 4px;
}

.sr-nav-group-title {
  font-size: 11px;
  text-transform: uppercase;
  color: var(--c-sidebar-text);
  padding: 12px 20px 6px;
  letter-spacing: 0.5px;
  font-weight: 600;
  opacity: 0.6;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.sr-nav-group-title.clickable {
  cursor: pointer;
  user-select: none;
}

.sr-nav-group-title.clickable:hover {
  opacity: 0.85;
}

.sr-group-arrow {
  font-size: 10px;
  transition: transform 0.2s ease;
}

.sr-group-arrow.expanded {
  transform: rotate(180deg);
}

.sr-nav-list {
  list-style: none;
}

.sr-nav-item {
  margin: 2px 8px;
  border-radius: var(--c-radius-md);
  transition: var(--c-transition);
}

.sr-nav-item:hover {
  background: var(--c-sidebar-hover);
}

.sr-nav-item.active {
  background: var(--c-sidebar-active);
}

.sr-nav-item.active .sr-nav-link {
  color: var(--c-primary-light);
}

.sr-nav-link {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  color: var(--c-sidebar-text);
  text-decoration: none;
  border-radius: var(--c-radius-md);
  transition: var(--c-transition);
  position: relative;
  white-space: nowrap;
}

.sr-nav-link:hover {
  color: var(--c-sidebar-text-active);
}

.sr-nav-icon {
  font-size: 18px;
  flex-shrink: 0;
}

.sr-nav-label {
  font-size: 14px;
  font-weight: 500;
}

.sr-nav-badge {
  margin-left: auto;
}

/* Main Content */
.sr-main-wrapper {
  flex: 1;
  margin-left: var(--c-sidebar-width);
  transition: var(--c-transition-slow);
  min-width: 0;
}

.sidebar-collapsed .sr-main-wrapper {
  margin-left: var(--c-sidebar-collapsed-width);
}

/* Topbar */
.sr-topbar {
  height: var(--c-topbar-height);
  background: var(--c-card);
  border-bottom: 1px solid var(--c-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  position: sticky;
  top: 0;
  z-index: 50;
  box-shadow: var(--c-shadow-sm);
}

.sr-topbar-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.sr-toggle-btn {
  font-size: 18px;
  color: var(--c-text-secondary);
}

.sr-topbar-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.sr-status-indicator {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--c-text-secondary);
}

.sr-status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--c-success);
}

.sr-status-dot.online {
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.sr-notification-btn {
  cursor: pointer;
}

.sr-user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: var(--c-radius-md);
  transition: var(--c-transition);
}

.sr-user-info:hover {
  background: var(--c-bg);
}

.sr-user-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--c-text);
}

.sr-user-role {
  font-size: 11px;
}

.sr-user-arrow {
  font-size: 12px;
  color: var(--c-text-secondary);
}

/* Notification Popover */
.sr-notification-list {
  max-height: 300px;
  overflow-y: auto;
}

.sr-notification-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
  font-weight: 600;
  color: var(--c-text);
  padding-bottom: 12px;
  border-bottom: 1px solid var(--c-border);
  margin-bottom: 8px;
}

.sr-notification-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.sr-notification-item {
  display: flex;
  gap: 8px;
  padding: 10px 6px;
  border-bottom: 1px solid var(--c-border-light);
  border-radius: var(--c-radius-md);
  cursor: pointer;
  transition: var(--c-transition);
}

.sr-notification-item:hover {
  background: var(--c-bg);
}

.sr-notification-dot {
  flex-shrink: 0;
  width: 6px;
  height: 6px;
  margin-top: 6px;
  border-radius: 50%;
  background: var(--c-primary);
}

.sr-notification-body {
  flex: 1;
  min-width: 0;
}

.sr-notification-item.unread .sr-notification-title {
  color: var(--c-text);
  font-weight: 500;
}

.sr-notification-title {
  font-size: 13px;
  color: var(--c-text);
  line-height: 1.5;
}

.sr-notification-time {
  font-size: 12px;
  color: var(--c-text-muted);
  margin-top: 4px;
}

/* Content */
.sr-content {
  min-height: calc(100vh - var(--c-topbar-height));
}

/* Footer */
.sr-footer {
  padding: 14px 24px;
  text-align: center;
  font-size: 12px;
  color: var(--c-text-muted);
  border-top: 1px solid var(--c-border-light);
  background: var(--el-bg-color);
}

/* Responsive */
@media (max-width: 768px) {
  .sr-sidebar {
    width: var(--c-sidebar-width);
    transform: translateX(-100%);
  }

  .sr-sidebar.collapsed {
    transform: translateX(0);
    width: var(--c-sidebar-collapsed-width);
  }

  .sr-main-wrapper {
    margin-left: 0 !important;
  }
}

/* -- 消息详情 -- */
.sr-notif-detail-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 8px;
}
.sr-notif-detail-time {
  font-size: 12px;
  color: #94a3b8;
  margin-bottom: 14px;
}
.sr-notif-detail-content {
  font-size: 14px;
  line-height: 1.7;
  color: #475569;
  white-space: pre-wrap;
  word-break: break-word;
}
.sr-notif-detail-url {
  margin-top: 14px;
  font-size: 12px;
  color: #94a3b8;
  word-break: break-all;
}
</style>

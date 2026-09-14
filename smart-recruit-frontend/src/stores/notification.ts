import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  getRecentNotifications,
  getUnreadNotificationCount,
  markAllNotificationsRead,
  markNotificationRead,
} from '@/api/notification'
import type { NotificationVO } from '@/types/models'

const POLL_INTERVAL_MS = 30_000

/**
 * 全局通知 Store。
 *
 * <p>维护未读数与最近通知列表，启动后定时轮询刷新，
 * 供顶部通知铃铛与通知中心页面实时消费。</p>
 */
export const useNotificationStore = defineStore('notification', () => {
  const unreadCount = ref(0)
  const recent = ref<NotificationVO[]>([])
  const loaded = ref(false)

  let timer: ReturnType<typeof setInterval> | null = null

  async function loadUnread() {
    try {
      // 后端 Long 统一序列化为字符串，这里强转为数字，保证 0 判断可靠
      unreadCount.value = Number(await getUnreadNotificationCount())
    } catch {
      // 静默失败，保留现有值
    }
  }

  async function loadRecent(limit = 8) {
    try {
      recent.value = await getRecentNotifications(limit)
      loaded.value = true
    } catch {
      // 静默失败
    }
  }

  async function refresh() {
    await Promise.all([loadUnread(), loadRecent()])
  }

  async function markRead(notification: NotificationVO) {
    try {
      await markNotificationRead(notification.id)
      notification.read = 1
      if (unreadCount.value > 0) unreadCount.value -= 1
    } catch {
      // HTTP 拦截器统一提示
    }
  }

  async function markAllRead() {
    try {
      await markAllNotificationsRead()
      unreadCount.value = 0
      recent.value.forEach(n => { n.read = 1 })
    } catch {
      // HTTP 拦截器统一提示
    }
  }

  function startPolling() {
    if (timer) return
    timer = setInterval(() => {
      // 仅当前页面可见时刷新，减少无效请求
      if (document.visibilityState === 'visible') {
        void refresh()
      }
    }, POLL_INTERVAL_MS)
  }

  function stopPolling() {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
  }

  return {
    unreadCount,
    recent,
    loaded,
    loadUnread,
    loadRecent,
    refresh,
    markRead,
    markAllRead,
    startPolling,
    stopPolling,
  }
})

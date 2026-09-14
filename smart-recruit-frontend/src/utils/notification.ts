/**
 * 通知业务分类展示工具。
 */

import type { Router } from 'vue-router'

const TYPE_LABELS: Record<number, string> = {
  0: '系统',
  1: '面试',
  2: 'Offer',
  3: '入职',
  4: '内推',
  5: '人才',
  6: '招聘',
  7: '合同',
}

export function notificationTypeLabel(type: number): string {
  return TYPE_LABELS[type] || '系统'
}

export function notificationTypeTag(type: number): 'info' | 'primary' | 'warning' | 'success' | 'danger' {
  switch (type) {
    case 0: return 'info'
    case 1: return 'primary'
    case 2: return 'warning'
    case 3: return 'success'
    case 4: return 'danger'
    case 6: return 'primary'
    default: return 'info'
  }
}

/**
 * 相对时间：刚刚 / N分钟前 / N小时前 / N天前 / 日期。
 */
export function formatRelativeTime(dateStr: string): string {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const diff = Date.now() - date.getTime()
  if (diff < 60 * 1000) return '刚刚'
  const minutes = Math.floor(diff / (60 * 1000))
  if (minutes < 60) return `${minutes}分钟前`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}小时前`
  const days = Math.floor(hours / 24)
  if (days < 7) return `${days}天前`
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

/**
 * 跳转到消息关联的业务页面。
 *
 * <p>兼容三种 actionUrl 形态：内部路径（如 /interviews/123）、外链（http/https，新窗口打开）、
 * 带前后空白的路径。命中兜底路由（页面不存在）时返回 false，由调用方提示用户。</p>
 *
 * @returns 'ok' 已跳转；'same' 目标就是当前页面；'none' 无有效跳转页面
 */
export function navigateToActionUrl(router: Router, url: string | null | undefined): 'ok' | 'same' | 'none' {
  console.log('[通知跳转] 触发跳转, actionUrl =', url)
  if (!url) return 'none'
  // 去除前后空白与可能残留的引号
  const trimmed = url.trim().replace(/^["']+|["']+$/g, '')
  if (!trimmed) return 'none'

  // 无实际业务页面的占位地址
  if (trimmed === '/' || trimmed === '/dashboard' || trimmed === '/notifications') {
    return 'none'
  }

  // 外链：新窗口打开
  if (/^https?:\/\//i.test(trimmed)) {
    window.open(trimmed, '_blank', 'noopener')
    return 'ok'
  }

  if (!trimmed.startsWith('/')) return 'none'

  const resolved = router.resolve(trimmed)
  // 命中兜底路由说明页面不存在
  if (resolved.matched.some(r => r.path === '/:pathMatch(.*)*')) {
    return 'none'
  }
  // 目标就是当前页面：避免无意义的整页刷新
  if (resolved.fullPath === router.currentRoute.value.path) {
    return 'same'
  }
  // 用硬跳转保证一定切换页面（不依赖 vue-router 内部状态，避免静默失败）
  console.log('[通知跳转] 跳转到', resolved.fullPath)
  window.location.href = resolved.fullPath
  return 'ok'
}

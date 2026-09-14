import request from './request'
import type { PageResult } from '@/types/api'
import type { NotificationVO } from '@/types/models'

/**
 * 分页查询当前用户通知。
 */
export function getNotifications(params: {
  page?: number
  size?: number
  type?: number
  read?: number
}): Promise<PageResult<NotificationVO>> {
  return request.get('/notifications', { params })
}

/**
 * 查询当前用户最近 N 条通知（顶部下拉预览）。
 */
export function getRecentNotifications(limit = 8): Promise<NotificationVO[]> {
  return request.get('/notifications/recent', { params: { limit } })
}

/**
 * 查询当前用户未读通知数。
 */
export function getUnreadNotificationCount(): Promise<number> {
  return request.get('/notifications/unread-count')
}

/**
 * 将一条通知标记为已读。
 */
export function markNotificationRead(id: string | number): Promise<void> {
  return request.put(`/notifications/${id}/read`)
}

/**
 * 将当前用户所有未读通知标记为已读。
 */
export function markAllNotificationsRead(): Promise<void> {
  return request.put('/notifications/read-all')
}

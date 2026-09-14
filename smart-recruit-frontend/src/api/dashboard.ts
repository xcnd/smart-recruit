import request from './request'
import type {
  DashboardKpiVO,
  FunnelStageVO,
  DepartmentProgressVO,
  RecentActivityVO,
  PendingTaskVO,
} from '@/types/models'

export function getKpi(): Promise<DashboardKpiVO> {
  return request.get('/dashboard/kpi')
}

export function getFunnel(): Promise<FunnelStageVO[]> {
  return request.get('/dashboard/funnel')
}

export function getDepartmentProgress(): Promise<DepartmentProgressVO[]> {
  return request.get('/dashboard/department-progress')
}

export function getRecentActivities(): Promise<RecentActivityVO[]> {
  return request.get('/dashboard/recent-activities')
}

export function getPendingTasks(): Promise<PendingTaskVO[]> {
  return request.get('/dashboard/pending-tasks')
}

export function completeTask(id: number | string): Promise<void> {
  return request.put(`/dashboard/pending-tasks/${id}/complete`)
}

export function dismissTask(id: number | string): Promise<void> {
  return request.put(`/dashboard/pending-tasks/${id}/dismiss`)
}

// ---- Paginated queries for dedicated pages ----

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export function getActivitiesPage(params: {
  page: number
  size: number
  type?: number
  keyword?: string
}): Promise<PageResult<RecentActivityVO>> {
  return request.get('/dashboard/activities', { params })
}

export function getTasksPage(params: {
  page: number
  size: number
  type?: number
  priority?: number
  status?: number
  keyword?: string
}): Promise<PageResult<PendingTaskVO>> {
  return request.get('/dashboard/tasks', { params })
}

import request from './request'
import type { AuditLogVO, AuditLogDetailVO, AuditLogStatsVO } from '@/types/models'
import type { PageResult, PageQuery } from '@/types/api'

export interface AuditLogQuery extends PageQuery {
  username?: string
  module?: number
  action?: number
  targetType?: string
  requestMethod?: string
  result?: number
  startDate?: string
  endDate?: string
  keyword?: string
}

/** 分页查询审计日志（多条件筛选）。 */
export function getAuditLogs(params: AuditLogQuery): Promise<PageResult<AuditLogVO>> {
  return request.get('/audit-logs', { params })
}

/** 查询审计日志统计概览。 */
export function getAuditLogStats(): Promise<AuditLogStatsVO> {
  return request.get('/audit-logs/stats')
}

/** 查询审计日志详情。 */
export function getAuditLogDetail(id: string | number): Promise<AuditLogDetailVO> {
  return request.get(`/audit-logs/${id}`)
}

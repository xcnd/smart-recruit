import request from './request'
import type { CareersJobVO } from '@/types/models'
import type { PageResult } from '@/types/api'

/**
 * 管理端：分页查询招聘官网职位
 */
export function getCareersJobs(params: {
  recType: string
  keyword?: string
  category?: string
  page?: number
  size?: number
}): Promise<PageResult<CareersJobVO>> {
  return request.get('/careers/jobs', { params })
}

/**
 * 管理端：创建职位
 */
export function createCareersJob(data: Partial<CareersJobVO> & { recType: string; title: string; dept: string; location: string }): Promise<CareersJobVO> {
  return request.post('/careers/jobs', data)
}

/**
 * 管理端：更新职位
 */
export function updateCareersJob(id: number, data: Partial<CareersJobVO>): Promise<CareersJobVO> {
  return request.put(`/careers/jobs/${id}`, data)
}

/**
 * 管理端：删除职位
 */
export function deleteCareersJob(id: number): Promise<void> {
  return request.delete(`/careers/jobs/${id}`)
}

/**
 * 管理端：切换职位状态
 */
export function updateCareersJobStatus(id: number, status: number): Promise<void> {
  return request.put(`/careers/jobs/${id}/status`, { status })
}

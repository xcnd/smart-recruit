import request from './request'
import type { JobVO, JobCreateDTO, JobStatsVO } from '@/types/models'
import type { PageResult, PageQuery } from '@/types/api'

export function getJobs(params: PageQuery & Record<string, unknown>): Promise<PageResult<JobVO>> {
  return request.get('/jobs', { params })
}

export function getJobById(id: string): Promise<JobVO> {
  return request.get(`/jobs/${id}`)
}

export function createJob(data: JobCreateDTO): Promise<JobVO> {
  return request.post('/jobs', data)
}

export function updateJob(id: string, data: Partial<JobCreateDTO>): Promise<JobVO> {
  return request.put(`/jobs/${id}`, data)
}

export function deleteJob(id: string): Promise<void> {
  return request.delete(`/jobs/${id}`)
}

export function updateJobStatus(id: string | number, status: number): Promise<void> {
  return request.put(`/jobs/${id}/status`, { status })
}

export interface AiGenerateJdParams {
  title: string
  department?: string
  experience?: string
  location?: string
  skills?: string[]
}

export interface JdGenerateTaskVO {
  taskId: string
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED'
  message?: string
  title?: string
  description?: string
}

/** 异步启动 AI 生成职位描述，返回任务 ID（避免 LLM 调用超时）。 */
export function startAiGenerateJd(data: AiGenerateJdParams): Promise<JdGenerateTaskVO> {
  return request.post('/jobs/ai-generate-jd', data)
}

/** 查询 AI 生成 JD 异步任务状态与结果（前端轮询）。 */
export function getAiGenerateJdTask(taskId: string): Promise<JdGenerateTaskVO> {
  return request.get(`/jobs/ai-generate-jd/${taskId}`)
}

export function getJobStats(): Promise<JobStatsVO> {
  return request.get('/job-statistics')
}

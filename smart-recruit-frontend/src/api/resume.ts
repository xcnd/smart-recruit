import request from './request'
import type { ResumeVO, AiAnalysisResultVO, BatchScreenResult, BatchScreenProgress, ResumeParseStatusVO } from '@/types/models'
import type { PageResult, PageQuery } from '@/types/api'

/** 全流程编排：简历文件同步解析结果。 */
export interface PipelineResumeParseVO {
  rawText: string
  parsed?: {
    name?: string
    email?: string
    phone?: string
    gender?: string
    household?: string
    location?: string
    age?: string
    birthDate?: string
    politicalStatus?: string
    desiredPosition?: string
    workYears?: string
    skills?: string[]
    skillsText?: string
    summary?: string
    education?: Array<{ school?: string; major?: string; degree?: string; start?: string; end?: string }>
    experience?: Array<{ company?: string; position?: string; start?: string; end?: string; description?: string }>
    projects?: Array<{ company?: string; position?: string; start?: string; end?: string; description?: string }>
  }
}

export function uploadResume(formData: FormData, jobPositionId?: string,
                             referrerId?: string, autoScreen = true): Promise<ResumeVO> {
  if (jobPositionId) {
    formData.append('jobPositionId', jobPositionId)
  }
  if (referrerId) {
    formData.append('referrerId', referrerId)
  }
  formData.append('autoScreen', autoScreen ? 'true' : 'false')
  return request.post('/resumes/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function batchScreen(params: { resumeIds?: string[]; jobId?: string; threshold?: number }): Promise<BatchScreenResult> {
  return request.post('/resumes/batch-screen', params)
}

/** 同步解析简历文件（全流程编排上传用，含 AI 解析，耗时较长）。 */
export function parseResumeFile(file: File): Promise<PipelineResumeParseVO> {
  const fd = new FormData()
  fd.append('file', file)
  return request.post('/resumes/parse-file', fd, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 180000,
  })
}

export function getBatchProgress(taskId: string): Promise<BatchScreenProgress> {
  return request.get(`/resumes/batch-screen/${taskId}/progress`)
}

export function getResumes(params: PageQuery & Record<string, unknown>): Promise<PageResult<ResumeVO>> {
  return request.get('/resumes', { params })
}

export function getResumeDetail(id: string): Promise<ResumeVO> {
  return request.get(`/resumes/${id}`)
}

export function getAiResult(id: string): Promise<AiAnalysisResultVO> {
  return request.get(`/resumes/${id}/ai-result`)
}

export function updateScreeningStatus(id: string, screeningStatus: number): Promise<void> {
  return request.put(`/resumes/${id}/screening-status`, { screeningStatus })
}

export function updateResumeJob(id: string, jobPositionId: string | null): Promise<void> {
  return request.put(`/resumes/${id}/job`, { jobPositionId })
}

export function getParseStatus(id: string | number): Promise<ResumeParseStatusVO> {
  return request.get(`/resumes/${id}/parse-status`)
}

export function deleteResume(id: string): Promise<void> {
  return request.delete(`/resumes/${id}`)
}

export function getResumeStats(): Promise<{ total: number; pending: number; passed: number; rejected: number }> {
  return request.get('/resume-statistics')
}

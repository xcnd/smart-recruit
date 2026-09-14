import request from './request'
import type {
  CandidateVO,
  CandidateStatsVO,
  CreateCandidateDTO,
  UpdateCandidateDTO,
  StageHistoryVO,
  StageTransition,
} from '@/types/models'
import type { PageResult, PageQuery } from '@/types/api'

export function getCandidates(params: PageQuery & Record<string, unknown>): Promise<PageResult<CandidateVO>> {
  return request.get('/candidates', { params })
}

export function getCandidateById(id: string): Promise<CandidateVO> {
  return request.get(`/candidates/${id}`)
}

export function createCandidate(data: CreateCandidateDTO): Promise<CandidateVO> {
  return request.post('/candidates', data)
}

export function updateCandidate(id: string, data: UpdateCandidateDTO): Promise<CandidateVO> {
  return request.put(`/candidates/${id}`, data)
}

export function deleteCandidate(id: string): Promise<void> {
  return request.delete(`/candidates/${id}`)
}

export function getStageHistory(id: string): Promise<StageHistoryVO[]> {
  return request.get(`/candidates/${id}/stage-history`)
}

export function getCandidateStats(): Promise<CandidateStatsVO> {
  return request.get('/candidates/stats')
}

export function updateStage(id: string, data: { stage: string; remark?: string }): Promise<void> {
  return request.put(`/candidates/${id}/stage`, data)
}

export function batchTransition(data: StageTransition[]): Promise<void> {
  return request.post('/candidates/batch-transition', data)
}

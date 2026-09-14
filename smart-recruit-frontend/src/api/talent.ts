import request from './request'
import type { TalentPoolVO, TalentCampaignVO, RecommendationTaskVO } from '@/types/models'
import type { PageResult, PageQuery } from '@/types/api'

// Talent Pool
export function getTalentPool(params: PageQuery & Record<string, unknown>): Promise<PageResult<TalentPoolVO>> {
  return request.get('/talent-pool', { params })
}

export function searchTalent(params: { keyword?: string; skills?: string[]; education?: string; experience?: string }): Promise<PageResult<TalentPoolVO>> {
  return request.post('/talent-pool/search', params)
}

export function getRecommendations(params: { jobId?: number; limit?: number }): Promise<TalentPoolVO[]> {
  return request.get('/talent-pool/recommendations', { params })
}

/** 启动异步 AI 人才推荐，返回任务 ID（后台匹配，避免 LLM 超时阻塞）。 */
export function startAsyncRecommendation(jobId: number): Promise<{ taskId: string }> {
  return request.post('/talent-pool/recommendations/async', { jobId })
}

/** 查询异步推荐任务状态与结果（前端轮询，支持按批分页）。 */
export function getAsyncRecommendation(
  taskId: string,
  params?: { page?: number; size?: number },
): Promise<RecommendationTaskVO> {
  return request.get(`/talent-pool/recommendations/async/${taskId}`, { params })
}

export function updateTags(id: string, tags: string[]): Promise<void> {
  return request.put(`/talent-pool/${id}/tags`, { tags })
}

export function addToPool(data: { candidateId: string; tags?: string[] }): Promise<TalentPoolVO> {
  return request.post('/talent-pool', data)
}

export function contactCandidate(id: string): Promise<void> {
  return request.post(`/talent-pool/${id}/contact`)
}

// Talent Activation Campaigns
export function getCampaigns(params: PageQuery): Promise<PageResult<TalentCampaignVO>> {
  return request.get('/talent-campaigns', { params })
}

export function createCampaign(data: Partial<TalentCampaignVO>): Promise<TalentCampaignVO> {
  return request.post('/talent-campaigns', data)
}

export function updateCampaignStatus(id: string, status: string): Promise<void> {
  return request.put(`/talent-campaigns/${id}/status`, { status })
}

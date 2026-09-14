import request from './request'
import type {
  OfferVO, OfferCreateDTO, ApprovalListVO, ApprovalStatsVO,
  ApprovalFlowConfigVO, CreateFlowConfigRequest, UpdateFlowConfigRequest, OfferOptionVO,
} from '@/types/models'
import type { PageResult, PageQuery } from '@/types/api'

export function getOffers(params: PageQuery & Record<string, unknown>): Promise<PageResult<OfferVO>> {
  return request.get('/offers', { params })
}

/** 查询可创建合同的 Offer 下拉选项（支持关键字搜索）。 */
export function getOfferOptions(keyword?: string): Promise<OfferOptionVO[]> {
  return request.get('/offers/options', { params: { keyword } })
}

export function createOffer(data: OfferCreateDTO): Promise<OfferVO> {
  return request.post('/offers', data)
}

export function getOfferDetail(id: string): Promise<OfferVO> {
  return request.get(`/offers/${id}`)
}

export function updateOffer(id: string, data: Partial<OfferCreateDTO>): Promise<OfferVO> {
  return request.put(`/offers/${id}`, data)
}

export function submitApproval(id: string): Promise<void> {
  return request.post(`/offers/${id}/submit-approval`)
}

export function approve(id: string, data: { approverId: number; approverName: string; approverRole?: string; status: number; comment?: string }): Promise<void> {
  return request.post(`/offers/${id}/approve`, data)
}

export function send(id: string): Promise<void> {
  return request.post(`/offers/${id}/send`)
}

export function sendOfferEmail(id: string): Promise<void> {
  return request.post(`/offers/${id}/send-email`)
}

export function manualConfirmOffer(id: string, data: { action: string; declineReason?: string }): Promise<void> {
  return request.post(`/offers/${id}/manual-confirm`, data)
}

export function deleteOffer(id: string): Promise<void> {
  return request.delete(`/offers/${id}`)
}

export interface OfferPredictionFactor {
  name: string
  impact: string
  weight: number
}

export interface OfferPredictionVO {
  offerId: string
  acceptanceProbability: number
  confidenceLevel?: number
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH'
  factors: OfferPredictionFactor[]
  suggestion?: string
}

export function getPrediction(id: string): Promise<OfferPredictionVO> {
  return request.get(`/offers/${id}/prediction`)
}

export function getPendingApprovals(params: PageQuery & Record<string, unknown>): Promise<PageResult<ApprovalListVO>> {
  return request.get('/offers/approvals/pending', { params })
}

export function getApprovalHistory(params: PageQuery): Promise<PageResult<ApprovalListVO>> {
  return request.get('/offers/approvals/history', { params })
}

export function getApprovalStats(): Promise<ApprovalStatsVO> {
  return request.get('/offers/approvals/stats')
}

// ---- Approval Flow Configs ----
export function getFlowConfigs(): Promise<ApprovalFlowConfigVO[]> {
  return request.get('/offers/flow-configs')
}

export function getFlowConfigByDepartment(departmentName: string): Promise<ApprovalFlowConfigVO> {
  return request.get(`/offers/flow-configs/department/${encodeURIComponent(departmentName)}`)
}

export function createFlowConfig(data: CreateFlowConfigRequest): Promise<ApprovalFlowConfigVO> {
  return request.post('/offers/flow-configs', data)
}

export function updateFlowConfig(id: string | number, data: UpdateFlowConfigRequest): Promise<ApprovalFlowConfigVO> {
  return request.put(`/offers/flow-configs/${id}`, data)
}

export function deleteFlowConfig(id: string | number): Promise<void> {
  return request.delete(`/offers/flow-configs/${id}`)
}

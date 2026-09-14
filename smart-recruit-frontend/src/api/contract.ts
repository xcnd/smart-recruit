import request from './request'
import type { ContractVO, ContractDetailVO, ContractStatsVO } from '@/types/models'
import type { PageResult, PageQuery } from '@/types/api'

export interface ContractQuery extends PageQuery {
  keyword?: string
  status?: number
  startDate?: string
  endDate?: string
}

/** 分页查询合同。 */
export function getContracts(params: ContractQuery): Promise<PageResult<ContractVO>> {
  return request.get('/contracts', { params })
}

/** 合同统计概览。 */
export function getContractStats(): Promise<ContractStatsVO> {
  return request.get('/contracts/stats')
}

/** 合同详情（含正文与签署记录）。 */
export function getContractDetail(id: string | number): Promise<ContractDetailVO> {
  return request.get(`/contracts/${id}`)
}

/** 从 Offer 创建合同。 */
export function createContract(offerId: string | number): Promise<ContractVO> {
  return request.post('/contracts', { offerId })
}

/** 提交审批。 */
export function submitContractApproval(id: string | number): Promise<void> {
  return request.post(`/contracts/${id}/submit-approval`)
}

/** 审批通过。 */
export function approveContract(id: string | number): Promise<void> {
  return request.post(`/contracts/${id}/approve`)
}

/** 发送给候选人签署。 */
export function sendContract(id: string | number): Promise<void> {
  return request.post(`/contracts/${id}/send`)
}

/** HR 签署。 */
export function hrSignContract(id: string | number, signerName: string): Promise<void> {
  return request.post(`/contracts/${id}/hr-sign`, { signerName })
}

/** 合同作废（已签署/生效中调用，状态变为已作废）。 */
export function voidContract(id: string | number, reason: string): Promise<void> {
  return request.post(`/contracts/${id}/void`, { reason })
}

/** 删除（仅草稿/已拒绝）。 */
export function deleteContract(id: string | number): Promise<void> {
  return request.delete(`/contracts/${id}`)
}

// ==================== 候选人公开签署 ====================

/** 通过签署 Token 获取合同。 */
export function getContractByToken(token: string): Promise<ContractDetailVO> {
  return request.get(`/public/contracts/${token}`)
}

/** 候选人确认签署/拒绝。 */
export function signContractByToken(
  token: string,
  data: {
    name: string
    accept: boolean
    remark?: string
    signature?: string
    idCard?: string
    phone?: string
    address?: string
  },
): Promise<void> {
  return request.post(`/public/contracts/${token}/sign`, data)
}

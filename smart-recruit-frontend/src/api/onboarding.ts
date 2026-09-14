import request from './request'
import type { OnboardingVO, OnboardingDetailVO, RetentionPredictionVO, OnboardingStatsVO } from '@/types/models'
import type { PageResult, PageQuery } from '@/types/api'

export function getOnboardings(params: PageQuery & Record<string, unknown>): Promise<PageResult<OnboardingVO>> {
  return request.get('/onboarding', { params })
}

export function getOnboardingDetail(id: string): Promise<OnboardingDetailVO> {
  return request.get(`/onboarding/${id}`)
}

export function createOnboarding(data: {
  offerId: string
  candidateId: string
  onboardDate?: string
  mentorId?: string
  buddyId?: string
}): Promise<OnboardingVO> {
  return request.post('/onboarding', data)
}

export function deleteOnboarding(id: string): Promise<void> {
  return request.delete(`/onboarding/${id}`)
}

export function advanceStep(id: string): Promise<OnboardingVO> {
  return request.post(`/onboarding/${id}/advance-step`)
}

export function completeOnboarding(id: string): Promise<void> {
  return request.post(`/onboarding/${id}/complete`)
}

export function updateDocuments(id: string, documents: Record<string, string>): Promise<void> {
  return request.put(`/onboarding/${id}/documents`, documents)
}

export function updateEquipment(
  onboardingId: string,
  equipmentId: string,
  data: { status?: number; assetNo?: string; remark?: string }
): Promise<void> {
  return request.put(`/onboarding/${onboardingId}/equipment/${equipmentId}`, data)
}

export function getRetentionPrediction(id: string): Promise<RetentionPredictionVO> {
  return request.get(`/onboarding/${id}/retention-prediction`)
}

export function getOnboardingStats(): Promise<OnboardingStatsVO> {
  return request.get('/onboarding/stats')
}

// ==================== 文件上传 & 单文档更新 ====================

export function uploadOnboardingFile(file: File): Promise<{ url: string; name: string; size: string }> {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/onboarding/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function updateSingleDocument(
  onboardingId: string,
  documentId: string,
  data: { filePath?: string; status?: number; remark?: string }
): Promise<void> {
  return request.put(`/onboarding/${onboardingId}/documents/${documentId}`, data)
}

export function deleteDocument(onboardingId: string, documentId: string): Promise<void> {
  return request.delete(`/onboarding/${onboardingId}/documents/${documentId}`)
}

// ==================== 步骤管理 ====================

export function updateMentor(id: string, data: { mentorId?: string; buddyId?: string }): Promise<void> {
  return request.put(`/onboarding/${id}/mentor`, data)
}

export function updateTraining(id: string, data: { trainingProgress?: number }): Promise<void> {
  return request.put(`/onboarding/${id}/training`, data)
}

export function sendWelcome(id: string): Promise<void> {
  return request.post(`/onboarding/${id}/welcome`)
}

export function updateAccount(id: string, data: { username?: string; email?: string; initialPassword?: string }): Promise<void> {
  return request.put(`/onboarding/${id}/account`, data)
}

/** 更新员工状态（0=待入职,1=试用期,2=正式,3=已离职）。 */
export function updateEmployeeStatus(id: string, status: number): Promise<void> {
  return request.put(`/onboarding/${id}/employee-status`, { status })
}

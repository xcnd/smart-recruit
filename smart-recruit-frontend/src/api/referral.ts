import request from './request'
import type {
  ReferralProgramVO,
  ReferralRecordVO,
  LeaderboardVO,
  RefProgramJobVO,
  BonusRecordVO,
  ReferralMatchVO,
} from '@/types/models'
import type { PageResult } from '@/types/api'

// ==================== 内推计划 ====================

/** 获取启用的内推计划列表 */
export function getPrograms() {
  return request.get<ReferralProgramVO[]>('/referrals/programs')
}

/** 内推计划分页查询 */
export function getProgramsPage(params: {
  page?: number
  size?: number
  keyword?: string
  status?: number
}) {
  return request.get<PageResult<ReferralProgramVO>>('/referrals/programs/page', { params })
}

/** 切换计划启用状态 */
export function toggleProgram(id: number) {
  return request.put<void>(`/referrals/programs/${id}/toggle`)
}

/** 创建内推计划 */
export function createProgram(data: {
  title: string
  description?: string
  bonusAmount: number
  bonusStructure?: Record<string, number>
  startDate: string
  endDate?: string
  eligibleDeptIds?: string[]
  status?: number
}) {
  return request.post<ReferralProgramVO>('/referrals/programs', data)
}

/** 更新内推计划 */
export function updateProgram(id: number, data: {
  title?: string
  description?: string
  bonusAmount?: number
  bonusStructure?: Record<string, number>
  startDate?: string
  endDate?: string
  eligibleDeptIds?: string[]
  status?: number
}) {
  return request.put<ReferralProgramVO>(`/referrals/programs/${id}`, data)
}

/** 删除内推计划 */
export function deleteProgram(id: number) {
  return request.delete<void>(`/referrals/programs/${id}`)
}

/** 更新计划奖金 */
export function updateBonus(id: number, bonusAmount: number) {
  return request.put<void>(`/referrals/programs/${id}/bonus`, { bonusAmount })
}

/** 批量保存计划设置 */
export function batchSave(programs: {
  id?: number
  jobId?: number
  jobTitle: string
  enabled: number
  bonusAmount: number
  description?: string
}[]) {
  return request.post<void>('/referrals/programs/batch-save', programs)
}

// ==================== 计划-职位关联 ====================

/** 获取计划下所有关联职位 */
export function getProgramJobs(programId: number) {
  return request.get<RefProgramJobVO[]>(`/referrals/programs/${programId}/jobs`)
}

/** 批量保存计划-职位关联 */
export function batchSaveProgramJobs(programId: number, jobs: {
  id?: number
  jobPositionId: number
  isEnabled?: number
  bonusAmount?: number | null
  tag?: number | null
}[]) {
  return request.post<void>(`/referrals/programs/${programId}/jobs/batch`, jobs)
}

/** 删除计划下的职位关联 */
export function deleteProgramJob(id: number) {
  return request.delete<void>(`/referrals/programs/jobs/${id}`)
}

/** 切换职位内推启用状态 */
export function toggleProgramJob(id: number) {
  return request.put<void>(`/referrals/programs/jobs/${id}/toggle`)
}

/** 更新职位级别内推奖金 */
export function updateProgramJobBonus(id: number, bonusAmount: number) {
  return request.put<void>(`/referrals/programs/jobs/${id}/bonus`, { bonusAmount })
}

// ==================== 内推记录 ====================

/** 获取所有内推记录（分页） */
export function getRecords(params: {
  page?: number
  size?: number
  keyword?: string
  status?: number
  referrerId?: number
  programId?: number
}) {
  return request.get<PageResult<ReferralRecordVO>>('/referrals/records', { params })
}

/** 创建内推记录 */
export function createReferral(data: {
  programId: number
  programJobId: number
  referrerId: number
  referrerName?: string
  referrerDept?: string
  candidateId: number
  candidateName?: string
  candidatePhone?: string
  candidateEmail?: string
  candidateResumeUrl?: string
  jobId: number
  jobTitle?: string
  relationship?: string
  referralNote?: string
  bonus?: number
}) {
  return request.post<ReferralRecordVO>('/referrals/records', data)
}

/** 获取我的内推记录 */
export function getMyReferrals(referrerId: number) {
  return request.get<ReferralRecordVO[]>('/referrals/my', { params: { referrerId } })
}

// ==================== 其他 ====================

/** 获取排行榜 */
export function getLeaderboard() {
  return request.get<LeaderboardVO[]>('/referrals/leaderboard')
}

/** 获取内推政策 */
export function getPolicy() {
  return request.get<Record<string, unknown>>('/referrals/policy')
}

/** 生成内推海报 */
export function generatePoster(programId: number) {
  return request.post<Record<string, string>>('/referrals/poster/generate', { programId })
}

// ==================== 奖金发放记录 ====================

/** 获取指定内推记录的分阶段奖金明细 */
export function getBonusRecords(recordId: string) {
  return request.get<BonusRecordVO[]>(`/referrals/records/${recordId}/bonus`)
}

/** 确认发放单条奖金 */
export function payBonusRecord(id: string) {
  return request.put<void>(`/referrals/bonus-records/${id}/pay`)
}

// ==================== 内推智能匹配 ====================

/** 查询内推记录的智能匹配推荐 */
export async function getReferralMatches(recordId: string): Promise<ReferralMatchVO[]> {
  return (await request.get(`/referrals/records/${recordId}/matches`)) as unknown as ReferralMatchVO[]
}

/** 重新触发内推智能匹配（AI Agent 能力网关，失败降级本地启发式） */
export async function refreshReferralMatches(recordId: string): Promise<ReferralMatchVO[]> {
  return (await request.post(`/referrals/records/${recordId}/matches/refresh`)) as unknown as ReferralMatchVO[]
}

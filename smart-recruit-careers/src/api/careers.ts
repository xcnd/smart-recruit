import request from './request'

export interface CareersJobVO {
  id: number
  recType: string
  title: string
  dept: string
  location: string
  exp: string
  salary: string
  category: string
  tags: Array<{ text: string; cls: string }>
  responsibilities: string[]
  requirements: string[]
  bonus: string[]
  sortOrder: number
  status: number
  createTime: string
  updateTime: string
}

/**
 * 公开端点：按类型获取所有已发布的招聘官网职位
 * @param recType 招聘类型：SOCIAL / CAMPUS / HOT
 */
export function getCareersPublicJobs(recType: string): Promise<CareersJobVO[]> {
  return request.get('/careers/jobs/public', { params: { recType } })
}

/**
 * 公开端点：获取单个职位详情（无需认证）
 * @param id 职位ID
 */
export function getCareersPublicJobById(id: number): Promise<CareersJobVO> {
  return request.get('/careers/jobs/public/detail', { params: { id } })
}

/**
 * 公开端点：上传简历文件
 * @param file 文件对象
 */
export function uploadResume(file: File): Promise<{ url: string }> {
  const fd = new FormData()
  fd.append('file', file)
  return request.post('/careers/jobs/public/upload-resume', fd, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

/**
 * 公开端点：提交投递申请
 */
export function submitApplication(data: {
  jobId: number
  candidateName: string
  candidatePhone: string
  candidateEmail: string
  resumeUrl?: string
  candidateId?: number
}): Promise<void> {
  return request.post('/careers/jobs/public/apply', data)
}

/**
 * 公开端点：检查是否已投递过某职位
 * @param jobId 职位ID
 * @param userId 系统用户ID（优先）
 * @param email 候选人邮箱（备用）
 */
export function checkApplied(jobId: number, userId?: number, email?: string): Promise<{ applied: boolean }> {
  return request.get('/careers/jobs/public/check-applied', { params: { jobId, userId, email } })
}

/**
 * 公开端点：获取候选人的投递记录（优先按 userId，其次按邮箱）
 * @param userId 系统用户ID（优先）
 * @param email 候选人邮箱（备用）
 */
export function getCareersMyApplications(userId?: number, email?: string): Promise<CareersApplicationVO[]> {
  return request.get('/careers/jobs/public/my-applications', { params: { userId, email } })
}

export interface CareersApplicationVO {
  id: number
  jobTitle: string
  departmentName: string
  status: number
  createTime: string
}

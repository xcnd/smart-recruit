import type { CandidateVO, ResumeVO } from '@/types/models'

/**
 * 将后端返回的原始数据标准化为前端 CandidateVO 格式。
 * 处理字段名差异（如 mobile → phone、createTime → createdAt），
 * 并为缺失字段提供默认值。
 */
export function mapCandidateRow(raw: Record<string, unknown>): CandidateVO {
  return {
    id: (raw.id as string | number) || '',
    name: (raw.name as string) || '',
    email: (raw.email as string) || '',
    phone: (raw.phone as string) || (raw.mobile as string) || '',
    jobId: raw.jobId as string | number | undefined,
    jobTitle: (raw.jobTitle as string) || '',
    currentStage: (raw.currentStage as number) ?? 0,
    aiMatchScore: raw.aiMatchScore as number | undefined,
    matchScore: (raw.aiMatchScore as number) || 0,
    skills: (raw.skills as string[]) || [],
    education: (raw.education as number) ?? 0,
    yearsOfExperience: raw.yearsOfExperience as number | undefined,
    experience: raw.yearsOfExperience != null ? `${raw.yearsOfExperience}年` : '',
    currentCompany: (raw.currentCompany as string) || '',
    source: (raw.source as number) ?? 0,
    resumeUrl: (raw.resumeUrl as string) || '',
    tags: (raw.tags as string[]) || [],
    createdAt: (raw.createdAt || raw.createTime || '') as string,
    updatedAt: raw.updatedAt as string | undefined,
    // 新增字段
    gender: raw.gender as number | undefined,
    birthDate: raw.birthDate as string | undefined,
    school: (raw.school as string) || '',
    major: (raw.major as string) || '',
    currentPosition: (raw.currentPosition as string) || '',
    currentSalary: raw.currentSalary as number | undefined,
    expectedSalaryMin: raw.expectedSalaryMin as number | undefined,
    expectedSalaryMax: raw.expectedSalaryMax as number | undefined,
    city: (raw.city as string) || '',
    sourceDetail: (raw.sourceDetail as string) || '',
    remark: (raw.remark as string) || '',
    avatarColor: (raw.avatarColor as string) || '',
    lastActiveTime: raw.lastActiveTime as string | undefined,
    referrerId: raw.referrerId as number | undefined,
    resumes: (raw.resumes as ResumeVO[]) || [],
  }
}

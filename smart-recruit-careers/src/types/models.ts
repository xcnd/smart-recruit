// ==================== Enums (labels referenced by careers types) ====================

/** Position type (JobVO.positionType) */
export const PositionTypeLabels: Record<number, string> = {
  0: '全职',
  1: '兼职',
  2: '实习',
  3: '合同制',
}

/** Experience level (JobVO.experienceLevel) */
export const ExperienceLevelLabels: Record<number, string> = {
  0: '应届生',
  1: '初级',
  2: '中级',
  3: '高级',
  4: '资深',
  5: '首席/专家',
}

/** Education level (JobVO.educationLevel / educationRequired) */
export const EducationLevelLabels: Record<number, string> = {
  0: '高中',
  1: '大专',
  2: '本科',
  3: '硕士',
  4: '博士',
}

/** Job status (JobVO.status) */
export const JobStatusLabels: Record<number, string> = {
  0: '草稿',
  1: '已发布',
  2: '已暂停',
  3: '已关闭',
}

/** Referral record status (ReferralRecordVO.status) */
export const ReferralRecordStatusLabels: Record<number, string> = {
  0: '待处理',
  1: '已联系',
  2: '面试中',
  3: '已入职',
  4: '已拒绝',
  5: '已取消',
}

/** Referral bonus status (ReferralRecordVO.bonusStatus) */
export const ReferralBonusStatusLabels: Record<number, string> = {
  0: '待发放',
  1: '部分发放',
  2: '全额发放',
}

/** Ref program job tag (RefProgramJobVO.tag) */
export const RefProgramJobTagLabels: Record<number, string> = {
  0: '紧急',
  1: '高奖金',
  2: '技术',
  3: '实习',
}

// ==================== Auth Models ====================

export interface UserVO {
  id: string
  username: string
  name?: string
  realName?: string
  avatar?: string
  email: string
  mobile?: string
  phone?: string
  deptId?: string
  departmentId?: string
  departmentName: string
  roleId?: string
  roleIds?: string
  roleName?: string
  gender?: number
  position?: string
  jobLevel?: string
  age?: number
  status: number
  lastLoginAt?: string
  lastLoginTime?: string
  createdAt?: string
  createTime?: string
  permissions: string[]
}

export interface LoginResultVO {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  userInfo: UserVO
}

/** Phone number send verification code DTO */
export interface PhoneSendCodeDTO {
  mobile: string
}

/** Phone number verification code login DTO */
export interface PhoneLoginDTO {
  mobile: string
  verificationCode: string
}

// ==================== Job Models ====================

export interface JobVO {
  id: string
  title: string
  departmentId: string
  departmentName: string
  positionType: number // 0=FULL_TIME, 1=PART_TIME, 2=INTERNSHIP, 3=CONTRACT
  experienceLevel: number // 0=ENTRY, 1=JUNIOR, 2=MIDDLE, 3=SENIOR, 4=STAFF, 5=PRINCIPAL
  educationLevel?: number // 0=HIGH_SCHOOL, 1=ASSOCIATE, 2=BACHELOR, 3=MASTER, 4=DOCTOR
  headCount: number
  filledCount: number
  salaryMin: number
  salaryMax: number
  location: string
  urgency: number
  description: string
  requirements: string
  skills: string[]
  benefits: string[]
  status: number // 0=DRAFT, 1=PUBLISHED, 2=PAUSED, 3=CLOSED
  educationRequired?: number // 0=HIGH_SCHOOL, 1=ASSOCIATE, 2=BACHELOR, 3=MASTER, 4=DOCTOR
  ageMin?: number
  ageMax?: number
  publishTime?: string
  closeTime?: string
  responsibleId?: string
  referralEnabled: boolean
  referralBonus: number
  applicationCount: number
  createdBy: string
  createdAt: string
  updatedAt: string
}

// ==================== Referral Models ====================

export interface ReferralProgramVO {
  id: string
  title: string
  description: string
  bonusAmount: number
  bonusStructure?: Record<string, number | { amount?: number; tier?: string; tierName?: string; description?: string }>
  startDate?: string
  endDate?: string
  eligibleDeptIds?: string[]
  status: number // 0=停用, 1=启用
  createTime: string
  updateTime: string
  createBy?: string
  updateBy?: string
}

export interface BonusRecordVO {
  id: string
  refRecordId: string
  stage: number // 0=入职, 1=入职首月, 2=试用期, 3=转正, 4=全额
  stageName: string
  amount: number
  status: number // 0=待发放, 1=已发放, 2=已取消
  paidTime?: string
}

export interface ReferralRecordVO {
  id: string
  programId: string
  programJobId: string
  referrerId: string
  candidateId: string
  jobPositionId: string
  relationship?: string
  referralNote?: string
  status: number // 0=PENDING, 1=CONTACTED, 2=INTERVIEWING, 3=HIRED, 4=REJECTED, 5=CANCELLED
  bonusStatus?: number // 0=PENDING, 1=PARTIAL_PAID, 2=FULL_PAID
  bonusAmount: number
  bonusPaid: number
  hiredTime?: string
  createTime: string
  updateTime: string
  bonusRecords?: BonusRecordVO[]
  candidateName?: string
  candidateUsername?: string
  jobTitle?: string
  departmentName?: string
  programJobTitle?: string
}

export interface RefProgramJobVO {
  id: string
  programId: string
  jobPositionId: string
  isEnabled: number // 0=禁用, 1=启用
  bonusAmount: number | null // null=使用计划默认奖金
  tag: number | null // 0=URGENT, 1=HIGH_BONUS, 2=TECH, 3=INTERN
  jobTitle?: string
  createTime: string
  updateTime: string
}

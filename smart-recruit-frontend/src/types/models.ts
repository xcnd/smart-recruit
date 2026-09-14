// ==================== System Models ====================
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

export interface UserCreateDTO {
  username: string
  realName: string
  password: string
  email: string
  mobile: string
  deptId: string
  roleIds: string[]
  gender?: number
  position?: string
  jobLevel?: string
  age?: number
}

export interface UserUpdateDTO {
  id: string
  realName?: string
  email?: string
  mobile?: string
  deptId?: string
  roleIds?: string[]
  gender?: number
  position?: string
  jobLevel?: string
  age?: number
}

export interface UpdateProfileDTO {
  realName?: string
  mobile?: string
  gender?: number // 0=未知, 1=男, 2=女
  avatar?: string
}

export interface ChangePasswordDTO {
  oldPassword: string
  newPassword: string
}

export interface ChangeEmailDTO {
  newEmail: string
  verificationCode: string
}

export interface UserDetailVO {
  id: string
  username: string
  realName: string
  email: string
  mobile: string
  avatar?: string
  gender: number
  deptId: string
  departmentName: string
  roleIds: string[]
  roleNames: string[]
  status: number
  lastLoginTime?: string
  lastLoginIp?: string
  remark?: string
  createTime: string
  updateTime: string
}

export interface UserStatsVO {
  total: number
  active: number
  frozen: number
}

export interface RoleStatsVO {
  total: number
  builtin: number
  custom: number
}

export interface RoleVO {
  id: string
  name: string
  code: string
  description: string
  type: number // 0=BUILTIN, 1=CUSTOM (computed: codes with ROLE_ prefix are builtin)
  userCount: number // not returned by list endpoint, populated where available
  status: number // 0=DISABLED, 1=ACTIVE
  sortOrder: number
  createTime: string
  createdAt?: string // legacy alias
  permissions: string[]
}

export interface RoleCreateDTO {
  name: string
  code: string
  description?: string
  type: number // 0=内置, 1=自定义
  permissionIds: string[]
}

export interface DepartmentTreeVO {
  id: string
  name: string
  code: string
  parentId?: string
  leaderId?: string
  leaderName?: string
  sortOrder?: number
  status: number        // 0=停用, 1=启用
  remark?: string
  children: DepartmentTreeVO[]
}

export interface DepartmentCreateDTO {
  name: string
  code: string
  parentId?: string
  leaderId?: string
  remark?: string
}

export interface PermissionVO {
  id: string
  name: string
  code: string
  type: number // 1=MENU, 2=BUTTON, 3=API
  module: number // 0=RECRUIT, 1=JOB, 2=CANDIDATE, 3=INTERVIEW, 4=TALENT, 5=ANALYTICS, 6=OFFER, 7=ONBOARD, 8=REFERRAL, 9=SYSTEM, 10=AI
  parentId: string
  path?: string
  component?: string
  icon?: string
  method?: number
  apiPath?: string
  sortOrder: number
  status?: number
  visible?: number
  createTime?: string
  children: PermissionVO[]
}

export interface PermissionCreateDTO {
  name: string
  code: string
  permType: number
  module: number
  parentId?: string
  path?: string
  component?: string
  icon?: string
  method?: number
  apiPath?: string
  sortOrder?: number
  status?: number
  visible?: number
}

export interface PermissionUpdateDTO {
  name?: string
  code?: string
  permType?: number
  module?: number
  parentId?: string
  path?: string
  component?: string
  icon?: string
  method?: number
  apiPath?: string
  sortOrder?: number
  status?: number
  visible?: number
}

// ==================== System Config Models ====================
export interface SysConfigVO {
  id: string
  configKey: string
  configValue: string
  description: string
  createTime: string
  updateTime: string
  createBy: string
  updateBy: string
}

export interface ConfigUpdateDTO {
  configKey: string
  configValue: string
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

export interface JobCreateDTO {
  title: string
  departmentId: string
  positionLevel: number // 0=P1, 1=P2, 2=P3, 3=P4, 4=P5, 5=P6, 6=P7, 7=P8, 8=P9, 9=P10, 10=M1, 11=M2, 12=M3
  headCount: number
  salaryMin: number
  salaryMax: number
  location: string
  experienceLevel: number // 0=ENTRY, 1=JUNIOR, 2=MIDDLE, 3=SENIOR, 4=STAFF, 5=PRINCIPAL
  positionType: number // 0=FULL_TIME, 1=PART_TIME, 2=INTERNSHIP, 3=CONTRACT
  description: string
  requirements?: string
  skills?: string[]
  educationRequired?: number
  ageMin?: number
  ageMax?: number
}

export interface JobStatsVO {
  total: number
  published: number
  draft: number
  closed: number
}

// ==================== Candidate Models ====================
// Backend returns: 0=NEW, 1=SCREENING, 2=SCREEN_PASSED, 3=INTERVIEWING, 4=OFFERED, 5=HIRED, 6=REJECTED, 7=WITHDRAWN
export interface CandidateVO {
  id: string | number
  name: string
  email: string
  phone: string
  jobId?: string | number
  jobTitle?: string
  currentStage: number // 0=NEW, 1=SCREENING, 2=SCREEN_PASSED, 3=INTERVIEWING, 4=OFFERED, 5=HIRED, 6=REJECTED, 7=WITHDRAWN
  aiMatchScore?: number
  matchScore?: number
  skills: string[]
  education: number // 0=HIGH_SCHOOL, 1=ASSOCIATE, 2=BACHELOR, 3=MASTER, 4=PHD
  yearsOfExperience?: number
  experience?: string
  currentCompany?: string
  source: number // 0=DIRECT, 1=REFERRAL, 2=WEBSITE, 3=LINKEDIN, 4=BOSS, 5=LAGOU, 6=LIEPIN, 7=OTHER
  referredBy?: string
  resumeUrl?: string
  tags?: string[]
  appliedAt?: string
  createdAt: string
  updatedAt?: string
  // Extended fields
  gender?: number // 0=未知, 1=男, 2=女
  birthDate?: string
  avatarColor?: string
  school?: string
  major?: string
  currentPosition?: string
  currentSalary?: number
  expectedSalaryMin?: number
  expectedSalaryMax?: number
  city?: string
  sourceDetail?: string
  lastActiveTime?: string
  remark?: string
  referrerId?: number
  stageHistory?: StageHistoryVO[]
  resumes?: ResumeVO[]
}

export interface StageTransition {
  candidateId: string
  fromStage: number
  toStage: number
  remark?: string
}

export interface StageHistoryVO {
  id: string | number
  fromStage: number
  toStage: number
  operatorName: string
  operatorId: string | number
  remark?: string
  createTime: string
}

export interface CandidateStatsVO {
  totalCount: number
  newCount: number
  screeningCount: number
  screenPassedCount: number
  interviewingCount: number
  offeredCount: number
  hiredCount: number
  rejectedCount: number
  withdrawnCount: number
  sourceDistribution: Record<string, number>
  monthlyTrend: { month: string; count: number }[]
}

export interface CreateCandidateDTO {
  name: string
  email: string
  phone?: string
  gender?: number
  birthDate?: string
  education?: number
  school?: string
  major?: string
  yearsOfExperience?: number
  currentCompany?: string
  currentPosition?: string
  currentSalary?: number
  expectedSalaryMin?: number
  expectedSalaryMax?: number
  city?: string
  skills?: string[]
  source?: number
  sourceDetail?: string
  tags?: string[]
  referrerId?: number
}

export interface UpdateCandidateDTO {
  name?: string
  email?: string
  phone?: string
  gender?: number
  birthDate?: string
  education?: number
  school?: string
  major?: string
  yearsOfExperience?: number
  currentCompany?: string
  currentPosition?: string
  currentSalary?: number
  expectedSalaryMin?: number
  expectedSalaryMax?: number
  city?: string
  skills?: string[]
  source?: number
  sourceDetail?: string
  tags?: string[]
  referrerId?: number
  remark?: string
}

// ==================== Resume Models ====================
export interface ResumeVO {
  id: string
  candidateId: string
  candidateName: string
  avatarUrl?: string
  jobId: string
  jobTitle: string
  fileName: string
  fileUrl: string
  parseStatus: string // PENDING / PARSING / COMPLETED / FAILED
  screeningStatus: number // 0=待处理, 1=已通过, 2=已淘汰
  autoScreen?: boolean // 上传后是否自动执行 AI 筛选
  matchScore: number
  aiResult?: AiAnalysisResultVO
  skills: string[]
  source?: number // 来源渠道：0=主动投递,1=内推,2=官网,3=LinkedIn,4=BOSS直聘,5=拉勾,6=猎聘,7=其他
  referrerId?: string // 推荐人用户 ID（仅 source=1 内推时有值）
  referrerName?: string // 推荐人姓名
  referrerDepartment?: string // 推荐人所在部门
  referrerPosition?: string // 推荐人职位
  createdAt: string
}

export interface AiAnalysisResultVO {
  overallScore: number
  dimensions: {
    name: string
    score: number
    maxScore: number
  }[]
  matchedKeywords: string[]
  missingKeywords: string[]
  strengths: string[]
  weaknesses: string[]
  suggestion: string // STRONG_HIRE / HIRE / CONSIDER / REJECT
  summary: string
}

export interface BatchScreenResult {
  taskId: string
  totalCount: number
}

export interface BatchScreenProgress {
  taskId: string
  total: number
  completed: number
  failed: number
  status: string // PROCESSING / COMPLETED / FAILED
  failedDetails?: string[]
}

export interface ResumeParseStatusVO {
  resumeId: number
  parseStatus: number
  parseError?: string
}

// ==================== Interview Models ====================
export interface InterviewVO {
  id: string
  candidateId: string
  candidateName: string
  jobId: string
  jobTitle: string
  type: number // 3=AI, 4=TECHNICAL, 5=HR, 6=LEADERSHIP
  typeLabel: string // e.g. "AI面试", "技术面试", "HR面试", "领导面试"
  interviewerIds?: string | number[] // JSON array e.g. "[200001]" or [200001]
  interviewerName: string
  round: number // 面试轮次：1, 2, 3...
  isFinalRound?: number // 是否为终面：0=否, 1=是
  scheduledAt: string
  duration: number
  status: number // 0=SCHEDULED, 1=IN_PROGRESS, 2=COMPLETED, 3=CANCELLED
  statusLabel: string // e.g. "已安排", "进行中", "已完成", "已取消"
  result?: number // 0=ADVANCE, 1=REJECT, 2=RETEST
  score?: number
  aiQuestionStatus?: number // AI 出题状态：0=模板/未生成, 1=生成中, 2=生成成功, 3=生成失败
  createdAt: string
}

export interface InterviewStatsVO {
  total: number
  today: number
  passed: number
  cancelled: number
}

export interface InterviewReportVO {
  interviewId: string
  candidateId: string
  candidateName: string
  jobTitle: string
  jobId: string
  type: number
  round: number
  scheduledAt: string
  candidate: {
    gender: number
    age: number
    education: string
    school: string
    major: string
    city: string
    phone: string
    email: string
    currentCompany: string
    currentPosition: string
    yearsOfExperience: number
  } | null
  overallScore: number
  result: number
  suggestion: string
  feedback: string
  dimensions: {
    name: string
    weight: number
    score: number
  }[]
  strengths: string
  weaknesses: string
  hasNextRound: boolean
  evaluationStatus?: string
}

// ==================== Offer Models ====================
export interface OfferVO {
  id: string | number
  applicationId?: number
  candidateId: number
  candidateName: string
  jobPositionId?: number
  offerNo?: string
  positionTitle: string
  departmentName: string
  level?: string
  baseSalary: number
  bonusMonths?: number
  totalPackage: number
  status: number // 0=DRAFT, 1=PENDING, 2=APPROVED, 3=SENT, 4=ACCEPTED, 5=REJECTED
  expectedOnboardDate?: string
  validUntil?: string
  approvedBy?: string
  sentAt?: string
  acceptedAt?: string
  respondTime?: string
  declineReason?: string
  stockOptions?: number
  signOnBonus?: number
  prediction?: {
    acceptanceProbability: number
    riskLevel: number // 0=LOW, 1=MEDIUM, 2=HIGH
    factors: string[]
  }
  approvals?: Array<{
    id: string | number
    approverId: string | number
    approverName: string
    approverRole: string
    approvalLevel: number
    status: number
    comment: string
    approveTime: string
  }>
  createBy?: string
  creatorName?: string
  createTime?: string
  updateTime?: string
}

export interface OfferCreateDTO {
  candidateId: number
  candidateName: string
  candidateEmail?: string
  jobPositionId: number
  jobTitle: string
  departmentId: number
  departmentName: string
  level: string
  baseSalary: number
  bonusMonths: number
  stockOptions?: number
  signOnBonus?: number
  expectedOnboardDate: string
  validUntil: string
}

// ==================== Offer Approval Models ====================
export interface ApprovalListVO {
  offerId: string | number
  offerNo: string
  candidateId: string
  candidateName: string
  positionTitle: string
  departmentName: string
  level: string
  baseSalary: number
  totalPackage: number
  expectedOnboardDate: string
  status: number
  statusLabel: string
  submitTime: string
  latestApproval: ApprovalInfo | null
  /** 当前审批级别（后端根据已有审批记录自动计算） */
  currentLevel?: number
  /** 申请人（创建人姓名） */
  creatorName?: string
  createTime: string
}

export interface ApprovalInfo {
  id: string | number
  approverId: string | number
  approverName: string
  approverRole: string
  approvalLevel: number
  status: number
  statusLabel: string
  comment: string
  approveTime: string
}

export interface ApprovalStatsVO {
  pendingCount: number
  todayApproved: number
  todayRejected: number
  monthlyTotal: number
}

// ==================== Approval Flow Config Models ====================
export interface ApprovalFlowConfigVO {
  id: string | number
  flowName: string
  departmentName: string
  isActive: number
  maxLevels: number
  nodes: FlowNode[]
  description: string
  createTime: string
  updateTime: string
}

export interface FlowNode {
  level: number
  nodeName: string
  approvers: ApproverInfo[]
}

export interface ApproverInfo {
  approverId: string | number
  approverName: string
  approverRole: string
}

export interface CreateFlowConfigRequest {
  flowName: string
  departmentName: string
  nodes: FlowNode[]
  description?: string
}

export interface UpdateFlowConfigRequest {
  flowName?: string
  departmentName?: string
  nodes?: FlowNode[]
  description?: string
}

// ==================== Onboarding Models ====================
export interface OnboardingVO {
  id: string
  offerId: string
  candidateId: string
  employeeName: string
  employeeNo: string
  jobTitle: string
  level?: string
  departmentName: string
  onboardDate: string
  currentStep: number
  totalSteps: number
  status: number // 0=PENDING, 1=ACTIVE, 2=DONE, 3=AT_RISK
  employeeStatus?: number // 0=待入职,1=试用期,2=正式,3=已离职
  employeeStatusLabel?: string
  riskLevel: number // 0=LOW, 1=MEDIUM, 2=HIGH
  mentorId: string
  completedDocumentsCount?: number
  totalDocumentsCount?: number
  createTime: string
}

export interface OnboardingStepVO {
  step: number
  name: string
  status: number // 0=COMPLETED, 1=IN_PROGRESS, 2=PENDING
  documents: {
    name: string
    submitted: boolean
    verified: boolean
  }[]
  completedAt?: string
}

export interface OnboardingDocumentItem {
  id: string
  docType: number       // 0=身份证,1=学历证明,2=离职证明,3=体检报告,4=银行卡,5=证件照
  docName: string
  status: number        // 0=缺失,1=待审核,2=已审核
  statusLabel: string
  filePath?: string
  verifiedBy?: string
  verifiedTime?: string
  remark?: string
}

export interface OnboardingEquipmentItem {
  id: string
  equipmentType: number  // 0=笔记本电脑,1=显示器,2=手机,3=门禁卡,4=工位
  equipmentName: string
  status: number         // 0=待分配,1=已分配,2=已发出,3=已签收
  statusLabel: string
  assetNo?: string
  assignedBy?: string
  assignedByName?: string
  assignedTime?: string
  deliveredTime?: string
  remark?: string
}

export interface OnboardingDetailVO extends OnboardingVO {
  riskScore?: number
  retentionScore?: number
  documentStatuses?: Record<string, string>
  documents?: OnboardingDocumentItem[]
  equipments?: OnboardingEquipmentItem[]
  equipment: EquipmentStatusVO[]
  equipmentStatus?: number
  // 导师/伙伴
  mentorName?: string
  buddyId?: string
  buddyName?: string
  // 欢迎页
  welcomeSent?: number
  // 培训
  trainingProgress?: number
  // 账号
  accountCreated: boolean
  accountUsername?: string
  accountEmail?: string
  // 其他
  checklist?: any
  updateTime?: string
}

export interface EquipmentStatusVO {
  id: string
  type: number
  name: string
  status: number
  assetNo?: string
}

export interface RetentionPredictionVO {
  onboardingId: string
  status?: string  // PENDING=评估中 / COMPLETED=已完成
  retentionScore6M: number
  retentionScore12M: number
  riskLevel: number
  riskFactors: string[]
  interventions: string[]
}

export interface OnboardingStatsVO {
  totalCount: number
  pendingCount: number
  activeCount: number
  doneCount: number
  atRiskCount: number
  lowRiskCount: number
  mediumRiskCount: number
  highRiskCount: number
}

// ==================== Talent Pool Models ====================
export interface TalentPoolVO {
  id: string
  candidateId: string
  candidateName: string
  email: string
  phone: string
  poolType: number // 0=GENERAL, 1=TECH, 2=MANAGEMENT, 3=DESIGN, 4=PRODUCT
  skills: string[]
  education: string
  experience: number
  lastPosition: string
  currentCompany: string
  matchScore: number
  matchDimensions?: Record<string, number>
  tags: string[]
  aiTags: string[]
  skillLevel: number // 0=BASIC, 1=INTERMEDIATE, 2=ADVANCED, 3=EXPERT, 4=MASTER
  availability: number // 0=ACTIVE, 1=PASSIVE, 2=NOT_AVAILABLE
  expectedPosition: string
  expectedLocation: string
  expectedSalaryMin?: number
  expectedSalaryMax?: number
  status: number // 0=AVAILABLE, 1=CONTACTED, 2=ENGAGED
  lastContactAt?: string
  createdAt: string
}

export interface RecommendationTaskVO {
  taskId: string
  status: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED'
  message?: string
  results: TalentPoolVO[]
  total: number
  page: number
  size: number
}

export interface TalentCampaignVO {
  id: string
  campaignName: string
  templateType: number // 0=EMAIL, 1=SMS, 2=PUSH, 3=WECHAT, 4=CUSTOM
  targetAudience: string
  targetCount: number
  messageContent: string
  sendMethod: number // 0=IMMEDIATE, 1=SCHEDULED, 2=DRIP, 3=AUTO
  scheduledTime?: string
  sentTime?: string
  reachCount: number
  responseCount: number
  responseRate?: number
  status: number // 0=DRAFT, 1=SCHEDULED, 2=SENDING, 3=COMPLETED, 4=CANCELLED
  createdAt: string
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

// ==================== Referral Match Models ====================
export interface ReferralMatchVO {
  id: string
  recordId: string
  candidateId: string
  candidateName: string
  matchedJobId: string | number
  matchedJobTitle: string
  matchScore: number
  recommendation: string | null
  suggestedApproach: string | null
  matchDimensions: unknown
  source: string
  rankNo: number
  createTime: string
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

export interface LeaderboardVO {
  referrerId: string
  referrerName?: string
  departmentName?: string
  avatar?: string
  rank: number
  referralCount: number
  totalBonus: number
}


// ==================== Dashboard Models ====================
export interface DashboardKpiVO {
  jobCount: number
  jobCountTrend: string
  todayCandidateCount: number
  yesterdayCandidateCount: number
  applicationCount: number
  applicationCountTrend: string
  pendingInterview: number
  pendingInterviewTrend: string
  pendingOffer: number
  pendingOfferTrend: string
  avgDaysToHire: number
  avgDaysToHireTrend: string
  hiresThisMonth: number
  hiresThisMonthTrend: string
}

export interface FunnelStageVO {
  stage: number
  count: number
  conversionRate: number
  sortOrder: number
  // 前端历史兼容
  label?: string
}

export interface DepartmentProgressVO {
  departmentId: number | string
  departmentName: string
  headcount: number
  inPipeline: number
  interviewing: number
  offered: number
  hired: number
  progressPercentage: number
  // 前端历史兼容
  total?: number
  filled?: number
  percentage?: number
}

export interface RecentActivityVO {
  id: number | string
  type: number
  title: string
  description: string
  candidateName: string
  jobTitle: string
  createdAt: string
  relatedType: string | null
  relatedId: number | string | null
  // 前端历史兼容
  user?: string
  time?: string
  actorName?: string
  typeLabel?: string
}

export interface PendingTaskVO {
  id: number | string
  category: number
  title: string
  description?: string
  priority: number
  relatedPerson: string
  dueDate: string | null
  createdAt: string | null
  relatedType: string | null
  relatedId: number | string | null
  // 前端历史兼容
  type?: number
  candidateName?: string
  scheduledAt?: string
  urgency?: number
}

// ==================== Analytics Models ====================
export interface AnalyticsKpiVO {
  key: string
  label: string
  value: number
  prevValue: number
  changePercent: number | null
  unit: string
}

export interface AnalyticsChannelVO {
  channel: string
  candidateCount: number
  percentage: number
  hireCount: number
  conversionRate: number
  costPerHire: number
  totalCost: number
  roi: number | null
}

export interface TrendPointVO {
  month: string
  candidateCount: number
  onboardCount: number
}

export interface OfferTrendPointVO {
  month: string
  sentCount: number
  acceptedCount: number
  avgConfirmDays: number
}

export interface AnalyticsInsightVO {
  id: number
  title: string
  description: string
  icon: string
  trend: 'UP' | 'DOWN' | 'STABLE'
  value: string
  aiGenerated?: boolean
}

export interface AnalyticsOverviewVO {
  startDate: string
  endDate: string
  kpis: AnalyticsKpiVO[]
  funnel: FunnelStageVO[]
  channels: AnalyticsChannelVO[]
  candidateTrend: TrendPointVO[]
  offerTrend: OfferTrendPointVO[]
  insights: AnalyticsInsightVO[]
}

export interface RecruitmentCycleVO {
  month: string
  cycle: number
  target: number
}

// ==================== Notification Models ====================
export interface NotificationVO {
  id: string
  userId: string
  title: string
  content: string
  type: number // 0=SYSTEM,1=INTERVIEW,2=OFFER,3=ONBOARDING,4=REFERRAL,5=TALENT
  read: number // 0=未读,1=已读
  readTime: string | null
  actionUrl: string | null
  businessType: string | null
  businessId: string | number | null
  createdAt: string
}

// ==================== Agent Models ====================
export interface AgentInfoVO {
  id: string
  name: string
  description?: string
  type: number // 0=ORCHESTRATION, 1=EXECUTION, 2=REVIEW
  status: number // 0=RUNNING, 1=IDLE, 2=PAUSED, 3=ERROR
  health: number
  metrics: {
    tasksCompleted: number
    avgResponseTime: number
    successRate: number
    uptime: number
  }
  config: Record<string, unknown>
  updatedAt: string
}

export interface AgentLogVO {
  id: string
  agentId: string
  agentName: string
  taskType?: number // 0=RESUME_PARSE,1=SCREEN,2=EVALUATE,3=PREDICT,4=RECOMMEND,5=QUESTION_GEN,6=JD_GEN
  status?: number // 0=QUEUED,1=RUNNING,2=COMPLETED,3=FAILED,4=RETRYING
  durationMs?: number
  inputSummary?: string
  outputSummary?: string
  startedAt?: string
  completedAt?: string
}

export interface TaskQueueItemVO {
  id: string
  agentId: string
  agentName: string
  type: string // 人类可读任务类型名：简历解析 / 候选人筛选 / 面试评估 等
  priority: string // HIGH / MEDIUM / LOW
  status: string // QUEUED / RUNNING / COMPLETED / FAILED / RETRYING
  createdAt: string
  startedAt?: string
  durationMs?: number
}

export interface AgentEventVO {
  eventId: string
  agentId: string
  agentName: string
  eventType?: number // 0=INFO, 1=WARNING, 2=ERROR, 3=SUCCESS
  message: string
  status?: number
  type: number // 0=INFO, 1=WARNING, 2=ERROR, 3=SUCCESS
  data: Record<string, unknown>
  timestamp: string
}

// ==================== Pipeline (全流程编排) Models ====================
export interface RunPipelineRequest {
  candidateName: string
  resumeText?: string
  jobTitle: string
  requiredSkills?: string[]
  minYearsOfExperience?: number
  offerTotalPackage?: number
  interviewSummary?: string
}

export interface CandidateProfileVO {
  name?: string
  email?: string
  phone?: string
  educationLevel?: string
  school?: string
  major?: string
  yearsOfExperience?: number
  currentCompany?: string
  currentPosition?: string
  skills?: string[]
  summary?: string
  overallScore?: number
}

export interface PipelineInterviewVO {
  overallScore?: number
  dimensionScores?: Record<string, number>
  communicationAssessment?: string
  technicalAssessment?: string
  behavioralAssessment?: string
  strengths?: string
  weaknesses?: string
  hireRecommendation?: string
}

export interface PipelineStageVO {
  stage: string
  stageName: string
  agentId: string
  agentName: string
  status: 'COMPLETED' | 'SKIPPED' | 'FAILED'
  durationMs?: number
  message: string
}

export interface PipelineResultVO {
  pipelineName: string
  status: 'COMPLETED' | 'FAILED'
  durationMs: number
  candidate?: CandidateProfileVO
  screening?: {
    overallScore?: number
    passed?: boolean
    recommendation?: string
    summary?: string
    dimensionScores?: Record<string, number>
  }
  interviewEvaluation?: PipelineInterviewVO
  offerPrediction?: {
    acceptanceProbability?: number
    riskLevel?: string
    recommendation?: string
    factorInfluences?: Record<string, number>
  }
  stages: PipelineStageVO[]
}

// ==================== Audit Log Models ====================
export interface AuditLogVO {
  id: string
  userId?: string
  username?: string
  module?: number
  moduleLabel?: string
  action?: number
  actionLabel?: string
  targetType?: string
  targetId?: string
  description?: string
  requestMethod?: string
  requestUri?: string
  responseStatus?: number
  clientIp?: string
  durationMs?: number
  errorMsg?: string
  createTime?: string
}

export interface AuditLogDetailVO extends AuditLogVO {
  requestParams?: string
  userAgent?: string
  traceId?: string
}

export interface AuditLogStatsVO {
  total: number
  todayCount: number
  successCount: number
  failCount: number
  successRate: number
  avgDurationMs: number
}

// ==================== Contract Models ====================
export interface ContractVO {
  id: string
  contractNo: string
  offerId?: string
  candidateId?: string
  candidateName: string
  candidateEmail?: string
  jobTitle?: string
  departmentName?: string
  offerNo?: string
  totalPackage?: number
  status: number
  statusLabel?: string
  signedByHr?: string
  signedByCandidate?: string
  voidReason?: string
  signTime?: string
  validFrom?: string
  validUntil?: string
  createBy?: string
  createTime?: string
  remark?: string
}

export interface ContractSignRecordVO {
  id: string
  contractId: string
  signerType?: number
  signerTypeLabel?: string
  signerName: string
  action?: number
  actionLabel?: string
  signTime: string
  signIp?: string
  remark?: string
}

export interface ContractDetailVO extends ContractVO {
  content?: string
  rejectReason?: string
  candidateSignature?: string
  candidateIdCard?: string
  candidatePhone?: string
  candidateAddress?: string
  signRecords: ContractSignRecordVO[]
}

export interface ContractStatsVO {
  total: number
  draftCount: number
  pendingCount: number
  sentCount: number
  signedCount: number
  archivedCount: number
  effectiveCount: number
}

export interface OfferOptionVO {
  id: string
  offerNo: string
  candidateName: string
  positionTitle?: string
  status?: number
  statusLabel?: string
}

// ==================== Auth Models ====================
export interface CaptchaVO {
  captchaId: string
  captchaImage: string
}

export interface LoginDTO {
  account: string
  password: string
  captchaId?: string
  captchaCode?: string
}

export interface SendCodeDTO {
  email: string
  purpose?: string // "register" | "reset"，默认为 "register"
}

/** 手机号发送验证码 DTO */
export interface PhoneSendCodeDTO {
  mobile: string
}

/** 手机号验证码登录 DTO */
export interface PhoneLoginDTO {
  mobile: string
  verificationCode: string
}

export interface RegisterDTO {
  username: string
  email: string
  password: string
  realName: string
  verificationCode: string
  referralCode?: string
}

export interface ResetPasswordDTO {
  email: string
  verificationCode: string
  newPassword: string
}

export interface LoginResultVO {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  userInfo: UserVO
}

export interface TokenRefreshVO {
  token: string
}

// ==================== AI Interview Models ====================
export interface AiStatsVO {
  todayInterviews: number
  completed: number
  aiAssessed: number
  passRate: number
}

export interface InterviewScheduleVO {
  id: string
  timeSlot: string
  startTime: string
  endTime: string
  candidateName: string
  jobTitle: string
  typeLabel: string
  interviewerName: string
  statusTag: string // pending | ongoing | done | cancelled
  statusCode: number // 0=待开始,1=进行中,2=已完成
  scheduledDate?: string // yyyy-MM-dd，用于周视图按日期分组
}

export interface QuestionGenerateTaskVO {
  taskId: string
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'NOT_FOUND'
  result?: QuestionGenerateResult
  errorMessage?: string
}

export interface QuestionGenerateResult {
  positionType: number
  positionLabel: string
  techQuestions: QuestionItem[]
  projectQuestions: QuestionItem[]
  behavioralQuestions: QuestionItem[]
  aiEnhanced: boolean
}

export interface QuestionItem {
  number: number
  question: string
  questionType?: string
  options?: string[]
  difficulty: string
  difficultyCode: number
  referenceAnswer?: string
}

export interface AssessmentResponse {
  id: string
  interviewId: string
  candidateId: string
  candidateName?: string
  jobTitle?: string
  technologyDepth: number
  communication: number
  problemSolving: number
  learningAbility: number
  teamwork: number
  overallScore: number
  overallComment: string
  strengths: string[]
  weaknesses: string[]
  keyMoments: KeyMomentItem[]
  suggestion: number
  suggestionLabel: string
  aiGenerated: boolean
  assessorId: string
  createTime: string
}

export interface KeyMomentItem {
  time: string
  text: string
}

export interface OnlineAssessmentVO {
  id: string
  interviewId?: string
  candidateId: string
  candidateName: string
  jobTitle: string
  type: number // 0=编程测试,1=性格测试,2=智商测试
  typeLabel: string
  sentTime?: string
  status: number // 0=未发送,1=待完成,2=已完成
  statusLabel: string
  score?: string
  candidateEmail?: string
  createTime: string
}

export interface FeedbackRequest {
  candidateId: string | number
  techRating?: number
  commRating?: number
  solveRating?: number
  learnRating?: number
  teamRating?: number
  comments: string
  hireRecommendation: number
  expectedSalaryMin?: number
  expectedSalaryMax?: number
  availableDate?: string
  specialRequirements?: string
}

// ==================== Public Assessment (Candidate-facing) ====================

export interface AssessmentPageVO {
  assessmentId: number
  candidateId: number
  candidateName: string
  jobTitle: string
  type: number
  typeLabel: string
  status: number
  candidateEmail: string
  sentTime: string
  durationMinutes: number
  questions: AssessmentQuestionItem[]
}

export interface AssessmentQuestionItem {
  questionId: number
  type: number
  questionText: string
  difficulty?: string
  questionType: 'single_choice' | 'multiple_choice' | 'true_false' | 'likert' | 'essay'
  options?: { key: string; value: string }[]
  correctAnswer?: string
  score: number
}

export interface SubmitAnswersRequest {
  token: string
  answers: { questionId: number; selectedAnswer: string }[]
}

export interface AssessedCandidateVO {
  interviewId: number
  candidateId: number
  departmentId: number | null
  candidateName: string
  jobTitle: string
  overallScore: number
  assessedAt: string
}

export interface AssessResultVO {
  assessmentId: number
  candidateName: string
  jobTitle: string
  type: number
  typeLabel: string
  score: string
  totalQuestions: number
  correctCount?: number
  resultDescription: string
  status: number
}

// ==================== Careers Models ====================
export interface CareersJobTag {
  text: string
  cls: string
}

export interface CareersJobVO {
  id?: number
  recType: string
  title: string
  dept: string
  location: string
  exp: string
  salary: string
  category: string
  tags: CareersJobTag[]
  responsibilities?: string[]
  requirements?: string[]
  bonus?: string[]
  sortOrder: number
  status: number
  createTime?: string
  updateTime?: string
}

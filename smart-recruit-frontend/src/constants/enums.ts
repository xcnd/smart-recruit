// ==================== System Enums ====================

export const PermissionModule = {
  RECRUIT: 0,
  JOB: 1,
  CANDIDATE: 2,
  INTERVIEW: 3,
  TALENT: 4,
  ANALYTICS: 5,
  OFFER: 6,
  ONBOARD: 7,
  REFERRAL: 8,
  SYSTEM: 9,
  AI: 10,
} as const;

export const PermissionModuleLabels: Record<number, string> = {
  0: '招聘',
  1: '职位',
  2: '候选人',
  3: '面试',
  4: '人才库',
  5: '数据分析',
  6: 'Offer',
  7: '入职',
  8: '内推',
  9: '系统管理',
  10: 'AI引擎',
};

export const ApiMethod = {
  GET: 0,
  POST: 1,
  PUT: 2,
  DELETE: 3,
} as const;

export const ApiMethodLabels: Record<number, string> = {
  0: 'GET',
  1: 'POST',
  2: 'PUT',
  3: 'DELETE',
};

export const NotificationType = {
  SYSTEM: 0,
  INTERVIEW: 1,
  OFFER: 2,
  REFERRAL: 3,
  TASK: 4,
  ALERT: 5,
} as const;

export const NotificationTypeLabels: Record<number, string> = {
  0: '系统通知',
  1: '面试通知',
  2: 'Offer通知',
  3: '内推通知',
  4: '任务提醒',
  5: '预警通知',
};

export const OperationLogModule = {
  USER: 0,
  ROLE: 1,
  DEPARTMENT: 2,
  JOB: 3,
  CANDIDATE: 4,
  INTERVIEW: 5,
  OFFER: 6,
  TALENT: 7,
  SYSTEM: 8,
} as const;

export const OperationLogModuleLabels: Record<number, string> = {
  0: '用户管理',
  1: '角色管理',
  2: '部门管理',
  3: '职位管理',
  4: '候选人管理',
  5: '面试管理',
  6: 'Offer管理',
  7: '人才库管理',
  8: '系统管理',
};

export const OperationLogAction = {
  CREATE: 0,
  UPDATE: 1,
  DELETE: 2,
  EXPORT: 3,
  APPROVE: 4,
} as const;

export const OperationLogActionLabels: Record<number, string> = {
  0: '创建',
  1: '修改',
  2: '删除',
  3: '导出',
  4: '审批',
};

// ==================== Recruitment Enums ====================

export const PositionLevel = {
  P1: 0,
  P2: 1,
  P3: 2,
  P4: 3,
  P5: 4,
  P6: 5,
  P7: 6,
  P8: 7,
  P9: 8,
  P10: 9,
  M1: 10,
  M2: 11,
  M3: 12,
} as const;

export const PositionLevelLabels: Record<number, string> = {
  0: 'P1',
  1: 'P2',
  2: 'P3',
  3: 'P4',
  4: 'P5',
  5: 'P6',
  6: 'P7',
  7: 'P8',
  8: 'P9',
  9: 'P10',
  10: 'M1',
  11: 'M2',
  12: 'M3',
};

export const PositionType = {
  FULL_TIME: 0,
  PART_TIME: 1,
  INTERNSHIP: 2,
  CONTRACT: 3,
} as const;

export const PositionTypeLabels: Record<number, string> = {
  0: '全职',
  1: '兼职',
  2: '实习',
  3: '合同制',
};

export const ExperienceLevel = {
  ENTRY: 0,
  JUNIOR: 1,
  MIDDLE: 2,
  SENIOR: 3,
  STAFF: 4,
  PRINCIPAL: 5,
} as const;

export const ExperienceLevelLabels: Record<number, string> = {
  0: '应届生',
  1: '初级',
  2: '中级',
  3: '高级',
  4: '资深',
  5: '首席',
};

export const EducationLevel = {
  HIGH_SCHOOL: 0,
  ASSOCIATE: 1,
  BACHELOR: 2,
  MASTER: 3,
  DOCTOR: 4,
} as const;

export const EducationLevelLabels: Record<number, string> = {
  0: '高中',
  1: '大专',
  2: '本科',
  3: '硕士',
  4: '博士',
};

export const JobStatus = {
  DRAFT: 0,
  PUBLISHED: 1,
  CLOSED: 2,
  CANCELLED: 3,
} as const;

export const JobStatusLabels: Record<number, string> = {
  0: '草稿',
  1: '已发布',
  2: '已关闭',
  3: '已取消',
};

export const ChannelCode = {
  ZHAOPIN: 0,
  JOB51: 1,
  LIEPIN: 2,
  BOSS: 3,
  LINKEDIN: 4,
  INTERNAL: 5,
} as const;

export const ChannelCodeLabels: Record<number, string> = {
  0: '智联招聘',
  1: '前程无忧',
  2: '猎聘',
  3: 'BOSS直聘',
  4: 'LinkedIn',
  5: '内部推荐',
};

export const CandidateSource = {
  DIRECT: 0,
  REFERRAL: 1,
  PORTAL: 2,
  HEADHUNTER: 3,
  CAMPUS: 4,
  SOCIAL: 5,
  WEBSITE: 6,
  OTHER: 7,
} as const;

export const CandidateSourceLabels: Record<number, string> = {
  0: '主动投递',
  1: '员工内推',
  2: '招聘门户',
  3: '猎头推荐',
  4: '校园招聘',
  5: '社会招聘',
  6: '官网投递',
  7: '其他渠道',
};

export const CandidateStatus = {
  NEW: 0,
  SCREENING: 1,
  INTERVIEW: 2,
  OFFER: 3,
  HIRED: 4,
  REJECTED: 5,
  WITHDRAWN: 6,
  BLACKLISTED: 7,
} as const;

export const CandidateStatusLabels: Record<number, string> = {
  0: '新简历',
  1: '筛选中',
  2: '面试中',
  3: 'Offer中',
  4: '已入职',
  5: '已淘汰',
  6: '已撤回',
  7: '黑名单',
};

export const FileType = {
  PDF: 0,
  DOC: 1,
  DOCX: 2,
  TXT: 3,
} as const;

export const FileTypeLabels: Record<number, string> = {
  0: 'PDF',
  1: 'Word',
  2: 'Word(DOCX)',
  3: '文本',
};

export const ParseStatus = {
  PENDING: 0,
  PARSING: 1,
  COMPLETED: 2,
  FAILED: 3,
} as const;

export const ParseStatusLabels: Record<number, string> = {
  0: '待解析',
  1: '解析中',
  2: '解析完成',
  3: '解析失败',
};

export const SourceChannel = {
  DIRECT: 0,
  REFERRAL: 1,
  PORTAL: 2,
  HEADHUNTER: 3,
  CAMPUS: 4,
  SOCIAL: 5,
  WEBSITE: 6,
  OTHER: 7,
} as const;

export const SourceChannelLabels: Record<number, string> = {
  0: '主动投递',
  1: '员工内推',
  2: '招聘门户',
  3: '猎头推荐',
  4: '校园招聘',
  5: '社会招聘',
  6: '官网投递',
  7: '其他渠道',
};

export const ApplicationStatus = {
  NEW: 0,
  SCREENING: 1,
  RESUME_PASSED: 2,
  INTERVIEWING: 3,
  INTERVIEW_PASSED: 4,
  OFFERING: 5,
  OFFER_ACCEPTED: 6,
  HIRED: 7,
  ONBOARDING: 8,
  REJECTED: 9,
  WITHDRAWN: 10,
  BLACKLISTED: 11,
  RESERVE: 12,
} as const;

export const ApplicationStatusLabels: Record<number, string> = {
  0: '新建',
  1: '筛选中',
  2: '简历通过',
  3: '面试中',
  4: '面试通过',
  5: 'Offer中',
  6: 'Offer接受',
  7: '已入职',
  8: '入职中',
  9: '已淘汰',
  10: '已撤回',
  11: '黑名单',
  12: '储备',
};

export const ApplicationStage = {
  RESUME: 0,
  SCREENING: 1,
  INTERVIEW: 2,
  OFFER: 3,
} as const;

export const ApplicationStageLabels: Record<number, string> = {
  0: '简历筛选',
  1: '初筛',
  2: '面试',
  3: 'Offer',
};

export const CommunicationType = {
  PHONE: 0,
  EMAIL: 1,
  SMS: 2,
  WECHAT: 3,
  VIDEO: 4,
  ONSITE: 5,
  ONLINE: 6,
  OTHER: 7,
} as const;

export const CommunicationTypeLabels: Record<number, string> = {
  0: '电话',
  1: '邮件',
  2: '短信',
  3: '微信',
  4: '视频',
  5: '现场',
  6: '线上',
  7: '其他',
};

export const ActivityType = {
  APPLICATION: 0,
  INTERVIEW: 1,
  OFFER: 2,
  ONBOARDING: 3,
  REFERRAL: 4,
  COMMUNICATION: 5,
  TASK: 6,
} as const;

export const ActivityTypeLabels: Record<number, string> = {
  0: '投递',
  1: '面试',
  2: 'Offer',
  3: '入职',
  4: '内推',
  5: '沟通',
  6: '任务',
};

// ==================== Interview Enums ====================

export const InterviewType = {
  PHONE: 0,
  VIDEO: 1,
  ONSITE: 2,
  AI: 3,
  TECHNICAL: 4,
  HR: 5,
  LEADERSHIP: 6,
} as const;

export const InterviewTypeLabels: Record<number, string> = {
  0: '电话面试',
  1: '视频面试',
  2: '现场面试',
  3: 'AI面试',
  4: '技术面试',
  5: 'HR面试',
  6: '领导面试',
};

export const InterviewStatus = {
  SCHEDULED: 0,
  IN_PROGRESS: 1,
  COMPLETED: 2,
  CANCELLED: 3,
  RESCHEDULED: 4,
  NO_SHOW: 5,
} as const;

export const InterviewStatusLabels: Record<number, string> = {
  0: '已安排',
  1: '进行中',
  2: '已完成',
  3: '已取消',
  4: '已改期',
  5: '爽约',
};

export const InterviewResult = {
  ADVANCE: 0,
  REJECT: 1,
  RETEST: 2,
} as const;

export const InterviewResultLabels: Record<number, string> = {
  0: '通过',
  1: '淘汰',
  2: '复试',
};

export const QuestionPositionType = {
  SINGLE: 0,
  MULTIPLE: 1,
  JUDGMENT: 2,
  SHORT_ANSWER: 3,
  ESSAY: 4,
  CODING: 5,
  ORAL: 6,
  SCENARIO: 7,
} as const;

export const QuestionPositionTypeLabels: Record<number, string> = {
  0: '单选题',
  1: '多选题',
  2: '判断题',
  3: '简答题',
  4: '论述题',
  5: '编程题',
  6: '口答题',
  7: '情景题',
};

export const QuestionCategory = {
  TECHNICAL: 0,
  BEHAVIORAL: 1,
  LOGICAL: 2,
  CULTURAL: 3,
} as const;

export const QuestionCategoryLabels: Record<number, string> = {
  0: '技术能力',
  1: '行为面试',
  2: '逻辑思维',
  3: '文化匹配',
};

export const QuestionDifficulty = {
  EASY: 0,
  MEDIUM: 1,
  HARD: 2,
} as const;

export const QuestionDifficultyLabels: Record<number, string> = {
  0: '简单',
  1: '中等',
  2: '困难',
};

export const AssessmentSuggestion = {
  STRONG_HIRE: 0,
  HIRE: 1,
  CONSIDER: 2,
  REJECT: 3,
  REEVALUATE: 4,
} as const;

export const AssessmentSuggestionLabels: Record<number, string> = {
  0: '强烈推荐',
  1: '推荐录用',
  2: '考虑录用',
  3: '不建议录用',
  4: '重新评估',
};

export const HireRecommendation = {
  STRONG_HIRE: 0,
  HIRE: 1,
  CONSIDER: 2,
  REJECT: 3,
} as const;

export const HireRecommendationLabels: Record<number, string> = {
  0: '强烈推荐',
  1: '推荐录用',
  2: '考虑录用',
  3: '不建议录用',
};

// ==================== Offer Enums ====================

export const OfferStatus = {
  DRAFT: 0,
  PENDING: 1,
  APPROVED: 2,
  SENT: 3,
  ACCEPTED: 4,
  REJECTED: 5,
  EXPIRED: 6,
  REVOKED: 7,
} as const;

export const OfferStatusLabels: Record<number, string> = {
  0: '草稿',
  1: '待审批',
  2: '已审批',
  3: '已发送',
  4: '已接受',
  5: '已拒绝',
  6: '已过期',
  7: '已撤销',
};

export const ApprovalStatus = {
  PENDING: 0,
  APPROVED: 1,
  REJECTED: 2,
} as const;

export const ApprovalStatusLabels: Record<number, string> = {
  0: '待审批',
  1: '已通过',
  2: '已拒绝',
};

export const OnboardingStatus = {
  IN_PROGRESS: 0,
  COMPLETED: 1,
  SUSPENDED: 2,
  TERMINATED: 3,
} as const;

export const OnboardingStatusLabels: Record<number, string> = {
  0: '进行中',
  1: '已完成',
  2: '已暂停',
  3: '已终止',
};

export const RiskLevel = {
  LOW: 0,
  MEDIUM: 1,
  HIGH: 2,
} as const;

export const RiskLevelLabels: Record<number, string> = {
  0: '低风险',
  1: '中风险',
  2: '高风险',
};

export const DocumentType = {
  ID_CARD: 0,
  DEGREE: 1,
  CERTIFICATE: 2,
  OFFER_LETTER: 3,
  CONTRACT: 4,
  OTHER: 5,
} as const;

export const DocumentTypeLabels: Record<number, string> = {
  0: '身份证',
  1: '学历学位证',
  2: '资格证书',
  3: '录用通知书',
  4: '劳动合同',
  5: '其他',
};

export const DocumentStatus = {
  SUBMITTED: 0,
  VERIFIED: 1,
  REJECTED: 2,
} as const;

export const DocumentStatusLabels: Record<number, string> = {
  0: '已提交',
  1: '已验证',
  2: '已退回',
};

export const EquipmentType = {
  LAPTOP: 0,
  MONITOR: 1,
  PHONE_DEVICE: 2,
  ACCESSORY: 3,
  SOFTWARE: 4,
} as const;

export const EquipmentTypeLabels: Record<number, string> = {
  0: '笔记本电脑',
  1: '显示器',
  2: '手机设备',
  3: '配件',
  4: '软件许可',
};

export const EquipmentStatus = {
  PENDING: 0,
  ALLOCATED: 1,
  COLLECTED: 2,
  RETURNED: 3,
} as const;

export const EquipmentStatusLabels: Record<number, string> = {
  0: '待分配',
  1: '已分配',
  2: '已领取',
  3: '已归还',
};

// ==================== Talent Enums ====================

export const PoolType = {
  GENERAL: 0,
  TECH: 1,
  MANAGEMENT: 2,
  DESIGN: 3,
  PRODUCT: 4,
} as const;

export const PoolTypeLabels: Record<number, string> = {
  0: '通用人才库',
  1: '技术人才库',
  2: '管理人才库',
  3: '设计人才库',
  4: '产品人才库',
};

export const SkillLevel = {
  BASIC: 0,
  INTERMEDIATE: 1,
  ADVANCED: 2,
  EXPERT: 3,
  MASTER: 4,
} as const;

export const SkillLevelLabels: Record<number, string> = {
  0: '基础',
  1: '熟练',
  2: '高级',
  3: '专家',
  4: '大师',
};

export const Availability = {
  ACTIVE: 0,
  PASSIVE: 1,
  NOT_AVAILABLE: 2,
} as const;

export const AvailabilityLabels: Record<number, string> = {
  0: '活跃求职',
  1: '被动观望',
  2: '暂不考虑',
};

export const TemplateType = {
  EMAIL: 0,
  SMS: 1,
  PUSH: 2,
  WECHAT: 3,
  CUSTOM: 4,
} as const;

export const TemplateTypeLabels: Record<number, string> = {
  0: '邮件模板',
  1: '短信模板',
  2: '推送模板',
  3: '微信模板',
  4: '自定义模板',
};

export const SendMethod = {
  IMMEDIATE: 0,
  SCHEDULED: 1,
  DRIP: 2,
  AUTO: 3,
} as const;

export const SendMethodLabels: Record<number, string> = {
  0: '立即发送',
  1: '定时发送',
  2: '分批发送',
  3: '自动发送',
};

export const CampaignStatus = {
  DRAFT: 0,
  SCHEDULED: 1,
  SENDING: 2,
  COMPLETED: 3,
  CANCELLED: 4,
} as const;

export const CampaignStatusLabels: Record<number, string> = {
  0: '草稿',
  1: '已排期',
  2: '发送中',
  3: '已完成',
  4: '已取消',
};

// ==================== Referral Enums ====================

export const ReferralStatus = {
  PENDING: 0,
  CONTACTED: 1,
  INTERVIEWING: 2,
  HIRED: 3,
  REJECTED: 4,
  CANCELLED: 5,
} as const;

export const ReferralStatusLabels: Record<number, string> = {
  0: '待处理',
  1: '已联系',
  2: '面试中',
  3: '已入职',
  4: '已淘汰',
  5: '已取消',
};

export const BonusStatus = {
  PENDING: 0,
  PARTIAL_PAID: 1,
  FULL_PAID: 2,
} as const;

export const BonusStatusLabels: Record<number, string> = {
  0: '未发放',
  1: '部分发放',
  2: '全额发放',
};

export const ProgramJobTag = {
  URGENT: 0,
  HIGH_BONUS: 1,
  TECH: 2,
  INTERN: 3,
} as const;

export const ProgramJobTagLabels: Record<number, string> = {
  0: '急聘',
  1: '高奖金',
  2: '技术',
  3: '实习',
};

// ==================== AI Enums ====================

export const TaskType = {
  RESUME_PARSE: 0,
  CANDIDATE_SCREEN: 1,
  INTERVIEW_SCHEDULE: 2,
  ASSESSMENT: 3,
  OFFER_PREDICT: 4,
  CAMPAIGN_SEND: 5,
  REPORT_GENERATE: 6,
} as const;

export const TaskTypeLabels: Record<number, string> = {
  0: '简历解析',
  1: '简历筛选',
  2: '面试安排',
  3: '智能评估',
  4: 'Offer预测',
  5: '活动发送',
  6: '报告生成',
};

export const Priority = {
  HIGH: 0,
  MEDIUM: 1,
  LOW: 2,
} as const;

export const PriorityLabels: Record<number, string> = {
  0: '高',
  1: '中',
  2: '低',
};

export const TaskStatus = {
  QUEUED: 0,
  PROCESSING: 1,
  COMPLETED: 2,
  FAILED: 3,
  CANCELLED: 4,
} as const;

export const TaskStatusLabels: Record<number, string> = {
  0: '排队中',
  1: '处理中',
  2: '已完成',
  3: '失败',
  4: '已取消',
};

export const EventType = {
  TASK_STARTED: 0,
  TASK_COMPLETED: 1,
  TASK_FAILED: 2,
  TASK_CANCELLED: 3,
} as const;

export const EventTypeLabels: Record<number, string> = {
  0: '任务开始',
  1: '任务完成',
  2: '任务失败',
  3: '任务取消',
};

export const EventSource = {
  RESUME_PARSER: 0,
  CANDIDATE_SCREENER: 1,
  INTERVIEW_SCHEDULER: 2,
  ASSESSMENT_ENGINE: 3,
  OFFER_PREDICTOR: 4,
  CAMPAIGN_ENGINE: 5,
} as const;

export const EventSourceLabels: Record<number, string> = {
  0: '简历解析引擎',
  1: '候选人筛选引擎',
  2: '面试安排引擎',
  3: '评估引擎',
  4: 'Offer预测引擎',
  5: '活动引擎',
};

export const ParseLogStatus = {
  SUCCESS: 0,
  FAILED: 1,
} as const;

export const ParseLogStatusLabels: Record<number, string> = {
  0: '成功',
  1: '失败',
};

export const AssessmentType = {
  RESUME: 0,
  TECHNICAL: 1,
  BEHAVIORAL: 2,
  COMPREHENSIVE: 3,
} as const;

export const AssessmentTypeLabels: Record<number, string> = {
  0: '简历评估',
  1: '技术评估',
  2: '行为评估',
  3: '综合评估',
};

export const InterviewLogStatus = {
  SUCCESS: 0,
  FAILED: 1,
} as const;

export const InterviewLogStatusLabels: Record<number, string> = {
  0: '成功',
  1: '失败',
};

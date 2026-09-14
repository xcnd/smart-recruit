export function formatDate(dateStr: string): string {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

export function formatDateTime(dateStr: string): string {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

export function formatMoney(amount: number | null | undefined): string {
  if (amount == null) return '0'
  if (amount === 0) return '0'
  if (amount >= 10000) {
    return `${(amount / 10000).toFixed(1)}万`
  }
  return amount.toLocaleString('zh-CN')
}

export function formatPercent(value: number, decimals = 1): string {
  return `${(value * 100).toFixed(decimals)}%`
}

export function maskPhone(phone: string): string {
  if (!phone || phone.length < 11) return phone
  return phone.slice(0, 3) + '****' + phone.slice(7)
}

export function maskEmail(email: string): string {
  if (!email || !email.includes('@')) return email
  const [name, domain] = email.split('@')
  if (name.length <= 2) return `${name[0]}***@${domain}`
  return `${name.slice(0, 2)}***@${domain}`
}

function pad(n: number): string {
  return n < 10 ? `0${n}` : `${n}`
}

export function getStatusLabel(status: number | string): string {
  // Numeric user status (backend returns Integer)
  const numMap: Record<number, string> = {
    0: '冻结',
    1: '正常',
  }
  if (typeof status === 'number') {
    return numMap[status] ?? String(status)
  }
  const map: Record<string, string> = {
    ACTIVE: '正常',
    DISABLED: '冻结',
    FROZEN: '冻结',
    PUBLISHED: '已发布',
    DRAFT: '草稿',
    CLOSED: '已关闭',
    NEW: '新候选人',
    SCREENING: '初筛中',
    INTERVIEW: '面试中',
    OFFER: 'Offer阶段',
    HIRED: '已入职',
    REJECTED: '已拒绝',
    SCHEDULED: '已安排',
    IN_PROGRESS: '进行中',
    COMPLETED: '已完成',
    CANCELLED: '已取消',
    PENDING: '待审批',
    APPROVED: '已审批',
    SENT: '已发送',
    ACCEPTED: '已接受',
    SUSPENDED: '已暂停',
    BUILTIN: '内置',
    CUSTOM: '自定义',
    RUNNING: '运行中',
    IDLE: '空闲',
    PAUSED: '已暂停',
    ERROR: '异常',
  }
  return map[status] || String(status)
}

/** 返回Offer状态的中文标签 */
export function getOfferStatusLabel(status: number | string): string {
  if (typeof status === 'number') {
    const numMap: Record<number, string> = {
      0: '草稿',
      1: '待审批',
      2: '已审批',
      3: '已发送',
      4: '已接受',
      5: '已拒绝',
      6: '洽谈中',
      7: '已过期',
    }
    return numMap[status] ?? String(status)
  }
  const map: Record<string, string> = {
    DRAFT: '草稿',
    PENDING: '待审批',
    APPROVED: '已审批',
    SENT: '已发送',
    ACCEPTED: '已接受',
    REJECTED: '已拒绝',
    NEGOTIATING: '洽谈中',
    EXPIRED: '已过期',
  }
  return map[status] || String(status)
}

/** 返回状态对应的 CSS class 名称（用于 status-dot） */
export function getStatusClass(status: number | string): string {
  if (typeof status === 'number') {
    return status === 1 ? 'active' : 'disabled'
  }
  // For string status, lowercase it
  const lowered = status.toLowerCase()
  if (['active', 'published', 'hired', 'completed', 'accepted', 'running'].includes(lowered)) return 'active'
  if (['frozen', 'pending', 'suspended', 'paused'].includes(lowered)) return 'frozen'
  return 'disabled'
}

export function getStatusType(status: number | string): 'success' | 'warning' | 'danger' | 'info' | '' {
  if (typeof status === 'number') {
    const numTypeMap: Record<number, 'success' | 'warning' | 'danger' | 'info' | ''> = {
      0: 'danger',
      1: 'success',
      2: 'warning',
    }
    return numTypeMap[status] || 'info'
  }
  const map: Record<string, 'success' | 'warning' | 'danger' | 'info' | ''> = {
    ACTIVE: 'success',
    DISABLED: 'danger',
    FROZEN: 'warning',
    PUBLISHED: 'success',
    DRAFT: 'info',
    CLOSED: 'info',
    HIRED: 'success',
    REJECTED: 'danger',
    COMPLETED: 'success',
    ACCEPTED: 'success',
    APPROVED: 'info',
    PENDING: 'warning',
    SCHEDULED: 'info',
    IN_PROGRESS: 'info',
    SUSPENDED: 'warning',
    RUNNING: 'success',
    IDLE: 'info',
    PAUSED: 'warning',
    ERROR: 'danger',
  }
  return map[status] || 'info'
}

/** 性别码 -> 显示标签：0=未知, 1=男, 2=女 */
export function getGenderLabel(gender: number): string {
  const map: Record<number, string> = { 0: '未知', 1: '男', 2: '女' }
  return map[gender] ?? '-'
}

export function isBuiltinRole(typeOrCode?: number | string): boolean {
  if (typeOrCode == null) return false
  if (typeof typeOrCode === 'number') {
    return typeOrCode === 0
  }
  // Fallback: check code prefix for backward compatibility
  return typeOrCode.startsWith('ROLE_')
}

export function getRoleTypeLabel(typeOrCode: number | string): string {
  return isBuiltinRole(typeOrCode) ? '内置' : '自定义'
}

export function getExperienceLabel(exp: number | string): string {
  if (typeof exp === 'number') {
    const numMap: Record<number, string> = {
      0: '应届生',
      1: '1-3年',
      2: '3-5年',
      3: '5-10年',
      4: '10年以上',
      5: '15年以上',
    }
    return numMap[exp] ?? String(exp)
  }
  const map: Record<string, string> = {
    FRESH: '应届生',
    JUNIOR: '1-3年',
    MIDDLE: '3-5年',
    SENIOR: '5-10年',
    EXPERT: '10年以上',
  }
  return map[exp] || String(exp)
}

export function getJobTypeLabel(type: number | string): string {
  if (typeof type === 'number') {
    const numMap: Record<number, string> = {
      0: '全职',
      1: '兼职',
      2: '实习',
      3: '合同制',
    }
    return numMap[type] ?? String(type)
  }
  const map: Record<string, string> = {
    FULL_TIME: '全职',
    INTERNSHIP: '实习',
    CONTRACT: '合同制',
    PART_TIME: '兼职',
  }
  return map[type] || String(type)
}

const DEPT_MAP: Record<number, string> = {
  100001: '公司总部',
  100002: '技术研发部',
  100003: '产品部',
  100004: '人力资源部',
  100005: '财务部',
  100006: '市场部',
  100007: '销售部',
  100008: '研发组',
  100009: '测试组',
  100010: '运维组',
}

export function getDepartmentLabel(deptId: number | string): string {
  const id = typeof deptId === 'string' ? parseInt(deptId, 10) : deptId
  return DEPT_MAP[id] || String(deptId)
}

// ==================== Candidate Enums ====================

/** 候选人阶段码 -> 显示标签：0=新入库,1=筛选中,2=筛选通过,3=面试中,4=Offer,5=已入职,6=已淘汰,7=已放弃 */
export function getCandidateStageLabel(code: number): string {
  const map: Record<number, string> = {
    0: '新入库',
    1: '筛选中',
    2: '筛选通过',
    3: '面试中',
    4: '已发Offer',
    5: '已入职',
    6: '已淘汰',
    7: '已放弃',
  }
  return map[code] ?? String(code)
}

/** 候选人阶段码 -> 看板阶段字符串 */
export function getKanbanStage(code: number): string {
  const map: Record<number, string> = {
    0: 'NEW',
    1: 'SCREENING',
    2: 'SCREENING',
    3: 'INTERVIEW',
    4: 'OFFER',
    5: 'HIRED',
    6: 'REJECTED',
    7: 'WITHDRAWN',
  }
  return map[code] ?? 'NEW'
}

/** 看板阶段字符串 -> 候选人阶段码 */
export function kanbanStageToCode(stage: string): number {
  const map: Record<string, number> = {
    'NEW': 0,
    'SCREENING': 1,
    'INTERVIEW': 3,
    'OFFER': 4,
    'HIRED': 5,
    'REJECTED': 6,
  }
  return map[stage] ?? 0
}

/** 候选人阶段码 -> tag type */
export function getCandidateStageType(code: number): 'success' | 'warning' | 'danger' | 'info' | '' {
  const map: Record<number, 'success' | 'warning' | 'danger' | 'info' | ''> = {
    0: 'info',
    1: 'warning',
    2: 'success',
    3: 'info',
    4: 'warning',
    5: 'success',
    6: 'danger',
    7: 'info',
  }
  return map[code] || 'info'
}

/** 来源渠道码 -> 显示标签 */
export function getSourceLabel(code: number): string {
  const map: Record<number, string> = {
    0: '主动投递',
    1: '内推',
    2: '官网',
    3: 'LinkedIn',
    4: 'BOSS直聘',
    5: '拉勾',
    6: '猎聘',
    7: '其他',
  }
  return map[code] ?? String(code)
}

/** 来源渠道码 -> el-tag type（颜色） */
export function getSourceTagType(code: number): 'primary' | 'success' | 'warning' | 'danger' | 'info' {
  const map: Record<number, 'primary' | 'success' | 'warning' | 'danger' | 'info'> = {
    0: 'primary',   // 主动投递 - 蓝色
    1: 'success',   // 内推 - 绿色
    2: 'info',      // 官网 - 灰色
    3: 'primary',   // LinkedIn - 蓝色
    4: 'warning',   // BOSS直聘 - 橙色
    5: 'success',   // 拉勾 - 绿色
    6: 'danger',    // 猎聘 - 红色
    7: 'info',      // 其他 - 灰色
  }
  return map[code] ?? 'info'
}

/** 学历码 -> 显示标签：0=高中,1=大专,2=本科,3=硕士,4=博士 */
export function getEducationLabel(code: number): string {
  const map: Record<number, string> = {
    0: '高中',
    1: '大专',
    2: '本科',
    3: '硕士',
    4: '博士',
  }
  return map[code] ?? String(code)
}

/** 工作年限 -> 显示标签 */
export function getExperienceYearsLabel(years: number): string {
  if (years == null || years < 0) return '-'
  if (years === 0) return '应届生'
  if (years < 3) return `${years}年`
  if (years < 5) return '3-5年'
  if (years < 10) return '5-10年'
  return `${years}年`
}

// ==================== Onboarding Enums ====================

/** 入职状态码 -> 标签：0=待入职,1=入职中,2=已入职,3=有风险 */
export function getOnboardingStatusLabel(status: number): string {
  const map: Record<number, string> = {
    0: '待入职',
    1: '入职中',
    2: '已入职',
    3: '有风险',
  }
  return map[status] ?? String(status)
}

/** 入职状态码 -> tag type */
export function getOnboardingStatusType(status: number): 'success' | 'warning' | 'danger' | 'info' | '' {
  const map: Record<number, 'success' | 'warning' | 'danger' | 'info' | ''> = {
    0: 'info',
    1: 'warning',
    2: 'success',
    3: 'danger',
  }
  return map[status] || 'info'
}

/** 员工状态中文名：0=待入职,1=试用期,2=正式,3=已离职。 */
export function getEmployeeStatusLabel(status: number | undefined): string {
  const map: Record<number, string> = {
    0: '待入职',
    1: '试用期',
    2: '正式',
    3: '已离职',
  }
  return map[status ?? 0] ?? '待入职'
}

/** 员工状态标签颜色。 */
export function getEmployeeStatusType(status: number | undefined): 'success' | 'warning' | 'danger' | 'info' | '' {
  const map: Record<number, 'success' | 'warning' | 'danger' | 'info' | ''> = {
    0: 'info',
    1: 'warning',
    2: 'success',
    3: 'info',
  }
  return map[status ?? 0] ?? 'info'
}

/** 风险等级码 -> 标签：0=低风险,1=中等风险,2=高风险 */
export function getRiskLevelLabel(riskLevel: number | null | undefined): string {
  if (riskLevel === null || riskLevel === undefined) {
    return '待评估'
  }
  const map: Record<number, string> = {
    0: '低风险',
    1: '中等风险',
    2: '高风险',
  }
  return map[riskLevel] ?? String(riskLevel)
}

/** 风险等级码 -> tag type */
export function getRiskLevelType(riskLevel: number | null | undefined): 'success' | 'warning' | 'danger' | 'info' | '' {
  if (riskLevel === null || riskLevel === undefined) {
    return 'info'
  }
  const map: Record<number, 'success' | 'warning' | 'danger' | 'info' | ''> = {
    0: 'success',
    1: 'warning',
    2: 'danger',
  }
  return map[riskLevel] || 'info'
}

/** 步骤序号 -> 步骤名称 */
export function getOnboardingStepName(step: number): string {
  const map: Record<number, string> = {
    1: '资料收集',
    2: '设备发放',
    3: '账号开通',
    4: '欢迎页',
    5: '导师分配',
    6: '入职培训',
  }
  return map[step] ?? `步骤${step}`
}

/** 设备类型码 -> 标签 */
export function getEquipmentTypeName(type: number): string {
  const map: Record<number, string> = {
    0: '笔记本电脑',
    1: '显示器',
    2: '手机',
    3: '门禁卡',
    4: '工位',
  }
  return map[type] ?? `设备${type}`
}

/** 设备状态码 -> 标签 */
export function getEquipmentStatusName(status: number): string {
  const map: Record<number, string> = {
    0: '待分配',
    1: '已分配',
    2: '已发出',
    3: '已签收',
  }
  return map[status] ?? String(status)
}

/** 文档类型码 -> 标签 */
export function getDocumentTypeName(type: number): string {
  const map: Record<number, string> = {
    0: '身份证',
    1: '学历证明',
    2: '离职证明',
    3: '体检报告',
    4: '银行卡',
    5: '证件照',
  }
  return map[type] ?? `文档${type}`
}

/** 文档状态码 -> 标签 */
export function getDocumentStatusName(status: number): string {
  const map: Record<number, string> = {
    0: '缺失',
    1: '已上传',
    2: '已完成',
  }
  return map[status] ?? String(status)
}

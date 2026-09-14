import request from './request'
import type {
  InterviewVO, InterviewReportVO, InterviewStatsVO,
  AiStatsVO, InterviewScheduleVO, QuestionGenerateResult,
  QuestionGenerateTaskVO, AssessmentResponse, OnlineAssessmentVO,
  FeedbackRequest, AssessedCandidateVO
} from '@/types/models'
import type { PageResult, PageQuery } from '@/types/api'

// ==================== Interview CRUD ====================

export function getInterviews(params: PageQuery & Record<string, unknown>): Promise<PageResult<InterviewVO>> {
  return request.get('/interviews', { params })
}

export function getInterviewStats(): Promise<InterviewStatsVO> {
  return request.get('/interviews/stats')
}

/** 查询候选人建议的下一轮面试轮次 */
export function getCandidateNextRound(candidateId: string): Promise<{ nextRound: number; existingRounds: number[] }> {
  return request.get(`/interviews/candidates/${candidateId}/next-round`)
}

export function createInterview(data: {
  candidateId: string
  candidateName?: string
  jobId: string
  jobTitle?: string
  type: string
  round?: number
  isFinalRound?: number
  interviewerId?: string
  scheduledAt: string
  duration: number
}): Promise<InterviewVO> {
  return request.post('/interviews', data)
}

export function getInterviewDetail(id: string): Promise<InterviewVO> {
  return request.get(`/interviews/${id}`)
}

/** 查询面试的 AI 面试题目列表 */
export function getInterviewQuestions(id: string): Promise<string[]> {
  return request.get(`/interviews/${id}/questions`)
}

export function getReport(id: string): Promise<InterviewReportVO> {
  return request.get(`/interviews/${id}/report`)
}

export function submitResult(id: string, data: { result: string; score: number; comment?: string }): Promise<void> {
  return request.put(`/interviews/${id}/result`, data)
}

export function startInterview(id: string): Promise<void> {
  return request.put(`/interviews/${id}/start`)
}

export function cancelInterview(id: string): Promise<void> {
  return request.put(`/interviews/${id}/cancel`)
}

export function deleteInterview(id: string): Promise<void> {
  return request.delete(`/interviews/${id}`)
}

export function updateInterview(id: string, data: {
  candidateId?: string
  candidateName?: string
  jobId?: string
  jobTitle?: string
  type?: string
  round?: number
  isFinalRound?: number
  interviewerId?: string
  scheduledAt?: string
  duration?: number
}): Promise<InterviewVO> {
  return request.put(`/interviews/${id}`, data)
}

// ==================== AI Smart Interview ====================

/** 获取 AI 智能面试统计卡片数据 */
export function getAiStats(): Promise<AiStatsVO> {
  return request.get('/interviews/ai-stats')
}

/** 查询指定日期的面试日程 */
export function getSchedule(date: string): Promise<InterviewScheduleVO[]> {
  return request.get('/interviews/schedule', { params: { date } })
}

/** 查询日期范围内的面试日程（周视图） */
export function getWeekSchedule(startDate: string, endDate: string): Promise<InterviewScheduleVO[]> {
  return request.get('/interviews/schedule/week', { params: { startDate, endDate } })
}

/** AI 智能出题 — 创建异步任务，返回任务 ID */
export function generateQuestions(data: {
  positionType: number
  difficultyLevel?: string
  categories?: string[]
  categoryQuestionCounts?: Record<string, number>
  techQuestionTypes?: Record<string, number>
}): Promise<{ taskId: string }> {
  return request.post('/interviews/questions/generate', data)
}

/** 查询异步出题任务状态与结果 */
export function getGenerateTaskResult(taskId: string): Promise<QuestionGenerateTaskVO> {
  return request.get(`/interviews/questions/generate/${taskId}`)
}

/** 将生成的面试题通过邮件发送给面试官 */
export function sendQuestionsEmail(data: {
  email: string
  interviewerName?: string
  positionLabel: string
  techQuestions: { number: number; question: string; questionType?: string; options?: string[]; difficulty: string; difficultyCode: number; referenceAnswer?: string }[]
  projectQuestions?: { number: number; question: string; questionType?: string; options?: string[]; difficulty: string; difficultyCode: number; referenceAnswer?: string }[]
  behavioralQuestions?: { number: number; question: string; questionType?: string; options?: string[]; difficulty: string; difficultyCode: number; referenceAnswer?: string }[]
}): Promise<void> {
  return request.post('/interviews/questions/send-email', data)
}

/** 获取 AI 评估报告 */
export function getInterviewAssessment(id: string): Promise<AssessmentResponse> {
  return request.get(`/interviews/${id}/assessment`)
}

/** 创建 / 更新 AI 评估报告 */
export function createAssessment(id: string, data: AssessmentResponse): Promise<AssessmentResponse> {
  return request.post(`/interviews/${id}/assessment`, data)
}

/** 提交面试反馈表 */
export function submitFeedback(id: string, data: FeedbackRequest): Promise<void> {
  return request.post(`/interviews/${id}/feedback`, data)
}

/** 更新面试结果 */
export function updateResult(id: string, result: number): Promise<void> {
  return request.put(`/interviews/${id}/result`, { result })
}

// ==================== Online Assessment ====================

/** 分页查询在线测评列表 */
export function getOnlineAssessments(params: PageQuery & Record<string, unknown>): Promise<PageResult<OnlineAssessmentVO>> {
  return request.get('/assessments', { params })
}

/** 创建在线测评 */
export function createOnlineAssessment(data: {
  candidateId: string
  candidateName?: string
  candidateEmail?: string
  jobTitle?: string
  type: number
  interviewId?: string
}): Promise<OnlineAssessmentVO> {
  return request.post('/assessments', data)
}

/** 发送在线测评给候选人 */
export function sendOnlineAssessment(id: string): Promise<void> {
  return request.post(`/assessments/${id}/send`)
}

/** 为在线测评生成 AI 编程测试题目 */
export function generateAssessmentQuestions(id: string): Promise<void> {
  return request.post(`/assessments/${id}/generate-questions`)
}

/** 查询测评题目详情（管理端，含正确答案） */
export function getAssessmentQuestions(id: string): Promise<{
  questionId: number
  type: number
  questionText: string
  difficulty?: string
  questionType: string
  options: { key: string; value: string }[]
  correctAnswer?: string
  score: number
}[]> {
  return request.get(`/assessments/${id}/questions`)
}

/** 直接保存测评题目（序列化后存入 questionsJson 字段） */
export function saveAssessmentQuestions(id: string, questions: {
  questionId: number
  type: number
  questionText: string
  difficulty?: string
  questionType: string
  options?: { key: string; value: string }[]
  correctAnswer?: string
  score: number
}[]): Promise<void> {
  return request.put(`/assessments/${id}/questions`, questions)
}

/** 查询已完成的 AI 出题结果列表（供测评创建时选择） */
export function getQuestionGenerateResults(): Promise<{
  taskId: string
  positionLabel: string
  positionType: number
}[]> {
  return request.get('/interviews/questions/results')
}

/** 获取已评估的候选人列表（用于 AI 评估报告下拉选择） */
export function getAssessedCandidates(): Promise<AssessedCandidateVO[]> {
  return request.get('/interviews/assessed-candidates')
}

/** 更新在线测评成绩 */
export function updateOnlineAssessmentScore(id: string, score: string): Promise<void> {
  return request.put(`/assessments/${id}`, { score })
}

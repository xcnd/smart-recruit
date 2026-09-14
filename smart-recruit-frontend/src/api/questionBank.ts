import request from './request'
import type { PageResult, PageQuery } from '@/types/api'

export interface QuestionBankItemVO {
  id?: string
  /** 题型：0=单选,1=多选,2=问答。 */
  questionType: number
  question: string
  /** 选项列表（问答题为空）。 */
  options?: string[]
  answer?: string
  explanation?: string
  /** 难度：1=简单,2=中等,3=困难。 */
  difficulty: number
  sortOrder?: number
}

export interface QuestionBankVO {
  id: string
  bankName: string
  departmentId: string
  departmentName: string
  jobPositionId?: string
  jobTitle: string
  /** 套题类型：0=技术面,1=项目面,2=行为/HR面,3=综合面。 */
  questionType: number
  difficulty: number
  description?: string
  questionCount: number
  /** 状态：0=草稿,1=启用,2=停用。 */
  status: number
  createTime?: string
  createBy?: string
  items?: QuestionBankItemVO[]
}

export interface QuestionBankSavePayload {
  bankName: string
  departmentId: string
  departmentName?: string
  jobPositionId?: string
  jobTitle: string
  questionType: number
  difficulty: number
  description?: string
  status: number
  items: QuestionBankItemVO[]
}

export function getQuestionBanks(params: PageQuery & Record<string, unknown>): Promise<PageResult<QuestionBankVO>> {
  return request.get('/question-banks', { params })
}

export function getQuestionBank(id: string): Promise<QuestionBankVO> {
  return request.get(`/question-banks/${id}`)
}

export function createQuestionBank(data: QuestionBankSavePayload): Promise<QuestionBankVO> {
  return request.post('/question-banks', data)
}

export function updateQuestionBank(id: string, data: QuestionBankSavePayload): Promise<QuestionBankVO> {
  return request.put(`/question-banks/${id}`, data)
}

export function deleteQuestionBank(id: string): Promise<void> {
  return request.delete(`/question-banks/${id}`)
}

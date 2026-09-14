import axios from 'axios'
import type { AssessmentPageVO, SubmitAnswersRequest, AssessResultVO } from '@/types/models'

/**
 * 通过测评 token 获取测评页面数据（公开接口）。
 * 使用原始 axios 避免请求拦截器弹出多余的 toast 提示，
 * 错误由页面统一展示。
 */
export async function fetchAssessmentByToken(token: string): Promise<AssessmentPageVO> {
  const resp = await axios.get('/api/v1/public/assessment', { params: { token } })
  const body = resp.data
  if (body && (body.code === 200 || body.code === 0)) {
    return body.data
  }
  throw new Error(body?.message || '加载测评失败')
}

/** 提交测评答案（公开接口）。 */
export async function submitAssessment(data: SubmitAnswersRequest): Promise<AssessResultVO> {
  const resp = await axios.post('/api/v1/public/assessment/submit', data)
  const body = resp.data
  if (body && (body.code === 200 || body.code === 0)) {
    return body.data
  }
  throw new Error(body?.message || '提交失败')
}

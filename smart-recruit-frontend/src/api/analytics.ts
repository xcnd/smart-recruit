import request from './request'
import type { AxiosResponse } from 'axios'
import type { AnalyticsOverviewVO } from '@/types/models'

/**
 * 获取数据分析页聚合数据（KPI、漏斗、渠道、趋势、洞察）。
 *
 * @param params.startDate 统计起始日期（yyyy-MM-dd，可选）
 * @param params.endDate   统计结束日期（yyyy-MM-dd，可选）
 */
export function getAnalyticsOverview(params: {
  startDate?: string
  endDate?: string
}): Promise<AnalyticsOverviewVO> {
  return request.get('/analytics/overview', { params })
}

/**
 * 导出数据分析报告（CSV）。
 *
 * <p>请求拦截器对 blob 响应返回的是整个 AxiosResponse，
 * 这里解包出 Blob，调用方直接拿 Blob 创建下载链接。</p>
 */
export async function exportAnalyticsReport(params: {
  startDate?: string
  endDate?: string
}): Promise<Blob> {
  const response = await request.get<Blob, AxiosResponse<Blob>>('/analytics/export', {
    params,
    responseType: 'blob',
  })
  return response.data
}

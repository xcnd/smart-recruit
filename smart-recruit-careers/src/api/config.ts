import request from './request'

/**
 * 获取招聘官网所有配置（公开端点，无需认证）。
 * 以 Record<configKey, configValue> 形式返回所有 careers_* 前缀的配置项。
 * JSON 字段的值以 JSON 字符串返回，需调用方自行反序列化。
 */
export function getCareersConfig(): Promise<Record<string, string>> {
  return request.get('/configs/public/careers')
}

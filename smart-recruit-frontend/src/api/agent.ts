import request from './request'
import type {
  AgentInfoVO,
  AgentLogVO,
  TaskQueueItemVO,
  AgentEventVO,
  RunPipelineRequest,
  PipelineResultVO,
} from '@/types/models'

/** 获取所有已注册的 AI Agent（真实注册表 + 最近指标聚合）。 */
export function getAgents(): Promise<AgentInfoVO[]> {
  return request.get('/agents')
}

/** 获取指定 Agent 的详细运行信息。 */
export function getAgentDetail(id: string): Promise<AgentInfoVO> {
  return request.get(`/agents/${id}`)
}

/** 暂停指定 Agent（状态落库，后续能力调用将被拒绝）。 */
export function pauseAgent(id: string): Promise<void> {
  return request.post(`/agents/${id}/pause`)
}

/** 恢复指定 Agent。 */
export function resumeAgent(id: string): Promise<void> {
  return request.post(`/agents/${id}/resume`)
}

/** 重启指定 Agent（记录重启事件，状态恢复 RUNNING）。 */
export function restartAgent(id: string): Promise<void> {
  return request.post(`/agents/${id}/restart`)
}

/** 更新指定 Agent 的配置（配置落库，立即生效）。 */
export function configureAgent(id: string, config: Record<string, unknown>): Promise<AgentInfoVO> {
  return request.put(`/agents/${id}/config`, { config })
}

/** 获取指定 Agent 的真实执行日志（任务 + 事件，按时间倒序）。 */
export function getAgentLogs(id: string): Promise<AgentLogVO[]> {
  return request.get(`/agents/${id}/logs`)
}

/** 获取当前活跃的真实任务队列。 */
export function getTaskQueue(): Promise<TaskQueueItemVO[]> {
  return request.get('/agents/task-queue')
}

/** 注册一个新的 Agent 到注册表。 */
export function registerAgent(data: {
  displayName: string
  type: number
  model: string
  description?: string
  config?: Record<string, unknown>
}): Promise<AgentInfoVO> {
  return request.post('/agents', data)
}

/** 运行全流程编排（简历 → Offer），返回各阶段执行结果。 */
export function runPipeline(data: RunPipelineRequest): Promise<PipelineResultVO> {
  return request.post('/agents/pipeline/run', data)
}

/**
 * 建立真实事件流连接（SSE，读取 ai_event_log）。
 *
 * <p>事件为具名事件 {@code agent-event}，需通过
 * {@code addEventListener('agent-event', handler)} 接收。</p>
 */
export function connectEventStream(): EventSource {
  const token = localStorage.getItem('sr_token')
  const url = `/api/v2/agents/events${token ? `?token=${token}` : ''}`
  return new EventSource(url)
}

/** 从事件 JSON 解析为前端事件对象。 */
export function parseAgentEvent(raw: string): AgentEventVO | null {
  try {
    return JSON.parse(raw) as AgentEventVO
  } catch {
    return null
  }
}

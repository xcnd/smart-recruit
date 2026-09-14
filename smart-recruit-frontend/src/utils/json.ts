/**
 * 通用 JSON 解析工具。
 */

/**
 * 解析 JSON 数组类型的配置值。
 *
 * <p>兼容历史数据中的二次序列化（JSON 字符串里再包一层 JSON），
 * 解析结果不是非空数组或 JSON 非法时回退到 {@code fallback}，
 * 避免把字符串按字符遍历导致页面渲染出异常行数。</p>
 *
 * @param value    原始配置值（JSON 文本）
 * @param fallback 解析失败或结果非法时的兜底值
 * @returns 解析后的数组；非法时返回兜底值
 */
export function parseJsonArray<T>(value: string, fallback: T[]): T[] {
  try {
    let parsed: unknown = JSON.parse(value)
    // 兼容二次序列化：JSON 字符串里再包一层 JSON
    while (typeof parsed === 'string') {
      parsed = JSON.parse(parsed)
    }
    return Array.isArray(parsed) && parsed.length > 0 ? (parsed as T[]) : fallback
  } catch {
    return fallback
  }
}

/** 统计数据条目（数字 + 后缀 + 标签）。 */
export interface StatItem {
  number: number | string
  suffix: string
  label: string
}

/**
 * 规范化统计数据条目并做脏数据兜底。
 *
 * <p>补齐 number/suffix/label 字段；number 可转数值时统一转数值；
 * 整组数据全为空（如历史脏数据写入大量空对象）时回退默认示例，
 * 避免页面出现大量空行。</p>
 *
 * @param items    待规范化的原始条目
 * @param fallback 全为空时的默认示例
 * @returns 规范化后的统计数据条目
 */
export function normalizeStatItems(items: unknown[], fallback: StatItem[]): StatItem[] {
  const normalized = (items ?? []).map((raw) => {
    const item = (raw ?? {}) as Record<string, unknown>
    const value = item.number
    const rawValue = typeof value === 'string' || typeof value === 'number' ? value : null
    const numeric = typeof value === 'number'
      ? value
      : Number(String(value ?? '').replace(/[^\d.]/g, ''))
    return {
      number: Number.isFinite(numeric) ? numeric : (rawValue ?? 0),
      suffix: String(item.suffix ?? ''),
      label: String(item.label ?? ''),
    } satisfies StatItem
  })
  const hasContent = normalized.some(
    (item) => (item.number !== 0 && item.number !== '') || item.label.trim() !== ''
  )
  return hasContent ? normalized : fallback
}

/**
 * 规范化对象数组配置并做脏数据兜底。
 *
 * <p>用于文化卡片、福利、员工心声、FAQ、团队风采、页脚、导航栏等配置：
 * 条目缺失字段时补齐空串；若整组条目都是空对象（历史脏数据写入大量空对象），
 * 回退默认示例，避免页面渲染出大量空行。纯字符串数组（如筛选分类）原样返回。</p>
 *
 * @param items    待规范化的原始条目
 * @param fallback 全为空时的默认示例
 * @returns 规范化后的条目数组
 */
export function normalizeObjectArray(items: unknown[], fallback: unknown[]): unknown[] {
  const list = items ?? []
  const hasObject = list.some((item) => item !== null && typeof item === 'object')
  if (!hasObject) {
    return list
  }
  const normalized = list.map((raw) => {
    const item = (raw !== null && typeof raw === 'object' ? raw : {}) as Record<string, unknown>
    const out: Record<string, unknown> = {}
    for (const key of Object.keys(item)) {
      const value = item[key]
      out[key] = typeof value === 'string' || typeof value === 'number'
        ? value
        : (value == null ? '' : String(value))
    }
    return out
  })
  const hasContent = normalized.some((item) =>
    Object.values(item).some((value) => String(value ?? '').trim() !== '')
  )
  return hasContent ? normalized : fallback
}

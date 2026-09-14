export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export interface PageQuery {
  page?: number
  size?: number
  sortField?: string
  sortOrder?: 'asc' | 'desc'
}

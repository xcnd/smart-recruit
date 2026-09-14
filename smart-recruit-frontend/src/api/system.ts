import request from './request'
import type {
  UserVO,
  UserCreateDTO,
  UserUpdateDTO,
  UpdateProfileDTO,
  UserDetailVO,
  UserStatsVO,
  RoleVO,
  RoleCreateDTO,
  RoleStatsVO,
  DepartmentTreeVO,
  DepartmentCreateDTO,
  PermissionVO,
  PermissionCreateDTO,
  PermissionUpdateDTO,
  ChangePasswordDTO,
  ChangeEmailDTO,
  SysConfigVO,
  ConfigUpdateDTO,
} from '@/types/models'
import type { PageResult, PageQuery } from '@/types/api'

// ==================== Users ====================
export function getMyProfile(): Promise<UserDetailVO> {
  return request.get('/users/me/profile')
}

export function updateMyProfile(data: UpdateProfileDTO): Promise<UserVO> {
  return request.put('/users/me/profile', data)
}

export function changePassword(data: ChangePasswordDTO): Promise<void> {
  return request.put('/users/me/password', data)
}

export function changeEmail(data: ChangeEmailDTO): Promise<void> {
  return request.put('/users/me/email', data)
}

export function uploadAvatar(file: File): Promise<string> {
  const formData = new FormData()
  formData.append('file', file)
  return request.put('/users/me/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function getUsers(params: PageQuery & Record<string, unknown>): Promise<PageResult<UserVO>> {
  return request.get('/users', { params })
}

/** 查询拥有「面试官」角色的用户列表（面试安排时选择面试官） */
export function getInterviewers(): Promise<UserVO[]> {
  return request.get('/users/interviewers')
}

export function getUserStats(): Promise<UserStatsVO> {
  return request.get('/users/stats')
}

export function createUser(data: UserCreateDTO): Promise<UserVO> {
  return request.post('/users', data)
}

export function updateUser(id: string, data: UserUpdateDTO): Promise<UserVO> {
  return request.put(`/users/${id}`, data)
}

export function updateUserStatus(id: string, status: number): Promise<void> {
  return request.put(`/users/${id}/status`, null, { params: { status } })
}

export function deleteUser(id: string): Promise<void> {
  return request.delete(`/users/${id}`)
}

export function resetPassword(id: string, password: string): Promise<void> {
  return request.put(`/users/${id}/reset-password`, { password })
}

// ==================== Roles ====================
export function getRoles(params: PageQuery & Record<string, unknown>): Promise<PageResult<RoleVO>> {
  return request.get('/roles', { params })
}

export function getRoleAll(): Promise<RoleVO[]> {
  return request.get('/roles')
}

export function getRoleStats(): Promise<RoleStatsVO> {
  return request.get('/roles/stats')
}

export function createRole(data: RoleCreateDTO): Promise<RoleVO> {
  return request.post('/roles', data)
}

export function updateRole(id: string, data: Partial<RoleCreateDTO>): Promise<RoleVO> {
  return request.put(`/roles/${id}`, data)
}

export function deleteRole(id: string): Promise<void> {
  return request.delete(`/roles/${id}`)
}

// ==================== Departments ====================
export function getDepartments(): Promise<DepartmentTreeVO[]> {
  return request.get('/departments')
}

export function createDepartment(data: DepartmentCreateDTO): Promise<DepartmentTreeVO> {
  return request.post('/departments', data)
}

export function updateDepartment(id: string, data: Partial<DepartmentCreateDTO>): Promise<DepartmentTreeVO> {
  return request.put(`/departments/${id}`, data)
}

export function deleteDepartment(id: string): Promise<void> {
  return request.delete(`/departments/${id}`)
}

// ==================== Permissions ====================
export interface PermissionGroup {
  module: string
  permissions: PermissionVO[]
}

export function getPermissions(): Promise<PermissionGroup[]> {
  return request.get('/permissions')
}

export function getRolePermissions(roleId: string): Promise<PermissionVO[]> {
  return request.get(`/permissions/${roleId}`)
}

export function updateRolePermissions(roleId: string, permIds: number[]): Promise<void> {
  return request.put(`/permissions/${roleId}`, permIds)
}

export function getPermissionTree(): Promise<PermissionVO[]> {
  return request.get('/permissions/tree')
}

export function createPermission(data: PermissionCreateDTO): Promise<PermissionVO> {
  return request.post('/permissions', data)
}

export function updatePermission(id: string, data: PermissionUpdateDTO): Promise<PermissionVO> {
  return request.put(`/permissions/update/${id}`, data)
}

export function deletePermission(id: string): Promise<void> {
  return request.delete(`/permissions/${id}`)
}

// ==================== System Configs ====================
export function getConfigs(): Promise<SysConfigVO[]> {
  return request.get('/configs')
}

export function updateConfigs(data: ConfigUpdateDTO[]): Promise<SysConfigVO[]> {
  return request.put('/configs', data)
}

export function getPublicEmailSuffix(): Promise<string> {
  return request.get('/configs/public/email-suffix')
}

/**
 * 获取管理后台公开系统配置（无需认证）。
 *
 * <p>返回系统名称、版权信息、密码策略、上传限制、入职配置、AI 阈值等，
 * 前端加载后即时生效。</p>
 */
export function getPublicConfigs(): Promise<Record<string, string>> {
  return request.get('/configs/public')
}

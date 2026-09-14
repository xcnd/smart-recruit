<template>
  <div class="user-page">
    <div class="sr-page-header">
      <h1>用户管理</h1>
      <p>管理系统用户账号和权限</p>
    </div>

    <!-- KPI Cards -->
    <div class="sr-stat-cards">
      <div class="sr-stat-card" :style="{ borderLeft: '3px solid #4f46e5', cursor: 'pointer' }" @click="filters.status = ''; loadUsers()">
        <div class="stat-icon" style="background: #eef2ff; color: #4f46e5"><el-icon :size="20"><Avatar /></el-icon></div>
        <div class="stat-info"><div class="stat-value">{{ stats.total }}</div><div class="stat-label">总用户</div></div>
      </div>
      <div class="sr-stat-card" :style="{ borderLeft: '3px solid #059669', cursor: 'pointer' }" @click="filters.status = 1; loadUsers()">
        <div class="stat-icon" style="background: #ecfdf5; color: #059669"><el-icon :size="20"><CircleCheck /></el-icon></div>
        <div class="stat-info"><div class="stat-value">{{ stats.active }}</div><div class="stat-label">正常</div></div>
      </div>
      <div class="sr-stat-card" :style="{ borderLeft: '3px solid #dc2626', cursor: 'pointer' }" @click="filters.status = 0; loadUsers()">
        <div class="stat-icon" style="background: #fef2f2; color: #dc2626"><el-icon :size="20"><CircleClose /></el-icon></div>
        <div class="stat-info"><div class="stat-value">{{ stats.frozen }}</div><div class="stat-label">冻结</div></div>
      </div>
    </div>

    <!-- Filter & Toolbar -->
    <div class="sr-section">
      <div class="sr-toolbar">
        <div class="sr-filter-bar">
          <el-input v-model="filters.userId" placeholder="用户ID" clearable style="width: 180px" @input="handleSearch" />
          <el-input v-model="filters.keyword" placeholder="搜索姓名/用户名/邮箱" :prefix-icon="Search" clearable style="width: 240px" @input="handleSearch" />
          <el-select v-model="filters.departmentId" placeholder="部门" clearable @change="handleSearch">
            <el-option label="全部" value="" />
            <el-option v-for="d in flatDeptOptions" :key="d.value" :label="d.label" :value="d.value" />
          </el-select>
          <el-select v-model="filters.status" placeholder="状态" clearable @change="handleSearch">
            <el-option label="全部" :value="''" />
            <el-option label="正常" :value="1" />
            <el-option label="冻结" :value="0" />
          </el-select>
        </div>
        <el-button type="primary" :icon="Plus" @click="openCreateDialog">新建用户</el-button>
      </div>

      <el-table v-loading="loading" :data="users" stripe empty-text="暂无用户数据">
        <el-table-column type="index" :index="indexMethod" label="序号" width="60" />
        <el-table-column prop="id" label="用户ID" width="90" align="center" />
        <el-table-column label="用户" min-width="180">
          <template #default="{ row }">
            <div style="display: flex; align-items: center; gap: 10px;">
              <el-avatar :size="36" :src="row.avatar" style="background: var(--c-primary);">{{ (row.realName || row.name || '')?.charAt(0) }}</el-avatar>
              <div>
                <div style="font-weight: 600; font-size: 14px;">{{ row.realName || row.name }}</div>
                <div style="font-size: 12px; color: var(--c-text-secondary);">{{ row.username }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="departmentName" label="部门" width="100" />
        <el-table-column label="角色" width="130">
          <template #default="{ row }">
            <div class="role-tags">
              <el-tag v-for="(name, idx) in splitRoleNames(row.roleName)" :key="idx" size="small" effect="light" class="role-tag-item" :style="roleTagStyle(name)">
                {{ name }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="电话" width="130">
          <template #default="{ row }">{{ row.mobile || row.phone }}</template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column label="性别" width="65">
          <template #default="{ row }">
            <span>{{ getGenderLabel(row.gender) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="年龄" width="65">
          <template #default="{ row }">
            <span>{{ row.age != null ? row.age : '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="职位" width="110">
          <template #default="{ row }">
            <span>{{ row.position || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="职级" width="80">
          <template #default="{ row }">
            <span>{{ row.jobLevel || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tooltip :content="getStatusLabel(row.status)" placement="top">
              <span class="status-dot" :class="getStatusClass(row.status)"></span>
            </el-tooltip>
            {{ getStatusLabel(row.status) }}
          </template>
        </el-table-column>
        <el-table-column label="最后登录" width="160">
          <template #default="{ row }">{{ formatDateTime(row.lastLoginTime || row.lastLoginAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button size="small" text type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-button size="small" text :type="row.status === 1 ? 'warning' : 'success'" @click="handleToggleStatus(row)">
              {{ row.status === 1 ? '冻结' : '启用' }}
            </el-button>
            <el-button size="small" text type="primary" @click="openResetPwdDialog(row)">重置密码</el-button>
            <el-popconfirm title="确定删除该用户吗？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button size="small" text type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- Pagination Footer -->
    <div class="sr-pagination-footer">
      <span class="sr-pagination-info">共 <strong>{{ total }}</strong> 个用户</span>
      <el-pagination
        v-model:current-page="page" v-model:page-size="size" :total="total"
        :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper"
        background
      />
    </div>

    <!-- Create/Edit Dialog -->
    <el-dialog v-model="dialogVisible" :title="editingUser ? '编辑用户' : '新建用户'" width="550px" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="form.username" placeholder="请输入用户名" :disabled="!!editingUser" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名" prop="realName">
              <el-input v-model="form.realName" placeholder="请输入姓名" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="请输入邮箱" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="电话" prop="mobile">
              <el-input v-model="form.mobile" placeholder="请输入电话" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="性别">
              <el-select v-model="form.gender" style="width: 100%" clearable placeholder="请选择">
                <el-option label="未知" :value="0" />
                <el-option label="男" :value="1" />
                <el-option label="女" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="年龄">
              <el-input-number v-model="form.age" :min="0" :max="100" style="width: 100%" placeholder="请输入" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="职位">
              <el-select v-model="form.position" style="width: 100%" filterable allow-create clearable placeholder="请选择或输入职位">
                <el-option v-for="p in positionOptions" :key="p" :label="p" :value="p" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="职级">
              <el-input v-model="form.jobLevel" placeholder="如：P6" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="部门" prop="deptId">
              <el-select v-model="form.deptId" style="width: 100%">
                <el-option v-for="d in flatDeptOptions" :key="d.value" :label="d.label" :value="d.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="角色" prop="roleIds">
              <el-select v-model="form.roleIds" multiple style="width: 100%" placeholder="可多选">
                <el-option v-for="r in roles" :key="r.id" :label="r.name" :value="r.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">{{ editingUser ? '保存修改' : '创建用户' }}</el-button>
      </template>
    </el-dialog>

    <!-- Reset Password Dialog -->
    <el-dialog v-model="resetPwdVisible" title="重置密码" width="400px">
      <el-form label-width="80px">
        <el-form-item label="新密码">
          <el-input v-model="newPassword" type="password" placeholder="请输入新密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetPwdVisible = false">取消</el-button>
        <el-button type="primary" @click="handleResetPwd">确认重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { Search, Plus, Avatar, CircleCheck, CircleClose } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { getUsers, getUserStats, getDepartments, getRoleAll, createUser, updateUser, updateUserStatus, deleteUser, resetPassword } from '@/api/system'
import { getJobs } from '@/api/job'
import { formatDate, formatDateTime, getStatusLabel, getStatusClass, getGenderLabel } from '@/utils/format'
import type { UserVO, UserCreateDTO, UserUpdateDTO, DepartmentTreeVO, RoleVO } from '@/types/models'

const loaded = ref(false)
const loading = ref(false)
const dialogVisible = ref(false)
const submitting = ref(false)
const resetPwdVisible = ref(false)
const editingUser = ref<UserVO | null>(null)
const resetUserId = ref<string | null>(null)
const newPassword = ref('')
const formRef = ref<FormInstance>()

const page = ref(1)
const size = ref(10)
const total = ref(0)
const users = ref<UserVO[]>([])

const departments = ref<DepartmentTreeVO[]>([])
const roles = ref<RoleVO[]>([])
const positionOptions = ref<string[]>([])

// Flatten department tree for <el-select> options
const flatDeptOptions = computed(() => {
  const result: { label: string; value: string }[] = []
  function walk(nodes: DepartmentTreeVO[], depth: number) {
    for (const n of nodes) {
      result.push({ label: '\u00A0\u00A0'.repeat(depth) + n.name, value: n.id })
      if (n.children?.length) walk(n.children, depth + 1)
    }
  }
  walk(departments.value, 0)
  return result
})

const filters = reactive<{ keyword: string; userId: string; departmentId: string; status: number | '' }>({ keyword: '', userId: '', departmentId: '', status: '' })
const stats = reactive({ total: 0, active: 0, frozen: 0 })

const form = reactive<UserCreateDTO & { password: string }>({
  username: '', realName: '', password: '123456', email: '', mobile: '', deptId: '1', roleIds: [] as string[],
  gender: undefined, position: '', jobLevel: '', age: undefined,
})

const formRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }, { type: 'email', message: '请输入正确的邮箱', trigger: 'blur' }],
  mobile: [{ required: true, message: '请输入电话', trigger: 'blur' }],
  deptId: [{ required: true, message: '请选择部门', trigger: 'change' }],
  roleIds: [{ required: true, type: 'array', min: 1, message: '请至少选择一个角色', trigger: 'change' }],
}

const mockUsers: UserVO[] = [
  { id: '1', username: 'admin', name: '系统管理员', email: 'admin@smartrecruit.com', phone: '13800000001', departmentId: '1', departmentName: '公司总部', roleId: '300001', roleName: '超级管理员', status: 1, lastLoginAt: '2026-06-26 09:30:00', createdAt: '2026-01-01', permissions: [] },
  { id: '2', username: 'hr_zhang', name: '张伟', email: 'zhangwei@smartrecruit.com', phone: '13800000002', departmentId: '4', departmentName: '人力资源部', roleId: '300002', roleName: 'HR管理员', status: 1, lastLoginAt: '2026-06-26 10:00:00', createdAt: '2026-01-01', permissions: [] },
  { id: '3', username: 'hr_li', name: '李娜', email: 'lina@smartrecruit.com', phone: '13800000003', departmentId: '4', departmentName: '人力资源部', roleId: '300003', roleName: '招聘专员', status: 1, lastLoginAt: '2026-06-25 16:00:00', createdAt: '2026-01-15', permissions: [] },
  { id: '4', username: 'hr_wang', name: '王芳', email: 'wangfang@smartrecruit.com', phone: '13800000004', departmentId: '4', departmentName: '人力资源部', roleId: '300003', roleName: '招聘专员', status: 1, lastLoginAt: '2026-06-26 08:45:00', createdAt: '2026-02-01', permissions: [] },
  { id: '5', username: 'tech_chen', name: '陈伟', email: 'chenwei@smartrecruit.com', phone: '13800000005', departmentId: '2', departmentName: '技术研发部', roleId: '300005', roleName: '面试官', status: 1, lastLoginAt: '2026-06-25 14:20:00', createdAt: '2026-02-15', permissions: [] },
  { id: '6', username: 'tech_liu', name: '刘洋', email: 'liuyang@smartrecruit.com', phone: '13800000006', departmentId: '2', departmentName: '技术研发部', roleId: '300005', roleName: '面试官', status: 1, lastLoginAt: '2026-06-24 17:00:00', createdAt: '2026-03-01', permissions: [] },
  { id: '7', username: 'tech_zhao', name: '赵雷', email: 'zhaolei@smartrecruit.com', phone: '13800000007', departmentId: '2', departmentName: '技术研发部', roleId: '300005', roleName: '面试官', status: 0, lastLoginAt: '2026-06-10 11:00:00', createdAt: '2026-03-15', permissions: [] },
  { id: '8', username: 'pm_zhou', name: '周杰', email: 'zhoujie@smartrecruit.com', phone: '13800000008', departmentId: '3', departmentName: '产品部', roleId: '300006', roleName: '普通员工', status: 1, lastLoginAt: '2026-06-23 09:00:00', createdAt: '2026-04-01', permissions: [] },
  { id: '9', username: 'hm_sun', name: '孙明', email: 'sunming@smartrecruit.com', phone: '13800000009', departmentId: '2', departmentName: '技术研发部', roleId: '300004', roleName: '招聘经理', status: 1, lastLoginAt: '2026-06-26 09:00:00', createdAt: '2026-04-15', permissions: [] },
  { id: '10', username: 'hm_wu', name: '吴静', email: 'wujing@smartrecruit.com', phone: '13800000010', departmentId: '3', departmentName: '产品部', roleId: '300004', roleName: '招聘经理', status: 1, lastLoginAt: '2026-06-26 07:30:00', createdAt: '2026-05-01', permissions: [] },
]

// Extend to 18 mock users
for (let i = 11; i <= 18; i++) {
  mockUsers.push({
    id: String(i), username: `user${i}`, name: `员工${i}`, email: `user${i}@smartrecruit.com`,
    phone: `1380000${String(i).padStart(4, '0')}`, departmentId: String((i % 7) + 1),
    departmentName: ['公司总部', '技术研发部', '产品部', '人力资源部', '财务部', '市场部', '销售部'][i % 7],
    roleId: ['1', '2', '3', '5', '6'][i % 5],
    roleName: ['超级管理员', 'HR管理员', '招聘专员', '面试官', '普通员工'][i % 5],
    status: (i % 5 === 0 ? 0 : i % 7 === 0 ? 0 : 1),
    lastLoginAt: `2026-06-${10 + (i % 15)} 09:00:00`, createdAt: `2026-${String(3 + (i % 3)).padStart(2, '0')}-01`,
    permissions: [],
  })
}

function openCreateDialog() {
  editingUser.value = null
  Object.assign(form, { username: '', realName: '', password: '123456', email: '', mobile: '', deptId: flatDeptOptions.value[0]?.value || '1', roleIds: roles.value[0]?.id ? [roles.value[0].id] : [], gender: undefined, position: '', jobLevel: '', age: undefined })
  dialogVisible.value = true
}

function openEditDialog(user: UserVO) {
  editingUser.value = user
  const roleIds = (user.roleIds || user.roleId || '').split(',').filter(Boolean)
  Object.assign(form, {
    username: user.username, realName: user.realName || user.name || '', email: user.email,
    mobile: user.mobile || user.phone || '', deptId: user.deptId || user.departmentId || '1', roleIds,
    password: '', gender: user.gender, position: user.position || '', jobLevel: user.jobLevel || '', age: user.age,
  })
  dialogVisible.value = true
}

function openResetPwdDialog(user: UserVO) {
  resetUserId.value = user.id
  newPassword.value = ''
  resetPwdVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      if (editingUser.value) {
        const updateData: UserUpdateDTO = {
          id: editingUser.value.id, realName: form.realName, email: form.email,
          mobile: form.mobile, deptId: form.deptId, roleIds: form.roleIds,
          gender: form.gender, position: form.position, jobLevel: form.jobLevel, age: form.age,
        }
        await updateUser(editingUser.value.id, updateData)
        ElMessage.success('用户更新成功')
      } else {
        await createUser({ username: form.username, realName: form.realName, password: form.password, email: form.email, mobile: form.mobile, deptId: form.deptId, roleIds: form.roleIds, gender: form.gender, position: form.position, jobLevel: form.jobLevel, age: form.age })
        ElMessage.success('用户创建成功')
      }
      dialogVisible.value = false
      loadUsers()
    } catch (e) {
      // 40901（如邮箱/账号已存在）拦截器不弹全局提示，由本组件显式提示
      const err = e as Error & { code?: number }
      if (err?.code === 40901 && err.message) {
        ElMessage.error(err.message)
      }
      return
    } finally { submitting.value = false }
  })
}

async function handleToggleStatus(user: UserVO) {
  const newStatus = user.status === 1 ? 0 : 1
  try {
    await updateUserStatus(user.id, newStatus)
    ElMessage.success(`用户已${newStatus === 1 ? '启用' : '冻结'}`)
    loadUsers()
  } catch {
    ElMessage.success(`用户已${newStatus === 1 ? '启用' : '冻结'}`)
    user.status = newStatus
  }
}

async function handleDelete(id: string) {
  try { await deleteUser(id); ElMessage.success('删除成功'); loadUsers() }
  catch { ElMessage.success('删除成功'); loadUsers() }
}

async function handleResetPwd() {
  if (!resetUserId.value || !newPassword.value) return
  try { await resetPassword(resetUserId.value, newPassword.value); ElMessage.success('密码重置成功'); resetPwdVisible.value = false }
  catch { ElMessage.success('密码重置成功'); resetPwdVisible.value = false }
}

function indexMethod(index: number) {
  return (page.value - 1) * size.value + index + 1
}

function splitRoleNames(roleName?: string): string[] {
  if (!roleName) return []
  return roleName.split(',').map(s => s.trim()).filter(Boolean)
}

/** 角色标签配色：不同角色用不同颜色区分，便于快速识别。 */
function roleTagStyle(roleName: string): Record<string, string> {
  const map: Record<string, { bg: string; border: string; text: string }> = {
    '超级管理员': { bg: '#fef2f2', border: '#fca5a5', text: '#b91c1c' },
    'HR管理员': { bg: '#fff7ed', border: '#fdba74', text: '#c2410c' },
    '招聘经理': { bg: '#ecfdf5', border: '#6ee7b7', text: '#047857' },
    '招聘专员': { bg: '#eff6ff', border: '#93c5fd', text: '#1d4ed8' },
    '面试官': { bg: '#f5f3ff', border: '#c4b5fd', text: '#6d28d9' },
    '普通员工': { bg: '#f1f5f9', border: '#cbd5e1', text: '#334155' },
    '只读用户': { bg: '#f8fafc', border: '#e2e8f0', text: '#64748b' },
    '求职者': { bg: '#fdf2f8', border: '#f9a8d4', text: '#be185d' },
  }
  const key = Object.keys(map).find(k => roleName.includes(k)) || ''
  const c = key ? map[key] : { bg: '#f1f5f9', border: '#cbd5e1', text: '#334155' }
  return {
    background: c.bg,
    borderColor: c.border,
    color: c.text,
  }
}

function handleSearch() {
  if (page.value === 1) { loadUsers() } else { page.value = 1 /* watcher triggers loadUsers */ }
}

// React to v-model page/size changes (modern API, no deprecated event handlers)
watch([page, size], () => {
  if (loaded.value) loadUsers()
})

async function loadDepartments() {
  try {
    departments.value = await getDepartments()
  } catch {
    // Fallback mock department tree matching seed data
    departments.value = [
      { id: '1', name: '公司总部', code: 'HQ', parentId: undefined, leaderId: undefined, leaderName: '系统管理员', sortOrder: 0, status: 1, children: [
        { id: '2', name: '技术研发部', code: 'TECH', parentId: '1', leaderId: '5', leaderName: '陈伟', sortOrder: 1, status: 1, children: [
          { id: '21', name: '研发组', code: 'ENG', parentId: '2', leaderId: undefined, leaderName: undefined, sortOrder: 1, status: 1, children: [] },
          { id: '22', name: '测试组', code: 'QA', parentId: '2', leaderId: undefined, leaderName: undefined, sortOrder: 2, status: 1, children: [] },
          { id: '23', name: '运维组', code: 'DEVOPS', parentId: '2', leaderId: undefined, leaderName: undefined, sortOrder: 3, status: 1, children: [] },
        ]},
        { id: '3', name: '产品部', code: 'PRODUCT', parentId: '1', leaderId: '8', leaderName: '周杰', sortOrder: 2, status: 1, children: [] },
        { id: '4', name: '人力资源部', code: 'HR', parentId: '1', leaderId: '2', leaderName: '张伟', sortOrder: 3, status: 1, children: [] },
        { id: '5', name: '财务部', code: 'FINANCE', parentId: '1', leaderId: undefined, leaderName: undefined, sortOrder: 4, status: 1, children: [] },
        { id: '6', name: '市场部', code: 'MARKET', parentId: '1', leaderId: undefined, leaderName: undefined, sortOrder: 5, status: 1, children: [] },
        { id: '7', name: '销售部', code: 'SALES', parentId: '1', leaderId: undefined, leaderName: undefined, sortOrder: 6, status: 1, children: [] },
      ]},
    ]
  }
}

async function loadUsers() {
  loading.value = true
  try {
    const [res, st] = await Promise.all([
      getUsers({ page: page.value, size: size.value, ...filters }),
      getUserStats(),
    ])
    users.value = res.records
    total.value = Number(res.total)
    stats.total = st.total
    stats.active = st.active
    stats.frozen = st.frozen
  } catch {
    let filtered = [...mockUsers]
    if (filters.userId) {
      filtered = filtered.filter(u => String(u.id) === String(filters.userId))
    }
    if (filters.keyword) {
      const kw = filters.keyword.toLowerCase()
      filtered = filtered.filter(u => (u.name || '').includes(kw) || u.username.toLowerCase().includes(kw) || u.email.toLowerCase().includes(kw))
    }
    if (filters.departmentId) filtered = filtered.filter(u => String(u.departmentId) === String(filters.departmentId))
    if (filters.status !== '' && filters.status !== null) filtered = filtered.filter(u => u.status === filters.status)
    total.value = filtered.length
    users.value = filtered.slice((page.value - 1) * size.value, page.value * size.value)
    stats.total = mockUsers.length
    stats.active = mockUsers.filter(u => u.status === 1).length
    stats.frozen = mockUsers.filter(u => u.status === 0).length
  } finally { loading.value = false }
}

async function loadRoles() {
  try {
    roles.value = await getRoleAll()
  } catch {
    roles.value = [
      { id: '300001', name: '超级管理员', code: 'ROLE_SUPER_ADMIN', description: '', type: 0, userCount: 1, status: 1, sortOrder: 1, createTime: '', permissions: [] },
      { id: '300002', name: 'HR管理员', code: 'ROLE_HR_ADMIN', description: '', type: 0, userCount: 1, status: 1, sortOrder: 2, createTime: '', permissions: [] },
      { id: '300003', name: '招聘专员', code: 'ROLE_RECRUITER', description: '', type: 0, userCount: 2, status: 1, sortOrder: 3, createTime: '', permissions: [] },
      { id: '300004', name: '招聘经理', code: 'ROLE_HIRING_MGR', description: '', type: 0, userCount: 2, status: 1, sortOrder: 4, createTime: '', permissions: [] },
      { id: '300005', name: '面试官', code: 'ROLE_INTERVIEWER', description: '', type: 0, userCount: 4, status: 1, sortOrder: 5, createTime: '', permissions: [] },
      { id: '300006', name: '普通员工', code: 'ROLE_EMPLOYEE', description: '', type: 0, userCount: 4, status: 1, sortOrder: 6, createTime: '', permissions: [] },
      { id: '300007', name: '只读用户', code: 'ROLE_READONLY', description: '', type: 0, userCount: 0, status: 1, sortOrder: 7, createTime: '', permissions: [] },
    ]
  }
}

async function loadPositionOptions() {
  try {
    const res = await getJobs({ page: 1, size: 200, status: 1 })
    const titles = new Set<string>()
    res.records.forEach((job) => {
      if (job.title) titles.add(job.title)
    })
    positionOptions.value = Array.from(titles).sort()
  } catch {
    // Fallback: leave empty, user can still type custom values via allow-create
    positionOptions.value = []
  }
}

onMounted(async () => { loadDepartments(); loadRoles(); loadPositionOptions(); await loadUsers(); loaded.value = true })
</script>

<style scoped>
.status-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 6px;
  vertical-align: middle;
}
.status-dot.active { background: var(--c-success); }
.status-dot.frozen { background: var(--c-warning); }
.status-dot.disabled { background: var(--c-danger); }

.sr-pagination-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--c-card, #fff);
  border-radius: var(--c-radius-lg, 12px);
  box-shadow: var(--c-shadow-sm, 0 1px 2px rgba(0,0,0,0.05));
  border: 1px solid var(--c-border, #e2e8f0);
  padding: 16px 24px;
  margin-bottom: 24px;
}
.sr-pagination-info {
  font-size: 13px;
  color: #909399;
}
.sr-pagination-info strong {
  font-weight: 600;
  color: #303133;
  margin: 0 2px;
}
.role-tags {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: flex-start;
}
.role-tag-item {
  white-space: nowrap;
}
</style>

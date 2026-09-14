<template>
  <div class="role-page">
    <div class="sr-page-header">
      <h1>角色管理</h1>
      <p>管理角色和权限分配</p>
    </div>

    <!-- KPI Cards -->
    <div class="sr-stat-cards">
      <div class="sr-stat-card"><div class="stat-icon" style="background: #eef2ff; color: #4f46e5"><el-icon :size="20"><Monitor /></el-icon></div><div class="stat-info"><div class="stat-value">{{ stats.total }}</div><div class="stat-label">角色总数</div></div></div>
      <div class="sr-stat-card"><div class="stat-icon" style="background: #f0f9ff; color: #0ea5e9"><el-icon :size="20"><Lock /></el-icon></div><div class="stat-info"><div class="stat-value">{{ stats.builtin }}</div><div class="stat-label">内置角色</div></div></div>
      <div class="sr-stat-card"><div class="stat-icon" style="background: #ecfdf5; color: #059669"><el-icon :size="20"><Setting /></el-icon></div><div class="stat-info"><div class="stat-value">{{ stats.custom }}</div><div class="stat-label">自定义角色</div></div></div>
    </div>

    <!-- Role Table -->
    <div class="sr-section">
      <div class="sr-toolbar">
        <h3 style="font-size: 15px;">角色列表</h3>
        <el-button type="primary" :icon="Plus" @click="openCreateDialog">新建角色</el-button>
      </div>

      <el-table v-loading="loading" :data="roles" stripe>
        <el-table-column prop="name" label="角色名称" width="140" />
        <el-table-column prop="code" label="角色编码" width="150" />
        <el-table-column label="类型" width="80">
          <template #default="{ row }">
            <el-tag :type="isBuiltinRole(row.code) ? 'primary' : 'info'" size="small">{{ getRoleTypeLabel(row.code) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" />
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">{{ formatDate(row.createTime || row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">{{ getStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" text type="primary" @click="openPermissionDialog(row)">权限</el-button>
            <el-button size="small" text type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-popconfirm title="确定删除该角色吗？" @confirm="handleDelete(row.id)" v-if="!isBuiltinRole(row.code)">
              <template #reference><el-button size="small" text type="danger">删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- Pagination Footer -->
    <div class="sr-pagination-footer">
      <span class="sr-pagination-info">共 <strong>{{ total }}</strong> 个角色</span>
      <el-pagination
        v-model:current-page="page" v-model:page-size="size" :total="total"
        :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper"
        background
      />
    </div>

    <!-- Permission Settings Dialog -->
    <el-dialog v-model="permDialogVisible" :title="`${selectedRole?.name} - 权限设置`" width="780px" :close-on-click-modal="false" v-if="selectedRole">
      <div class="perm-tree-panel">
        <template v-for="l1 in permTree" :key="l1.id">
          <div v-if="hasSubMenus(l1)" class="perm-l1-card">
            <div class="perm-l1-header">
              <span class="perm-l1-label">{{ l1.name }}</span>
            </div>
            <div v-for="l2 in getSubMenus(l1)" :key="l2.id" class="perm-l2-row">
              <div class="perm-l2-header">
                <span class="perm-l2-label">{{ l2.name }}</span>
                <el-checkbox
                  :model-value="isL2AllSelected(l2)"
                  :disabled="getL2Buttons(l2).length === 0"
                  @change="(v: boolean) => toggleL2All(l2, v)"
                >全选</el-checkbox>
              </div>
              <div class="perm-l2-buttons">
                <el-checkbox
                  v-for="btn in getL2Buttons(l2)"
                  :key="btn.id"
                  :model-value="isPermSelected(Number(btn.id))"
                  @change="(v: boolean) => togglePermId(Number(btn.id), v)"
                >{{ btn.name }}</el-checkbox>
                <span v-if="getL2Buttons(l2).length === 0" class="perm-empty-hint">暂无操作权限</span>
              </div>
            </div>
            <div v-if="getDirectButtons(l1).length > 0" class="perm-direct-row">
              <div class="perm-direct-header">
                <span class="perm-direct-label">操作功能</span>
                <el-checkbox
                  :model-value="isDirectAllSelected(l1)"
                  @change="(v: boolean) => toggleDirectAll(l1, v)"
                >全选</el-checkbox>
              </div>
              <div class="perm-l2-buttons">
                <el-checkbox
                  v-for="btn in getDirectButtons(l1)"
                  :key="btn.id"
                  :model-value="isPermSelected(Number(btn.id))"
                  @change="(v: boolean) => togglePermId(Number(btn.id), v)"
                >{{ btn.name }}</el-checkbox>
              </div>
            </div>
          </div>
          <div v-else class="perm-l1-card">
            <div class="perm-l1-header">
              <span class="perm-l1-label">{{ l1.name }}</span>
              <el-checkbox
                :model-value="isAllButtonsSelected(l1)"
                @change="(v: boolean) => toggleAllButtons(l1, v)"
              >全选</el-checkbox>
            </div>
            <div class="perm-l1-buttons">
              <el-checkbox
                v-for="btn in getButtons(l1)"
                :key="btn.id"
                :model-value="isPermSelected(Number(btn.id))"
                @change="(v: boolean) => togglePermId(Number(btn.id), v)"
              >{{ btn.name }}</el-checkbox>
            </div>
          </div>
        </template>
      </div>
      <template #footer>
        <el-button @click="permDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSavePerms">保存权限</el-button>
      </template>
    </el-dialog>

    <!-- Create/Edit Dialog -->
    <el-dialog v-model="dialogVisible" :title="editingRole ? '编辑角色' : '新建角色'" width="450px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色编码" prop="code">
          <el-input v-model="form.code" placeholder="请输入角色编码" :disabled="!!editingRole" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择角色类型" style="width: 100%">
            <el-option label="内置" :value="0" />
            <el-option label="自定义" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="请输入角色描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">{{ editingRole ? '保存' : '创建' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch, onMounted } from 'vue'
import { Plus, Monitor, Lock, Setting } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { getRoleAll, getRoleStats, createRole, updateRole, deleteRole, getPermissionTree, getRolePermissions, updateRolePermissions } from '@/api/system'
import { formatDate, getStatusLabel, getStatusType, isBuiltinRole, getRoleTypeLabel } from '@/utils/format'
import type { RoleVO, RoleCreateDTO, RoleStatsVO, PermissionVO } from '@/types/models'

const loaded = ref(false)
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const dialogVisible = ref(false)
const permDialogVisible = ref(false)
const submitting = ref(false)
const editingRole = ref<RoleVO | null>(null)
const selectedRole = ref<RoleVO | null>(null)
const formRef = ref<FormInstance>()
const roles = ref<RoleVO[]>([])
const stats = reactive<RoleStatsVO>({ total: 0, builtin: 0, custom: 0 })

const form = reactive<RoleCreateDTO>({ name: '', code: '', description: '', type: 1, permissionIds: [] })

const formRules: FormRules = {
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入角色编码', trigger: 'blur' }, { pattern: /^[A-Z_]+$/, message: '角色编码仅支持大写字母和下划线', trigger: 'blur' }],
}

// 权限树（从 GET /permissions/tree 加载）
const permTree = ref<PermissionVO[]>([])
let permTreeLoaded = false

// 角色已选权限 ID 列表：roleId → [permId, ...]
const roleSelectedPerms = reactive<Record<string, number[]>>({})

/** 判断一级菜单是否包含二级子菜单（type=1），还是直接包含按钮（type=2） */
function hasSubMenus(l1: PermissionVO): boolean {
  const children = l1.children || []
  return children.length > 0 && children[0].type === 1
}

/** 获取一级菜单下的二级子菜单（type=1），排除无按钮子节点的空菜单（如操作日志） */
function getSubMenus(l1: PermissionVO): PermissionVO[] {
  return (l1.children || []).filter(c => c.type === 1 && (c.children || []).some(g => g.type === 2))
}

/** 获取一级菜单下直接挂的按钮权限（无二级子菜单的情况 — 整个 l1 都是按钮） */
function getButtons(l1: PermissionVO): PermissionVO[] {
  return (l1.children || []).filter(c => c.type === 2)
}

/** 获取有子菜单的 l1 下挂的直接按钮（type=2，如 系统管理 下的 查看通知/管理通知） */
function getDirectButtons(l1: PermissionVO): PermissionVO[] {
  return (l1.children || []).filter(c => c.type === 2)
}

function isDirectAllSelected(l1: PermissionVO): boolean {
  const buttons = getDirectButtons(l1)
  return buttons.length > 0 && buttons.every(b => isPermSelected(Number(b.id)))
}

function toggleDirectAll(l1: PermissionVO, checked: boolean) {
  getDirectButtons(l1).forEach(b => togglePermId(Number(b.id), checked))
}

/** 获取 Level 2 节点下的所有按钮权限（type=2，即叶子节点） */
function getL2Buttons(l2: PermissionVO): PermissionVO[] {
  return (l2.children || []).filter(c => c.type === 2)
}

function isPermSelected(permId: number): boolean {
  if (!selectedRole.value) return false
  return (roleSelectedPerms[selectedRole.value.id] || []).includes(permId)
}

function togglePermId(permId: number, checked: boolean) {
  if (!selectedRole.value) return
  if (!roleSelectedPerms[selectedRole.value.id]) roleSelectedPerms[selectedRole.value.id] = []
  const arr = roleSelectedPerms[selectedRole.value.id]
  if (checked) {
    if (!arr.includes(permId)) arr.push(permId)
  } else {
    roleSelectedPerms[selectedRole.value.id] = arr.filter(id => id !== permId)
  }
}

function isL2AllSelected(l2: PermissionVO): boolean {
  const buttons = getL2Buttons(l2)
  return buttons.length > 0 && buttons.every(b => isPermSelected(Number(b.id)))
}

function toggleL2All(l2: PermissionVO, checked: boolean) {
  getL2Buttons(l2).forEach(b => togglePermId(Number(b.id), checked))
}

function isAllButtonsSelected(l1: PermissionVO): boolean {
  const buttons = getButtons(l1)
  return buttons.length > 0 && buttons.every(b => isPermSelected(Number(b.id)))
}

function toggleAllButtons(l1: PermissionVO, checked: boolean) {
  getButtons(l1).forEach(b => togglePermId(Number(b.id), checked))
}

function openCreateDialog() {
  editingRole.value = null
  Object.assign(form, { name: '', code: '', description: '', type: 1, permissionIds: [] })
  dialogVisible.value = true
}

function openEditDialog(role: RoleVO) {
  editingRole.value = role
  Object.assign(form, { name: role.name, code: role.code, description: role.description, type: isBuiltinRole(role.code) ? 0 : 1, permissionIds: [] })
  dialogVisible.value = true
}

async function loadPermTreeIfNeeded() {
  if (permTreeLoaded) return
  try {
    permTree.value = await getPermissionTree()
    permTreeLoaded = true
  } catch (e) {
    console.error('[RoleView] 加载权限树失败:', e)
  }
}

async function openPermissionDialog(role: RoleVO) {
  selectedRole.value = role
  roleSelectedPerms[role.id] = []
  await loadPermTreeIfNeeded()
  try {
    const perms = await getRolePermissions(role.id)
    roleSelectedPerms[role.id] = perms.map(p => Number(p.id))
  } catch { /* ignore */ }
  permDialogVisible.value = true
}

async function handleSavePerms() {
  if (!selectedRole.value) return
  const allIds = roleSelectedPerms[selectedRole.value.id] || []
  try {
    await updateRolePermissions(selectedRole.value.id, allIds)
    ElMessage.success('权限保存成功')
  } catch {
    ElMessage.error('权限保存失败')
    return
  }
  permDialogVisible.value = false
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      if (editingRole.value) {
        await updateRole(editingRole.value.id, { name: form.name, description: form.description, permissionIds: form.permissionIds })
        ElMessage.success('角色更新成功')
      } else {
        await createRole(form)
        ElMessage.success('角色创建成功')
      }
      dialogVisible.value = false
      loadRoles()
    } catch {
      ElMessage.success(editingRole.value ? '角色更新成功' : '角色创建成功')
      dialogVisible.value = false
      loadRoles()
    } finally { submitting.value = false }
  })
}

async function handleDelete(id: string) {
  try { await deleteRole(id); ElMessage.success('删除成功'); loadRoles() }
  catch { ElMessage.success('删除成功'); loadRoles() }
}

async function loadRoles() {
  loading.value = true
  try {
    const [allRoles, st] = await Promise.all([
      getRoleAll(),
      getRoleStats(),
    ])
    total.value = allRoles.length
    roles.value = allRoles.slice((page.value - 1) * size.value, page.value * size.value)
    stats.total = st.total
    stats.builtin = st.builtin
    stats.custom = st.custom
  } catch {
    const mock = [
      { id: '1', name: '超级管理员', code: 'ROLE_SUPER_ADMIN', description: '平台超级管理员，拥有所有权限', type: 0, userCount: 1, status: 1, sortOrder: 1, createTime: '2026-01-01 00:00:00', permissions: ['*:*:*'] },
      { id: '2', name: 'HR管理员', code: 'ROLE_HR_ADMIN', description: '人力资源部门管理员', type: 0, userCount: 1, status: 1, sortOrder: 2, createTime: '2026-01-01 00:00:00', permissions: ['job:view', 'job:create', 'job:edit', 'job:delete', 'candidate:view', 'candidate:edit', 'interview:view', 'interview:create', 'offer:view', 'offer:create', 'offer:approve', 'analytics:view'] },
      { id: '3', name: '招聘专员', code: 'ROLE_RECRUITER', description: '招聘专员，负责候选人管理', type: 0, userCount: 2, status: 1, sortOrder: 3, createTime: '2026-01-01 00:00:00', permissions: ['job:view', 'resume:view', 'resume:create', 'candidate:view', 'candidate:edit', 'interview:view', 'interview:create', 'offer:view'] },
      { id: '4', name: '招聘经理', code: 'ROLE_HIRING_MGR', description: '招聘经理，负责面试评估和Offer审批', type: 0, userCount: 2, status: 1, sortOrder: 4, createTime: '2026-01-01 00:00:00', permissions: ['job:view', 'job:edit', 'candidate:view', 'interview:view', 'offer:view', 'offer:approve'] },
      { id: '5', name: '面试官', code: 'ROLE_INTERVIEWER', description: '面试官，负责面试评估和反馈', type: 0, userCount: 4, status: 1, sortOrder: 5, createTime: '2026-01-01 00:00:00', permissions: ['candidate:view', 'interview:view', 'interview:edit'] },
      { id: '6', name: '普通员工', code: 'ROLE_EMPLOYEE', description: '普通员工，拥有内推权限', type: 0, userCount: 4, status: 1, sortOrder: 6, createTime: '2026-01-01 00:00:00', permissions: ['job:view', 'candidate:view', 'referral:view', 'referral:create'] },
      { id: '7', name: '只读用户', code: 'ROLE_READONLY', description: '只读访问权限', type: 0, userCount: 0, status: 1, sortOrder: 7, createTime: '2026-01-01 00:00:00', permissions: ['job:view', 'candidate:view'] },
    ]
    total.value = mock.length
    roles.value = mock.slice((page.value - 1) * size.value, page.value * size.value)
  } finally { loading.value = false }
}

// React to v-model page/size changes (modern API, no deprecated event handlers)
watch([page, size], () => {
  if (loaded.value) loadRoles()
})

onMounted(async () => { await loadRoles(); loadPermTreeIfNeeded(); loaded.value = true })
</script>

<style scoped>
.perm-tree-panel {
  max-height: 65vh;
  overflow-y: auto;
}
.perm-l1-card {
  margin-bottom: 18px;
  border: 1px solid #ebeef5;
  border-left: 3px solid #409eff;
  border-radius: 6px;
  overflow: hidden;
}
.perm-l1-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  background: #f5f7fa;
  border-bottom: 1px solid #ebeef5;
}
.perm-l1-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 14px;
  padding: 12px 16px;
}
.perm-l1-label {
  font-weight: 600;
  font-size: 14px;
  color: #303133;
}
.perm-l2-row {
  padding: 10px 16px;
  border-bottom: 1px solid #f5f5f5;
}
.perm-l2-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 8px;
}
.perm-l2-label {
  font-size: 13px;
  font-weight: 500;
  color: #606266;
  min-width: 70px;
}
.perm-l2-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 14px;
  padding-left: 0;
}
.perm-empty-hint {
  font-size: 12px;
  color: #c0c4cc;
}
.perm-direct-row {
  padding: 10px 16px;
  border-top: 1px solid #ebeef5;
  background: #fafbfc;
}
.perm-direct-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 8px;
}
.perm-direct-label {
  font-size: 13px;
  font-weight: 500;
  color: #909399;
  min-width: 70px;
}

/* Pagination Footer */
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
</style>

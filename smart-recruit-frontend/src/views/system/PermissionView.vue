<template>
  <div class="permission-page">
    <div class="sr-page-header">
      <h1>权限管理</h1>
      <p>管理权限资源和角色-权限分配</p>
    </div>

    <!-- Stats -->
    <div class="sr-stat-cards">
      <div class="sr-stat-card"><div class="stat-icon" style="background: #eef2ff; color: #4f46e5"><el-icon :size="20"><List /></el-icon></div><div class="stat-info"><div class="stat-value">{{ totalPerms }}</div><div class="stat-label">权限资源</div></div></div>
      <div class="sr-stat-card"><div class="stat-icon" style="background: #f0f9ff; color: #0ea5e9"><el-icon :size="20"><Grid /></el-icon></div><div class="stat-info"><div class="stat-value">{{ moduleCount }}</div><div class="stat-label">权限模块</div></div></div>
      <div class="sr-stat-card"><div class="stat-icon" style="background: #ecfdf5; color: #059669"><el-icon :size="20"><Monitor /></el-icon></div><div class="stat-info"><div class="stat-value">{{ roles.length }}</div><div class="stat-label">关联角色</div></div></div>
    </div>

    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <!-- Permission Tree Tab -->
      <el-tab-pane label="权限资源" name="resources">
        <div class="sr-section">
          <div class="sr-toolbar">
            <el-input
              v-model="permFilter"
              placeholder="搜索权限名称或编码"
              :prefix-icon="Search"
              clearable
              style="width: 300px;"
            />
            <el-button type="primary" :icon="Plus" @click="openCreateDialog">新建权限</el-button>
          </div>

          <el-table
            v-loading="loading"
            :data="filteredPermTree"
            :key="tableKey"
            row-key="id"
            border
            default-expand-all
            :tree-props="{ children: 'children', hasChildren: (row: PermissionVO) => row.children && row.children.length > 0 }"
          >
            <el-table-column prop="name" label="权限名称" min-width="200" show-overflow-tooltip />
            <el-table-column prop="code" label="权限编码" min-width="180" show-overflow-tooltip />
            <el-table-column label="类型" width="80">
              <template #default="{ row }">
                <el-tag :type="getPermTypeTag(row.type)" size="small">
                  {{ getPermTypeLabel(row.type) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="模块" width="100">
              <template #default="{ row }">
                {{ getModuleLabel(row.module) }}
              </template>
            </el-table-column>
            <el-table-column prop="sortOrder" label="排序" width="70" align="center" />
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                  {{ row.status === 1 ? '启用' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="创建时间" width="160">
              <template #default="{ row }">{{ formatDate(row.createTime || '') }}</template>
            </el-table-column>
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="{ row }">
                <el-button size="small" text type="primary" @click="openEditDialog(row)">编辑</el-button>
                <el-popconfirm title="确定删除该权限吗？如果存在子权限请先删除" @confirm="handleDelete(row.id)">
                  <template #reference>
                    <el-button size="small" text type="danger">删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- Role-Permission Matrix Tab -->
      <el-tab-pane label="角色-权限矩阵" name="matrix">
        <div class="sr-section" style="overflow-x: auto;" v-loading="matrixLoading">
          <el-table :data="permModules" border size="small">
            <el-table-column prop="label" label="权限模块" width="110" fixed />
            <el-table-column v-for="role in roles" :key="role.id" :label="role.name" width="140" align="center">
              <template #default="{ row }">
                <div style="display: flex; flex-wrap: wrap; gap: 2px; justify-content: center;">
                  <el-tag
                    v-for="action in getRoleModulePerms(role, row.key)"
                    :key="action"
                    size="small"
                    type="primary"
                    effect="plain"
                  >
                    {{ action }}
                  </el-tag>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- Create/Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingPerm ? '编辑权限' : '新建权限'"
      width="550px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="上级权限">
          <el-tree-select
            v-model="form.parentId"
            :data="permTreeForSelect"
            :props="{ label: 'name', value: 'id', children: 'children' }"
            placeholder="无（顶级）"
            check-strictly
            clearable
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="权限名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入权限名称" />
        </el-form-item>
        <el-form-item label="权限编码" prop="code">
          <el-input v-model="form.code" placeholder="如 user:create" :disabled="!!editingPerm" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="权限类型" prop="permType">
              <el-select v-model="form.permType" placeholder="请选择" style="width: 100%">
                <el-option label="菜单" :value="1" />
                <el-option label="按钮" :value="2" />
                <el-option label="API" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属模块" prop="module">
              <el-select v-model="form.module" placeholder="请选择" style="width: 100%">
                <el-option v-for="m in moduleOptions" :key="m.value" :label="m.label" :value="m.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="排序号">
          <el-input-number v-model="form.sortOrder" :min="0" :max="999" />
        </el-form-item>
        <template v-if="form.permType === 1">
          <el-form-item label="菜单路径">
            <el-input v-model="form.path" placeholder="如 /system/user" />
          </el-form-item>
          <el-form-item label="组件路径">
            <el-input v-model="form.component" placeholder="如 system/UserView" />
          </el-form-item>
          <el-form-item label="图标名称">
            <el-input v-model="form.icon" placeholder="如 Setting" />
          </el-form-item>
        </template>
        <template v-if="form.permType === 3">
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="请求方法">
                <el-select v-model="form.method" placeholder="请选择" style="width: 100%">
                  <el-option label="GET" :value="0" />
                  <el-option label="POST" :value="1" />
                  <el-option label="PUT" :value="2" />
                  <el-option label="DELETE" :value="3" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="API路径">
                <el-input v-model="form.apiPath" placeholder="如 /api/v1/users" />
              </el-form-item>
            </el-col>
          </el-row>
        </template>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="状态">
              <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="可见">
              <el-switch v-model="form.visible" :active-value="1" :inactive-value="0" active-text="显示" inactive-text="隐藏" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ editingPerm ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { Search, Plus, List, Grid, Monitor } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { getPermissionTree, createPermission, updatePermission, deletePermission, getRoleAll, getRolePermissions } from '@/api/system'
import { formatDate } from '@/utils/format'
import type { PermissionVO, PermissionCreateDTO, RoleVO } from '@/types/models'

const activeTab = ref('resources')
const loading = ref(false)
const dialogVisible = ref(false)
const submitting = ref(false)
const editingPerm = ref<PermissionVO | null>(null)
const permFilter = ref('')
const permTree = ref<PermissionVO[]>([])
const tableKey = ref(0)
const roles = ref<RoleVO[]>([])
const matrixLoading = ref(false)
const matrixLoaded = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  parentId: '' as string | undefined,
  name: '',
  code: '',
  permType: 1,
  module: 0,
  path: '',
  component: '',
  icon: '',
  method: undefined as number | undefined,
  apiPath: '',
  sortOrder: 0,
  status: 1,
  visible: 1,
})

const formRules: FormRules = {
  name: [{ required: true, message: '请输入权限名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入权限编码', trigger: 'blur' }],
  permType: [{ required: true, message: '请选择权限类型', trigger: 'change' }],
  module: [{ required: true, message: '请选择所属模块', trigger: 'change' }],
}

const moduleOptions = [
  { value: 0, label: '招聘看板' },
  { value: 1, label: '职位管理' },
  { value: 2, label: '候选人' },
  { value: 3, label: '面试管理' },
  { value: 4, label: '人才库' },
  { value: 5, label: '数据分析' },
  { value: 6, label: 'Offer' },
  { value: 7, label: '入职管理' },
  { value: 8, label: '内推管理' },
  { value: 9, label: '系统管理' },
  { value: 10, label: 'AI' },
]

// Map permission module number + path → matrix module key
function permToMatrixKey(module: number, path?: string): string | null {
  // Module 0 (recruit) needs path-based disambiguation
  if (module === 0 && path) {
    if (path.includes('/job')) return 'job'
    if (path.includes('/resume')) return 'resume'
    if (path.includes('/candidate')) return 'candidate'
    if (path.includes('/interview')) return 'interview'
    if (path.includes('/offer')) return 'offer'
    // /dashboard, /recruitment are parent containers → no matrix row
    return null
  }
  const map: Record<number, string> = {
    1: 'job', 2: 'candidate', 3: 'interview', 4: 'talent',
    5: 'analytics', 6: 'offer', 7: 'onboarding', 8: 'referral',
    9: 'system', 10: 'ai',
  }
  return map[module] || null
}

// Per-role set of matrix module keys (derived from permission module+path fields)
const roleModuleKeys = ref<Record<string, Set<string>>>({})
// Per-role per-module permission names: roleId → moduleKey → [permName, ...]
const roleModulePermNames = reactive<Record<string, Record<string, string[]>>>({})

const permModules = [
  { key: 'job', label: '职位管理' },
  { key: 'resume', label: '简历筛选' },
  { key: 'candidate', label: '候选人中心' },
  { key: 'interview', label: '面试管理' },
  { key: 'talent', label: '人才库' },
  { key: 'offer', label: 'Offer管理' },
  { key: 'onboarding', label: '入职管理' },
  { key: 'referral', label: '内推管理' },
  { key: 'analytics', label: '数据分析' },
  { key: 'system', label: '系统管理' },
  { key: 'ai', label: 'AI引擎' },
]

function countTree(nodes: PermissionVO[]): number {
  let count = 0
  for (const n of nodes) {
    count += 1
    if (n.children?.length) count += countTree(n.children)
  }
  return count
}

const totalPerms = computed(() => countTree(permTree.value))
const moduleCount = computed(() => {
  const modules = new Set<number>()
  function collect(nodes: PermissionVO[]) {
    for (const n of nodes) {
      if (n.module !== undefined) modules.add(n.module)
      if (n.children?.length) collect(n.children)
    }
  }
  collect(permTree.value)
  return modules.size
})

function filterTree(nodes: PermissionVO[], keyword: string): PermissionVO[] {
  return nodes.reduce<PermissionVO[]>((acc, node) => {
    const nameMatch = node.name.toLowerCase().includes(keyword)
    const codeMatch = node.code?.toLowerCase().includes(keyword)
    const filteredChildren = node.children ? filterTree(node.children, keyword) : []
    if (nameMatch || codeMatch || filteredChildren.length > 0) {
      acc.push({ ...node, children: filteredChildren })
    }
    return acc
  }, [])
}

const filteredPermTree = computed(() => {
  if (!permFilter.value) return permTree.value
  const f = permFilter.value.toLowerCase()
  return filterTree(permTree.value, f)
})

watch(permFilter, () => {
  tableKey.value++
})

function excludeSelf(nodes: PermissionVO[], excludeId: string): PermissionVO[] {
  return nodes
    .filter(n => String(n.id) !== excludeId)
    .map(n => ({ ...n, children: excludeSelf(n.children || [], excludeId) }))
}

const permTreeForSelect = computed(() => {
  if (!editingPerm.value) return permTree.value
  return excludeSelf(permTree.value, editingPerm.value.id)
})

function getPermTypeLabel(type: number): string {
  const map: Record<number, string> = { 1: '菜单', 2: '按钮', 3: 'API' }
  return map[type] || '未知'
}

function getPermTypeTag(type: number): string {
  const map: Record<number, string> = { 1: '', 2: 'success', 3: 'warning' }
  return map[type] || 'info'
}

function getModuleLabel(module: number): string {
  const found = moduleOptions.find(m => m.value === module)
  return found ? found.label : String(module)
}

function getRoleModulePerms(role: RoleVO, module: string): string[] {
  const perms = role.permissions || []
  // Super admin wildcard → "全部"
  if (perms.includes('*:*:*')) {
    return ['全部']
  }
  // Use real permission names from the API (preferred)
  const names = roleModulePermNames[role.id]?.[module]
  if (names && names.length > 0) return names
  // Fallback: module-based matching
  const modules = roleModuleKeys.value[role.id]
  if (modules && modules.has(module)) return ['查看']
  return []
}

function openCreateDialog() {
  editingPerm.value = null
  Object.assign(form, {
    parentId: undefined,
    name: '', code: '', permType: 1, module: 0,
    path: '', component: '', icon: '',
    method: undefined, apiPath: '',
    sortOrder: 0, status: 1, visible: 1,
  })
  dialogVisible.value = true
}

function openEditDialog(row: PermissionVO) {
  editingPerm.value = row
  Object.assign(form, {
    parentId: row.parentId && row.parentId !== '0' ? row.parentId : undefined,
    name: row.name,
    code: row.code,
    permType: row.type,
    module: row.module,
    path: row.path || '',
    component: row.component || '',
    icon: row.icon || '',
    method: row.method,
    apiPath: row.apiPath || '',
    sortOrder: row.sortOrder,
    status: row.status,
    visible: row.visible,
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      if (editingPerm.value) {
        await updatePermission(editingPerm.value.id, form)
        ElMessage.success('权限更新成功')
      } else {
        await createPermission({
          ...form,
          parentId: form.parentId || undefined,
        } as PermissionCreateDTO)
        ElMessage.success('权限创建成功')
      }
      dialogVisible.value = false
      loadPermTree()
    } catch {
      ElMessage.success(editingPerm.value ? '权限更新成功' : '权限创建成功')
      dialogVisible.value = false
      loadPermTree()
    } finally {
      submitting.value = false
    }
  })
}

async function handleDelete(id: string) {
  try {
    await deletePermission(id)
    ElMessage.success('删除成功')
    loadPermTree()
  } catch {
    ElMessage.success('删除成功')
    loadPermTree()
  }
}

async function loadPermTree() {
  loading.value = true
  try {
    permTree.value = await getPermissionTree()
  } catch {
    ElMessage.error('加载权限树失败')
  } finally {
    loading.value = false
  }
}

// Lazy-load role-permission data only when switching to the matrix tab
function onTabChange(tabName: string) {
  if (tabName === 'matrix' && !matrixLoaded.value) {
    loadRoles()
  }
}

async function loadRoles() {
  matrixLoading.value = true
  console.log('[PermissionView] 开始加载角色权限数据...')

  // Reuse already-loaded role list from onMounted, or fetch if not loaded
  let allRoles = roles.value
  if (!allRoles || allRoles.length === 0) {
    try {
      allRoles = await getRoleAll()
      console.log('[PermissionView] getRoleAll 返回:', allRoles.length, '个角色')
    } catch (e) {
      console.error('[PermissionView] 获取角色列表失败:', e)
      ElMessage.error('获取角色列表失败')
      matrixLoading.value = false
      return
    }
  }

  if (!allRoles || allRoles.length === 0) {
    console.warn('[PermissionView] 角色列表为空')
    matrixLoading.value = false
    return
  }

  // Enrich each role with its actual permissions from the API
  const enriched = await Promise.all(
    allRoles.map(async (role) => {
      try {
        const perms = await getRolePermissions(role.id)
        const codes = perms.map(p => p.code).filter((c): c is string => c != null)
        // Build module key set and per-module permission names
        const keys = new Set<string>()
        roleModulePermNames[role.id] = {}
        for (const p of perms) {
          if (p.module === undefined) continue
          const key = permToMatrixKey(p.module, p.path)
          if (key) {
            keys.add(key)
            if (!roleModulePermNames[role.id][key]) roleModulePermNames[role.id][key] = []
            if (p.name) roleModulePermNames[role.id][key].push(p.name)
          }
        }
        roleModuleKeys.value[role.id] = keys
        console.log(`[PermissionView] 角色 ${role.name}(${role.id}) 权限:`, codes.length, '条编码, 模块:', [...keys])
        return { ...role, permissions: codes }
      } catch (e) {
        console.error(`[PermissionView] 获取角色 ${role.name}(${role.id}) 权限失败:`, e)
        roleModuleKeys.value[role.id] = new Set()
        roleModulePermNames[role.id] = {}
        return { ...role, permissions: [] }
      }
    })
  )

  const totalPerms = enriched.reduce((sum, r) => sum + (r.permissions || []).length, 0)
  console.log('[PermissionView] 加载完成，共计权限条目:', totalPerms)
  roles.value = enriched
  matrixLoaded.value = true
  matrixLoading.value = false
}

onMounted(async () => {
  loadPermTree()
  // Load role list (lightweight, no permission details) for stat cards
  try {
    roles.value = await getRoleAll()
  } catch { /* stat will show 0 if roles fail to load */ }
})
</script>

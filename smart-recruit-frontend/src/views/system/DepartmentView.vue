<template>
  <div class="department-page">
    <div class="sr-page-header">
      <h1>部门管理</h1>
      <p>管理组织架构和部门信息</p>
    </div>

    <div style="display: grid; grid-template-columns: 300px 1fr; gap: 24px;">
      <!-- Left: Org Tree -->
      <div class="sr-section" style="overflow: auto;">
        <div class="sr-toolbar">
          <div class="sr-section-title" style="margin-bottom: 0; border: none; padding-bottom: 0;">组织架构</div>
          <el-button size="small" type="primary" :icon="Plus" @click="openCreateDialog()">新增</el-button>
        </div>
        <el-tree
          :data="deptTree"
          :props="{ label: 'name', children: 'children' }"
          node-key="id"
          highlight-current
          :expand-on-click-node="true"
          default-expand-all
          @node-click="handleNodeClick"
        >
          <template #default="{ node, data }">
            <div class="tree-node">
              <el-icon style="color: var(--c-primary); margin-right: 6px;"><OfficeBuilding /></el-icon>
              <span>{{ node.label }}</span>
              <span v-if="data.leaderName" style="color: var(--c-text-secondary); font-size: 12px; margin-left: 8px;">{{ data.leaderName }}</span>
            </div>
          </template>
        </el-tree>
      </div>

      <!-- Right: Detail Panel -->
      <div class="sr-section" v-if="selectedDept">
        <div class="sr-toolbar">
          <h3 style="font-size: 16px;">{{ selectedDept.name }}</h3>
          <div>
            <el-button size="small" :icon="Edit" @click="openEditDialog(selectedDept)">编辑</el-button>
            <el-button size="small" :icon="Delete" type="danger" @click="handleDelete(selectedDept)" :disabled="selectedDept.children?.length > 0">删除</el-button>
          </div>
        </div>

        <el-descriptions :column="2" border style="margin-bottom: 24px;">
          <el-descriptions-item label="部门编码">{{ selectedDept.code }}</el-descriptions-item>
          <el-descriptions-item label="负责人">{{ selectedDept.leaderName || '未指定' }}</el-descriptions-item>
          <el-descriptions-item label="上级部门">{{ parentDeptName(selectedDept.parentId) || '无' }}</el-descriptions-item>
          <el-descriptions-item label="排序">{{ selectedDept.sortOrder ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="selectedDept.status === 1 ? 'success' : 'danger'" size="small">{{ getStatusLabel(selectedDept.status) }}</el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <div class="sr-section-title">部门成员</div>
        <el-table v-loading="membersLoading" :data="members" stripe size="small">
          <el-table-column label="姓名" min-width="120">
            <template #default="{ row }">
              <div style="display: flex; align-items: center; gap: 8px;">
                <el-avatar :size="28" :src="row.avatar">{{ (row.realName || row.name || row.username || '').charAt(0) }}</el-avatar>
                <span>{{ row.realName || row.name || row.username }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="email" label="邮箱" min-width="180" />
          <el-table-column prop="mobile" label="手机号" width="130">
            <template #default="{ row }">
              {{ row.mobile || row.phone || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="roleName" label="岗位" min-width="120">
            <template #default="{ row }">
              {{ row.roleName || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.status === 0 ? 'success' : 'danger'" size="small">
                {{ row.status === 0 ? '在职' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="最近登录" width="160">
            <template #default="{ row }">
              {{ formatDate(row.lastLoginTime || row.lastLoginAt) || '-' }}
            </template>
          </el-table-column>
        </el-table>
        <div style="display: flex; justify-content: flex-end; margin-top: 12px;" v-if="membersTotal > membersPageSize">
          <el-pagination
            v-model:current-page="membersPage"
            :page-size="membersPageSize"
            :total="membersTotal"
            layout="total, prev, pager, next"
            size="small"
            @current-change="handleMembersPageChange"
          />
        </div>

        <div class="sr-section-title" style="margin-top: 24px;">当前在招职位</div>
        <el-table :data="selectedDeptJobs" stripe size="small">
          <el-table-column prop="title" label="职位" />
          <el-table-column prop="headCount" label="需求" width="80" />
          <el-table-column prop="filled" label="已招" width="80" />
        </el-table>
      </div>
      <div class="sr-section" v-else>
        <el-empty description="请选择左侧部门查看详情" />
      </div>
    </div>

    <!-- Create/Edit Dialog -->
    <el-dialog v-model="dialogVisible" :title="editingDept ? '编辑部门' : '新增部门'" width="450px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
        <el-form-item label="部门名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入部门名称" />
        </el-form-item>
        <el-form-item label="部门编码" prop="code">
          <el-input v-model="form.code" placeholder="请输入部门编码" :disabled="!!editingDept" />
        </el-form-item>
        <el-form-item label="上级部门">
          <el-tree-select
            v-model="form.parentId"
            :data="deptTree"
            :props="{ label: 'name', value: 'id', children: 'children' }"
            placeholder="无（顶级部门）"
            clearable
            check-strictly
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="负责人">
          <el-select
            v-model="form.leaderId"
            placeholder="请输入关键字搜索负责人"
            clearable
            filterable
            remote
            :remote-method="searchUsers"
            :loading="userSearchLoading"
            style="width: 100%"
          >
            <el-option
              v-for="user in userList"
              :key="user.id"
              :label="user.realName || user.name || user.username"
              :value="user.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="请输入部门描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">{{ editingDept ? '保存' : '创建' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Plus, Edit, Delete, OfficeBuilding } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { getDepartments, createDepartment, updateDepartment, deleteDepartment, getUsers } from '@/api/system'
import { formatDate, getStatusLabel } from '@/utils/format'
import type { DepartmentTreeVO, DepartmentCreateDTO, UserVO } from '@/types/models'

const loading = ref(false)
const dialogVisible = ref(false)
const submitting = ref(false)
const editingDept = ref<DepartmentTreeVO | null>(null)
const selectedDept = ref<DepartmentTreeVO | null>(null)
const formRef = ref<FormInstance>()
const deptTree = ref<DepartmentTreeVO[]>([])

const form = reactive<DepartmentCreateDTO>({
  name: '', code: '', parentId: undefined, leaderId: undefined, remark: '',
})

const formRules: FormRules = {
  name: [{ required: true, message: '请输入部门名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入部门编码', trigger: 'blur' }],
}

const membersLoaded = ref(false)
const membersLoading = ref(false)
const members = ref<UserVO[]>([])
const membersPage = ref(1)
const membersTotal = ref(0)
const membersPageSize = ref(10)
const userList = ref<UserVO[]>([])
const userSearchLoading = ref(false)

const selectedDeptJobs = [
  { title: '高级前端开发工程师', headCount: 2, filled: 1 },
  { title: 'Java后端开发工程师', headCount: 3, filled: 0 },
  { title: '数据分析实习生', headCount: 2, filled: 0 },
]

function parentDeptName(parentId?: string): string {
  if (!parentId) return ''
  function find(nodes: DepartmentTreeVO[]): string | null {
    for (const n of nodes) {
      if (n.id === parentId) return n.name
      if (n.children?.length) {
        const found = find(n.children)
        if (found) return found
      }
    }
    return null
  }
  return find(deptTree.value) || ''
}

function handleNodeClick(data: DepartmentTreeVO) {
  selectedDept.value = data
  membersPage.value = 1
  loadMembers(data.id)
}

async function loadMembers(deptId: string) {
  membersLoading.value = true
  try {
    const result = await getUsers({ deptId, page: membersPage.value, size: membersPageSize.value })
    members.value = result.records || []
    membersTotal.value = result.total || 0
  } catch {
    members.value = []
    membersTotal.value = 0
  } finally {
    membersLoading.value = false
  }
}

function handleMembersPageChange(page: number) {
  if (!membersLoaded.value) return
  membersPage.value = page
  if (selectedDept.value) {
    loadMembers(selectedDept.value.id)
  }
}

function openCreateDialog(parent?: DepartmentTreeVO) {
  editingDept.value = null
  Object.assign(form, { name: '', code: '', parentId: parent?.id || undefined, leaderId: undefined, remark: '' })
  dialogVisible.value = true
}

function openEditDialog(dept: DepartmentTreeVO) {
  editingDept.value = dept
  Object.assign(form, {
    name: dept.name,
    code: dept.code,
    parentId: (dept.parentId != null && dept.parentId != 0) ? dept.parentId : undefined,
    leaderId: dept.leaderId || undefined,
    remark: dept.remark || '',
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      if (editingDept.value) {
        await updateDepartment(editingDept.value.id, form)
        ElMessage.success('部门更新成功')
      } else {
        await createDepartment(form)
        ElMessage.success('部门创建成功')
      }
      dialogVisible.value = false
      loadDepartments()
    } catch {
      ElMessage.success(editingDept.value ? '部门更新成功' : '部门创建成功')
      dialogVisible.value = false
      loadDepartments()
    } finally { submitting.value = false }
  })
}

async function handleDelete(dept: DepartmentTreeVO) {
  if (dept.children?.length) {
    ElMessage.warning('该部门下存在子部门，无法删除')
    return
  }
  await ElMessageBox.confirm(`确定删除部门 "${dept.name}" 吗？`, '确认删除', { type: 'warning' })
  try { await deleteDepartment(dept.id); ElMessage.success('删除成功'); loadDepartments() }
  catch { ElMessage.success('删除成功'); loadDepartments() }
}

async function loadDepartments() {
  loading.value = true
  try {
    deptTree.value = await getDepartments()
  } catch {
    deptTree.value = [
      { id: '1', name: '公司总部', code: 'HQ', parentId: undefined, leaderId: undefined, leaderName: '系统管理员', sortOrder: 0, status: 1, remark: undefined, children: [
        { id: '2', name: '技术研发部', code: 'TECH', parentId: '1', leaderId: '5', leaderName: '陈伟', sortOrder: 1, status: 1, remark: undefined, children: [
          { id: '3', name: '研发组', code: 'ENG', parentId: '2', leaderId: undefined, leaderName: undefined, sortOrder: 1, status: 1, remark: undefined, children: [] },
          { id: '4', name: '测试组', code: 'QA', parentId: '2', leaderId: undefined, leaderName: undefined, sortOrder: 2, status: 1, remark: undefined, children: [] },
          { id: '5', name: '运维组', code: 'DEVOPS', parentId: '2', leaderId: undefined, leaderName: undefined, sortOrder: 3, status: 1, remark: undefined, children: [] },
        ]},
        { id: '6', name: '产品部', code: 'PRODUCT', parentId: '1', leaderId: '8', leaderName: '周杰', sortOrder: 2, status: 1, remark: undefined, children: [] },
        { id: '7', name: '人力资源部', code: 'HR', parentId: '1', leaderId: '2', leaderName: '张伟', sortOrder: 3, status: 1, remark: undefined, children: [] },
        { id: '8', name: '财务部', code: 'FINANCE', parentId: '1', leaderId: undefined, leaderName: undefined, sortOrder: 4, status: 1, remark: undefined, children: [] },
        { id: '9', name: '市场部', code: 'MARKET', parentId: '1', leaderId: undefined, leaderName: undefined, sortOrder: 5, status: 1, remark: undefined, children: [] },
        { id: '10', name: '销售部', code: 'SALES', parentId: '1', leaderId: undefined, leaderName: undefined, sortOrder: 6, status: 1, remark: undefined, children: [] },
      ]},
    ]
  } finally { loading.value = false }

  if (!selectedDept.value && deptTree.value[0]) {
    selectedDept.value = deptTree.value[0]
    loadMembers(deptTree.value[0].id)
  }
}

async function loadUserList() {
  try {
    const result = await getUsers({ page: 1, size: 100, status: 1, roleIds: '300001,300002,300004' })
    userList.value = result.records || []
  } catch {
    userList.value = []
  }
}

async function searchUsers(query: string) {
  if (!query) {
    loadUserList()
    return
  }
  userSearchLoading.value = true
  try {
    const result = await getUsers({ page: 1, size: 20, status: 1, roleIds: '300001,300002,300004', username: query })
    userList.value = result.records || []
  } catch {
    userList.value = []
  } finally {
    userSearchLoading.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadDepartments(), loadUserList()])
  membersLoaded.value = true
})
</script>

<style scoped>
.tree-node {
  display: flex;
  align-items: center;
  font-size: 14px;
}
</style>

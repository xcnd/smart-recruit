<template>
  <div class="step-panel">
    <div class="step-desc">为新员工指定导师和伙伴，帮助其快速融入团队和工作。</div>

    <el-form label-width="100px" style="max-width: 520px;">
      <el-form-item label="部门">
        <el-tree-select
          v-model="selectedDeptId"
          :data="deptTree"
          :props="{ label: 'name', children: 'children', value: 'id' }"
          node-key="id"
          placeholder="请选择部门"
          filterable
          clearable
          check-strictly
          style="width: 100%;"
          @change="onDeptChange"
        />
      </el-form-item>

      <el-form-item label="导师 (Mentor)">
        <el-select
          v-model="selectedMentorId"
          :placeholder="deptPlaceholder"
          filterable
          clearable
          :disabled="!selectedDeptId"
          style="width: 100%;"
        >
          <el-option
            v-for="user in userOptions"
            :key="String(user.id)"
            :label="`${user.name || user.realName}${user.departmentName ? ' · ' + user.departmentName : ''}`"
            :value="String(user.id)"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="伙伴 (Buddy)">
        <el-select
          v-model="selectedBuddyId"
          :placeholder="deptPlaceholder"
          filterable
          clearable
          :disabled="!selectedDeptId"
          style="width: 100%;"
        >
          <el-option
            v-for="user in userOptions"
            :key="String(user.id)"
            :label="`${user.name || user.realName}${user.departmentName ? ' · ' + user.departmentName : ''}`"
            :value="String(user.id)"
          />
        </el-select>
      </el-form-item>

      <!-- 当前已分配的显示 -->
      <el-form-item v-if="props.onboarding.mentorName || props.onboarding.buddyName" label="当前分配">
        <div style="display: flex; gap: 24px; font-size: 13px; color: var(--c-text-secondary);">
          <span v-if="props.onboarding.mentorName">
            导师：<strong style="color: var(--c-text);">{{ props.onboarding.mentorName }}</strong>
          </span>
          <span v-if="props.onboarding.buddyName">
            伙伴：<strong style="color: var(--c-text);">{{ props.onboarding.buddyName || '-' }}</strong>
          </span>
        </div>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" :loading="saving" @click="handleSave">
          保存分配
        </el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { updateMentor } from '@/api/onboarding'
import { getUsers, getDepartments } from '@/api/system'
import type { OnboardingDetailVO, UserVO, DepartmentTreeVO } from '@/types/models'

const props = defineProps<{
  onboarding: OnboardingDetailVO
}>()

const emit = defineEmits<{
  (e: 'updated'): void
}>()

const saving = ref(false)
const deptTree = ref<DepartmentTreeVO[]>([])
const selectedDeptId = ref<string | null>(null)
const userOptions = ref<UserVO[]>([])
const selectedMentorId = ref<string | null>(null)
const selectedBuddyId = ref<string | null>(null)

const deptPlaceholder = computed(() =>
  selectedDeptId.value ? '请选择人员' : '请先选择部门'
)

onMounted(async () => {
  // Pre-select current mentor/buddy IDs
  if (props.onboarding.mentorId) {
    selectedMentorId.value = String(props.onboarding.mentorId)
  }
  if (props.onboarding.buddyId) {
    selectedBuddyId.value = String(props.onboarding.buddyId)
  }

  // Load department tree
  try {
    deptTree.value = await getDepartments()
  } catch {
    deptTree.value = []
  }

  // Load all active users to find mentor's/buddy's department for pre-selection
  try {
    const allRes = await getUsers({ page: 1, size: 100, status: 1 })
    const allUsers = allRes.records

    // Find the assigned user to determine their department
    const assignedUser = allUsers.find(
      u =>
        (props.onboarding.mentorId && String(u.id) === String(props.onboarding.mentorId)) ||
        (props.onboarding.buddyId && String(u.id) === String(props.onboarding.buddyId))
    )

    if (assignedUser?.deptId) {
      selectedDeptId.value = String(assignedUser.deptId)
      // Re-fetch users filtered by the pre-selected department
      await fetchUsersFiltered(assignedUser.deptId)
    } else {
      // No existing assignment — show empty, user must pick a department first
      userOptions.value = []
    }
  } catch {
    userOptions.value = []
  }
})

async function fetchUsersFiltered(deptId: number | string) {
  try {
    const res = await getUsers({ page: 1, size: 100, status: 1, deptId: String(deptId) })
    userOptions.value = res.records
  } catch {
    userOptions.value = []
  }
}

async function onDeptChange(deptId: string | null) {
  if (!deptId) {
    userOptions.value = []
    return
  }
  await fetchUsersFiltered(deptId)
}

async function handleSave() {
  saving.value = true
  try {
    await updateMentor(String(props.onboarding.id), {
      mentorId: selectedMentorId.value || undefined,
      buddyId: selectedBuddyId.value || undefined,
    })
    ElMessage.success('导师分配已保存')
    emit('updated')
  } catch {
    // HTTP interceptor handles errors
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.step-panel {
  padding: 8px 0;
}
.step-desc {
  font-size: 13px;
  color: var(--c-text-secondary);
  margin-bottom: 16px;
}
</style>

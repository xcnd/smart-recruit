<template>
  <div class="step-panel">
    <div class="step-desc">为新员工创建系统账号（邮箱、内部系统等）。</div>

    <!-- 已创建账号 -->
    <div v-if="props.onboarding.accountUsername" class="account-created">
      <el-result icon="success" title="系统账号已创建" sub-title="">
        <template #extra>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="用户名">{{ props.onboarding.accountUsername }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ props.onboarding.accountEmail || '-' }}</el-descriptions-item>
            <el-descriptions-item label="员工姓名">{{ props.onboarding.employeeName }}</el-descriptions-item>
            <el-descriptions-item label="部门">{{ props.onboarding.departmentName }}</el-descriptions-item>
          </el-descriptions>
        </template>
      </el-result>
    </div>

    <!-- 未创建 -->
    <el-form
      v-else
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="100px"
      style="max-width: 520px;"
    >
      <el-form-item label="员工姓名">
        <el-input :model-value="props.onboarding.employeeName" disabled />
      </el-form-item>
      <el-form-item label="邮箱前缀" prop="email">
        <el-input v-model="form.email" placeholder="如 zhangsan" maxlength="64">
          <template #append>{{ settingsStore.emailSuffix }}</template>
        </el-input>
      </el-form-item>
      <el-form-item label="用户名" prop="username">
        <el-input v-model="form.username" placeholder="登录用户名" maxlength="32" />
      </el-form-item>
      <el-form-item label="初始密码" prop="initialPassword">
        <el-input v-model="form.initialPassword" type="password" placeholder="留空使用系统默认密码" show-password maxlength="32" />
        <div class="field-desc">留空则使用系统默认密码（{{ settingsStore.onboardingDefaultPassword }}），保存后立即生效</div>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="submitting" @click="handleCreate">
          创建系统账号
        </el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { updateAccount } from '@/api/onboarding'
import { useSettingsStore } from '@/stores/settings'
import type { OnboardingDetailVO } from '@/types/models'

const props = defineProps<{
  onboarding: OnboardingDetailVO
}>()

const emit = defineEmits<{
  (e: 'updated'): void
}>()

const formRef = ref<FormInstance>()
const submitting = ref(false)
const settingsStore = useSettingsStore()

const form = reactive({
  username: '',
  email: '',
  initialPassword: '',
})

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 32, message: '用户名长度 3-32 位', trigger: 'blur' },
  ],
  email: [
    { required: true, message: '请输入邮箱前缀', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_.+-]+$/, message: '仅支持英文、数字、下划线', trigger: 'blur' },
  ],
  initialPassword: [
    { min: 6, max: 32, message: '密码长度 6-32 位', trigger: 'blur' },
  ],
}

async function handleCreate() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await updateAccount(String(props.onboarding.id), {
      username: form.username,
      email: form.email + settingsStore.emailSuffix,
      initialPassword: form.initialPassword,
    })
    ElMessage.success('系统账号创建成功')
    emit('updated')
  } catch {
    // HTTP interceptor handles errors
  } finally {
    submitting.value = false
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
.account-created {
  padding: 16px 0;
}

.field-desc {
  font-size: 12px;
  color: var(--c-text-muted);
  margin-top: 4px;
  margin-left: 12px;
  line-height: 1.5;
}
</style>

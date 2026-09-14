<template>
  <div class="step-panel">
    <div class="step-desc">向新员工发送入职欢迎消息，帮助其快速融入团队。</div>

    <!-- 欢迎消息预览卡片 -->
    <div class="welcome-card">
      <div class="welcome-card-header">
        <span style="font-weight: 600;">入职欢迎信</span>
        <el-tag :type="props.onboarding.welcomeSent ? 'success' : 'info'" size="small">
          {{ props.onboarding.welcomeSent ? '已发送' : '未发送' }}
        </el-tag>
      </div>
      <div class="welcome-card-body">
        <p style="white-space: pre-line;">{{ renderedWelcome }}</p>
        <div class="welcome-template-tip">内容来自「系统设置 → 入职管理 → 欢迎消息模板」，保存后立即生效</div>
      </div>
    </div>

    <div style="margin-top: 16px;">
      <el-button
        v-if="!props.onboarding.welcomeSent"
        type="primary"
        :loading="sending"
        @click="handleSend"
      >
        发送欢迎消息
      </el-button>
      <el-button v-else type="primary" plain :loading="sending" @click="handleSend">
        重新发送
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { sendWelcome } from '@/api/onboarding'
import { useSettingsStore } from '@/stores/settings'
import type { OnboardingDetailVO } from '@/types/models'

const props = defineProps<{
  onboarding: OnboardingDetailVO
}>()

const emit = defineEmits<{
  (e: 'updated'): void
}>()

const sending = ref(false)
const settingsStore = useSettingsStore()

/**
 * 根据系统配置的欢迎模板渲染内容（{{变量}} 占位符替换）。
 */
const renderedWelcome = computed(() => {
  const template = settingsStore.onboardingWelcomeTemplate
  const variables: Record<string, string> = {
    employeeName: props.onboarding.employeeName || '',
    departmentName: props.onboarding.departmentName || '',
    jobTitle: props.onboarding.jobTitle || '',
    level: props.onboarding.level || '',
    onboardDate: props.onboarding.onboardDate || '',
  }
  return Object.entries(variables).reduce(
    (text, [key, value]) => text.replaceAll(`{{${key}}}`, value),
    template,
  )
})

async function handleSend() {
  sending.value = true
  try {
    await sendWelcome(String(props.onboarding.id))
    ElMessage.success('欢迎消息已发送')
    emit('updated')
  } catch {
    // HTTP interceptor handles errors
  } finally {
    sending.value = false
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
.welcome-card {
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  overflow: hidden;
  max-width: 600px;
}
.welcome-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: var(--el-fill-color-light);
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.welcome-card-body {
  padding: 20px 24px;
  line-height: 1.8;
  font-size: 14px;
  color: var(--c-text);
  background: #fff;
}

.welcome-template-tip {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px dashed var(--el-border-color-lighter);
  font-size: 12px;
  color: var(--c-text-muted);
}
</style>

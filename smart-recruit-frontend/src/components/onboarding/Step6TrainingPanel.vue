<template>
  <div class="step-panel">
    <div class="step-desc">跟踪新员工的入职培训完成情况。</div>

    <div class="training-layout">
      <!-- 进度环 -->
      <div class="training-progress-ring">
        <el-progress
          type="circle"
          :percentage="computedProgress"
          :color="computedProgress >= 80 ? '#059669' : computedProgress >= 40 ? '#d97706' : '#dc2626'"
          :width="140"
          :stroke-width="12"
        >
          <template #default>
            <span style="font-size: 22px; font-weight: 700;">{{ computedProgress }}%</span>
          </template>
        </el-progress>
      </div>

      <!-- 培训模块清单 -->
      <div class="training-modules">
        <h4 style="font-size: 14px; margin-bottom: 12px; color: var(--c-text);">培训模块</h4>
        <el-checkbox-group v-model="completedModules" @change="onModuleChange">
          <div v-for="m in modules" :key="m.key" class="module-item">
            <el-checkbox :label="m.key" :value="m.key">
              <span style="font-size: 13px;">{{ m.label }}</span>
            </el-checkbox>
          </div>
        </el-checkbox-group>

        <div style="margin-top: 16px; display: flex; align-items: center; gap: 12px;">
          <span style="font-size: 13px; color: var(--c-text-secondary);">手动调整进度：</span>
          <el-input-number
            v-model="manualProgress"
            :min="0" :max="100" :step="5"
            size="small"
            style="width: 120px;"
          />
          <span style="font-size: 13px; color: var(--c-text-secondary);">%</span>
        </div>

        <el-button
          type="primary"
          :loading="saving"
          style="margin-top: 16px;"
          @click="handleSave"
        >
          更新培训进度
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { updateTraining } from '@/api/onboarding'
import { useSettingsStore } from '@/stores/settings'
import type { OnboardingDetailVO } from '@/types/models'

const props = defineProps<{
  onboarding: OnboardingDetailVO
}>()

const emit = defineEmits<{
  (e: 'updated'): void
}>()

const settingsStore = useSettingsStore()

/**
 * 培训模块清单：来自系统配置（系统设置 → 入职管理 → 培训模块列表），保存后立即生效。
 */
const modules = computed(() =>
  settingsStore.onboardingTrainingModules.map((label, index) => ({
    key: `module-${index}`,
    label,
  })),
)

const MODULE_COUNT = computed(() => modules.value.length)

const saving = ref(false)
const completedModules = ref<string[]>([])
const manualProgress = ref(0)

const computedProgress = computed(() => {
  const autoProgress = MODULE_COUNT.value === 0
    ? 0
    : Math.round((completedModules.value.length / MODULE_COUNT.value) * 100)
  // Use manual progress if it differs from auto (user manually adjusted)
  if (manualProgress.value > 0 && manualProgress.value !== autoProgress) {
    return manualProgress.value
  }
  return autoProgress
})

onMounted(() => {
  // Initialize from existing progress
  const existingProgress = props.onboarding.trainingProgress
    ? Number(props.onboarding.trainingProgress)
    : 0
  if (existingProgress > 0) {
    manualProgress.value = existingProgress
    // Auto-complete modules proportional to progress
    const moduleCount = MODULE_COUNT.value === 0
      ? 0
      : Math.round((existingProgress / 100) * MODULE_COUNT.value)
    completedModules.value = modules.value.slice(0, moduleCount).map(m => m.key)
  }
})

function onModuleChange() {
  // When modules are checked, update manual progress to match auto
  manualProgress.value = 0
}

async function handleSave() {
  saving.value = true
  try {
    await updateTraining(String(props.onboarding.id), {
      trainingProgress: computedProgress.value,
    })
    ElMessage.success('培训进度已更新')
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
.training-layout {
  display: flex;
  gap: 40px;
  align-items: flex-start;
  flex-wrap: wrap;
}
.training-progress-ring {
  flex-shrink: 0;
}
.training-modules {
  flex: 1;
  min-width: 280px;
}
.module-item {
  padding: 6px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.module-item:last-child {
  border-bottom: none;
}
</style>

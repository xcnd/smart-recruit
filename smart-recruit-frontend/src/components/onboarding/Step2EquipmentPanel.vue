<template>
  <div class="step-panel">
    <div class="step-header">
      <div class="step-desc">为新员工分配工作设备。共 5 项设备需要配置。</div>
      <el-button
        v-if="pendingCount > 0"
        type="primary"
        size="default"
        :loading="assignAllLoading"
        @click="handleAssignAll"
      >
        全部分配 ({{ pendingCount }})
      </el-button>
    </div>
    <el-table :data="props.onboarding.equipments || []" stripe size="default">
      <el-table-column label="设备名称" width="140">
        <template #default="{ row }">
          <span style="font-weight: 500;">{{ getEquipmentTypeName(row.equipmentType) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag
            :type="row.status === 3 ? 'success' : row.status >= 1 ? 'warning' : 'info'"
            size="small"
          >
            {{ getEquipmentStatusName(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="资产编号" min-width="150">
        <template #default="{ row }">
          <el-input
            v-model="assetNoCache[String(row.id)]"
            :placeholder="assetNoPlaceholder(row)"
            size="small"
            clearable
            @blur="saveAssetNo(row)"
            @keyup.enter="saveAssetNo(row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="备注" min-width="140">
        <template #default="{ row }">
          <el-input
            v-model="remarkCache[String(row.id)]"
            :placeholder="row.remark || '添加备注'"
            size="small"
            clearable
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" align="center">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 0"
            size="small" type="primary" plain
            :loading="loadingId === row.id"
            @click="handleAssign(row)"
          >
            分配
          </el-button>
          <el-button
            v-else-if="row.status === 1"
            size="small" type="warning" plain
            :loading="loadingId === row.id"
            @click="handleShip(row)"
          >
            发出
          </el-button>
          <el-button
            v-else-if="row.status === 2"
            size="small" type="success" plain
            :loading="loadingId === row.id"
            @click="handleDeliver(row)"
          >
            签收
          </el-button>
          <el-tag v-else type="success" size="small">已签收</el-tag>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { updateEquipment } from '@/api/onboarding'
import { getEquipmentTypeName, getEquipmentStatusName } from '@/utils/format'
import type { OnboardingDetailVO, OnboardingEquipmentItem } from '@/types/models'

const props = defineProps<{
  onboarding: OnboardingDetailVO
}>()

const emit = defineEmits<{
  (e: 'updated'): void
}>()

const loadingId = ref<string | null>(null)
const assignAllLoading = ref(false)
const assetNoCache = reactive<Record<string, string>>({})
const remarkCache = reactive<Record<string, string>>({})

const pendingCount = computed(() =>
  (props.onboarding.equipments || []).filter(e => e.status === 0).length
)

/** 每类资产的示例编号 */
const ASSET_NO_EXAMPLES: Record<number, string> = {
  0: '示例：NB-2026-0001',   // 笔记本电脑
  1: '示例：MON-2026-0001',  // 显示器
  2: '示例：MP-2026-0001',   // 手机
  3: '示例：BADGE-2026-0001',// 门禁卡
  4: '示例：DESK-A-301',     // 工位
}

function assetNoPlaceholder(row: OnboardingEquipmentItem): string {
  return row.assetNo || ASSET_NO_EXAMPLES[row.equipmentType] || '请输入资产编号'
}

async function saveAssetNo(equip: OnboardingEquipmentItem) {
  const assetNo = assetNoCache[String(equip.id)]
  if (assetNo === undefined) return
  try {
    await updateEquipment(String(props.onboarding.id), String(equip.id), { assetNo })
    ElMessage.success('资产编号已保存')
    emit('updated')
  } catch { /* ignore */ }
}

async function handleAssign(equip: OnboardingEquipmentItem) {
  loadingId.value = equip.id
  try {
    const assetNo = assetNoCache[String(equip.id)] || undefined
    await updateEquipment(String(props.onboarding.id), String(equip.id), {
      status: 1, // ASSIGNED
      assetNo,
    })
    ElMessage.success(`${equip.equipmentName} 已分配`)
    emit('updated')
  } catch { /* ignore */ }
  finally { loadingId.value = null }
}

async function handleAssignAll() {
  assignAllLoading.value = true
  const pending = (props.onboarding.equipments || []).filter(e => e.status === 0)
  let successCount = 0
  try {
    for (const equip of pending) {
      const assetNo = assetNoCache[String(equip.id)] || undefined
      try {
        await updateEquipment(String(props.onboarding.id), String(equip.id), {
          status: 1,
          assetNo,
        })
        successCount++
      } catch { /* 单个失败继续下一个 */ }
    }
    if (successCount > 0) {
      ElMessage.success(`已分配 ${successCount} 项设备`)
      emit('updated')
    } else {
      ElMessage.warning('分配失败，请重试')
    }
  } finally {
    assignAllLoading.value = false
  }
}

async function handleShip(equip: OnboardingEquipmentItem) {
  loadingId.value = equip.id
  try {
    await updateEquipment(String(props.onboarding.id), String(equip.id), { status: 2 })
    ElMessage.success(`${equip.equipmentName} 已发出`)
    emit('updated')
  } catch { /* ignore */ }
  finally { loadingId.value = null }
}

async function handleDeliver(equip: OnboardingEquipmentItem) {
  loadingId.value = equip.id
  try {
    await updateEquipment(String(props.onboarding.id), String(equip.id), { status: 3 })
    ElMessage.success(`${equip.equipmentName} 已签收`)
    emit('updated')
  } catch { /* ignore */ }
  finally { loadingId.value = null }
}
</script>

<style scoped>
.step-panel {
  padding: 8px 0;
}
.step-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.step-desc {
  font-size: 13px;
  color: var(--c-text-secondary);
}
</style>

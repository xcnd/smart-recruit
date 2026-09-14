<template>
  <div class="step-panel">
    <div class="step-desc">请收集新员工的入职资料。支持拖拽文件到上传区域，或点击选择文件。共 6 项资料需要完成。</div>
    <el-table :data="props.onboarding.documents || []" stripe size="default" class="doc-table">
      <el-table-column label="资料名称" width="140">
        <template #default="{ row }">
          <div style="display: flex; align-items: center; gap: 6px;">
            <el-icon :size="16" :style="{ color: docIconColor(row.status) }">
              <component :is="docIcon(row.status)" />
            </el-icon>
            <span style="font-weight: 500;">{{ getDocumentTypeName(row.docType) }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status >= 1 ? 'success' : 'info'" size="small">
            {{ getDocumentStatusName(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="附件" min-width="280">
        <template #default="{ row }">
          <!-- 已有文件：图片直接显示缩略图，其他文件显示文件名 -->
          <div v-if="row.filePath" class="file-row">
            <template v-if="isImageFile(row.filePath)">
              <el-image
                :src="row.filePath"
                class="file-thumb"
                fit="cover"
                @click="handlePreview(row)"
              />
            </template>
            <template v-else>
              <div class="file-info">
                <el-icon :size="16" color="#409EFF"><Document /></el-icon>
                <span
                  class="file-name"
                  :title="row.filePath"
                  @click="handlePreview(row)"
                >
                  {{ extractFileName(row.filePath) }}
                </span>
              </div>
            </template>
            <el-upload
              :show-file-list="false"
              :http-request="(opt: any) => customUpload(opt, row)"
              :accept="uploadAccept"
              :before-upload="beforeUpload"
              class="upload-inline"
            >
              <el-button size="small" text type="primary" :loading="uploadingId === row.id">
                重新上传
              </el-button>
            </el-upload>
          </div>

          <!-- 无文件：拖拽上传区域 -->
          <el-upload
            v-else
            :show-file-list="false"
            :http-request="(opt: any) => customUpload(opt, row)"
            drag
            :accept="uploadAccept"
            :before-upload="beforeUpload"
            class="upload-drop-zone"
          >
            <template v-if="uploadingId !== row.id">
              <el-icon :size="24" class="upload-icon"><UploadFilled /></el-icon>
              <div class="upload-text">
                <span>拖拽文件到此处，或</span>
                <em>点击上传</em>
              </div>
              <div class="upload-hint">支持 {{ settingsStore.uploadAllowedExtensions.join(', ') }}，最大 {{ settingsStore.uploadMaxSizeMb }}MB</div>
            </template>
            <template v-else>
              <el-icon :size="24" class="upload-icon is-loading"><Loading /></el-icon>
              <div class="upload-text">上传中...</div>
            </template>
          </el-upload>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="80" align="center">
        <template #default="{ row }">
          <span v-if="!row.filePath" style="font-size: 12px; color: var(--c-text-muted);">待提交</span>
          <el-button
            v-else
            size="small" text type="danger"
            :loading="deletingId === row.id"
            @click="handleDelete(row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 图片预览 -->
    <el-image-viewer
      v-if="previewVisible"
      :url-list="previewUrlList"
      :initial-index="0"
      @close="previewVisible = false"
      :hide-on-click-modal="true"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, UploadFilled, Loading, CircleCheck, InfoFilled } from '@element-plus/icons-vue'
import { updateSingleDocument, uploadOnboardingFile, deleteDocument } from '@/api/onboarding'
import { getDocumentTypeName, getDocumentStatusName } from '@/utils/format'
import { useSettingsStore } from '@/stores/settings'
import type { OnboardingDetailVO, OnboardingDocumentItem } from '@/types/models'

const IMAGE_EXTENSIONS = ['jpg', 'jpeg', 'png', 'gif']

const props = defineProps<{ onboarding: OnboardingDetailVO }>()
const emit = defineEmits<{ (e: 'updated'): void }>()
const settingsStore = useSettingsStore()

const uploadAccept = computed(() => settingsStore.uploadAllowedExtensions.join(','))

const uploadingId = ref<string | null>(null)
const deletingId = ref<string | null>(null)
const previewVisible = ref(false)
const previewUrlList = ref<string[]>([])

function extractFileName(path: string): string {
  const parts = path.split('/')
  return parts[parts.length - 1] || path
}

function isImageFile(path: string): boolean {
  const ext = path.substring(path.lastIndexOf('.') + 1).toLowerCase()
  return IMAGE_EXTENSIONS.includes(ext)
}

function docIcon(status: number) {
  return status >= 1 ? CircleCheck : InfoFilled
}

function docIconColor(status: number): string {
  return status >= 1 ? '#059669' : '#94a3b8'
}

function beforeUpload(file: File): boolean {
  const error = settingsStore.validateUploadFile(file)
  if (error) {
    ElMessage.warning(error)
    return false
  }
  return true
}

function handlePreview(doc: OnboardingDocumentItem) {
  if (!doc.filePath) return
  if (isImageFile(doc.filePath)) {
    previewUrlList.value = [doc.filePath]
    previewVisible.value = true
  } else {
    window.open(doc.filePath, '_blank')
  }
}

async function customUpload(options: any, doc: OnboardingDocumentItem) {
  uploadingId.value = doc.id
  try {
    const result = await uploadOnboardingFile(options.file)
    await updateSingleDocument(String(props.onboarding.id), String(doc.id), { filePath: result.url })
    ElMessage.success(`${getDocumentTypeName(doc.docType)} 上传成功`)
    emit('updated')
  } catch {
    // HTTP 拦截器统一提示
  } finally {
    uploadingId.value = null
  }
}

async function handleDelete(doc: OnboardingDocumentItem) {
  try {
    await ElMessageBox.confirm(
      `确定要删除「${getDocumentTypeName(doc.docType)}」的附件吗？`,
      '删除确认',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return // 取消
  }

  deletingId.value = doc.id
  try {
    await deleteDocument(String(props.onboarding.id), String(doc.id))
    ElMessage.success('附件已删除')
    emit('updated')
  } catch {
    // HTTP 拦截器统一提示
  } finally {
    deletingId.value = null
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
.doc-table :deep(.el-table__body-wrapper) {
  overflow-x: visible;
}

/* 已有文件的显示行 */
.file-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  width: 100%;
}
.file-thumb {
  width: 120px;
  height: 80px;
  border-radius: 6px;
  border: 1px solid var(--el-border-color);
  cursor: pointer;
  flex-shrink: 0;
  object-fit: cover;
}
.file-thumb:hover {
  border-color: var(--el-color-primary);
}
.file-info {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  flex: 1;
}
.file-actions {
  display: flex;
  align-items: center;
  gap: 2px;
  flex-shrink: 0;
}
.file-name {
  font-size: 12px;
  color: var(--c-primary);
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 140px;
}
.file-name:hover {
  text-decoration: underline;
  color: var(--el-color-primary-light-3);
}
.upload-inline {
  flex-shrink: 0;
}

/* 拖拽上传区域 */
.upload-drop-zone {
  width: 100%;
}
.upload-drop-zone :deep(.el-upload) {
  width: 100%;
}
.upload-drop-zone :deep(.el-upload-dragger) {
  width: 100%;
  padding: 16px 12px;
  border-radius: 8px;
  border: 2px dashed var(--el-border-color);
  transition: border-color 0.3s, background 0.3s;
}
.upload-drop-zone :deep(.el-upload-dragger:hover) {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}
.upload-drop-zone :deep(.el-upload-dragger.is-dragover) {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-8);
}
.upload-icon {
  color: var(--el-color-primary);
  margin-bottom: 4px;
}
.upload-icon.is-loading {
  animation: rotating 1.2s linear infinite;
}
.upload-text {
  font-size: 12px;
  color: var(--c-text-secondary);
  line-height: 1.6;
}
.upload-text em {
  color: var(--el-color-primary);
  font-style: normal;
}
.upload-hint {
  font-size: 11px;
  color: var(--c-text-muted);
  margin-top: 2px;
}
@keyframes rotating {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>

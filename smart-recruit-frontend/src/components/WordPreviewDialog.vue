<template>
  <el-dialog
    v-model="visible"
    :title="title"
    fullscreen
    :close-on-click-modal="false"
    :destroy-on-close="true"
    @closed="handleClosed"
  >
    <template #header>
      <div class="preview-header">
        <span class="preview-title">{{ title }}</span>
        <el-button size="small" text @click="handleDownload">
          <el-icon style="margin-right: 4px;"><Download /></el-icon>
          下载原始文件
        </el-button>
      </div>
    </template>

    <div class="preview-container" v-loading="loading">
      <div v-if="error" class="preview-error">
        <el-result icon="warning" title="预览失败" :sub-title="error">
          <template #extra>
            <el-button type="primary" @click="loadPreview">重试</el-button>
            <el-button @click="handleDownload">下载文件</el-button>
          </template>
        </el-result>
      </div>
      <!-- .docx: mammoth.js 渲染 -->
      <div
        v-if="isDocx && !error && !loading"
        ref="contentRef"
        class="preview-content"
        v-html="htmlContent"
      ></div>
      <!-- .doc: iframe 加载服务端 HTML -->
      <iframe
        v-if="!isDocx && !error"
        ref="iframeRef"
        :src="previewUrl"
        class="preview-iframe"
        @load="onIframeLoaded"
        sandbox="allow-same-origin allow-scripts"
      ></iframe>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import { Download } from '@element-plus/icons-vue'
import mammoth from 'mammoth'

const props = defineProps<{
  modelValue: boolean
  resumeId: number | string
  fileName?: string
  fileUrl?: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', val: boolean): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
})

const title = computed(() => {
  return props.fileName ? `${props.fileName} — 预览` : '简历预览'
})

const isDocx = computed(() => {
  const name = (props.fileName || '').toLowerCase()
  return name.endsWith('.docx')
})

const previewUrl = computed(() => `/api/v1/resumes/${props.resumeId}/preview`)

const loading = ref(true)
const error = ref('')
const htmlContent = ref('')
const contentRef = ref<HTMLElement>()
const iframeRef = ref<HTMLIFrameElement>()

async function loadPreview() {
  loading.value = true
  error.value = ''
  htmlContent.value = ''

  if (isDocx.value) {
    await loadDocx()
  }
  // .doc: iframe 自动加载 previewUrl
}

async function loadDocx() {
  try {
    const res = await fetch(previewUrl.value)
    if (!res.ok) throw new Error(`文件加载失败 (${res.status})`)
    const arrayBuffer = await res.arrayBuffer()
    const result = await mammoth.convertToHtml({ arrayBuffer })
    htmlContent.value = result.value
    await nextTick()
    loading.value = false
  } catch (e: any) {
    loading.value = false
    error.value = e.message || '文档加载失败，请尝试下载后查看'
  }
}

function onIframeLoaded() {
  loading.value = false
}

function handleDownload() {
  if (props.fileUrl) {
    window.open(props.fileUrl, '_blank')
  }
}

function handleClosed() {
  loading.value = true
  error.value = ''
  htmlContent.value = ''
  if (iframeRef.value) {
    iframeRef.value.src = ''
  }
}

watch(() => props.modelValue, (val) => {
  if (val) {
    loadPreview()
  }
})
</script>

<style scoped>
.preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding-right: 16px;
}
.preview-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a2e;
}
.preview-container {
  height: calc(100vh - 120px);
  position: relative;
  background: #f5f5f5;
  border-radius: 8px;
  overflow: hidden;
}
.preview-content {
  width: 100%;
  height: 100%;
  overflow: auto;
  padding: 40px 48px;
  background: #fff;
  line-height: 1.8;
  color: #333;
}
.preview-content :deep(h1) { font-size: 24px; margin: 20px 0 12px; }
.preview-content :deep(h2) { font-size: 20px; margin: 16px 0 10px; }
.preview-content :deep(h3) { font-size: 17px; margin: 14px 0 8px; }
.preview-content :deep(p) { margin: 0 0 8px; }
.preview-content :deep(table) { border-collapse: collapse; width: 100%; margin: 8px 0 16px; }
.preview-content :deep(td), .preview-content :deep(th) { border: 1px solid #d0d0d0; padding: 6px 10px; }
.preview-content :deep(ul), .preview-content :deep(ol) { padding-left: 24px; margin: 8px 0; }
.preview-content :deep(img) { max-width: 100%; height: auto; }
.preview-iframe {
  width: 100%;
  height: 100%;
  border: none;
  background: #fff;
}
.preview-error {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  background: #fff;
}
</style>

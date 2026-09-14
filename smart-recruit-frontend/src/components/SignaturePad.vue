<template>
  <canvas ref="canvasRef" class="signature-pad-canvas"></canvas>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import SignaturePad from 'signature_pad'

/**
 * 手写签名板组件（基于开源库 signature_pad）。
 *
 * <p>开箱支持鼠标 / 触摸屏手写、笔迹平滑与压力感应，
 * 自动按设备像素比高清渲染，窗口缩放时保留已画内容。</p>
 *
 * @expose clear 清空签名；getSignature 获取 PNG data URL（未签名返回 null）
 */
const emit = defineEmits<{
  (e: 'begin'): void
  (e: 'change', empty: boolean): void
}>()

const canvasRef = ref<HTMLCanvasElement>()
let pad: SignaturePad | null = null
let sizeObserver: ResizeObserver | null = null
let lastW = 0
let lastH = 0

onMounted(() => {
  const canvas = canvasRef.value
  if (!canvas) return
  pad = new SignaturePad(canvas, {
    penColor: '#0f172a',
    minWidth: 0.5,
    maxWidth: 2.5,
    backgroundColor: 'rgba(0,0,0,0)',
    onBegin: () => emit('begin'),
    onEnd: () => emit('change', pad ? pad.isEmpty() : true),
  })
  // 布局稳定后再初始化尺寸（避免挂载时宽高为 0 导致画不上字）
  requestAnimationFrame(() => resizeCanvas())
  if (typeof ResizeObserver !== 'undefined') {
    sizeObserver = new ResizeObserver(() => resizeCanvas())
    sizeObserver.observe(canvas)
  }
  window.addEventListener('resize', resizeCanvas)
})

/** 按容器尺寸 + 设备像素比重置画布，并恢复已画内容。 */
function resizeCanvas() {
  const canvas = canvasRef.value
  if (!canvas || !pad) return
  const rect = canvas.getBoundingClientRect()
  const width = Math.round(rect.width)
  const height = Math.round(rect.height)
  if (width <= 0 || height <= 0) return
  // 尺寸未变化时跳过，避免 ResizeObserver 死循环
  if (width === lastW && height === lastH) return
  lastW = width
  lastH = height
  const ratio = Math.max(window.devicePixelRatio || 1, 1)
  const existing = pad.isEmpty() ? null : pad.toDataURL()
  canvas.width = width * ratio
  canvas.height = height * ratio
  canvas.getContext('2d')?.setTransform(ratio, 0, 0, ratio, 0, 0)
  if (existing) {
    pad.fromDataURL(existing)
  } else {
    pad.clear()
  }
}

/** 清空签名。 */
function clear() {
  pad?.clear()
  emit('change', true)
}

/** 获取签名 PNG data URL；未签名返回 null。 */
function getSignature(): string | null {
  if (!pad || pad.isEmpty()) return null
  return pad.toDataURL('image/png')
}

defineExpose({ clear, getSignature })

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCanvas)
  sizeObserver?.disconnect()
  sizeObserver = null
  pad = null
})
</script>

<style scoped>
.signature-pad-canvas {
  width: 100%;
  height: 220px;
  display: block;
  border: 2px dashed #cbd5e1;
  border-radius: 8px;
  background: #f8fafc;
  touch-action: none;
  cursor: crosshair;
}
</style>

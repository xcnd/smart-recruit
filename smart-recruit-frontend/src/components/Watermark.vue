<template>
  <div v-if="enabled" class="sr-watermark" aria-hidden="true">
    <div v-for="i in rows" :key="i" class="sr-watermark-row">
      <span v-for="j in cols" :key="j" class="sr-watermark-item">{{ line }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useSettingsStore } from '@/stores/settings'
import { useUserStore } from '@/stores/user'

/**
 * 全局防截图水印。
 *
 * <p>由系统设置中的“页面水印”开关控制；开启后在所有页面显示
 * 当前登录账号、姓名与实时时间的半透明水印，且不拦截任何鼠标事件。</p>
 */
const settingsStore = useSettingsStore()
const userStore = useUserStore()

const enabled = computed(() => settingsStore.watermarkEnabled && !!userStore.userInfo)
const nowText = ref('')
let timer: ReturnType<typeof setInterval> | null = null

const rows = 8
const cols = 4

const line = computed(() => {
  const u = userStore.userInfo
  const username = u?.username || ''
  const realName = u?.realName || userStore.userName || ''
  return `账号：${username}  姓名：${realName}  ${nowText.value}`
})

function tick() {
  const d = new Date()
  const p = (n: number) => String(n).padStart(2, '0')
  nowText.value =
    `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ` +
    `${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

onMounted(() => {
  tick()
  timer = setInterval(tick, 1000)
})

onBeforeUnmount(() => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
})
</script>

<style scoped>
.sr-watermark {
  position: fixed;
  inset: 0;
  z-index: 9999;
  pointer-events: none;
  overflow: hidden;
  opacity: 0.07;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}
.sr-watermark-row {
  display: flex;
  justify-content: space-between;
  transform: rotate(-20deg) scale(1.05);
  white-space: nowrap;
  padding: 0 4%;
}
.sr-watermark-item {
  font-size: 15px;
  color: #334155;
  user-select: none;
}
</style>

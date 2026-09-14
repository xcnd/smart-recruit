import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import { createPinia } from 'pinia'
import router from './router'
import App from './App.vue'
import './styles/common.css'
import { useSettingsStore } from './stores/settings'

const app = createApp(App)

// Register all Element Plus icons
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(ElementPlus, { locale: zhCn })
app.use(createPinia())
app.use(router)

// 先加载公开系统配置（系统名称、密码策略等），再挂载应用，
// 保证标题、品牌等展示从首帧起即使用配置值。
router.isReady().then(async () => {
  await useSettingsStore().load()
  app.mount('#app')
})

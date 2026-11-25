import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import '@/assets/main.css'
import App from './App.vue'
import router from './router'
import { useSystemConfigStore } from '@/store/systemConfig'

const app = createApp(App)
const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)

app.use(pinia)
app.use(router)
app.use(ElementPlus, { locale: zhCn })

// 先读取系统品牌配置，登录页与浏览器标题在首屏就使用最新名称
const systemConfigStore = useSystemConfigStore(pinia)
systemConfigStore.load()

app.mount('#app')

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import i18n from './i18n'
import router from './router'
import App from './App.vue'
import { installA11yDirectives } from '@shared/plugins/a11y-directive'
import './style.css'
import '@shared/styles/a11y.scss'

const app = createApp(App)
const pinia = createPinia()

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(pinia)
app.use(router)
app.use(ElementPlus, { locale: zhCn })
app.use(i18n)
installA11yDirectives(app)
app.mount('#app')

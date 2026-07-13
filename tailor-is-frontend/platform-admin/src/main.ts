import { createApp } from 'vue';
import { createPinia } from 'pinia';
import ElementPlus from 'element-plus';
import zhCn from 'element-plus/es/locale/lang/zh-cn';
import * as ElementPlusIconsVue from '@element-plus/icons-vue';
import App from './App.vue';
import router from './router';
import i18n from './i18n';
import './style.css';
import { installA11yDirectives } from '@shared/plugins/a11y-directive';
import 'element-plus/dist/index.css';
import { injectRouter } from '@/api/request';
// TODO: 响应式布局待后续实现（需添加 sass 依赖及 styles/responsive.scss）

const app = createApp(App);
const pinia = createPinia();

// 注入 router 到 request 模块，打破循环依赖
injectRouter(router);

app.use(pinia);
app.use(router);
app.use(ElementPlus, { locale: zhCn });
app.use(i18n);

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component);
}

installA11yDirectives(app);
app.mount('#app');

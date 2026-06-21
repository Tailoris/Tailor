import App from './App'
import { createSSRApp } from 'vue'
import type { App as VueApp } from 'vue'

export function createApp(): { app: VueApp } {
  const app: VueApp = createSSRApp(App)
  return { app }
}

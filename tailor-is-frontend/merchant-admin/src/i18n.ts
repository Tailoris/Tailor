import { createI18n } from 'vue-i18n'
import zhCN from './locales/zh-CN.json'
import enUS from './locales/en-US.json'

const i18n = createI18n({
  legacy: false,
  locale: getInitialLocale(),
  fallbackLocale: 'zh-CN',
  messages: {
    'zh-CN': zhCN,
    'en-US': enUS,
  },
})

function getInitialLocale(): string {
  const saved = localStorage.getItem('tailor-is-admin-locale')
  if (saved && (saved === 'zh-CN' || saved === 'en-US')) {
    return saved
  }
  const browserLang = navigator.language
  if (browserLang.startsWith('zh')) return 'zh-CN'
  if (browserLang.startsWith('en')) return 'en-US'
  return 'zh-CN'
}

export function switchLocale(locale: string) {
  if (i18n.global.availableLocales.includes(locale)) {
    i18n.global.locale.value = locale
    localStorage.setItem('tailor-is-admin-locale', locale)
  }
}

export default i18n
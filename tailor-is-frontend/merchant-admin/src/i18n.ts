import { createI18n } from 'vue-i18n';
import zhCN from './locales/zh-CN.json';
import enUS from './locales/en-US.json';
import esES from './locales/es-ES.json';
import jaJP from './locales/ja-JP.json';
import frFR from './locales/fr-FR.json';
import koKR from './locales/ko-KR.json';

/** 支持的语言列表 */
const SUPPORTED_LOCALES = ['zh-CN', 'en-US', 'es-ES', 'ja-JP', 'fr-FR', 'ko-KR'] as const;

const i18n = createI18n({
  legacy: false,
  locale: getInitialLocale(),
  fallbackLocale: 'zh-CN',
  messages: {
    'zh-CN': zhCN,
    'en-US': enUS,
    'es-ES': esES,
    'ja-JP': jaJP,
    'fr-FR': frFR,
    'ko-KR': koKR,
  },
});

/**
 * 获取初始语言设置
 * 优先级: localStorage > 浏览器语言 > 默认 zh-CN
 */
function getInitialLocale(): string {
  const saved = localStorage.getItem('tailor-is-admin-locale');
  if (saved && SUPPORTED_LOCALES.includes(saved as typeof SUPPORTED_LOCALES[number])) {
    return saved;
  }
  const browserLang = navigator.language;
  if (browserLang.startsWith('zh')) return 'zh-CN';
  if (browserLang.startsWith('en')) return 'en-US';
  if (browserLang.startsWith('es')) return 'es-ES';
  if (browserLang.startsWith('ja')) return 'ja-JP';
  if (browserLang.startsWith('fr')) return 'fr-FR';
  if (browserLang.startsWith('ko')) return 'ko-KR';
  return 'zh-CN';
}

/**
 * 切换语言
 * @param locale 目标语言代码 (zh-CN|en-US|es-ES|ja-JP|fr-FR|ko-KR)
 */
export function switchLocale(locale: string) {
  if (i18n.global.availableLocales.includes(locale)) {
    i18n.global.locale.value = locale;
    localStorage.setItem('tailor-is-admin-locale', locale);
  }
}

export default i18n;
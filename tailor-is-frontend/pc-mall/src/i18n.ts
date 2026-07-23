import { createI18n } from 'vue-i18n';
import zhCN from './locales/zh-CN.json';
import enUS from './locales/en-US.json';
import esES from './locales/es-ES.json';
import frFR from './locales/fr-FR.json';
import jaJP from './locales/ja-JP.json';
import koKR from './locales/ko-KR.json';

export type SupportedLocale = 'zh-CN' | 'en-US' | 'es-ES' | 'fr-FR' | 'ja-JP' | 'ko-KR';

const i18n = createI18n({
  legacy: false,
  locale: getInitialLocale(),
  fallbackLocale: 'zh-CN',
  messages: {
    'zh-CN': zhCN,
    'en-US': enUS,
    'es-ES': esES,
    'fr-FR': frFR,
    'ja-JP': jaJP,
    'ko-KR': koKR,
  },
});

export function getInitialLocale(): SupportedLocale {
  const saved = localStorage.getItem('tailor-is-locale');
  if (saved && ['zh-CN', 'en-US', 'es-ES', 'fr-FR', 'ja-JP', 'ko-KR'].includes(saved)) {
    return saved as SupportedLocale;
  }
  const browserLang = navigator.language;
  if (browserLang.startsWith('zh')) return 'zh-CN';
  if (browserLang.startsWith('en')) return 'en-US';
  if (browserLang.startsWith('es')) return 'es-ES';
  if (browserLang.startsWith('fr')) return 'fr-FR';
  if (browserLang.startsWith('ja')) return 'ja-JP';
  if (browserLang.startsWith('ko')) return 'ko-KR';
  return 'zh-CN';
}

let localeChangeCallback: ((locale: SupportedLocale) => void) | null = null;

export function setLocaleChangeCallback(callback: (locale: SupportedLocale) => void) {
  localeChangeCallback = callback;
}

export function switchLocale(locale: SupportedLocale) {
  if (i18n.global.availableLocales.includes(locale)) {
    i18n.global.locale.value = locale;
    localStorage.setItem('tailor-is-locale', locale);
    document.documentElement.lang = locale;
    if (localeChangeCallback) {
      localeChangeCallback(locale);
    }
  }
}

export default i18n;
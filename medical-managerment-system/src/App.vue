<template>
  <div id="app">
    <router-view></router-view>
    <ui-theme-toggle :theme="theme" @change="handleThemeChange" />
  </div>
</template>

<script>
import UiThemeToggle from './components/UiThemeToggle.vue';

const THEME_STORAGE_KEY = 'medical-theme';
const DEFAULT_THEME = 'light';

function normalizeTheme(theme) {
  return theme === 'light' || theme === 'dark' ? theme : DEFAULT_THEME;
}

function readStoredTheme() {
  try {
    return normalizeTheme(window.localStorage.getItem(THEME_STORAGE_KEY));
  } catch (error) {
    return DEFAULT_THEME;
  }
}

export default {
  name: 'app',
  components: {
    UiThemeToggle,
  },
  data() {
    return {
      theme: readStoredTheme(),
    };
  },
  created() {
    this.persistTheme(this.theme);
    this.syncDocumentTheme(this.theme);
  },
  methods: {
    persistTheme(theme) {
      try {
        window.localStorage.setItem(THEME_STORAGE_KEY, theme);
      } catch (error) {
        // localStorage 不可用时仍允许本次会话正常切换。
      }
    },
    syncDocumentTheme(theme) {
      const isDark = theme === 'dark';
      // medical-ui 始终启用主题；dark 切换夜间变量（变量定义见 theme.css）。
      document.documentElement.classList.add('medical-ui');
      document.documentElement.classList.toggle('dark', isDark);
      if (document.body) {
        document.body.classList.add('medical-ui');
        document.body.classList.toggle('dark', isDark);
      }
    },
    handleThemeChange(theme) {
      const nextTheme = normalizeTheme(theme);
      if (nextTheme === this.theme) {
        return;
      }

      const previousTheme = this.theme;
      this.theme = nextTheme;
      this.persistTheme(nextTheme);
      this.syncDocumentTheme(nextTheme);

      this.$nextTick(() => {
        window.dispatchEvent(
          new CustomEvent('medical-theme-change', {
            detail: {
              theme: nextTheme,
              previousTheme,
            },
          })
        );
      });
    },
  },
};
</script>

<template>
  <div id="app" :class="{ 'ui-ios26': uiMode === 'ios26' }">
    <router-view></router-view>
    <ui-theme-toggle :mode="uiMode" @change="handleUiModeChange" />
  </div>
</template>

<script>
import UiThemeToggle from "./components/UiThemeToggle.vue";

const UI_MODE_STORAGE_KEY = "medical-ui-mode";
const DEFAULT_UI_MODE = "ios26";

function normalizeUiMode(mode) {
  return mode === "classic" || mode === "ios26" ? mode : DEFAULT_UI_MODE;
}

function readStoredUiMode() {
  try {
    return normalizeUiMode(window.localStorage.getItem(UI_MODE_STORAGE_KEY));
  } catch (error) {
    return DEFAULT_UI_MODE;
  }
}

export default {
  name: "app",
  components: {
    UiThemeToggle,
  },
  data() {
    return {
      uiMode: readStoredUiMode(),
    };
  },
  created() {
    this.persistUiMode(this.uiMode);
    this.syncDocumentUiMode(this.uiMode);
  },
  methods: {
    persistUiMode(mode) {
      try {
        window.localStorage.setItem(UI_MODE_STORAGE_KEY, mode);
      } catch (error) {
        // localStorage 不可用时仍允许本次会话正常切换。
      }
    },
    syncDocumentUiMode(mode) {
      const isIos26 = mode === "ios26";
      document.documentElement.classList.toggle("ios26-ui", isIos26);
      if (document.body) {
        document.body.classList.toggle("ios26-ui", isIos26);
      }
    },
    handleUiModeChange(mode) {
      const nextMode = normalizeUiMode(mode);
      if (nextMode === this.uiMode) {
        return;
      }

      const previousMode = this.uiMode;
      this.uiMode = nextMode;
      this.persistUiMode(nextMode);
      this.syncDocumentUiMode(nextMode);

      this.$nextTick(() => {
        window.dispatchEvent(
          new CustomEvent("medical-ui-mode-change", {
            detail: {
              mode: nextMode,
              previousMode,
            },
          })
        );
      });
    },
  },
};
</script>

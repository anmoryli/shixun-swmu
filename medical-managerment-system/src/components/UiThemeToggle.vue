<template>
  <button
    class="ui-theme-toggle"
    :class="{ 'is-ios26': isIos26 }"
    type="button"
    :aria-label="toggleLabel"
    :aria-pressed="isIos26 ? 'true' : 'false'"
    :title="toggleLabel"
    @click="toggleMode"
  >
    <span class="toggle-track" aria-hidden="true">
      <span class="toggle-thumb"></span>
    </span>
    <span class="toggle-label">{{ toggleLabel }}</span>
  </button>
</template>

<script>
export default {
  name: 'UiThemeToggle',
  props: {
    mode: {
      type: String,
      required: true,
      validator(value) {
        return value === 'classic' || value === 'ios26';
      },
    },
  },
  computed: {
    isIos26() {
      return this.mode === 'ios26';
    },
    toggleLabel() {
      return this.isIos26 ? '切换到经典界面' : '切换到新界面';
    },
  },
  methods: {
    toggleMode() {
      this.$emit('change', this.isIos26 ? 'classic' : 'ios26');
    },
  },
};
</script>

<style scoped>
.ui-theme-toggle {
  position: fixed;
  right: max(18px, env(safe-area-inset-right));
  bottom: max(18px, env(safe-area-inset-bottom));
  z-index: 3000;
  display: inline-flex;
  min-height: 42px;
  align-items: center;
  gap: 9px;
  box-sizing: border-box;
  padding: 7px 13px 7px 9px;
  border: 1px solid rgba(90, 103, 115, 0.28);
  border-radius: 999px;
  color: #34404b;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 6px 20px rgba(31, 45, 58, 0.14);
  font: 600 13px/1.2 -apple-system, BlinkMacSystemFont, "Segoe UI",
    "Microsoft YaHei", sans-serif;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
  transition: border-color 160ms ease, box-shadow 160ms ease,
    transform 160ms ease;
}

.ui-theme-toggle:hover {
  border-color: rgba(64, 78, 91, 0.45);
  box-shadow: 0 8px 24px rgba(31, 45, 58, 0.18);
  transform: translateY(-1px);
}

.ui-theme-toggle:focus {
  outline: 3px solid rgba(10, 132, 255, 0.34);
  outline-offset: 3px;
}

.ui-theme-toggle:focus:not(:focus-visible) {
  outline: none;
}

.ui-theme-toggle:focus-visible {
  outline: 3px solid rgba(10, 132, 255, 0.34);
  outline-offset: 3px;
}

.ui-theme-toggle.is-ios26 {
  border-color: rgba(10, 132, 255, 0.22);
  color: #174b78;
  background: rgba(247, 251, 255, 0.94);
}

.toggle-track {
  position: relative;
  width: 34px;
  height: 20px;
  flex: 0 0 34px;
  border-radius: 999px;
  background: #a8b0b8;
  transition: background-color 160ms ease;
}

.toggle-thumb {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 1px 4px rgba(18, 30, 42, 0.3);
  transition: transform 160ms ease;
}

.is-ios26 .toggle-track {
  background: #0a84ff;
}

.is-ios26 .toggle-thumb {
  transform: translateX(14px);
}

.toggle-label {
  white-space: nowrap;
}

@media (max-width: 480px) {
  .ui-theme-toggle {
    right: max(12px, env(safe-area-inset-right));
    bottom: max(12px, env(safe-area-inset-bottom));
    min-height: 40px;
    padding-right: 11px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .ui-theme-toggle,
  .toggle-track,
  .toggle-thumb {
    transition: none;
  }
}
</style>

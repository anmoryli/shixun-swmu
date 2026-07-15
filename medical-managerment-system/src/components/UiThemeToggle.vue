<template>
  <button
    class="theme-toggle"
    type="button"
    :aria-label="label"
    :aria-pressed="isDark ? 'true' : 'false'"
    :title="label"
    @click="toggle"
  >
    <svg
      v-if="isDark"
      class="theme-toggle-icon"
      viewBox="0 0 24 24"
      width="20"
      height="20"
      fill="none"
      stroke="currentColor"
      stroke-width="2"
      stroke-linecap="round"
      stroke-linejoin="round"
      aria-hidden="true"
    >
      <circle cx="12" cy="12" r="4" />
      <path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41" />
    </svg>
    <svg
      v-else
      class="theme-toggle-icon"
      viewBox="0 0 24 24"
      width="20"
      height="20"
      fill="none"
      stroke="currentColor"
      stroke-width="2"
      stroke-linecap="round"
      stroke-linejoin="round"
      aria-hidden="true"
    >
      <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" />
    </svg>
  </button>
</template>

<script>
export default {
  name: 'UiThemeToggle',
  props: {
    theme: {
      type: String,
      required: true,
      validator(value) {
        return value === 'light' || value === 'dark';
      },
    },
  },
  computed: {
    isDark() {
      return this.theme === 'dark';
    },
    label() {
      return this.isDark ? '切换到日间模式' : '切换到夜间模式';
    },
  },
  methods: {
    toggle() {
      this.$emit('change', this.isDark ? 'light' : 'dark');
    },
  },
};
</script>

<style scoped>
.theme-toggle {
  position: fixed;
  right: 20px;
  bottom: 20px;
  z-index: 3000;
  display: grid;
  width: 44px;
  height: 44px;
  box-sizing: border-box;
  place-items: center;
  padding: 0;
  border: 1px solid var(--app-line-strong, rgba(44, 82, 99, 0.2));
  border-radius: 50%;
  background: var(--app-surface, #ffffff);
  color: var(--app-ink-soft, #5a6b7b);
  box-shadow: var(--app-shadow-sm, 0 6px 18px rgba(31, 58, 71, 0.07));
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
  transition: background-color 180ms ease, color 180ms ease, border-color 180ms ease,
    transform 180ms ease;
}

.theme-toggle:hover {
  color: var(--app-accent-strong, #167d72);
  border-color: var(--app-accent, #1f9d8f);
  transform: translateY(-1px);
}

.theme-toggle:focus {
  outline: none;
}

.theme-toggle:focus-visible {
  outline: 3px solid var(--app-accent-wash, rgba(31, 157, 143, 0.24));
  outline-offset: 2px;
}

.theme-toggle-icon {
  display: block;
}

@media (max-width: 480px) {
  .theme-toggle {
    right: 14px;
    bottom: 14px;
    width: 40px;
    height: 40px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .theme-toggle {
    transition: none;
  }
}
</style>

<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script>
export default {
  name: 'App'
}
</script>

<style>
:root {
  --c-bg: #f8fafc;
  --c-bg-card: #ffffff;
  --c-bg-elevated: #ffffff;
  --c-text: #1e293b;
  --c-text-secondary: #64748b;
  --c-text-muted: #94a3b8;
  --c-border: #e2e8f0;
  --c-border-light: #f1f5f9;
  --c-primary: #0ea5e9;
  --c-primary-hover: #38bdf8;
  --c-primary-dark: #0284c7;
  --c-danger: #ef4444;
  --c-success: #22c55e;
  --c-warning: #f59e0b;

  --font-display: 'Playfair Display', 'Noto Serif SC', 'Songti SC', Georgia, serif;
  --font-body: 'Outfit', 'PingFang SC', 'Microsoft YaHei', system-ui, sans-serif;
  --font-mono: 'JetBrains Mono', 'Fira Code', 'Consolas', monospace;

  --space-xs: 4px;
  --space-sm: 8px;
  --space-md: 16px;
  --space-lg: 24px;
  --space-xl: 32px;
  --space-2xl: 48px;
  --space-3xl: 64px;

  --radius-sm: 6px;
  --radius-md: 10px;
  --radius-lg: 14px;

  --shadow-sm: 0 1px 2px rgba(0,0,0,0.04);
  --shadow-md: 0 4px 12px rgba(0,0,0,0.06);
  --shadow-lg: 0 8px 24px rgba(0,0,0,0.08);
  --shadow-primary: 0 4px 14px rgba(14,165,233,0.15);

  --transition-fast: 0.15s ease;
  --transition-base: 0.25s ease;
  --transition-slow: 0.4s cubic-bezier(0.16, 1, 0.3, 1);

  --max-width: 1200px;
}

*,
*::before,
*::after {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

html {
  scroll-behavior: smooth;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

body {
  font-family: var(--font-body);
  font-size: 15px;
  line-height: 1.7;
  color: var(--c-text);
  background-color: var(--c-bg);
}

::selection {
  background: var(--c-primary);
  color: #fff;
}

a {
  color: var(--c-primary);
  text-decoration: none;
  transition: color var(--transition-fast);
}

a:hover {
  color: var(--c-primary-hover);
}

h1, h2, h3, h4, h5, h6 {
  font-family: var(--font-display);
  font-weight: 700;
  line-height: 1.3;
  color: var(--c-text);
}

.el-button--primary {
  --el-button-bg-color: var(--c-primary) !important;
  --el-button-border-color: var(--c-primary) !important;
  --el-button-hover-bg-color: var(--c-primary-hover) !important;
  --el-button-hover-border-color: var(--c-primary-hover) !important;
  --el-button-active-bg-color: var(--c-primary-dark) !important;
  --el-button-active-border-color: var(--c-primary-dark) !important;
}

.el-tag {
  --el-tag-bg-color: var(--c-border-light);
  --el-tag-border-color: transparent;
  --el-tag-text-color: var(--c-text);
}

.el-tag--dark {
  --el-tag-bg-color: var(--c-primary);
  --el-tag-text-color: #fff;
}

.el-input__wrapper {
  border-radius: var(--radius-sm) !important;
}

.el-pagination {
  --el-pagination-hover-color: var(--c-primary);
}
</style>

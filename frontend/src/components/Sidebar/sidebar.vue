<template>
  <div class="sidebar-wrap">
    <div class="sidebar-brand">
      <span class="brand-bracket">&lt;</span>
      <span class="brand-name">管理后台</span>
      <span class="brand-bracket">/&gt;</span>
    </div>
    <el-menu
      :default-active="activeMenu"
      class="sidebar-menu"
      background-color="#1e293b"
      text-color="#94a3b8"
      active-text-color="#0ea5e9"
      unique-opened
      router
    >
      <el-menu-item index="/home">
        <el-icon><House /></el-icon>
        <span>首页</span>
      </el-menu-item>

      <el-sub-menu index="user-management">
        <template #title>
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </template>
        <el-menu-item index="/user/list">用户列表</el-menu-item>
      </el-sub-menu>

      <el-sub-menu index="post-management">
        <template #title>
          <el-icon><Document /></el-icon>
          <span>文章管理</span>
        </template>
        <el-menu-item index="/post/list">文章列表</el-menu-item>
      </el-sub-menu>

      <el-sub-menu index="role-management">
        <template #title>
          <el-icon><Document /></el-icon>
          <span>角色管理</span>
        </template>
        <el-menu-item index="/role/list">角色列表</el-menu-item>
      </el-sub-menu>

      <el-sub-menu index="category-management">
        <template #title>
          <el-icon><Folder /></el-icon>
          <span>分类管理</span>
        </template>
        <el-menu-item index="/category/list">分类列表</el-menu-item>
      </el-sub-menu>

      <el-menu-item index="/settings">
        <el-icon><Setting /></el-icon>
        <span>系统设置</span>
      </el-menu-item>
    </el-menu>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import {
  House,
  User,
  Document,
  Folder,
  Setting
} from '@element-plus/icons-vue'

const route = useRoute()

const activeMenu = computed(() => {
  const { meta, path } = route
  if (meta.activeMenu) {
    return meta.activeMenu
  }
  return path
})
</script>

<style lang="scss" scoped>
.sidebar-wrap {
  height: 100%;
  background-color: #1e293b;
  width: 220px !important;
  overflow: hidden;
  display: flex;
  flex-direction: column;

  .sidebar-brand {
    height: 52px;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 2px;
    border-bottom: 2px solid var(--c-primary);

    .brand-bracket {
      font-family: var(--font-mono);
      font-size: 13px;
      color: #64748b;
    }

    .brand-name {
      font-family: var(--font-display);
      font-size: 16px;
      font-weight: 700;
      color: #f1f5f9;
      margin: 0 4px;
    }
  }

  .sidebar-menu {
    border: none;
    flex: 1;
    overflow-y: auto;
    width: 100% !important;

    &::-webkit-scrollbar {
      width: 3px;
    }

    &::-webkit-scrollbar-thumb {
      background: #334155;
      border-radius: 2px;
    }
  }

  :deep(.el-menu-item),
  :deep(.el-sub-menu__title) {
    height: 44px;
    line-height: 44px;
    font-size: 13px;
    font-weight: 500;
    letter-spacing: 0.01em;
    border-left: 3px solid transparent;
    transition: all var(--transition-fast);

    &:hover {
      background-color: #334155 !important;
      border-left-color: var(--c-primary);
    }
  }

  :deep(.el-menu-item.is-active) {
    background-color: #334155 !important;
    border-left-color: var(--c-primary);
    font-weight: 600;
  }

  :deep(.el-sub-menu .el-menu-item) {
    padding-left: 52px !important;
    font-size: 12px;
    height: 38px;
    line-height: 38px;
    border-left: 3px solid transparent;

    &:hover {
      border-left-color: var(--c-primary-dark);
    }
  }
}
</style>

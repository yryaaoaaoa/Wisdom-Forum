<!-- src/components/Sidebar/sidebar.vue -->
<template>
  <div class="sidebar-container">
    <div class="sidebar-logo">
      <h2>博客管理系统</h2>
    </div>
    <el-menu
        :default-active="activeMenu"
        class="sidebar-menu"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409eff"
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

      <el-sub-menu index="category-management">
        <template #title>
          <el-icon><Folder /></el-icon>
          <span>分类管理</span>
        </template>
        <el-menu-item index="/category/list">分类列表</el-menu-item>
        <el-menu-item index="/category/create">添加分类</el-menu-item>
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
  // if set path, the sidebar will highlight the path you set
  if (meta.activeMenu) {
    return meta.activeMenu
  }
  return path
})
</script>

<style lang="scss" scoped>
.sidebar-container {
  height: 100%;
  background-color: #304156;
  transition: width 0.28s;
  width: 210px !important;
  overflow: hidden;

  .sidebar-logo {
    height: 50px;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: #2b3a4d;

    h2 {
      color: #fff;
      margin: 0;
      font-size: 18px;
      font-weight: 600;
    }
  }

  .sidebar-menu {
    border: none;
    height: calc(100% - 50px);
    width: 100% !important;
  }

  :deep(.el-menu-item),
  :deep(.el-sub-menu__title) {
    &:hover {
      background-color: #263445 !important;
    }
  }

  :deep(.el-menu-item.is-active) {
    background-color: #1f2d3d !important;
  }
}
</style>

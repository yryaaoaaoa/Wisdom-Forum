<!-- src/views/Home/home.vue -->
<template>
  <div class="app-wrapper">
    <!-- 侧边栏 -->
    <div class="sidebar-container">
      <Sidebar />
    </div>

    <!-- 主容器 -->
    <div class="main-container">
      <!-- 头部导航 -->
      <div class="navbar">
        <div class="navbar-left">
          <!-- 可以添加面包屑导航等内容 -->
        </div>
        <div class="navbar-right">
          <el-dropdown @command="handleUserCommand">
            <span class="el-dropdown-link">
              <el-avatar :size="30" icon="el-icon-user-solid" />
              <span class="user-name">管理员</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="settings">设置</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>

      <!-- 主要内容区域 -->
      <div class="app-main">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import Sidebar from '@/components/Sidebar/sidebar.vue'

const router = useRouter()

const handleUserCommand = (command) => {
  if (command === 'logout') {
    // 完整的退出登录逻辑
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('rememberedUsername')
    // 可以添加更多清理逻辑，如清除用户信息缓存等
    router.push('/login')
  } else if (command === 'profile') {
    // router.push('/profile')
  } else if (command === 'settings') {
    // router.push('/settings')
  }
}

</script>

<style lang="scss" scoped>
.app-wrapper {
  display: flex;
  height: 100vh;
  width: 100%;
  position: relative;
}

.sidebar-container {
  width: 210px;
  background-color: #304156;
  transition: width 0.28s;
  height: 100%;
  position: fixed;
  top: 0;
  left: 0;
  z-index: 1001;
  overflow: hidden;
}

.main-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  height: 100%;
  margin-left: 210px;
}

.navbar {
  height: 50px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);

  .navbar-left {
    display: flex;
    align-items: center;
  }

  .navbar-right {
    display: flex;
    align-items: center;

    .user-name {
      margin-left: 10px;
      font-size: 14px;
    }
  }
}

.app-main {
  flex: 1;
  overflow: auto;
  padding: 20px;
  background-color: #f0f2f5;
}
</style>

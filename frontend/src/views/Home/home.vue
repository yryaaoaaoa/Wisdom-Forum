<template>
  <div class="admin-layout">
    <div class="admin-sidebar">
      <Sidebar />
    </div>

    <div class="admin-main">
      <header class="admin-navbar">
        <div class="navbar-left">
          <button class="back-btn" @click="goToBlog">
            <el-icon><House /></el-icon>
            <span>返回前台</span>
          </button>
        </div>
        <div class="navbar-right">
          <el-dropdown @command="handleUserCommand" trigger="click">
            <span class="user-link">
              <el-avatar :size="28" :src="getAvatarUrl(avatarUrl)" class="nav-avatar">
                <el-icon :size="14"><User /></el-icon>
              </el-avatar>
              <span class="user-label">{{ username }}</span>
              <span class="user-chevron">▾</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>个人中心
                </el-dropdown-item>
                <el-dropdown-item command="settings">
                  <el-icon><Setting /></el-icon>设置
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="admin-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { House, User, Setting, SwitchButton } from '@element-plus/icons-vue'
import Sidebar from '@/components/Sidebar/sidebar.vue'
import request from '@/utils/requests'
import { getAvatarUrl } from '@/utils/avatar.js'
import { handleLogout } from '@/utils/logout.js'

const router = useRouter()
const username = ref(localStorage.getItem('username') || '管理员')
const avatarUrl = ref('')

const fetchUserInfo = async () => {
  try {
    const res = await request({ url: '/api/users/me', method: 'get' })
    if (res.data?.data) {
      username.value = res.data.data.username || res.data.data.userName || '管理员'
      avatarUrl.value = res.data.data.avatarUrl || ''
      localStorage.setItem('username', username.value)
    }
  } catch (error) {
    console.error('获取用户信息失败:', error)
  }
}

const goToBlog = () => {
  router.push('/blog')
}

const handleUserCommand = async (command) => {
  if (command === 'logout') {
    await handleLogout('/login')
  } else if (command === 'profile') {
    router.push('/blog/profile')
  } else if (command === 'settings') {
    router.push('/settings')
  }
}

onMounted(() => {
  fetchUserInfo()
})
</script>

<style lang="scss" scoped>
.admin-layout {
  display: flex;
  height: 100vh;
  width: 100%;
  position: relative;
}

.admin-sidebar {
  width: 220px;
  position: fixed;
  top: 0;
  left: 0;
  z-index: 1001;
  height: 100%;
  overflow: hidden;
}

.admin-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  height: 100%;
  margin-left: 220px;
  background: var(--c-bg);
}

.admin-navbar {
  height: 52px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 var(--space-lg);
  background: var(--c-bg-card);
  border-bottom: 1px solid var(--c-border);
  box-shadow: var(--shadow-sm);

  .navbar-left {
    display: flex;
    align-items: center;
  }

  .back-btn {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 6px 12px;
    font-family: var(--font-body);
    font-size: 13px;
    font-weight: 500;
    color: var(--c-text-secondary);
    background: transparent;
    border: 1px solid var(--c-border);
    border-radius: var(--radius-md);
    cursor: pointer;
    transition: all var(--transition-fast);

    &:hover {
      color: var(--c-primary);
      border-color: var(--c-primary);
    }
  }

  .navbar-right {
    display: flex;
    align-items: center;
  }

  .user-link {
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;

    .nav-avatar {
      background: var(--c-primary);
    }

    .user-label {
      font-size: 13px;
      font-weight: 500;
      color: var(--c-text);
    }

    .user-chevron {
      font-size: 10px;
      color: var(--c-text-muted);
    }
  }
}

.admin-content {
  flex: 1;
  overflow: auto;
  padding: var(--space-lg);
  background: var(--c-bg);
}
</style>

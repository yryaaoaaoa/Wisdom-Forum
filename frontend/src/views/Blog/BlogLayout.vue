<template>
  <div class="blog-layout">
    <header class="blog-header">
      <div class="header-inner">
        <div class="logo" @click="$router.push('/blog')">
          <span class="logo-bracket">&lt;</span>
          <span class="logo-name">智汇论坛</span>
          <span class="logo-bracket">/&gt;</span>
        </div>

        <nav class="nav-menu">
          <router-link to="/blog" class="nav-item" active-class="active">
            <span class="nav-label">首页</span>
            <span class="nav-line"></span>
          </router-link>
          <router-link to="/blog/categories" class="nav-item" active-class="active">
            <span class="nav-label">分类</span>
            <span class="nav-line"></span>
          </router-link>
          <router-link to="/blog/archives" class="nav-item" active-class="active">
            <span class="nav-label">归档</span>
            <span class="nav-line"></span>
          </router-link>
          <router-link to="/blog/about" class="nav-item" active-class="active">
            <span class="nav-label">关于</span>
            <span class="nav-line"></span>
          </router-link>
        </nav>

        <div class="header-actions">
          <div class="search-box" :class="{ expanded: searchExpanded }">
            <el-icon class="search-icon" @click="toggleSearch"><Search /></el-icon>
            <input
              v-if="searchExpanded"
              ref="searchInputRef"
              v-model="searchKeyword"
              placeholder="搜索..."
              class="search-field"
              @keyup.enter="handleSearch"
              @blur="collapseSearch"
            />
          </div>

          <el-badge
            v-if="isLoggedIn"
            :value="unreadCount"
            :hidden="unreadCount === 0"
            class="notification-badge"
          >
            <el-icon class="action-icon" @click="$router.push('/blog/notifications')">
              <Bell />
            </el-icon>
          </el-badge>

          <el-dropdown v-if="isLoggedIn" @command="handleUserCommand" trigger="click">
            <span class="user-trigger">
              <el-avatar :size="30" :src="userAvatar" class="user-avatar">
                <el-icon :size="16"><User /></el-icon>
              </el-avatar>
              <span class="user-chevron">▾</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="admin">
                  <el-icon><Setting /></el-icon>管理后台
                </el-dropdown-item>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>个人中心
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <button v-else class="login-btn" @click="$router.push('/login')">
            登录
          </button>
        </div>
      </div>
    </header>

    <main class="blog-main">
      <router-view v-slot="{ Component }">
        <transition name="page" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>

    <footer class="blog-footer">
      <div class="footer-inner">
        <div class="footer-brand">
          <span class="footer-logo">&lt;智汇论坛/&gt;</span>
          <p class="footer-desc">汇聚智慧，分享知识</p>
        </div>
        <div class="footer-divider"></div>
        <div class="footer-bottom">
          <span class="footer-copy">© {{ new Date().getFullYear() }} 智汇论坛. Powered by Vue 3 & Spring Boot</span>
          <div class="footer-links">
            <a href="https://github.com" target="_blank" rel="noopener">
              <el-icon><Link /></el-icon> GitHub
            </a>
          </div>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { Search, User, Setting, SwitchButton, Link, Bell } from '@element-plus/icons-vue'
import { getUnreadCount } from '@/api/notification/notification.js'
import { getCurrentUser } from '@/api/user/profile.js'
import { getAvatarUrl } from '@/utils/avatar.js'
import { handleLogout } from '@/utils/logout.js'

const router = useRouter()
const searchKeyword = ref('')
const searchExpanded = ref(false)
const searchInputRef = ref(null)
const unreadCount = ref(0)
const avatarUrl = ref('')

const isLoggedIn = computed(() => !!localStorage.getItem('access_token'))
const userAvatar = computed(() => getAvatarUrl(avatarUrl.value))

const toggleSearch = () => {
  searchExpanded.value = !searchExpanded.value
  if (searchExpanded.value) {
    nextTick(() => searchInputRef.value?.focus())
  }
}

const collapseSearch = () => {
  setTimeout(() => {
    if (!searchKeyword.value.trim()) {
      searchExpanded.value = false
    }
  }, 200)
}

const handleSearch = () => {
  if (searchKeyword.value.trim()) {
    router.push({ path: '/blog', query: { keyword: searchKeyword.value } })
    searchExpanded.value = false
  }
}

const fetchUserInfo = async () => {
  if (!isLoggedIn.value) return
  try {
    const res = await getCurrentUser()
    if (res.data.code === 200 && res.data.data) {
      avatarUrl.value = res.data.data.avatarUrl || ''
    }
  } catch { /* ignore */ }
}

const fetchUnreadCount = async () => {
  if (!isLoggedIn.value) return
  try {
    const res = await getUnreadCount()
    if (res.data.code === 200) {
      unreadCount.value = res.data.data
    }
  } catch { /* ignore */ }
}

const handleUserCommand = async (command) => {
  switch (command) {
    case 'admin':
      router.push('/home')
      break
    case 'profile':
      router.push('/blog/profile')
      break
    case 'logout':
      await handleLogout('/blog')
      break
  }
}

onMounted(() => {
  fetchUnreadCount()
  fetchUserInfo()
})
</script>

<style lang="scss" scoped>
.blog-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: var(--c-bg);
}

.blog-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: var(--c-bg-card);
  border-bottom: 1px solid var(--c-border);
  box-shadow: var(--shadow-sm);

  .header-inner {
    max-width: var(--max-width);
    margin: 0 auto;
    padding: 0 var(--space-lg);
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
}

.logo {
  display: flex;
  align-items: center;
  gap: 2px;
  cursor: pointer;
  user-select: none;

  .logo-bracket {
    font-family: var(--font-mono);
    font-size: 14px;
    font-weight: 400;
    color: var(--c-text-muted);
    transition: color var(--transition-base);
  }

  .logo-name {
    font-family: var(--font-display);
    font-size: 20px;
    font-weight: 700;
    color: var(--c-text);
    letter-spacing: -0.02em;
    margin: 0 4px;
  }

  &:hover {
    .logo-bracket {
      color: var(--c-primary);
    }
  }
}

.nav-menu {
  display: flex;
  gap: var(--space-xl);
  align-items: center;

  .nav-item {
    position: relative;
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 6px 0;
    text-decoration: none;

    .nav-label {
      font-family: var(--font-body);
      font-size: 14px;
      font-weight: 500;
      color: var(--c-text-secondary);
      transition: color var(--transition-base);
    }

    .nav-line {
      display: block;
      width: 0;
      height: 2px;
      background: var(--c-primary);
      transition: width var(--transition-base);
      margin-top: 4px;
      border-radius: 1px;
    }

    &:hover .nav-label {
      color: var(--c-text);
    }

    &:hover .nav-line {
      width: 100%;
    }

    &.active {
      .nav-label {
        color: var(--c-primary);
      }

      .nav-line {
        width: 100%;
      }
    }
  }
}

.header-actions {
  display: flex;
  align-items: center;
  gap: var(--space-md);
}

.search-box {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: 6px 10px;
  border-radius: var(--radius-md);
  transition: all var(--transition-base);

  .search-icon {
    font-size: 18px;
    color: var(--c-text-muted);
    cursor: pointer;
    transition: color var(--transition-fast);

    &:hover {
      color: var(--c-primary);
    }
  }

  .search-field {
    width: 160px;
    padding: 4px 0;
    background: transparent;
    border: none;
    outline: none;
    font-family: var(--font-body);
    font-size: 13px;
    color: var(--c-text);
    caret-color: var(--c-primary);

    &::placeholder {
      color: var(--c-text-muted);
    }
  }

  &.expanded {
    background: var(--c-bg);
    border: 1px solid var(--c-border);
  }
}

.notification-badge {
  .action-icon {
    font-size: 18px;
    color: var(--c-text-muted);
    cursor: pointer;
    transition: color var(--transition-fast);

    &:hover {
      color: var(--c-primary);
    }
  }

  :deep(.el-badge__content) {
    background-color: var(--c-primary);
    border: none;
  }
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;

  .user-avatar {
    background: var(--c-border-light);
    border: 2px solid var(--c-border);
    transition: border-color var(--transition-fast);

    &:hover {
      border-color: var(--c-primary);
    }
  }

  .user-chevron {
    font-size: 10px;
    color: var(--c-text-muted);
  }
}

.login-btn {
  padding: 8px 20px;
  font-family: var(--font-body);
  font-size: 13px;
  font-weight: 600;
  color: #fff;
  background: var(--c-primary);
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-fast);

  &:hover {
    background: var(--c-primary-hover);
    transform: translateY(-1px);
    box-shadow: var(--shadow-primary);
  }
}

.blog-main {
  flex: 1;
  max-width: var(--max-width);
  width: 100%;
  margin: 0 auto;
  padding: var(--space-xl) var(--space-lg);
}

.blog-footer {
  margin-top: auto;
  background: var(--c-bg-card);
  border-top: 1px solid var(--c-border);

  .footer-inner {
    max-width: var(--max-width);
    margin: 0 auto;
    padding: var(--space-2xl) var(--space-lg) var(--space-lg);
  }

  .footer-brand {
    margin-bottom: var(--space-lg);

    .footer-logo {
      font-family: var(--font-mono);
      font-size: 16px;
      color: var(--c-primary);
    }

    .footer-desc {
      margin-top: var(--space-sm);
      font-size: 13px;
      color: var(--c-text-muted);
      letter-spacing: 0.02em;
    }
  }

  .footer-divider {
    height: 1px;
    background: var(--c-border);
    margin-bottom: var(--space-lg);
  }

  .footer-bottom {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .footer-copy {
      font-size: 12px;
      color: var(--c-text-muted);
    }

    .footer-links {
      display: flex;
      gap: var(--space-lg);

      a {
        display: flex;
        align-items: center;
        gap: 4px;
        font-size: 13px;
        color: var(--c-text-secondary);
        text-decoration: none;
        transition: color var(--transition-fast);

        &:hover {
          color: var(--c-primary);
        }
      }
    }
  }
}

.page-enter-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.page-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}

.page-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.page-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>

<template>
  <div class="notification-page">
    <div v-if="!isLoggedIn" class="login-prompt">
      <el-empty description="请先登录后查看消息通知">
        <el-button type="primary" @click="router.push('/login')">去登录</el-button>
      </el-empty>
    </div>
    <template v-else>
    <div class="notification-header">
      <h2>消息通知</h2>
      <el-button 
        v-if="notifications.length > 0" 
        type="primary" 
        text 
        @click="handleMarkAllRead"
      >
        全部已读
      </el-button>
    </div>

    <div class="notification-list" v-loading="loading">
      <template v-if="notifications.length > 0">
        <div 
          v-for="item in notifications" 
          :key="item.id" 
          class="notification-item"
          :class="{ unread: item.isRead === 0 }"
          @click="handleClick(item)"
        >
          <el-avatar :size="40" :src="getAvatarUrl(item.senderAvatar)">
            <el-icon><User /></el-icon>
          </el-avatar>
          <div class="notification-content">
            <div class="notification-title">
              <span class="sender-name">{{ item.senderName }}</span>
              <span class="action-text">{{ getActionText(item.type) }}</span>
              <span class="target-title">《{{ item.targetTitle }}》</span>
            </div>
            <div class="notification-time">{{ formatTime(item.createTime) }}</div>
          </div>
          <div v-if="item.isRead === 0" class="unread-dot"></div>
        </div>
      </template>
      <el-empty v-else description="暂无通知" />
    </div>

    <div class="pagination-container" v-if="total > pageSize">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="fetchNotifications"
      />
    </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { User } from '@element-plus/icons-vue'
import { getNotifications, markAsRead, markAllAsRead } from '@/api/notification/notification.js'
import { getAvatarUrl } from '@/utils/avatar.js'

const router = useRouter()
const loading = ref(false)
const isLoggedIn = computed(() => !!localStorage.getItem('access_token'))
const notifications = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const getActionText = (type) => {
  const actions = {
    LIKE: '点赞了你的文章',
    COMMENT: '评论了你的文章',
    MENTION: '在评论中@了你'
  }
  return actions[type] || '与你互动'
}

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  return date.toLocaleDateString()
}

const fetchNotifications = async () => {
  loading.value = true
  try {
    const res = await getNotifications(currentPage.value, pageSize.value)
    if (res.data.code === 200) {
      notifications.value = res.data.data.records
      total.value = res.data.data.total
    }
  } catch (error) {
    console.error('获取通知失败:', error)
  } finally {
    loading.value = false
  }
}

const handleClick = async (item) => {
  if (item.isRead === 0) {
    await markAsRead(item.id)
    item.isRead = 1
  }
  if (item.targetId) {
    router.push(`/blog/article/${item.targetId}`)
  }
}

const handleMarkAllRead = async () => {
  try {
    await markAllAsRead()
    notifications.value.forEach(n => n.isRead = 1)
  } catch (error) {
    console.error('标记已读失败:', error)
  }
}

onMounted(() => {
  if (isLoggedIn.value) {
    fetchNotifications()
  }
})
</script>

<style lang="scss" scoped>
.notification-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;

  h2 {
    margin: 0;
    font-size: 20px;
    color: #303133;
  }
}

.notification-list {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.notification-item {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background-color 0.2s;

  &:hover {
    background-color: #f5f7fa;
  }

  &:last-child {
    border-bottom: none;
  }

  &.unread {
    background-color: #ecf5ff;
  }
}

.notification-content {
  flex: 1;
  margin-left: 12px;
}

.notification-title {
  font-size: 14px;
  color: #303133;

  .sender-name {
    font-weight: 500;
    color: #409eff;
  }

  .action-text {
    margin: 0 4px;
    color: #606266;
  }

  .target-title {
    color: #909399;
  }
}

.notification-time {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.unread-dot {
  width: 8px;
  height: 8px;
  background-color: #409eff;
  border-radius: 50%;
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>

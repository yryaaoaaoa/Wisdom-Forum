<template>
  <div class="dashboard">
    <div class="dashboard-header">
      <h1>仪表盘</h1>
      <p class="welcome-text">欢迎回来，{{ username }}</p>
    </div>

    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon articles">
          <el-icon><Document /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ stats.articleCount }}</span>
          <span class="stat-label">文章总数</span>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon categories">
          <el-icon><Folder /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ stats.categoryCount }}</span>
          <span class="stat-label">分类数量</span>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon users">
          <el-icon><User /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ stats.userCount }}</span>
          <span class="stat-label">用户数量</span>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon views">
          <el-icon><View /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ formatNumber(stats.totalReadCount) }}</span>
          <span class="stat-label">总阅读量</span>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon likes">
          <el-icon><Star /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ formatNumber(stats.totalLikeCount) }}</span>
          <span class="stat-label">总点赞数</span>
        </div>
      </div>
    </div>

    <div class="dashboard-content">
      <div class="recent-section">
        <div class="section-header">
          <h2>最近文章</h2>
          <el-button text type="primary" @click="goToPostList">
            查看全部
            <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
        <div class="article-list" v-if="recentArticles.length > 0">
          <div 
            v-for="article in recentArticles" 
            :key="article.id" 
            class="article-item"
            @click="goToArticle(article.id)"
          >
            <div class="article-info">
              <span class="article-title">{{ article.title }}</span>
              <span class="article-meta">
                <el-tag size="small" :type="getStatusType(article.status)">
                  {{ getStatusText(article.status) }}
                </el-tag>
                <span class="article-date">{{ formatDate(article.createdAt) }}</span>
              </span>
            </div>
            <div class="article-stats">
              <span><el-icon><View /></el-icon> {{ article.readCount || 0 }}</span>
              <span><el-icon><Star /></el-icon> {{ article.likeCount || 0 }}</span>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无文章" :image-size="80" />
      </div>

      <div class="quick-actions">
        <h2>快捷操作</h2>
        <div class="action-grid">
          <div class="action-card" @click="goToPostList">
            <el-icon><EditPen /></el-icon>
            <span>写文章</span>
          </div>
          <div class="action-card" @click="goToUserList">
            <el-icon><UserFilled /></el-icon>
            <span>用户管理</span>
          </div>
          <div class="action-card" @click="goToCategoryList">
            <el-icon><Files /></el-icon>
            <span>分类管理</span>
          </div>
          <div class="action-card" @click="goToSettings">
            <el-icon><Setting /></el-icon>
            <span>系统设置</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { 
  Document, Folder, User, View, ArrowRight, Star,
  EditPen, UserFilled, Files, Setting 
} from '@element-plus/icons-vue'
import { getStats, pageArticles } from '@/api/post/post'

const router = useRouter()
const username = ref(localStorage.getItem('username') || '管理员')

const stats = ref({
  articleCount: 0,
  categoryCount: 0,
  userCount: 0,
  totalReadCount: 0,
  totalLikeCount: 0
})

const recentArticles = ref([])

const fetchStats = async () => {
  try {
    const res = await getStats()
    if (res.data?.code === 200) {
      stats.value.articleCount = res.data.data.articleCount || 0
      stats.value.categoryCount = res.data.data.categoryCount || 0
      stats.value.userCount = res.data.data.userCount || 0
      stats.value.totalReadCount = res.data.data.totalReadCount || 0
      stats.value.totalLikeCount = res.data.data.totalLikeCount || 0
    }
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

const fetchRecentArticles = async () => {
  try {
    const res = await pageArticles({
      current: 1,
      size: 5,
      status: 1
    })
    recentArticles.value = res.data?.data?.records || []
  } catch (error) {
    console.error('获取最近文章失败:', error)
  }
}

const formatNumber = (num) => {
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + 'w'
  }
  return num
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN')
}

const getStatusType = (status) => {
  const types = {
    1: 'success',
    0: 'info'
  }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = {
    1: '已发布',
    0: '草稿'
  }
  return texts[status] || '未知'
}

const goToPostList = () => router.push('/post/list')
const goToUserList = () => router.push('/user/list')
const goToCategoryList = () => router.push('/category/list')
const goToSettings = () => router.push('/settings')
const goToArticle = (id) => router.push(`/blog/article/${id}`)

onMounted(() => {
  fetchStats()
  fetchRecentArticles()
})
</script>

<style lang="scss" scoped>
.dashboard {
  padding: 0;
}

.dashboard-header {
  margin-bottom: 24px;
  
  h1 {
    font-size: 24px;
    font-weight: 600;
    color: var(--c-text);
    margin: 0 0 8px 0;
  }
  
  .welcome-text {
    font-size: 14px;
    color: var(--c-text-secondary);
    margin: 0;
  }
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: var(--c-bg-card);
  border-radius: var(--radius-lg);
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: var(--shadow-sm);
  transition: transform var(--transition-fast), box-shadow var(--transition-fast);
  
  &:hover {
    transform: translateY(-2px);
    box-shadow: var(--shadow-md);
  }
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  
  &.articles {
    background: rgba(64, 158, 255, 0.1);
    color: #409eff;
  }
  
  &.categories {
    background: rgba(103, 194, 58, 0.1);
    color: #67c23a;
  }
  
  &.users {
    background: rgba(230, 162, 60, 0.1);
    color: #e6a23c;
  }
  
  &.views {
    background: rgba(144, 147, 153, 0.1);
    color: #909399;
  }
  
  &.likes {
    background: rgba(245, 108, 108, 0.1);
    color: #f56c6c;
  }
}

.stat-info {
  display: flex;
  flex-direction: column;
  
  .stat-value {
    font-size: 28px;
    font-weight: 600;
    color: var(--c-text);
    line-height: 1.2;
  }
  
  .stat-label {
    font-size: 13px;
    color: var(--c-text-secondary);
    margin-top: 4px;
  }
}

.dashboard-content {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 24px;
}

.recent-section {
  background: var(--c-bg-card);
  border-radius: var(--radius-lg);
  padding: 20px;
  box-shadow: var(--shadow-sm);
  
  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
    
    h2 {
      font-size: 16px;
      font-weight: 600;
      color: var(--c-text);
      margin: 0;
    }
  }
}

.article-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.article-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background var(--transition-fast);
  
  &:hover {
    background: var(--c-bg-hover);
  }
}

.article-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
  
  .article-title {
    font-size: 14px;
    font-weight: 500;
    color: var(--c-text);
  }
  
  .article-meta {
    display: flex;
    align-items: center;
    gap: 8px;
    
    .article-date {
      font-size: 12px;
      color: var(--c-text-muted);
    }
  }
}

.article-stats {
  display: flex;
  gap: 16px;
  
  span {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 13px;
    color: var(--c-text-secondary);
  }
}

.quick-actions {
  background: var(--c-bg-card);
  border-radius: var(--radius-lg);
  padding: 20px;
  box-shadow: var(--shadow-sm);
  
  h2 {
    font-size: 16px;
    font-weight: 600;
    color: var(--c-text);
    margin: 0 0 16px 0;
  }
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.action-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 20px 16px;
  border-radius: var(--radius-md);
  background: var(--c-bg);
  cursor: pointer;
  transition: all var(--transition-fast);
  
  &:hover {
    background: var(--c-primary);
    color: white;
  }
  
  .el-icon {
    font-size: 24px;
  }
  
  span {
    font-size: 13px;
    font-weight: 500;
  }
}

@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: repeat(3, 1fr);
  }
  
  .dashboard-content {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>

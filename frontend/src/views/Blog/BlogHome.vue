<template>
  <div class="blog-home">
    <div class="main-content">
      <div class="section-bar">
        <h2 class="section-title">
          <template v-if="searchKeyword">
            搜索结果 <small>找到 {{ total }} 篇关于 "{{ searchKeyword }}" 的文章</small>
          </template>
          <template v-else>
            最新文章
          </template>
        </h2>
        <div class="filter-strip">
          <button
            v-for="tag in categories"
            :key="tag.id ?? 'all'"
            class="filter-chip"
            :class="{ active: activeCategory === tag.id }"
            @click="filterByCategory(tag.id)"
          >
            {{ tag.name }}
          </button>
        </div>
      </div>

      <div v-if="loading" class="loading-skeleton">
        <div class="skeleton-card" v-for="n in 3" :key="n">
          <div class="sk-cover"></div>
          <div class="sk-body">
            <div class="sk-line sk-title"></div>
            <div class="sk-line sk-text"></div>
            <div class="sk-line sk-text short"></div>
          </div>
        </div>
      </div>

      <template v-else>
        <article
          v-for="(article, index) in articles"
          :key="article.id"
          class="article-card"
          :style="{ animationDelay: `${index * 0.06}s` }"
          @click="goToArticle(article.id)"
        >
          <div class="article-cover" v-if="article.coverUrl">
            <img :src="article.coverUrl" :alt="article.title" />
            <div class="cover-overlay"></div>
          </div>
          <div class="article-body">
            <div class="article-meta">
              <span class="meta-item category-tag">
                <el-icon :size="12"><Folder /></el-icon>
                {{ article.categoryName || '未分类' }}
              </span>
              <span class="meta-item">
                <el-icon :size="12"><Calendar /></el-icon>
                {{ formatDate(article.createdAt) }}
              </span>
            </div>
            <h3 class="article-title">{{ article.title }}</h3>
            <p class="article-summary">{{ article.summary }}</p>
            <div class="article-footer">
              <div class="article-stats">
                <span class="stat"><el-icon :size="13"><View /></el-icon> {{ article.readCount || 0 }}</span>
                <span class="stat"><el-icon :size="13"><Star /></el-icon> {{ article.likeCount || 0 }}</span>
                <span class="stat"><el-icon :size="13"><ChatDotRound /></el-icon> {{ article.commentCount || 0 }}</span>
              </div>
              <span class="read-link">
                阅读 <el-icon :size="12"><ArrowRight /></el-icon>
              </span>
            </div>
          </div>
        </article>

        <el-empty v-if="!articles.length" description="暂无文章" />

        <div class="pagination-wrap" v-if="total > pageSize">
          <el-pagination
            v-model:current-page="currentPage"
            :page-size="pageSize"
            :total="total"
            layout="prev, pager, next"
            @current-change="handlePageChange"
          />
        </div>
      </template>
    </div>

    <aside class="sidebar">
      <div class="sidebar-block author-block">
        <el-avatar :size="72" class="author-avatar">
          <el-icon :size="36"><User /></el-icon>
        </el-avatar>
        <h3 class="author-name">智汇论坛</h3>
        <p class="author-bio">汇聚智慧，分享知识</p>
        <div class="author-stats">
          <div class="as-item">
            <span class="as-val">{{ stats.articleCount }}</span>
            <span class="as-label">文章</span>
          </div>
          <div class="as-item">
            <span class="as-val">{{ stats.categoryCount }}</span>
            <span class="as-label">分类</span>
          </div>
          <div class="as-item">
            <span class="as-val">{{ stats.viewCount }}</span>
            <span class="as-label">阅读</span>
          </div>
          <div class="as-item">
            <span class="as-val">{{ stats.likeCount }}</span>
            <span class="as-label">点赞</span>
          </div>
        </div>
      </div>

      <div class="sidebar-block">
        <h4 class="block-title">
          <el-icon><TrendCharts /></el-icon>
          热门文章
        </h4>
        <div class="hot-list">
          <div
            v-for="(article, index) in hotArticles"
            :key="article.id"
            class="hot-item"
            @click="goToArticle(article.id)"
          >
            <span class="hot-rank" :class="{ top: index < 3 }">{{ String(index + 1).padStart(2, '0') }}</span>
            <span class="hot-title">{{ article.title }}</span>
          </div>
        </div>
      </div>
    </aside>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { pageArticles, articleSearch, getStats, getHotArticles } from '@/api/post/post'
import { getAllCategories } from '@/api/category/category'
import {
  Folder, Calendar, View, Star, ChatDotRound,
  ArrowRight, User, TrendCharts
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const articles = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const activeCategory = ref(null)
const searchKeyword = ref('')

const categories = ref([
  { id: null, name: '全部' }
])

const fetchCategories = async () => {
  try {
    const res = await getAllCategories()
    if (res.data.code === 200) {
      const categoryList = res.data.data || []
      categories.value = [
        { id: null, name: '全部' },
        ...categoryList.map(c => ({ id: c.id, name: c.name }))
      ]
    }
  } catch (error) {
    console.error('获取分类列表失败:', error)
  }
}

const hotArticles = ref([])

const stats = reactive({
  articleCount: 0,
  categoryCount: 0,
  viewCount: 0,
  likeCount: 0
})

const fetchStats = async () => {
  try {
    const res = await getStats()
    if (res.data.code === 200) {
      stats.articleCount = res.data.data.articleCount || 0
      stats.categoryCount = res.data.data.categoryCount || 0
      stats.viewCount = res.data.data.totalReadCount || 0
      stats.likeCount = res.data.data.totalLikeCount || 0
    }
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

const fetchHotArticles = async () => {
  try {
    const res = await getHotArticles(5)
    if (res.data.code === 200) {
      hotArticles.value = res.data.data || []
    }
  } catch (error) {
    console.error('获取热门文章失败:', error)
  }
}

const fetchArticles = async () => {
  loading.value = true
  try {
    const keyword = route.query.keyword || ''
    searchKeyword.value = keyword

    if (keyword) {
      // 有关键词时走 ES 全文搜索
      const res = await articleSearch(keyword, currentPage.value, pageSize.value)
      if (res.data.code === 200) {
        articles.value = res.data.data.records || []
        total.value = res.data.data.total || 0
      }
    } else {
      // 无关键词时走 MySQL 分页查询
      const params = {
        current: currentPage.value,
        size: pageSize.value
      }
      if (activeCategory.value) {
        params.categoryId = activeCategory.value
      }

      const res = await pageArticles(params)
      if (res.data.code === 200) {
        articles.value = res.data.data.records || []
        total.value = res.data.data.total || 0
      }
    }
  } catch (error) {
    console.error('获取文章列表失败:', error)
  } finally {
    loading.value = false
  }
}

const filterByCategory = (categoryId) => {
  activeCategory.value = categoryId
  currentPage.value = 1
  fetchArticles()
}

const handlePageChange = (page) => {
  currentPage.value = page
  fetchArticles()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const goToArticle = (id) => {
  router.push(`/blog/article/${id}`)
}

const formatDate = (date) => {
  if (!date) return ''
  return new Date(date).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}

watch([() => route.query.keyword, () => route.query.category], ([keyword, category]) => {
  currentPage.value = 1
  searchKeyword.value = keyword || ''
  if (category) {
    activeCategory.value = Number(category)
  } else {
    activeCategory.value = null
  }
  fetchArticles()
})

onMounted(() => {
  if (route.query.category) {
    activeCategory.value = Number(route.query.category)
  }
  fetchCategories()
  fetchArticles()
  fetchStats()
  fetchHotArticles()
})
</script>

<style lang="scss" scoped>
.blog-home {
  display: flex;
  gap: var(--space-xl);

  .main-content {
    flex: 1;
    min-width: 0;
  }

  .sidebar {
    width: 300px;
    flex-shrink: 0;
  }
}

.section-bar {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: var(--space-lg);
  padding-bottom: var(--space-md);
  border-bottom: 2px solid var(--c-text);

  .section-title {
    font-family: var(--font-display);
    font-size: 24px;
    font-weight: 700;
    color: var(--c-text);
    letter-spacing: -0.02em;

    small {
      font-size: 13px;
      font-weight: 400;
      color: var(--c-text-muted);
      margin-left: 8px;
      font-family: var(--font-body);
      letter-spacing: 0;
    }
  }

  .filter-strip {
    display: flex;
    gap: var(--space-sm);
    flex-wrap: wrap;
    justify-content: flex-end;
  }

  .filter-chip {
    padding: 6px 14px;
    font-family: var(--font-body);
    font-size: 12px;
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

    &.active {
      color: #fff;
      background: var(--c-primary);
      border-color: var(--c-primary);
    }
  }
}

.article-card {
  background: var(--c-bg-card);
  border-radius: var(--radius-md);
  overflow: hidden;
  margin-bottom: var(--space-lg);
  cursor: pointer;
  transition: all var(--transition-base);
  border: 1px solid var(--c-border);
  animation: cardIn 0.4s cubic-bezier(0.16, 1, 0.3, 1) both;

  &:hover {
    border-color: var(--c-primary);
    box-shadow: var(--shadow-primary);
    transform: translateY(-2px);
  }

  &:hover .article-title {
    color: var(--c-primary);
  }

  &:hover .cover-overlay {
    opacity: 1;
  }

  &:hover .article-cover img {
    transform: scale(1.04);
  }

  .article-cover {
    height: 200px;
    overflow: hidden;
    position: relative;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      transition: transform var(--transition-slow);
    }

    .cover-overlay {
      position: absolute;
      bottom: 0;
      left: 0;
      right: 0;
      height: 60px;
      background: linear-gradient(transparent, rgba(0,0,0,0.3));
      opacity: 0;
      transition: opacity var(--transition-base);
    }
  }

  .article-body {
    padding: var(--space-lg);
  }

  .article-meta {
    display: flex;
    gap: var(--space-md);
    margin-bottom: var(--space-sm);

    .meta-item {
      display: flex;
      align-items: center;
      gap: 4px;
      font-size: 12px;
      font-weight: 500;
      letter-spacing: 0.02em;
      color: var(--c-text-muted);
    }

    .category-tag {
      color: var(--c-primary);
      font-weight: 600;
    }
  }

  .article-title {
    font-family: var(--font-display);
    font-size: 20px;
    font-weight: 700;
    color: var(--c-text);
    margin-bottom: var(--space-sm);
    line-height: 1.4;
    transition: color var(--transition-fast);
  }

  .article-summary {
    font-size: 14px;
    color: var(--c-text-secondary);
    line-height: 1.7;
    margin-bottom: var(--space-md);
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  .article-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-top: var(--space-md);
    border-top: 1px solid var(--c-border-light);

    .article-stats {
      display: flex;
      gap: var(--space-md);

      .stat {
        display: flex;
        align-items: center;
        gap: 4px;
        font-size: 12px;
        color: var(--c-text-muted);
      }
    }

    .read-link {
      display: flex;
      align-items: center;
      gap: 4px;
      font-size: 13px;
      font-weight: 600;
      color: var(--c-primary);
      letter-spacing: 0.02em;
      transition: gap var(--transition-fast);

      &:hover {
        gap: 8px;
      }
    }
  }
}

@keyframes cardIn {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: var(--space-2xl);
  padding-top: var(--space-lg);
  border-top: 1px solid var(--c-border);
}

.sidebar-block {
  background: var(--c-bg-card);
  border-radius: var(--radius-md);
  padding: var(--space-lg);
  margin-bottom: var(--space-lg);
  border: 1px solid var(--c-border);

  .block-title {
    display: flex;
    align-items: center;
    gap: var(--space-sm);
    font-family: var(--font-display);
    font-size: 16px;
    font-weight: 700;
    color: var(--c-text);
    margin-bottom: var(--space-md);
    padding-bottom: var(--space-sm);
    border-bottom: 2px solid var(--c-primary);
  }
}

.author-block {
  text-align: center;

  .author-avatar {
    margin-bottom: var(--space-md);
    background: var(--c-border-light);
    border: 3px solid var(--c-primary);
  }

  .author-name {
    font-family: var(--font-display);
    font-size: 20px;
    font-weight: 700;
    color: var(--c-text);
    margin-bottom: var(--space-xs);
  }

  .author-bio {
    font-size: 13px;
    color: var(--c-text-muted);
    margin-bottom: var(--space-md);
  }

  .author-stats {
    display: flex;
    justify-content: space-around;
    padding-top: var(--space-md);
    border-top: 1px solid var(--c-border-light);

    .as-item {
      text-align: center;

      .as-val {
        display: block;
        font-family: var(--font-display);
        font-size: 22px;
        font-weight: 700;
        color: var(--c-primary);
      }

      .as-label {
        font-size: 11px;
        font-weight: 500;
        letter-spacing: 0.06em;
        color: var(--c-text-muted);
      }
    }
  }
}

.hot-list {
  .hot-item {
    display: flex;
    align-items: center;
    gap: var(--space-md);
    padding: var(--space-sm) 0;
    cursor: pointer;
    border-bottom: 1px solid var(--c-border-light);
    transition: all var(--transition-fast);

    &:last-child {
      border-bottom: none;
    }

    &:hover .hot-title {
      color: var(--c-primary);
    }

    &:hover .hot-rank {
      color: var(--c-primary);
      border-color: var(--c-primary);
    }

    .hot-rank {
      width: 28px;
      height: 28px;
      border-radius: var(--radius-sm);
      border: 1px solid var(--c-border);
      color: var(--c-text-muted);
      font-family: var(--font-mono);
      font-size: 11px;
      font-weight: 600;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
      transition: all var(--transition-fast);

      &.top {
        background: var(--c-primary);
        border-color: var(--c-primary);
        color: #fff;
      }
    }

    .hot-title {
      flex: 1;
      font-size: 13px;
      font-weight: 500;
      color: var(--c-text);
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      transition: color var(--transition-fast);
    }
  }
}

.loading-skeleton {
  .skeleton-card {
    background: var(--c-bg-card);
    border-radius: var(--radius-md);
    overflow: hidden;
    margin-bottom: var(--space-lg);
    border: 1px solid var(--c-border);
  }

  .sk-cover {
    height: 200px;
    background: linear-gradient(90deg, var(--c-border-light) 25%, var(--c-border) 50%, var(--c-border-light) 75%);
    background-size: 200% 100%;
    animation: shimmer 1.5s infinite;
  }

  .sk-body {
    padding: var(--space-lg);
  }

  .sk-line {
    height: 14px;
    background: linear-gradient(90deg, var(--c-border-light) 25%, var(--c-border) 50%, var(--c-border-light) 75%);
    background-size: 200% 100%;
    animation: shimmer 1.5s infinite;
    margin-bottom: 10px;
    border-radius: var(--radius-sm);

    &.sk-title {
      width: 70%;
      height: 18px;
    }

    &.sk-text {
      width: 100%;
    }

    &.short {
      width: 50%;
    }
  }
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>

<template>
  <div class="category-articles-page">
    <aside class="sidebar">
      <div class="sidebar-header">
        <h3 class="sidebar-title">分类</h3>
        <p class="sidebar-count">共 {{ categories.length }} 个分类</p>
      </div>
      <nav class="category-nav">
        <a
          v-for="cat in categories"
          :key="cat.id"
          :class="{ active: currentCategoryId === cat.id }"
          @click.prevent="switchCategory(cat.id)"
        >
          <span>{{ cat.name }}</span>
          <span class="count">{{ cat.articleCount || 0 }}</span>
        </a>
      </nav>
    </aside>

    <main class="main-content">
      <div class="page-header">
        <h1>{{ categoryName }} <small>{{ total }} 篇文章</small></h1>
        <p v-if="categoryDesc" class="page-desc">{{ categoryDesc }}</p>
      </div>

      <div v-if="loading" class="loading-state">
        <div v-for="n in 3" :key="n" class="skeleton-card">
          <div class="sk-line sk-title"></div>
          <div class="sk-line sk-text"></div>
          <div class="sk-line sk-text short"></div>
        </div>
      </div>

      <template v-else>
        <div v-if="articles.length === 0" class="empty-state">
          <p>该分类下暂无文章</p>
        </div>

        <div v-else class="article-list">
          <a
            v-for="article in articles"
            :key="article.id"
            class="article-item"
            @click.prevent="goToArticle(article.id)"
          >
            <div class="article-meta">
              <span class="date">{{ formatDate(article.createdAt) }}</span>
              <span v-if="article.categoryName" class="tag">{{ article.categoryName }}</span>
            </div>
            <h3 class="article-title">{{ article.title }}</h3>
            <p class="article-summary">{{ article.summary }}</p>
            <div class="article-footer">
              <span class="stat">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                {{ article.readCount || 0 }}
              </span>
              <span class="stat">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
                {{ article.likeCount || 0 }}
              </span>
            </div>
          </a>
        </div>

        <div v-if="total > pageSize" class="pagination">
          <a
            v-for="p in pageNumbers"
            :key="p"
            :class="{ active: currentPage === p }"
            @click.prevent="handlePageChange(p)"
          >{{ p }}</a>
          <span v-if="totalPages > 5" class="ellipsis">...</span>
          <a v-if="totalPages > 5" :class="{ active: currentPage === totalPages }" @click.prevent="handlePageChange(totalPages)">{{ totalPages }}</a>
        </div>
      </template>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getAllCategories } from '@/api/category/category'
import { pageArticles } from '@/api/post/post'

const router = useRouter()
const route = useRoute()

const categories = ref([])
const articles = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const currentCategoryId = ref(null)
const categoryName = ref('')
const categoryDesc = ref('')

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

const pageNumbers = computed(() => {
  const pages = totalPages.value
  if (pages <= 5) return Array.from({ length: pages }, (_, i) => i + 1)
  const current = currentPage.value
  if (current <= 3) return [1, 2, 3, 4]
  if (current >= pages - 2) return [pages - 3, pages - 2, pages - 1]
  return [current - 1, current, current + 1]
})

const fetchCategories = async () => {
  try {
    const res = await getAllCategories()
    if (res.data.code === 200) {
      categories.value = res.data.data || []
    }
  } catch (error) {
    console.error('获取分类列表失败:', error)
  }
}

const fetchArticles = async () => {
  loading.value = true
  try {
    const params = {
      current: currentPage.value,
      size: pageSize.value
    }
    if (currentCategoryId.value) {
      params.categoryId = currentCategoryId.value
    }
    const res = await pageArticles(params)
    if (res.data.code === 200) {
      articles.value = res.data.data.records || []
      total.value = res.data.data.total || 0
    }
  } catch (error) {
    console.error('获取文章列表失败:', error)
    articles.value = []
  } finally {
    loading.value = false
  }
}

const updateCategoryMeta = () => {
  const cat = categories.value.find(c => c.id === currentCategoryId.value)
  categoryName.value = cat?.name || '未分类'
  categoryDesc.value = cat?.description || ''
}

const switchCategory = (categoryId) => {
  router.push(`/blog/category/${categoryId}`)
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
    year: 'numeric', month: '2-digit', day: '2-digit'
  })
}

watch(() => route.params.id, (newId) => {
  const id = Number(newId)
  if (id && id !== currentCategoryId.value) {
    currentCategoryId.value = id
    currentPage.value = 1
    updateCategoryMeta()
    fetchArticles()
  }
})

onMounted(() => {
  const id = Number(route.params.id)
  if (id) currentCategoryId.value = id
  fetchCategories().then(() => {
    updateCategoryMeta()
  })
  fetchArticles()
})
</script>

<style scoped>
.category-articles-page {
  display: flex;
  gap: var(--space-xl);
}

/* ─── Sidebar ─── */
.sidebar {
  width: 220px;
  flex-shrink: 0;
  position: sticky;
  top: calc(60px + var(--space-xl));
  align-self: start;
}

.sidebar-header {
  margin-bottom: var(--space-md);
  padding-bottom: var(--space-md);
  border-bottom: 2px solid var(--c-text);
}

.sidebar-title {
  font-family: var(--font-display);
  font-size: 18px;
  font-weight: 700;
  color: var(--c-text);
  margin-bottom: 2px;
}

.sidebar-count {
  font-size: 13px;
  color: var(--c-text-muted);
}

.category-nav {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.category-nav a {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 9px 12px;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 500;
  color: var(--c-text-secondary);
  text-decoration: none;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.category-nav a:hover {
  background: var(--c-border-light);
  color: var(--c-text);
}

.category-nav a.active {
  background: var(--c-primary);
  color: #fff;
}

.category-nav a .count {
  font-size: 12px;
  color: var(--c-text-muted);
  background: var(--c-bg);
  padding: 1px 8px;
  border-radius: 20px;
  font-family: var(--font-mono);
  min-width: 28px;
  text-align: center;
}

.category-nav a.active .count {
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
}

/* ─── Main ─── */
.main-content {
  flex: 1;
  min-width: 0;
}

.page-header {
  margin-bottom: var(--space-lg);
  padding-bottom: var(--space-lg);
  border-bottom: 2px solid var(--c-text);
}

.page-header h1 {
  font-family: var(--font-display);
  font-size: 28px;
  font-weight: 700;
  color: var(--c-text);
  letter-spacing: -0.02em;
}

.page-header h1 small {
  font-size: 15px;
  font-weight: 400;
  color: var(--c-text-muted);
  margin-left: 10px;
  font-family: var(--font-body);
}

.page-desc {
  font-size: 14px;
  color: var(--c-text-secondary);
  margin-top: var(--space-xs);
}

/* ─── Loading ─── */
.loading-state {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.skeleton-card {
  background: var(--c-bg-card);
  border-radius: var(--radius-md);
  padding: var(--space-lg);
  border: 1px solid var(--c-border);
}

.sk-line {
  height: 14px;
  background: linear-gradient(90deg, var(--c-border-light) 25%, var(--c-border) 50%, var(--c-border-light) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: var(--radius-sm);
  margin-bottom: 10px;
}

.sk-title {
  width: 70%;
  height: 20px;
}

.sk-text {
  width: 100%;
}

.sk-text.short {
  width: 50%;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* ─── Empty ─── */
.empty-state {
  text-align: center;
  padding: 64px 24px;
  color: var(--c-text-muted);
  font-size: 15px;
}

/* ─── Article List ─── */
.article-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.article-item {
  display: block;
  padding: var(--space-lg);
  background: var(--c-bg-card);
  border-radius: var(--radius-md);
  text-decoration: none;
  color: inherit;
  border: 1px solid var(--c-border);
  cursor: pointer;
  transition: all var(--transition-base);
}

.article-item:hover {
  border-color: var(--c-primary);
  box-shadow: var(--shadow-primary);
  transform: translateY(-2px);
}

.article-meta {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  margin-bottom: var(--space-sm);
  font-size: 12px;
  color: var(--c-text-muted);
}

.article-meta .date {
  font-family: var(--font-mono);
}

.article-meta .tag {
  padding: 2px 10px;
  background: var(--c-border-light);
  border-radius: 20px;
  font-size: 11px;
  color: var(--c-primary);
  font-weight: 500;
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

.article-item:hover .article-title {
  color: var(--c-primary);
}

.article-summary {
  font-size: 14px;
  color: var(--c-text-secondary);
  line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.article-footer {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  margin-top: var(--space-md);
  padding-top: var(--space-md);
  border-top: 1px solid var(--c-border-light);
  font-size: 12px;
  color: var(--c-text-muted);
}

.article-footer .stat {
  display: flex;
  align-items: center;
  gap: 4px;
}

.article-footer .stat svg {
  opacity: 0.5;
}

/* ─── Pagination ─── */
.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-sm);
  margin-top: var(--space-xl);
  padding-top: var(--space-lg);
  border-top: 1px solid var(--c-border);
}

.pagination a {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 36px;
  height: 36px;
  font-size: 13px;
  font-weight: 500;
  color: var(--c-text-secondary);
  text-decoration: none;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.pagination a:hover {
  background: var(--c-border-light);
  color: var(--c-text);
}

.pagination a.active {
  background: var(--c-primary);
  color: #fff;
}

.pagination .ellipsis {
  font-size: 13px;
  color: var(--c-text-muted);
  padding: 0 4px;
}
</style>

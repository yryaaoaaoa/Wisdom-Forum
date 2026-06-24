<template>
  <div class="categories-page">
    <div class="page-header">
      <h1>分类 <small>探索 {{ categories.length }} 个技术领域</small></h1>
      <span class="stats-line">共 {{ totalArticleCount }} 篇文章</span>
    </div>

    <div class="categories-grid">
      <div
        v-for="cat in categories"
        :key="cat.id"
        class="category-card"
        :style="{ '--card-accent': cat.color, '--card-accent-bg': cat.colorBg }"
        @click="selectCategory(cat)"
      >
        <div class="card-accent-line"></div>
        <div class="card-top">
          <div class="card-icon">{{ cat.icon }}</div>
          <div class="card-info">
            <h3 class="card-name">{{ cat.name }}</h3>
            <span class="card-count">{{ cat.articleCount }} 篇文章</span>
          </div>
        </div>

        <p v-if="cat.description" class="card-desc">{{ cat.description }}</p>

        <div class="card-preview">
          <div class="preview-label">最近文章</div>
          <div v-if="cat.loading" class="preview-loading">
            <span class="preview-skeleton"></span>
            <span class="preview-skeleton"></span>
          </div>
          <template v-else-if="cat.recentArticles && cat.recentArticles.length > 0">
            <div
              v-for="article in cat.recentArticles"
              :key="article.id"
              class="preview-item"
              @click.stop="goToArticle(article.id)"
            >
              {{ article.title }}
            </div>
          </template>
          <div v-else class="preview-empty">暂无文章</div>
        </div>

        <span class="card-arrow">→</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { getAllCategories } from '@/api/category/category'
import { pageArticles } from '@/api/post/post'

const router = useRouter()

const categories = ref([])

const totalArticleCount = computed(() =>
  categories.value.reduce((sum, c) => sum + (c.articleCount || 0), 0)
)

const categoryPalette = [
  { color: '#0ea5e9', colorBg: '#f0f9ff', icon: '☕' },
  { color: '#16a34a', colorBg: '#f0fdf4', icon: '🍃' },
  { color: '#8b5cf6', colorBg: '#f5f3ff', icon: '⚛' },
  { color: '#d97706', colorBg: '#fffbeb', icon: '🗄' },
  { color: '#dc2626', colorBg: '#fef2f2', icon: '🔧' },
  { color: '#0891b2', colorBg: '#ecfeff', icon: '🏗' },
  { color: '#7c3aed', colorBg: '#f5f3ff', icon: '🧮' },
  { color: '#78716c', colorBg: '#f5f5f4', icon: '✍' },
  { color: '#e11d48', colorBg: '#fff1f2', icon: '📊' },
  { color: '#65a30d', colorBg: '#f7fee7', icon: '⚙' },
]

const fetchCategories = async () => {
  try {
    const res = await getAllCategories()
    if (res.data.code === 200) {
      const list = res.data.data || []
      categories.value = list.map((c, i) => ({
        id: c.id,
        name: c.name,
        description: c.description || '',
        articleCount: c.articleCount || 0,
        color: categoryPalette[i % categoryPalette.length].color,
        colorBg: categoryPalette[i % categoryPalette.length].colorBg,
        icon: categoryPalette[i % categoryPalette.length].icon,
        recentArticles: [],
        loading: true
      }))
      // After categories are set, fetch previews in background
      fetchPreviews()
    }
  } catch (error) {
    console.error('获取分类失败:', error)
  }
}

const fetchPreviews = async () => {
  const results = await Promise.allSettled(
    categories.value.map(async (cat) => {
      const res = await pageArticles({ current: 1, size: 2, categoryId: cat.id })
      return { id: cat.id, articles: res.data.data?.records || [] }
    })
  )

  results.forEach((result) => {
    if (result.status === 'fulfilled') {
      const cat = categories.value.find(c => c.id === result.value.id)
      if (cat) {
        cat.recentArticles = result.value.articles
        cat.loading = false
      }
    }
  })
}

const selectCategory = (cat) => {
  router.push(`/blog/category/${cat.id}`)
}

const goToArticle = (id) => {
  router.push(`/blog/article/${id}`)
}

onMounted(() => {
  fetchCategories()
})
</script>

<style scoped>
.categories-page {
}

/* ─── Header ─── */
.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: var(--space-xl);
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
  font-size: 14px;
  font-weight: 400;
  color: var(--c-text-muted);
  margin-left: 10px;
  font-family: var(--font-body);
}

.page-header .stats-line {
  font-size: 13px;
  color: var(--c-text-muted);
  flex-shrink: 0;
}

/* ─── Grid ─── */
.categories-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: var(--space-lg);
}

.category-card {
  background: var(--c-bg-card);
  border-radius: var(--radius-md);
  border: 1px solid var(--c-border);
  padding: var(--space-lg);
  cursor: pointer;
  transition: all var(--transition-base);
  position: relative;
  overflow: hidden;
}

.card-accent-line {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: var(--card-accent, var(--c-primary));
  opacity: 0;
  transition: opacity var(--transition-base);
}

.category-card:hover {
  border-color: var(--card-accent, var(--c-primary));
  box-shadow: 0 4px 14px rgba(0,0,0,0.06), 0 0 0 1px var(--card-accent, transparent);
  transform: translateY(-3px);
}

.category-card:hover .card-accent-line {
  opacity: 1;
}

/* ─── Top Section ─── */
.card-top {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  margin-bottom: var(--space-sm);
}

.card-icon {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-sm);
  background: var(--card-accent-bg, var(--c-border-light));
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
  transition: transform var(--transition-fast);
}

.category-card:hover .card-icon {
  transform: scale(1.08);
}

.card-name {
  font-family: var(--font-display);
  font-size: 18px;
  font-weight: 700;
  color: var(--c-text);
  margin-bottom: 1px;
  line-height: 1.3;
}

.card-count {
  font-size: 12px;
  color: var(--c-text-muted);
  font-family: var(--font-mono);
}

/* ─── Description ─── */
.card-desc {
  font-size: 13px;
  color: var(--c-text-secondary);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: var(--space-md);
  min-height: 39px;
}

/* ─── Preview ─── */
.card-preview {
  padding-top: var(--space-sm);
  border-top: 1px solid var(--c-border-light);
}

.preview-label {
  font-size: 11px;
  color: var(--c-text-muted);
  font-weight: 500;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  margin-bottom: 6px;
}

.preview-item {
  font-size: 13px;
  color: var(--c-text-secondary);
  padding: 3px 0;
  padding-left: 12px;
  position: relative;
  transition: color var(--transition-fast);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.preview-item::before {
  content: '·';
  position: absolute;
  left: 2px;
  color: var(--card-accent, var(--c-primary));
  font-weight: 700;
}

.category-card:hover .preview-item {
  color: var(--c-text);
}

/* ─── Preview Loading ─── */
.preview-loading {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 4px 0;
}

.preview-skeleton {
  height: 12px;
  border-radius: 4px;
  background: linear-gradient(90deg, var(--c-border-light) 25%, var(--c-border) 50%, var(--c-border-light) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.preview-skeleton:first-child {
  width: 85%;
}

.preview-skeleton:last-child {
  width: 60%;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* ─── Empty Preview ─── */
.preview-empty {
  font-size: 12px;
  color: var(--c-text-muted);
  padding: 4px 0;
}

/* ─── Arrow ─── */
.card-arrow {
  position: absolute;
  right: var(--space-lg);
  bottom: var(--space-lg);
  font-size: 16px;
  color: var(--c-border);
  transition: all var(--transition-fast);
  line-height: 1;
}

.category-card:hover .card-arrow {
  color: var(--card-accent, var(--c-primary));
  transform: translateX(4px);
}

/* ─── Responsive ─── */
@media (max-width: 640px) {
  .categories-grid {
    grid-template-columns: 1fr;
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--space-xs);
  }

  .card-arrow {
    display: none;
  }
}
</style>

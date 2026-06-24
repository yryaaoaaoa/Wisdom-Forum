<template>
  <div class="archives-page">
    <div class="page-header">
      <h1>归档</h1>
      <p>共 {{ totalArticles }} 篇文章</p>
    </div>
    
    <div class="timeline">
      <div
        v-for="(group, year) in articleGroups"
        :key="year"
        class="year-group"
      >
        <div class="year-header">
          <span class="year">{{ year }}</span>
          <span class="count">{{ group.length }} 篇</span>
        </div>
        
        <div class="articles">
          <div
            v-for="article in group"
            :key="article.id"
            class="article-item"
            @click="goToArticle(article.id)"
          >
            <span class="date">{{ formatMonthDay(article.createdAt) }}</span>
            <span class="title">{{ article.title }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { pageArticles } from '@/api/post/post'

const router = useRouter()

const articles = ref([])
const totalArticles = ref(0)

const articleGroups = computed(() => {
  const groups = {}
  articles.value.forEach(article => {
    const year = new Date(article.createdAt).getFullYear()
    if (!groups[year]) {
      groups[year] = []
    }
    groups[year].push(article)
  })
  return Object.keys(groups).sort((a, b) => b - a).reduce((obj, key) => {
    obj[key] = groups[key]
    return obj
  }, {})
})

const fetchArticles = async () => {
  try {
    const res = await pageArticles({ current: 1, size: 100 })
    if (res.data.code === 200) {
      articles.value = res.data.data.records || []
      totalArticles.value = res.data.data.total || 0
    }
  } catch (error) {
    console.error('获取文章失败:', error)
  }
}

const goToArticle = (id) => {
  router.push(`/blog/article/${id}`)
}

const formatMonthDay = (date) => {
  if (!date) return ''
  const d = new Date(date)
  return `${d.getMonth() + 1}-${String(d.getDate()).padStart(2, '0')}`
}

onMounted(() => {
  fetchArticles()
})
</script>

<style lang="scss" scoped>
.archives-page {
  .page-header {
    margin-bottom: 32px;

    h1 {
      font-family: var(--font-display);
      font-size: 28px;
      font-weight: 700;
      color: var(--c-text);
      margin-bottom: 8px;
    }

    p {
      color: var(--c-text-secondary);
    }
  }

  .timeline {
    .year-group {
      margin-bottom: 32px;

      .year-header {
        display: flex;
        align-items: center;
        gap: 12px;
        margin-bottom: 16px;

        .year {
          font-family: var(--font-display);
          font-size: 24px;
          font-weight: 700;
          color: var(--c-primary);
        }

        .count {
          font-size: 14px;
          color: var(--c-text-muted);
        }
      }

      .articles {
        padding-left: 24px;
        border-left: 2px solid var(--c-primary-dark);

        .article-item {
          display: flex;
          align-items: center;
          gap: 16px;
          padding: 12px 16px;
          margin-bottom: 8px;
          background: var(--c-bg-card);
          border-radius: var(--radius-md);
          cursor: pointer;
          transition: all 0.3s;
          border: 1px solid var(--c-border);

          &:hover {
            background: var(--c-bg);
            transform: translateX(4px);
            border-color: var(--c-primary);
          }

          .date {
            font-size: 13px;
            color: var(--c-text-muted);
            flex-shrink: 0;
            width: 50px;
          }

          .title {
            flex: 1;
            font-size: 15px;
            color: var(--c-text);
          }
        }
      }
    }
  }
}
</style>

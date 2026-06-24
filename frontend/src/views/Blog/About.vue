<template>
  <div class="about-page">
    <div class="about-header">
      <el-avatar :size="120" class="avatar">
        <el-icon :size="60"><User /></el-icon>
      </el-avatar>
      <h1>TechBlog</h1>
      <p class="bio">专注于技术分享与交流</p>
    </div>
    
    <div class="about-content">
      <section class="about-section">
        <h2>关于我</h2>
        <p>
          你好！我是一名热爱技术的开发者，专注于后端开发和系统架构设计。
          这个博客是我记录学习笔记、分享技术心得的地方。
        </p>
        <p>
          主要技术栈：Java、Spring Boot、Vue.js、MySQL、Redis、Docker 等。
        </p>
      </section>
      
      <section class="about-section">
        <h2>联系方式</h2>
        <div class="contact-list">
          <div class="contact-item">
            <el-icon><Message /></el-icon>
            <span>example@email.com</span>
          </div>
          <div class="contact-item">
            <el-icon><Link /></el-icon>
            <a href="https://github.com" target="_blank">GitHub</a>
          </div>
        </div>
      </section>
      
      <section class="about-section">
        <h2>博客统计</h2>
        <div class="stats-grid">
          <div class="stat-item">
            <span class="stat-value">{{ stats.articles }}</span>
            <span class="stat-label">文章</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ stats.categories }}</span>
            <span class="stat-label">分类</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ stats.views }}</span>
            <span class="stat-label">阅读</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ stats.days }}</span>
            <span class="stat-label">天数</span>
          </div>
        </div>
      </section>
      
      <section class="about-section">
        <h2>技术栈</h2>
        <div class="tech-stack">
          <div class="tech-category">
            <h4>后端</h4>
            <div class="tech-tags">
              <el-tag>Java</el-tag>
              <el-tag>Spring Boot</el-tag>
              <el-tag>MyBatis Plus</el-tag>
              <el-tag>MySQL</el-tag>
              <el-tag>Redis</el-tag>
              <el-tag>Elasticsearch</el-tag>
            </div>
          </div>
          <div class="tech-category">
            <h4>前端</h4>
            <div class="tech-tags">
              <el-tag type="success">Vue 3</el-tag>
              <el-tag type="success">Element Plus</el-tag>
              <el-tag type="success">Pinia</el-tag>
              <el-tag type="success">Axios</el-tag>
            </div>
          </div>
          <div class="tech-category">
            <h4>运维</h4>
            <div class="tech-tags">
              <el-tag type="warning">Docker</el-tag>
              <el-tag type="warning">Nginx</el-tag>
              <el-tag type="warning">MinIO</el-tag>
              <el-tag type="warning">RocketMQ</el-tag>
            </div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { User, Message, Link } from '@element-plus/icons-vue'
import { getStats } from '@/api/post/post'

const stats = ref({
  articles: 0,
  categories: 0,
  views: 0,
  days: 0
})

const fetchStats = async () => {
  try {
    const res = await getStats()
    if (res.data.code === 200) {
      stats.value.articles = res.data.data.articleCount || 0
      stats.value.categories = res.data.data.categoryCount || 0
      stats.value.views = res.data.data.totalReadCount || 0
    }
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

onMounted(() => {
  fetchStats()
})
</script>

<style lang="scss" scoped>
.about-page {
  max-width: 800px;
  margin: 0 auto;

  .about-header {
    text-align: center;
    padding: 40px 0;

    .avatar {
      background: var(--c-primary);
      margin-bottom: 20px;
    }

    h1 {
      font-family: var(--font-display);
      font-size: 32px;
      font-weight: 700;
      color: var(--c-text);
      margin-bottom: 8px;
    }

    .bio {
      font-size: 16px;
      color: var(--c-text-secondary);
    }
  }

  .about-content {
    .about-section {
      background: var(--c-bg-card);
      border-radius: var(--radius-md);
      padding: 24px;
      margin-bottom: 20px;
      border: 1px solid var(--c-border);

      h2 {
        font-family: var(--font-display);
        font-size: 20px;
        font-weight: 600;
        color: var(--c-text);
        margin-bottom: 16px;
        padding-bottom: 12px;
        border-bottom: 1px solid var(--c-border);
      }

      p {
        font-size: 15px;
        color: var(--c-text-secondary);
        line-height: 1.8;
        margin-bottom: 12px;
      }
    }

    .contact-list {
      .contact-item {
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 12px 0;
        font-size: 15px;
        color: var(--c-text-secondary);

        a {
          color: var(--c-primary);
          text-decoration: none;

          &:hover {
            text-decoration: underline;
          }
        }
      }
    }

    .stats-grid {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 16px;

      .stat-item {
        text-align: center;
        padding: 20px;
        background: var(--c-bg);
        border-radius: var(--radius-md);

        .stat-value {
          display: block;
          font-family: var(--font-display);
          font-size: 28px;
          font-weight: 700;
          color: var(--c-primary);
        }

        .stat-label {
          font-size: 14px;
          color: var(--c-text-muted);
        }
      }
    }

    .tech-stack {
      .tech-category {
        margin-bottom: 20px;

        &:last-child {
          margin-bottom: 0;
        }

        h4 {
          font-size: 14px;
          color: var(--c-text-muted);
          margin-bottom: 12px;
        }

        .tech-tags {
          display: flex;
          flex-wrap: wrap;
          gap: 8px;
        }
      }
    }
  }
}
</style>

<template>
  <div class="article-detail">
    <div class="main-content">
      <div v-if="loading" class="loading-container">
        <el-skeleton :rows="10" animated />
      </div>

      <div v-else-if="!articleExists" class="not-found">
        <el-empty description="文章不存在" />
      </div>

      <article v-else class="article-container">
        <header class="article-header">
          <h1 class="article-title">{{ article.title }}</h1>
          <div class="article-meta">
            <span class="author">
              <el-avatar :size="24" :src="getAvatarUrl(article.authAvatar)">
                <el-icon><User /></el-icon>
              </el-avatar>
              {{ article.authName || '匿名作者' }}
            </span>
            <span class="date">
              <el-icon><Calendar /></el-icon>
              {{ formatDate(article.createdAt) }}
            </span>
            <span class="views">
              <el-icon><View /></el-icon>
              {{ article.readCount || 0 }} 阅读
            </span>
            <span class="likes">
              <el-icon><Star /></el-icon>
              {{ article.likeCount || 0 }} 点赞
            </span>
          </div>
        </header>
        
        <div class="article-content" @click="handleContentClick">
          <MdPreview :editorId="articlePreviewId" :modelValue="article.content" />
        </div>
        
        <footer class="article-footer">
          <div class="action-buttons">
            <el-button
              :type="isLiked ? 'primary' : 'default'"
              :icon="isLiked ? StarFilled : Star"
              @click="handleLike"
            >
              {{ isLiked ? '已点赞' : '点赞' }} ({{ article.likeCount || 0 }})
            </el-button>
            <el-button :icon="Share" @click="handleShare">分享</el-button>
          </div>
          
          <div class="copyright-notice">
            <p>本文作者：{{ article.authName || 'TechBlog' }}</p>
            <p>版权声明：本博客所有文章除特别声明外，均采用 CC BY-NC-SA 4.0 许可协议。</p>
          </div>
        </footer>
        
        <section class="comments-section">
          <h3 class="section-title">
            <el-icon><ChatDotRound /></el-icon>
            评论 ({{ comments.length }})
          </h3>
          
          <div class="comment-form">
            <el-input
              v-model="newComment"
              type="textarea"
              :rows="3"
              placeholder="写下你的评论..."
            />
            <el-button type="primary" @click="submitComment" :loading="submitting">
              发表评论
            </el-button>
          </div>
          
          <div class="comment-list">
            <div v-for="comment in comments" :key="comment.id" class="comment-item">
              <el-avatar :size="40" class="comment-avatar" :src="getAvatarUrl(comment.avatarUrl)">
                <el-icon><User /></el-icon>
              </el-avatar>
              <div class="comment-content">
                <div class="comment-header">
                  <span class="comment-author">{{ comment.nickname || '匿名用户' }}</span>
                  <span class="comment-time">{{ formatDate(comment.createdAt) }}</span>
                </div>
                <p class="comment-text">{{ comment.content }}</p>
                <div class="comment-actions">
                  <el-button link size="small" @click="likeComment(comment.id)" :type="likedComments.has(comment.id) ? 'primary' : ''">
                    <el-icon><Star /></el-icon> {{ comment.likeCount || 0 }}
                  </el-button>
                  <el-button link size="small" @click="replyComment(comment)">
                    <el-icon><ChatDotRound /></el-icon> 回复
                  </el-button>
                </div>
                <div v-if="comment.children && comment.children.length > 0" class="comment-replies">
                  <div v-for="reply in comment.children" :key="reply.id" class="comment-item reply-item">
                    <el-avatar :size="32" class="comment-avatar" :src="getAvatarUrl(reply.avatarUrl)">
                      <el-icon><User /></el-icon>
                    </el-avatar>
                    <div class="comment-content">
                      <div class="comment-header">
                        <span class="comment-author">{{ reply.nickname || '匿名用户' }}</span>
                        <span class="comment-time">{{ formatDate(reply.createdAt) }}</span>
                      </div>
                      <p class="comment-text">{{ reply.content }}</p>
                      <div class="comment-actions">
                        <el-button link size="small" @click="likeComment(reply.id)" :type="likedComments.has(reply.id) ? 'primary' : ''">
                          <el-icon><Star /></el-icon> {{ reply.likeCount || 0 }}
                        </el-button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            
            <el-empty v-if="!comments.length" description="暂无评论，快来抢沙发吧！" />
          </div>
        </section>
      </article>
    </div>
    
    <aside class="sidebar">
      <div class="sidebar-card">
        <h4 class="card-title">目录</h4>
        <div class="catalog-container">
          <MdCatalog :editorId="articlePreviewId" :scrollElement="scrollElement" />
        </div>
      </div>
      
      <div class="sidebar-card">
        <h4 class="card-title">相关文章</h4>
        <div class="related-articles">
          <div
            v-for="item in relatedArticles"
            :key="item.id"
            class="related-item"
            @click="goToArticle(item.id)"
          >
            <span class="related-title">{{ item.title }}</span>
          </div>
        </div>
      </div>
    </aside>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getArticleById, getArticleComments, addComment, likeArticleOrComment, getLikeCount, hasLiked, getRelatedArticles } from '@/api/post/post'
import { User, Calendar, View, Star, StarFilled, Share, ChatDotRound } from '@element-plus/icons-vue'
import { MdPreview, MdCatalog } from 'md-editor-v3'
import 'md-editor-v3/lib/preview.css'
import { getAvatarUrl } from '@/utils/avatar.js'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const articleExists = ref(false)
const article = ref({})
const comments = ref([])
const likedComments = ref(new Set())
const newComment = ref('')
const submitting = ref(false)
const isLiked = ref(false)
const articlePreviewId = 'article-preview'
const scrollElement = document.documentElement

const relatedArticles = ref([])

const fetchArticle = async () => {
  loading.value = true
  try {
    const id = route.params.id
    if (!id || !/^\d+$/.test(id)) {
      loading.value = false
      articleExists.value = false
      return
    }
    const res = await getArticleById(id)
    if (res.data.code === 200 && res.data.data) {
      articleExists.value = true
      article.value = res.data.data
      fetchLikeStatus()
      fetchLikeCount()
      fetchRelatedArticles()
    } else {
      articleExists.value = false
      ElMessage.error(res.data.msg || '文章不存在或已删除')
    }
  } catch (error) {
    console.error('获取文章失败:', error)
    ElMessage.error('文章加载失败')
    articleExists.value = false
  } finally {
    loading.value = false
  }
}

const fetchRelatedArticles = async () => {
  if (!article.value.id) return
  try {
    const res = await getRelatedArticles(article.value.id, 5)
    if (res.data.code === 200) {
      relatedArticles.value = res.data.data || []
    }
  } catch (error) {
    console.error('获取相关文章失败:', error)
  }
}

const fetchLikeStatus = async () => {
  if (!localStorage.getItem('access_token')) {
    isLiked.value = false
    return
  }
  try {
    const res = await hasLiked(article.value.id, 1)
    console.log('点赞状态返回:', res.data)
    if (res.data.code === 200) {
      isLiked.value = res.data.data === true
    }
  } catch (error) {
    console.error('获取点赞状态失败:', error)
  }
}

const fetchLikeCount = async () => {
  try {
    const res = await getLikeCount(article.value.id, 1)
    if (res.data.code === 200) {
      article.value.likeCount = res.data.data
    }
  } catch (error) {
    console.error('获取点赞数失败:', error)
  }
}

const fetchComments = async () => {
  try {
    const id = route.params.id
    if (!id || !/^\d+$/.test(id)) return
    const res = await getArticleComments(id)
    if (res.data.code === 200) {
      comments.value = res.data.data || []
    }
  } catch (error) {
    console.error('获取评论失败:', error)
  }
}

const handleLike = async () => {
  if (!localStorage.getItem('access_token')) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  
  try {
    const res = await likeArticleOrComment(article.value.id, 1)
    if (res.data.code === 200) {
      const wasLiked = isLiked.value
      isLiked.value = !wasLiked
      await fetchLikeCount()
      ElMessage.success(isLiked.value ? '点赞成功' : '取消点赞')
    }
  } catch (error) {
    console.error('点赞失败:', error)
    ElMessage.error('操作失败')
  }
}

const handleShare = () => {
  if (navigator.share) {
    navigator.share({
      title: article.value.title,
      url: window.location.href
    })
  } else {
    navigator.clipboard.writeText(window.location.href)
    ElMessage.success('链接已复制到剪贴板')
  }
}

const submitComment = async () => {
  if (!newComment.value.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }
  
  if (!localStorage.getItem('access_token')) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  
  submitting.value = true
  try {
    await addComment({
      articleId: route.params.id,
      content: newComment.value
    })
    ElMessage.success('评论发表成功')
    newComment.value = ''
    fetchComments()
  } catch (error) {
    console.error('评论失败:', error)
    ElMessage.error('评论发表失败')
  } finally {
    submitting.value = false
  }
}

const updateCommentCount = (list, targetId, delta) => {
  for (const c of list) {
    if (c.id === targetId) {
      c.likeCount = (c.likeCount || 0) + delta
      return true
    }
    if (c.children && updateCommentCount(c.children, targetId, delta)) return true
  }
  return false
}

const likeComment = async (commentId) => {
  if (!localStorage.getItem('access_token')) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  try {
    const res = await likeArticleOrComment(commentId, 2)
    if (res.data.code === 200) {
      if (res.data.data) {
        likedComments.value.add(commentId)
        updateCommentCount(comments.value, commentId, 1)
        ElMessage.success('点赞成功')
      } else {
        likedComments.value.delete(commentId)
        updateCommentCount(comments.value, commentId, -1)
        ElMessage.success('取消点赞')
      }
      likedComments.value = new Set(likedComments.value)
    } else {
      ElMessage.error(res.data.msg || '操作失败')
    }
  } catch (error) {
    console.error('点赞失败:', error)
    ElMessage.error('点赞失败')
  }
}

const replyComment = (comment) => {
  newComment.value = `@${comment.nickname} `
}

const goToArticle = (id) => {
  router.push(`/blog/article/${id}`)
}

const handleContentClick = (e) => {
  const link = e.target.closest('a')
  if (!link) return
  const href = link.getAttribute('href')
  if (href && href.endsWith('.md')) {
    e.preventDefault()
  }
}

const formatDate = (date) => {
  if (!date) return ''
  return new Date(date).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}

onMounted(() => {
  fetchArticle()
  fetchComments()
})
</script>

<style lang="scss" scoped>
.article-detail {
  display: flex;
  gap: 24px;
  
  .main-content {
    flex: 1;
    min-width: 0;
  }
  
  .sidebar {
    width: 280px;
    flex-shrink: 0;
    position: sticky;
    top: 24px;
    align-self: flex-start;
    max-height: calc(100vh - 48px);
    overflow-y: auto;

    &::-webkit-scrollbar {
      width: 4px;
    }

    &::-webkit-scrollbar-thumb {
      background: var(--c-border);
      border-radius: 2px;
    }
  }
}

.article-container {
  background: var(--c-bg-card);
  border-radius: var(--radius-md);
  padding: 32px;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--c-border);
}

.article-header {
  margin-bottom: 32px;
  padding-bottom: 24px;
  border-bottom: 1px solid var(--c-border);

  .article-title {
    font-family: var(--font-display);
    font-size: 28px;
    font-weight: 700;
    color: var(--c-text);
    line-height: 1.4;
    margin-bottom: 16px;
  }

  .article-meta {
    display: flex;
    flex-wrap: wrap;
    gap: 20px;
    font-size: 14px;
    color: var(--c-text-secondary);
    margin-bottom: 16px;

    span {
      display: flex;
      align-items: center;
      gap: 6px;
    }

    .author {
      display: flex;
      align-items: center;
      gap: 8px;
    }
  }
}

.article-content {
  :deep(.md-editor-preview-wrapper) {
    padding: 0;
  }

  :deep(.md-editor-preview) {
    font-size: 16px;
    line-height: 1.8;
    color: var(--c-text);
  }

  :deep(.md-editor-preview) {
    h1, h2, h3, h4 {
      margin: 24px 0 16px;
      font-weight: 600;
      color: var(--c-text);
    }

    blockquote {
      border-left: 4px solid var(--c-primary);
      padding-left: 16px;
      margin: 16px 0;
      color: var(--c-text-secondary);
      font-style: italic;
    }

    img {
      max-width: 100%;
      border-radius: var(--radius-md);
      margin: 16px 0;
    }
  }
}

.article-footer {
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid var(--c-border);

  .action-buttons {
    display: flex;
    gap: 12px;
    margin-bottom: 24px;

    .el-button {
      &.el-button--primary {
        background: var(--c-primary);
        border-color: transparent;

        &:hover {
          background: var(--c-primary-hover);
        }
      }
    }
  }
  
  .copyright-notice {
    background: var(--c-bg);
    padding: 16px;
    border-radius: var(--radius-md);
    font-size: 13px;
    color: var(--c-text-muted);

    p {
      margin: 4px 0;
    }
  }
}

.comments-section {
  margin-top: 48px;

  .section-title {
    display: flex;
    align-items: center;
    gap: 8px;
    font-family: var(--font-display);
    font-size: 20px;
    font-weight: 600;
    color: var(--c-text);
    margin-bottom: 24px;
  }

  .comment-form {
    margin-bottom: 32px;

    .el-textarea {
      margin-bottom: 12px;
    }
  }

  .comment-list {
    .comment-item {
      display: flex;
      gap: 16px;
      padding: 20px 0;
      border-bottom: 1px solid var(--c-border);

      &:last-child {
        border-bottom: none;
      }

      .comment-avatar {
        flex-shrink: 0;
        background: var(--c-primary);
      }

      .comment-content {
        flex: 1;

        .comment-header {
          display: flex;
          align-items: center;
          gap: 12px;
          margin-bottom: 8px;

          .comment-author {
            font-weight: 600;
            color: var(--c-text);
          }

          .comment-time {
            font-size: 12px;
            color: var(--c-text-muted);
          }
        }

        .comment-text {
          font-size: 14px;
          color: var(--c-text-secondary);
          line-height: 1.6;
          margin-bottom: 8px;
        }

        .comment-actions {
          display: flex;
          gap: 16px;
        }

        .comment-replies {
          margin-top: 16px;
          padding-left: 16px;
          border-left: 2px solid var(--c-border);

          .reply-item {
            padding: 12px 0;

            .comment-avatar {
              background: var(--c-primary-dark);
            }
          }
        }
      }
    }
  }
}

.sidebar-card {
  background: var(--c-bg-card);
  border-radius: var(--radius-md);
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--c-border);

  .card-title {
    font-family: var(--font-display);
    font-size: 16px;
    font-weight: 600;
    color: var(--c-text);
    margin-bottom: 16px;
    padding-bottom: 12px;
    border-bottom: 1px solid var(--c-border);
  }
}

.catalog-container {
  :deep(.md-editor-catalog) {
    font-size: 14px;
  }

  :deep(.md-editor-catalog-link) {
    color: var(--c-text-secondary);
    text-decoration: none;
    padding: 6px 0;
    display: block;
    transition: color 0.3s;

    &:hover {
      color: var(--c-primary);
    }
  }

  :deep(.md-editor-catalog-active > .md-editor-catalog-link) {
    color: var(--c-primary);
    font-weight: 600;
  }

  :deep(.md-editor-catalog-level2) {
    padding-left: 16px;
  }

  :deep(.md-editor-catalog-level3) {
    padding-left: 32px;
  }
}

.related-articles {
  .related-item {
    padding: 12px 0;
    border-bottom: 1px dashed var(--c-border);
    cursor: pointer;

    &:last-child {
      border-bottom: none;
    }

    &:hover .related-title {
      color: var(--c-primary);
    }

    .related-title {
      font-size: 14px;
      color: var(--c-text-secondary);
      transition: color 0.3s;
    }
  }
}

.loading-container {
  background: var(--c-bg-card);
  border-radius: var(--radius-md);
  padding: 32px;
}

.not-found {
  background: var(--c-bg-card);
  border-radius: var(--radius-md);
  padding: 80px 32px;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--c-border);
}
</style>

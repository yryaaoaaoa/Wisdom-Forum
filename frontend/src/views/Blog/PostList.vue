<template>
  <div class="post-list">
    <div class="header">
      <h2>文章管理</h2>
      <div class="header-actions">
        <el-button @click="handleReindex" :loading="reindexing">
          <el-icon><Search /></el-icon>{{ reindexing ? '重建中...' : '重建ES索引' }}
        </el-button>
        <el-button type="primary" @click="handleCreate">
          <el-icon><Plus /></el-icon>写文章
        </el-button>
      </div>
    </div>

    <div class="filter-bar">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索文章标题..."
        style="width: 300px"
        @keyup.enter="handleSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-button type="primary" @click="handleSearch">
        <el-icon><Search /></el-icon>搜索
      </el-button>
      <el-select v-model="filterStatus" placeholder="状态筛选" clearable @change="fetchPosts">
        <el-option label="全部" value="" />
        <el-option label="已发布" :value="1" />
        <el-option label="草稿" :value="0" />
      </el-select>
    </div>

    <el-table :data="posts" border style="width: 100%" v-loading="loading" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" />
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="title" label="文章标题" min-width="200">
        <template #default="scope">
          <div class="title-cell">
            <img v-if="scope.row.coverUrl" :src="scope.row.coverUrl" class="cover-thumb" />
            <span class="title-text">{{ scope.row.title || '(无标题)' }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="summary" label="摘要" min-width="200">
        <template #default="scope">
          <el-tooltip v-if="scope.row.summary" :content="scope.row.summary" placement="top">
            <div class="summary-content">{{ scope.row.summary }}</div>
          </el-tooltip>
          <span v-else class="empty-text">(无摘要)</span>
        </template>
      </el-table-column>
      <el-table-column prop="readCount" label="阅读" width="80" align="center" />
      <el-table-column prop="likeCount" label="点赞" width="80" align="center" />
      <el-table-column prop="status" label="状态" width="100" align="center">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'warning'">
            {{ scope.row.status === 1 ? '已发布' : '草稿' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="160">
        <template #default="scope">
          {{ formatDate(scope.row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280" align="center" fixed="right">
        <template #default="scope">
          <el-button-group>
            <el-button size="small" type="primary" @click="handleEdit(scope.row)">
              编辑
            </el-button>
            <el-button size="small" @click="handlePreview(scope.row)" :disabled="scope.row.status !== 1">
              预览
            </el-button>
            <el-button
              size="small"
              :type="scope.row.status === 1 ? 'warning' : 'success'"
              @click="handleToggleStatus(scope.row)"
            >
              {{ scope.row.status === 1 ? '下架' : '发布' }}
            </el-button>
            <el-button size="small" type="danger" @click="handleDelete(scope.row)">
              删除
            </el-button>
          </el-button-group>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-button
        type="danger"
        @click="handleBatchDelete"
        :disabled="selectedPosts.length === 0"
      >
        批量删除
      </el-button>
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <el-dialog
      v-model="editorVisible"
      :title="isEdit ? '编辑文章' : '写文章'"
      width="95%"
      top="2vh"
      :close-on-click-modal="false"
      @opened="handleEditorOpened"
      @closed="handleDialogClosed"
      class="article-dialog"
    >
      <div class="article-editor">
        <div class="editor-main">
          <div class="editor-header">
            <el-input
              v-model="articleForm.title"
              placeholder="请输入文章标题..."
              class="title-input"
              size="large"
            />
            <el-select v-model="articleForm.categoryId" placeholder="选择分类" clearable class="category-select">
              <el-option
                v-for="cat in categories"
                :key="cat.id"
                :label="cat.name"
                :value="cat.id"
              />
            </el-select>
            <el-button type="success" @click="triggerImportMd" :loading="importing">
              <el-icon><Document /></el-icon> 导入MD
            </el-button>
            <input
              ref="mdFileInput"
              type="file"
              accept=".md,.markdown"
              style="display: none"
              @change="handleImportMd"
            />
          </div>

          <div class="editor-meta">
            <div class="meta-item">
              <label>摘要</label>
              <el-input
                v-model="articleForm.summary"
                type="textarea"
                :rows="2"
                placeholder="请输入文章摘要（可选，不填将自动提取）"
              />
            </div>
            <div class="meta-item cover-item">
              <label>封面</label>
              <div class="cover-upload-area">
                <el-upload
                  class="cover-uploader"
                  action="#"
                  :show-file-list="false"
                  :before-upload="beforeCoverUpload"
                >
                  <template v-if="articleForm.coverUrl">
                    <img :src="articleForm.coverUrl" class="cover-preview" />
                    <div class="cover-overlay">
                      <el-icon><Edit /></el-icon>
                      <span>更换</span>
                    </div>
                  </template>
                  <div v-else class="cover-placeholder">
                    <el-icon><Picture /></el-icon>
                    <span>上传封面</span>
                  </div>
                </el-upload>
                <el-button v-if="articleForm.coverUrl" text type="danger" @click="articleForm.coverUrl = ''">
                  移除封面
                </el-button>
              </div>
            </div>
          </div>

          <div class="editor-content">
            <MdEditor
              v-if="editorReady"
              :editorId="editorId"
              v-model="articleForm.content"
              :theme="editorTheme"
              :previewTheme="previewTheme"
              :codeTheme="codeTheme"
              :toolbars="editorToolbars"
              :preview="true"
              :previewOnly="false"
              :htmlPreview="false"
              :language="language"
              placeholder="开始写作..."
              class="md-editor"
              @onUploadImg="handleUploadImg"
            />
          </div>

          <div class="editor-footer">
            <div class="theme-switch">
              <el-tooltip content="切换编辑器主题">
                <el-button circle @click="toggleEditorTheme">
                  <el-icon><Sunny v-if="editorTheme === 'dark'" /><Moon v-else /></el-icon>
                </el-button>
              </el-tooltip>
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="editorVisible = false">取消</el-button>
          <el-button @click="saveDraft" :loading="saving">
            <el-icon><Document /></el-icon>保存草稿
          </el-button>
          <el-button type="primary" @click="handlePublish" :loading="saving">
            <el-icon><Promotion /></el-icon>发布文章
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick, defineAsyncComponent } from 'vue'
import { Plus, Search, Edit, Picture, Document, Promotion, Sunny, Moon } from '@element-plus/icons-vue'
import { pageArticles, deleteArticle, updateArticle, publishArticle as publishArticleApi, getDraft, getArticleById, uploadArticleCover, reindexArticles } from '@/api/post/post'
import { getAllCategories } from '@/api/category/category'

const MdEditor = defineAsyncComponent(async () => {
  const [mod] = await Promise.all([
    import('md-editor-v3'),
    import('md-editor-v3/lib/style.css')
  ])
  return mod.MdEditor
})

const posts = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const searchKeyword = ref('')
const filterStatus = ref(null)
const selectedPosts = ref([])

const editorVisible = ref(false)
const editorReady = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const importing = ref(false)
const reindexing = ref(false)
const mdFileInput = ref(null)

const editorTheme = ref('light')
const previewTheme = ref('default')
const codeTheme = ref('atom')
const language = ref('zh-CN')
const editorId = ref('article-md-editor-' + Date.now())

const editorToolbars = [
  'bold',
  'underline',
  'italic',
  '-',
  'strikeThrough',
  'title',
  'sub',
  'sup',
  'quote',
  'unorderedList',
  'orderedList',
  'task',
  '-',
  'codeRow',
  'code',
  'link',
  'image',
  'table',
  'mermaid',
  '-',
  'revoke',
  'next',
  '=',
  'pageFullscreen',
  'fullscreen',
  'preview',
  'catalog'
]

const categories = ref([])

const initialForm = {
  id: null,
  title: '',
  summary: '',
  content: '',
  coverUrl: '',
  categoryId: null,
  status: 0
}

const articleForm = reactive({ ...initialForm })

const compressImage = (file, options = {}) => {
  const { maxWidth = 1920, maxHeight = 1080, quality = 0.8, maxSizeMB = 2 } = options
  return new Promise((resolve) => {
    if (!file.type.startsWith('image/')) {
      resolve(file)
      return
    }

    const isSmallFile = file.size / 1024 / 1024 <= maxSizeMB
    if (isSmallFile && !options.force) {
      resolve(file)
      return
    }

    const reader = new FileReader()
    reader.onload = (e) => {
      const img = new Image()
      img.onload = () => {
        let { width, height } = img

        if (width > maxWidth || height > maxHeight) {
          const ratio = Math.min(maxWidth / width, maxHeight / height)
          width = Math.round(width * ratio)
          height = Math.round(height * ratio)
        }

        const canvas = document.createElement('canvas')
        canvas.width = width
        canvas.height = height
        const ctx = canvas.getContext('2d')
        ctx.drawImage(img, 0, 0, width, height)

        canvas.toBlob(
          (blob) => {
            if (blob && blob.size < file.size) {
              const compressedFile = new File([blob], file.name, {
                type: file.type,
                lastModified: Date.now()
              })
              resolve(compressedFile)
            } else {
              resolve(file)
            }
          },
          file.type,
          quality
        )
      }
      img.onerror = () => resolve(file)
      img.src = e.target.result
    }
    reader.onerror = () => resolve(file)
    reader.readAsDataURL(file)
  })
}

const toggleEditorTheme = () => {
  editorTheme.value = editorTheme.value === 'light' ? 'dark' : 'light'
}

const handleUploadImg = async (files, callback) => {
  const urls = []
  const token = localStorage.getItem('access_token')
  for (const file of files) {
    const compressedFile = await compressImage(file)
    const formData = new FormData()
    formData.append('file', compressedFile)
    if (articleForm.id) {
      formData.append('articleId', articleForm.id)
    }
    try {
      const res = await fetch('/api/articles/minio/img', {
        method: 'POST',
        headers: {
          Authorization: token ? `Bearer ${token}` : ''
        },
        body: formData
      })
      const text = await res.text()
      let url
      try {
        const data = JSON.parse(text)
        url = typeof data.data === 'string' ? data.data : data.data?.url
      } catch {
        url = text
      }
      if (url) {
        urls.push(url)
      } else {
        ElMessage.error('图片上传失败：未获取到URL')
      }
    } catch (e) {
      ElMessage.error('图片上传失败')
    }
  }
  callback(urls)
}

const triggerImportMd = () => {
  mdFileInput.value?.click()
}

const handleImportMd = async (event) => {
  const file = event.target.files?.[0]
  if (!file) return

  if (!articleForm.id) {
    ElMessage.warning('请先创建草稿再导入')
    event.target.value = ''
    return
  }

  importing.value = true
  try {
    const mdContent = await file.text()
    const mdDir = file.name.substring(0, file.name.lastIndexOf('.'))

    const imageRegex = /!\[([^\]]*)\]\(([^)]+)\)/g
    const localImages = []
    let match
    while ((match = imageRegex.exec(mdContent)) !== null) {
      const src = match[2].trim()
      if (!src.startsWith('http://') && !src.startsWith('https://') && !src.startsWith('data:')) {
        localImages.push({
          fullMatch: match[0],
          alt: match[1],
          src: src
        })
      }
    }

    if (localImages.length === 0) {
      articleForm.content = mdContent
      if (!articleForm.title) {
        articleForm.title = mdDir
      }
      ElMessage.success('MD文件导入成功（无本地图片需要上传）')
      event.target.value = ''
      return
    }

    ElMessage.info(`检测到 ${localImages.length} 张本地图片，正在尝试上传...`)

    let processedContent = mdContent
    let uploadedCount = 0

    const dirHandle = await window.showDirectoryPicker?.().catch(() => null)

    for (const img of localImages) {
      try {
        let imageFile = null

        if (dirHandle) {
          imageFile = await findFileInDirectory(dirHandle, img.src)
        }

        if (!imageFile) {
          const fileHandles = await window.showOpenFilePicker?.({
            multiple: false,
            types: [{
              description: '图片文件',
              accept: { 'image/*': ['.png', '.jpg', '.jpeg', '.gif', '.webp', '.svg', '.bmp'] }
            }]
          }).catch(() => null)

          if (fileHandles && fileHandles.length > 0) {
            imageFile = await fileHandles[0].getFile()
          }
        }

        if (imageFile) {
          const url = await uploadImageToMinio(imageFile)
          if (url) {
            processedContent = processedContent.replace(img.fullMatch, `![${img.alt}](${url})`)
            uploadedCount++
            continue
          }
        }

        ElMessage.warning(`图片 "${img.src}" 未找到，请手动上传替换`)
      } catch (e) {
        console.warn(`处理图片 ${img.src} 失败:`, e)
      }
    }

    articleForm.content = processedContent
    if (!articleForm.title) {
      articleForm.title = mdDir
    }
    ElMessage.success(`导入完成！成功上传 ${uploadedCount}/${localImages.length} 张图片`)
  } catch (e) {
    console.error('导入MD文件失败:', e)
    ElMessage.error('导入MD文件失败')
  } finally {
    importing.value = false
    event.target.value = ''
  }
}

const findFileInDirectory = async (dirHandle, imagePath) => {
  try {
    const parts = imagePath.replace(/\\/g, '/').split('/')
    let currentHandle = dirHandle

    for (let i = 0; i < parts.length - 1; i++) {
      currentHandle = await currentHandle.getDirectoryHandle(parts[i])
    }

    const fileName = parts[parts.length - 1]
    const fileHandle = await currentHandle.getFileHandle(fileName)
    return await fileHandle.getFile()
  } catch {
    return null
  }
}

const uploadImageToMinio = async (file) => {
  const compressed = await compressImage(file)
  const token = localStorage.getItem('access_token')
  const formData = new FormData()
  formData.append('file', compressed)
  if (articleForm.id) {
    formData.append('articleId', articleForm.id)
  }
  try {
    const res = await fetch('/api/articles/minio/img', {
      method: 'POST',
      headers: {
        Authorization: token ? `Bearer ${token}` : ''
      },
      body: formData
    })
    const text = await res.text()
    try {
      const data = JSON.parse(text)
      return typeof data.data === 'string' ? data.data : data.data?.url
    } catch {
      return text
    }
  } catch {
    return null
  }
}

const fetchPosts = async () => {
  loading.value = true
  try {
    const params = {
      current: currentPage.value,
      size: pageSize.value
    }
    if (searchKeyword.value) {
      params.keyword = searchKeyword.value
    }
    if (filterStatus.value !== null && filterStatus.value !== '') {
      params.status = parseInt(filterStatus.value)
    }

    const res = await pageArticles(params)
    if (res.data.code === 200) {
      posts.value = res.data.data.records || []
      total.value = res.data.data.total || 0
    }
  } catch {
    ElMessage.error('获取文章列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  fetchPosts()
}

const handleSelectionChange = (val) => {
  selectedPosts.value = val
}

const resetForm = () => {
  Object.assign(articleForm, initialForm)
  isEdit.value = false
}

const handleEditorOpened = async () => {
  await nextTick()
  editorReady.value = true
}

const handleDialogClosed = () => {
  editorReady.value = false
  editorId.value = 'article-md-editor-' + Date.now()
  resetForm()
}

const handleReindex = async () => {
  reindexing.value = true
  try {
    const res = await reindexArticles()
    if (res.data.code === 200) {
      ElMessage.success(res.data.data || 'ES索引重建完成')
    } else {
      ElMessage.error(res.data.msg || '重建失败')
    }
  } catch (error) {
    ElMessage.error('ES索引重建失败: ' + (error.message || '未知错误'))
  } finally {
    reindexing.value = false
  }
}

const handleCreate = async () => {
  try {
    const res = await getDraft()
    if (res.data.code === 200 && res.data.data) {
      const draft = res.data.data
      isEdit.value = false
      Object.assign(articleForm, {
        id: draft.id,
        title: draft.title || '',
        summary: draft.summary || '',
        content: draft.content || '',
        coverUrl: draft.coverUrl || '',
        categoryId: null,
        status: 0
      })
      editorVisible.value = true
    } else {
      ElMessage.error('创建草稿失败')
    }
  } catch {
    ElMessage.error('创建草稿失败')
  }
}

const handleEdit = async (row) => {
  try {
    const res = await getArticleById(row.id)
    if (res.data.code === 200 && res.data.data) {
      const article = res.data.data
      isEdit.value = true
      Object.assign(articleForm, {
        id: article.id,
        title: article.title || '',
        summary: article.summary || '',
        content: article.content || '',
        coverUrl: article.coverUrl || '',
        categoryId: article.categoryId || null,
        status: article.status
      })
      editorVisible.value = true
    } else {
      ElMessage.error('获取文章详情失败')
    }
  } catch {
    ElMessage.error('获取文章详情失败')
  }
}

const handlePreview = (row) => {
  if (row.status === 1) {
    window.open(`/blog/article/${row.id}`, '_blank')
  }
}

const handleToggleStatus = async (row) => {
  const action = row.status === 1 ? '下架' : '发布'
  try {
    await ElMessageBox.confirm(`确认${action}文章「${row.title || '(无标题)'}」吗？`, '提示')

    if (row.status === 0) {
      if (!row.title || !row.content) {
        ElMessage.warning('请先编辑文章，填写标题和内容后再发布')
        return
      }
      await publishArticleApi(row.id)
    } else {
      await updateArticle({
        id: row.id,
        articleDTO: { status: 0 }
      })
    }
    ElMessage.success(`${action}成功`)
    fetchPosts()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(`${action}失败`)
    }
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确认删除文章「${row.title || '(无标题)'}」吗？`, '警告', { type: 'warning' })
    await deleteArticle(row.id)
    ElMessage.success('删除成功')
    fetchPosts()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const handleBatchDelete = async () => {
  if (selectedPosts.value.length === 0) {
    ElMessage.warning('请选择要删除的文章')
    return
  }

  try {
    await ElMessageBox.confirm(`确认删除选中的 ${selectedPosts.value.length} 篇文章吗？`, '警告', { type: 'warning' })
    await Promise.all(selectedPosts.value.map(post => deleteArticle(post.id)))
    ElMessage.success('批量删除成功')
    fetchPosts()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('批量删除失败')
    }
  }
}

const saveDraft = async () => {
  await saveArticle(0)
}

const handlePublish = async () => {
  if (!articleForm.title || !articleForm.title.trim()) {
    ElMessage.warning('请输入文章标题')
    return
  }
  if (!articleForm.content || !articleForm.content.trim()) {
    ElMessage.warning('请输入文章内容')
    return
  }
  await saveArticle(1)
}

const saveArticle = async (status) => {
  if (!articleForm.id) {
    ElMessage.error('文章ID不存在，请重新打开编辑器')
    return
  }

  saving.value = true
  try {
    const data = {
      title: articleForm.title,
      summary: articleForm.summary,
      content: articleForm.content,
      coverUrl: articleForm.coverUrl,
      categoryId: articleForm.categoryId
    }

    if (status === 1) {
      data.status = 1
    }

    const updateRes = await updateArticle({ id: articleForm.id, articleDTO: data })

    if (updateRes.data.code !== 200) {
      ElMessage.error(updateRes.data.msg || '保存失败')
      return
    }

    if (status === 1) {
      const publishRes = await publishArticleApi(articleForm.id)
      if (publishRes.data.code !== 200) {
        ElMessage.error(publishRes.data.msg || '发布失败')
        return
      }
    }

    ElMessage.success(status === 1 ? '发布成功' : '保存成功')
    editorVisible.value = false
    fetchPosts()
  } catch {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const beforeCoverUpload = async (file) => {
  if (!file.type.startsWith('image/')) {
    ElMessage.error('只能上传图片文件')
    return false
  }

  if (!articleForm.id) {
    ElMessage.warning('请先创建草稿再上传封面')
    return false
  }

  try {
    const compressed = await compressImage(file, { maxWidth: 1200, maxHeight: 675, quality: 0.85, maxSizeMB: 5 })
    const res = await uploadArticleCover(compressed, articleForm.id)
    
    if (res.data) {
      const url = typeof res.data === 'string' ? res.data : res.data.data
      if (url) {
        articleForm.coverUrl = url
        ElMessage.success('封面上传成功')
      } else {
        ElMessage.error('封面上传失败：未获取到URL')
      }
    }
  } catch (error) {
    console.error('封面上传失败:', error)
    ElMessage.error('封面上传失败')
  }
  return false
}

const handleSizeChange = () => {
  currentPage.value = 1
  fetchPosts()
}

const handleCurrentChange = () => {
  fetchPosts()
}

const formatDate = (date) => {
  if (!date) return ''
  return new Date(date).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const fetchCategories = async () => {
  try {
    const res = await getAllCategories()
    if (res.data.code === 200) {
      categories.value = res.data.data || []
    }
  } catch (e) {
    console.error('获取分类失败', e)
  }
}

onMounted(() => {
  fetchPosts()
  fetchCategories()
})
</script>

<style lang="scss" scoped>
.post-list {
  background-color: #fff;
  padding: 24px;
  border-radius: 8px;

  .header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    h2 {
      margin: 0;
      font-size: 20px;
      color: #303133;
    }
  }

  .filter-bar {
    display: flex;
    gap: 16px;
    margin-bottom: 20px;
  }

  .title-cell {
    display: flex;
    align-items: center;
    gap: 12px;

    .cover-thumb {
      width: 60px;
      height: 40px;
      object-fit: cover;
      border-radius: 4px;
    }

    .title-text {
      flex: 1;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  .summary-content {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .empty-text {
    color: #909399;
    font-style: italic;
  }

  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}

.article-dialog {
  :deep(.el-dialog__body) {
    padding: 0;
    max-height: 80vh;
    overflow-y: auto;
  }

  :deep(.el-dialog__header) {
    border-bottom: 1px solid #ebeef5;
    padding: 16px 20px;
    margin: 0;
  }

  :deep(.el-dialog__footer) {
    border-top: 1px solid #ebeef5;
    padding: 16px 20px;
  }
}

.article-editor {
  .editor-main {
    padding: 20px;

    .editor-header {
      display: flex;
      gap: 16px;
      margin-bottom: 20px;

      .title-input {
        flex: 1;

        :deep(.el-input__wrapper) {
          padding: 8px 15px;
          font-size: 18px;
          font-weight: 500;
          box-shadow: none;
          border: 1px solid #dcdfe6;
          border-radius: 8px;

          &:hover, &:focus-within {
            border-color: #409eff;
          }
        }
      }

      .category-select {
        width: 200px;
      }
    }

    .editor-meta {
      display: flex;
      gap: 20px;
      margin-bottom: 20px;

      .meta-item {
        flex: 1;

        > label {
          display: block;
          font-size: 14px;
          font-weight: 500;
          color: #606266;
          margin-bottom: 8px;
        }
      }

      .cover-item {
        flex: 0 0 280px;
      }
    }

    .cover-upload-area {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .cover-uploader {
      :deep(.el-upload) {
        border: 2px dashed #dcdfe6;
        border-radius: 8px;
        cursor: pointer;
        position: relative;
        overflow: hidden;
        transition: all 0.3s;
        width: 260px;
        height: 140px;
        display: flex;
        align-items: center;
        justify-content: center;

        &:hover {
          border-color: #409eff;

          .cover-overlay {
            opacity: 1;
          }
        }
      }

      .cover-preview {
        width: 100%;
        height: 100%;
        object-fit: cover;
      }

      .cover-overlay {
        position: absolute;
        inset: 0;
        background: rgba(0, 0, 0, 0.5);
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        gap: 4px;
        color: white;
        opacity: 0;
        transition: opacity 0.3s;

        .el-icon {
          font-size: 24px;
        }
      }

      .cover-placeholder {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 8px;
        color: #909399;

        .el-icon {
          font-size: 32px;
        }
      }
    }

    .editor-content {
      margin-bottom: 20px;
      border-radius: 8px;
      overflow: hidden;
      border: 1px solid #e4e7ed;

      .md-editor {
        height: 500px;
      }
    }

    .editor-footer {
      display: flex;
      justify-content: flex-end;
      align-items: flex-end;

      .theme-switch {
        margin-left: 16px;
      }
    }
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>

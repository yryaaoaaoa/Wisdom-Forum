<template>
  <div class="settings-page">
    <div class="settings-card">
      <div class="card-header">
        <h2 class="title">系统设置</h2>
      </div>

      <div class="setting-section">
        <div class="section-title">登录页背景设置</div>
        
        <div class="setting-item">
          <div class="setting-label">上传背景图</div>
          <el-upload
            class="upload-area"
            :action="uploadUrl"
            :headers="uploadHeaders"
            :show-file-list="false"
            :on-success="handleUploadSuccess"
            :on-error="handleUploadError"
            :before-upload="beforeUpload"
            accept="image/*"
          >
            <div class="upload-content">
              <el-icon class="upload-icon"><Upload /></el-icon>
              <div class="upload-text">
                <span>点击或拖拽上传</span>
                <span class="upload-hint">支持 jpg/png/webp，建议 1920x1080 以上</span>
              </div>
            </div>
          </el-upload>
        </div>

        <div class="setting-item">
          <div class="setting-label">背景预览</div>
          <div 
            class="preview-box"
            :style="previewStyle"
          >
            <div class="preview-overlay" :style="previewOverlayStyle">
              <span v-if="!backgroundUrl">默认渐变背景</span>
              <span v-else>自定义背景</span>
            </div>
          </div>
        </div>

        <div class="setting-item">
          <div class="setting-label">
            遮罩深度
            <span class="label-hint">控制背景图上方黑色遮罩的透明度，数值越大背景越暗</span>
          </div>
          <div class="opacity-control">
            <el-radio-group v-model="overlayMode" @change="onOverlayModeChange" class="mode-switch">
              <el-radio-button value="auto">自动适配</el-radio-button>
              <el-radio-button value="manual">手动调节</el-radio-button>
            </el-radio-group>
            <div v-if="overlayMode === 'manual'" class="slider-row">
              <el-slider
                v-model="overlayValue"
                :min="0"
                :max="60"
                :step="1"
                :format-tooltip="(val) => (val / 100).toFixed(2)"
                class="opacity-slider"
              />
              <span class="slider-value">{{ (overlayValue / 100).toFixed(2) }}</span>
            </div>
            <div v-else class="auto-hint">
              根据背景图亮度自动计算最佳遮罩深度
            </div>
          </div>
        </div>

        <div class="setting-actions">
          <el-button type="primary" @click="saveBackground" :loading="saving">
            保存设置
          </el-button>
          <el-button @click="resetBackground">
            恢复默认
          </el-button>
        </div>
      </div>

      <div class="setting-section">
        <div class="section-title">推荐背景图（点击自动下载并存储到MinIO）</div>
        <div class="image-gallery">
          <div 
            v-for="(img, index) in recommendedImages" 
            :key="index"
            class="gallery-item"
            :class="{ active: backgroundUrl === img.url, loading: fetchingIndex === index }"
            @click="selectImage(index)"
          >
            <img :src="img.thumbnail" :alt="img.name" />
            <div v-if="fetchingIndex === index" class="loading-overlay">
              <el-icon class="is-loading"><Loading /></el-icon>
            </div>
            <div class="img-name">{{ img.name }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Upload, Loading } from '@element-plus/icons-vue'
import request from '@/utils/requests'

const backgroundUrl = ref('')
const overlayOpacity = ref('auto')
const overlayMode = ref('auto')
const overlayValue = ref(30)
const saving = ref(false)
const fetchingIndex = ref(-1)

const uploadUrl = '/api/config/login-background/upload'
const uploadHeaders = computed(() => {
  const token = localStorage.getItem('access_token')
  return token ? { Authorization: `Bearer ${token}` } : {}
})

const previewStyle = computed(() => {
  if (backgroundUrl.value) {
    return {
      backgroundImage: `url(${backgroundUrl.value})`,
      backgroundSize: 'cover',
      backgroundPosition: 'center'
    }
  }
  return {
    background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)'
  }
})

const previewOverlayStyle = computed(() => {
  if (!backgroundUrl.value) {
    return { background: 'transparent' }
  }
  if (overlayMode.value === 'manual') {
    return { background: `rgba(0, 0, 0, ${(overlayValue.value / 100).toFixed(2)})` }
  }
  return { background: 'rgba(0, 0, 0, 0.25)' }
})

const onOverlayModeChange = (val) => {
  if (val === 'auto') {
    overlayOpacity.value = 'auto'
  } else {
    overlayOpacity.value = (overlayValue.value / 100).toFixed(2)
  }
}

const recommendedImages = ref([
  { name: '山脉日出', thumbnail: 'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=400', fullUrl: 'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=1920' },
  { name: '海洋日落', thumbnail: 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=400', fullUrl: 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1920' },
  { name: '森林小径', thumbnail: 'https://images.unsplash.com/photo-1441974231531-c6227db76b6e?w=400', fullUrl: 'https://images.unsplash.com/photo-1441974231531-c6227db76b6e?w=1920' },
  { name: '城市夜景', thumbnail: 'https://images.unsplash.com/photo-1514565131-fce0801e5785?w=400', fullUrl: 'https://images.unsplash.com/photo-1514565131-fce0801e5785?w=1920' },
  { name: '星空银河', thumbnail: 'https://images.unsplash.com/photo-1419242902214-272b3f66ee7a?w=400', fullUrl: 'https://images.unsplash.com/photo-1419242902214-272b3f66ee7a?w=1920' },
  { name: '抽象渐变', thumbnail: 'https://images.unsplash.com/photo-1557682250-33bd709cbe85?w=400', fullUrl: 'https://images.unsplash.com/photo-1557682250-33bd709cbe85?w=1920' }
])

const fetchConfig = async () => {
  try {
    const response = await request({ url: '/api/config/login-background', method: 'get' })
    if (response.data.code === 200) {
      backgroundUrl.value = response.data.data?.background || ''
      const savedOpacity = response.data.data?.overlay_opacity || 'auto'
      overlayOpacity.value = savedOpacity
      if (savedOpacity === 'auto') {
        overlayMode.value = 'auto'
      } else {
        overlayMode.value = 'manual'
        const parsed = parseFloat(savedOpacity)
        if (!isNaN(parsed)) {
          overlayValue.value = Math.round(parsed * 100)
        }
      }
    }
  } catch (error) {
    console.error('获取配置失败:', error)
  }
}

const beforeUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 < 5

  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过 5MB')
    return false
  }
  return true
}

const handleUploadSuccess = (response) => {
  if (response.code === 200) {
    backgroundUrl.value = response.data.url
    ElMessage.success('上传成功')
  } else {
    ElMessage.error(response.msg || '上传失败')
  }
}

const handleUploadError = () => {
  ElMessage.error('上传失败，请检查网络或登录状态')
}

const selectImage = async (index) => {
  if (fetchingIndex.value !== -1) return
  
  fetchingIndex.value = index
  const img = recommendedImages.value[index]
  
  try {
    const response = await request({
      url: '/api/config/login-background/fetch',
      method: 'post',
      data: { url: img.fullUrl }
    })
    
    if (response.data.code === 200) {
      const url = response.data.data?.url || response.data.data
      if (url) {
        backgroundUrl.value = url
        ElMessage.success('已下载并保存到MinIO')
      } else {
        ElMessage.error('下载图片失败：未获取到URL')
      }
    } else {
      ElMessage.error(response.data.msg || '下载图片失败')
    }
  } catch (error) {
    console.error('下载图片失败:', error)
    ElMessage.error('下载图片失败')
  } finally {
    fetchingIndex.value = -1
  }
}

const saveBackground = async () => {
  saving.value = true
  try {
    const data = { background: backgroundUrl.value }
    if (overlayMode.value === 'manual') {
      data.overlay_opacity = (overlayValue.value / 100).toFixed(2)
    } else {
      data.overlay_opacity = 'auto'
    }
    const response = await request({
      url: '/api/config/login-background',
      method: 'post',
      data
    })
    if (response.data.code === 200) {
      ElMessage.success('保存成功')
    } else {
      ElMessage.error(response.data.msg || '保存失败')
    }
  } catch {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const resetBackground = async () => {
  backgroundUrl.value = ''
  overlayMode.value = 'auto'
  overlayOpacity.value = 'auto'
  overlayValue.value = 30
  await saveBackground()
}

onMounted(() => {
  fetchConfig()
})
</script>

<style lang="scss" scoped>
.settings-page {
  padding: 24px;
  min-height: 100vh;
  background: #f5f7fa;
}

.settings-card {
  max-width: 800px;
  margin: 0 auto;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.card-header {
  padding: 20px 24px;
  border-bottom: 1px solid #ebeef5;

  .title {
    font-size: 18px;
    font-weight: 600;
    color: #303133;
    margin: 0;
  }
}

.setting-section {
  padding: 24px;
  border-bottom: 1px solid #ebeef5;

  &:last-child {
    border-bottom: none;
  }

  .section-title {
    font-size: 15px;
    font-weight: 600;
    color: #303133;
    margin-bottom: 20px;
  }
}

.setting-item {
  margin-bottom: 20px;

  .setting-label {
    font-size: 14px;
    color: #606266;
    margin-bottom: 8px;
  }

  .setting-input {
    width: 100%;
  }
}

.upload-area {
  width: 100%;
  
  :deep(.el-upload) {
    width: 100%;
    border: 1px dashed #d9d9d9;
    border-radius: 8px;
    background: #fafafa;
    transition: all 0.3s;
    
    &:hover {
      border-color: #409eff;
    }
  }
}

.upload-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  
  .upload-icon {
    font-size: 48px;
    color: #c0c4cc;
    margin-bottom: 12px;
  }
  
  .upload-text {
    text-align: center;
    
    span {
      display: block;
      color: #606266;
      font-size: 14px;
    }
    
    .upload-hint {
      font-size: 12px;
      color: #909399;
      margin-top: 4px;
    }
  }
}

.preview-box {
  width: 100%;
  height: 200px;
  border-radius: 8px;
  border: 1px solid #dcdfe6;
  position: relative;
  overflow: hidden;

  .preview-overlay {
    position: absolute;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    font-size: 14px;
    transition: background 0.3s ease;
  }
}

.opacity-control {
  .mode-switch {
    margin-bottom: 14px;
  }

  .slider-row {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 0 4px;

    .opacity-slider {
      flex: 1;
    }

    .slider-value {
      font-size: 14px;
      font-weight: 600;
      color: #409eff;
      min-width: 40px;
      text-align: right;
      font-family: 'Courier New', monospace;
    }
  }

  .auto-hint {
    font-size: 13px;
    color: #909399;
    padding: 4px 0;
  }
}

.label-hint {
  display: block;
  font-size: 12px;
  font-weight: 400;
  color: #909399;
  margin-top: 2px;
}

.setting-actions {
  display: flex;
  gap: 12px;
  margin-top: 20px;
}

.image-gallery {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.gallery-item {
  border-radius: 8px;
  overflow: hidden;
  border: 2px solid transparent;
  cursor: pointer;
  transition: all 0.3s;
  position: relative;

  &:hover {
    border-color: #409eff;
  }

  &.active {
    border-color: #409eff;
    box-shadow: 0 0 8px rgba(64, 158, 255, 0.4);
  }

  &.loading {
    pointer-events: none;
    
    .loading-overlay {
      position: absolute;
      inset: 0;
      background: rgba(255, 255, 255, 0.8);
      display: flex;
      align-items: center;
      justify-content: center;
      
      .el-icon {
        font-size: 24px;
        color: #409eff;
      }
    }
  }

  img {
    width: 100%;
    height: 100px;
    object-fit: cover;
    display: block;
  }

  .img-name {
    padding: 8px;
    text-align: center;
    font-size: 12px;
    color: #606266;
    background: #f5f7fa;
  }
}
</style>

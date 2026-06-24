<template>
  <div class="reset-page">
    <div class="page-left" :style="backgroundStyle">
      <div class="brand-overlay">
        <div class="brand-content">
          <h1 class="brand-title">智汇论坛</h1>
          <div class="brand-divider"></div>
          <p class="brand-slogan">汇聚智慧，分享知识</p>
        </div>
      </div>
    </div>
    <div class="page-right">
      <div class="reset-card">
        <div class="card-header">
          <div class="card-logo">&lt;智汇论坛/&gt;</div>
          <div class="card-subtitle">设置新密码</div>
        </div>

        <div v-if="tokenValid === false" class="error-state">
          <div class="error-icon">
            <el-icon><CircleCloseFilled /></el-icon>
          </div>
          <div class="error-title">链接已失效</div>
          <div class="error-desc">重置链接已过期或无效</div>
          <el-button type="primary" class="retry-btn" @click="goToForget">
            重新获取
          </el-button>
        </div>

        <template v-else-if="tokenValid === true">
          <div v-if="errorMessage" class="msg-box error">
            <el-icon><WarningFilled /></el-icon>
            <span>{{ errorMessage }}</span>
          </div>

          <div v-if="successMessage" class="msg-box success">
            <el-icon><CircleCheckFilled /></el-icon>
            <span>{{ successMessage }}</span>
          </div>

          <el-form 
            :model="form" 
            ref="formRef" 
            :rules="rules" 
            class="form"
            :validate-on-rule-change="false"
          >
            <el-form-item prop="newPassword">
              <el-input
                v-model="form.newPassword"
                placeholder="请输入新密码（6-20位）"
                type="password"
                show-password
                class="input"
              >
                <template #prefix>
                  <el-icon class="icon"><Lock /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item prop="confirmPassword">
              <el-input
                v-model="form.confirmPassword"
                placeholder="请再次输入新密码"
                type="password"
                show-password
                class="input"
                @keyup.enter="handleSubmit"
              >
                <template #prefix>
                  <el-icon class="icon"><Lock /></el-icon>
                </template>
              </el-input>
            </el-form-item>
          </el-form>

          <el-button
            type="primary"
            class="submit-btn"
            @click="handleSubmit"
            :loading="loading"
          >
            {{ loading ? '重置中...' : '确认重置' }}
          </el-button>
        </template>

        <div v-else class="loading-state">
          <el-icon class="loading-icon"><Loading /></el-icon>
          <span>正在验证链接...</span>
        </div>

        <div class="footer-link">
          <span @click="goToLogin">返回登录</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Lock, CircleCloseFilled, CircleCheckFilled, Loading, WarningFilled } from '@element-plus/icons-vue'
import request from '@/utils/requests'

const route = useRoute()
const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const tokenValid = ref(null)
const errorMessage = ref('')
const successMessage = ref('')

const loginBackground = ref('')

const backgroundStyle = computed(() => {
  if (loginBackground.value) {
    return {
      backgroundImage: `url(${loginBackground.value})`,
      backgroundSize: 'cover',
      backgroundPosition: 'center'
    }
  }
  return {
    background: 'linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%)'
  }
})

const fetchLoginBackground = async () => {
  try {
    const response = await request({ url: '/api/config/login-background', method: 'get' })
    if (response.data.code === 200 && response.data.data?.background) {
      loginBackground.value = response.data.data.background
    }
  } catch {
    // 获取背景配置失败时使用默认渐变背景
  }
}

const form = reactive({
  newPassword: '',
  confirmPassword: ''
})

const validateConfirm = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请再次输入密码'))
  } else if (value !== form.newPassword) {
    callback(new Error('两次密码输入不一致'))
  } else {
    callback()
  }
}

const rules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度6-20位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

const validateToken = async () => {
  const token = route.query.token
  if (!token) {
    tokenValid.value = false
    return
  }

  try {
    const response = await request({
      url: '/auth/validate-reset-token',
      method: 'get',
      params: { token }
    })
    tokenValid.value = response.data.code === 200
  } catch {
    tokenValid.value = false
  }
}

const handleSubmit = async () => {
  formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      errorMessage.value = ''
      successMessage.value = ''

      try {
        const token = route.query.token
        const response = await request({
          url: '/auth/reset-password',
          method: 'post',
          data: {
            captcha: token,
            newPassword: form.newPassword,
            confirmPassword: form.confirmPassword
          }
        })

        if (response.data.code === 200) {
          successMessage.value = '密码重置成功！即将跳转...'
          ElMessage.success('重置成功')
          setTimeout(() => router.push('/login'), 2000)
        } else {
          errorMessage.value = response.data.msg || '重置失败'
        }
      } catch (error) {
        errorMessage.value = error.response?.data?.msg || '重置失败'
      } finally {
        loading.value = false
      }
    }
  })
}

const goToLogin = () => router.push('/login')
const goToForget = () => router.push('/forget-password')

onMounted(() => {
  validateToken()
  fetchLoginBackground()
})
</script>

<style lang="scss" scoped>
.reset-page {
  min-height: 100vh;
  display: flex;
  font-family: var(--font-body);
}

.page-left {
  flex: 1;
  position: relative;
  background-size: cover;
  background-position: center;

  .brand-overlay {
    position: absolute;
    inset: 0;
    background: rgba(255, 255, 255, 0.85);
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .brand-content {
    text-align: center;
    color: var(--c-text);

    .brand-title {
      font-family: var(--font-display);
      font-size: 56px;
      font-weight: 700;
      letter-spacing: -0.03em;
      margin-bottom: 16px;
      color: var(--c-text);
    }

    .brand-divider {
      width: 48px;
      height: 3px;
      background: var(--c-primary);
      margin: 0 auto 16px;
    }

    .brand-slogan {
      font-size: 16px;
      font-weight: 400;
      letter-spacing: 0.08em;
      color: var(--c-text-secondary);
    }
  }
}

.page-right {
  width: 480px;
  min-width: 480px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  border-left: 3px solid var(--c-primary);
}

.reset-card {
  width: 380px;
  padding: var(--space-xl) 0;
}

.card-header {
  margin-bottom: var(--space-lg);

  .card-logo {
    font-family: var(--font-mono);
    font-size: 16px;
    font-weight: 600;
    color: var(--c-primary);
    margin-bottom: var(--space-sm);
  }

  .card-subtitle {
    font-size: 14px;
    color: var(--c-text-muted);
  }
}

.msg-box {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: 10px 12px;
  border-radius: var(--radius-sm);
  font-size: 13px;
  margin-bottom: var(--space-md);

  &.error {
    background: #fef2f2;
    border-left: 3px solid var(--c-danger);
    color: var(--c-danger);
  }

  &.success {
    background: #f0fdf4;
    border-left: 3px solid var(--c-success);
    color: var(--c-success);
  }
}

.form {
  :deep(.el-form-item) {
    margin-bottom: var(--space-md);
  }
}

.input {
  :deep(.el-input__wrapper) {
    height: 42px;
    border-radius: var(--radius-md);
    border: 1px solid var(--c-border);
    box-shadow: none;
    padding: 0 12px;
    background: var(--c-bg);
    transition: border-color var(--transition-fast);

    &:hover {
      border-color: var(--c-text-muted);
    }

    &.is-focus {
      border-color: var(--c-primary);
      box-shadow: 0 0 0 3px rgba(14,165,233,0.1);
    }
  }

  :deep(.el-input__inner) {
    height: 42px;
    line-height: 42px;
    font-size: 14px;
    color: var(--c-text);

    &::placeholder {
      color: var(--c-text-muted);
    }
  }

  .icon {
    font-size: 14px;
    color: var(--c-text-muted);
  }
}

.submit-btn {
  width: 100%;
  height: 44px;
  font-family: var(--font-body);
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.02em;
  background: var(--c-primary) !important;
  border: none !important;
  border-radius: var(--radius-md) !important;
  color: #fff !important;
  margin-top: var(--space-sm);

  &:hover {
    background: var(--c-primary-hover) !important;
    box-shadow: var(--shadow-primary);
  }
}

.footer-link {
  text-align: center;
  margin-top: var(--space-md);

  span {
    font-size: 13px;
    color: var(--c-text-muted);
    cursor: pointer;

    &:hover {
      color: var(--c-primary);
    }
  }
}

.error-state {
  text-align: center;
  padding: var(--space-xl) 0;

  .error-icon {
    width: 56px;
    height: 56px;
    margin: 0 auto var(--space-md);
    background: #fef2f2;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;

    .el-icon {
      font-size: 28px;
      color: var(--c-danger);
    }
  }

  .error-title {
    font-family: var(--font-display);
    font-size: 18px;
    font-weight: 700;
    color: var(--c-text);
    margin-bottom: var(--space-sm);
  }

  .error-desc {
    font-size: 13px;
    color: var(--c-text-muted);
    margin-bottom: var(--space-lg);
  }

  .retry-btn {
    height: 36px;
    padding: 0 20px;
    font-size: 13px;
    border-radius: var(--radius-md);
  }
}

.loading-state {
  text-align: center;
  padding: var(--space-3xl) 0;
  color: var(--c-text-muted);
  font-size: 14px;

  .loading-icon {
    font-size: 28px;
    color: var(--c-primary);
    animation: spin 1s linear infinite;
    margin-bottom: var(--space-md);
    display: block;
  }
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@media (max-width: 900px) {
  .page-left {
    display: none;
  }

  .page-right {
    width: 100%;
    min-width: auto;
    border-left: none;
  }
}
</style>

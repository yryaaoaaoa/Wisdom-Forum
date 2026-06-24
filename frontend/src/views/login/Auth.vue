<template>
  <div class="auth-page">
    <div class="background-layer" :style="backgroundStyle"></div>
    
    <div class="auth-card">
      <div class="card-header">
        <div class="card-logo">智汇论坛</div>
        <div class="card-subtitle">{{ subtitleText }}</div>
      </div>

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
        :rules="currentRules"
        class="form"
        :validate-on-rule-change="false"
      >
        <transition name="fade" mode="out-in">
          <div :key="mode" class="form-content">
            <template v-if="mode === 'login'">
              <el-form-item prop="username">
                <el-input
                  v-model="form.username"
                  placeholder="请输入用户名/手机号"
                  clearable
                  class="input"
                >
                  <template #prefix>
                    <el-icon class="icon"><User /></el-icon>
                  </template>
                </el-input>
              </el-form-item>

              <el-form-item prop="password">
                <el-input
                  v-model="form.password"
                  placeholder="请输入密码"
                  type="password"
                  show-password
                  class="input"
                  @keyup.enter="handleLogin"
                >
                  <template #prefix>
                    <el-icon class="icon"><Lock /></el-icon>
                  </template>
                </el-input>
              </el-form-item>

              <el-form-item prop="captcha">
                <div class="captcha-row">
                  <el-input
                    v-model="form.captcha"
                    placeholder="请输入验证码"
                    class="input captcha-input"
                    @keyup.enter="handleLogin"
                  >
                    <template #prefix>
                      <el-icon class="icon"><Key /></el-icon>
                    </template>
                  </el-input>
                  <div class="captcha-right">
                    <img
                      :src="captchaImage"
                      alt="验证码"
                      @click="fetchCaptcha"
                      class="captcha-img"
                      title="点击刷新"
                    />
                    <el-icon class="refresh-btn" @click="fetchCaptcha" title="换一张">
                      <RefreshRight />
                    </el-icon>
                  </div>
                </div>
              </el-form-item>

              <div class="card-tip">
                <el-checkbox v-model="remember" class="remember-checkbox">记住账号</el-checkbox>
                <span class="link" @click="switchMode('forget')">忘记密码？</span>
              </div>
            </template>

            <template v-else-if="mode === 'register'">
              <el-form-item prop="username">
                <el-input
                  v-model="form.username"
                  placeholder="请输入用户名"
                  clearable
                  class="input"
                >
                  <template #prefix>
                    <el-icon class="icon"><User /></el-icon>
                  </template>
                </el-input>
              </el-form-item>

              <el-form-item prop="nickname">
                <el-input
                  v-model="form.nickname"
                  placeholder="请输入昵称"
                  clearable
                  class="input"
                >
                  <template #prefix>
                    <el-icon class="icon"><Avatar /></el-icon>
                  </template>
                </el-input>
              </el-form-item>

              <el-form-item prop="email">
                <el-input
                  v-model="form.email"
                  placeholder="请输入邮箱"
                  clearable
                  class="input"
                >
                  <template #prefix>
                    <el-icon class="icon"><Message /></el-icon>
                  </template>
                </el-input>
              </el-form-item>

              <el-form-item prop="password">
                <el-input
                  v-model="form.password"
                  placeholder="请输入密码（6-20位）"
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
                  placeholder="请再次输入密码"
                  type="password"
                  show-password
                  class="input"
                  @keyup.enter="handleRegister"
                >
                  <template #prefix>
                    <el-icon class="icon"><Lock /></el-icon>
                  </template>
                </el-input>
              </el-form-item>

              <el-form-item prop="captcha">
                <div class="captcha-row">
                  <el-input
                    v-model="form.captcha"
                    placeholder="请输入验证码"
                    class="input captcha-input"
                    @keyup.enter="handleRegister"
                  >
                    <template #prefix>
                      <el-icon class="icon"><Key /></el-icon>
                    </template>
                  </el-input>
                  <div class="captcha-right">
                    <img
                      :src="captchaImage"
                      alt="验证码"
                      @click="fetchCaptcha"
                      class="captcha-img"
                      title="点击刷新"
                    />
                    <el-icon class="refresh-btn" @click="fetchCaptcha" title="换一张">
                      <RefreshRight />
                    </el-icon>
                  </div>
                </div>
              </el-form-item>
            </template>

            <template v-else-if="mode === 'forget'">
              <el-form-item prop="email">
                <el-input
                  v-model="form.email"
                  placeholder="请输入注册邮箱"
                  clearable
                  class="input"
                >
                  <template #prefix>
                    <el-icon class="icon"><Message /></el-icon>
                  </template>
                </el-input>
              </el-form-item>

              <el-form-item prop="captcha">
                <div class="captcha-row">
                  <el-input
                    v-model="form.captcha"
                    placeholder="请输入验证码"
                    class="input captcha-input"
                    @keyup.enter="handleResetPassword"
                  >
                    <template #prefix>
                      <el-icon class="icon"><Key /></el-icon>
                    </template>
                  </el-input>
                  <div class="captcha-right">
                    <img
                      :src="captchaImage"
                      alt="验证码"
                      @click="fetchCaptcha"
                      class="captcha-img"
                      title="点击刷新"
                    />
                    <el-icon class="refresh-btn" @click="fetchCaptcha" title="换一张">
                      <RefreshRight />
                    </el-icon>
                  </div>
                </div>
              </el-form-item>

              <div class="tip-text">
                输入注册邮箱，我们将发送重置密码链接
              </div>
            </template>
          </div>
        </transition>
      </el-form>

      <el-button
        type="primary"
        class="submit-btn"
        @click="handleSubmit"
        :loading="loading"
      >
        {{ buttonText }}
      </el-button>

      <div class="footer-links">
        <template v-if="mode === 'login'">
          <span class="text">还没有账号？</span>
          <span class="link" @click="switchMode('register')">立即注册</span>
        </template>
        <template v-else-if="mode === 'register'">
          <span class="text">已有账号？</span>
          <span class="link" @click="switchMode('login')">立即登录</span>
        </template>
        <template v-else>
          <span class="text">想起密码了？</span>
          <span class="link" @click="switchMode('login')">返回登录</span>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { User, Lock, Key, WarningFilled, RefreshRight, Avatar, Message, CircleCheckFilled } from '@element-plus/icons-vue'
import { login, getCaptcha } from '@/api/auth/auth.js'
import request from '@/utils/requests'

const router = useRouter()
const route = useRoute()
const formRef = ref(null)

const mode = ref('login')
const loginBackground = ref('')
const captchaImage = ref('')
const remember = ref(true)
const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')

const backgroundStyle = computed(() => {
  if (loginBackground.value) {
    return {
      backgroundImage: `url(${loginBackground.value})`,
      backgroundSize: 'cover',
      backgroundPosition: 'center'
    }
  }
  return {
    background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)'
  }
})

const subtitleText = computed(() => {
  const map = {
    login: '欢迎回到社区',
    register: '创建新账号',
    forget: '重置密码'
  }
  return map[mode.value]
})

const buttonText = computed(() => {
  const map = {
    login: loading.value ? '登录中...' : '登 录',
    register: loading.value ? '注册中...' : '注 册',
    forget: loading.value ? '发送中...' : '发送重置链接'
  }
  return map[mode.value]
})

const form = reactive({
  username: '',
  nickname: '',
  email: '',
  password: '',
  confirmPassword: '',
  captcha: '',
  captchaKey: ''
})

const validateConfirm = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请再次输入密码'))
  } else if (value !== form.password) {
    callback(new Error('两次密码输入不一致'))
  } else {
    callback()
  }
}

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captcha: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名3-20个字符', trigger: 'blur' }
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 2, max: 20, message: '昵称2-20个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码6-20个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ],
  captcha: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

const forgetRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  captcha: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

const currentRules = computed(() => {
  const map = {
    login: loginRules,
    register: registerRules,
    forget: forgetRules
  }
  return map[mode.value]
})

const switchMode = (newMode) => {
  mode.value = newMode
  errorMessage.value = ''
  successMessage.value = ''
  resetForm()
  fetchCaptcha()
}

const resetForm = () => {
  form.username = ''
  form.nickname = ''
  form.email = ''
  form.password = ''
  form.confirmPassword = ''
  form.captcha = ''
  formRef.value?.clearValidate()
}

const fetchCaptcha = async () => {
  try {
    const response = await getCaptcha()
    const data = response.data.data
    if (data?.imageBase64 && data?.key) {
      captchaImage.value = `data:image/png;base64,${data.imageBase64}`
      form.captchaKey = data.key
    }
  } catch {
    errorMessage.value = '验证码获取失败'
  }
}

const fetchLoginBackground = async () => {
  try {
    const response = await request({ url: '/api/config/login-background', method: 'get' })
    if (response.data.code === 200 && response.data.data?.background) {
      loginBackground.value = response.data.data.background
    }
  } catch { /* ignore */ }
}

const shouldRefreshToken = (token) => {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    return payload.exp * 1000 - Date.now() < 5 * 60 * 1000
  } catch {
    return false
  }
}

const handleLogin = async () => {
  localStorage.removeItem('access_token')
  localStorage.removeItem('refresh_token')
  loading.value = true
  errorMessage.value = ''

  try {
    const response = await login({
      username: form.username,
      password: form.password,
      captcha: form.captcha,
      captchaKey: form.captchaKey
    })

    const { access_token, refresh_token } = response.data.data
    localStorage.setItem('access_token', access_token)
    localStorage.setItem('refresh_token', refresh_token)

    if (remember.value) {
      localStorage.setItem('rememberedUsername', form.username)
    } else {
      localStorage.removeItem('rememberedUsername')
    }

    await router.push('/home')
  } catch {
    errorMessage.value = '登录失败，请检查用户名、密码和验证码'
    fetchCaptcha()
  } finally {
    loading.value = false
  }
}

const handleRegister = async () => {
  loading.value = true
  errorMessage.value = ''

  try {
    const response = await request({
      url: '/auth/register',
      method: 'post',
      data: form
    })

    if (response.data.code === 200) {
      successMessage.value = '注册成功！请登录'
      setTimeout(() => {
        switchMode('login')
        form.username = response.data.data?.username || form.username
      }, 1500)
    } else {
      errorMessage.value = response.data.msg || '注册失败'
      fetchCaptcha()
    }
  } catch (error) {
    errorMessage.value = error.response?.data?.msg || '注册失败'
    fetchCaptcha()
  } finally {
    loading.value = false
  }
}

const handleResetPassword = async () => {
  loading.value = true
  errorMessage.value = ''

  try {
    const response = await request({
      url: '/auth/reset-password',
      method: 'post',
      data: {
        email: form.email,
        captcha: form.captcha,
        captchaKey: form.captchaKey
      }
    })

    if (response.data.code === 200) {
      successMessage.value = '重置链接已发送到您的邮箱，请查收'
    } else {
      errorMessage.value = response.data.msg || '发送失败'
      fetchCaptcha()
    }
  } catch (error) {
    errorMessage.value = error.response?.data?.msg || '发送失败'
    fetchCaptcha()
  } finally {
    loading.value = false
  }
}

const handleSubmit = () => {
  formRef.value.validate(async (valid) => {
    if (valid) {
      if (mode.value === 'login') {
        await handleLogin()
      } else if (mode.value === 'register') {
        await handleRegister()
      } else if (mode.value === 'forget') {
        await handleResetPassword()
      }
    }
  })
}

const checkStoredToken = () => {
  const token = localStorage.getItem('access_token')
  const refreshToken = localStorage.getItem('refresh_token')
  if (token && refreshToken && !shouldRefreshToken(token)) {
    router.push('/home')
    return true
  }
  return false
}

watch(() => route.path, (path) => {
  if (path === '/register') {
    mode.value = 'register'
  } else if (path === '/forget-password') {
    mode.value = 'forget'
  } else {
    mode.value = 'login'
  }
  resetForm()
  fetchCaptcha()
}, { immediate: true })

onMounted(() => {
  if (!checkStoredToken()) {
    fetchCaptcha()
    fetchLoginBackground()
    const rememberedUsername = localStorage.getItem('rememberedUsername')
    if (rememberedUsername) {
      form.username = rememberedUsername
      remember.value = true
    }
  }
})
</script>

<style lang="scss" scoped>
@keyframes wheat-sway {
  0% {
    transform: scale(1.05) translate(0, 0);
  }
  25% {
    transform: scale(1.06) translate(-0.5%, 0.2%);
  }
  50% {
    transform: scale(1.07) translate(0, 0.3%);
  }
  75% {
    transform: scale(1.06) translate(0.5%, 0.1%);
  }
  100% {
    transform: scale(1.05) translate(0, 0);
  }
}

.auth-page {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  font-family: var(--font-body);
}

.background-layer {
  position: absolute;
  top: -5%;
  left: -5%;
  width: 110%;
  height: 110%;
  background-size: cover;
  background-position: center;
  filter: blur(3px) saturate(0.75);
  z-index: 0;
  animation: wheat-sway 25s ease-in-out infinite;
  will-change: transform;
  
  &::after {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: rgba(0, 0, 0, 0.15);
  }
}

.auth-card {
  position: relative;
  z-index: 1;
  width: 420px;
  padding: 48px 40px;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(10px) saturate(1.2);
  -webkit-backdrop-filter: blur(10px) saturate(1.2);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}

.card-header {
  margin-bottom: 36px;
  text-align: center;

  .card-logo {
    font-size: 30px;
    font-weight: 600;
    color: #fff;
    margin-bottom: 10px;
    letter-spacing: 2px;
    text-shadow: 0 2px 12px rgba(255, 154, 60, 0.4);
  }

  .card-subtitle {
    font-size: 14px;
    color: rgba(255, 255, 255, 0.8);
    letter-spacing: 1px;
    transition: all 0.3s ease;
  }
}

.msg-box {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  border-radius: 6px;
  font-size: 14px;
  margin-bottom: 20px;

  &.error {
    background: rgba(255, 82, 82, 0.2);
    border: 1px solid rgba(255, 82, 82, 0.4);
    color: #fff;
    backdrop-filter: blur(10px);
  }

  &.success {
    background: rgba(255, 154, 60, 0.2);
    border: 1px solid rgba(255, 183, 77, 0.4);
    color: #fff;
    backdrop-filter: blur(10px);
  }
}

.form {
  :deep(.el-form-item) {
    margin-bottom: 18px;
  }
}

.form-content {
  transition: all 0.3s ease;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.fade-enter-from {
  opacity: 0;
  transform: translateX(10px);
}

.fade-leave-to {
  opacity: 0;
  transform: translateX(-10px);
}

.input {
  :deep(.el-input__wrapper) {
    height: 44px;
    border-radius: 6px;
    border: 1px solid rgba(255, 255, 255, 0.3);
    box-shadow: none;
    padding: 0 14px;
    background: rgba(255, 255, 255, 0.1);
    backdrop-filter: blur(10px);
    transition: all 0.3s ease;

    &:hover {
      border-color: rgba(255, 255, 255, 0.5);
      background: rgba(255, 255, 255, 0.15);
    }

    &.is-focus {
      border-color: #ff9a3c;
      background: rgba(255, 255, 255, 0.18);
      box-shadow: 0 0 0 3px rgba(255, 154, 60, 0.2);
    }
  }

  :deep(.el-input__inner) {
    height: 44px;
    line-height: 44px;
    font-size: 14px;
    color: #fff;

    &::placeholder {
      color: rgba(255, 255, 255, 0.6);
    }
  }

  .icon {
    font-size: 16px;
    color: rgba(255, 255, 255, 0.8);
  }
}

.captcha-row {
  display: flex;
  gap: 12px;
  align-items: stretch;

  .captcha-input {
    flex: 1;
  }

  .captcha-right {
    display: flex;
    flex-direction: column;
    gap: 6px;
    min-width: 120px;

    .captcha-img {
      height: 32px;
      width: 100%;
      cursor: pointer;
      border: 1px solid rgba(255, 255, 255, 0.3);
      border-radius: 6px;
      object-fit: cover;
      background: rgba(255, 255, 255, 0.1);
      transition: all 0.3s ease;

      &:hover {
        border-color: #ff9a3c;
        transform: scale(1.02);
      }
    }

    .refresh-btn {
      font-size: 16px;
      color: rgba(255, 255, 255, 0.8);
      cursor: pointer;
      text-align: center;
      transition: all 0.3s ease;
      padding: 4px;
      border-radius: 4px;

      &:hover {
        color: #ffb74d;
        background: rgba(255, 255, 255, 0.1);
        transform: rotate(180deg);
      }
    }
  }
}

.card-tip {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;

  .remember-checkbox {
    :deep(.el-checkbox__label) {
      font-size: 14px;
      color: rgba(255, 255, 255, 0.8);
    }

    :deep(.el-checkbox__inner) {
      background: rgba(255, 255, 255, 0.1);
      border: 1px solid rgba(255, 255, 255, 0.3);
    }

    :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
      background: #ff9a3c;
      border-color: #ff9a3c;
    }
  }

  .link {
    font-size: 14px;
    color: rgba(255, 255, 255, 0.8);
    cursor: pointer;
    transition: all 0.3s ease;

    &:hover {
      color: #ffb74d;
    }
  }
}

.tip-text {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
  text-align: center;
  margin-bottom: 16px;
}

.submit-btn {
  width: 100%;
  height: 46px;
  font-family: var(--font-body);
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 2px;
  background: linear-gradient(135deg, #ff9a3c, #ffb74d) !important;
  border: none !important;
  border-radius: 6px !important;
  color: #fff !important;
  box-shadow: 0 4px 20px rgba(255, 154, 60, 0.35);
  transition: all 0.3s ease !important;

  &:hover {
    background: linear-gradient(135deg, #ffab56, #ffc67d) !important;
    box-shadow: 0 6px 25px rgba(255, 154, 60, 0.45);
    transform: translateY(-2px);
  }

  &:active {
    transform: translateY(0);
  }
}

.footer-links {
  text-align: center;
  margin-top: 24px;
  display: flex;
  justify-content: center;
  gap: 8px;

  .text {
    font-size: 14px;
    color: rgba(255, 255, 255, 0.8);
  }

  .link {
    font-size: 14px;
    color: #ffb74d;
    cursor: pointer;
    transition: all 0.3s ease;

    &:hover {
      color: #ffc67d;
    }
  }
}

@media (max-width: 520px) {
  .auth-card {
    width: calc(100% - 32px);
    padding: 36px 28px;
    margin: 16px;
  }

  .card-header {
    .card-logo {
      font-size: 26px;
    }
  }
}
</style>

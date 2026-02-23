<template>
  <div class="login">
    <!-- 背景装饰 -->
    <div class="login-bg-decoration"></div>

    <!-- 登录卡片 -->
    <div class="login-card">
      <!-- 登录卡片头部 -->
      <div class="login-card-header">

        <div class="login-card-title">智汇论坛</div>
        <div class="login-card-subtitle">欢迎回来，请登录您的账号</div>
      </div>

      <!-- 错误提示 -->
      <div v-if="errorMessage" class="error-message">
        <i class="el-icon-warning"></i> {{ errorMessage }}
      </div>

      <!-- 表单区域 -->
      <div class="login-form-wrapper">
        <el-form :model="loginInfo" ref="loginForm" :rules="rules" class="login-form">
          <!-- 用户名输入项 -->
          <el-form-item prop="username" class="form-item">
            <el-input
                v-model="loginInfo.username"
                placeholder="请输入用户名/手机号"
                clearable
                size="large"
                class="form-input"
            >
              <template #prefix>
                <i class="el-icon-user input-icon"></i>
              </template>
            </el-input>
          </el-form-item>

          <!-- 密码输入项 -->
          <el-form-item prop="password" class="form-item">
            <el-input
                v-model="loginInfo.password"
                placeholder="请输入密码"
                type="password"
                show-password
                size="large"
                class="form-input"
                @keyup.enter="handleLogin"
            >
              <template #prefix>
                <i class="el-icon-lock input-icon"></i>
              </template>
            </el-input>
          </el-form-item>

          <!-- 验证码输入项 -->
          <el-form-item prop="captcha" class="form-item">
            <div class="captcha-wrapper">
              <el-input
                  v-model="loginInfo.captcha"
                  placeholder="请输入验证码"
                  size="large"
                  class="form-input captcha-input"
                  @keyup.enter="handleLogin"
              >
                <template #prefix>
                  <i class="el-icon-key input-icon"></i>
                </template>
              </el-input>
              <img
                  :src="captchaImage"
                  alt="验证码"
                  @click="fetchCaptcha"
                  class="captcha-image"
                  title="点击刷新验证码"
              />
            </div>
          </el-form-item>
        </el-form>
      </div>

      <!-- 登录提示信息区域 -->
      <div class="login-card-tip">
        <el-checkbox v-model="remember" class="remember-checkbox">记住用户名</el-checkbox>
        <span class="register-link" style="cursor: pointer;">立即注册</span>
      </div>

      <!-- 登录按钮 -->
      <el-button
          type="primary"
          class="login-card-btn"
          @click="handleLogin"
          :loading="loading"
          size="large"
      >
        {{ loading ? '登录中...' : '登 录' }}
      </el-button>
    </div>
  </div>
</template>

<script setup>
// 导入Vue Composition API相关函数
import { ref, reactive, onMounted } from 'vue'
// 导入认证相关API
import { login, getCaptcha } from '@/api/auth/auth.js'
import { useRouter } from 'vue-router'

// 表单引用，用于访问表单实例
const loginForm = ref(null)

// 登录信息对象，使用reactive创建响应式数据
const loginInfo = reactive({
  username: '',
  password: '',
  captcha: ''
})

// 获取路由实例
const router = useRouter()

// 验证码相关数据
const captchaKey = ref('')     // 验证码标识
const captchaImage = ref('')   // 验证码图片数据
const remember = ref(true)     // 是否记住用户名
const loading = ref(false)     // 登录加载状态
const errorMessage = ref('')   // 错误信息

// 表单验证规则
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ],
  captcha: [
    { required: true, message: '请输入验证码', trigger: 'blur' }
  ]
}

// 获取验证码函数
const fetchCaptcha = async () => {
  try {
    const response = await getCaptcha()
    const captchaData = response.data.data
    // 检查验证码数据是否完整
    if (captchaData && captchaData.imageBase64 && captchaData.key) {
      // 将base64图片数据转换为可用的图片URL
      captchaImage.value = `data:image/png;base64,${captchaData.imageBase64}`
      captchaKey.value = captchaData.key
    }
  } catch (error) {
    errorMessage.value = '验证码获取失败，请刷新重试'
  }
}

// 检查token是否即将过期
const shouldRefreshToken = (token) => {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    const exp = payload.exp * 1000
    const now = Date.now()
    // 如果距离过期时间小于5分钟，则需要刷新
    return exp - now < 5 * 60 * 1000
  } catch (e) {
    return false
  }
}

// 登录处理函数
const handleLogin = async () => {
  // 表单验证
  loginForm.value.validate(async (valid) => {
    if (valid) {
      // 登录前清除旧的token
      localStorage.removeItem('access_token')
      localStorage.removeItem('refreshToken')

      // 设置加载状态
      loading.value = true
      errorMessage.value = ''

      try {
        // 构造登录数据
        const loginData = {
          username: loginInfo.username,
          password: loginInfo.password,
          captcha: loginInfo.captcha,
          captchaKey: captchaKey.value
        }

        // 发起登录请求
        const response = await login(loginData)
        console.log(response.data)

        // 保存新的token
        const { access_token, refresh_token } = response.data.data
        localStorage.setItem('access_token', access_token)
        localStorage.setItem('refreshToken', refresh_token)
        console.log('Token saved:', access_token)

        // 如果记住用户名，则保存用户名
        if (remember.value) {
          localStorage.setItem('rememberedUsername', loginInfo.username)
        } else {
          localStorage.removeItem('rememberedUsername')
        }

        // 登录成功后跳转到指定页面
        await router.push('/home')
      } catch (error) {
        console.error('登录失败:', error)
        errorMessage.value = '登录失败，请检查用户名、密码和验证码'
        // 登录失败后刷新验证码
        fetchCaptcha()
      } finally {
        loading.value = false
      }
    }
  })
}

// 检查本地存储的token并尝试自动登录
const checkStoredToken = () => {
  const token = localStorage.getItem('access_token')
  const refreshToken = localStorage.getItem('refreshToken')

  if (token && refreshToken) {
    // 检查token是否有效或即将过期
    if (!shouldRefreshToken(token)) {
      // token有效，直接跳转到主页
      router.push('/home')
      return true
    }
  }
  return false
}

// 组件挂载时获取验证码或尝试自动登录
onMounted(() => {
  // 尝试自动登录
  if (!checkStoredToken()) {
    // 无法自动登录，显示登录表单
    fetchCaptcha()

    // 如果记住用户名，填充用户名字段
    const rememberedUsername = localStorage.getItem('rememberedUsername')
    if (rememberedUsername) {
      loginInfo.username = rememberedUsername
      remember.value = true
    }
  }
})
</script>

<style lang="scss" scoped>
// 登录页面整体样式
.login {
  width: 100%;
  height: 100vh;
  background: linear-gradient(135deg, #e8f4f8 0%, #f0f8fb 100%);
  position: relative;
  overflow: hidden;

  // 背景装饰元素
  &-bg-decoration {
    position: absolute;
    top: -10%;
    right: -10%;
    width: 60%;
    height: 60%;
    background: radial-gradient(circle, rgba(64, 158, 255, 0.05) 0%, rgba(64, 158, 255, 0) 70%);
    border-radius: 50%;
    z-index: 0;
  }

  // 登录卡片样式
  &-card {
    position: relative;
    z-index: 1;
    width: 420px;
    padding: 45px 40px 50px;
    background-color: #ffffff;
    border-radius: 12px;
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.06);
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    transition: all 0.3s ease;

    &:hover {
      box-shadow: 0 12px 40px rgba(0, 0, 0, 0.08);
    }

    // 卡片头部
    &-header {
      text-align: center;
      margin-bottom: 30px;

      .login-card-logo {
        width: 60px;
        height: 60px;
        line-height: 60px;
        background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
        border-radius: 50%;
        margin: 0 auto 15px;
        color: white;
        font-size: 24px;
        box-shadow: 0 4px 12px rgba(64, 158, 255, 0.2);
      }

      .login-card-title {
        color: #1f2937;
        font-size: 22px;
        font-weight: 600;
        margin-bottom: 8px;
      }

      .login-card-subtitle {
        color: #9ca3af;
        font-size: 14px;
      }
    }

    // 卡片提示信息区域样式
    &-tip {
      margin-top: 18px;
      padding: 0 2px;
      display: flex;
      justify-content: space-between;
      align-items: center;

      .remember-checkbox {
        color: #6b7280;
        font-size: 14px;

        :deep(.el-checkbox__label) {
          font-weight: 400;
        }
      }

      .register-link {
        color: #409eff;
        font-size: 14px;
        transition: color 0.2s;

        &:hover {
          color: #66b1ff;
        }
      }
    }

    // 登录按钮样式
    &-btn {
      width: 100%;
      height: 48px;
      font-size: 16px;
      font-weight: 500;
      background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
      border: none;
      border-radius: 8px;
      box-shadow: 0 4px 12px rgba(64, 158, 255, 0.2);
      transition: all 0.2s;
      margin-top: 25px;

      &:hover {
        transform: translateY(-1px);
        box-shadow: 0 6px 16px rgba(64, 158, 255, 0.25);
      }

      &:active {
        transform: translateY(0);
      }
    }
  }
}

// 表单容器
.login-form-wrapper {
  margin-bottom: 10px;
}

// 表单样式
.login-form {
  .form-item {
    margin-bottom: 20px;

    &:last-of-type {
      margin-bottom: 0;
    }
  }

  .form-input {
    height: 48px;
    border-radius: 8px;
    transition: all 0.2s;

    :deep(.el-input__inner) {
      height: 48px;
      line-height: 48px;
      border-radius: 8px;
      border: 1px solid #e5e7eb;
      padding: 0 16px;
      font-size: 14px;
      background-color: #ffffff;

      &:focus {
        border-color: #409eff;
        box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.1);
      }

      &:hover {
        border-color: #d1d5db;
      }
    }
  }
}

// 输入框图标样式
.input-icon {
  font-size: 18px;
  color: #9ca3af;
  margin-right: 8px;
}

// 验证码容器样式
.captcha-wrapper {
  display: flex;
  gap: 12px;
  align-items: center;

  .captcha-input {
    flex: 1;
  }

  .captcha-image {
    height: 48px;
    width: 120px;
    cursor: pointer;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    transition: all 0.2s;
    object-fit: cover;

    &:hover {
      border-color: #d1d5db;
      transform: scale(1.02);
    }
  }
}

// 错误信息样式
.error-message {
  color: #ef4444;
  margin-bottom: 20px;
  padding: 12px 16px;
  background-color: #fef2f2;
  border-radius: 8px;
  text-align: left;
  font-size: 14px;
  display: flex;
  align-items: center;

  i {
    margin-right: 8px;
    font-size: 16px;
  }
}

// 覆盖Element Plus默认样式
:deep(.el-form-item__error) {
  font-size: 12px;
  padding-top: 4px;
}

:deep(.el-checkbox__inner) {
  border-radius: 4px;
  border-color: #d1d5db;
}

:deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background-color: #409eff;
  border-color: #409eff;
}
</style>

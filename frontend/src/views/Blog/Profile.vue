<template>
  <div class="profile-page">
    <div v-if="!isLoggedIn" class="login-prompt">
      <el-empty description="请先登录后访问个人中心">
        <el-button type="primary" @click="router.push('/login')">去登录</el-button>
      </el-empty>
    </div>
    <div v-else class="profile-container">
      <div class="profile-header">
        <h2>个人中心</h2>
      </div>

      <div class="profile-content" v-loading="loading">
        <div class="avatar-section">
          <div class="avatar-wrapper">
            <el-avatar :size="100" :src="avatarDisplayUrl">
              <el-icon :size="50"><User /></el-icon>
            </el-avatar>
            <div class="avatar-overlay" @click="triggerUpload">
              <el-icon><Camera /></el-icon>
              <span>更换头像</span>
            </div>
          </div>
          <input 
            ref="fileInput" 
            type="file" 
            accept="image/*" 
            style="display: none" 
            @change="handleAvatarChange"
          />
        </div>

        <el-form 
          ref="formRef" 
          :model="formData" 
          :rules="rules" 
          label-width="80px" 
          class="profile-form"
        >
          <el-form-item label="用户名">
            <el-input v-model="userInfo.username" disabled />
          </el-form-item>

          <el-form-item label="昵称" prop="nickname">
            <el-input v-model="formData.nickname" placeholder="请输入昵称" />
          </el-form-item>

          <el-form-item label="邮箱" prop="email">
            <el-input v-model="formData.email" placeholder="请输入邮箱" />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" @click="handleSaveInfo" :loading="saving">
              保存修改
            </el-button>
          </el-form-item>
        </el-form>

        <el-divider />

        <div class="password-section">
          <h3>修改密码</h3>
          <el-form 
            ref="passwordFormRef" 
            :model="passwordForm" 
            :rules="passwordRules" 
            label-width="100px"
          >
            <el-form-item label="当前密码" prop="oldPassword">
              <el-input 
                v-model="passwordForm.oldPassword" 
                type="password" 
                placeholder="请输入当前密码"
                show-password
              />
            </el-form-item>

            <el-form-item label="新密码" prop="newPassword">
              <el-input 
                v-model="passwordForm.newPassword" 
                type="password" 
                placeholder="请输入新密码"
                show-password
              />
            </el-form-item>

            <el-form-item label="确认新密码" prop="confirmPassword">
              <el-input 
                v-model="passwordForm.confirmPassword" 
                type="password" 
                placeholder="请再次输入新密码"
                show-password
              />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" @click="handleResetPassword" :loading="resetting">
                修改密码
              </el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { User, Camera } from '@element-plus/icons-vue'
import { getCurrentUser, uploadAvatar, updateUserInfo, resetPassword } from '@/api/user/profile.js'
import { getAvatarUrl } from '@/utils/avatar.js'

const router = useRouter()
const isLoggedIn = computed(() => !!localStorage.getItem('access_token'))

const loading = ref(false)
const saving = ref(false)
const resetting = ref(false)
const fileInput = ref(null)
const formRef = ref(null)
const passwordFormRef = ref(null)

const userInfo = ref({
  id: null,
  username: '',
  nickname: '',
  email: '',
  avatarUrl: ''
})

const avatarDisplayUrl = computed(() => getAvatarUrl(userInfo.value.avatarUrl))

const formData = reactive({
  nickname: '',
  email: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const rules = {
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ]
}

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const fetchUserInfo = async () => {
  loading.value = true
  try {
    const res = await getCurrentUser()
    if (res.data.code === 200) {
      userInfo.value = res.data.data
      formData.nickname = res.data.data.nickname
      formData.email = res.data.data.email
    }
  } catch (error) {
    console.error('获取用户信息失败:', error)
  } finally {
    loading.value = false
  }
}

const triggerUpload = () => {
  fileInput.value?.click()
}

const handleAvatarChange = async (event) => {
  const file = event.target.files?.[0]
  if (!file) return

  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return
  }

  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB')
    return
  }

  try {
    const formDataUpload = new FormData()
    formDataUpload.append('file', file)
    
    const res = await uploadAvatar(formDataUpload)
    if (res.data.code === 200 && res.data.data) {
      userInfo.value.avatarUrl = res.data.data
      ElMessage.success('头像更新成功')
    } else {
      ElMessage.error(res.data.msg || '头像上传失败')
    }
  } catch (error) {
    console.error('头像上传失败:', error)
    ElMessage.error('头像上传失败')
  }

  event.target.value = ''
}

const handleSaveInfo = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    const res = await updateUserInfo(userInfo.value.id, formData)
    if (res.data.code === 200) {
      userInfo.value.nickname = res.data.data.nickname
      userInfo.value.email = res.data.data.email
      ElMessage.success('保存成功')
    }
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const handleResetPassword = async () => {
  const valid = await passwordFormRef.value?.validate().catch(() => false)
  if (!valid) return

  resetting.value = true
  try {
    const res = await resetPassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    if (res.data.code === 200) {
      ElMessage.success('密码修改成功')
      passwordForm.oldPassword = ''
      passwordForm.newPassword = ''
      passwordForm.confirmPassword = ''
      passwordFormRef.value?.resetFields()
    }
  } catch (error) {
    ElMessage.error('密码修改失败')
  } finally {
    resetting.value = false
  }
}

onMounted(() => {
  if (isLoggedIn.value) {
    fetchUserInfo()
  }
})
</script>

<style lang="scss" scoped>
.profile-page {
  padding: 24px;
}

.profile-container {
  max-width: 600px;
  margin: 0 auto;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.profile-header {
  padding: 20px 24px;
  border-bottom: 1px solid #f0f0f0;

  h2 {
    margin: 0;
    font-size: 18px;
    color: #303133;
  }
}

.profile-content {
  padding: 24px;
}

.avatar-section {
  display: flex;
  justify-content: center;
  margin-bottom: 32px;
}

.avatar-wrapper {
  position: relative;
  cursor: pointer;

  .avatar-overlay {
    position: absolute;
    top: 0;
    left: 0;
    width: 100px;
    height: 100px;
    border-radius: 50%;
    background: rgba(0, 0, 0, 0.5);
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    color: #fff;
    opacity: 0;
    transition: opacity 0.3s;

    .el-icon {
      font-size: 20px;
      margin-bottom: 4px;
    }

    span {
      font-size: 12px;
    }
  }

  &:hover .avatar-overlay {
    opacity: 1;
  }
}

.profile-form {
  max-width: 400px;
}

.password-section {
  h3 {
    margin: 0 0 20px;
    font-size: 16px;
    color: #303133;
  }
}
</style>

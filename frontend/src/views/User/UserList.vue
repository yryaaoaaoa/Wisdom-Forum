<!-- src/views/User/UserList.vue -->
<template>
  <div class="user-list">
    <div class="header">
      <h2>用户管理</h2>
      <el-button type="primary" @click="handleAddUser">添加用户</el-button>
    </div>

    <el-table :data="users" border style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="账号" width="160" />
      <el-table-column prop="nickname" label="用户名" width="150" />
      <el-table-column label="头像" width="80">
        <template #default="scope">
          <el-avatar :size="40" :src="getAvatarUrl(scope.row.avatarUrl)">
            <el-icon><User /></el-icon>
          </el-avatar>
        </template>
      </el-table-column>
      <el-table-column prop="email" label="邮箱" width="200" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.status === 'active' ? 'success' : 'warning'">
            {{ scope.row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="roleName" label="角色" width="150" />
      <el-table-column prop="createdAt" label="创建时间" width="180" />

      <el-table-column label="操作" width="200">
        <template #default="scope">
          <el-button size="small" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button
              size="small"
              type="danger"
              @click="handleDelete(scope.row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
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

    <!-- 用户编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form
          :model="currentUser"
          :rules="formRules"
          ref="userForm"
          label-width="80px"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="currentUser.username" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="currentUser.nickname" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="currentUser.email" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch
              v-model="currentUser.status"
              active-value="active"
              inactive-value="inactive"
          />
        </el-form-item>
        <el-form-item v-if="!isEdit" label="密码" prop="password">
          <el-input v-model="currentUser.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="currentUser.role" placeholder="请选择角色" style="width: 100%">
            <el-option
              v-for="role in roles"
              :key="role.id"
              :label="role.roleName"
              :value="role.roleName"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm" :loading="submitLoading">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { User } from '@element-plus/icons-vue'
import { getUsers, createUser, updateUser, deleteUser } from '@/api/user/user.js'
import request from '@/utils/requests'
import { getAvatarUrl } from '@/utils/avatar.js'

const roles = ref([])

const fetchRoles = async () => {
  try {
    const res = await request({ url: '/api/roles', method: 'get' })
    if (res.data.code === 200) {
      roles.value = res.data.data || []
    }
  } catch (error) {
    console.error('获取角色列表失败:', error)
  }
}

// 用户数据
const users = ref([])

// 分页相关
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 加载状态
const loading = ref(false)
const submitLoading = ref(false)

// 对话框相关
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)

// 当前编辑的用户
const currentUser = reactive({
  id: '',
  username: '',
  email: '',
  role: 'user',
  status: 'active',
  nickname: '',
  password: ''
})

// 表单验证规则
const formRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  role: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ],
  nickname: [
    { required: false }
  ],
  password: [
    { required: !isEdit.value, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ]
}

const userForm = ref(null)

// 获取用户列表
const fetchUsers = async () => {
  loading.value = true
  try {
    const params = {
      current: currentPage.value,
      size: pageSize.value
    }

    const response = await getUsers(params)

    if (response.data.code !== 200) {
      ElMessage.error(response.data.msg || '获取用户列表失败')
      users.value = []
      total.value = 0
    } else if (response.data.data && response.data.data.records) {
      console.log(response.data.data.records)
      users.value = response.data.data.records.map(user => ({
        ...user,
        createTime: user.created_at,
        status: user.enabled ? 'active' : 'inactive'
      }))
      total.value = response.data.data.total
    } else {
      users.value = []
      total.value = 0
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || '获取用户列表失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 添加用户
const handleAddUser = () => {
  dialogTitle.value = '添加用户'
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

// 编辑用户
const handleEdit = (row) => {
  dialogTitle.value = '编辑用户'
  isEdit.value = true
  Object.assign(currentUser, {
    id: row.id,
    username: row.username,
    email: row.email,
    role: row.roleName || 'ROLE_USER',
    status: row.status,
    nickname: row.nickname || row.username
    // 注意：不复制 password
  })
  dialogVisible.value = true
}

// 删除用户
const handleDelete = (row) => {
  ElMessageBox.confirm(
      `确认删除用户 "${row.username}" 吗？`,
      '警告',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'warning'
      }
  ).then(async () => {
    try {
      await deleteUser(row.id)
      ElMessage.success('删除成功')
      fetchUsers()
    } catch (error) {
      ElMessage.error(error.response?.data?.msg || '删除失败')
      console.error(error)
    }
  }).catch(() => {
    ElMessage.info('已取消删除')
  })
}

// 提交表单
const submitForm = () => {
  userForm.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        if (isEdit.value) {
          const userData = {
            nickname: currentUser.nickname || currentUser.username,
            email: currentUser.email,
            enabled: currentUser.status === 'active',
            roles: currentUser.role ? [currentUser.role] : []
          }
          await updateUser(currentUser.id, userData)
          ElMessage.success('更新成功')
        } else {
          const userData = {
            username: currentUser.username,
            nickname: currentUser.nickname || currentUser.username,
            email: currentUser.email,
            password: currentUser.password
          }
          await createUser(userData)
          ElMessage.success('添加成功')
        }

        dialogVisible.value = false
        fetchUsers()
      } catch (error) {
        ElMessage.error(error.response?.data?.msg || (isEdit.value ? '更新失败' : '添加失败'))
        console.error(error)
      } finally {
        submitLoading.value = false
      }
    }
  })
}

// 重置表单
const resetForm = () => {
  Object.assign(currentUser, {
    id: '',
    username: '',
    email: '',
    role: 'user',
    status: 'active',
    nickname: '',
    password: ''
  })
}

// 分页事件
const handleSizeChange = (val) => {
  pageSize.value = val
  fetchUsers()
}

const handleCurrentChange = (val) => {
  currentPage.value = val
  fetchUsers()
}

onMounted(() => {
  fetchUsers()
  fetchRoles()
})
</script>

<style lang="scss" scoped>
.user-list {
  background-color: #fff;
  padding: 20px;
  border-radius: 4px;

  .header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
  }

  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>

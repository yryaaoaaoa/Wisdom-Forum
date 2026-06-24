<template>
  <div class="role-list">
    <div class="header">
      <h2>角色管理</h2>
      <el-button type="primary" @click="handleAddRole">添加角色</el-button>
    </div>

    <el-table :data="roles" border style="width: 100%" v-loading="loading">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="roleName" label="角色名称" width="150" />
      <el-table-column prop="roleDescription" label="角色描述" width="200" />
      <el-table-column label="权限" min-width="300">
        <template #default="scope">
          <div class="permission-tags">
            <el-tag 
              v-for="permission in scope.row.permissions?.slice(0, 5)" 
              :key="permission" 
              size="small" 
              class="permission-tag"
            >
              {{ permission }}
            </el-tag>
            <el-tag 
              v-if="scope.row.permissions?.length > 5" 
              size="small" 
              type="info"
            >
              +{{ scope.row.permissions.length - 5 }}
            </el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180">
        <template #default="scope">
          {{ formatTime(scope.row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form
        :model="currentRole"
        :rules="formRules"
        ref="roleForm"
        label-width="80px"
      >
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="currentRole.roleName" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色描述" prop="roleDescription">
          <el-input 
            v-model="currentRole.roleDescription" 
            type="textarea" 
            :rows="3"
            placeholder="请输入角色描述"
          />
        </el-form-item>
        <el-form-item label="权限配置" prop="permissions">
          <el-checkbox-group v-model="currentRole.permissions" class="permission-group">
            <div 
              v-for="group in permissionGroups" 
              :key="group.label" 
              class="permission-group-item"
            >
              <div class="group-label">{{ group.label }}</div>
              <el-checkbox 
                v-for="permission in group.permissions" 
                :key="permission.code"
                :label="permission.code"
              >
                {{ permission.name }}
              </el-checkbox>
            </div>
          </el-checkbox-group>
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
import { ref, reactive, onMounted, computed } from 'vue'
import request from '@/utils/requests'

const roles = ref([])
const permissions = ref([])
const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const roleForm = ref(null)

const currentRole = reactive({
  id: '',
  roleName: '',
  roleDescription: '',
  permissions: []
})

const formRules = {
  roleName: [
    { required: true, message: '请输入角色名称', trigger: 'blur' }
  ],
  roleDescription: [
    { required: true, message: '请输入角色描述', trigger: 'blur' }
  ]
}

const permissionGroups = computed(() => {
  const groups = {}
  permissions.value.forEach(p => {
    const prefix = p.code.split(':')[0]
    if (!groups[prefix]) {
      groups[prefix] = {
        label: getGroupLabel(prefix),
        permissions: []
      }
    }
    groups[prefix].permissions.push(p)
  })
  return Object.values(groups)
})

function getGroupLabel(prefix) {
  const labels = {
    user: '用户管理',
    role: '角色管理',
    post: '文章管理',
    comment: '评论管理',
    like: '点赞管理',
    category: '分类管理',
    system: '系统管理'
  }
  return labels[prefix] || prefix
}

function formatTime(time) {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

const fetchRoles = async () => {
  loading.value = true
  try {
    const response = await request({ url: '/api/roles', method: 'get' })
    if (response.data.code === 200) {
      roles.value = response.data.data || []
    } else {
      ElMessage.error(response.data.msg || '获取角色列表失败')
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || '获取角色列表失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

const fetchPermissions = async () => {
  try {
    const response = await request({ url: '/api/roles/permissions/all', method: 'get' })
    if (response.data.code === 200) {
      permissions.value = response.data.data || []
    }
  } catch (error) {
    console.error('获取权限列表失败', error)
  }
}

const handleAddRole = () => {
  dialogTitle.value = '添加角色'
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑角色'
  isEdit.value = true
  Object.assign(currentRole, {
    id: row.id,
    roleName: row.roleName,
    roleDescription: row.roleDescription,
    permissions: row.permissions || []
  })
  dialogVisible.value = true
}

const handleDelete = (row) => {
  ElMessageBox.confirm(
    `确认删除角色 "${row.roleName}" 吗？`,
    '警告',
    {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      const response = await request({ url: `/api/roles/${row.id}`, method: 'delete' })
      if (response.data.code === 200) {
        ElMessage.success('删除成功')
        fetchRoles()
      } else {
        ElMessage.error(response.data.msg || '删除失败')
      }
    } catch (error) {
      ElMessage.error(error.response?.data?.msg || '删除失败')
    }
  }).catch(() => {
    ElMessage.info('已取消删除')
  })
}

const submitForm = () => {
  roleForm.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        const data = {
          roleName: currentRole.roleName,
          roleDescription: currentRole.roleDescription,
          permissions: currentRole.permissions
        }

        let response
        if (isEdit.value) {
          response = await request({ url: `/api/roles/${currentRole.id}`, method: 'put', data })
        } else {
          response = await request({ url: '/api/roles', method: 'post', data })
        }

        if (response.data.code === 200) {
          ElMessage.success(isEdit.value ? '更新成功' : '添加成功')
          dialogVisible.value = false
          fetchRoles()
        } else {
          ElMessage.error(response.data.msg || '操作失败')
        }
      } catch (error) {
        ElMessage.error(error.response?.data?.msg || '操作失败')
      } finally {
        submitLoading.value = false
      }
    }
  })
}

const resetForm = () => {
  Object.assign(currentRole, {
    id: '',
    roleName: '',
    roleDescription: '',
    permissions: []
  })
}

onMounted(() => {
  fetchRoles()
  fetchPermissions()
})
</script>

<style lang="scss" scoped>
.role-list {
  background-color: #fff;
  padding: 20px;
  border-radius: 4px;

  .header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
  }

  .permission-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 4px;
    
    .permission-tag {
      margin: 2px;
    }
  }

  .permission-group {
    width: 100%;
    
    .permission-group-item {
      margin-bottom: 16px;
      
      .group-label {
        font-weight: 500;
        margin-bottom: 8px;
        color: #606266;
        border-bottom: 1px solid #ebeef5;
        padding-bottom: 4px;
      }
      
      .el-checkbox {
        margin-right: 16px;
        margin-bottom: 8px;
      }
    }
  }
}
</style>

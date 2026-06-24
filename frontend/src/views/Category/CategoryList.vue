<template>
  <div class="category-list">
    <div class="header">
      <h2>分类管理</h2>
      <el-button type="primary" @click="handleAddCategory">添加分类</el-button>
    </div>

    <el-table :data="categories" border style="width: 100%" v-loading="loading">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="分类名称" width="200" />
      <el-table-column prop="description" label="分类描述" min-width="300" />
      <el-table-column prop="articleCount" label="文章数量" width="120" align="center">
        <template #default="scope">
          <el-tag type="info">{{ scope.row.articleCount || 0 }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180">
        <template #default="scope">
          {{ formatTime(scope.row.createdAt) }}
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form
        :model="currentCategory"
        :rules="formRules"
        ref="categoryForm"
        label-width="80px"
      >
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="currentCategory.name" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="分类描述" prop="description">
          <el-input 
            v-model="currentCategory.description" 
            type="textarea" 
            :rows="4"
            placeholder="请输入分类描述"
          />
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
import request from '@/utils/requests'

const categories = ref([])
const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const categoryForm = ref(null)

const currentCategory = reactive({
  id: '',
  name: '',
  description: ''
})

const formRules = {
  name: [
    { required: true, message: '请输入分类名称', trigger: 'blur' },
    { min: 2, max: 50, message: '分类名称长度在2-50个字符之间', trigger: 'blur' }
  ],
  description: [
    { max: 200, message: '分类描述不能超过200个字符', trigger: 'blur' }
  ]
}

function formatTime(time) {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

const fetchCategories = async () => {
  loading.value = true
  try {
    const response = await request({ url: '/api/categories', method: 'get' })
    if (response.data.code === 200) {
      categories.value = response.data.data || []
    }
  } catch (error) {
    ElMessage.error('获取分类列表失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleAddCategory = () => {
  dialogTitle.value = '添加分类'
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑分类'
  isEdit.value = true
  Object.assign(currentCategory, {
    id: row.id,
    name: row.name,
    description: row.description || ''
  })
  dialogVisible.value = true
}

const handleDelete = (row) => {
  ElMessageBox.confirm(
    `确认删除分类 "${row.name}" 吗？`,
    '警告',
    {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      const response = await request({ url: `/api/categories/${row.id}`, method: 'delete' })
      if (response.data.code === 200) {
        ElMessage.success('删除成功')
        fetchCategories()
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
  categoryForm.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        const data = {
          name: currentCategory.name,
          description: currentCategory.description
        }

        let response
        if (isEdit.value) {
          response = await request({ url: `/api/categories/${currentCategory.id}`, method: 'put', data })
        } else {
          response = await request({ url: '/api/categories', method: 'post', data })
        }

        if (response.data.code === 200) {
          ElMessage.success(isEdit.value ? '更新成功' : '添加成功')
          dialogVisible.value = false
          fetchCategories()
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
  Object.assign(currentCategory, {
    id: '',
    name: '',
    description: ''
  })
}

onMounted(() => {
  fetchCategories()
})
</script>

<style lang="scss" scoped>
.category-list {
  background-color: #fff;
  padding: 20px;
  border-radius: 4px;

  .header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
  }
}
</style>

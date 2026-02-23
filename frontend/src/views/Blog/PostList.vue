<template>
  <div class="post-list">
    <div class="header">
      <h2>文章管理</h2>
      <!-- 移除添加文章按钮 -->
    </div>

    <!-- 仅保留表格展示结构 -->
    <el-table :data="posts" border style="width: 100%" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" />
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="authId" label="作者ID" width="150" />
      <el-table-column prop="title" label="文章标题" width="200" />
      <el-table-column prop="coverUrl" label="封面" width="200" />
      <el-table-column prop="summary" label="文章摘要" width="250">
        <template #default="scope">
          <el-tooltip :content="scope.row.content" placement="top">
            <div class="summary-content">{{ scope.row.summary }}</div>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column prop="readCount" label="阅读数" width="100" />
      <el-table-column prop="likeCount" label="点赞数" width="100" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'warning'">
            {{ scope.row.status === 1 ? '发布' : '草稿' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180" />
      <el-table-column label="操作" width="240" align="center"> <!-- 注意：宽度需调整为足够容纳3个按钮 -->
        <template v-slot="scope">
          <!-- 使用按钮组包裹，自动横向排列 -->
          <el-button-group>
            <!-- 编辑按钮 -->
            <el-button
                size="small"
                @click="handleEdit(scope.row)"
            >
              编辑
            </el-button>
            <!-- 预览按钮 -->
            <el-button
                size="small"
                @click="handlePreview(scope.row)"
            >
              预览
            </el-button>
            <!-- 删除按钮 -->
            <el-button
                size="small"
                @click="handleDelete(scope.row)"
            >
              删除
            </el-button>
          </el-button-group>
        </template>
      </el-table-column>
    </el-table>

    <!-- 保留分页和批量删除 -->
    <div class="pagination">
      <el-button
          type="danger"
          icon="el-icon-delete"
          @click="handleBatchDelete"
          :disabled="selectedPosts.length === 0 || !hasDeletePermission"
          class="batch-delete-btn"
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
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus' // 引入提示组件
import { pageArticles, deleteArticle } from "@/api/post/post";

// 核心数据：仅保留列表和分页相关
const posts = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false) // 新增：加载状态
const editDialogVisible = ref(false);
const editForm = ref({ id: '', name: '', age: '' });

// 批量操作相关
const selectedPosts = ref([])
const hasDeletePermission = ref(true)

// 封装请求方法（修复参数+解析+异常处理）
const fetchPosts = async () => {
  loading.value = true // 开启加载
  try {
    // 修复1：参数名改为current（和后端分页参数一致）
    const res = await pageArticles({
      current: currentPage.value, // 替换page为current
      size: pageSize.value
    });

    // 打印接口返回的完整数据（调试用）
    console.log('【文章列表接口】完整返回数据：', res);
    // 修复2：解析后端返回的records/total（和实际返回结构一致）
    console.log('【文章列表】数据列表：', res.data?.data?.records);
    console.log('【文章列表】总条数：', res.data?.data?.total);

    // 修复3：正确赋值（兼容空值）
    posts.value = res.data?.data?.records || [];
    total.value = res.data?.data?.total || 0;

  } catch (error) {
    // 修复4：添加用户可见的错误提示
    ElMessage.error('获取文章列表失败，请稍后重试');
    console.error('【文章列表接口】请求失败：', error);
    console.error('错误详情：', error.response || error.message);
    // 兜底：清空列表
    posts.value = [];
    total.value = 0;
  } finally {
    loading.value = false // 关闭加载
  }
};

// 批量操作相关
const handleSelectionChange = (val) => {
  selectedPosts.value = val
}

// 修复5：完善批量删除逻辑
const handleBatchDelete = async () => {
  if (selectedPosts.value.length === 0) {
    ElMessage.warning('请选择要删除的文章');
    return;
  }

  try {
    await ElMessageBox.confirm(
        `确认删除选中的 ${selectedPosts.value.length} 篇文章吗？`,
        '警告',
        {
          confirmButtonText: '确认',
          cancelButtonText: '取消',
          type: 'warning'
        }
    );

    // 批量删除（如果后端支持批量，可优化为一次请求；否则循环删除）
    const deletePromises = selectedPosts.value.map(post => deleteArticle(post.id));
    await Promise.all(deletePromises);

    ElMessage.success('批量删除成功');
    fetchPosts(); // 重新加载列表
  } catch (error) {
    if (error !== 'cancel') { // 排除取消操作的错误
      ElMessage.error('批量删除失败');
      console.error('批量删除错误：', error);
    }
  }
};

// 分页尺寸变化时重新请求
const handleSizeChange = (newSize) => {
  pageSize.value = newSize;
  currentPage.value = 1; // 优化：切换页大小时重置页码为1
  fetchPosts();
};

// 页码变化时重新请求
const handleCurrentChange = (newPage) => {
  currentPage.value = newPage;
  fetchPosts();
};

const handleEdit = (rowData) => {
  // 1. 深拷贝选中行的数据，填充到编辑表单（同步）
  editForm.value = JSON.parse(JSON.stringify(rowData));
  // 2. 显示编辑弹窗（同步）
  editDialogVisible.value = true;
};

// 页面挂载时执行请求
onMounted(() => {
  fetchPosts();
});
</script>

<style lang="scss" scoped>
.post-list {
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
    justify-content: space-between;
    align-items: center;

    .batch-delete-btn {
      margin-right: 20px;
    }
  }

  .summary-content {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}
</style>

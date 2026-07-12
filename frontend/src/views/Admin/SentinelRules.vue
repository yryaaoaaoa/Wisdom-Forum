<template>
  <div class="sentinel-page">
    <div class="page-header">
      <h2>限流规则管理</h2>
      <el-button type="primary" @click="showDialog(null)">
        <el-icon><Plus /></el-icon> 新建规则
      </el-button>
    </div>

    <el-card shadow="never">
      <el-table :data="rules" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="resource" label="资源名称" min-width="180" />
        <el-table-column label="限流类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.grade === 1 ? 'primary' : 'warning'" size="small">
              {{ row.grade === 1 ? 'QPS' : '线程数' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="count" label="阈值" width="80" />
        <el-table-column label="控制行为" width="120">
          <template #default="{ row }">
            {{ behaviorMap[row.controlBehavior] || '快速失败' }}
          </template>
        </el-table-column>
        <el-table-column label="策略" width="80">
          <template #default="{ row }">
            {{ strategyMap[row.strategy] || '直接' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-switch
              :model-value="row.enabled === 1"
              @change="toggleRule(row)"
              size="small"
            />
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" width="170" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="showDialog(row)">编辑</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="editingRule ? '编辑限流规则' : '新建限流规则'"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form :model="form" :rules="formRules" ref="formRef" label-width="110px">
        <el-form-item label="资源名称" prop="resource">
          <el-input v-model="form.resource" placeholder="例如: /api/article/list 或 getUserInfo" />
          <div class="form-tip">接口URL或方法名，支持 Spring Web 资源名</div>
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="限流类型" prop="grade">
              <el-select v-model="form.grade" style="width:100%">
                <el-option :value="1" label="QPS" />
                <el-option :value="0" label="线程数" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="阈值" prop="count">
              <el-input-number v-model="form.count" :min="1" :max="999999" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="控制行为" prop="controlBehavior">
              <el-select v-model="form.controlBehavior" style="width:100%">
                <el-option :value="0" label="快速失败" />
                <el-option :value="1" label="Warm Up" />
                <el-option :value="2" label="排队等待" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="策略" prop="strategy">
              <el-select v-model="form.strategy" style="width:100%">
                <el-option :value="0" label="直接" />
                <el-option :value="1" label="关联" />
                <el-option :value="2" label="链路" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item v-if="form.controlBehavior === 1" label="预热时长(秒)" prop="warmUpPeriodSec">
          <el-input-number v-model="form.warmUpPeriodSec" :min="1" :max="600" style="width:100%" />
        </el-form-item>
        <el-form-item v-if="form.controlBehavior === 2" label="排队超时(毫秒)" prop="maxQueueingTimeMs">
          <el-input-number v-model="form.maxQueueingTimeMs" :min="1" :max="300000" style="width:100%" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getSentinelRules, createSentinelRule, updateSentinelRule, deleteSentinelRule } from '@/api/sentinel/sentinel'

const loading = ref(false)
const rules = ref([])
const dialogVisible = ref(false)
const editingRule = ref(null)
const saving = ref(false)
const formRef = ref(null)

const behaviorMap = { 0: '快速失败', 1: 'Warm Up', 2: '排队等待' }
const strategyMap = { 0: '直接', 1: '关联', 2: '链路' }

const form = ref({
  resource: '',
  grade: 1,
  count: 10,
  limitApp: 'default',
  strategy: 0,
  controlBehavior: 0,
  warmUpPeriodSec: 10,
  maxQueueingTimeMs: 500
})

const formRules = {
  resource: [{ required: true, message: '请输入资源名称', trigger: 'blur' }],
  count: [{ required: true, message: '请输入阈值', trigger: 'blur' }]
}

const fetchRules = async () => {
  loading.value = true
  try {
    const res = await getSentinelRules()
    rules.value = res.data.data || []
  } catch {
    ElMessage.error('获取规则列表失败')
  } finally {
    loading.value = false
  }
}

const showDialog = (rule) => {
  if (rule) {
    editingRule.value = rule
    form.value = {
      resource: rule.resource,
      grade: rule.grade ?? 1,
      count: rule.count ?? 10,
      limitApp: rule.limitApp || 'default',
      strategy: rule.strategy ?? 0,
      controlBehavior: rule.controlBehavior ?? 0,
      warmUpPeriodSec: rule.warmUpPeriodSec ?? 10,
      maxQueueingTimeMs: rule.maxQueueingTimeMs ?? 500
    }
    editingRule.value = rule
  } else {
    editingRule.value = null
    form.value = {
      resource: '',
      grade: 1,
      count: 10,
      limitApp: 'default',
      strategy: 0,
      controlBehavior: 0,
      warmUpPeriodSec: 10,
      maxQueueingTimeMs: 500
    }
  }
  dialogVisible.value = true
}

const handleSave = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    if (editingRule.value) {
      await updateSentinelRule(editingRule.value.id, form.value)
      ElMessage.success('更新成功')
    } else {
      await createSentinelRule(form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await fetchRules()
  } catch {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const handleDelete = async (rule) => {
  try {
    await ElMessageBox.confirm(`确定删除资源「${rule.resource}」的限流规则吗？`, '确认删除')
    await deleteSentinelRule(rule.id)
    ElMessage.success('删除成功')
    await fetchRules()
  } catch {
    // cancelled
  }
}

const toggleRule = async (rule) => {
  try {
    await updateSentinelRule(rule.id, { ...rule, enabled: rule.enabled === 1 ? 0 : 1 })
    ElMessage.success(rule.enabled === 1 ? '已禁用' : '已启用')
    await fetchRules()
  } catch {
    ElMessage.error('操作失败')
  }
}

onMounted(() => {
  fetchRules()
})
</script>

<style lang="scss" scoped>
.sentinel-page {
  padding: 24px;

  .page-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 20px;

    h2 {
      margin: 0;
      font-size: 20px;
      font-weight: 600;
      color: #303133;
    }
  }
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  line-height: 1.4;
}
</style>

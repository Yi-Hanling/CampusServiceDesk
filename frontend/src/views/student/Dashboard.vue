<template>
  <div>
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span>我的工单列表</span>
          <el-button type="primary" @click="$router.push('/student/create')">提交新工单</el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="id" label="编号" width="80" />
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="content" label="问题描述" show-overflow-tooltip />
        <el-table-column prop="createTime" label="提交时间" width="180" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type">
              {{ statusMap[row.status]?.label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button link type="info" @click="showLogs(row.id)">日志</el-button>

            <template v-if="row.status === 2">
              <el-button link type="success" @click="handleConfirm(row.id)">确认完成</el-button>
              <el-button link type="warning" @click="handleReopen(row.id)">申请返工</el-button>
            </template>

            <!-- 注意：这里传的是 row 整行对象 -->
            <template v-if="row.status === 3">
              <el-button link type="primary" @click="openComment(row)">去评价</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 评价弹窗 -->
    <el-dialog v-model="commentVisible" title="工单评价" width="400px">
      <el-form>
        <el-form-item label="评分">
          <el-rate v-model="commentForm.rating" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="commentForm.content" type="textarea" rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="commentVisible = false">取消</el-button>
        <el-button type="primary" @click="submitComment">提交</el-button>
      </template>
    </el-dialog>

    <!-- 日志弹窗 -->
    <el-dialog v-model="logVisible" title="流转日志" width="500px">
      <el-timeline>
        <el-timeline-item v-for="log in logData" :key="log.id" :timestamp="log.createTime" placement="top">
          <p>
            {{ log.remark }}
            (状态: {{ log.oldStatus !== null ? statusMap[log.oldStatus]?.label : '无' }}
            -> {{ statusMap[log.newStatus]?.label }})
          </p>
        </el-timeline-item>
      </el-timeline>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
// 注意：不需要引入 getOrderDetail 了
import { getMyOrders, confirmOrder, reopenOrder, addComment, getOrderLogs } from '@/api/orders'
import { statusMap } from '@/utils/status'
import { ElMessage, ElMessageBox } from 'element-plus'

const tableData = ref([])
const loading = ref(false)
const currentOrderId = ref(null)

// 评价逻辑
const commentVisible = ref(false)
const commentForm = reactive({ rating: 5, content: '' })

// 日志逻辑
const logVisible = ref(false)
const logData = ref([])

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getMyOrders()
    tableData.value = res.data
  } finally {
    loading.value = false
  }
}

const handleConfirm = (id) => {
  ElMessageBox.confirm('确认已维修完成吗？', '提示', { type: 'warning' }).then(async () => {
    await confirmOrder(id)
    ElMessage.success('已确认完成')
    fetchData()
  }).catch(() => {})
}

const handleReopen = (id) => {
  ElMessageBox.confirm('对维修结果不满意？将打回给维修工重新处理。', '申请返工', { type: 'warning' }).then(async () => {
    await reopenOrder(id)
    ElMessage.success('已申请返工')
    fetchData()
  }).catch(() => {})
}

// 点击去评价
const openComment = (row) => {
  if (row.status !== 3) {
    ElMessage.warning('该工单状态已变更，请刷新页面后再试！')
    fetchData()
    return
  }
  currentOrderId.value = row.id
  commentForm.content = ''
  commentVisible.value = true
}

// 提交评价（加了错误捕获）
const submitComment = async () => {
  try {
    await addComment(currentOrderId.value, commentForm.rating, commentForm.content)
    ElMessage.success('评价成功')
    commentVisible.value = false
    fetchData() // 成功后刷新
  } catch (error) {
    // 👇 核心改动：如果提交失败（被 Service 拦截了），关掉弹窗，强制刷新列表！
    commentVisible.value = false
    fetchData()
  }
}

const showLogs = async (id) => {
  const res = await getOrderLogs(id)
  logData.value = res.data
  logVisible.value = true
}

onMounted(fetchData)
</script>


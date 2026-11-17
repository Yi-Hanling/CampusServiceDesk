<template>
  <div>
    <el-row :gutter="20">
      <!-- 左侧：待接单池 -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center;">
              <span style="font-weight: bold; color: #E6A23C;">🔥 待接单大厅</span>
              <el-button size="small" @click="fetchPending">刷新大厅</el-button>
            </div>
          </template>
          <el-table :data="pendingOrders" v-loading="loadingPending" border stripe height="600">
            <el-table-column prop="id" label="编号" width="70" />
            <el-table-column prop="title" label="标题" show-overflow-tooltip />
            <el-table-column prop="createTime" label="发布时间" width="160" />
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button type="warning" size="small" @click="handleGrab(row.id)">立即抢单</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <!-- 右侧：我的任务 -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center;">
              <span style="font-weight: bold; color: #409EFF;">📋 我的任务</span>
              <el-button size="small" @click="fetchMyTasks">刷新任务</el-button>
            </div>
          </template>
          <el-table :data="myOrders" v-loading="loadingMy" border stripe height="600">
            <el-table-column prop="id" label="编号" width="70" />
            <el-table-column prop="title" label="标题" show-overflow-tooltip />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="statusMap[row.status]?.type">{{ statusMap[row.status]?.label }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button link type="info" @click="showLogs(row.id)">日志</el-button>
                <template v-if="row.status === 1">
                  <el-button link type="success" @click="handleComplete(row.id)">提交完工</el-button>
                  <el-button link type="danger" @click="handleRefuse(row.id)">拒绝接单</el-button>
                </template>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 日志弹窗 -->
    <el-dialog v-model="logVisible" title="流转日志" width="500px">
      <el-timeline>
        <el-timeline-item v-for="log in logData" :key="log.id" :timestamp="log.createTime" placement="top">
          <p>
            {{ log.remark }}
            <!-- 👇 这里把状态数字转成了中文 -->
            (状态: {{ log.oldStatus !== null ? statusMap[log.oldStatus]?.label : '无' }}
            -> {{ statusMap[log.newStatus]?.label }})
          </p>
        </el-timeline-item>
      </el-timeline>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getAssignedOrders, getPendingOrders, grabOrder, completeOrder, refuseOrder, getOrderLogs } from '@/api/orders'
import { statusMap } from '@/utils/status'
import { ElMessage, ElMessageBox } from 'element-plus'

// 左侧待接单数据
const pendingOrders = ref([])
const loadingPending = ref(false)

// 右侧我的任务数据
const myOrders = ref([])
const loadingMy = ref(false)

// 日志相关
const logVisible = ref(false)
const logData = ref([])

// 获取大厅待接单列表
const fetchPending = async () => {
  loadingPending.value = true
  try {
    const res = await getPendingOrders()
    pendingOrders.value = res.data
  } finally {
    loadingPending.value = false
  }
}

// 获取我的任务列表
const fetchMyTasks = async () => {
  loadingMy.value = true
  try {
    const res = await getAssignedOrders()
    myOrders.value = res.data
  } finally {
    loadingMy.value = false
  }
}

// 抢单逻辑
const handleGrab = async (id) => {
  try {
    await grabOrder(id)
    ElMessage.success('抢单成功，请尽快处理！')
    // 抢单成功后，两边数据都要刷新
    fetchPending()
    fetchMyTasks()
  } catch (error) {
    // 如果抢单失败（比如被别人抢先了），后端的异常会被 axios 拦截器捕获并弹窗提示
    fetchPending() // 刷新一下列表，把别人抢走的单子刷掉
  }
}

// 提交完工
const handleComplete = (id) => {
  ElMessageBox.confirm('确认已维修完毕？', '提示', { type: 'success' }).then(async () => {
    await completeOrder(id)
    ElMessage.success('已提交完工')
    fetchMyTasks()
  }).catch(() => {})
}

// 拒绝接单
const handleRefuse = (id) => {
  ElMessageBox.confirm('确定拒绝接单吗？工单将退回待分配。', '警告', { type: 'warning' }).then(async () => {
    await refuseOrder(id)
    ElMessage.success('已拒绝，工单已退回大厅')
    fetchMyTasks()
    fetchPending() // 退回了，大厅也要刷新
  }).catch(() => {})
}

// 查看日志
const showLogs = async (id) => {
  const res = await getOrderLogs(id)
  logData.value = res.data
  logVisible.value = true
}

onMounted(() => {
  fetchPending()
  fetchMyTasks()
})
</script>
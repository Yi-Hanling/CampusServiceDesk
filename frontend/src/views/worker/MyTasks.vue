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
            <el-table-column label="发布时间" width="160">
              <template #default="{ row }">
                <div v-if="row.createTime" style="line-height: 1.5; font-size: 13px;">
                  <div>{{ row.createTime.split('T')[0] }}</div>
                  <div style="color: #909399;">{{ row.createTime.split('T')[1]?.substring(0, 8) }}</div>
                </div>
              </template>
            </el-table-column>
            <!-- 操作列宽度稍微加大，容纳详情和抢单按钮 -->
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <!-- 👇 左侧详情按钮 -->
                <el-button link type="info" @click="showDetail(row)">详情</el-button>
                <el-button type="warning" size="small" @click="handleGrab(row.id)">抢单</el-button>
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
            <!-- 操作列宽度加大 -->
            <el-table-column label="操作" width="260" fixed="right">
              <template #default="{ row }">
                <!-- 👇 右侧详情按钮 -->
                <el-button link type="info" @click="showDetail(row)">详情</el-button>
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
            (状态: {{ log.oldStatus !== null ? statusMap[log.oldStatus]?.label : '无' }}
            -> {{ statusMap[log.newStatus]?.label }})
          </p>
        </el-timeline-item>
      </el-timeline>
    </el-dialog>

    <!-- 工单详情弹窗 -->
    <el-dialog v-model="detailVisible" title="工单详细信息" width="600px">
      <el-descriptions border :column="2" v-if="currentDetail" label-width="90px">
        <el-descriptions-item label="工单编号">{{ currentDetail.id }}</el-descriptions-item>
        <el-descriptions-item label="提交时间">
          <span v-if="currentDetail.createTime">
            {{ currentDetail.createTime.split('T')[0] }}
            {{ currentDetail.createTime.split('T')[1]?.substring(0, 8) }}
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="工单标题" :span="2">{{ currentDetail.title }}</el-descriptions-item>
        <el-descriptions-item label="当前状态" :span="2">
          <el-tag :type="statusMap[currentDetail.status]?.type">
            {{ statusMap[currentDetail.status]?.label }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="问题描述" :span="2">
          <div style="white-space: pre-wrap; word-break: break-all; line-height: 1.6; max-height: 200px; overflow-y: auto;">
            {{ currentDetail.content || '无描述' }}
          </div>
        </el-descriptions-item>

        <!-- 👇 新增：故障图片展示 -->
        <el-descriptions-item label="故障图片" :span="2">
          <div v-if="detailImageList.length === 0" style="color: #999;">暂无图片</div>
          <div v-else style="display: flex; flex-wrap: wrap; gap: 10px;">
            <el-image
                v-for="(url, index) in detailImageList"
                :key="index"
                :src="url"
                :preview-src-list="detailImageList"
                style="width: 100px; height: 100px; border-radius: 6px; border: 1px solid #eee;"
                fit="cover"
            />
          </div>
        </el-descriptions-item>

      </el-descriptions>
      <template #footer>
        <el-button type="primary" @click="detailVisible = false">关 闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
// 👇 引入获取图片的接口
import { getAssignedOrders, getPendingOrders, grabOrder, completeOrder, refuseOrder, getOrderLogs, getOrderImages } from '@/api/orders'
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

// 详情弹窗相关逻辑
const detailVisible = ref(false)
const currentDetail = ref(null)
const detailImageList = ref([]) // 👈 新增：详情弹窗专属的图片列表

// 👇 修改：点击详情时，异步获取图片
const showDetail = async (row) => {
  currentDetail.value = row
  detailImageList.value = [] // 先清空上一次的图片
  detailVisible.value = true

  try {
    const res = await getOrderImages(row.id)
    if (res.data && res.data.length > 0) {
      detailImageList.value = res.data.map(url => 'http://localhost:8080' + url)
    }
  } catch (error) {
    console.error("获取详情图片失败", error)
  }
}

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
    fetchPending()
    fetchMyTasks()
  } catch (error) {
    fetchPending()
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
    fetchPending()
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
<template>
  <div>
    <!-- 顶部统计卡片 -->
    <el-row :gutter="20" style="margin-bottom: 20px;">
      <el-col :span="6">
        <el-card shadow="hover">
          <div style="font-size: 14px; color: #909399;">工单总量</div>
          <div style="font-size: 24px; font-weight: bold; color: #409EFF;">{{ stats.total }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div style="font-size: 14px; color: #909399;">待分配</div>
          <div style="font-size: 24px; font-weight: bold; color: #E6A23C;">{{ stats.pending }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div style="font-size: 14px; color: #909399;">处理中</div>
          <div style="font-size: 24px; font-weight: bold; color: #409EFF;">{{ stats.processing }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div style="font-size: 14px; color: #909399;">已完成</div>
          <div style="font-size: 24px; font-weight: bold; color: #67C23A;">{{ stats.completed }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card>
      <template #header>工单管理</template>

      <!-- 筛选区 -->
      <el-form :inline="true" style="margin-bottom: 20px;">
        <el-form-item label="标题搜索">
          <el-input v-model="query.keyword" placeholder="输入关键字" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 150px;">
            <el-option label="待分配" :value="0" />
            <el-option label="处理中" :value="1" />
            <el-option label="待确认" :value="2" />
            <el-option label="已完成" :value="3" />
            <el-option label="已驳回" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
        </el-form-item>
      </el-form>

      <!-- 表格区 -->
      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column prop="id" label="编号" width="80" />
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="creatorId" label="提交人ID" width="100" />
        <el-table-column prop="createTime" label="提交时间" width="180" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type">{{ statusMap[row.status]?.label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button link type="info" @click="showLogs(row.id)">日志</el-button>

            <template v-if="row.status === 0">
              <el-button link type="primary" @click="openAssign(row.id)">分配</el-button>
              <el-button link type="danger" @click="handleReject(row.id)">驳回</el-button>
            </template>

            <template v-if="row.status === 1">
              <el-button link type="warning" @click="openAssign(row.id)">重新分配</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div style="margin-top: 20px; display: flex; justify-content: flex-end;">
        <el-pagination
            v-model:current-page="query.pageNum"
            v-model:page-size="query.pageSize"
            :total="total"
            layout="total, prev, pager, next"
            @current-change="fetchData"
        />
      </div>
    </el-card>

    <!-- 分配弹窗 -->
    <el-dialog v-model="assignVisible" title="分配工单" width="400px">
      <el-form>
        <el-form-item label="选择维修工">
          <el-select v-model="selectedWorkerId" placeholder="请选择维修工">
            <el-option v-for="w in workers" :key="w.id" :label="w.username" :value="w.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAssign">确认分配</el-button>
      </template>
    </el-dialog>

    <!-- 日志弹窗 -->
    <el-dialog v-model="logVisible" title="流转日志" width="500px">
      <el-timeline>
        <el-timeline-item v-for="log in logData" :key="log.id" :timestamp="log.createTime">
          {{ log.remark }} (状态: {{ log.oldStatus ?? '无' }} -> {{ log.newStatus }})
        </el-timeline-item>
      </el-timeline>
    </el-dialog>
  </div>

</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getPageOrders, assignOrder, rejectOrder, getStatistics, getOrderLogs } from '@/api/orders'
import { getWorkers } from '@/api/auth'
import { statusMap } from '@/utils/status'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrderImages } from '@/api/orders'
// 统计数据
const stats = reactive({ total: 0, pending: 0, processing: 0, completed: 0 })

// 表格数据
const tableData = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ pageNum: 1, pageSize: 10, status: null, keyword: '' })

// 分配相关
const assignVisible = ref(false)
const workers = ref([])
const selectedWorkerId = ref(null)
const currentOrderId = ref(null)

// 日志相关
const logVisible = ref(false)
const logData = ref([])

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getPageOrders(query)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const fetchStats = async () => {
  const res = await getStatistics()
  // 解析后端视图返回的数据
  const data = res.data
  stats.total = data.reduce((sum, item) => sum + item.orderCount, 0)
  stats.pending = data.find(i => i.status === 0)?.orderCount || 0
  stats.processing = data.find(i => i.status === 1)?.orderCount || 0
  stats.completed = data.find(i => i.status === 3)?.orderCount || 0
}

const openAssign = async (id) => {
  currentOrderId.value = id
  selectedWorkerId.value = null
  if (workers.value.length === 0) {
    const res = await getWorkers()
    workers.value = res.data
  }
  assignVisible.value = true
}

const submitAssign = async () => {
  if (!selectedWorkerId.value) return ElMessage.warning('请选择维修工')
  await assignOrder(currentOrderId.value, selectedWorkerId.value)
  ElMessage.success('分配成功')
  assignVisible.value = false
  fetchData()
  fetchStats()
}

const handleReject = (id) => {
  ElMessageBox.confirm('确定驳回此工单吗？', '提示', { type: 'warning' }).then(async () => {
    await rejectOrder(id)
    ElMessage.success('已驳回')
    fetchData()
    fetchStats()
  }).catch(() => {})
}

const showLogs = async (id) => {
  const res = await getOrderLogs(id)
  logData.value = res.data
  logVisible.value = true
}

onMounted(() => {
  fetchData()
  fetchStats()
})

const imageVisible = ref(false)
const imageList = ref([])

const showImages = async (id) => {
  const res = await getOrderImages(id)
  if (res.data.length === 0) return ElMessage.info('该工单暂无图片')
  imageList.value = res.data.map(url => 'http://localhost:8080' + url)
  imageVisible.value = true
}
</script>
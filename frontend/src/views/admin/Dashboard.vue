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
        <el-table-column label="提交时间" width="160">
          <template #default="{ row }">
            <div v-if="row.createTime" style="line-height: 1.5; font-size: 13px;">
              <div>{{ row.createTime.split('T')[0] }}</div>
              <div style="color: #909399;">{{ row.createTime.split('T')[1]?.substring(0, 8) }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type">{{ statusMap[row.status]?.label }}</el-tag>
          </template>
        </el-table-column>
        <!-- 操作列宽度调整，去掉了单独的看图按钮 -->
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="showDetail(row)">详情</el-button>
            <el-button link type="info" @click="showLogs(row.id)">日志</el-button>

            <template v-if="row.status === 0">
              <el-button link type="success" @click="openAssign(row.id)">分配</el-button>
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
import { ref, reactive, onMounted } from 'vue'
import { getPageOrders, assignOrder, rejectOrder, getStatistics, getOrderLogs, getOrderImages } from '@/api/orders'
import { getWorkers } from '@/api/auth'
import { statusMap } from '@/utils/status'
import { ElMessage, ElMessageBox } from 'element-plus'

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

// 详情弹窗相关
const detailVisible = ref(false)
const currentDetail = ref(null)
const detailImageList = ref([]) // 详情里的图片列表

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

// 👇 修改：显示工单详情时，同时拉取图片
const showDetail = async (row) => {
  currentDetail.value = row
  detailImageList.value = []
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

onMounted(() => {
  fetchData()
  fetchStats()
})
</script>
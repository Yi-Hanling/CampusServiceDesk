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

        <!-- 修改：使用CSS限制最多显示两行，多出省略号 -->
        <el-table-column prop="content" label="问题描述">
          <template #default="{ row }">
            <span style="display: -webkit-box; -webkit-box-orient: vertical; -webkit-line-clamp: 2; overflow: hidden; word-break: break-all;">
              {{ row.content }}
            </span>
          </template>
        </el-table-column>

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
            <el-tag :type="statusMap[row.status]?.type">
              {{ statusMap[row.status]?.label }}
            </el-tag>
          </template>
        </el-table-column>

        <!-- 操作列稍微加宽一点，容纳详情按钮 -->
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <!-- 新增：详情按钮 -->
            <el-button link type="info" @click="showDetail(row)">详情</el-button>
            <el-button link type="info" @click="showLogs(row.id)">日志</el-button>

            <template v-if="row.status === 2">
              <el-button link type="success" @click="handleConfirm(row.id)">确认完成</el-button>
              <el-button link type="warning" @click="handleReopen(row.id)">申请返工</el-button>
            </template>

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

    <!-- 工单详情弹窗 -->
    <el-dialog v-model="detailVisible" title="工单详细信息" width="600px">
      <!-- 👇 修改1：将 column 改为 2，并设置 label-width 防止文字被挤成竖排 -->
      <el-descriptions border :column="2" v-if="currentDetail" label-width="90px">

        <!-- 第一行：左边编号，右边时间 -->
        <el-descriptions-item label="工单编号">{{ currentDetail.id }}</el-descriptions-item>
        <el-descriptions-item label="提交时间">
          <!-- 顺手把时间也美化了，去掉了难看的 T -->
          <span v-if="currentDetail.createTime">
            {{ currentDetail.createTime.split('T')[0] }}
            {{ currentDetail.createTime.split('T')[1]?.substring(0, 8) }}
          </span>
        </el-descriptions-item>

        <!-- 第二行开始：使用 span="2" 让它们独占一行 -->
        <el-descriptions-item label="工单标题" :span="2">{{ currentDetail.title }}</el-descriptions-item>

        <el-descriptions-item label="当前状态" :span="2">
          <el-tag :type="statusMap[currentDetail.status]?.type">
            {{ statusMap[currentDetail.status]?.label }}
          </el-tag>
        </el-descriptions-item>

        <el-descriptions-item label="问题描述" :span="2">
          <!-- 这里的样式能让超长文本自动换行，并支持滚动条 -->
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
import { ref, onMounted, reactive } from 'vue'
// 👇 引入获取图片的接口
import { getMyOrders, confirmOrder, reopenOrder, addComment, getOrderLogs, getOrderImages } from '@/api/orders'
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

// 详情弹窗逻辑
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
      // 拼接完整路径
      detailImageList.value = res.data.map(url => 'http://localhost:8080' + url)
    }
  } catch (error) {
    console.error("获取详情图片失败", error)
  }
}

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

const submitComment = async () => {
  try {
    await addComment(currentOrderId.value, commentForm.rating, commentForm.content)
    ElMessage.success('评价成功')
    commentVisible.value = false
    fetchData()
  } catch (error) {
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
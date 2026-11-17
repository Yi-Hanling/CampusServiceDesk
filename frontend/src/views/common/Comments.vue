<template>
  <el-card>
    <template #header>
      <div style="display: flex; justify-content: space-between; align-items: center;">
        <span style="font-weight: bold;">评价列表</span>
        <el-button size="small" @click="fetchData">刷新</el-button>
      </div>
    </template>

    <el-table :data="tableData" border stripe v-loading="loading">
      <el-table-column prop="workOrderId" label="工单号" width="100" />
      <el-table-column prop="orderTitle" label="工单标题" show-overflow-tooltip />

      <!-- 角色判断：管理员看全部，维修工看学生，学生看维修工 -->
      <el-table-column v-if="userStore.role === 2 || userStore.role === 1" prop="studentName" label="提交学生" width="120" />
      <el-table-column v-if="userStore.role === 2 || userStore.role === 0" prop="workerName" label="维修工" width="120" />

      <el-table-column label="评分" width="160">
        <template #default="{ row }">
          <el-rate v-model="row.rating" disabled />
        </template>
      </el-table-column>
      <el-table-column prop="content" label="评价内容" />
      <el-table-column prop="createTime" label="评价时间" width="180" />
    </el-table>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getComments } from '@/api/orders'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore() // 获取当前登录用户信息
const tableData = ref([])
const loading = ref(false)

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getComments()
    tableData.value = res.data
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)
</script>
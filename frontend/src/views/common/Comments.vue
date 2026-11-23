<template>
  <el-card>
    <template #header>
      <div style="display: flex; justify-content: space-between; align-items: center;">
        <span style="font-weight: bold;">评价列表</span>
        <el-button size="small" @click="fetchData">刷新</el-button>
      </div>
    </template>

    <!-- 👇 新增：三栏查询区 -->
    <el-form :inline="true" style="margin-bottom: 20px;">
      <!-- 第一栏：关键字 -->
      <el-form-item label="关键字">
        <el-input v-model="searchKeyword" placeholder="输入工单号/标题" clearable style="width: 200px;" />
      </el-form-item>

      <!-- 第二栏：角色/人员 -->
      <el-form-item label="人员">
        <el-input v-model="searchPerson" placeholder="查找姓名" clearable style="width: 200px;" />
      </el-form-item>

      <!-- 第三栏：评分 -->
      <el-form-item label="评分">
        <el-select v-model="searchRating" placeholder="全部评分" clearable style="width: 150px;">
          <el-option label="5 星" :value="5" />
          <el-option label="4 星" :value="4" />
          <el-option label="3 星" :value="3" />
          <el-option label="2 星" :value="2" />
          <el-option label="1 星" :value="1" />
        </el-select>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 表格区，注意绑定的数据改为了 filteredData -->
    <el-table :data="filteredData" border stripe v-loading="loading">
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
      <el-table-column label="评价时间" width="160">
        <template #default="{ row }">
          <div v-if="row.createTime" style="line-height: 1.5; font-size: 13px;">
            <div>{{ row.createTime.split('T')[0] }}</div>
            <div style="color: #909399;">{{ row.createTime.split('T')[1]?.substring(0, 8) }}</div>
          </div>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { getComments } from '@/api/orders'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore() // 获取当前登录用户信息
const tableData = ref([])
const loading = ref(false)

// 👇 新增：三栏搜索条件变量
const searchKeyword = ref('') // 对应第一栏：工单号/标题
const searchPerson = ref('')  // 对应第二栏：学生/维修工名字
const searchRating = ref(null) // 对应第三栏：评分

// 👇 新增：计算属性，用于本地过滤数据
const filteredData = computed(() => {
  return tableData.value.filter(item => {
    // 1. 匹配第一栏（关键字）：工单号 或 标题
    const matchKeyword = !searchKeyword.value ||
        String(item.workOrderId).includes(searchKeyword.value) ||
        item.orderTitle?.includes(searchKeyword.value)

    // 2. 匹配第二栏（人员）：学生姓名 或 维修工姓名
    const matchPerson = !searchPerson.value ||
        item.studentName?.includes(searchPerson.value) ||
        item.workerName?.includes(searchPerson.value)

    // 3. 匹配第三栏（评分）
    const matchRating = searchRating.value === null || searchRating.value === '' || item.rating === searchRating.value

    return matchKeyword && matchPerson && matchRating
  })
})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getComments()
    tableData.value = res.data
  } finally {
    loading.value = false
  }
}

// 👇 新增：查询与重置
const handleSearch = () => {
  // 本地过滤，输入时已实时生效，这里只为配合按钮
}

const handleReset = () => {
  searchKeyword.value = ''
  searchPerson.value = ''
  searchRating.value = null
}

onMounted(fetchData)
</script>
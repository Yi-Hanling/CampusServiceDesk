<template>
  <el-card>
    <template #header>
      <div style="display: flex; justify-content: space-between; align-items: center;">
        <span>用户管理</span>
      </div>
    </template>

    <!-- 筛选区 -->
    <el-form :inline="true" style="margin-bottom: 20px;">
      <el-form-item label="账号/姓名">
        <el-input v-model="searchKeyword" placeholder="输入账号或姓名" clearable style="width: 200px;" />
      </el-form-item>
      <el-form-item label="角色">
        <el-select v-model="searchRole" placeholder="全部角色" clearable style="width: 150px;">
          <el-option label="学生" :value="0" />
          <el-option label="维修工" :value="1" />
          <el-option label="管理员" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 表格区 -->
    <el-table :data="filteredData" border stripe v-loading="loading">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="账号(学号)" />
      <el-table-column prop="realName" label="姓名" />
      <el-table-column label="角色" width="120">
        <template #default="{ row }">
          <el-tag :type="roleMap[row.role]?.type">{{ roleMap[row.role]?.label }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="userStatusMap[row.status]?.type">{{ userStatusMap[row.status]?.label }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button
              link
              :type="row.status === 1 ? 'danger' : 'success'"
              @click="toggleStatus(row)"
          >
            {{ row.status === 1 ? '禁用账号' : '启用账号' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { getAllUsers, changeUserStatus } from '@/api/auth'
import { roleMap, userStatusMap } from '@/utils/status'
import { ElMessage } from 'element-plus'

const tableData = ref([])
const loading = ref(false)

// 👇 新增：搜索条件变量
const searchKeyword = ref('')
const searchRole = ref(null)

// 👇 新增：使用 computed 计算属性进行本地过滤
const filteredData = computed(() => {
  return tableData.value.filter(user => {
    // 1. 关键字匹配（账号 或 姓名）
    const keywordMatch = !searchKeyword.value ||
        user.username?.includes(searchKeyword.value) ||
        user.realName?.includes(searchKeyword.value)

    // 2. 角色匹配
    const roleMatch = searchRole.value === null || searchRole.value === '' || user.role === searchRole.value

    return keywordMatch && roleMatch
  })
})

const fetchUsers = async () => {
  loading.value = true
  try {
    const res = await getAllUsers()
    tableData.value = res.data
  } finally {
    loading.value = false
  }
}

// 👇 新增：查询按钮（由于使用了 computed，其实输入时就已经实时过滤了，这里主要是为了形式感和触发刷新）
const handleSearch = () => {
  // 目前是本地过滤，点击查询可以什么都不做，或者强制触发视图更新
}

// 👇 新增：重置按钮
const handleReset = () => {
  searchKeyword.value = ''
  searchRole.value = null
}

const toggleStatus = async (row) => {
  const newStatus = row.status === 1 ? 0 : 1
  await changeUserStatus(row.id, newStatus)
  ElMessage.success('操作成功')
  fetchUsers()
}

onMounted(fetchUsers)
</script>
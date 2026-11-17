<template>
  <el-card>
    <template #header>用户管理</template>
    <el-table :data="tableData" border stripe v-loading="loading">
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
      <el-table-column label="操作" width="150">
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
import { ref, onMounted } from 'vue'
import { getAllUsers, changeUserStatus } from '@/api/auth'
import { roleMap, userStatusMap } from '@/utils/status'
import { ElMessage } from 'element-plus'

const tableData = ref([])
const loading = ref(false)

const fetchUsers = async () => {
  loading.value = true
  try {
    const res = await getAllUsers()
    tableData.value = res.data
  } finally {
    loading.value = false
  }
}

const toggleStatus = async (row) => {
  const newStatus = row.status === 1 ? 0 : 1
  await changeUserStatus(row.id, newStatus)
  ElMessage.success('操作成功')
  fetchUsers()
}

onMounted(fetchUsers)
</script>
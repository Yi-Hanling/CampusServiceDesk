<template>
  <div class="login-container">
    <el-card class="login-card">
      <h2 style="text-align: center; margin-bottom: 20px;">校园安全工单系统</h2>
      <el-form :model="form">
        <el-form-item>
          <el-input v-model="form.username" placeholder="请输入学号" prefix-icon="User" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password prefix-icon="Lock" />
        </el-form-item>
        <el-button type="primary" style="width: 100%;" @click="handleLogin" :loading="loading">登 录</el-button>
      </el-form>

      <div style="margin-top: 15px; text-align: right; font-size: 14px;">
        <span style="color: #666;">还没有账号？</span>
        <el-link type="primary" @click="$router.push('/register')">去注册</el-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { login } from '@/api/auth'
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const form = reactive({ username: '', password: '' })
const loading = ref(false)
const userStore = useUserStore()
const router = useRouter()

const handleLogin = async () => {
  if (!form.username || !form.password) return ElMessage.warning('请输入账号密码')
  loading.value = true
  try {
    const res = await login(form)
    userStore.setUserInfo(res.data)
    ElMessage.success('登录成功')

    // 核心逻辑：后端返回什么角色，前端就跳什么页面
    if (res.data.role === 0) router.push('/student/home')
    else if (res.data.role === 1) router.push('/worker/tasks')
    else if (res.data.role === 2) router.push('/admin/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container { height: 100vh; display: flex; justify-content: center; align-items: center; background-color: #2d3a4b; }
.login-card { width: 400px; padding: 20px; border-radius: 8px; }
</style>
<template>
  <div class="login-container">
    <el-card class="login-card">
      <h2 style="text-align: center; margin-bottom: 20px;">学生注册</h2>
      <el-form :model="form">
        <el-form-item>
          <!-- 使用 username 字段接收学号 -->
          <el-input v-model="form.username" placeholder="请输入学号 (作为登录账号)" prefix-icon="Postcard" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.realName" placeholder="请输入真实姓名" prefix-icon="User" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="设置密码" show-password prefix-icon="Lock" />
        </el-form-item>
        <el-button type="success" style="width: 100%;" @click="handleRegister" :loading="loading">注 册</el-button>
      </el-form>
      <div style="margin-top: 15px; text-align: right; font-size: 14px;">
        <el-link type="primary" @click="$router.push('/login')">返回登录</el-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { register } from '@/api/auth'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const form = reactive({ username: '', realName: '', password: '' })
const loading = ref(false)
const router = useRouter()

const handleRegister = async () => {
  if (!form.username || !form.realName || !form.password) {
    return ElMessage.warning('请填写完整信息')
  }
  loading.value = true
  try {
    // 直接把学号作为 username 传给后端
    await register(form)
    ElMessage.success('注册成功，请使用学号登录')
    router.push('/login')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container { height: 100vh; display: flex; justify-content: center; align-items: center; background-color: #2d3a4b; }
.login-card { width: 400px; padding: 20px; border-radius: 8px; }
</style>
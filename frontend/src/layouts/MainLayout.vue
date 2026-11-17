<template>
  <el-container class="layout-container">
    <!-- 侧边栏 -->
    <el-aside width="200px" style="background-color: #304156;">
      <div class="logo">校园工单系统</div>
      <el-menu
          :default-active="$route.path"
          router
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#409EFF"
      >
        <template v-if="userStore.role === 0">
          <el-menu-item index="/student/home">我的工单</el-menu-item>
          <el-menu-item index="/student/create">提交新工单</el-menu-item>
          <el-menu-item index="/comments">查看评价</el-menu-item>
        </template>
        <template v-if="userStore.role === 1">
          <el-menu-item index="/worker/tasks">我的维修任务</el-menu-item>
          <el-menu-item index="/comments">查看评价</el-menu-item>
        </template>
        <template v-if="userStore.role === 2">
          <el-menu-item index="/admin/dashboard">工单管理</el-menu-item>
          <el-menu-item index="/admin/users">用户管理</el-menu-item>
          <el-menu-item index="/comments">查看评价</el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <el-container>
      <!-- 顶栏 -->
      <el-header class="header">
        <div class="header-right">
          <span>欢迎您，{{ userStore.realName }}</span>
          <el-button type="danger" size="small" @click="handleLogout" style="margin-left: 20px;">退出登录</el-button>
        </div>
      </el-header>

      <!-- 内容区 -->
      <el-main>
        <router-view></router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'

const userStore = useUserStore()
const router = useRouter()

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout-container { height: 100vh; }
.logo { height: 60px; line-height: 60px; text-align: center; color: white; font-size: 18px; font-weight: bold; }
.header { background-color: white; display: flex; align-items: center; justify-content: flex-end; border-bottom: 1px solid #ccc; }
</style>
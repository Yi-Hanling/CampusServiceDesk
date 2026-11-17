<template>
  <el-card style="max-width: 600px; margin: 0 auto;">
    <template #header>提交新工单</template>
    <el-form :model="form" label-width="80px">
      <el-form-item label="工单标题">
        <el-input v-model="form.title" placeholder="例如：宿舍水管破裂" maxlength="50" show-word-limit />
      </el-form-item>
      <el-form-item label="问题描述">
        <el-input v-model="form.content" type="textarea" rows="5" placeholder="请详细描述故障情况... (选填)" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSubmit" :loading="loading">提交工单</el-button>
        <el-button @click="$router.back()">取消</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { createOrder } from '@/api/orders'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const form = reactive({ title: '', content: '' })
const loading = ref(false)
const router = useRouter()

const handleSubmit = async () => {
  // 只校验标题必填
  if (!form.title) {
    return ElMessage.warning('请填写工单标题')
  }

  loading.value = true
  try {
    // 如果 content 为空，传一个空字符串，防止后端报 null 错误
    await createOrder({
      title: form.title,
      content: form.content || ''
    })
    ElMessage.success('工单提交成功')
    router.push('/student/home')
  } finally {
    loading.value = false
  }
}
</script>
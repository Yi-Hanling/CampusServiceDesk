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

      <!-- 👇 新增：图片上传组件 -->
      <el-form-item label="故障照片">
        <el-upload
            action="#"
            list-type="picture-card"
            :auto-upload="false"
            :limit="3"
            v-model:file-list="fileList"
            accept="image/*"
        >
          <el-icon><Plus /></el-icon>
        </el-upload>
        <div style="font-size: 12px; color: #909399; width: 100%;">最多上传 3 张图片 (选填)</div>
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
import { createOrder, uploadOrderImage } from '@/api/orders' // 👈 引入上传接口
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue' // 👈 引入图标

const form = reactive({ title: '', content: '' })
const fileList = ref([]) // 👈 用来存放用户选中的图片
const loading = ref(false)
const router = useRouter()

const handleSubmit = async () => {
  if (!form.title) {
    return ElMessage.warning('请填写工单标题')
  }

  loading.value = true
  try {
    // 1. 先提交文字，创建工单，拿到工单 ID
    const res = await createOrder({
      title: form.title,
      content: form.content || ''
    })

    const newOrderId = res.data.id // 获取刚刚创建的工单 ID

    // 2. 如果用户选了图片，循环上传图片
    if (fileList.value.length > 0) {
      // 使用 Promise.all 并发上传所有图片
      const uploadPromises = fileList.value.map(item => {
        const formData = new FormData()
        formData.append('file', item.raw) // item.raw 是真实的文件对象
        return uploadOrderImage(newOrderId, formData)
      })

      await Promise.all(uploadPromises)
    }

    ElMessage.success('工单及图片提交成功')
    router.push('/student/home')
  } catch (error) {
    ElMessage.error('提交失败，请重试')
  } finally {
    loading.value = false
  }
}
</script>
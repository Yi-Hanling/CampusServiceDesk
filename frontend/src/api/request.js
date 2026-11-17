import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request = axios.create({
    baseURL: '/api',
    timeout: 10000
})

// 请求拦截器：自动带 Token
request.interceptors.request.use(config => {
    const token = localStorage.getItem('token')
    if (token) {
        config.headers['Authorization'] = 'Bearer ' + token
    }
    return config
})

// 响应拦截器：统一处理报错（这就是后端严格写中文报错的作用）
request.interceptors.response.use(
    response => {
        const res = response.data
        if (res.code !== 200) {
            ElMessage.error(res.message || '系统错误')
            if (res.code === 401) {
                localStorage.clear()
                router.push('/login')
            }
            return Promise.reject(new Error(res.message))
        }
        return res
    },
    error => {
        ElMessage.error(error.message || '网络请求失败')
        return Promise.reject(error)
    }
)

export default request
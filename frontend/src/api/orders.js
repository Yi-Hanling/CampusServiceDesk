import request from './request'

export const createOrder = (data) => request.post('/orders/create', data)
export const getMyOrders = () => request.get('/orders/my')
export const getAssignedOrders = () => request.get('/orders/assigned')
export const getPageOrders = (params) => request.get('/orders/page', { params })
export const getOrderDetail = (id) => request.get(`/orders/${id}/detail`)
export const getOrderLogs = (id) => request.get(`/orders/${id}/logs`)

export const assignOrder = (id, workerId) => request.put(`/orders/${id}/assign?workerId=${workerId}`)
export const rejectOrder = (id) => request.put(`/orders/${id}/reject`)
export const refuseOrder = (id) => request.put(`/orders/${id}/refuse`)
export const completeOrder = (id) => request.put(`/orders/${id}/complete`)
export const confirmOrder = (id) => request.put(`/orders/${id}/confirm`)
export const reopenOrder = (id) => request.put(`/orders/${id}/reopen`) // 👈 补上这个
export const addComment = (id, rating, content) => request.post(`/orders/${id}/comment?rating=${rating}&content=${content}`)
export const getStatistics = () => request.get('/orders/statistics')
export const getPendingOrders = () => request.get('/orders/pending')
export const grabOrder = (id) => request.put(`/orders/${id}/grab`)
// 获取评价列表（后端会自动根据角色返回对应数据）
export const getComments = () => request.get('/orders/comments')
// 上传图片（注意：需要传 FormData 格式）
export const uploadOrderImage = (id, formData) => {
    return request.post(`/orders/${id}/uploadImage`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
    })
}

// 查询工单的图片列表
export const getOrderImages = (id) => request.get(`/orders/${id}/images`)
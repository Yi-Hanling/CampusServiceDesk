import request from './request'

export const login = (data) => request.post('/auth/login', data)
export const register = (data) => request.post('/auth/register', data)
export const getWorkers = () => request.get('/auth/users/workers')
export const getAllUsers = () => request.get('/auth/users/list')
export const changeUserStatus = (id, status) => request.put(`/auth/user/${id}/status?status=${status}`)
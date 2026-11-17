import { createRouter, createWebHistory } from 'vue-router'

const routes = [
    // 👇 新增：注册页面路由
    { path: '/register', component: () => import('@/views/auth/Register.vue') },
    { path: '/login', component: () => import('@/views/auth/Login.vue') },
    {
        path: '/',
        component: () => import('@/layouts/MainLayout.vue'),
        redirect: '/login',
        children: [
            // 学生路由
            { path: 'student/home', component: () => import('@/views/student/Dashboard.vue'), meta: { role: 0 } },
            { path: 'student/create', component: () => import('@/views/student/CreateOrder.vue'), meta: { role: 0 } },
            // 维修工路由
            { path: 'worker/tasks', component: () => import('@/views/worker/MyTasks.vue'), meta: { role: 1 } },
            // 管理员路由
            { path: 'admin/dashboard', component: () => import('@/views/admin/Dashboard.vue'), meta: { role: 2 } },
            { path: 'admin/users', component: () => import('@/views/admin/UserManagement.vue'), meta: { role: 2 } },
            // 👇 把这一行加上！这是所有角色共用的评价页面
            { path: 'comments', component: () => import('@/views/common/Comments.vue') }

        ]
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

// 路由守卫：未登录踢回登录页，角色不匹配踢回各自首页
router.beforeEach((to, from, next) => {
    const token = localStorage.getItem('token')
    const role = Number(localStorage.getItem('role'))

    // 如果是去登录页或注册页，直接放行
    if (to.path === '/login' || to.path === '/register') {
        next()
    } else if (!token) {
        // 没登录且去的不是登录/注册页，踢回登录
        next('/login')
    } else {
        // 如果当前访问的页面有角色限制，且角色不符
        if (to.meta.role !== undefined && to.meta.role !== role) {
            if (role === 0) next('/student/home')
            else if (role === 1) next('/worker/tasks')
            else if (role === 2) next('/admin/dashboard')
            else next('/login')
        } else {
            next()
        }
    }
})

export default router
import { createRouter, createWebHistory } from 'vue-router'

const routes = [
    {
        path: '/',
        redirect: '/blog'
    },
    {
        path: '/blog',
        component: () => import('@/views/Blog/BlogLayout.vue'),
        children: [
            {
                path: '',
                name: 'BlogHome',
                component: () => import('@/views/Blog/BlogHome.vue')
            },
            {
                path: 'article/:id',
                name: 'ArticleDetail',
                component: () => import('@/views/Blog/ArticleDetail.vue')
            },
            {
                path: 'categories',
                name: 'Categories',
                component: () => import('@/views/Blog/Categories.vue')
            },
            {
                path: 'category/:id',
                name: 'CategoryArticles',
                component: () => import('@/views/Blog/CategoryArticles.vue')
            },
            {
                path: 'archives',
                name: 'Archives',
                component: () => import('@/views/Blog/Archives.vue')
            },
            {
                path: 'about',
                name: 'About',
                component: () => import('@/views/Blog/About.vue')
            },
            {
                path: 'notifications',
                name: 'Notifications',
                component: () => import('@/views/Blog/Notifications.vue')
            },
            {
                path: 'profile',
                name: 'Profile',
                component: () => import('@/views/Blog/Profile.vue')
            }
        ]
    },
    {
        path: '/home',
        name: 'Home',
        component: () => import('@/views/Home/home.vue'),
        meta: { requiresAuth: true },
        redirect: '/home/dashboard',
        children: [
            {
                path: 'dashboard',
                name: 'Dashboard',
                component: () => import('@/views/Home/Dashboard.vue'),
                meta: { requiresAuth: true }
            },
            {
                path: '/user/list',
                name: 'UserList',
                component: () => import('@/views/User/UserList.vue'),
                meta: { requiresAuth: true }
            },
            {
                path: '/post/list',
                name: 'PostList',
                component: () => import('@/views/Blog/PostList.vue'),
                meta: { requiresAuth: true }
            },
            {
                path: '/role/list',
                name: 'RoleList',
                component: () => import('@/views/Role/RoleList.vue'),
                meta: { requiresAuth: true }
            },
            {
                path: '/category/list',
                name: 'CategoryList',
                component: () => import('@/views/Category/CategoryList.vue'),
                meta: { requiresAuth: true }
            },
            {
                path: '/settings',
                name: 'Settings',
                component: () => import('@/views/Admin/Settings.vue'),
                meta: { requiresAuth: true }
            },
        ]
    },
    {
        path: '/login',
        name: 'Login',
        component: () => import('@/views/login/Auth.vue'),
        meta: { guest: true }
    },
    {
        path: '/register',
        name: 'Register',
        component: () => import('@/views/login/Auth.vue'),
        meta: { guest: true }
    },
    {
        path: '/forget-password',
        name: 'ForgetPassword',
        component: () => import('@/views/login/Auth.vue'),
        meta: { guest: true }
    },
    {
        path: '/reset-password',
        name: 'ResetPassword',
        component: () => import('@/views/login/ResetPassword.vue'),
        meta: { guest: true }
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

router.beforeEach((to, from, next) => {
    const token = localStorage.getItem('access_token')

    if (to.meta.requiresAuth && !token) {
        next('/login')
    } else if (to.meta.guest && token) {
        next('/home')
    } else {
        next()
    }
})

export default router

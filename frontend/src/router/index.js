// src/router/index.js
import { createRouter, createWebHistory } from 'vue-router'
import Login from '@/views/login/login.vue'
import Home from "@/views/Home/home.vue"
import UserList from '@/views/User/UserList.vue'
import PostList from "@/views/Blog/PostList.vue";

const routes = [
    {
        path: '/',
        redirect: '/login'  // 改为重定向到登录页
    },
    {
        path: '/home',
        name: 'Home',
        component: Home,
        children: [
            {
                path: '/user/list',
                name: 'UserList',
                component: UserList
            },
            {
                path: '/post/list',
                name: 'PostList',
                component: PostList
            }
        ]
    },
    {
        path: '/login',
        name: 'Login',
        component: Login
    }
]


// 创建路由实例
const router = createRouter({
    history: createWebHistory(),
    routes
})

export default router

// src/store/permission.js
import { defineStore } from 'pinia'

export const usePermissionStore = defineStore('permission', {
    state: () => ({
        routes: [] // 存储用户有权限访问的路由
    }),

    getters: {
        // 获取权限路由
        permissionRoutes: (state) => state.routes
    },

    actions: {
        // 设置路由权限
        setRoutes(routes) {
            this.routes = routes
        },


        // 清空路由权限
        clearRoutes() {
            this.routes = []
        },

        // 添加单个路由
        addRoute(route) {
            if (!this.routes.some(r => r.path === route.path)) {
                this.routes.push(route)
            }
        }
    }
})

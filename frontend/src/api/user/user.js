// src/api/user/user.js
import request from '@/utils/requests'

/**
 * 用户管理 API
 */

// 获取用户列表（分页）
export function getUsers(params) {
    return request({
        url: '/api/users',
        method: 'get',
        params
    })
}

// 创建新用户
export function createUser(data) {
    return request({
        url: '/api/users',
        method: 'post',
        data
    })
}

// 更新用户信息
export function updateUser(id, data) {
    return request({
        url: `/api/users/${id}`,
        method: 'put',
        data
    })
}

// 删除用户
export function deleteUser(id) {
    return request({
        url: `/api/users/${id}`,
        method: 'delete'
    })
}

// 重置当前用户密码
export function resetPassword(data) {
    return request({
        url: '/api/users/me/password',
        method: 'put',
        data
    })
}

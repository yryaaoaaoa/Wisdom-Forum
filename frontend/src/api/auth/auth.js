// api/auth/auth.js

import request from '@/utils/requests'

/**
 * 用户登录接口
 * @param {Object} data - 登录数据
 * @param {string} data.username - 用户名
 * @param {string} data.password - 密码
 * @param {string} data.captchaCode - 验证码
 * @param {string} data.captchaKey - 验证码key
 * @returns {Promise} 登录响应
 */
export function login(data) {
    return request({
        url: '/auth/login',
        method: 'post',
        data
    })
}

/**
 * 用户登出接口
 * @param {string} refreshToken - 刷新令牌
 * @returns {Promise} 登出响应
 */
export function logout(refreshToken) {
    return request({
        url: '/auth/logout',
        method: 'post',
        headers: {
            'Authorization': `Bearer ${refreshToken}`
        }
    })
}

/**
 * 获取验证码接口
 * @returns {Promise} 验证码响应
 */
export function getCaptcha() {
    return request({
        url: '/auth/captcha',
        method: 'get'
    })
}

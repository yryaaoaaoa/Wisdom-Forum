import axios from 'axios'
import router from '@/router' // 假设你有路由实例，用于跳转登录页
import { ElMessage } from 'element-plus' // 可选，用于提示用户

const instance = axios.create({
    baseURL: '',
    timeout: 5000,
    headers: {
        'Content-Type': 'application/json'
    }
})

// 用于防止并发请求时重复刷新Token的防抖锁
let isRefreshing = false
// 存储等待刷新Token后重试的请求队列
let requestQueue = []

/**
 * 解析JWT Token，判断是否过期
 * @param {string} token - JWT Token字符串
 * @returns {boolean} 是否过期
 */
function isTokenExpired(token) {
    try {
        if (!token) return true; // 无Token直接视为过期
        const payload = JSON.parse(atob(token.split('.')[1]));
        return Date.now() >= payload.exp * 1000;
    } catch (e) {
        console.error('解析Token失败：', e);
        return true;
    }
}

/**
 * 刷新Token的请求方法
 * @param {string} refreshToken - 刷新令牌
 * @returns {Promise<Object>} 新的token信息 {access_token, refresh_token}
 */
async function refreshTokenRequest(refreshToken) {
    try {
        // 注意：这里要用原生axios，避免触发拦截器导致循环调用
        const response = await axios.post('/auth/refresh', {
            refresh_token: refreshToken
        }, {
            baseURL: '', // 填写你的接口域名，和instance保持一致
            timeout: 5000,
            headers: {
                'Content-Type': 'application/json'
            }
        });
        return response.data; // 假设返回格式：{access_token: 'xxx', refresh_token: 'xxx'}
    } catch (error) {
        // 刷新Token失败，直接抛出错误
        throw new Error('刷新Token失败，请重新登录');
    }
}

// 添加请求拦截器，自动添加认证头
instance.interceptors.request.use(
    async config => {
        const noAuthUrls = ['/auth/login', '/auth/captcha'];
        const requiresAuth = !noAuthUrls.some(url => config.url.includes(url));

        if (requiresAuth) {
            const accessToken = localStorage.getItem('access_token');

            // 1. access_token有效，直接添加到请求头
            if (accessToken && !isTokenExpired(accessToken)) {
                config.headers.Authorization = `Bearer ${accessToken}`;
            }
            // 2. access_token过期，但有可用的refresh_token，先刷新Token
            else {
                const refreshToken = localStorage.getItem('refresh_token');
                if (refreshToken && !isTokenExpired(refreshToken)) {
                    // 如果正在刷新Token，将当前请求加入队列等待
                    if (isRefreshing) {
                        return new Promise((resolve) => {
                            requestQueue.push((newToken) => {
                                config.headers.Authorization = `Bearer ${newToken}`;
                                resolve(config);
                            });
                        });
                    }

                    isRefreshing = true; // 加锁，防止重复刷新
                    try {
                        // 发起刷新Token请求
                        const tokenData = await refreshTokenRequest(refreshToken);
                        // 保存新的Token
                        localStorage.setItem('access_token', tokenData.access_token);
                        localStorage.setItem('refresh_token', tokenData.refresh_token);
                        // 给当前请求添加新Token
                        config.headers.Authorization = `Bearer ${tokenData.access_token}`;
                        // 执行队列中的所有请求
                        requestQueue.forEach(callback => callback(tokenData.access_token));
                        requestQueue = []; // 清空队列
                        return config;
                    } catch (error) {
                        // 刷新失败，清空Token并跳转登录
                        localStorage.removeItem('access_token');
                        localStorage.removeItem('refresh_token');
                        ElMessage.error(error.message);
                        router.push('/login'); // 跳转到登录页
                        return Promise.reject(new Error('Token刷新失败'));
                    } finally {
                        isRefreshing = false; // 解锁
                    }
                }
                // 3. 无可用Token，阻止请求并跳转登录
                else {
                    ElMessage.warning('登录状态已过期，请重新登录');
                    localStorage.removeItem('access_token');
                    localStorage.removeItem('refresh_token');
                    router.push('/login');
                    return Promise.reject(new Error('无有效登录凭证'));
                }
            }
        }
        return config;
    },
    error => {
        return Promise.reject(error);
    }
);

// 添加响应拦截器
instance.interceptors.response.use(
    response => {
        return response
    },
    error => {
        // 处理401错误（兜底：比如refresh_token也过期的情况）
        if (error.response && error.response.status === 401) {
            localStorage.removeItem('access_token'); // 修正命名不一致问题
            localStorage.removeItem('refresh_token');
            ElMessage.error('登录已过期，请重新登录');
            // 避免重复跳转登录页
            if (router.currentRoute.path !== '/login') {
                router.push('/login');
            }
        }
        return Promise.reject(error);
    }
);

// 导出封装后的请求函数
export default function request(config) {
    return instance(config)
}

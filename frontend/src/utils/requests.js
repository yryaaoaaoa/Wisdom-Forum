import axios from 'axios'
import router from '@/router'

const instance = axios.create({
    baseURL: '',
    timeout: 5000,
    headers: {
        'Content-Type': 'application/json'
    }
})

let isRefreshing = false
let requestQueue = []

function isTokenExpired(token) {
    try {
        if (!token) return true
        const payload = JSON.parse(atob(token.split('.')[1]));
        return Date.now() >= payload.exp * 1000;
    } catch (e) {
        console.error('解析Token失败：', e);
        return true;
    }
}

async function refreshTokenRequest(refreshToken) {
    try {
        const response = await axios.post('/auth/refresh', {
            refresh_token: refreshToken
        }, {
            baseURL: '',
            timeout: 5000,
            headers: {
                'Content-Type': 'application/json'
            }
        });
        return response.data.data;
    } catch (error) {
        throw new Error('刷新Token失败');
    }
}

function isAdminPage() {
    return router.currentRoute.value.path.startsWith('/home') ||
        router.currentRoute.value.path.startsWith('/user') ||
        router.currentRoute.value.path.startsWith('/post') ||
        router.currentRoute.value.path.startsWith('/role') ||
        router.currentRoute.value.path.startsWith('/category') ||
        router.currentRoute.value.path.startsWith('/settings')
}

instance.interceptors.request.use(
    async config => {
        const accessToken = localStorage.getItem('access_token');

        if (accessToken && !isTokenExpired(accessToken)) {
            config.headers.Authorization = `Bearer ${accessToken}`;
        } else {
            const refreshToken = localStorage.getItem('refresh_token');
            if (refreshToken && !isTokenExpired(refreshToken)) {
                if (isRefreshing) {
                    return new Promise((resolve) => {
                        requestQueue.push((newToken) => {
                            config.headers.Authorization = `Bearer ${newToken}`;
                            resolve(config);
                        });
                    });
                }

                isRefreshing = true;
                try {
                    const tokenData = await refreshTokenRequest(refreshToken);
                    localStorage.setItem('access_token', tokenData.access_token);
                    localStorage.setItem('refresh_token', tokenData.refresh_token);
                    config.headers.Authorization = `Bearer ${tokenData.access_token}`;
                    requestQueue.forEach(callback => callback(tokenData.access_token));
                    requestQueue = [];
                } catch (error) {
                    localStorage.removeItem('access_token');
                    localStorage.removeItem('refresh_token');
                    if (isAdminPage()) {
                        ElMessage.error('登录已过期，请重新登录');
                        router.push('/login');
                        return Promise.reject(new Error('Token刷新失败'));
                    }
                } finally {
                    isRefreshing = false;
                }
            } else {
                localStorage.removeItem('access_token');
                localStorage.removeItem('refresh_token');
                if (isAdminPage()) {
                    ElMessage.warning('登录状态已过期，请重新登录');
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

instance.interceptors.response.use(
    response => {
        return response
    },
    error => {
        if (error.response && error.response.status === 401) {
            localStorage.removeItem('access_token');
            localStorage.removeItem('refresh_token');

            if (isAdminPage()) {
                ElMessage.error('登录已过期，请重新登录');
                if (router.currentRoute.value.path !== '/login') {
                    router.push('/login');
                }
            }
        }
        return Promise.reject(error);
    }
);

export default function request(config) {
    return instance(config)
}

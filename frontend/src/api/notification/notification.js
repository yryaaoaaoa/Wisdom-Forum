import request from '@/utils/requests.js'

export const getNotifications = (page = 1, size = 10) => {
    return request({
        url: '/api/notifications/list',
        method: 'get',
        params: { page, size }
    })
}

export const getUnreadCount = () => {
    return request({
        url: '/api/notifications/unread-count',
        method: 'get'
    })
}

export const markAsRead = (id) => {
    return request({
        url: `/api/notifications/read/${id}`,
        method: 'put'
    })
}

export const markAllAsRead = () => {
    return request({
        url: '/api/notifications/read-all',
        method: 'put'
    })
}

export default {
    getNotifications,
    getUnreadCount,
    markAsRead,
    markAllAsRead
}

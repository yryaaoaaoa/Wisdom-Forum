import request from '@/utils/requests.js'

export const getCurrentUser = () => {
    return request({
        url: '/api/users/me',
        method: 'get'
    })
}

export const uploadAvatar = (formData) => {
    return request({
        url: '/api/users/me/avatar',
        method: 'post',
        data: formData,
        headers: {
            'Content-Type': 'multipart/form-data'
        }
    })
}

export const updateUserInfo = (id, data) => {
    return request({
        url: `/api/users/${id}`,
        method: 'put',
        data
    })
}

export const resetPassword = (data) => {
    return request({
        url: '/api/users/me/password',
        method: 'put',
        data
    })
}

export default {
    getCurrentUser,
    uploadAvatar,
    updateUserInfo,
    resetPassword
}

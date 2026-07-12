import request from '@/utils/requests'

export function getSentinelRules() {
    return request({
        url: '/api/sentinel/rules',
        method: 'get'
    })
}

export function createSentinelRule(data) {
    return request({
        url: '/api/sentinel/rules',
        method: 'post',
        data
    })
}

export function updateSentinelRule(id, data) {
    return request({
        url: `/api/sentinel/rules/${id}`,
        method: 'put',
        data
    })
}

export function deleteSentinelRule(id) {
    return request({
        url: `/api/sentinel/rules/${id}`,
        method: 'delete'
    })
}

import request from '@/utils/requests.js'

export const publishArticle = (articleId) => {
    return request({
        url: '/api/articles/publish',
        method: 'post',
        params: { articleId }
    })
}

export const updateArticle = (params) => {
    const { id, articleDTO } = params
    return request({
        url: '/api/articles/update',
        method: 'put',
        params: { id },
        data: articleDTO
    })
}

export const deleteArticle = (articleId) => {
    return request({
        url: `/api/articles/delete/${articleId}`,
        method: 'delete'
    })
}

export const getDraft = () => {
    return request({
        url: '/api/articles/draft',
        method: 'post'
    })
}

export const getArticleById = (id) => {
    return request({
        url: '/api/articles/get',
        method: 'get',
        params: { id }
    })
}

export const pageArticles = (queryDTO) => {
    return request({
        url: '/api/articles/page',
        method: 'post',
        data: queryDTO
    })
}

export const getArticleComments = (articleId) => {
    return request({
        url: '/api/articles/comment/view',
        method: 'get',
        params: { articleId }
    })
}

export const addComment = (commentDTO) => {
    return request({
        url: '/api/articles/comment/add',
        method: 'post',
        data: commentDTO
    })
}

export const deleteComment = (commentDTO) => {
    return request({
        url: '/api/articles/comment/delete',
        method: 'post',
        data: commentDTO
    })
}

export const likeArticleOrComment = (targetId, likeType) => {
    return request({
        url: '/api/articles/like',
        method: 'post',
        params: { targetId, likeType }
    })
}

export const getLikeCount = (targetId, likeType) => {
    return request({
        url: '/api/articles/like/count',
        method: 'get',
        params: { targetId, likeType }
    })
}

export const hasLiked = (targetId, likeType) => {
    return request({
        url: '/api/articles/like/status',
        method: 'get',
        params: { targetId, likeType }
    })
}

export const getStats = () => {
    return request({
        url: '/api/articles/stats',
        method: 'get'
    })
}

export const getHotArticles = (limit = 5) => {
    return request({
        url: '/api/articles/hot',
        method: 'get',
        params: { limit }
    })
}

export const uploadArticleCover = (file, articleId) => {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('articleId', articleId)
    return request({
        url: '/api/articles/minio/cover',
        method: 'post',
        data: formData,
        headers: {
            'Content-Type': 'multipart/form-data'
        }
    })
}

export const uploadArticleImage = (file, articleId) => {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('articleId', articleId)
    return request({
        url: '/api/articles/minio/img',
        method: 'post',
        data: formData,
        headers: {
            'Content-Type': 'multipart/form-data'
        }
    })
}

export const articleSearch = (keyword, page = 1, size = 10) => {
    return request({
        url: '/api/articles/search',
        method: 'get',
        params: { keyword, page, size }
    })
}

export const advancedSearch = (searchDTO) => {
    return request({
        url: '/api/articles/search/advanced',
        method: 'post',
        data: searchDTO
    })
}

export const reindexArticles = () => {
    return request({
        url: '/api/articles/reindex',
        method: 'post'
    })
}

export const getRelatedArticles = (articleId, limit = 5) => {
    return request({
        url: `/api/articles/related/${articleId}`,
        method: 'get',
        params: { limit }
    })
}

export default {
    publishArticle,
    updateArticle,
    deleteArticle,
    getDraft,
    getArticleById,
    pageArticles,
    getArticleComments,
    addComment,
    deleteComment,
    likeArticleOrComment,
    getLikeCount,
    hasLiked,
    getStats,
    getHotArticles,
    uploadArticleCover,
    uploadArticleImage,
    getRelatedArticles,
    articleSearch,
    advancedSearch,
    reindexArticles
}

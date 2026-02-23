// 引入axios请求库（集成之前的Token拦截器）
import request from '@/utils/requests.js' // 替换为你之前封装的带Token的request

// -------------------------- 文章相关接口 --------------------------
/**
 * 发布文章
 * @param {Number} articleId 文章ID
 * @returns {Promise} Promise对象
 */
export const publishArticle = (articleId) => {
    return request({
        url: '/api/articles/publish',
        method: 'post',
        // 修复：@RequestBody 接收对象，不能直接传数字
        data: { articleId }
    })
}

/**
 * 更新文章
 * @param {Object} params 请求参数
 * @param {Number} params.id 文章ID（url参数）
 * @param {Object} params.articleDTO 文章更新数据（请求体）
 * @returns {Promise} Promise对象
 */
export const updateArticle = (params) => {
    const { id, articleDTO } = params
    return request({
        url: '/api/articles/update',
        method: 'put',
        params: { id },
        data: articleDTO
    })
}

/**
 * 删除文章（需校验作者权限）
 * @param {Number} articleId 文章ID
 * @returns {Promise} Promise对象
 */
export const deleteArticle = (articleId) => {
    return request({
        url: '/api/articles/delete',
        method: 'delete',
        // 修复：delete请求传递请求体需要特殊处理（axios默认delete不支持data）
        data: { articleId },
        headers: {
            'Content-Type': 'application/json'
        }
    })
}

/**
 * 获取草稿文章
 * @returns {Promise} Promise对象，返回草稿文章数据
 */
export const getDraft = () => {
    return request({
        url: '/api/articles/draft',
        method: 'post'
    })
}

/**
 * 根据文章ID获取文章详情
 * @param {Number} id 文章ID
 * @returns {Promise} Promise对象，返回文章详情
 */
export const getArticleById = (id) => {
    return request({
        url: '/api/articles/get',
        method: 'get',
        params: { id }
    })
}

/**
 * 分页查询文章列表
 * @param {Object} queryDTO 查询条件（包含分页参数和筛选条件）
 * @returns {Promise} Promise对象，返回分页文章列表
 */
export const pageArticles = (queryDTO) => {
    return request({
        url: '/api/articles/page',
        method: 'post',
        data: queryDTO
    })
}

// -------------------------- 评论相关接口 --------------------------
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

// -------------------------- 点赞相关接口 --------------------------
export const likeArticleOrComment = (targetId, likeType) => {
    return request({
        url: '/api/articles/like',
        method: 'post',
        params: { targetId, likeType }
    })
}

// 导出所有接口
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
    likeArticleOrComment
}

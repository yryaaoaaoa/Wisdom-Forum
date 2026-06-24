import requests from '@/utils/requests'

export function getAllCategories() {
  return requests({
    url: '/api/categories',
    method: 'get'
  })
}

export function getCategoryById(id) {
  return requests({
    url: `/api/categories/${id}`,
    method: 'get'
  })
}

export function getCategoriesByArticleId(articleId) {
  return requests({
    url: `/api/categories/article/${articleId}`,
    method: 'get'
  })
}

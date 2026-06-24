/**
 * 头像 URL 处理工具
 * 统一处理各种格式的头像 URL，确保前端能正确显示
 */

/**
 * 获取可用的头像 URL
 * @param {string} url - 原始头像 URL
 * @returns {string} 处理后的头像 URL
 */
export function getAvatarUrl(url) {
  if (!url) return ''

  // 如果已经是相对路径（后端代理路径），直接返回
  if (url.startsWith('/api/files/')) {
    return url
  }

  // 如果是 MinIO 的完整 URL，转换为后端代理路径
  if (url.includes('localhost:9000') || url.includes('minio')) {
    // 尝试匹配 /avatars/... 格式
    const match = url.match(/\/(avatars\/.+)$/)
    if (match) {
      return '/api/files/' + match[1]
    }

    // 尝试匹配 /bucket/avatars/... 格式
    const bucketMatch = url.match(/\/[^/]+\/(avatars\/.+)$/)
    if (bucketMatch) {
      return '/api/files/' + bucketMatch[1]
    }
  }

  // 其他情况直接返回原 URL
  return url
}

/**
 * 获取完整的头像 URL（带服务器地址）
 * @param {string} url - 原始头像 URL
 * @param {string} baseUrl - API 基础地址
 * @returns {string} 完整的头像 URL
 */
export function getFullAvatarUrl(url, baseUrl = '') {
  const processedUrl = getAvatarUrl(url)

  if (!processedUrl) return ''

  // 如果已经是完整 URL，直接返回
  if (processedUrl.startsWith('http://') || processedUrl.startsWith('https://')) {
    return processedUrl
  }

  // 如果是相对路径，拼接基础地址
  if (baseUrl && processedUrl.startsWith('/')) {
    return baseUrl.replace(/\/$/, '') + processedUrl
  }

  return processedUrl
}

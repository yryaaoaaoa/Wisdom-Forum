package com.yry.blog.myblogcommon.cache.constants;

/**
 * 公共缓存常量定义类
 * 包含通用的缓存配置参数和基础常量
 */
public class CacheConstants {

    // ==================== 本地缓存配置 ====================

    /** 本地缓存最大容量 */
    public static final int LOCAL_CACHE_MAX_SIZE = 1000;

    /** 本地缓存过期时间 - 5分钟 */
    public static final int LOCAL_CACHE_EXPIRE = 300;

    // ==================== 分布式缓存配置 ====================

    /** Redis缓存默认过期时间 - 30分钟 */
    public static final int REDIS_DEFAULT_EXPIRE = 1800;

    // ==================== 缓存键基础前缀 ====================

    /** 缓存键分隔符 */
    public static final String KEY_SEPARATOR = ":";

    /** 缓存键前缀格式 */
    public static final String CACHE_KEY_PATTERN = "%s%s%s";

    /** 文章模块缓存前缀 */
    public static final String CACHE_PREFIX_ARTICLE = "forum:article";
    
    /** 文章列表缓存前缀（不含content） */
    public static final String CACHE_PREFIX_ARTICLE_LIST = "forum:article:list";
    
    /** 文章详情缓存前缀（含content） */
    public static final String CACHE_PREFIX_ARTICLE_DETAIL = "forum:article:detail";

    /** 用户模块缓存前缀 */
    public static final String CACHE_PREFIX_USER = "user";

    /** 评论模块缓存前缀 */
    public static final String CACHE_PREFIX_COMMENT = "comment";

    /** 缓存锁前缀 */
    public static final String CACHE_PREFIX_LOCK = "cache:lock";

    // 分页ID列表过期时间：10分钟
    public static final long PAGE_ID_EXPIRE = 10;

    // 单篇文章过期时间：30分钟
    public static final long ARTICLE_OBJ_EXPIRE = 30;
}

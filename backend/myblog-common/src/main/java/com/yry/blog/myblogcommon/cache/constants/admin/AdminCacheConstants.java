package com.yry.blog.myblogcommon.cache.constants.admin;

import com.yry.blog.myblogcommon.cache.constants.CacheConstants;

/**
 * Admin模块缓存常量定义类
 * 包含用户与认证相关的缓存名称、过期时间等配置常量
 */
public class AdminCacheConstants extends CacheConstants {

    // ==================== 用户缓存名称定义 ====================

    /** 单个用户信息缓存 */
    public static final String USER_INFO_CACHE = "admin:user_info";

    /** 用户分页列表缓存 */
    public static final String USER_PAGE_CACHE = "admin:user_page";

    /** 用户名唯一性检查缓存 */
    public static final String USER_EXIST_CACHE = "admin:user_exist";

    /** 用户名查询缓存 */
    public static final String USER_BY_USERNAME_CACHE = "admin:user_by_username";

    // ==================== 用户缓存过期时间定义(秒) ====================

    /** 用户信息缓存过期时间 - 30分钟 */
    public static final int USER_INFO_EXPIRE = 1800;

    /** 用户分页列表缓存过期时间 - 10分钟 */
    public static final int USER_PAGE_EXPIRE = 600;

    /** 用户存在性检查缓存过期时间 - 5分钟 */
    public static final int USER_EXIST_EXPIRE = 300;

    /** 用户名查询缓存过期时间 - 30分钟 */
    public static final int USER_BY_USERNAME_EXPIRE = 1800;

    // ==================== 登录认证缓存 ====================

    /** 用户登录状态缓存 */
    public static final String USER_LOGIN_STATUS_CACHE = "admin:auth:login_status";

    /** JWT访问令牌缓存 */
    public static final String JWT_ACCESS_TOKEN_CACHE = "admin:auth:access_token";

    /** JWT刷新令牌缓存 */
    public static final String JWT_REFRESH_TOKEN_CACHE = "admin:auth:refresh_token";

    /** 登录失败次数缓存 */
    public static final String LOGIN_FAILED_ATTEMPTS_CACHE = "admin:auth:failed_attempts";

    /** 登录会话信息缓存 */
    public static final String USER_SESSION_CACHE = "admin:auth:session";

    // ==================== 认证缓存过期时间定义(秒) ====================

    /** 登录状态缓存过期时间 - 30分钟 */
    public static final int LOGIN_STATUS_EXPIRE = 1800;

    /** 访问令牌缓存过期时间 - 30分钟 */
    public static final int ACCESS_TOKEN_EXPIRE = 1800;

    /** 刷新令牌缓存过期时间 - 7天 */
    public static final int REFRESH_TOKEN_EXPIRE = 7 * 24 * 60 * 60;

    /** 登录失败次数缓存过期时间 - 10分钟 */
    public static final int LOGIN_FAILED_ATTEMPTS_EXPIRE = 600;

    /** 用户会话缓存过期时间 - 30分钟 */
    public static final int USER_SESSION_EXPIRE = 1800;

    // ==================== 验证码缓存 ====================

    /** 登录验证码缓存 */
    public static final String LOGIN_CAPTCHA_CACHE = "admin:auth:captcha";

    /** 验证码过期时间 - 5分钟 */
    public static final int CAPTCHA_EXPIRE = 300;

    /** 验证码缓存键模板 */
    public static final String LOGIN_CAPTCHA_KEY_TEMPLATE = LOGIN_CAPTCHA_CACHE + KEY_SEPARATOR + "%s";


    // ==================== 密码重置缓存 ====================

    /** 密码重置尝试次数缓存 */
    public static final String PASSWORD_RESET_ATTEMPT_CACHE = "admin:pwd_reset_attempt";

    /** 密码重置冷却时间 - 5分钟 */
    public static final int PASSWORD_RESET_COOLDOWN = 300;

    // ==================== 用户缓存键生成模板 ====================

    /** 用户信息缓存键模板 */
    public static final String USER_INFO_KEY_TEMPLATE = USER_INFO_CACHE + KEY_SEPARATOR + "%s";

    /** 用户分页查询缓存键模板 */
    public static final String USER_PAGE_KEY_TEMPLATE = USER_PAGE_CACHE + KEY_SEPARATOR + "page_%s_size_%s_query_%s";

    /** 用户存在性检查缓存键模板 */
    public static final String USER_EXIST_KEY_TEMPLATE = USER_EXIST_CACHE + KEY_SEPARATOR + "%s";

    /** 用户名查询缓存键模板 */
    public static final String USER_BY_USERNAME_KEY_TEMPLATE = USER_BY_USERNAME_CACHE + KEY_SEPARATOR + "%s";

    // ==================== 认证缓存键生成模板 ====================

    /** 用户登录状态缓存键模板 */
    public static final String USER_LOGIN_STATUS_KEY_TEMPLATE = USER_LOGIN_STATUS_CACHE + KEY_SEPARATOR + "%s";

    /** JWT访问令牌缓存键模板 */
    public static final String JWT_ACCESS_TOKEN_KEY_TEMPLATE = JWT_ACCESS_TOKEN_CACHE + KEY_SEPARATOR + "%s";

    /** JWT刷新令牌缓存键模板 */
    public static final String JWT_REFRESH_TOKEN_KEY_TEMPLATE = JWT_REFRESH_TOKEN_CACHE + KEY_SEPARATOR + "%s";

    /** 登录失败次数缓存键模板 */
    public static final String LOGIN_FAILED_ATTEMPTS_KEY_TEMPLATE =   LOGIN_FAILED_ATTEMPTS_CACHE + KEY_SEPARATOR + "%s";

    /** 用户会话缓存键模板 */
    public static final String USER_SESSION_KEY_TEMPLATE = USER_SESSION_CACHE + KEY_SEPARATOR + "%s";

}

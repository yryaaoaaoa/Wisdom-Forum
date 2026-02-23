package com.yry.blog.myblogcommon.cache.constants.permission;

import com.yry.blog.myblogcommon.cache.constants.CacheConstants;

/**
 * 权限管理模块缓存常量定义类
 * 包含角色、权限相关的缓存名称、过期时间等配置常量
 */
public class PermissionCacheConstants extends CacheConstants {

    // ==================== 角色缓存名称定义 ====================

    /** 单个角色信息缓存 */
    public static final String ROLE_INFO_CACHE = "permission:role_info";

    /** 角色分页列表缓存 */
    public static final String ROLE_PAGE_CACHE = "permission:role_page";

    /** 角色权限列表缓存 */
    public static final String ROLE_PERMISSIONS_CACHE = "permission:role_permissions";

    // ==================== 权限缓存名称定义 ====================

    /** 权限列表缓存 */
    public static final String PERMISSION_LIST_CACHE = "permission:permission_list";

    /** 用户权限缓存 */
    public static final String USER_PERMISSIONS_CACHE = "permission:user_permissions";

    /** 用户角色缓存 */
    public static final String USER_ROLES_CACHE = "permission:user_roles";

    // ==================== 缓存过期时间定义(秒) ====================

    /** 角色信息缓存过期时间 - 30分钟 */
    public static final int ROLE_INFO_EXPIRE = 1800;

    /** 角色权限缓存过期时间 - 30分钟 */
    public static final int ROLE_PERMISSIONS_EXPIRE = 1800;

    /** 用户权限缓存过期时间 - 30分钟 */
    public static final int USER_PERMISSIONS_EXPIRE = 1800;

    /** 用户角色缓存过期时间 - 30分钟 */
    public static final int USER_ROLES_EXPIRE = 1800;

    // ==================== 缓存键生成模板 ====================

    /** 角色信息缓存键模板 */
    public static final String ROLE_INFO_KEY_TEMPLATE = ROLE_INFO_CACHE + KEY_SEPARATOR + "%s";

    /** 角色权限列表缓存键模板 */
    public static final String ROLE_PERMISSIONS_KEY_TEMPLATE = ROLE_PERMISSIONS_CACHE + KEY_SEPARATOR + "%s";

    /** 用户权限缓存键模板 */
    public static final String USER_PERMISSIONS_KEY_TEMPLATE = USER_PERMISSIONS_CACHE + KEY_SEPARATOR + "%s";

    /** 用户角色缓存键模板 */
    public static final String USER_ROLES_KEY_TEMPLATE = USER_ROLES_CACHE + KEY_SEPARATOR + "%s";
}

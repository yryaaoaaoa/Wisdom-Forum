package com.yry.blog.myblogcommon.enums;

/**
 * 权限枚举类
 */
public enum PermissionEnum {
    // 用户管理权限
    USER_VIEW("user:view", "查看用户"),
    USER_CREATE("user:create", "创建用户"),
    USER_UPDATE("user:update", "更新用户"),
    USER_DELETE("user:delete", "删除用户"),

    // 角色管理权限
    ROLE_VIEW("role:view", "查看角色"),
    ROLE_CREATE("role:create", "创建角色"),
    ROLE_UPDATE("role:update", "更新角色"),
    ROLE_DELETE("role:delete", "删除角色"),

    // 文章管理权限
    POST_VIEW("post:view", "查看文章"),
    POST_CREATE("post:create", "创建文章"),
    POST_UPDATE("post:update", "更新文章"),
    POST_DELETE("post:delete", "删除文章"),

    // 评论权限
    COMMENT_VIEW("comment:view", "查看评论"),
    COMMENT_CREATE("comment:create", "创建评论"),
    COMMENT_DELETE("comment:delete", "删除评论"),

    // 点赞权限
    LIKE_CREATE("like:create", "点赞"),
    LIKE_DELETE("like:delete", "取消点赞"),

    // 系统管理权限
    SYSTEM_CONFIG("system:config", "系统配置");

    private final String permission;
    private final String description;

    PermissionEnum(String permission, String description) {
        this.permission = permission;
        this.description = description;
    }

    public String getPermission() {
        return permission;
    }

    public String getDescription() {
        return description;
    }
}

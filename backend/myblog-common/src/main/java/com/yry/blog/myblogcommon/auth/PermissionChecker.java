package com.yry.blog.myblogcommon.auth;

import java.util.List;

public interface PermissionChecker {
    /**
     * 检查当前用户是否为指定资源的所有者
     * @param resourceOwnerId 资源所有者的用户ID
     * @return true 表示是所有者，false 表示不是所有者
     */
    boolean check(Long resourceOwnerId);
    Long getCurrentUserId();
    List<String> getPermissions(Long userId);
}
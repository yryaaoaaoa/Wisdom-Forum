package com.yry.blog.myblogadmin.service;

import com.yry.blog.myblogcommon.entity.Role.Role;
import java.util.List;

/**
 * 角色服务接口
 */
public interface RoleService {

    /**
     * 根据ID获取角色
     */
    Role getRoleById(Long id);

    /**
     * 获取所有角色列表
     */
    List<Role> getAllRoles();

    /**
     * 创建新角色
     */
    Role createRole(Role role);

    /**
     * 更新角色信息
     */
    Role updateRole(Role role);

    /**
     * 删除角色（逻辑删除或物理删除）
     */
    void deleteRole(Long id);
}
package com.yry.blog.myblogauth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yry.blog.myblogauth.dto.PermissionCreateDTO;
import com.yry.blog.myblogauth.dto.PermissionUpdateDTO;
import com.yry.blog.myblogcommon.entity.Permission.Permission;

import java.util.List;

/**
 * 权限服务接口
 */
public interface PermissionService extends IService<Permission> {

    /**
     * 根据用户ID获取权限码列表
     * @param userId 用户ID
     * @return 权限码列表
     */
    List<String> getPermissionCodesByUserId(Long userId);

    /**
     * 根据角色ID获取权限列表
     * @param roleId 角色ID
     * @return 权限列表
     */
    List<Permission> getPermissionsByRoleId(Long roleId);

    /**
     * 根据角色ID列表获取权限列表
     * @param roleIds 角色ID列表
     * @return 权限列表
     */
    List<Permission> getPermissionsByRoleIds(List<Long> roleIds);

    /**
     * 获取所有权限列表
     * @return 所有权限列表
     */
    List<Permission> getAllPermissions();

    /**
     * 根据权限ID获取权限
     * @param id 权限ID
     * @return 权限对象
     */
    Permission getPermissionById(Long id);

    /**
     * 创建新权限
     * @param dto 权限创建参数
     * @return 创建后的权限对象
     */
    Permission createPermission(PermissionCreateDTO dto);

    /**
     * 更新权限信息
     * @param id 权限ID
     * @param dto 权限更新参数
     * @return 更新后的权限对象
     */
    Permission updatePermission(Long id, PermissionUpdateDTO dto);

    /**
     * 删除权限
     * @param id 权限ID
     */
    void deletePermission(Long id);
}
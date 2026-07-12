package com.yry.blog.myblogauth.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yry.blog.myblogauth.dto.PermissionCreateDTO;
import com.yry.blog.myblogauth.dto.PermissionUpdateDTO;
import com.yry.blog.myblogauth.service.PermissionService;
import com.yry.blog.myblogcommon.entity.Permission.Permission;
import com.yry.blog.myblogauth.mapper.PermissionMapper;
import com.yry.blog.myblogauth.mapper.RoleMapper;
import com.yry.blog.myblogauth.mapper.RolePermissionMapper;
import com.yry.blog.myblogauth.mapper.UserAuthMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission> implements PermissionService {

    @Autowired
    private UserAuthMapper userAuthMapper;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private PermissionMapper permissionMapper;

    @Autowired
    private RolePermissionMapper rolePermissionMapper;

    @Override
    public List<String> getPermissionCodesByUserId(Long userId) {
        // 先获取用户的角色ID列表
        List<Long> roleIds = getUserRoleIds(userId);

        // 根据角色ID列表获取权限列表
        List<Permission> code = getPermissionsByRoleIds(roleIds);

        // 提取权限码
        return code.stream()
                .map(Permission::getCode)
                .collect(Collectors.toList());
    }

    /**
     * 获取用户的角色ID列表
     * @param userId 用户ID
     * @return 角色ID列表
     */
    private List<Long> getUserRoleIds(Long userId) {
        // 查询user_role表获取用户的角色ID列表
        return userAuthMapper.selectRoleIdsByUserId(userId);
    }

    @Override
    public List<Permission> getPermissionsByRoleId(Long roleId) {
        List<Long> roleIds = new ArrayList<>();
        roleIds.add(roleId);
        return getPermissionsByRoleIds(roleIds);
    }

    @Override
    public List<Permission> getPermissionsByRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return new ArrayList<>();
        }

        return roleMapper.selectPermissionsByRoleIds(roleIds);
    }

    @Override
    public List<Permission> getAllPermissions() {
        // 查询permission表获取所有权限
        return permissionMapper.selectList(new QueryWrapper<>());
    }

    @Override
    public Permission getPermissionById(Long id) {
        // 根据权限ID获取权限
        return permissionMapper.selectById(id);
    }

    @Override
    public Permission createPermission(PermissionCreateDTO dto) {
        Permission permission = new Permission();
        permission.setCode(dto.getCode());
        permission.setName(dto.getName());
        permissionMapper.insert(permission);
        return permission;
    }

    @Override
    public Permission updatePermission(Long id, PermissionUpdateDTO dto) {
        Permission permission = new Permission();
        permission.setId(id);
        permission.setCode(dto.getCode());
        permission.setName(dto.getName());
        permissionMapper.updateById(permission);
        return permission;
    }

    @Override
    @Transactional
    public void deletePermission(Long id) {
        rolePermissionMapper.deleteByPermissionId(id);
        permissionMapper.deleteById(id);
    }
}
package com.yry.blog.myblogadmin.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yry.blog.myblogadmin.dto.RoleCreateDTO;
import com.yry.blog.myblogadmin.mapper.RoleMapper;
import com.yry.blog.myblogadmin.mapper.RolePermissionMapper;
import com.yry.blog.myblogadmin.mapper.UserRoleMapper;
import com.yry.blog.myblogadmin.service.RoleService;
import com.yry.blog.myblogadmin.vo.RoleVO;
import com.yry.blog.myblogcommon.entity.Role.Role;
import com.yry.blog.myblogcommon.entity.RolePermission.RolePermission;
import com.yry.blog.myblogcommon.entity.UserRole.UserRole;
import com.yry.blog.myblogcommon.enums.ResponseCodeEnums;
import com.yry.blog.myblogcommon.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoleServiceImpl implements RoleService {

    @Autowired
    private RoleMapper roleMapper;
    
    @Autowired
    private RolePermissionMapper rolePermissionMapper;
    
    @Autowired
    private UserRoleMapper userRoleMapper;

    @Override
    public Role getRoleById(Long id) {
        return roleMapper.selectById(id);
    }

    @Override
    public List<RoleVO> getAllRoles() {
        List<Role> roles = roleMapper.selectList(
            new LambdaQueryWrapper<Role>().orderByDesc(Role::getCreateTime)
        );
        
        return roles.stream().map(role -> {
            List<String> permissions = rolePermissionMapper.selectPermissionsByRoleId(role.getId());
            return RoleVO.builder()
                .id(role.getId())
                .roleName(role.getRoleName())
                .roleDescription(role.getRoleDescription())
                .createTime(role.getCreateTime())
                .updateTime(role.getUpdateTime())
                .permissions(permissions)
                .build();
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RoleVO createRole(RoleCreateDTO dto) {
        // 检查角色名是否已存在
        Role existing = roleMapper.selectOne(
            new LambdaQueryWrapper<Role>().eq(Role::getRoleName, dto.getRoleName())
        );
        if (existing != null) {
            throw new BusinessException(ResponseCodeEnums.DATA_EXISTS, "角色名已存在");
        }
        Role role = new Role();
        role.setRoleName(dto.getRoleName());
        role.setRoleDescription(dto.getRoleDescription());
        roleMapper.insert(role);
        
        if (dto.getPermissions() != null && !dto.getPermissions().isEmpty()) {
            assignPermissions(role.getId(), dto.getPermissions());
        }
        
        return getRoleWithPermissions(role.getId());
    }

    @Override
    @Transactional
    public RoleVO updateRole(Long id, RoleCreateDTO dto) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ResponseCodeEnums.NOT_FOUND, "角色不存在");
        }

        // 检查角色名是否已被其他角色使用
        Role existing = roleMapper.selectOne(
            new LambdaQueryWrapper<Role>()
                .eq(Role::getRoleName, dto.getRoleName())
                .ne(Role::getId, id)
        );
        if (existing != null) {
            throw new BusinessException(ResponseCodeEnums.DATA_EXISTS, "角色名已存在");
        }

        role.setRoleName(dto.getRoleName());
        role.setRoleDescription(dto.getRoleDescription());
        roleMapper.updateById(role);
        
        rolePermissionMapper.deleteByRoleId(id);
        if (dto.getPermissions() != null && !dto.getPermissions().isEmpty()) {
            assignPermissions(id, dto.getPermissions());
        }
        
        return getRoleWithPermissions(id);
    }

    @Override
    @Transactional
    public void deleteRole(Long id) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ResponseCodeEnums.NOT_FOUND, "角色不存在");
        }
        
        long userCount = userRoleMapper.selectCount(
            new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, id)
        );
        if (userCount > 0) {
            throw new BusinessException(ResponseCodeEnums.BAD_REQUEST, "该角色已分配给用户，无法删除");
        }
        
        rolePermissionMapper.deleteByRoleId(id);
        roleMapper.deleteById(id);
    }

    @Override
    public RoleVO getRoleWithPermissions(Long id) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ResponseCodeEnums.NOT_FOUND, "角色不存在");
        }
        
        List<String> permissions = rolePermissionMapper.selectPermissionsByRoleId(id);
        return RoleVO.builder()
            .id(role.getId())
            .roleName(role.getRoleName())
            .roleDescription(role.getRoleDescription())
            .createTime(role.getCreateTime())
            .updateTime(role.getUpdateTime())
            .permissions(permissions)
            .build();
    }

    @Override
    @Transactional
    public void assignPermissions(Long roleId, List<String> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return;
        }
        
        List<RolePermission> rolePermissions = permissions.stream()
            .map(permission -> RolePermission.builder()
                .roleId(roleId)
                .permission(permission)
                .build())
            .collect(Collectors.toList());
        
        rolePermissionMapper.insertBatch(rolePermissions);
    }
}

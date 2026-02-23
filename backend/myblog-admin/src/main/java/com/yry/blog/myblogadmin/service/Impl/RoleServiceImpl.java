package com.yry.blog.myblogadmin.service.Impl;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yry.blog.myblogadmin.service.RoleService;
import com.yry.blog.myblogcommon.entity.Role.Role;
import com.yry.blog.myblogadmin.mapper.RoleMapper; // 假设你有这个 Mapper
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@TableName("role")
public class RoleServiceImpl implements RoleService {

    @Autowired
    private RoleMapper roleMapper; // 你可以替换为 JPA Repository 或其他 DAO

    @Override
    public Role getRoleById(Long id) {
        return roleMapper.selectById(id);
    }

    @Override
    public List<Role> getAllRoles() {
        return roleMapper.selectAll();
    }

    @Override
    public Role createRole(Role role) {
        roleMapper.insert(role);
        return role;
    }

    @Override
    public Role updateRole(Role role) {
        roleMapper.updateById(role);
        return role;
    }

    @Override
    public void deleteRole(Long id) {
        // 可选：实现逻辑删除（如设置 deleted 字段）或直接物理删除
        roleMapper.deleteById(id);
    }
}
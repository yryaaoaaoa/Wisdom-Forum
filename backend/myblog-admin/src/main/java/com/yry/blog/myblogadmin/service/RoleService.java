package com.yry.blog.myblogadmin.service;

import com.yry.blog.myblogadmin.dto.RoleCreateDTO;
import com.yry.blog.myblogadmin.vo.RoleVO;
import com.yry.blog.myblogcommon.entity.Role.Role;

import java.util.List;

public interface RoleService {

    Role getRoleById(Long id);

    List<RoleVO> getAllRoles();

    RoleVO createRole(RoleCreateDTO dto);

    RoleVO updateRole(Long id, RoleCreateDTO dto);

    void deleteRole(Long id);

    RoleVO getRoleWithPermissions(Long id);

    void assignPermissions(Long roleId, List<String> permissions);
}

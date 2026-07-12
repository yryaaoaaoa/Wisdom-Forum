package com.yry.blog.myblogadmin.controller;

import com.yry.blog.myblogauth.dto.RoleCreateDTO;
import com.yry.blog.myblogauth.service.PermissionService;
import com.yry.blog.myblogauth.service.RoleService;
import com.yry.blog.myblogauth.vo.RoleVO;
import com.yry.blog.myblogcommon.annotation.RequiresPermission;
import com.yry.blog.myblogcommon.entity.Permission.Permission;
import com.yry.blog.myblogcommon.entity.Role.Role;
import com.yry.blog.myblogcommon.result.Response;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/roles")
public class RoleController {

    private final RoleService roleService;
    private final PermissionService permissionService;

    public RoleController(RoleService roleService, PermissionService permissionService) {
        this.roleService = roleService;
        this.permissionService = permissionService;
    }

    @RequiresPermission("role:view")
    @GetMapping
    public Response<List<RoleVO>> getAllRoles() {
        List<RoleVO> roles = roleService.getAllRoles();
        return Response.success(roles);
    }

    @RequiresPermission("role:view")
    @GetMapping("/{id}")
    public Response<RoleVO> getRoleById(@PathVariable Long id) {
        RoleVO role = roleService.getRoleWithPermissions(id);
        return Response.success(role);
    }

    @RequiresPermission("role:create")
    @PostMapping
    public Response<RoleVO> createRole(@RequestBody RoleCreateDTO dto) {
        RoleVO createdRole = roleService.createRole(dto);
        return Response.success(createdRole);
    }

    @RequiresPermission("role:update")
    @PutMapping("/{id}")
    public Response<RoleVO> updateRole(@PathVariable Long id, @RequestBody RoleCreateDTO dto) {
        RoleVO updatedRole = roleService.updateRole(id, dto);
        return Response.success(updatedRole);
    }

    @RequiresPermission("role:delete")
    @DeleteMapping("/{id}")
    public Response<Object> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return Response.success(id);
    }

    @RequiresPermission("role:view")
    @GetMapping("/{id}/permissions")
    public Response<List<String>> getRolePermissions(@PathVariable Long id) {
        RoleVO role = roleService.getRoleWithPermissions(id);
        return Response.success(role.getPermissions());
    }

    @RequiresPermission("role:update")
    @PostMapping("/{id}/permissions")
    public Response<Object> assignPermissions(@PathVariable Long id, @RequestBody List<String> permissions) {
        roleService.assignPermissions(id, permissions);
        return Response.success(null);
    }

    @RequiresPermission("role:view")
    @GetMapping("/permissions/all")
    public Response<List<Permission>> getAllPermissions() {
        List<Permission> permissions = permissionService.getAllPermissions();
        return Response.success(permissions);
    }
}

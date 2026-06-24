package com.yry.blog.myblogadmin.controller;

import com.yry.blog.myblogcommon.annotation.RequiresPermission;
import com.yry.blog.myblogcommon.entity.Permission.Permission;
import com.yry.blog.myblogcommon.result.Response;
import com.yry.blog.myblogadmin.service.PermissionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/permissions")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @RequiresPermission("permission:view")
    @GetMapping
    public Response<List<Permission>> getAllPermissions() {
        List<Permission> permissions = permissionService.getAllPermissions();
        return Response.success(permissions);
    }

    @RequiresPermission("permission:view")
    @GetMapping("/{id}")
    public Response<Permission> getPermissionById(@PathVariable Long id) {
        Permission permission = permissionService.getPermissionById(id);
        if (permission == null) {
            return Response.error(com.yry.blog.myblogcommon.enums.ResponseCodeEnums.NOT_FOUND, "权限不存在");
        }
        return Response.success(permission);
    }

    @RequiresPermission("permission:create")
    @PostMapping
    public Response<Permission> createPermission(@RequestBody Permission permission) {
        Permission created = permissionService.createPermission(permission);
        return Response.success(created);
    }

    @RequiresPermission("permission:update")
    @PutMapping("/{id}")
    public Response<Permission> updatePermission(@PathVariable Long id, @RequestBody Permission permission) {
        permission.setId(id);
        Permission updated = permissionService.updatePermission(permission);
        return Response.success(updated);
    }

    @RequiresPermission("permission:delete")
    @DeleteMapping("/{id}")
    public Response<Object> deletePermission(@PathVariable Long id) {
        permissionService.deletePermission(id);
        return Response.success(id);
    }
}

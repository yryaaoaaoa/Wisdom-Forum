package com.yry.blog.myblogadmin.controller;

import com.yry.blog.myblogadmin.vo.PermissionVO;
import com.yry.blog.myblogauth.dto.PermissionCreateDTO;
import com.yry.blog.myblogauth.dto.PermissionUpdateDTO;
import com.yry.blog.myblogcommon.annotation.RequiresPermission;
import com.yry.blog.myblogcommon.entity.Permission.Permission;
import com.yry.blog.myblogcommon.enums.ResponseCodeEnums;
import com.yry.blog.myblogcommon.result.Response;
import com.yry.blog.myblogauth.service.PermissionService;
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
    public Response<List<PermissionVO>> getAllPermissions() {
        List<Permission> permissions = permissionService.getAllPermissions();
        List<PermissionVO> vos = permissions.stream().map(this::toVO).toList();
        return Response.success(vos);
    }

    @RequiresPermission("permission:view")
    @GetMapping("/{id}")
    public Response<PermissionVO> getPermissionById(@PathVariable Long id) {
        Permission permission = permissionService.getPermissionById(id);
        if (permission == null) {
            return Response.error(ResponseCodeEnums.NOT_FOUND, "权限不存在");
        }
        return Response.success(toVO(permission));
    }

    @RequiresPermission("permission:create")
    @PostMapping
    public Response<PermissionVO> createPermission(@RequestBody PermissionCreateDTO dto) {
        Permission created = permissionService.createPermission(dto);
        return Response.success(toVO(created));
    }

    @RequiresPermission("permission:update")
    @PutMapping("/{id}")
    public Response<PermissionVO> updatePermission(@PathVariable Long id, @RequestBody PermissionUpdateDTO dto) {
        Permission updated = permissionService.updatePermission(id, dto);
        return Response.success(toVO(updated));
    }

    @RequiresPermission("permission:delete")
    @DeleteMapping("/{id}")
    public Response<Object> deletePermission(@PathVariable Long id) {
        permissionService.deletePermission(id);
        return Response.success(id);
    }

    private PermissionVO toVO(Permission p) {
        PermissionVO vo = new PermissionVO();
        vo.setId(p.getId());
        vo.setCode(p.getCode());
        vo.setName(p.getName());
        vo.setCreateTime(p.getCreateTime());
        return vo;
    }
}

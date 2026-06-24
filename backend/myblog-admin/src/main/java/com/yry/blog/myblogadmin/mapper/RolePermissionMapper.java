package com.yry.blog.myblogadmin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yry.blog.myblogcommon.entity.RolePermission.RolePermission;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RolePermissionMapper extends BaseMapper<RolePermission> {
    
    @Select("SELECT permission FROM role_permission WHERE role_id = #{roleId}")
    List<String> selectPermissionsByRoleId(@Param("roleId") Long roleId);
    
    @Delete("DELETE FROM role_permission WHERE role_id = #{roleId}")
    int deleteByRoleId(@Param("roleId") Long roleId);

    @Delete("DELETE FROM role_permission WHERE permission_id = #{permissionId}")
    int deleteByPermissionId(@Param("permissionId") Long permissionId);

    @Insert({"<script>",
            "INSERT INTO role_permission(role_id, permission) VALUES ",
            "<foreach collection='list' item='rp' separator=','>",
            "(#{rp.roleId}, #{rp.permission})",
            "</foreach>",
            "</script>"})
    void insertBatch(@Param("list") List<RolePermission> list);
}

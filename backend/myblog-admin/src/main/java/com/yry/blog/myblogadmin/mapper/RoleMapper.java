package com.yry.blog.myblogadmin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yry.blog.myblogcommon.entity.Permission.Permission;
import com.yry.blog.myblogcommon.entity.Role.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    /**
     * 根据角色ID查询其拥有的所有权限
     * @param roleIds 角色ID列表
     * @return 权限列表
     */
    @Select({
            "<script>",
            "SELECT",
            "  p.id,",
            "  p.name,",
            "  p.code,",
            "  p.create_time,",
            "  p.update_time",
            "FROM permission p",
            "INNER JOIN role_permission rp ON p.code = rp.permission",
            "WHERE rp.role_id IN",
            "<foreach collection='roleIds' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "</script>"
    })
    List<Permission> selectPermissionsByRoleIds(@Param("roleIds") List<Long> roleIds);
    // 这里特别注意roleIds是List，而MyBatis 的 #{...} 无法自动展开 List，所以不能直接把#{roleIds} 写在@Select里，应该单独放在xml的<foreach>标签里

    /**
     * 查询所有角色
     * @return 角色列表
     */
    @Select("SELECT id, role_name, role_description, create_time, update_time FROM role")
    List<Role> selectAll();
}
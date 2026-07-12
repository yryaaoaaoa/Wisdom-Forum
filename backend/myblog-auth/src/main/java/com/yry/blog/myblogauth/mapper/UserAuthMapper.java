package com.yry.blog.myblogauth.mapper;
// UserMapper.java

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yry.blog.myblogcommon.entity.Role.Role;
import com.yry.blog.myblogcommon.entity.user.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserAuthMapper extends BaseMapper<User> {
    // 基础 CRUD 由 MyBatis-Plus 提供

    // 自定义权限查询方法
    @Select("SELECT r.* FROM role r " +
            "JOIN user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId}")
    List<Role> selectRolesByUserId(Long userId);


    /**
     * 根据用户ID查询其拥有的所有角色ID
     */
    @Select("SELECT role_id FROM user_role WHERE user_id = #{userId}")
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);

}
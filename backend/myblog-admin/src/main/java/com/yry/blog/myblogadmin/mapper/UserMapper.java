package com.yry.blog.myblogadmin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yry.blog.myblogcommon.entity.user.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
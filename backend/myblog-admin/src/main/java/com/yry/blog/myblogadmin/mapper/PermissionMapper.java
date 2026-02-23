package com.yry.blog.myblogadmin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yry.blog.myblogcommon.entity.Permission.Permission;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {
}

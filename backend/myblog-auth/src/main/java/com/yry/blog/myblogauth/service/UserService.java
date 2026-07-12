package com.yry.blog.myblogauth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yry.blog.myblogauth.dto.UserRegisterDTO;
import com.yry.blog.myblogcommon.entity.user.User;

public interface UserService extends IService<User> {
    User findByUsername(String username);
    void addUser(UserRegisterDTO dto);
}

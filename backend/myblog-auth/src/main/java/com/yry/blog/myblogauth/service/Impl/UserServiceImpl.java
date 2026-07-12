package com.yry.blog.myblogauth.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yry.blog.myblogauth.dto.UserRegisterDTO;
import com.yry.blog.myblogauth.mapper.UserAuthMapper;
import com.yry.blog.myblogauth.service.UserService;
import com.yry.blog.myblogcommon.entity.user.User;
import com.yry.blog.myblogcommon.enums.ResponseCodeEnums;
import com.yry.blog.myblogcommon.exception.BusinessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserAuthMapper, User> implements UserService {

    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User findByUsername(String username) {
        return this.lambdaQuery()
                .eq(User::getUsername, username)
                .one();
    }

    @Override
    public void addUser(UserRegisterDTO dto) {
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname());
        user.setEmail(dto.getEmail());
        user.setEnabled(true);
        this.save(user);
    }
}

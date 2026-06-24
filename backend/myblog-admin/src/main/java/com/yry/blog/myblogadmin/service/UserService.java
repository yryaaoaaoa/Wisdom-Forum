package com.yry.blog.myblogadmin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yry.blog.myblogadmin.vo.UserAdminVO;
import com.yry.blog.myblogadmin.dto.UserQueryDTO;
import com.yry.blog.myblogadmin.dto.UserRegisterDTO;
import com.yry.blog.myblogadmin.dto.UserUpdateDTO;
import com.yry.blog.myblogcommon.entity.user.User;
import com.yry.blog.myblogcommon.result.PaginationResponse;
import com.yry.blog.myblogcommon.result.Response;

public interface UserService extends IService<User> {
    User findByUsername (String username);
    void addUser(UserRegisterDTO dto);
    Response<UserUpdateDTO> updateUser(Long id,UserUpdateDTO dto);
    Response<Object> deleteUser(Long id);
    Response<Boolean> resetPassword(String username, String oldPassword, String newPassword);
    Response<PaginationResponse<UserAdminVO>> pageUsers(UserQueryDTO queryDTO);
    void forgetPassword(String username, String email);
    Response<String> updateAvatar(Long userId, String avatarUrl);
    Response<UserAdminVO> getCurrentUser(Long userId);
}

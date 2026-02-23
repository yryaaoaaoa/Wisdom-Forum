package com.yry.blog.myblogadmin.controller;

import com.yry.blog.myblogadmin.dto.*;
import com.yry.blog.myblogadmin.service.UserLoginService;
import com.yry.blog.myblogadmin.service.UserService;
import com.yry.blog.myblogadmin.vo.UserAdminVO;
import com.yry.blog.myblogcommon.annotation.RequiresPermission;
import com.yry.blog.myblogcommon.result.PaginationResponse;
import com.yry.blog.myblogcommon.result.Response;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * RESTful API控制器，处理与用户相关的HTTP请求。
 * 该控制器提供对用户资源的CRUD操作，返回JSON格式数据。
 */
@RestController // 组合注解，等价于@Controller + @ResponseBody，将返回值自动序列化为JSON响应
@RequestMapping("api/users") // 映射所有以/api/users开头的请求到本控制器,这里用了nginx做反向代理所以只要写/users即可
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 分页获取指定条件用户列表（若查询条件为空则默认查询所有用户）
     * @return 包含符合条件用户的列表
     */
    @RequiresPermission("user:view")
    @GetMapping
    public Response<PaginationResponse<UserAdminVO>> getUsers(
            @Validated UserQueryDTO queryDTO) {
        return userService.pageUsers(queryDTO);
    }



    /**
     * 创建新用户
     * @param user 从请求体中解析的用户对象
     * @return 创建后的用户对象（通常包含自动生成的ID）
     */
    @RequiresPermission("user:create")
    @PostMapping ("/add")// 处理HTTP POST请求，路径为/api/users
    public UserRegisterDTO createUser(@RequestBody UserRegisterDTO user) { // @RequestBody将请求体JSON解析为User对象
        userService.addUser(user);
        return user;
    }

    /**
     * 更新现有用户
     * @param id 要更新的用户ID，从URL路径变量获取
     * @param user 包含更新信息的用户对象，从请求体获取
     * @return 更新后的用户对象
     */
    @RequiresPermission("user:update")
    @PutMapping("/{id}") // 处理PUT请求，路径为/api/users/{id}
    public Response<UserUpdateDTO> updateUser(@PathVariable Long id, @RequestBody UserUpdateDTO user) {
        return userService.updateUser(id,user);
    }

    /**
     * 删除用户
     * @param id 要删除的用户ID，从URL路径变量获取
     * @return 删除操作结果（成功返回true，失败返回false）
     */
    @RequiresPermission("user:delete")
    @DeleteMapping("/{id}") // 处理DELETE请求，路径为/api/users/{id}
    public Response<Object> deleteUser(@PathVariable Long id) {
        return userService.deleteUser(id);
    }
    /**
     * 重置用户密码
     * @param userDetails 当前认证用户的详细信息，从Security上下文获取
     * @param passwordResetDTO 包含旧密码和新密码的DTO对象，从请求体获取
     * @return 重置操作结果（成功返回true，失败返回false）
     */
    @RequiresPermission("user:password")
    @PutMapping("/me/password")
    public Response<Boolean> resetPassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid UserPasswordResetDTO passwordResetDTO) {

        String username = userDetails.getUsername();

        return userService.resetPassword(username,
                passwordResetDTO.getOldPassword(),
                passwordResetDTO.getNewPassword());
    }


}
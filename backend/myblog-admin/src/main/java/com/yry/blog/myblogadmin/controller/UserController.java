package com.yry.blog.myblogadmin.controller;

import com.yry.blog.myblogadmin.dto.UserQueryDTO;
import com.yry.blog.myblogadmin.dto.UserUpdateDTO;
import com.yry.blog.myblogauth.dto.UserPasswordResetDTO;
import com.yry.blog.myblogauth.dto.UserRegisterDTO;
import com.yry.blog.myblogauth.service.UserService;
import com.yry.blog.myblogadmin.service.UserManageService;
import com.yry.blog.myblogadmin.vo.UserAdminVO;
import com.yry.blog.myblogcommon.annotation.RequiresPermission;
import com.yry.blog.myblogcommon.auth.PermissionChecker;
import com.yry.blog.myblogcommon.minio.MinioService;
import com.yry.blog.myblogcommon.result.PaginationResponse;
import com.yry.blog.myblogcommon.enums.ResponseCodeEnums;
import com.yry.blog.myblogcommon.result.Response;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("api/users")
public class UserController {

    private final UserService userService;
    private final UserManageService userManageService;

    @Autowired
    private PermissionChecker permissionChecker;

    @Autowired
    private MinioService minioService;

    public UserController(UserService userService, UserManageService userManageService) {
        this.userService = userService;
        this.userManageService = userManageService;
    }

    @RequiresPermission("user:view")
    @GetMapping
    public Response<PaginationResponse<UserAdminVO>> getUsers(
            @Validated UserQueryDTO queryDTO) {
        return userManageService.pageUsers(queryDTO);
    }

    @RequiresPermission("user:create")
    @PostMapping
    public Response<Object> createUser(@RequestBody UserRegisterDTO user) {
        userService.addUser(user);
        return Response.success("添加成功");
    }

    @RequiresPermission("user:update")
    @PutMapping("/{id}")
    public Response<UserUpdateDTO> updateUser(@PathVariable Long id, @RequestBody UserUpdateDTO user) {
        return userManageService.updateUser(id,user);
    }

    @RequiresPermission("user:delete")
    @DeleteMapping("/{id}")
    public Response<Object> deleteUser(@PathVariable Long id) {
        return userManageService.deleteUser(id);
    }

    @RequiresPermission("user:password")
    @PutMapping("/me/password")
    public Response<Boolean> resetPassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid UserPasswordResetDTO passwordResetDTO) {
        String username = userDetails.getUsername();
        return userManageService.resetPassword(username,
                passwordResetDTO.getOldPassword(),
                passwordResetDTO.getNewPassword());
    }

    @GetMapping("/me")
    public Response<UserAdminVO> getCurrentUser() {
        Long userId = permissionChecker.getCurrentUserId();
        if (userId == null) {
            return Response.error(ResponseCodeEnums.UNAUTHORIZED);
        }
        return userManageService.getCurrentUser(userId);
    }

    @PostMapping("/me/avatar")
    public Response<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Long userId = permissionChecker.getCurrentUserId();
        if (userId == null) {
            return Response.error(ResponseCodeEnums.UNAUTHORIZED);
        }

        if (file.isEmpty()) {
            return Response.error(ResponseCodeEnums.FILE_UPLOAD_FAILED, "上传文件为空");
        }

        try {
            UserAdminVO currentUser = userManageService.getCurrentUser(userId).getData();
            String oldAvatarUrl = currentUser != null ? currentUser.getAvatarUrl() : null;

            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".") 
                    ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
                    : ".jpg";
            String filename = UUID.randomUUID() + extension;
            String objectName = "avatars/" + userId + "/" + filename;
            
            minioService.uploadFile(file, objectName);
            
            String avatarUrl = "/api/files/avatars/" + userId + "/" + filename;
            
            Response<String> result = userManageService.updateAvatar(userId, avatarUrl);

            if (result.getData() != null && oldAvatarUrl != null && oldAvatarUrl.startsWith("/api/files/avatars/")) {
                try {
                    String oldObjectName = oldAvatarUrl.replace("/api/files/", "");
                    minioService.deleteFile(oldObjectName);
                } catch (Exception e) {
                    // 删除旧头像失败不影响主流程
                }
            }
            
            return result;
        } catch (Exception e) {
            return Response.error(ResponseCodeEnums.FILE_UPLOAD_FAILED);
        }
    }
}
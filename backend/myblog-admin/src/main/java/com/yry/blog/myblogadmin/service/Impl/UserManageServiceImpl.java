package com.yry.blog.myblogadmin.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yry.blog.myblogauth.auth.Impl.PermissionCheckerImpl;
import com.yry.blog.myblogauth.mapper.RoleMapper;
import com.yry.blog.myblogauth.mapper.UserRoleMapper;
import com.yry.blog.myblogadmin.dto.UserQueryDTO;
import com.yry.blog.myblogadmin.dto.UserUpdateDTO;
import com.yry.blog.myblogadmin.mapper.UserMapper;
import com.yry.blog.myblogadmin.mapper.mapstruct.UserConvertMapper;
import com.yry.blog.myblogadmin.service.UserManageService;
import com.yry.blog.myblogadmin.vo.UserAdminVO;
import com.yry.blog.myblogcommon.entity.Role.Role;
import com.yry.blog.myblogcommon.entity.UserRole.UserRole;
import com.yry.blog.myblogcommon.entity.user.User;
import com.yry.blog.myblogcommon.enums.ResponseCodeEnums;
import com.yry.blog.myblogcommon.exception.BusinessException;
import com.yry.blog.myblogcommon.result.PaginationResponse;
import com.yry.blog.myblogcommon.result.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class UserManageServiceImpl extends ServiceImpl<UserMapper, User> implements UserManageService {

    private final PasswordEncoder passwordEncoder;
    private final UserConvertMapper userConvertMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final PermissionCheckerImpl checkPermission;

    public UserManageServiceImpl(PasswordEncoder passwordEncoder, RoleMapper roleMapper, UserRoleMapper userRoleMapper, PermissionCheckerImpl checkPermission, UserConvertMapper userConvertMapper) {
        this.passwordEncoder = passwordEncoder;
        this.checkPermission = checkPermission;
        this.userConvertMapper = userConvertMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
    }

    @Override
    public Response<UserUpdateDTO> updateUser(Long id, UserUpdateDTO dto) {
        if (id == null || dto == null) {
            return Response.error(ResponseCodeEnums.BAD_REQUEST);
        }

        if (!checkPermission.check(id)) {
            return Response.error(ResponseCodeEnums.PERMISSION_DENIED);
        }

        User existingUser = this.getById(id);
        if (existingUser == null) {
            return Response.error(ResponseCodeEnums.USER_NOT_EXIST);
        }

        UpdateWrapper<User> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", id)
                .set("nickname", dto.getNickname())
                .set("email", dto.getEmail());
        if (dto.getAvatarUrl() != null) {
            updateWrapper.set("avatar_url", dto.getAvatarUrl());
        }
        if (dto.getEnabled() != null) {
            updateWrapper.set("enabled", dto.getEnabled());
        }
        boolean updateSuccess = this.update(updateWrapper);
        if (!updateSuccess) {
            return Response.error(ResponseCodeEnums.INTERNAL_SERVER_ERROR);
        }

        if (dto.getRoles() != null) {
            userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, id));
            for (String roleName : dto.getRoles()) {
                Role role = roleMapper.selectOne(
                        new LambdaQueryWrapper<Role>().eq(Role::getRoleName, roleName)
                );
                if (role != null) {
                    UserRole userRole = UserRole.builder()
                            .userId(id)
                            .roleId(role.getId())
                            .build();
                    userRoleMapper.insert(userRole);
                }
            }
        }

        User updatedUser = this.getById(id);
        return Response.success(userConvertMapper.userToUpdateDTO(updatedUser));
    }

    @Override
    public Response<Object> deleteUser(Long id) {
        if (!checkPermission.check(id)){
            return Response.error(ResponseCodeEnums.PERMISSION_DENIED);
        }
        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, id));
        boolean removed = this.removeById(id);
        log.info("删除用户成功, userId: {}", id);
        return Response.success(removed);
    }

    @Transactional(readOnly = true)
    @Override
    public User findByUsername(String username) {
        return this.lambdaQuery()
                .eq(User::getUsername, username)
                .one();
    }

    @Override
    public Response<PaginationResponse<UserAdminVO>> pageUsers(UserQueryDTO queryDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.debug("当前认证用户: {}", authentication);
        Page<User> page = new Page<>(queryDTO.getCurrent(), queryDTO.getSize());
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(queryDTO.getKeyword())) {
            String k = queryDTO.getKeyword();
            wrapper.and(w -> w
                    .like(User::getUsername, k)
                    .or().like(User::getNickname, k)
                    .or().like(User::getEmail, k)
            );
        }
        if (queryDTO.getEnabled() != null) {
            wrapper.eq(User::isEnabled, queryDTO.getEnabled());
        }
        if (queryDTO.getStartTime() != null) {
            wrapper.ge(User::getCreatedAt, queryDTO.getStartTime());
        }
        if (queryDTO.getEndTime() != null) {
            wrapper.le(User::getCreatedAt, queryDTO.getEndTime());
        }

        if ("username".equals(queryDTO.getOrderBy())) {
            wrapper.orderBy(true, "ASC".equals(queryDTO.getOrderType()), User::getUsername);
        } else if ("createdAt".equals(queryDTO.getOrderBy())) {
            wrapper.orderBy(true, "ASC".equals(queryDTO.getOrderType()), User::getCreatedAt);
        } else {
            wrapper.orderByDesc(User::getId);
        }
        IPage<User> userPage = this.page(page, wrapper);
        IPage<UserAdminVO> dtoPage = userPage.convert(user -> {
            UserAdminVO vo = userConvertMapper.userToAdminDTO(user);
            vo.setEnabled(user.isEnabled());
            return vo;
        });
        List<Long> userIds = dtoPage.getRecords().stream()
                .map(UserAdminVO::getId)
                .collect(Collectors.toList());
        if (!userIds.isEmpty()) {
            List<UserRole> allUserRoles = userRoleMapper.selectList(
                    new LambdaQueryWrapper<UserRole>().in(UserRole::getUserId, userIds)
            );
            Map<Long, List<Long>> userRoleMap = allUserRoles.stream()
                    .collect(Collectors.groupingBy(UserRole::getUserId,
                            Collectors.mapping(UserRole::getRoleId, Collectors.toList())));
            List<Long> allRoleIds = allUserRoles.stream()
                    .map(UserRole::getRoleId)
                    .distinct()
                    .collect(Collectors.toList());
            Map<Long, String> roleNameMap = roleMapper.selectBatchIds(allRoleIds).stream()
                    .collect(Collectors.toMap(Role::getId, Role::getRoleName));
            dtoPage.getRecords().forEach(vo -> {
                List<Long> roleIds = userRoleMap.getOrDefault(vo.getId(), Collections.emptyList());
                vo.setRoleName(roleIds.stream()
                        .map(rid -> roleNameMap.getOrDefault(rid, ""))
                        .filter(n -> !n.isEmpty())
                        .collect(Collectors.joining(", ")));
            });
        }
        PaginationResponse<UserAdminVO> paginationResponse = new PaginationResponse<>(dtoPage);
        return Response.success(paginationResponse);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<Boolean> resetPassword(String username, String oldPassword, String newPassword) {
        User user = this.getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) {
            throw new BusinessException(ResponseCodeEnums.USER_NOT_EXIST);
        }
        if (oldPassword == null || oldPassword.trim().isEmpty() || newPassword.trim().isEmpty()) {
            throw new BusinessException(ResponseCodeEnums.PASSWORD_EMPTY);
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(ResponseCodeEnums.OLD_PASSWORD_ERROR);
        }
        if (oldPassword.equals(newPassword)) {
            throw new BusinessException(ResponseCodeEnums.NEW_PASSWORD_SAME_AS_OLD);
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        boolean success = this.updateById(user);
        return Response.success(success);
    }

    @Override
    public void forgetPassword(String username, String email) {
        User user = this.getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) {
            throw new BusinessException(ResponseCodeEnums.USER_NOT_EXIST);
        }
        if (!user.getEmail().equals(email)) {
            throw new BusinessException(ResponseCodeEnums.EMAIL_ERROR);
        }
        String newPassword = RandomStringUtils.randomAlphanumeric(8);
        user.setPassword(passwordEncoder.encode(newPassword));
        this.updateById(user);
    }

    @Override
    public Response<String> updateAvatar(Long userId, String avatarUrl) {
        User user = this.getById(userId);
        if (user == null) {
            return Response.error(ResponseCodeEnums.USER_NOT_EXIST);
        }

        UpdateWrapper<User> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", userId)
                .set("avatar_url", avatarUrl);

        boolean success = this.update(updateWrapper);
        if (!success) {
            return Response.error(ResponseCodeEnums.INTERNAL_SERVER_ERROR);
        }

        return Response.success(avatarUrl);
    }

    @Override
    public Response<UserAdminVO> getCurrentUser(Long userId) {
        User user = this.getById(userId);
        if (user == null) {
            return Response.error(ResponseCodeEnums.USER_NOT_EXIST);
        }
        UserAdminVO vo = userConvertMapper.userToAdminDTO(user);
        vo.setEnabled(user.isEnabled());
        List<UserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId)
        );
        if (!userRoles.isEmpty()) {
            List<Long> roleIds = userRoles.stream()
                    .map(UserRole::getRoleId)
                    .collect(Collectors.toList());
            String roleName = roleMapper.selectBatchIds(roleIds).stream()
                    .map(Role::getRoleName)
                    .collect(Collectors.joining(", "));
            vo.setRoleName(roleName);
        }
        return Response.success(vo);
    }
}

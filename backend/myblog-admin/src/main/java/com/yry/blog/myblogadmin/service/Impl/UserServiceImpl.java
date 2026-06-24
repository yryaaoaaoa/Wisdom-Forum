package com.yry.blog.myblogadmin.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yry.blog.myblogadmin.auth.Impl.PermissionCheckerImpl;
import com.yry.blog.myblogadmin.vo.UserAdminVO;
import com.yry.blog.myblogadmin.dto.UserQueryDTO;
import com.yry.blog.myblogadmin.dto.UserRegisterDTO;
import com.yry.blog.myblogadmin.dto.UserUpdateDTO;
import com.yry.blog.myblogadmin.mapper.mapstruct.UserConvertMapper;
import com.yry.blog.myblogadmin.service.UserService;
import com.yry.blog.myblogcommon.entity.Role.Role;
import com.yry.blog.myblogcommon.entity.UserRole.UserRole;
import com.yry.blog.myblogcommon.entity.user.User;
import com.yry.blog.myblogcommon.enums.ResponseCodeEnums;
import com.yry.blog.myblogcommon.exception.BusinessException;
import com.yry.blog.myblogadmin.mapper.RoleMapper;
import com.yry.blog.myblogadmin.mapper.UserMapper;
import com.yry.blog.myblogadmin.mapper.UserRoleMapper;
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

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;

@Slf4j
@Service
@Transactional
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService { //这里集成了ServiceImpl，所以可以直接使用它的方法

    private final PasswordEncoder passwordEncoder;  // Spring Security密码编码器
    private final UserConvertMapper userConvertMapper;  // MapStruct对象转换器
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final PermissionCheckerImpl checkPermission;

    public UserServiceImpl(PasswordEncoder passwordEncoder, RoleMapper roleMapper, UserRoleMapper userRoleMapper , PermissionCheckerImpl checkPermission, UserConvertMapper userConvertMapper) {
        this.passwordEncoder = passwordEncoder;
        this.checkPermission = checkPermission;
        this.userConvertMapper = userConvertMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
    }

    /**
     * 添加新用户
     * @param dto 用户注册数据传输对象
     * @throws IllegalArgumentException 当用户名或密码为空时抛出
     */
    @Override
    public void addUser(UserRegisterDTO dto) {
        // DTO转Entity
        User user = userConvertMapper.registerDtoToUser(dto);
        if (this.lambdaQuery()
                .eq(User::getUsername, user.getUsername())
                .exists()) {  // 直接返回 boolean，无需查询实体
            throw new BusinessException(ResponseCodeEnums.USER_EXIST);
        }
        // 密码加密存储
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        // 保存到数据库
        this.save(user);
        assignDefaultRole(user);
    }

    /**
     * 更新用户信息
     * @param dto 用户更新数据传输对象
     * @return 包含更新后数据的响应对象
     * @throws NoSuchElementException 当用户不存在时抛出
     */
    @Override
    public Response<UserUpdateDTO> updateUser(Long id,UserUpdateDTO dto) {
        // 1. 基础参数校验
        if (id == null || dto == null) {
            return Response.error(ResponseCodeEnums.BAD_REQUEST);
        }

        // 2. 权限校验（统一返回Response，而非混合抛异常）
        if (!checkPermission.check(id)) {
            return Response.error(ResponseCodeEnums.PERMISSION_DENIED);
        }

        // 3. 校验用户存在性（统一返回Response，避免抛未捕获的异常）
        User existingUser = this.getById(id);
        if (existingUser == null) {
            return Response.error(ResponseCodeEnums.USER_NOT_EXIST);
        }

        // 4. 构建更新条件 + 仅更新允许修改的字段（局部更新）
        UpdateWrapper<User> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", id)
                .set("nickname", dto.getNickname())
                .set("email", dto.getEmail())
                .set("avatar_url", dto.getAvatarUrl());
        if (dto.getEnabled() != null) {
            updateWrapper.set("enabled", dto.getEnabled());
        }
        // 5. 执行更新并校验结果
        boolean updateSuccess = this.update(updateWrapper);
        if (!updateSuccess) {
            return Response.error(ResponseCodeEnums.INTERNAL_SERVER_ERROR);
        }

        // 6. 更新用户角色
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

        // 6. 返回数据库中实际更新后的数据（保证一致性）
        User updatedUser = this.getById(id);
        return Response.success(userConvertMapper.userToUpdateDTO(updatedUser));
    }

    /**
     * 删除用户
     *
     * @param id 用户ID
     * @return 是否删除成功
     */
    @Override
    public Response<Object> deleteUser(Long id) {
        if (!checkPermission.check(id)){
            return Response.error(ResponseCodeEnums.PERMISSION_DENIED);
        }
        // 级联清理用户角色关联
        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, id));
        boolean removed = this.removeById(id);
        log.info("删除用户成功, userId: {}", id);
        return Response.success(removed);
    }
    /**
     * 根据用户名查询用户
     * @param username 用户名
     * @return 用户实体
     */
    @Transactional(readOnly = true)
    @Override
    public User findByUsername(String username) {
        return this.lambdaQuery()
                .eq(User::getUsername, username)
                .one();
    }
    /**
     * 分页查询用户列表（带条件）
     * @param queryDTO 包含查询条件和分页参数
     * @return 分页响应数据
     */
    @Override
    public Response<PaginationResponse<UserAdminVO>> pageUsers(UserQueryDTO queryDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.debug("当前认证用户: {}", authentication);
        Page<User> page = new Page<>(queryDTO.getCurrent(), queryDTO.getSize());
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>(); // LambdaQueryWrapper链式查询条件构造器
        if (StringUtils.hasText(queryDTO.getKeyword())) {
            String k = queryDTO.getKeyword();
            wrapper.and(w -> w
                    .like(User::getUsername, k)
                    .or().like(User::getNickname, k)
                    .or().like(User::getEmail, k)
            );
        }
        // ========== 动态构建查询条件 ==========
        // 启用状态精确查询
        if (queryDTO.getEnabled() != null) {
            wrapper.eq(User::isEnabled, queryDTO.getEnabled());
        }
        // 时间范围查询
        if (queryDTO.getStartTime() != null) {
            wrapper.ge(User::getCreatedAt, queryDTO.getStartTime());
        }
        if (queryDTO.getEndTime() != null) {
            wrapper.le(User::getCreatedAt, queryDTO.getEndTime());
        }

        // ========== 排序逻辑 ==========
        if ("username".equals(queryDTO.getOrderBy())) {
            wrapper.orderBy(true, "ASC".equals(queryDTO.getOrderType()), User::getUsername);
        } else if ("createdAt".equals(queryDTO.getOrderBy())) {
            wrapper.orderBy(true, "ASC".equals(queryDTO.getOrderType()), User::getCreatedAt);
        } else {
            // 默认排序：按创建时间倒序
            wrapper.orderByDesc(User::getId);
        }
        // 执行分页查询
        IPage<User> userPage = this.page(page, wrapper);
        // 将实体分页转换为DTO分页
        IPage<UserAdminVO> dtoPage = userPage.convert(user -> {
            UserAdminVO vo = userConvertMapper.userToAdminDTO(user);
            vo.setEnabled(user.isEnabled());
            return vo;
        });
        // 批量填充角色名
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
        // 包装分页响应
        PaginationResponse<UserAdminVO> paginationResponse = new PaginationResponse<>(dtoPage);

        return Response.success(paginationResponse);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)  // 明确声明事务
    public Response<Boolean> resetPassword(String username, String oldPassword, String newPassword) {
        // 查询用户
        User user = this.getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) {
            throw new BusinessException(ResponseCodeEnums.USER_NOT_EXIST);
        }
        // 新旧密码都不能为空
        if (oldPassword == null || oldPassword.trim().isEmpty() || newPassword.trim().isEmpty()) {
            throw new BusinessException(ResponseCodeEnums.PASSWORD_EMPTY);
        }
        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(ResponseCodeEnums.OLD_PASSWORD_ERROR);
        }
        // 新旧密码应该不同
        if (oldPassword.equals(newPassword)) {
            throw new BusinessException(ResponseCodeEnums.NEW_PASSWORD_SAME_AS_OLD);
        }
        // 设置新密码并更新
        user.setPassword(passwordEncoder.encode(newPassword));
        boolean success = this.updateById(user);

        return Response.success(success);
    }
    @Override
    public void forgetPassword(String username, String email) {
        // 获取用户
        User user = this.getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) {
            throw new BusinessException(ResponseCodeEnums.USER_NOT_EXIST);
        }
        // 验证邮箱
        if (!user.getEmail().equals(email)) {
            throw new BusinessException(ResponseCodeEnums.EMAIL_ERROR);
        }
        // 生成随机密码
        String newPassword = RandomStringUtils.randomAlphanumeric(8);
        // 更新用户密码
        user.setPassword(passwordEncoder.encode(newPassword));
        this.updateById(user);
    }
    private void assignDefaultRole(User user) {
        Role normalUserRole = roleMapper.selectOne(new LambdaQueryWrapper<Role>()
                .eq(Role::getRoleName, "ROLE_USER"));

        if (normalUserRole != null) {
            UserRole userRole = new UserRole();
            userRole.setUserId(user.getId());
            userRole.setRoleId(normalUserRole.getId());
            userRoleMapper.insert(userRole);
        }
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
        // 填充角色名
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
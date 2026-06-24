package com.yry.blog.myblogarticle.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yry.blog.myblogarticle.service.NotificationService;
import com.yry.blog.myblogarticle.vo.NotificationVO;
import com.yry.blog.myblogcommon.auth.PermissionChecker;
import com.yry.blog.myblogcommon.enums.ResponseCodeEnums;
import com.yry.blog.myblogcommon.result.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private PermissionChecker permissionChecker;

    @GetMapping("/list")
    public Response<IPage<NotificationVO>> getNotifications(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Long userId = permissionChecker.getCurrentUserId();
        if (userId == null) {
            return Response.error(ResponseCodeEnums.UNAUTHORIZED);
        }
        return Response.success(notificationService.getNotifications(userId, page, size));
    }

    @GetMapping("/unread-count")
    public Response<Integer> getUnreadCount() {
        Long userId = permissionChecker.getCurrentUserId();
        if (userId == null) {
            return Response.success(0);
        }
        return Response.success(notificationService.getUnreadCount(userId));
    }

    @PutMapping("/read/{id}")
    public Response<Object> markAsRead(@PathVariable Long id) {
        Long userId = permissionChecker.getCurrentUserId();
        if (userId == null) {
            return Response.error(ResponseCodeEnums.UNAUTHORIZED);
        }
        notificationService.markAsRead(id, userId);
        return Response.success(null);
    }

    @PutMapping("/read-all")
    public Response<Object> markAllAsRead() {
        Long userId = permissionChecker.getCurrentUserId();
        if (userId == null) {
            return Response.error(ResponseCodeEnums.UNAUTHORIZED);
        }
        notificationService.markAllAsRead(userId);
        return Response.success(null);
    }
}

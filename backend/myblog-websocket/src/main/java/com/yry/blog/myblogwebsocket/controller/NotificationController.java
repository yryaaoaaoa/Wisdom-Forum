package com.yry.blog.myblogwebsocket.controller;

import com.yry.blog.myblogwebsocket.dto.NotificationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

/**
 * 通知发送接口（整合自定义线程池，处理点赞、评论@提醒）
 */
@Slf4j
@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationController {

    // Spring WebSocket核心推送工具
    private final SimpMessagingTemplate messagingTemplate;

    // 注入你自定义的线程池（指定bean名称）
    @Qualifier("myblogTaskExecutor")
    private final ExecutorService myblogTaskExecutor;

    /**
     * 发送点赞通知（点对点）
     * 基于自定义线程池异步执行，避免阻塞主线程
     */
    @PostMapping("/like")
    public String sendLikeNotification(@RequestBody NotificationMessage message) {
        // 1. 前置参数校验（必做，避免无效任务提交）
        if (message.getSenderName() == null || message.getContentId() == null) {
            log.warn("点赞通知参数缺失，senderName={}, postId={}", message.getSenderName(), message.getContentId());
            return "参数错误：发送者ID和帖子ID不能为空";
        }
        // 2. 补全通知基础信息
        message.setType("LIKE");
        message.setCreateTime(LocalDateTime.now());

        // 3. 提交异步任务到自定义线程池
        try {
            myblogTaskExecutor.submit(() -> {
                // 核心：推送WebSocket广播消息
                messagingTemplate.convertAndSendToUser(message.getReceiverId(),"/queue/like", message);
                log.info("点赞通知推送成功，postId={}, senderName={}", message.getContentId(), message.getSenderName());
            });
            return "点赞通知已提交发送";
        } catch (RejectedExecutionException e) {
            // 捕获线程池饱和异常，返回友好提示（不影响核心业务）
            log.error("线程池饱和，点赞通知推送失败，postId={}", message.getContentId(), e);
            return "通知发送暂时繁忙，请稍后再试（点赞已生效）";
        } catch (Exception e) {
            // 其他异常兜底
            log.error("点赞通知提交失败，message={}", message, e);
            return "通知发送失败（点赞已生效）";
        }
    }

    /**
     * 发送@提醒（点对点）
     * 基于自定义线程池异步执行
     */
    @PostMapping("/mention")
    public String sendMentionNotification(@RequestBody NotificationMessage message) {
        // 1. 参数校验
        if (message.getReceiverId() == null || message.getSenderName() == null || message.getContentId() == null) {
            log.warn("@提醒参数缺失，receiverId={}, senderName={}, postId={}",
                    message.getReceiverId(), message.getSenderName(), message.getContentId());
            return "参数错误：接收者ID、发送者ID、帖子ID不能为空";
        }

        // 2. 补全通知基础信息
        message.setType("MENTION");
        message.setCreateTime(LocalDateTime.now());

        // 3. 提交异步任务
        try {
            myblogTaskExecutor.submit(() -> {
                // 核心：点对点推送（只推给被@用户）
                messagingTemplate.convertAndSendToUser(
                        message.getReceiverId(),
                        "/queue/mention",
                        message
                );
                log.info("@提醒推送成功，receiverId={}, postId={}", message.getReceiverId(), message.getContentId());
            });
            return "@提醒已提交发送";
        } catch (RejectedExecutionException e) {
            log.error("线程池饱和，@提醒推送失败，receiverId={}", message.getReceiverId(), e);
            return "通知发送暂时繁忙，请稍后再试";
        } catch (Exception e) {
            log.error("@提醒提交失败，message={}", message, e);
            return "通知发送失败";
        }
    }
}
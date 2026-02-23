package com.yry.blog.myblogarticle.service.Impl;

import com.yry.blog.myblogarticle.mq.DefaultSendCallback;
import com.yry.blog.myblogarticle.service.LikeService;
import com.yry.blog.myblogcommon.auth.PermissionChecker;
import com.yry.blog.myblogcommon.cache.manager.RedisCacheManager;
import com.yry.blog.myblogarticle.mq.LikeSyncMessage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class LikeServiceImpl implements LikeService {
    @Autowired
    private RedisCacheManager redisCacheManager;
    @Autowired
    private RocketMQTemplate rocketMQTemplate;
    @Autowired
    private PermissionChecker permissionChecker;
    @Autowired
    @Qualifier("myblogTaskExecutor")
    private ThreadPoolExecutor myblogTaskExecutor;

    // MQ主题
    private static final String LIKE_SYNC_TOPIC = "like-db-sync-topic";
    // 点赞缓存基础过期时间（1天），转秒
    private static final long BASE_EXPIRE_SECONDS = 86400L;
    // 随机过期时间偏移（±1小时），避免缓存雪崩
    private static final long RANDOM_EXPIRE_OFFSET = 3600L;
    public static final String LIKE_USERS_KEY = "like:users:%d:%d";
    public static final String LIKE_COUNT_KEY = "like:count:%d:%d";
    // 发送超时时间（毫秒）
    private static final long SEND_TIMEOUT = 30000L;

    // 注入点赞/取消点赞的Lua脚本Bean
    @Resource(name = "likeRedisScript")
    private DefaultRedisScript<Long> likeRedisScript;

    /**
     * 改造后的点赞操作（原子化Lua脚本）
     * @param targetId 文章/评论ID
     * @param likeType 1=文章 2=评论
     * @return true=点赞成功 false=已点赞（重复操作）
     */
    @Override
    public boolean like(Long targetId, Integer likeType) {
        // 1. 获取当前用户ID，校验登录状态
        Long userId = permissionChecker.getCurrentUserId();
        if (userId == null) {
            log.error("点赞失败：用户未登录");
            return false;
        }

        // 2. 生成Redis Key
        String usersKey = String.format(LIKE_USERS_KEY, likeType, targetId);
        String countKey = String.format(LIKE_COUNT_KEY, likeType, targetId);
        // 3. 计算带随机偏移的过期时间（避免缓存雪崩）
        long expireSeconds = BASE_EXPIRE_SECONDS + new Random().nextLong(RANDOM_EXPIRE_OFFSET * 2) - RANDOM_EXPIRE_OFFSET;

        try {
            // 4. 构造脚本入参
            List<String> keys = Arrays.asList(usersKey, countKey);
            // 5. 执行Lua脚本（原子化所有操作）
            Long result = redisCacheManager.getRedisTemplate().execute(
                    likeRedisScript,
                    keys,
                    userId.toString(),
                    String.valueOf(expireSeconds)
            );
            System.out.println("结果是：" + result);
            // 6. 处理脚本结果（只改这部分！适配新返回值）
            if (result == null) {
                log.error("点赞/取消操作异常：脚本返回null，userId:{}, targetId:{}", userId, targetId);
                return false;
            }
            // 新返回值逻辑：1=点赞成功，2=取消成功，其他=失败/重复
            if (result == 1) {
                // 点赞成功 → 原有逻辑
                Long newCount = getLikeCount(targetId, likeType);
                log.debug("用户{}给{}({})点赞成功，当前点赞数：{}", userId, likeType == 1 ? "文章" : "评论", targetId, newCount);
                // 发送点赞MQ消息
                sendLikeSyncMessage(userId, targetId, likeType, 1);
                return true;
            } else if (result == 2) {
                // 取消点赞成功 → 新增取消逻辑
                Long newCount = getLikeCount(targetId, likeType);
                log.debug("用户{}取消给{}({})的点赞，当前点赞数：{}", userId, likeType == 1 ? "文章" : "评论", targetId, newCount);
                // 发送取消点赞MQ消息（注意最后一个参数改成0，表示取消）
                sendLikeSyncMessage(userId, targetId, likeType, -1);
                return true; // 取消成功也返回true，前端根据业务判断状态
            } else {
                // result=-1（非法ID）或0（其他异常）
                log.debug("用户{}操作{}({})失败：参数非法或无操作", userId, likeType == 1 ? "文章" : "评论", targetId);
                return false;
            }
        } catch (Exception e) {
            log.error("点赞操作异常，userId:{}, targetId:{}, likeType:{}", userId, targetId, likeType, e);
            return false;
        }
    }

    /**
     * 获取点赞数（高频展示用）
     */
    @Override
    public Long getLikeCount(Long targetId, Integer likeType) {
        String countKey = String.format(LIKE_COUNT_KEY, likeType, targetId);
        Object countObj = redisCacheManager.get(countKey);
        return countObj == null ? 0L : Long.parseLong(countObj.toString());
    }

    /**
     * 发送点赞同步消息（异步+回调重试）
     */
    public void sendLikeSyncMessage(Long userId, Long targetId, Integer likeType, Integer operation) {
        LikeSyncMessage message = new LikeSyncMessage();
        message.setUserId(userId);
        message.setTargetId(targetId);
        message.setLikeType(likeType);
        message.setOperation(operation);

        AtomicInteger retryCount = new AtomicInteger(3);
        doAsyncSend(message, retryCount);
    }

    /**
     * 带重试的异步发送逻辑
     */
    private void doAsyncSend(LikeSyncMessage message, AtomicInteger retryCount) {
        rocketMQTemplate.asyncSend(
                LIKE_SYNC_TOPIC,
                message,
                new DefaultSendCallback() {
                    @Override
                    public void onSuccess(SendResult sendResult) {
                        log.info("点赞消息发送成功 | msgId:{} | userId:{} | targetId:{}",
                                sendResult.getMsgId(), message.getUserId(), message.getTargetId());
                    }

                    @Override
                    public void onException(Throwable e) {
                        int remainingRetry = retryCount.decrementAndGet();
                        log.error("点赞消息发送失败，剩余重试次数：{}，userId:{}，targetId:{}",
                                remainingRetry, message.getUserId(), message.getTargetId(), e);

                        if (remainingRetry > 0 && isRetryableException(e)) {
                            myblogTaskExecutor.submit(() -> {
                                try {
                                    long sleepTime = (3 - remainingRetry) * 100L;
                                    Thread.sleep(sleepTime);
                                    doAsyncSend(message, retryCount);
                                } catch (InterruptedException ex) {
                                    Thread.currentThread().interrupt();
                                    log.error("点赞消息重试睡眠被中断 | userId:{}", message.getUserId(), ex);
                                }
                            });
                        } else {
                            log.error("点赞消息发送失败，重试耗尽 | userId:{} | targetId:{} | 消息内容:{}",
                                    message.getUserId(), message.getTargetId(), message, e);
                        }
                    }
                },
                SEND_TIMEOUT
        );
    }

    /**
     * 判断是否为可重试异常
     */
    private boolean isRetryableException(Throwable e) {
        String exceptionMsg = e.getMessage();
        return exceptionMsg != null && (
                exceptionMsg.contains("NetworkException") ||
                        exceptionMsg.contains("BrokerNotAvailable") ||
                        exceptionMsg.contains("Timeout") ||
                        exceptionMsg.contains("RemotingException")
        );
    }

    /**
     * 保留原increment方法（兼容可能的其他调用）
     */
    private Long increment(String key, int delta) {
        try {
            return redisCacheManager.getRedisTemplate().opsForValue().increment(key, delta);
        } catch (Exception e) {
            log.error("Redis计数器增减异常，key:{}, delta:{}", key, delta, e);
            throw new RuntimeException("Redis计数器增减异常");
        }
    }

    private void pushLikeNotification(Long targetId, Long likeType, Integer operation) {
        // 可扩展点赞通知逻辑
    }
}
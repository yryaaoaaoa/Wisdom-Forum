package com.yry.blog.myblogcommon.stat;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 帖子访问次数统计组件
 * 核心：本地内存临时统计（1分钟） + Redis聚合（按时间段存储）
 */
@Slf4j
@Component
public class PostAccessCounter {

    // ------------------------------ 本地内存缓存（临时存储1分钟内的访问计数） ------------------------------
    // Key: post:access:临时:帖子ID，Value: 原子计数器（保证并发安全）
    private final Cache<String, AtomicInteger> localAccessCache;

    // ------------------------------ Redis存储（聚合后的数据，按时间段划分） ------------------------------
    // Redis Key规则：
    // - 10分钟维度：post:access:10min:202601111000（2026-01-11 10:00-10:10）:帖子ID
    // - 1小时维度：post:access:1h:2026011110（2026-01-11 10:00-11:00）:帖子ID
    @Autowired
    private RedisTemplate<String, Integer> redisTemplate;

    // 初始化本地缓存：1分钟过期（自动清理），最大存储1万条（适配论坛规模）
    public PostAccessCounter() {
        this.localAccessCache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES) // 1分钟过期，避免内存堆积
                .maximumSize(10000) // 最多统计1万条帖子的访问量
                .build();
    }

    /**
     * 1. 记录单次访问（核心方法，接口层调用）
     * @param postId 帖子ID
     */
    public void recordAccess(String postId) {
        Objects.requireNonNull(postId, "帖子ID不能为空");
        String localKey = "post:access:temp:" + postId;

        // 原子计数：不存在则初始化为0，存在则自增（并发安全）
        localAccessCache.get(localKey, k -> new AtomicInteger(0)).incrementAndGet();
        log.trace("记录帖子{}访问，本地计数+1", postId);
    }

    /**
     * 2. 定时聚合任务（每分钟执行一次，把本地计数同步到Redis）
     * 注：用@Scheduled注解触发，也可配置为Spring定时任务
     */
    public void aggregateToRedis() {
        // 1. 获取本地缓存中所有的计数数据
        Map<String, AtomicInteger> allLocalCount = localAccessCache.asMap();
        if (allLocalCount.isEmpty()) {
            return;
        }

        // 2. 遍历聚合到Redis
        for (Map.Entry<String, AtomicInteger> entry : allLocalCount.entrySet()) {
            String localKey = entry.getKey();
            int count = entry.getValue().get();
            if (count == 0) {
                continue;
            }

            // 解析帖子ID（localKey格式：post:access:temp:1001）
            String postId = localKey.replace("post:access:temp:", "");
            // 生成Redis的10分钟/1小时维度Key
            String redisKey10min = get10minRedisKey(postId);
            String redisKey1h = get1hRedisKey(postId);

            // 3. Redis累加计数（INCRBY，原子操作，支持分布式）
            redisTemplate.opsForValue().increment(redisKey10min, count);
            redisTemplate.opsForValue().increment(redisKey1h, count);

            // 4. 设置Redis Key过期时间（避免内存浪费）：10分钟维度保留2小时，1小时维度保留1天
            redisTemplate.expire(redisKey10min, 2, TimeUnit.HOURS);
            redisTemplate.expire(redisKey1h, 1, TimeUnit.DAYS);

            log.debug("聚合帖子{}的访问计数：{}次 → Redis（10min维度Key：{}）", postId, count, redisKey10min);

            // 5. 清空本地计数（避免重复聚合）
            localAccessCache.invalidate(localKey);
        }
    }

    /**
     * 3. 查询指定时间段内的访问次数（核心查询方法，供热点判定使用）
     * @param postId 帖子ID
     * @param timeType 时间段类型：10min（近10分钟）、1h（近1小时）
     * @return 访问次数
     */
    public int getAccessCount(String postId, String timeType) {
        Objects.requireNonNull(postId, "帖子ID不能为空");
        Objects.requireNonNull(timeType, "时间段类型不能为空");

        int totalCount = 0;
        if ("10min".equals(timeType)) {
            // 近10分钟：只查当前10分钟维度的Key
            String redisKey = get10minRedisKey(postId);
            totalCount = getRedisCount(redisKey);
        } else if ("1h".equals(timeType)) {
            // 近1小时：需要累加最近6个10分钟维度的Key（1小时=6个10分钟）
            for (int i = 0; i < 6; i++) {
                String redisKey = get10minRedisKey(postId, i);
                totalCount += getRedisCount(redisKey);
            }
        }

        log.debug("查询帖子{}近{}访问次数：{}次", postId, timeType, totalCount);
        return totalCount;
    }

    // ------------------------------ 私有辅助方法 ------------------------------
    /**
     * 获取当前10分钟维度的Redis Key
     * 格式：post:access:10min:202601111000:1001（2026-01-11 10:00-10:10，帖子1001）
     */
    private String get10minRedisKey(String postId) {
        return get10minRedisKey(postId, 0);
    }

    /**
     * 获取指定偏移量的10分钟维度Redis Key（比如偏移1=前10分钟）
     */
    private String get10minRedisKey(String postId, int offset) {
        long now = System.currentTimeMillis();
        // 计算当前时间所属的10分钟时间段（比如10:00-10:10、10:10-10:20）
        long tenMinuteMillis = 10 * 60 * 1000L;
        long currentTenMinute = (now - offset * tenMinuteMillis) / tenMinuteMillis * tenMinuteMillis;
        // 格式化时间段为yyyyMMddHHmm（只保留10分钟级，比如202601111000）
        String timeSegment = new java.text.SimpleDateFormat("yyyyMMddHHmm")
                .format(new java.util.Date(currentTenMinute))
                .substring(0, 10); // 去掉最后一位分钟，保留10分钟级（比如1000→100，代表10:00）
        return "post:access:10min:" + timeSegment + ":" + postId;
    }

    /**
     * 获取当前1小时维度的Redis Key
     * 格式：post:access:1h:2026011110:1001（2026-01-11 10:00-11:00，帖子1001）
     */
    private String get1hRedisKey(String postId) {
        String timeSegment = new java.text.SimpleDateFormat("yyyyMMddHH")
                .format(new java.util.Date());
        return "post:access:1h:" + timeSegment + ":" + postId;
    }

    /**
     * 从Redis获取计数（为空则返回0）
     */
    private int getRedisCount(String redisKey) {
        Integer count = redisTemplate.opsForValue().get(redisKey);
        return count == null ? 0 : count;
    }
}
package com.yry.blog.myblogcommon.stat;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class PostAccessCounter {

    private static final String READ_COUNT_KEY_PREFIX = "article:read:count:";

    private final Cache<String, AtomicInteger> localAccessCache;

    @Autowired
    private RedisTemplate<String, Integer> redisTemplate;

    public PostAccessCounter() {
        this.localAccessCache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .maximumSize(10000)
                .build();
    }

    public void recordAccess(String postId) {
        Objects.requireNonNull(postId, "帖子ID不能为空");
        String localKey = "post:access:temp:" + postId;
        localAccessCache.get(localKey, k -> new AtomicInteger(0)).incrementAndGet();
        log.trace("记录帖子{}访问，本地计数+1", postId);
    }

    public void aggregateToRedis() {
        Map<String, AtomicInteger> allLocalCount = localAccessCache.asMap();
        if (allLocalCount.isEmpty()) {
            return;
        }

        for (Map.Entry<String, AtomicInteger> entry : allLocalCount.entrySet()) {
            String localKey = entry.getKey();
            int count = entry.getValue().get();
            if (count == 0) {
                continue;
            }

            String postId = localKey.replace("post:access:temp:", "");
            String redisKey10min = get10minRedisKey(postId);
            String redisKey1h = get1hRedisKey(postId);

            redisTemplate.opsForValue().increment(redisKey10min, count);
            redisTemplate.opsForValue().increment(redisKey1h, count);

            redisTemplate.expire(redisKey10min, 2, TimeUnit.HOURS);
            redisTemplate.expire(redisKey1h, 1, TimeUnit.DAYS);

            String countKey = READ_COUNT_KEY_PREFIX + postId;
            redisTemplate.opsForValue().increment(countKey, count);

            log.debug("聚合帖子{}的访问计数：{}次 → Redis", postId, count);

            localAccessCache.invalidate(localKey);
        }
    }

    public int getAccessCount(String postId, String timeType) {
        Objects.requireNonNull(postId, "帖子ID不能为空");
        Objects.requireNonNull(timeType, "时间段类型不能为空");

        int totalCount = 0;
        if ("10min".equals(timeType)) {
            String redisKey = get10minRedisKey(postId);
            totalCount = getRedisCount(redisKey);
        } else if ("1h".equals(timeType)) {
            for (int i = 0; i < 6; i++) {
                String redisKey = get10minRedisKey(postId, i);
                totalCount += getRedisCount(redisKey);
            }
        }

        log.debug("查询帖子{}近{}访问次数：{}次", postId, timeType, totalCount);
        return totalCount;
    }

    public Long getTotalReadCount() {
        Set<String> keys = redisTemplate.keys(READ_COUNT_KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return 0L;
        }

        long total = 0L;
        for (String key : keys) {
            Integer count = redisTemplate.opsForValue().get(key);
            if (count != null) {
                total += count;
            }
        }
        return total;
    }

    public int getReadCountIncrement(String postId) {
        String key = READ_COUNT_KEY_PREFIX + postId;
        Integer count = redisTemplate.opsForValue().get(key);
        return count == null ? 0 : count;
    }

    public String getReadCountKeyPrefix() {
        return READ_COUNT_KEY_PREFIX;
    }

    private String get10minRedisKey(String postId) {
        return get10minRedisKey(postId, 0);
    }

    private String get10minRedisKey(String postId, int offset) {
        long now = System.currentTimeMillis();
        long tenMinuteMillis = 10 * 60 * 1000L;
        long currentTenMinute = (now - offset * tenMinuteMillis) / tenMinuteMillis * tenMinuteMillis;
        String timeSegment = new java.text.SimpleDateFormat("yyyyMMddHHmm")
                .format(new java.util.Date(currentTenMinute))
                .substring(0, 10);
        return "post:access:10min:" + timeSegment + ":" + postId;
    }

    private String get1hRedisKey(String postId) {
        String timeSegment = new java.text.SimpleDateFormat("yyyyMMddHH")
                .format(new java.util.Date());
        return "post:access:1h:" + timeSegment + ":" + postId;
    }

    private int getRedisCount(String redisKey) {
        Integer count = redisTemplate.opsForValue().get(redisKey);
        return count == null ? 0 : count;
    }
}
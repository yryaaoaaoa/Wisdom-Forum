package com.yry.blog.myblogcommon.cache.lock;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;

/**
 * 基于Redisson的分布式锁实现（项目中实际使用）
 */
@Slf4j
@Component
public class RedisDistributedLock {

    private final RedissonClient redissonClient;

    // 构造器注入RedissonClient（需先配置Redisson）
    public RedisDistributedLock(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 尝试获取分布式锁
     * @param lockKey 锁Key
     * @param waitTime 等待时间（秒）
     * @param leaseTime 锁过期时间（秒）
     * @return 是否获取成功
     */
    public boolean tryLock(String lockKey, long waitTime, long leaseTime) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            // 尝试获取锁：最多等waitTime秒，拿到后leaseTime秒自动释放（避免死锁）
            return lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("获取分布式锁失败，key:{}", lockKey, e);
            return false;
        }
    }

    /**
     * 释放分布式锁
     * @param lockKey 锁Key
     */
    public void unlock(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        // 只有持有锁的线程才能释放（避免误解锁）
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
            log.debug("释放分布式锁成功，key:{}", lockKey);
        }
    }
}
package com.yry.blog.myblogcommon.aop;

import com.yry.blog.myblogcommon.annotation.DistributedLock;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 分布式锁切面类
 * 用于在方法级别自动添加Redisson分布式锁，防止并发执行
 */
@Aspect
@Component
public class DistributedLockAspect {
    /**
     * 注入Redisson客户端，用于获取分布式锁
     */
    @Autowired
    private RedissonClient redissonClient;

    /**
     * 环绕通知方法，拦截带有@DistributedLock注解的方法
     *
     * @param point 连接点，代表被拦截的方法
     * @param distributedLock 分布式锁注解实例
     * @return 原方法的返回值
     * @throws Throwable 方法执行异常
     */
    @Around("@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint point, DistributedLock distributedLock) throws Throwable {
        // 从注解中获取锁的key
        String lockKey = distributedLock.key();
        // 通过Redisson获取锁对象
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 尝试获取锁，等待指定时间，锁持有指定时间
            boolean isLocked = lock.tryLock(distributedLock.waitTime(), distributedLock.leaseTime(), TimeUnit.SECONDS);
            if (!isLocked) {
                // 获取锁失败，抛出异常
                throw new RuntimeException("获取分布式锁失败");
            }
            // 获取锁成功，执行原方法
            return point.proceed();
        } finally {
            // 确保当前线程持有锁时才释放锁
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}


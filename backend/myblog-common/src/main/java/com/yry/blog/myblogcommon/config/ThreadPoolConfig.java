package com.yry.blog.myblogcommon.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Configuration
public class ThreadPoolConfig {

    /**
     * 核心线程数
     */
    private static final int CORE_POOL_SIZE = 5;
    /**
     * 最大线程数
     */
    private static final int MAX_POOL_SIZE = 10;
    /**
     * 队列容量
     */
    private static final int QUEUE_CAPACITY = 200;
    /**
     * 线程活跃时间（秒）
     */
    private static final int KEEP_ALIVE_SECONDS = 60;
    /**
     * 线程名前缀
     */
    private static final String THREAD_NAME_PREFIX = "myblog-";
    /**
     * 任务执行超时警告阈值（毫秒）
     */
    private static final long TASK_TIMEOUT_WARN_THRESHOLD = 200;

    @Bean(name = "myblogTaskExecutor")
    public ThreadPoolExecutor myblogTaskExecutor() {
        return new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAX_POOL_SIZE,
                KEEP_ALIVE_SECONDS,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(QUEUE_CAPACITY),
                new NamedThreadFactory(THREAD_NAME_PREFIX),
                new RejectedHandler()
        ) {
            @Override
            protected void beforeExecute(Thread t, Runnable r) {
                // 任务执行前包装，添加监控和异常处理
                super.beforeExecute(t, wrapTask(r));
            }
        };
    }

    /**
     * 包装任务，添加耗时监控和统一异常处理
     */
    private Runnable wrapTask(Runnable runnable) {
        return () -> {
            long start = System.currentTimeMillis();
            try {
                runnable.run();
            } catch (Exception e) {
                log.error("线程池任务执行异常", e);
                throw e; // 重新抛出异常，保证线程池的异常处理机制正常工作
            } finally {
                long cost = System.currentTimeMillis() - start;
                if (cost > TASK_TIMEOUT_WARN_THRESHOLD) {
                    log.warn("任务执行耗时过长：{} ms，任务：{}", cost, runnable.toString());
                }
            }
        };
    }

    /**
     * 自定义线程工厂，用于创建具有自定义名称的线程
     */
    static class NamedThreadFactory implements ThreadFactory {
        private final AtomicInteger threadIndex = new AtomicInteger(1);
        private final String namePrefix;

        NamedThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, namePrefix + threadIndex.getAndIncrement());
            thread.setDaemon(false); // 非守护线程，确保任务能执行完成
            thread.setPriority(Thread.NORM_PRIORITY); // 正常优先级
            return thread;
        }
    }

    /**
     * 自定义拒绝策略，当线程池饱和时记录日志并抛出异常
     */
    static class RejectedHandler implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            log.error("线程池[{}]饱和，任务被拒绝！当前状态：核心线程数={}, 最大线程数={}, 活跃线程数={}, 队列容量={}, 队列当前大小={}",
                    THREAD_NAME_PREFIX,
                    executor.getCorePoolSize(),
                    executor.getMaximumPoolSize(),
                    executor.getActiveCount(),
                    executor.getQueue().size(),
                    executor.getQueue().remainingCapacity() + executor.getQueue().size()
            );
            throw new RejectedExecutionException("线程池饱和，任务执行被拒绝: " + r.toString());
        }
    }
}
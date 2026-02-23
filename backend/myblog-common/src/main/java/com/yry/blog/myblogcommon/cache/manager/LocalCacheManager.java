package com.yry.blog.myblogcommon.cache.manager;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.yry.blog.myblogcommon.cache.constants.CacheConstants;
import com.yry.blog.myblogcommon.cache.lock.RedisDistributedLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 本地缓存管理器实现（Caffeine一级缓存）
 * 适配智汇论坛分布式场景：二级缓存协同、缓存穿透/击穿防护、高并发兼容
 */
@Slf4j
@Component
public class LocalCacheManager {

    // 普通缓存：用于存储非热点高频数据（如用户基础信息）
    private final Cache<String, Object> normalCache;

    // 加载式缓存：用于热点数据（如热门帖子），支持自动加载+重试机制
    private final LoadingCache<String, Object> hotCache;

    // 分布式锁前缀（配合Redis分布式锁，解决缓存击穿）
    private static final String LOCK_KEY_PREFIX = "cache:lock:";

    // 空值缓存标识（解决缓存穿透）
    private static final Object EMPTY_VALUE = new Object();

    // 本地锁（防止单节点内高并发重复加载数据）
    private final ReentrantLock localLock = new ReentrantLock();

    // 注入RedisTemplate（二级缓存）和分布式锁
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisDistributedLock redisDistributedLock;

    // 构造器注入依赖（替代原抽象的DistributedLock）
    @Autowired
    public LocalCacheManager(RedisTemplate<String, Object> redisTemplate,
                             RedisDistributedLock redisDistributedLock) {
        // 初始化缓存配置
        this.normalCache = Caffeine.newBuilder()
                .maximumSize(1000) // 最大缓存1000条（根据服务器内存调整）
                .expireAfterWrite(10 + (int) (Math.random() * 5), TimeUnit.MINUTES) // 10-15分钟随机过期
                .recordStats() // 开启缓存统计（命中率、加载时间等）
                .build();

        this.hotCache = Caffeine.newBuilder()
                .maximumSize(500) // 热点数据量更少，优先保证访问速度
                .expireAfterWrite(5 + (int) (Math.random() * 3), TimeUnit.MINUTES) // 5-8分钟随机过期
                .refreshAfterWrite(2, TimeUnit.MINUTES) // 2分钟无访问自动刷新
                .recordStats()
                .build(this::loadDataFromSecondaryCache); // 替换lambda为方法引用，避免编译问题

        // 赋值依赖
        this.redisTemplate = redisTemplate;
        this.redisDistributedLock = redisDistributedLock;
    }

    /**
     * 核心获取方法：自动选择缓存类型，支持穿透/击穿防护
     * @param key 缓存键（建议格式：业务模块:唯一标识，如 post:hot:1001）
     * @param isHotData 是否热点数据（true=热门帖子，false=普通数据）
     * @return 缓存值（null表示无数据，非EMPTY_VALUE）
     */
    public Object get(String key, boolean isHotData) {
        Objects.requireNonNull(key, "缓存key不能为空");

        // 第一步：优先从本地缓存获取
        Object value = isHotData ? hotCache.getIfPresent(key) : normalCache.getIfPresent(key);

        // 第二步：缓存命中（过滤空值标识）
        if (value != null) {
            return value == EMPTY_VALUE ? null : value;
        }

        // 第三步：缓存未命中，加载数据（区分热点/普通数据）
        try {
            return isHotData ? loadHotData(key) : loadNormalData(key);
        } catch (Exception e) {
            log.error("获取缓存key:{}失败", key, e);
            return null;
        }
    }

    /**
     * 存储缓存（普通数据）
     * @param key 缓存键
     * @param value 缓存值（null将被转为EMPTY_VALUE存储）
     */
    public void put(String key, Object value) {
        Objects.requireNonNull(key, "缓存key不能为空");
        Object cacheValue = value == null ? EMPTY_VALUE : value;
        normalCache.put(key, cacheValue);
        log.debug("本地缓存存储成功，key:{}", key);
    }

    /**
     * 存储热点缓存
     * @param key 缓存键
     * @param value 缓存值（非null，热点数据不允许空）
     */
    public void putHotData(String key, Object value) {
        Objects.requireNonNull(key, "缓存key不能为空");
        Objects.requireNonNull(value, "热点缓存值不能为null");
        hotCache.put(key, value);
        log.debug("热点缓存存储成功，key:{}", key);
    }

    // 批量获取本地缓存（核心新增方法）
    public Map<String, Object> multiGet(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return new HashMap<>();
        }
        // 选择对应的缓存池（热点/普通）

        // Caffeine批量获取：返回Map<key, value>，未命中的key不会出现在Map中
        return normalCache.getAllPresent(keys);
    }

    // 批量写入本地缓存（核心新增方法）
    public void multiPut(Map<String, Object> keyValueMap) {
        if (keyValueMap == null || keyValueMap.isEmpty()) {
            return;
        }
        normalCache.putAll(keyValueMap);
    }

    /**
     * 删除缓存（支持普通/热点缓存）
     * @param key 缓存键
     * @param isHotData 是否热点数据
     */
    public void evict(String key, boolean isHotData) {
        Objects.requireNonNull(key, "缓存key不能为空");
        if (isHotData) {
            hotCache.invalidate(key);
        } else {
            normalCache.invalidate(key);
        }
        log.debug("本地缓存删除成功，key:{}", key);
    }

    /**
     * 清空所有缓存（谨慎使用，仅用于系统重启/数据全量更新场景）
     */
    public void clear() {
        normalCache.invalidateAll();
        hotCache.invalidateAll();
        log.warn("本地缓存已全部清空");
    }

    /**
     * 获取缓存统计信息（用于监控：命中率、加载次数等）
     * @param isHotData 是否热点缓存
     * @return 统计信息
     */
    public com.github.benmanes.caffeine.cache.stats.CacheStats stats(boolean isHotData) {
        return isHotData ? hotCache.stats() : normalCache.stats();
    }

    // ------------------------------ 私有辅助方法 ------------------------------
    /**
     * 加载普通数据：本地锁+空值缓存，解决穿透/击穿
     */
    private Object loadNormalData(String key) {
        localLock.lock();
        try {
            // 双重检查：防止锁等待期间缓存已被其他线程加载
            Object value = normalCache.getIfPresent(key);
            if (value != null) {
                return value == EMPTY_VALUE ? null : value;
            }

            // 从二级缓存（Redis）加载数据
            Object dataFromRedis = redisTemplate.opsForValue().get(key);
            if (dataFromRedis == null) {
                // 缓存穿透防护：存储空值
                normalCache.put(key, EMPTY_VALUE);
                log.debug("缓存穿透防护：key:{} 无数据，存储空值标识", key);
                return null;
            }

            // 存储到本地缓存
            normalCache.put(key, dataFromRedis);
            return dataFromRedis;
        } finally {
            localLock.unlock();
        }
    }

    /**
     * 加载热点数据：本地锁+分布式锁，解决高并发击穿
     */
    private Object loadHotData(String key) {
        localLock.lock();
        try {
            // 双重检查本地缓存
            Object value = hotCache.getIfPresent(key);
            if (value != null) {
                return value == EMPTY_VALUE ? null : value;
            }

            // 分布式锁：防止集群内多节点同时加载热点数据
            String lockKey = LOCK_KEY_PREFIX + key;
            boolean lockAcquired = false;
            try {
                // 尝试获取分布式锁（超时3秒，过期5秒）
                lockAcquired = redisDistributedLock.tryLock(lockKey, 3, 5);
                if (!lockAcquired) {
                    log.warn("获取分布式锁失败，key:{}，返回降级数据", key);
                    return getDegradeData(key); // 降级：返回旧数据或默认值
                }

                // 从Redis加载热点数据
                Object dataFromRedis = redisTemplate.opsForValue().get(key);
                if (dataFromRedis == null) {
                    // 热点数据不应为空，存储空值并报警
                    hotCache.put(key, EMPTY_VALUE);
                    log.error("热点缓存key:{} 无数据，可能存在异常", key);
                    return null;
                }

                // 存储到热点缓存
                hotCache.put(key, dataFromRedis);
                return dataFromRedis;
            } finally {
                if (lockAcquired) {
                    redisDistributedLock.unlock(lockKey); // 释放分布式锁（修复unlock方法解析问题）
                }
            }
        } finally {
            localLock.unlock();
        }
    }

    /**
     * 从二级缓存加载数据（LoadingCache自动触发的加载逻辑）
     */
    private Object loadDataFromSecondaryCache(String key) {
        try {
            Object data = redisTemplate.opsForValue().get(key);
            return data == null ? EMPTY_VALUE : data;
        } catch (Exception e) {
            log.error("从Redis加载数据失败，key:{}", key, e);
            // 加载失败时返回空值，避免缓存雪崩
            return EMPTY_VALUE;
        }
    }

    /**
     * 降级数据获取（缓存加载失败时兜底）
     */
    private Object getDegradeData(String key) {
        // 示例：返回本地缓存旧数据
        Object oldData = hotCache.getIfPresent(key);
        if (oldData != null && oldData != EMPTY_VALUE) {
            return oldData;
        }
        // 无旧数据时返回默认降级数据
        log.debug("缓存降级：key:{} 返回默认数据", key);
        return getDefaultDegradeData(key);
    }

    /**
     * 获取默认降级数据（根据业务定制）
     */
    private Object getDefaultDegradeData(String key) {
        // 示例：热门帖子返回固定默认数据
        if (key.startsWith("post:hot:")) {
            return "[{id:1001, title:'热门帖子默认标题'}]";
        }
        return null;
    }
    // 工具类中新增方法：打印所有缓存内容
    public void printAllCacheContent() {
        // asMap()返回缓存的线程安全视图，遍历不会阻塞其他操作
        Map<String, Object> cacheMap = normalCache.asMap();
        if (cacheMap.isEmpty()) {
            System.out.println("Caffeine缓存为空");
            return;
        }
        System.out.println("=== Caffeine缓存内容 ===");
        System.out.println("缓存总条数：" + cacheMap.size());
        for (Map.Entry<String, Object> entry : cacheMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            // 针对你的分页场景，可过滤文章缓存键
            if (key.startsWith(CacheConstants.CACHE_PREFIX_ARTICLE)) {
                System.out.printf("Key: %s, Value: %s%n", key, value);
            }
        }
    }
}
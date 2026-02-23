package com.yry.blog.myblogcommon.cache.util;

import com.yry.blog.myblogcommon.cache.enums.CacheTypeEnum;
import com.yry.blog.myblogcommon.cache.manager.LocalCacheManager;
import com.yry.blog.myblogcommon.cache.manager.RedisCacheManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 缓存工具类（统一封装两级缓存：本地Caffeine + Redis）
 * 核心：通过CacheTypeEnum控制缓存存储位置，适配所有业务场景
 */
@Slf4j
@Component
public class CacheUtil {

    private final LocalCacheManager localCacheManager;
    private final RedisCacheManager redisCacheManager;

    // 构造器注入（推荐，避免@Autowired）
    public CacheUtil(LocalCacheManager localCacheManager, RedisCacheManager redisCacheManager) {
        this.localCacheManager = localCacheManager;
        this.redisCacheManager = redisCacheManager;
    }

    /**
     * 泛型版获取缓存（避免类型转换，更安全）
     * 先查本地缓存 → 再查Redis → Redis命中则回写本地缓存
     *
     * @param key 缓存键
     * @param clazz 返回值类型
     * @param cacheType 缓存类型（LOCAL/REDIS/BOTH）
     * @return 缓存值（null表示未命中）
     */
    public <T> T get(String key, Class<T> clazz, CacheTypeEnum cacheType,Boolean isHot) {
        // 空值校验（避免空指针）
        if (key == null || clazz == null || cacheType == null) {
            log.warn("缓存查询参数异常：key={}, clazz={}, cacheType={}", key, clazz, cacheType);
            return null;
        }

        try {
            switch (cacheType) {
                case LOCAL:
                    Object localValue = localCacheManager.get(key,isHot);
                    return clazz.cast(localValue);
                case REDIS:
                    return redisCacheManager.get(key, clazz);
                case BOTH:
                default:
                    // 两级缓存核心逻辑：本地优先
                    Object value = localCacheManager.get(key,isHot);
                    if (value != null) {
                        return clazz.cast(value);
                    }
                    // Redis命中后回写本地缓存（提升下次访问速度）
                    T redisValue = redisCacheManager.get(key, clazz);
                    if (redisValue != null) {
                        localCacheManager.put(key, redisValue);
                        log.debug("Redis缓存命中，回写本地缓存：{}", key);
                    }
                    return redisValue;
            }
        } catch (ClassCastException e) {
            log.error("缓存值类型转换失败：key={}, targetClass={}", key, clazz.getName(), e);
            return null;
        } catch (Exception e) {
            log.error("缓存查询异常：key={}", key, e);
            return null;
        }
    }

    public <T> List<T> multiGet(Collection<String> keys, Class<T> clazz) {
        // 1. 空值校验：快速返回空列表，避免无效操作
        if (keys == null || keys.isEmpty()) {
            log.debug("批量缓存查询：入参keys为空，直接返回空列表");
            return Collections.emptyList();
        }
        if (clazz == null) {
            log.warn("批量缓存查询：目标类型clazz为null，keys={}", keys);
            return Collections.emptyList();
        }

        try {
            // 2. 第一步：批量查本地缓存（返回Map<key, value>）
            Map<String, Object> localHitMap = localCacheManager.multiGet(keys);
            localCacheManager.printAllCacheContent();
            // 存储最终结果（保证顺序）
            Map<String, T> resultMap = new HashMap<>(keys.size());

            // 3. 处理本地缓存命中的key，安全转换类型
            for (Map.Entry<String, Object> entry : localHitMap.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                try {
                    // 安全类型转换，避免ClassCastException
                    resultMap.put(key, clazz.cast(value));
                } catch (ClassCastException e) {
                    log.error("批量缓存查询：本地缓存值类型转换失败，key={}, targetClass={}",
                            key, clazz.getName(), e);
                }
            }

            // 4. 收集本地缓存未命中的key，准备查Redis
            List<String> redisMissKeys = keys.stream()
                    .filter(key -> !localHitMap.containsKey(key))
                    .collect(Collectors.toList());

            // 5. 第二步：批量查Redis（仅查本地未命中的key）
            if (!redisMissKeys.isEmpty()) {
                List<T> redisHitList = redisCacheManager.multiGet(redisMissKeys, clazz);
                // 构建Redis命中的key-value映射（保证key和value顺序对应）
                for (int i = 0; i < redisMissKeys.size(); i++) {
                    String key = redisMissKeys.get(i);
                    T value = redisHitList.get(i);
                    if (value != null) {
                        resultMap.put(key, value);
                        // 可选：Redis命中后回写本地缓存，提升下次访问速度
                        localCacheManager.put(key, value);
                    }
                }
                log.debug("批量缓存查询：Redis补充查询{}个key，命中{}个",
                        redisMissKeys.size(), resultMap.size() - localHitMap.size());
            }

            // 6. 按入参keys顺序组装最终结果（核心：保证顺序一致）
            List<T> finalResult = new ArrayList<>(keys.size());
            for (String key : keys) {
                finalResult.add(resultMap.get(key));
            }

            log.debug("批量缓存查询完成：总请求keys数={}，本地命中={}，最终命中={}",
                    keys.size(), localHitMap.size(), resultMap.size());
            return finalResult;

        } catch (Exception e) {
            // 兜底：捕获所有异常，避免影响主流程，降级为仅查Redis
            log.error("批量缓存查询异常，降级为仅查询Redis，keys={}", keys, e);
            return redisCacheManager.multiGet(keys, clazz);
        }
    }

    /**
     * 简化版get（默认返回Object，兼容旧代码）
     */
    public Object get(String key, CacheTypeEnum cacheType) {
        return get(key, Object.class, cacheType, null);
    }



    /**
     * 设置缓存（支持不同缓存类型+过期时间）
     *
     * @param key 缓存键
     * @param value 缓存值
     * @param timeout 过期时间（Redis有效，本地缓存过期时间在LocalCacheManager初始化时配置）
     * @param unit 时间单位
     * @param cacheType 缓存类型
     */
    public void put(String key, Object value, long timeout, TimeUnit unit, CacheTypeEnum cacheType) {
        if (key == null || value == null || cacheType == null) {
            log.warn("缓存存入参数异常：key={}, value={}, cacheType={}", key, value, cacheType);
            return;
        }

        try {
            switch (cacheType) {
                case LOCAL:
                    // 本地缓存的过期时间由LocalCacheManager统一配置（比如1分钟），这里无需传
                    localCacheManager.put(key, value);
                    log.debug("存入本地缓存：{}", key);
                    break;
                case REDIS:
                    redisCacheManager.put(key, value, timeout, unit);
                    log.debug("存入Redis缓存：{}，过期时间：{} {}", key, timeout, unit);
                    break;
                case BOTH:
                default:
                    localCacheManager.put(key, value);
                    redisCacheManager.put(key, value, timeout, unit);
                    log.debug("存入两级缓存：{}", key);
                    break;
            }
        } catch (Exception e) {
            log.error("缓存存入异常：key={}", key, e);
        }
    }



    /**
     * 删除缓存（支持指定类型）
     */
    public void evict(String key, CacheTypeEnum cacheType,Boolean isHot) {
        if (key == null || cacheType == null) {
            log.warn("缓存删除参数异常：key={}, cacheType={}", key, cacheType);
            return;
        }

        try {
            switch (cacheType) {
                case LOCAL:
                    localCacheManager.evict(key, isHot);
                    log.debug("删除本地缓存：{}", key);
                    break;
                case REDIS:
                    redisCacheManager.evict(key);
                    log.debug("删除Redis缓存：{}", key);
                    break;
                case BOTH:
                default:
                    localCacheManager.evict(key, isHot);
                    redisCacheManager.evict(key);
                    log.debug("删除两级缓存：{}", key);
                    break;
            }
        } catch (Exception e) {
            log.error("缓存删除异常：key={}", key, e);
        }
    }

    /**
     * 清空缓存（慎用！建议只在测试/重置场景用）
     */
    public void clear(CacheTypeEnum cacheType) {
        if (cacheType == null) {
            log.warn("缓存清空参数异常：cacheType=null");
            return;
        }

        try {
            switch (cacheType) {
                case LOCAL:
                    localCacheManager.clear();
                    log.info("清空所有本地缓存");
                    break;
                case REDIS:
                    redisCacheManager.clear();
                    log.info("清空所有Redis缓存");
                    break;
                case BOTH:
                default:
                    localCacheManager.clear();
                    redisCacheManager.clear();
                    log.info("清空所有两级缓存");
                    break;
            }
        } catch (Exception e) {
            log.error("缓存清空异常", e);
        }
    }

    /**
     * 生成规范的缓存键（避免键名混乱）
     * 格式：prefix:param1:param2:...
     * 示例：token:user:1001、post:hot:1002
     */
    public String generateKey(String prefix, Object... params) {
        if (prefix == null) {
            throw new IllegalArgumentException("缓存键前缀不能为空");
        }
        StringBuilder keyBuilder = new StringBuilder(prefix);
        for (Object param : params) {
            if (param != null) {
                keyBuilder.append(":").append(param);
            }
        }
        String key = keyBuilder.toString();
        log.debug("生成缓存键：{}", key);
        return key;
    }

    // ------------------------------ Redis集合操作（专属） ------------------------------
    public void removeFromSet(String key, Object... values) {
        if (key == null || values == null) {
            log.warn("Redis集合删除参数异常：key={}, values={}", key, values);
            return;
        }
        redisCacheManager.removeFromSet(key, values);
    }

    public void addToSet(String key, Object... values) {
        if (key == null || values == null) {
            log.warn("Redis集合添加参数异常：key={}, values={}", key, values);
            return;
        }
        redisCacheManager.addToSet(key, values);
    }

}
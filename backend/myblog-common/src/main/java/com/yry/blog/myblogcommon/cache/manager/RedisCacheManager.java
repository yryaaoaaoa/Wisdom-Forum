package com.yry.blog.myblogcommon.cache.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Redis缓存管理器实现
 * 基于Redis实现的二级缓存，支持多种数据类型
 */
@Slf4j // 新增日志，方便排查问题
@Component
public class RedisCacheManager {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisCacheManager(RedisTemplate<String, Object> redisTemplate,
                             @Qualifier("redisObjectMapper") ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    // --- String/Value Operations ---
    /**
     * 获取缓存值 (String/Value)
     * @param key 缓存键
     * @return 缓存值
     */
    public Object get(String key) {
        if (key == null) {
            log.warn("Redis get操作：key不能为空");
            return null;
        }
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Redis get操作异常，key:{}", key, e);
            return null;
        }
    }

    /**
     * 泛型版get（避免类型转换，更安全）
     */
    public <T> T get(String key, Class<T> clazz) {
        Object value = get(key);
        if (value == null) {
            return null;
        }
        try {
            if (value instanceof java.util.LinkedHashMap) {
                return objectMapper.convertValue(value, clazz);
            }
            return clazz.cast(value);
        } catch (Exception e) {
            log.error("Redis值类型转换失败，key:{}, targetClass:{}", key, clazz.getName(), e);
            return null;
        }
    }

    /**
     * 设置缓存值 (String/Value)
     * @param key 缓存键
     * @param value 缓存值
     * @param timeout 过期时间
     * @param unit 时间单位
     */
    public void put(String key, Object value, long timeout, TimeUnit unit) {
        if (key == null || value == null) {
            log.warn("Redis put操作：key或value不能为空");
            return;
        }
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
            log.debug("Redis put成功，key:{}, 过期时间:{} {}", key, timeout, unit);
        } catch (Exception e) {
            log.error("Redis put操作异常，key:{}", key, e);
        }
    }

    /**
     * 设置缓存值（使用默认过期时间） (String/Value)
     * @param key 缓存键
     * @param value 缓存值
     */
    public void put(String key, Object value) {
        // 默认1小时过期，可后续抽成配置项
        put(key, value, 1, TimeUnit.HOURS);
    }

    // --- Hash Operations ---
    public void putInHash(String key, String hashKey, Object value) {
        if (key == null || hashKey == null || value == null) {
            log.warn("Redis putInHash操作：key/hashKey/value不能为空");
            return;
        }
        try {
            redisTemplate.opsForHash().put(key, hashKey, value);
            log.debug("Redis putInHash成功，key:{}, hashKey:{}", key, hashKey);
        } catch (Exception e) {
            log.error("Redis putInHash操作异常，key:{}, hashKey:{}", key, hashKey, e);
        }
    }

    public void putInHash(String key, String hashKey, Object value, long timeout, TimeUnit unit) {
        putInHash(key, hashKey, value);
        try {
            redisTemplate.expire(key, timeout, unit);
            log.debug("Redis putInHash设置过期时间，key:{}, 过期时间:{} {}", key, timeout, unit);
        } catch (Exception e) {
            log.error("Redis putInHash设置过期时间异常，key:{}", key, e);
        }
    }

    public Object getFromHash(String key, String hashKey) {
        if (key == null || hashKey == null) {
            log.warn("Redis getFromHash操作：key/hashKey不能为空");
            return null;
        }
        try {
            return redisTemplate.opsForHash().get(key, hashKey);
        } catch (Exception e) {
            log.error("Redis getFromHash操作异常，key:{}, hashKey:{}", key, hashKey, e);
            return null;
        }
    }

    /**
     * 泛型版getFromHash
     */
    public <T> T getFromHash(String key, String hashKey, Class<T> clazz) {
        Object value = getFromHash(key, hashKey);
        if (value == null) {
            return null;
        }
        try {
            if (value instanceof java.util.LinkedHashMap) {
                return objectMapper.convertValue(value, clazz);
            }
            return clazz.cast(value);
        } catch (Exception e) {
            log.error("Redis Hash值类型转换失败，key:{}, hashKey:{}, targetClass:{}", key, hashKey, clazz.getName(), e);
            return null;
        }
    }

    public Map<Object, Object> getHashEntries(String key) {
        if (key == null) {
            log.warn("Redis getHashEntries操作：key不能为空");
            return Collections.emptyMap();
        }
        try {
            return redisTemplate.opsForHash().entries(key);
        } catch (Exception e) {
            log.error("Redis getHashEntries操作异常，key:{}", key, e);
            return Collections.emptyMap();
        }
    }

    public Long deleteFromHash(String key, Object... hashKeys) {
        if (key == null || hashKeys == null || hashKeys.length == 0) {
            log.warn("Redis deleteFromHash操作：key或hashKeys不能为空");
            return 0L;
        }
        try {
            Long deleteCount = redisTemplate.opsForHash().delete(key, hashKeys);
            log.debug("Redis deleteFromHash成功，key:{}, 删除数量:{}", key, deleteCount);
            return deleteCount;
        } catch (Exception e) {
            log.error("Redis deleteFromHash操作异常，key:{}", key, e);
            return 0L;
        }
    }

    public boolean hasHashKey(String key, String hashKey) {
        if (key == null || hashKey == null) {
            log.warn("Redis hasHashKey操作：key/hashKey不能为空");
            return false;
        }
        try {
            return redisTemplate.opsForHash().hasKey(key, hashKey);
        } catch (Exception e) {
            log.error("Redis hasHashKey操作异常，key:{}, hashKey:{}", key, hashKey, e);
            return false;
        }
    }

    // --- List Operations ---
    public Long leftPushToList(String key, Object value) {
        if (key == null || value == null) {
            log.warn("Redis leftPushToList操作：key/value不能为空");
            return 0L;
        }
        try {
            Long size = redisTemplate.opsForList().leftPush(key, value);
            log.debug("Redis leftPushToList成功，key:{}, 列表长度:{}", key, size);
            return size;
        } catch (Exception e) {
            log.error("Redis leftPushToList操作异常，key:{}", key, e);
            return 0L;
        }
    }

    public Long leftPushAllToList(String key, Object... values) {
        if (key == null || values == null || values.length == 0) {
            log.warn("Redis leftPushAllToList操作：key或values不能为空");
            return 0L;
        }
        try {
            Long size = redisTemplate.opsForList().leftPushAll(key, values);
            log.debug("Redis leftPushAllToList成功，key:{}, 列表长度:{}", key, size);
            return size;
        } catch (Exception e) {
            log.error("Redis leftPushAllToList操作异常，key:{}", key, e);
            return 0L;
        }
    }

    public Long rightPushToList(String key, Object value) {
        if (key == null || value == null) {
            log.warn("Redis rightPushToList操作：key/value不能为空");
            return 0L;
        }
        try {
            Long size = redisTemplate.opsForList().rightPush(key, value);
            log.debug("Redis rightPushToList成功，key:{}, 列表长度:{}", key, size);
            return size;
        } catch (Exception e) {
            log.error("Redis rightPushToList操作异常，key:{}", key, e);
            return 0L;
        }
    }

    public Object leftPopFromList(String key) {
        if (key == null) {
            log.warn("Redis leftPopFromList操作：key不能为空");
            return null;
        }
        try {
            return redisTemplate.opsForList().leftPop(key);
        } catch (Exception e) {
            log.error("Redis leftPopFromList操作异常，key:{}", key, e);
            return null;
        }
    }

    public Object rightPopFromList(String key) {
        if (key == null) {
            log.warn("Redis rightPopFromList操作：key不能为空");
            return null;
        }
        try {
            return redisTemplate.opsForList().rightPop(key);
        } catch (Exception e) {
            log.error("Redis rightPopFromList操作异常，key:{}", key, e);
            return null;
        }
    }

    public List<Object> getListRange(String key, long start, long end) {
        if (key == null) {
            log.warn("Redis getListRange操作：key不能为空");
            return Collections.emptyList();
        }
        try {
            return redisTemplate.opsForList().range(key, start, end);
        } catch (Exception e) {
            log.error("Redis getListRange操作异常，key:{}", key, e);
            return Collections.emptyList();
        }
    }

    public Long getListSize(String key) {
        if (key == null) {
            log.warn("Redis getListSize操作：key不能为空");
            return 0L;
        }
        try {
            return redisTemplate.opsForList().size(key);
        } catch (Exception e) {
            log.error("Redis getListSize操作异常，key:{}", key, e);
            return 0L;
        }
    }

    public Object getListElementByIndex(String key, long index) {
        if (key == null) {
            log.warn("Redis getListElementByIndex操作：key不能为空");
            return null;
        }
        try {
            return redisTemplate.opsForList().index(key, index);
        } catch (Exception e) {
            log.error("Redis getListElementByIndex操作异常，key:{}, index:{}", key, index, e);
            return null;
        }
    }

    // --- ZSet (Sorted Set) Operations ---
    public Boolean addToZSet(String key, Object value, double score) {
        if (key == null || value == null) {
            log.warn("Redis addToZSet操作：key/value不能为空");
            return false;
        }
        try {
            Boolean result = redisTemplate.opsForZSet().add(key, value, score);
            log.debug("Redis addToZSet成功，key:{}, value:{}, score:{}", key, value, score);
            return result;
        } catch (Exception e) {
            log.error("Redis addToZSet操作异常，key:{}, value:{}", key, value, e);
            return false;
        }
    }

    /**
     * 批量添加/更新ZSet成员分数（排行榜批量更新用）
     */
    public Long addAllToZSet(String key, Set<ZSetOperations.TypedTuple<Object>> tuples) {
        if (key == null || tuples == null || tuples.isEmpty()) {
            log.warn("Redis addAllToZSet操作：key或tuples不能为空");
            return 0L;
        }
        try {
            Long count = redisTemplate.opsForZSet().add(key, tuples);
            log.debug("Redis addAllToZSet成功，key:{}, 操作数量:{}", key, count);
            return count;
        } catch (Exception e) {
            log.error("Redis addAllToZSet操作异常，key:{}", key, e);
            return 0L;
        }
    }

    public Long getZSetSize(String key) {
        if (key == null) {
            log.warn("Redis getZSetSize操作：key不能为空");
            return 0L;
        }
        try {
            return redisTemplate.opsForZSet().zCard(key);
        } catch (Exception e) {
            log.error("Redis getZSetSize操作异常，key:{}", key, e);
            return 0L;
        }
    }

    public Set<Object> getZSetRange(String key, long start, long end) {
        if (key == null) {
            log.warn("Redis getZSetRange操作：key不能为空");
            return Collections.emptySet();
        }
        try {
            return redisTemplate.opsForZSet().range(key, start, end);
        } catch (Exception e) {
            log.error("Redis getZSetRange操作异常，key:{}", key, e);
            return Collections.emptySet();
        }
    }

    public Set<ZSetOperations.TypedTuple<Object>> getZSetRangeWithScores(String key, long start, long end) {
        if (key == null) {
            log.warn("Redis getZSetRangeWithScores操作：key不能为空");
            return Collections.emptySet();
        }
        try {
            return redisTemplate.opsForZSet().rangeWithScores(key, start, end);
        } catch (Exception e) {
            log.error("Redis getZSetRangeWithScores操作异常，key:{}", key, e);
            return Collections.emptySet();
        }
    }

    public Set<Object> getZSetReverseRange(String key, long start, long end) {
        if (key == null) {
            log.warn("Redis getZSetReverseRange操作：key不能为空");
            return Collections.emptySet();
        }
        try {
            return redisTemplate.opsForZSet().reverseRange(key, start, end);
        } catch (Exception e) {
            log.error("Redis getZSetReverseRange操作异常，key:{}", key, e);
            return Collections.emptySet();
        }
    }

    public Set<ZSetOperations.TypedTuple<Object>> getZSetReverseRangeWithScores(String key, long start, long end) {
        if (key == null) {
            log.warn("Redis getZSetReverseRangeWithScores操作：key不能为空");
            return Collections.emptySet();
        }
        try {
            return redisTemplate.opsForZSet().reverseRangeWithScores(key, start, end);
        } catch (Exception e) {
            log.error("Redis getZSetReverseRangeWithScores操作异常，key:{}", key, e);
            return Collections.emptySet();
        }
    }

    public Long removeFromZSet(String key, Object... values) {
        if (key == null || values == null || values.length == 0) {
            log.warn("Redis removeFromZSet操作：key或values不能为空");
            return 0L;
        }
        try {
            Long count = redisTemplate.opsForZSet().remove(key, values);
            log.debug("Redis removeFromZSet成功，key:{}, 删除数量:{}", key, count);
            return count;
        } catch (Exception e) {
            log.error("Redis removeFromZSet操作异常，key:{}", key, e);
            return 0L;
        }
    }

    public Double getZSetScore(String key, Object value) {
        if (key == null || value == null) {
            log.warn("Redis getZSetScore操作：key/value不能为空");
            return null;
        }
        try {
            return redisTemplate.opsForZSet().score(key, value);
        } catch (Exception e) {
            log.error("Redis getZSetScore操作异常，key:{}, value:{}", key, value, e);
            return null;
        }
    }

    /**
     * 获取成员在ZSet中的排名（按分数降序，从1开始）
     * @param key Redis key
     * @param value 成员
     * @return 排名（1=第一名，null=不存在）
     */
    public Long getZSetReverseRank(String key, Object value) {
        if (key == null || value == null) {
            log.warn("Redis getZSetReverseRank操作：key/value不能为空");
            return null;
        }
        try {
            Long rank = redisTemplate.opsForZSet().reverseRank(key, value);
            return rank == null ? null : rank + 1; // 转成从1开始的排名
        } catch (Exception e) {
            log.error("Redis getZSetReverseRank操作异常，key:{}, value:{}", key, value, e);
            return null;
        }
    }

    // --- Common Operations ---
    public void evict(String key) {
        if (key == null) {
            log.warn("Redis evict操作：key不能为空");
            return;
        }
        try {
            redisTemplate.delete(key);
            log.debug("Redis evict成功，key:{}", key);
        } catch (Exception e) {
            log.error("Redis evict操作异常，key:{}", key, e);
        }
    }

    // 兼容CacheUtil的delete方法名
    public void delete(String key) {
        evict(key);
    }

    public boolean hasKey(String key) {
        if (key == null) {
            log.warn("Redis hasKey操作：key不能为空");
            return false;
        }
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.error("Redis hasKey操作异常，key:{}", key, e);
            return false;
        }
    }

    /**
     * 清空所有缓存（慎用！仅测试环境使用）
     */
    @Deprecated
    public void clear() {
        try {
            redisTemplate.execute((RedisCallback<Void>) connection -> {
                connection.flushDb();
                return null;
            });
            log.error("警告：RedisCacheManager.clear() 已调用，清空了整个Redis DB！");
        } catch (Exception e) {
            log.error("Redis clear操作异常", e);
        }
    }

    // --- Set Operations ---
    public void removeFromSet(String key, Object... values) {
        if (key == null || values == null || values.length == 0) {
            log.warn("Redis removeFromSet操作：key或values不能为空");
            return;
        }
        try {
            redisTemplate.opsForSet().remove(key, values);
            log.debug("Redis removeFromSet成功，key:{}, 删除数量:{}", key, values.length);
        } catch (Exception e) {
            log.error("Redis removeFromSet操作异常，key:{}", key, e);
        }
    }

    public void addToSet(String key, Object... values) {
        if (key == null || values == null || values.length == 0) {
            log.warn("Redis addToSet操作：key或values不能为空");
            return;
        }
        try {
            redisTemplate.opsForSet().add(key, values);
            log.debug("Redis addToSet成功，key:{}, 添加数量:{}", key, values.length);
        } catch (Exception e) {
            log.error("Redis addToSet操作异常，key:{}", key, e);
        }
    }

    public Set<Object> getSetMembers(String key) {
        if (key == null) {
            log.warn("Redis getSetMembers操作：key不能为空");
            return Collections.emptySet();
        }
        try {
            return redisTemplate.opsForSet().members(key);
        } catch (Exception e) {
            log.error("Redis getSetMembers操作异常，key:{}", key, e);
            return Collections.emptySet();
        }
    }
    /**
     * 批量获取缓存值 (String/Value) - 对应Redis的MGET命令
     * @param keys 缓存键列表
     * @return 按传入顺序返回的缓存值列表（不存在的key对应位置返回null）
     */
    public List<Object> multiGet(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            log.warn("Redis multiGet操作：keys不能为空或空集合");
            return Collections.emptyList();
        }
        try {
            List<Object> result = redisTemplate.opsForValue().multiGet(keys);
            log.debug("Redis multiGet成功，获取key数量:{}", keys.size());
            return result;
        } catch (Exception e) {
            log.error("Redis multiGet操作异常，keys:{}", keys, e);
            return Collections.emptyList();
        }
    }

    /**
     * 泛型版批量获取（类型安全，统一转换为指定类型）
     * @param keys 缓存键列表
     * @param clazz 目标类型
     * @return 按传入顺序返回的泛型列表（转换失败/不存在的key对应位置返回null）
     */
    public <T> List<T> multiGet(Collection<String> keys, Class<T> clazz) {
        List<Object> values = multiGet(keys);
        if (values.isEmpty()) {
            return Collections.emptyList();
        }

        List<T> result = new ArrayList<>(values.size());
        for (Object value : values) {
            if (value == null) {
                result.add(null);
                continue;
            }
            try {
                if (value instanceof java.util.LinkedHashMap) {
                    result.add(objectMapper.convertValue(value, clazz));
                } else {
                    result.add(clazz.cast(value));
                }
            } catch (Exception e) {
                log.error("Redis批量值类型转换失败，value:{}, targetClass:{}", value, clazz.getName(), e);
                result.add(null);
            }
        }
        return result;
    }

    /**
     * 批量设置缓存值（带过期时间）- 可选补充（对应MSET + EXPIRE，按需使用）
     * @param keyValueMap key-value映射
     * @param timeout 过期时间
     * @param unit 时间单位
     */
    public void multiPut(Map<String, Object> keyValueMap, long timeout, TimeUnit unit) {
        if (keyValueMap == null || keyValueMap.isEmpty()) {
            log.warn("Redis multiPut操作：keyValueMap不能为空或空集合");
            return;
        }
        try {
            // 批量设置值（无过期时间）
            redisTemplate.opsForValue().multiSet(keyValueMap);
            // 批量设置过期时间（逐个设置，Redis本身无MSETEX命令，这是折中方案）
            for (String key : keyValueMap.keySet()) {
                redisTemplate.expire(key, timeout, unit);
            }
            log.debug("Redis multiPut成功，设置key数量:{}, 过期时间:{} {}", keyValueMap.size(), timeout, unit);
        } catch (Exception e) {
            log.error("Redis multiPut操作异常，key数量:{}", keyValueMap.size(), e);
        }
    }
    /**
     * Redis String计数器增减（点赞数统计核心）
     * @param key 缓存键
     * @param delta 增减值（正数加，负数减）
     * @return 增减后的值
     */
    public Long increment(String key, long delta) {
        if (key == null) {
            log.warn("Redis increment操作：key不能为空");
            return 0L;
        }
        try {
            Long result = redisTemplate.opsForValue().increment(key, delta);
            log.debug("Redis increment成功，key:{}, delta:{}, 结果:{}", key, delta, result);
            return result;
        } catch (Exception e) {
            log.error("Redis increment操作异常，key:{}, delta:{}", key, delta, e);
            return 0L;
        }
    }

    // 新增：获取底层的RedisTemplate（方便特殊场景调用，比如上面的like方法）
    public RedisTemplate<String, Object> getRedisTemplate() {
        return this.redisTemplate;
    }
    public boolean isMemberOfSet(String key, String value) {
     return redisTemplate.opsForSet().isMember(key, value);
 }
}
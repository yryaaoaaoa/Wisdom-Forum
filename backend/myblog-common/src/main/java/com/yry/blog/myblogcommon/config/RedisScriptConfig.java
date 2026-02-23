package com.yry.blog.myblogcommon.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.script.DefaultRedisScript;

/**
 * Redis Lua脚本配置类：注册全局可复用的Script Bean
 */
@Configuration
public class RedisScriptConfig {

    // 点赞操作的Lua脚本
    private static final String LIKE_LUA_SCRIPT = """
        -- 第一步：参数容错处理（把输入转成整数，避免类型错误）
           local userId = tonumber(ARGV[1]) or 0;        -- ARGV[1] = 前端传的用户ID（比如1）
           local expireSeconds = tonumber(ARGV[2]) or 86400;  -- ARGV[2] = 缓存过期时间（比如86400秒）
        
           
        
           -- 第三步：定义Redis的Key（从KEYS数组取，Java端传过来的）
           local usersKey = KEYS[1];  -- 存点赞用户的Set集合，比如 "like:users:1:12"（1=文章，12=文章ID）
           local countKey = KEYS[2];  -- 存点赞数的数字，比如 "like:count:1:12"
        
           -- 第四步：核心判断：用户是否已点赞
           local isLiked = redis.call('SISMEMBER', usersKey, userId);
           -- 解释：SISMEMBER = 检查集合里有没有这个用户ID → 返回1（已点赞）/0（未点赞）
        
           -- 第五步：分支1 → 用户已点赞 → 执行「取消点赞」逻辑
           if isLiked == 1 then
               -- 5.1 把用户从点赞集合里删掉（取消点赞）
               redis.call('SREM', usersKey, userId);
        
               -- 5.2 处理点赞数：先检查计数Key是否存在，不存在则设为0（避免减成负数）
               if redis.call('EXISTS', countKey) == 0 then
                   redis.call('SET', countKey, 0);  -- 计数Key不存在，初始化为0
               else
                   local currentCount = tonumber(redis.call('GET', countKey) or 0);
                   if currentCount > 0 then
                       redis.call('DECR', countKey);  -- 计数>0时，才减1（避免点赞数变成负数）
                   end
               end
        
               -- 5.3 重置缓存过期时间（避免操作后缓存永久存在）
               if redis.call('TTL', usersKey) == -1 then  -- TTL=-1 = 没有过期时间
                   redis.call('EXPIRE', usersKey, expireSeconds);  -- 设过期时间
               end
               if redis.call('TTL', countKey) == -1 then
                   redis.call('EXPIRE', countKey, expireSeconds);
               end
        
               -- 5.4 返回2 → 告诉Java：取消点赞成功
               return 2;
        
           -- 第六步：分支2 → 用户未点赞 → 执行「点赞」逻辑
           else
               -- 6.1 把用户加入点赞集合（标记已点赞）
               redis.call('SADD', usersKey, userId);
        
               -- 6.2 点赞数加1（INCR会自动创建key，初始值0+1=1）
               redis.call('INCR', countKey);
        
               -- 6.3 同样重置过期时间（和取消逻辑一致，保证缓存有效期）
               if redis.call('TTL', usersKey) == -1 then
                   redis.call('EXPIRE', usersKey, expireSeconds);
               end
               if redis.call('TTL', countKey) == -1 then
                   redis.call('EXPIRE', countKey, expireSeconds);
               end
        
               -- 6.4 返回1 → 告诉Java：点赞成功
               return 1;
           end
        """;

    // 注册点赞脚本的Bean（指定名称，避免冲突）
    @Bean("likeRedisScript")
    public DefaultRedisScript<Long> likeRedisScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(LIKE_LUA_SCRIPT);
        script.setResultType(Long.class); // 指定返回值为Long类型
        return script;
    }

}
package com.yry.blog.myblogcommon.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@Configuration
public class RedisScriptConfig {

    private static final String LIKE_LUA_SCRIPT = """
        local userId = ARGV[1];
        local expireSeconds = tonumber(ARGV[2]);
        
        local usersKey = KEYS[1];
        local countKey = KEYS[2];
        
        local isLiked = redis.call('SISMEMBER', usersKey, userId);
        
        if isLiked == 1 then
            redis.call('SREM', usersKey, userId);
        
            if redis.call('EXISTS', countKey) == 0 then
                redis.call('SET', countKey, 0);
            else
                local currentCount = tonumber(redis.call('GET', countKey) or 0);
                if currentCount > 0 then
                    redis.call('DECR', countKey);
                end
            end
        
            return 2;
        else
            redis.call('SADD', usersKey, userId);
            redis.call('INCR', countKey);
        
            return 1;
        end
        """;

    @Bean("likeRedisScript")
    public DefaultRedisScript<Long> likeRedisScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(LIKE_LUA_SCRIPT);
        script.setResultType(Long.class);
        return script;
    }
}

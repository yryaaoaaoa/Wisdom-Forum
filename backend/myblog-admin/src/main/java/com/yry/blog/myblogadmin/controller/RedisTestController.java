package com.yry.blog.myblogadmin.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/test")
public class RedisTestController {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @GetMapping("/redis")
    public String testRedis() {
        // 设置值
        redisTemplate.opsForValue().set("test_key", "Hello from Spring Boot", 60, TimeUnit.SECONDS);

        // 获取值
        String value = redisTemplate.opsForValue().get("test_key");

        return value;
    }
}


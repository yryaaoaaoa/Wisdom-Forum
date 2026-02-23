package com.yry.blog.myblogcommon.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Configuration
public class RedisConfig {

    // 第一步：自定义ObjectMapper，解决LocalDateTime序列化问题
    @Bean
    public ObjectMapper redisObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        // 1. 注册Java 8时间模块（核心：解决LocalDateTime不支持的问题）
        JavaTimeModule javaTimeModule = new JavaTimeModule();

        // 2. 自定义LocalDateTime序列化/反序列化格式（可选，但推荐，避免序列化后是数组）
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(dateTimeFormatter));
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(dateTimeFormatter));

        objectMapper.registerModule(javaTimeModule);

        // 3. 关闭时间戳序列化（让时间以字符串格式存储，而非数字）
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return objectMapper;
    }

    // 第二步：修改RedisTemplate配置，使用自定义的ObjectMapper
    @Bean
    public RedisTemplate<String, ?> redisTemplate(RedisConnectionFactory connectionFactory, ObjectMapper redisObjectMapper) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 替换默认的JSON序列化器为自定义的（带Java 8时间支持）
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(redisObjectMapper);

        // 保持原有Key序列化规则（字符串）
        template.setKeySerializer(RedisSerializer.string());
        // 替换Value序列化器为自定义的JSON序列化器
        template.setValueSerializer(jsonSerializer);
        template.setHashKeySerializer(RedisSerializer.string());
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
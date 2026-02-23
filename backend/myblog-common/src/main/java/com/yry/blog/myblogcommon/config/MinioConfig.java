package com.yry.blog.myblogcommon.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO配置类
 * 用于配置和初始化MinIO客户端实例
 */
@Configuration
public class MinioConfig {

    /**
     * MinIO服务端点URL
     */
    @Value("${minio.endpoint}")
    private String endpoint;

    /**
     * MinIO访问密钥
     */
    @Value("${minio.access-key}")
    private String accessKey;

    /**
     * MinIO私有密钥
     */
    @Value("${minio.secret-key}")
    private String secretKey;

    /**
     * 创建并配置MinIO客户端Bean
     * @return MinioClient实例
     */
    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}

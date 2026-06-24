package com.yry.blog.myblogcommon.config;

import com.yry.blog.myblogcommon.minio.MinioService;
import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "minio.endpoint")
public class MinioBucketInitializer {

    @Value("${minio.bucket-name}")
    private String bucketName;

    private final MinioService minioService;
    private final MinioClient minioClient;

    public MinioBucketInitializer(MinioService minioService, MinioClient minioClient) {
        this.minioService = minioService;
        this.minioClient = minioClient;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initBucket() {
        try {
            log.info("正在检查 MinIO 存储桶 [{}]...", bucketName);
            if (!minioService.bucketExists(bucketName)) {
                log.info("存储桶 [{}] 不存在，正在创建...", bucketName);
                minioService.createBucket(bucketName);
                log.info("存储桶 [{}] 创建成功", bucketName);
            } else {
                log.info("存储桶 [{}] 已存在", bucketName);
            }
            
            setBucketPolicy();
        } catch (Exception e) {
            log.warn("初始化 MinIO 存储桶失败: {}", e.getMessage());
        }
    }

    private void setBucketPolicy() {
        String policy = """
            {
                "Version": "2012-10-17",
                "Statement": [
                    {
                        "Effect": "Allow",
                        "Principal": {"AWS": ["*"]},
                        "Action": ["s3:GetObject"],
                        "Resource": ["arn:aws:s3:::%s/*"]
                    }
                ]
            }
            """.formatted(bucketName);

        try {
            SetBucketPolicyArgs args = SetBucketPolicyArgs.builder()
                    .bucket(bucketName)
                    .config(policy)
                    .build();
            minioClient.setBucketPolicy(args);
            log.info("存储桶 [{}] 已设置为公开读取", bucketName);
        } catch (Exception e) {
            log.warn("设置存储桶公开策略失败: {}", e.getMessage());
        }
    }
}

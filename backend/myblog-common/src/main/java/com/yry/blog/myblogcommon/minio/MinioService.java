package com.yry.blog.myblogcommon.minio;

import com.yry.blog.myblogcommon.exception.MinioException; // ← 正确导入自定义异常
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.Item;
import jakarta.activation.MimetypesFileTypeMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * MinIO文件存储服务类
 * 提供文件上传、下载、删除等核心功能
 * 基于MinIO SDK实现对象存储操作
 */
@Slf4j
@Service
public class MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;
    @Value("${minio.endpoint}")
    private String endpoint;

    public MinioService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    /**
     * 创建存储桶
     */
    public void createBucket(String bucketName) {
        if (!bucketExists(bucketName)) {
            MakeBucketArgs args = MakeBucketArgs.builder()
                    .bucket(bucketName)
                    .build();
            try {
                minioClient.makeBucket(args);
                log.info("MinIO 存储桶 [{}] 创建成功", bucketName);
            } catch (Exception e) {
                log.error("创建 MinIO 存储桶 [{}] 失败", bucketName, e);
                throw new MinioException("创建存储桶 [" + bucketName + "] 失败: " + e.getMessage(), e);
            }
        }
    }

    /**
     * 检查存储桶是否存在
     */
    public boolean bucketExists(String bucketName) {
        BucketExistsArgs args = BucketExistsArgs.builder()
                .bucket(bucketName)
                .build();
        try {
            return minioClient.bucketExists(args);
        } catch (Exception e) {
            log.warn("检查存储桶 [{}] 是否存在时发生异常，视为不存在", bucketName, e);
            return false; // 安全策略：异常时当作不存在
        }
    }

    /**
     * 上传文件到MinIO存储
     */
    public String uploadFile(MultipartFile file, String objectName) {
        try {
            PutObjectArgs args = PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build();

            minioClient.putObject(args);
            return getObjectUrl(objectName);
        } catch (Exception e) {
            log.error("上传文件 [{}] 到 MinIO 失败", objectName, e);
            throw new MinioException("文件上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 将字符串内容上传为 MinIO 中的一个文件
     */
    public String uploadStringAsFile(String content, String objectName) {
        String contentType = getContentTypeByFileName(objectName);
        try (InputStream stream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))) {
            PutObjectArgs args = PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(stream, content.getBytes(StandardCharsets.UTF_8).length, -1)
                    .contentType(contentType)
                    .build();

            minioClient.putObject(args);
            return getObjectUrl(objectName);

        } catch (Exception e) {
            log.error("上传字符串内容作为文件 [{}] 到 MinIO 失败", objectName, e);
            throw new MinioException("字符串内容上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从MinIO存储中下载文件
     */
    public InputStream downloadFile(String objectName) {
        GetObjectArgs args = GetObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .build();
        try {
            return minioClient.getObject(args);
        } catch (Exception e) {
            log.error("从 MinIO 下载文件 [{}] 失败", objectName, e);
            throw new MinioException("文件下载失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从MinIO存储中删除文件
     */
    public void deleteFile(String objectName) {
        RemoveObjectArgs args = RemoveObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .build();
        try {
            minioClient.removeObject(args);
            log.debug("文件 [{}] 已从 MinIO 删除", objectName);
        } catch (Exception e) {
            log.error("删除 MinIO 文件 [{}] 失败", objectName, e);
            throw new MinioException("文件删除失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取文件的访问URL
     */
    public String getObjectUrl(String objectName) {
        GetPresignedObjectUrlArgs args = GetPresignedObjectUrlArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .method(Method.GET)
                .expiry(7, TimeUnit.DAYS)
                .build();
        try {
            return endpoint + "/" + bucketName + "/" + objectName;
        } catch (Exception e) {
            log.error("生成 MinIO 预签名 URL 失败，对象: {}", objectName, e);
            throw new MinioException("无法生成文件访问链接: " + e.getMessage(), e);
        }
    }

    /**
     * 检查指定对象是否存在于存储中
     */
    public boolean doesObjectExist(String objectName) {
        try {
            StatObjectArgs args = StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build();
            minioClient.statObject(args);
            return true;
        } catch (Exception e) {
            log.debug("对象 [{}] 不存在或无权限访问", objectName, e);
            return false;
        }
    }

    /**
     * 获取文件的详细信息
     */
    public StatObjectResponse getFileInfo(String objectName) {
        StatObjectArgs args = StatObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .build();
        try {
            return minioClient.statObject(args);
        } catch (Exception e) {
            log.error("获取 MinIO 文件信息 [{}] 失败", objectName, e);
            throw new MinioException("获取文件信息失败: " + e.getMessage(), e);
        }
    }

    /**
     * 根据文件名推断 MIME 类型
     */
    private String getContentTypeByFileName(String fileName) {
        MimetypesFileTypeMap fileTypeMap = new MimetypesFileTypeMap();
        String contentType = fileTypeMap.getContentType(new File(fileName));
        if (contentType == null || contentType.trim().isEmpty()) {
            return "application/octet-stream";
        }
        return contentType;
    }

    /**
     * 根据前缀批量删除对象
     */
    public void deleteObjectsByPrefix(String prefix) {
        ListObjectsArgs args = ListObjectsArgs.builder()
                .bucket(bucketName)
                .prefix(prefix)
                .recursive(true)
                .build();

        try {
            Iterable<Result<Item>> results = minioClient.listObjects(args);
            for (Result<Item> result : results) {
                try {
                    Item item = result.get();
                    String objectName = item.objectName();
                    minioClient.removeObject(RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build());
                    log.debug("已删除对象: {}", objectName);
                } catch (Exception innerEx) {
                    log.warn("删除对象 [{}] 失败，继续处理下一个", result.get().objectName(), innerEx);
                    // 可选择记录失败项或抛出聚合异常，这里选择继续
                }
            }
        } catch (Exception e) {
            log.error("批量删除前缀 [{}] 下的对象失败", prefix, e);
            throw new MinioException("批量删除文件失败: " + e.getMessage(), e);
        }
    }
}
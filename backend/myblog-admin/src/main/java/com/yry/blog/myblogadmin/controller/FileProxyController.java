package com.yry.blog.myblogadmin.controller;

import com.yry.blog.myblogcommon.minio.MinioService;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileProxyController {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;
    
    private static final String BG_IMAGE_PATH = "system/background/";

    @GetMapping("/avatars/{userId}/{filename}")
    public void getAvatar(
            @PathVariable String userId,
            @PathVariable String filename,
            HttpServletResponse response) {
        
        String objectName = "avatars/" + userId + "/" + filename;
        
        try (InputStream inputStream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build())) {
            
            String contentType = getContentType(filename);
            response.setContentType(contentType);
            response.setHeader("Cache-Control", "public, max-age=31536000");
            
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                    .replace("+", "%20");
            response.setHeader("Content-Disposition", "inline; filename=\"" + encodedFilename + "\"");
            
            try (OutputStream outputStream = response.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
            }
        } catch (Exception e) {
            log.error("获取头像失败: {}", objectName, e);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @GetMapping("/backgrounds/{filename}")
    public void getBackground(
            @PathVariable String filename,
            HttpServletResponse response) {
        
        String objectName = BG_IMAGE_PATH + filename;
        
        try (InputStream inputStream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build())) {
            
            String contentType = getContentType(filename);
            response.setContentType(contentType);
            response.setHeader("Cache-Control", "public, max-age=31536000");
            
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                    .replace("+", "%20");
            response.setHeader("Content-Disposition", "inline; filename=\"" + encodedFilename + "\"");
            
            try (OutputStream outputStream = response.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
            }
        } catch (Exception e) {
            log.error("获取背景图失败: {}", objectName, e);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private String getContentType(String filename) {
        String extension = filename.toLowerCase();
        if (extension.endsWith(".jpg") || extension.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG_VALUE;
        } else if (extension.endsWith(".png")) {
            return MediaType.IMAGE_PNG_VALUE;
        } else if (extension.endsWith(".gif")) {
            return MediaType.IMAGE_GIF_VALUE;
        } else if (extension.endsWith(".webp")) {
            return "image/webp";
        }
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }
}

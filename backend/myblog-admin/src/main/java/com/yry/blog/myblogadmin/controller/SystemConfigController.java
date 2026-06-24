package com.yry.blog.myblogadmin.controller;

import com.yry.blog.myblogadmin.service.SysConfigService;
import com.yry.blog.myblogcommon.minio.MinioService;
import com.yry.blog.myblogcommon.result.Response;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/config")
public class SystemConfigController {

    private static final String LOGIN_BG_KEY = "login_background";
    private static final String OVERLAY_OPACITY_KEY = "login_overlay_opacity";
    private static final String BG_IMAGE_PATH = "system/background/";

    private final SysConfigService sysConfigService;
    private final MinioService minioService;

    public SystemConfigController(SysConfigService sysConfigService, MinioService minioService) {
        this.sysConfigService = sysConfigService;
        this.minioService = minioService;
    }

    @GetMapping("/login-background")
    public Response<Map<String, String>> getLoginBackground() {
        String background = sysConfigService.getConfigValue(LOGIN_BG_KEY);
        String overlayOpacity = sysConfigService.getConfigValue(OVERLAY_OPACITY_KEY);
        Map<String, String> result = new HashMap<>();
        result.put("background", background != null ? background : "");
        result.put("overlay_opacity", overlayOpacity != null ? overlayOpacity : "auto");
        return Response.success(result);
    }

    @PostMapping("/login-background")
    public Response<String> setLoginBackground(@RequestBody Map<String, String> request) {
        String background = request.get("background");
        sysConfigService.setConfigValue(LOGIN_BG_KEY, background != null ? background : "");
        String overlayOpacity = request.get("overlay_opacity");
        sysConfigService.setConfigValue(OVERLAY_OPACITY_KEY, overlayOpacity != null ? overlayOpacity : "auto");
        return Response.success("配置保存成功");
    }

    @PostMapping("/login-background/upload")
    public Response<Map<String, String>> uploadLoginBackground(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Response.error(com.yry.blog.myblogcommon.enums.ResponseCodeEnums.BAD_REQUEST, "请选择要上传的文件");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String fileName = UUID.randomUUID().toString() + extension;
        String objectName = BG_IMAGE_PATH + fileName;

        minioService.uploadFile(file, objectName);

        String fileUrl = "/api/files/backgrounds/" + fileName;
        sysConfigService.setConfigValue(LOGIN_BG_KEY, fileUrl);

        Map<String, String> result = new HashMap<>();
        result.put("url", fileUrl);
        return Response.success(result);
    }

    @PostMapping("/login-background/fetch")
    public Response<Map<String, String>> fetchAndUploadBackground(@RequestBody Map<String, String> request) {
        String imageUrl = request.get("url");
        if (imageUrl == null || imageUrl.isEmpty()) {
            return Response.error(com.yry.blog.myblogcommon.enums.ResponseCodeEnums.BAD_REQUEST, "图片URL不能为空");
        }

        try {
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                return Response.error(com.yry.blog.myblogcommon.enums.ResponseCodeEnums.BAD_REQUEST, "无法获取图片，HTTP状态码: " + responseCode);
            }

            String contentType = connection.getContentType();
            String extension = ".jpg";
            if (contentType != null) {
                if (contentType.contains("png")) {
                    extension = ".png";
                } else if (contentType.contains("webp")) {
                    extension = ".webp";
                } else if (contentType.contains("gif")) {
                    extension = ".gif";
                }
            }

            byte[] imageBytes;
            try (InputStream inputStream = connection.getInputStream()) {
                imageBytes = inputStream.readAllBytes();
            }
            connection.disconnect();

            if (imageBytes.length == 0) {
                return Response.error(com.yry.blog.myblogcommon.enums.ResponseCodeEnums.BAD_REQUEST, "下载的图片内容为空");
            }

            if (imageBytes.length > 10 * 1024 * 1024) {
                return Response.error(com.yry.blog.myblogcommon.enums.ResponseCodeEnums.BAD_REQUEST, "图片大小超过10MB限制");
            }

            try (ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes)) {
                String fileName = UUID.randomUUID().toString() + extension;
                String objectName = BG_IMAGE_PATH + fileName;

                minioService.uploadFile(new MultipartFile() {
                    @Override
                    public String getName() {
                        return objectName;
                    }
                    @Override
                    public String getOriginalFilename() {
                        return objectName;
                    }
                    @Override
                    public String getContentType() {
                        return contentType != null ? contentType : "image/jpeg";
                    }
                    @Override
                    public boolean isEmpty() {
                        return imageBytes.length == 0;
                    }
                    @Override
                    public long getSize() {
                        return imageBytes.length;
                    }
                    @Override
                    public byte[] getBytes() {
                        return imageBytes;
                    }
                    @Override
                    public InputStream getInputStream() {
                        return bais;
                    }
                    @Override
                    public void transferTo(java.io.File dest) {
                        throw new UnsupportedOperationException();
                    }
                }, objectName);

                String fileUrl = "/api/files/backgrounds/" + fileName;
                sysConfigService.setConfigValue(LOGIN_BG_KEY, fileUrl);

                Map<String, String> result = new HashMap<>();
                result.put("url", fileUrl);
                return Response.success(result);
            }
        } catch (Exception e) {
            return Response.error(com.yry.blog.myblogcommon.enums.ResponseCodeEnums.BAD_REQUEST, "下载图片失败: " + e.getMessage());
        }
    }
}

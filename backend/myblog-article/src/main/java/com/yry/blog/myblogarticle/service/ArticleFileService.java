package com.yry.blog.myblogarticle.service;

import com.yry.blog.myblogcommon.minio.MinioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
public class ArticleFileService {

    // === 路径常量定义（保持原样，非常好的实践） ===
    private static final String ARTICLE_BASE_PATH = "article/";
    private static final String AVATAR_BASE_PATH = "avatar/"; // 虽然没用到，但可以保留
    private static final String ARTICLE_COVER_PATH = "cover/";
    private static final String ARTICLE_CONTENT_PATH = "content/"; // 建议将此常量用于.md文件的路径
    private static final String ARTICLE_SOURCE_PATH = "img/";

    // === 文件名常量 ===
    private static final String CONTENT_FILENAME = "content.md";

    @Autowired
    private MinioService minioService;
    /**
     * 上传文章封面
     */
    public String uploadArticleCover(MultipartFile file, Long articleId){
        // 直接调用通用方法，指定子路径即可
        return uploadArticleFile(file, articleId, ARTICLE_COVER_PATH);
    }

    /**
     * 上传文章内容中的图片
     */
    public String uploadArticleContentImage(MultipartFile file, Long articleId){
        // 同样调用通用方法
        return uploadArticleFile(file, articleId, ARTICLE_SOURCE_PATH);
    }

    /**
     * 上传文章内容（Markdown文件）
     */
    public String uploadArticleContent(String content, Long articleId){
        // 使用常量拼接路径，更加清晰和健壮
        String objectName = String.format(
                "%s%s/%s%s",
                ARTICLE_BASE_PATH,
                articleId,
                ARTICLE_CONTENT_PATH, // 使用专门的CONTENT_PATH
                CONTENT_FILENAME      // 使用文件名常量
        );
        return minioService.uploadStringAsFile(content, objectName);
    }

    /**
     * 【私有通用方法】上传与文章相关的文件
     * 提取了重复逻辑，统一处理文件上传
     *
     * @param file        待上传的文件
     * @param articleId   文章ID
     * @param subPath     子路径（如 "cover/", "img/"）
     * @return 文件的URL
     */
    private String uploadArticleFile(MultipartFile file, Long articleId, String subPath){
        String originalFilename = file.getOriginalFilename();

        // 1. 处理空文件名的情况
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            originalFilename = "file_" + System.currentTimeMillis();
        }

        // 2. 分离文件名和扩展名
        String baseName = originalFilename;
        String extension = "";
        int lastDotIndex = originalFilename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < originalFilename.length() - 1) {
            baseName = originalFilename.substring(0, lastDotIndex);
            extension = originalFilename.substring(lastDotIndex);
        }
        String cleanedBaseName = baseName.replaceAll("[^a-zA-Z0-9]", "_");
        // 2. 生成唯一文件名
        String uniqueFileName = cleanedBaseName + "_" + System.currentTimeMillis() + extension;

        // 3. 拼接最终的对象路径
        String objectName = String.format(
                "%s%s/%s%s",
                ARTICLE_BASE_PATH,
                articleId,
                subPath,
                uniqueFileName
        );

        // 4. 调用MinIO服务上传
        return minioService.uploadFile(file, objectName);
    }
    public void deleteArticleFiles(Long articleId) throws Exception { // 删除文章以及其关联的文件
        if (articleId == null || articleId <= 0) {
            throw new IllegalArgumentException("文章ID无效");
        }
        String prefix = ARTICLE_BASE_PATH + articleId + "/";
        minioService.deleteObjectsByPrefix(prefix); // 直接删除，不先 list
    }
    public void deleteArticleCover(Long articleId) throws Exception {
        if (articleId == null || articleId <= 0) {
            throw new IllegalArgumentException("<UNK>ID<UNK>");
        }
        String prefix = ARTICLE_BASE_PATH + articleId + "/" + ARTICLE_COVER_PATH;
        minioService.deleteFile(prefix);
    }
}
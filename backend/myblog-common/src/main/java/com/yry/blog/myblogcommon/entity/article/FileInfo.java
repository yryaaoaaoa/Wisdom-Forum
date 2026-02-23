package com.yry.blog.myblogcommon.entity.article;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("file_info")
public class FileInfo {
    @TableId(type = IdType.AUTO)
    private Long id; // 主键id
    private Long articleId; // 关联的文章ID（外键）
    private String fileName; // 文件名（比如「关于XX的思考.md」）
    private String fileType; // 文件类型（比如「text/markdown」「image/png」）
    private Long fileSize; // 文件大小（字节）
    private String bucketName; // MinIO的存储桶名（比如「articles」）
    private String objectName; // MinIO的对象名（比如「2024/05/xxx-1234.md」，MinIO中唯一）
    private String url; // MinIO返回的访问URL（可选，也可动态拼接）
    private String uploaderId; // 上传人ID（关联用户表）
}


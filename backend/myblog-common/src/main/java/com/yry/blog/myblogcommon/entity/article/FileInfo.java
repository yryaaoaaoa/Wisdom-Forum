package com.yry.blog.myblogcommon.entity.article;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("file_info")
public class FileInfo {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("file_name")
    private String fileName;
    
    @TableField("file_type")
    private String fileType;
    
    @TableField("file_size")
    private Long fileSize;
    
    @TableField("bucket_name")
    private String bucketName;
    
    @TableField("object_name")
    private String objectName;
    
    @TableField("url")
    private String url;
    
    @TableField("upload_time")
    private LocalDateTime uploadTime;
    
    @TableField("uploader_id")
    private Long uploaderId;
    
    @TableField("biz_type")
    private String bizType;
    
    @TableField("biz_id")
    private Long bizId;
    
    @TableField("is_deleted")
    @TableLogic
    private Integer isDeleted;
}

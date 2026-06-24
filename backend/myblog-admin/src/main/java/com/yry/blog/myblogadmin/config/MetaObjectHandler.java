package com.yry.blog.myblogadmin.config;

import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
@Component
public class MetaObjectHandler implements com.baomidou.mybatisplus.core.handlers.MetaObjectHandler {
    @Override
    // mybatis会自动识别操作为插入还是更新
    public void insertFill(MetaObject metaObject){ // 插入时自动填充字段
        // 4个参数，分别是 元对象，字段名，字段值，字段类型
        // 支持多种命名方式，确保不同实体类都能被正确填充
        this.strictInsertFill(metaObject, "createTime", () -> LocalDateTime.now(), LocalDateTime.class);
        this.strictInsertFill(metaObject, "updateTime", () -> LocalDateTime.now(), LocalDateTime.class);
        this.strictInsertFill(metaObject, "createdAt", () -> LocalDateTime.now(), LocalDateTime.class);
        this.strictInsertFill(metaObject, "updatedAt", () -> LocalDateTime.now(), LocalDateTime.class);
    }
    @Override
    public void updateFill(MetaObject metaObject) // 更新时自动填充字段
    {
        // 支持多种命名方式，确保不同实体类都能被正确填充
        this.strictUpdateFill(metaObject, "updateTime", () -> LocalDateTime.now(), LocalDateTime.class);
        this.strictUpdateFill(metaObject, "updatedAt", () -> LocalDateTime.now(), LocalDateTime.class);
    }
}
package com.yry.blog.myblogarticle.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class ESIndexInitializer {

    @Autowired
    private ElasticsearchClient esClient;

    @PostConstruct
    public void createIndexIfNotExists() {
        try {
            // 检查索引是否已存在
            boolean exists = esClient.indices()
                .exists(ExistsRequest.of(e -> e.index(ArticleDocument.INDEX_NAME)))
                .value();

            if (!exists) {
                log.info("ES索引不存在，项目启动后手动创建或通过代码创建: {}", ArticleDocument.INDEX_NAME);
            } else {
                log.info("ES索引已存在: {}", ArticleDocument.INDEX_NAME);
            }
        } catch (IOException e) {
            log.warn("ES连接异常，跳过索引初始化: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("ES索引初始化失败: {}", e.getMessage());
        }
    }
}

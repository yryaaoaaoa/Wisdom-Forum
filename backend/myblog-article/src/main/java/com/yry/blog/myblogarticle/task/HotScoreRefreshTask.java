package com.yry.blog.myblogarticle.task;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yry.blog.myblogarticle.elasticsearch.ArticleDocument;
import com.yry.blog.myblogarticle.elasticsearch.HotArticleService;
import com.yry.blog.myblogarticle.mapper.ArticleMapper;
import com.yry.blog.myblogarticle.mapper.CommentMapper;
import com.yry.blog.myblogcommon.entity.article.Article;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class HotScoreRefreshTask {

    private static final int BATCH_SIZE = 500;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private HotArticleService hotArticleService;

    @Autowired
    private ElasticsearchClient esClient;

    @Value("${elasticsearch.enabled:true}")
    private boolean esEnabled;

    @Scheduled(cron = "0 0 * * * ?")
    public void refreshHotScore() {
        if (!esEnabled) {
            log.warn("ES功能未启用，跳过热度分数刷新任务");
            return;
        }

        LocalDateTime startTime = LocalDateTime.now();
        log.info("========== 热度分数刷新任务开始 | 开始时间: {} ==========", startTime);

        int totalProcessed = 0;
        int currentPage = 1;
        int successCount = 0;
        int failCount = 0;

        try {
            while (true) {
                Page<Article> page = new Page<>(currentPage, BATCH_SIZE);
                LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(Article::getStatus, 1)
                           .eq(Article::getIsDeleted, 0)
                           .orderByAsc(Article::getId);

                Page<Article> articlePage = articleMapper.selectPage(page, queryWrapper);
                List<Article> articles = articlePage.getRecords();

                if (articles.isEmpty()) {
                    break;
                }

                List<ArticleDocument> documentsToUpdate = new ArrayList<>();

                for (Article article : articles) {
                    try {
                        int commentCount = commentMapper.countByArticleId(article.getId());
                        double hotScore = hotArticleService.calculateHotScore(article, commentCount);

                        ArticleDocument document = ArticleDocument.builder()
                                .id(article.getId())
                                .title(article.getTitle())
                                .summary(article.getSummary())
                                .content(article.getContent())
                                .authorName(article.getAuthName())
                                .authId(article.getAuthId())
                                .coverUrl(article.getCoverUrl())
                                .status(article.getStatus())
                                .readCount(article.getReadCount())
                                .likeCount(article.getLikeCount())
                                .createdAt(article.getCreatedAt())
                                .updatedAt(article.getUpdatedAt())
                                .hotScore(hotScore)
                                .commentCount(commentCount)
                                .build();

                        documentsToUpdate.add(document);
                    } catch (Exception e) {
                        log.error("计算文章热度分数失败: articleId={}", article.getId(), e);
                        failCount++;
                    }
                }

                if (!documentsToUpdate.isEmpty()) {
                    try {
                        bulkUpdateToElasticsearch(documentsToUpdate);
                        successCount += documentsToUpdate.size();
                        log.info("批量更新ES成功 | 第{}页 | 更新数量: {}", currentPage, documentsToUpdate.size());
                    } catch (Exception e) {
                        log.error("批量更新ES失败 | 第{}页 | 文章数量: {}", currentPage, documentsToUpdate.size(), e);
                        failCount += documentsToUpdate.size();
                    }
                }

                totalProcessed += articles.size();
                currentPage++;

                if (!articlePage.hasNext()) {
                    break;
                }
            }

        } catch (Exception e) {
            log.error("热度分数刷新任务执行异常", e);
        } finally {
            LocalDateTime endTime = LocalDateTime.now();
            long durationSeconds = java.time.Duration.between(startTime, endTime).getSeconds();

            log.info("========== 热度分数刷新任务结束 ==========");
            log.info("开始时间: {}", startTime);
            log.info("结束时间: {}", endTime);
            log.info("耗时: {} 秒", durationSeconds);
            log.info("处理文章总数: {}", totalProcessed);
            log.info("成功更新: {}", successCount);
            log.info("失败数量: {}", failCount);
            log.info("========================================");
        }
    }

    private void bulkUpdateToElasticsearch(List<ArticleDocument> documents) throws IOException {
        BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();

        for (ArticleDocument doc : documents) {
            bulkBuilder.operations(op -> op
                .index(idx -> idx
                    .index(ArticleDocument.INDEX_NAME)
                    .id(doc.getId().toString())
                    .document(doc)
                )
            );
        }

        BulkResponse bulkResponse = esClient.bulk(bulkBuilder.build());

        if (bulkResponse.errors()) {
            log.warn("批量更新部分失败，请检查ES日志");
            bulkResponse.items().forEach(item -> {
                if (item.error() != null) {
                    log.error("文档更新失败: id={}, error={}", item.id(), item.error().reason());
                }
            });
        }
    }
}

package com.yry.blog.myblogarticle.mq;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.yry.blog.myblogarticle.elasticsearch.ArticleDocument;
import com.yry.blog.myblogarticle.elasticsearch.ArticleSearchService;
import com.yry.blog.myblogarticle.elasticsearch.HotArticleService;
import com.yry.blog.myblogarticle.mapper.ArticleMapper;
import com.yry.blog.myblogarticle.mapper.CommentMapper;
import com.yry.blog.myblogcommon.entity.article.Article;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RocketMQMessageListener(
    topic = "article-sync-topic",
    consumerGroup = "article-es-sync-group",
    consumeMode = ConsumeMode.CONCURRENTLY,
    maxReconsumeTimes = 3
)
@Slf4j
public class ArticleSyncConsumer implements RocketMQListener<ArticleSyncMessage> {

    @Autowired
    private ArticleMapper articleMapper;
    
    @Autowired
    private CommentMapper commentMapper;
    
    @Autowired
    private ArticleSearchService searchService;
    
    @Autowired
    private HotArticleService hotArticleService;

    @Override
    public void onMessage(ArticleSyncMessage message) {
        log.info("收到文章同步消息: articleId={}, operation={}", 
            message.getArticleId(), message.getOperation());

        try {
            if ("DELETE".equals(message.getOperation())) {
                searchService.deleteArticle(message.getArticleId());
            } else {
                Article article = articleMapper.selectById(message.getArticleId());
                if (article != null && article.getStatus() == 1) {
                    int commentCount = commentMapper.countByArticleId(message.getArticleId());
                    ArticleDocument document = convertToDocument(article, commentCount);
                    searchService.indexArticle(document);
                } else if (article != null && article.getStatus() != 1) {
                    searchService.deleteArticle(message.getArticleId());
                }
            }
        } catch (Exception e) {
            log.error("同步文章到ES失败: articleId={}", message.getArticleId(), e);
            throw new RuntimeException("同步失败，触发重试", e);
        }
    }

    private ArticleDocument convertToDocument(Article article, int commentCount) {
        double hotScore = hotArticleService.calculateHotScore(article, commentCount);
        
        return ArticleDocument.builder()
            .id(article.getId())
            .title(article.getTitle())
            .summary(article.getSummary())
            .content(article.getContent())
            .authId(article.getAuthId())
            .status(article.getStatus())
            .readCount(article.getReadCount())
            .likeCount(article.getLikeCount())
            .commentCount(commentCount)
            .hotScore(hotScore)
            .coverUrl(article.getCoverUrl())
            .createdAt(article.getCreatedAt())
            .updatedAt(article.getUpdatedAt())
            .build();
    }
}

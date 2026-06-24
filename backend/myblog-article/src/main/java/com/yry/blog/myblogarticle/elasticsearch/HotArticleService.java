package com.yry.blog.myblogarticle.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import com.yry.blog.myblogcommon.entity.article.Article;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
public class HotArticleService {

    @Autowired
    private ElasticsearchClient esClient;

    @Value("${elasticsearch.enabled:true}")
    private boolean esEnabled;

    private static final double READ_WEIGHT = 0.3;
    private static final double LIKE_WEIGHT = 0.4;
    private static final double COMMENT_WEIGHT = 0.2;
    private static final double TIME_WEIGHT = 0.1;
    private static final double HOT_SCORE_THRESHOLD = 1.0;
    private static final long ES_QUERY_TIMEOUT_MS = 500;

    public SearchResponse<ArticleDocument> getHotArticles(int page, int size) {
        if (!esEnabled) {
            log.warn("ES功能未启用，无法获取热门文章");
            return null;
        }
        try {
            return esClient.search(s -> s
                .index(ArticleDocument.INDEX_NAME)
                .query(q -> q
                    .term(t -> t.field("status").value(1))
                )
                .from((page - 1) * size)
                .size(size)
                .sort(sort -> sort
                    .field(f -> f.field("hotScore").order(SortOrder.Desc))
                )
                .sort(sort -> sort
                    .field(f -> f.field("createdAt").order(SortOrder.Desc))
                ),
                ArticleDocument.class
            );
        } catch (IOException e) {
            log.error("获取热门文章失败", e);
            throw new RuntimeException("获取热门文章失败", e);
        }
    }

    public SearchResponse<ArticleDocument> searchWithRelevance(String keyword, int page, int size) {
        if (!esEnabled) {
            log.warn("ES功能未启用，无法执行相关性搜索");
            return null;
        }
        try {
            return esClient.search(s -> s
                .index(ArticleDocument.INDEX_NAME)
                .query(q -> q
                    .bool(b -> b
                        .must(m -> m
                            .multiMatch(mm -> mm
                                .fields("title^3", "summary^2", "content")
                                .query(keyword)
                                .type(TextQueryType.BestFields)
                            )
                        )
                        .filter(f -> f
                            .term(t -> t.field("status").value(1))
                        )
                    )
                )
                .highlight(h -> h
                    .fields("title", f -> f
                        .preTags("<em class='highlight'>")
                        .postTags("</em>")
                    )
                    .fields("summary", f -> f
                        .preTags("<em class='highlight'>")
                        .postTags("</em>")
                    )
                )
                .from((page - 1) * size)
                .size(size)
                .sort(sort -> sort
                    .field(f -> f.field("likeCount").order(SortOrder.Desc))
                )
                .sort(sort -> sort
                    .field(f -> f.field("readCount").order(SortOrder.Desc))
                ),
                ArticleDocument.class
            );
        } catch (IOException e) {
            log.error("相关性搜索失败: keyword={}", keyword, e);
            throw new RuntimeException("相关性搜索失败", e);
        }
    }

    public SearchResponse<ArticleDocument> getTrendingArticles(int hours, int size) {
        if (!esEnabled) {
            log.warn("ES功能未启用，无法获取趋势文章");
            return null;
        }
        try {
            // 先简化这个方法，不使用range查询，避免API问题
            return esClient.search(s -> s
                .index(ArticleDocument.INDEX_NAME)
                .query(q -> q
                    .term(t -> t.field("status").value(1))
                )
                .size(size)
                .sort(sort -> sort
                    .field(f -> f.field("readCount").order(SortOrder.Desc))
                )
                .sort(sort -> sort
                    .field(f -> f.field("likeCount").order(SortOrder.Desc))
                ),
                ArticleDocument.class
            );
        } catch (IOException e) {
            log.error("获取趋势文章失败", e);
            throw new RuntimeException("获取趋势文章失败", e);
        }
    }

    public double calculateHotScore(Article article, int commentCount) {
        double readScore = Math.log1p(article.getReadCount() != null ? article.getReadCount() : 0) * READ_WEIGHT;
        double likeScore = Math.log1p(article.getLikeCount() != null ? article.getLikeCount() : 0) * LIKE_WEIGHT;
        double commentScore = Math.log1p(commentCount) * COMMENT_WEIGHT;

        double timeScore = 0;
        if (article.getCreatedAt() != null) {
            long days = java.time.Duration.between(article.getCreatedAt(), LocalDateTime.now()).toDays();
            timeScore = Math.exp(-days / 7.0) * TIME_WEIGHT;
        }

        return readScore + likeScore + commentScore + timeScore;
    }

    public List<Map<String, Object>> extractResults(SearchResponse<ArticleDocument> response) {
        List<Map<String, Object>> results = new ArrayList<>();

        for (Hit<ArticleDocument> hit : response.hits().hits()) {
            Map<String, Object> item = new HashMap<>();
            item.put("document", hit.source());
            item.put("score", hit.score());
            results.add(item);
        }

        return results;
    }

    public long getTotalHits(SearchResponse<ArticleDocument> response) {
        return response.hits().total() != null ? response.hits().total().value() : 0;
    }

    public boolean isHotArticle(Long articleId) {
        if (!esEnabled) {
            return false;
        }

        try {
            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                try {
                    SearchResponse<ArticleDocument> response = esClient.search(s -> s
                        .index(ArticleDocument.INDEX_NAME)
                        .query(q -> q
                            .bool(b -> b
                                .must(m -> m.term(t -> t.field("id").value(articleId)))
                                .must(m -> m.term(t -> t.field("status").value(1)))
                            )
                        )
                        .size(1),
                        ArticleDocument.class
                    );

                    return response.hits().total() != null && response.hits().total().value() > 0;
                } catch (IOException e) {
                    log.warn("ES查询热度失败: articleId={}, error={}", articleId, e.getMessage());
                    return false;
                }
            });

            return future.get(ES_QUERY_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            log.warn("ES查询超时: articleId={}, timeout={}ms", articleId, ES_QUERY_TIMEOUT_MS);
            return false;
        } catch (Exception e) {
            log.warn("判断文章热度失败: articleId={}", articleId, e);
            return false;
        }
    }

    public Double getArticleHotScore(Long articleId) {
        if (!esEnabled) {
            log.warn("ES功能未启用，无法获取文章热度分数");
            return null;
        }
        try {
            SearchResponse<ArticleDocument> response = esClient.search(s -> s
                .index(ArticleDocument.INDEX_NAME)
                .query(q -> q
                    .bool(b -> b
                        .must(m -> m.term(t -> t.field("id").value(articleId)))
                    )
                )
                .size(1),
                ArticleDocument.class
            );

            if (response.hits().hits().isEmpty()) {
                return null;
            }

            ArticleDocument doc = response.hits().hits().get(0).source();
            return doc != null ? doc.getHotScore() : null;
        } catch (Exception e) {
            log.warn("获取文章热度分数失败: articleId={}", articleId, e);
            return null;
        }
    }
}

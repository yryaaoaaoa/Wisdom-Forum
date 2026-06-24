package com.yry.blog.myblogarticle.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ArticleSearchServiceImpl implements ArticleSearchService {

    @Autowired
    private ElasticsearchClient esClient;

    @Override
    public void indexArticle(ArticleDocument document) {
        try {
            esClient.index(i -> i
                .index(ArticleDocument.INDEX_NAME)
                .id(document.getId().toString())
                .document(document)
            );
            log.info("文章索引成功: articleId={}", document.getId());
        } catch (IOException e) {
            log.error("索引文章失败: articleId={}", document.getId(), e);
            throw new RuntimeException("ES索引失败", e);
        }
    }

    @Override
    public void deleteArticle(Long articleId) {
        try {
            esClient.delete(d -> d
                .index(ArticleDocument.INDEX_NAME)
                .id(articleId.toString())
            );
            log.info("删除文章索引: articleId={}", articleId);
        } catch (IOException e) {
            log.error("删除文章索引失败: articleId={}", articleId, e);
        }
    }

    @Override
    public void bulkIndex(List<ArticleDocument> documents) {
        try {
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
            
            esClient.bulk(bulkBuilder.build());
            log.info("批量索引成功: count={}", documents.size());
        } catch (IOException e) {
            log.error("批量索引失败", e);
            throw new RuntimeException("批量索引失败", e);
        }
    }

    @Override
    public SearchResponse<ArticleDocument> search(String keyword, int page, int size) {
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
                    .field(f -> f.field("createdAt").order(SortOrder.Desc))
                ),
                ArticleDocument.class
            );
        } catch (IOException e) {
            log.error("搜索失败: keyword={}", keyword, e);
            throw new RuntimeException("搜索失败", e);
        }
    }

    @Override
    public SearchResponse<ArticleDocument> advancedSearch(ArticleSearchDTO searchDTO) {
        try {
            SearchRequest.Builder builder = new SearchRequest.Builder()
                .index(ArticleDocument.INDEX_NAME);
            
            builder.query(q -> q.bool(b -> {
                if (StringUtils.hasText(searchDTO.getKeyword())) {
                    b.must(m -> m
                        .multiMatch(mm -> mm
                            .fields("title^3", "summary^2", "content")
                            .query(searchDTO.getKeyword())
                            .type(TextQueryType.BestFields)
                        )
                    );
                }
                
                if (searchDTO.getAuthId() != null) {
                    b.filter(f -> f
                        .term(t -> t.field("authId").value(searchDTO.getAuthId()))
                    );
                }
                
                b.filter(f -> f
                    .term(t -> t.field("status").value(1))
                );
                
                return b;
            }));
            
            if (Boolean.TRUE.equals(searchDTO.getSortByReadCount())) {
                builder.sort(sort -> sort
                    .field(f -> f.field("readCount").order(SortOrder.Desc))
                );
            } else {
                builder.sort(sort -> sort
                    .field(f -> f.field("createdAt").order(SortOrder.Desc))
                );
            }
            
            builder.from((searchDTO.getPage() - 1) * searchDTO.getSize())
                   .size(searchDTO.getSize());
                   
            return esClient.search(builder.build(), ArticleDocument.class);
        } catch (IOException e) {
            log.error("高级搜索失败", e);
            throw new RuntimeException("高级搜索失败", e);
        }
    }

    public List<Map<String, Object>> extractSearchResults(SearchResponse<ArticleDocument> response) {
        return response.hits().hits().stream()
            .map(hit -> {
                Map<String, Object> result = new java.util.HashMap<>();
                ArticleDocument doc = hit.source();
                result.put("id", doc.getId());
                result.put("title", doc.getTitle());
                result.put("summary", doc.getSummary());
                result.put("authorName", doc.getAuthorName());
                result.put("coverUrl", doc.getCoverUrl());
                result.put("readCount", doc.getReadCount());
                result.put("likeCount", doc.getLikeCount());
                result.put("createdAt", doc.getCreatedAt());
                
                if (hit.highlight() != null) {
                    if (hit.highlight().containsKey("title")) {
                        result.put("highlightTitle", hit.highlight().get("title").get(0));
                    }
                    if (hit.highlight().containsKey("summary")) {
                        result.put("highlightSummary", hit.highlight().get("summary").get(0));
                    }
                }
                return result;
            })
            .collect(Collectors.toList());
    }

    public long getTotalHits(SearchResponse<ArticleDocument> response) {
        return response.hits().total() != null ? response.hits().total().value() : 0;
    }
}

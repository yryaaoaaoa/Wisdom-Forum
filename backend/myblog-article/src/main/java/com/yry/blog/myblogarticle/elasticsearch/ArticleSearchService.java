package com.yry.blog.myblogarticle.elasticsearch;

import co.elastic.clients.elasticsearch.core.SearchResponse;

import java.util.List;

public interface ArticleSearchService {
    void indexArticle(ArticleDocument document);
    void deleteArticle(Long articleId);
    void bulkIndex(List<ArticleDocument> documents);
    SearchResponse<ArticleDocument> search(String keyword, int page, int size);
    SearchResponse<ArticleDocument> advancedSearch(ArticleSearchDTO searchDTO);
}

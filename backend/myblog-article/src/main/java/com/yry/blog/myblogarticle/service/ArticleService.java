package com.yry.blog.myblogarticle.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yry.blog.myblogarticle.dto.ArticleQueryDTO;
import com.yry.blog.myblogarticle.dto.ArticleUpdateDTO;
import com.yry.blog.myblogarticle.vo.ArticleContentVO;
import com.yry.blog.myblogarticle.vo.ArticleVO;
import com.yry.blog.myblogarticle.vo.CommentVO;
import com.yry.blog.myblogarticle.vo.StatsVO;
import com.yry.blog.myblogcommon.entity.article.Article;
import com.yry.blog.myblogcommon.result.PaginationResponse;
import com.yry.blog.myblogcommon.result.Response;
import jakarta.validation.Valid;

import java.util.List;


public interface ArticleService extends IService<Article>{
    void publishArticle(Long articleId);
    Response<ArticleUpdateDTO> updateArticle(Long id, @Valid ArticleUpdateDTO articleDTO);
    Response<Object> deleteArticle(Long articleId);
    Response<Article> getArticleById(Long id);
    Response<PaginationResponse<ArticleVO>> pageArticles(ArticleQueryDTO queryDTO);
    Response<ArticleUpdateDTO> getDraft();
    Response<StatsVO> getStats();
    Response<List<ArticleVO>> getHotArticles(Integer limit);
    Response<List<ArticleVO>> getRelatedArticles(Long articleId, Integer limit);
    Response<Object> reindexAllToES();
}


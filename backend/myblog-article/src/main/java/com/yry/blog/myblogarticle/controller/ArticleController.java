package com.yry.blog.myblogarticle.controller;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.yry.blog.myblogarticle.dto.AddCommentDTO;
import com.yry.blog.myblogarticle.dto.ArticleQueryDTO;
import com.yry.blog.myblogarticle.dto.ArticleUpdateDTO;
import com.yry.blog.myblogarticle.dto.DeleteCommentDTO;
import com.yry.blog.myblogarticle.elasticsearch.ArticleDocument;
import com.yry.blog.myblogarticle.elasticsearch.ArticleSearchDTO;
import com.yry.blog.myblogarticle.elasticsearch.ArticleSearchServiceImpl;
import com.yry.blog.myblogarticle.elasticsearch.HotArticleService;
import com.yry.blog.myblogarticle.service.ArticleService;
import com.yry.blog.myblogarticle.service.CommentService;
import com.yry.blog.myblogarticle.service.LikeService;
import com.yry.blog.myblogarticle.vo.ArticleVO;
import com.yry.blog.myblogarticle.vo.StatsVO;
import com.yry.blog.myblogarticle.vo.CommentVO;
import com.yry.blog.myblogcommon.entity.article.Article;
import com.yry.blog.myblogcommon.result.PaginationResponse;
import com.yry.blog.myblogcommon.result.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文章模块控制器
 * 处理文章的发布、更新、删除、查询，以及评论、点赞相关的HTTP请求
 * @author （可补充作者信息）
 * @date （可补充创建日期）
 */
@RestController
@Controller
@RequestMapping("/api/articles")
public class ArticleController {

    /**
     * 文章业务逻辑处理服务
     */
    @Autowired
    private ArticleService articleService;

    /**
     * 评论业务逻辑处理服务
     */
    @Autowired
    private CommentService commentService;

    /**
     * 点赞业务逻辑处理服务
     */
    @Autowired
    private LikeService likeService;

    @Autowired
    private ArticleSearchServiceImpl searchService;

    @Autowired
    private HotArticleService hotArticleService;

    /**
     * 发布文章接口
     * 将草稿状态的文章发布为正式文章
     * @param articleId 文章ID
     */
    @PostMapping("/publish")
    public Response<Object> publishArticle(@RequestParam Long articleId){
        articleService.publishArticle(articleId);
        return Response.success(null);
    }

    /**
     * 更新文章接口
     * 根据文章ID修改文章内容（标题、正文等）
     * @param id 文章ID（路径参数）
     * @param articleDTO 文章更新数据传输对象，包含更新后的文章信息
     * @return Response<ArticleUpdateDTO> 响应结果，包含更新后的文章数据
     */
    @PutMapping("/update")
    public Response<ArticleUpdateDTO> updateArticle(@RequestParam Long id, @RequestBody ArticleUpdateDTO articleDTO) {
        return articleService.updateArticle(id,articleDTO);
    }

    /**
     * 删除文章接口
     * 通过作者ID校验权限后，删除指定ID的文章
     * @param articleId 文章ID
     * @return Response<Object> 响应结果，返回删除操作的执行状态
     */
    @DeleteMapping("/delete/{articleId}")
    public Response<Object> deleteArticle(@PathVariable Long articleId) {
        return articleService.deleteArticle(articleId);
    }

    /**
     * 获取草稿文章接口
     * 查询当前登录用户的草稿箱中的文章
     * @return Response<ArticleUpdateDTO> 响应结果，包含草稿文章数据
     */
    @PostMapping("/draft")
    public Response<ArticleUpdateDTO> getDraft(){
        return articleService.getDraft();
    }

    /**
     * 根据文章ID查询文章详情接口
     * @param id 文章ID（请求参数）
     * @return Response<Article> 响应结果，包含文章完整实体信息
     */
    @GetMapping("/get")
    public Response<Article> getArticleById(@RequestParam Long id) {
        return articleService.getArticleById(id);
    }

    /**
     * 分页查询文章列表接口
     * 支持多条件筛选（如作者、分类、关键词等）的文章分页查询
     * @param queryDTO 文章查询条件数据传输对象，包含分页参数和筛选条件
     * @return Response<PaginationResponse<ArticleVO>> 响应结果，包含分页信息和文章VO列表
     */
    @PostMapping("/page")
    public Response<PaginationResponse<ArticleVO>> pageArticles(@RequestBody ArticleQueryDTO queryDTO) {
        return articleService.pageArticles(queryDTO);
    }

    /**
     * 查询文章评论列表接口
     * 根据文章ID查询该文章下的所有评论
     * @param articleId 文章ID（请求参数）
     * @return Response<List<CommentVO>> 响应结果，包含该文章的评论VO列表
     */
    @GetMapping("/comment/view")
    public Response<List<CommentVO>> commentArticle(@RequestParam Long articleId) {
        return commentService.getArticleComments(articleId);
    }

    /**
     * 添加文章评论接口
     * 给指定文章新增一条评论
     * @param commentDTO 评论数据传输对象，包含文章ID、评论内容、评论人等信息
     */
    @PostMapping("/comment/add")
    public Response<Object> addComment(@RequestBody AddCommentDTO commentDTO) {
        commentService.addComment(commentDTO);
        return Response.success(null);
    }

    /**
     * 删除文章评论接口
     * 删除指定ID的评论（需校验评论作者权限）
     * @param commentDTO 评论删除数据传输对象，包含要删除的评论ID
     */
    @PostMapping("/comment/delete")
    public Response<Object> deleteComment(@RequestBody DeleteCommentDTO commentDTO) {
        commentService.deleteComment(commentDTO.getId());
        return Response.success(null);
    }

    /**
     * 文章/评论点赞接口
     * 对指定目标（文章或评论）进行点赞/取消点赞操作
     * @param targetId 点赞目标ID（文章ID或评论ID）
     * @param likeType 点赞类型（1-文章点赞，2-评论点赞，可根据业务定义）
     * @return boolean 操作结果：true-点赞成功，false-取消点赞成功
     */
    @PostMapping("/like")
    public Response<Boolean> like(@RequestParam Long targetId, @RequestParam Integer likeType){
        return Response.success(likeService.like(targetId, likeType));
    }

    /**
     * 获取点赞数接口
     * @param targetId 目标ID（文章ID或评论ID）
     * @param likeType 点赞类型（1-文章，2-评论）
     * @return 点赞数
     */
    @GetMapping("/like/count")
    public Response<Long> getLikeCount(@RequestParam Long targetId, @RequestParam Integer likeType) {
        return Response.success(likeService.getLikeCount(targetId, likeType));
    }

    /**
     * 检查用户是否已点赞接口
     * @param targetId 目标ID（文章ID或评论ID）
     * @param likeType 点赞类型（1-文章，2-评论）
     * @return 是否已点赞
     */
    @GetMapping("/like/status")
    public Response<Boolean> hasLiked(@RequestParam Long targetId, @RequestParam Integer likeType) {
        return Response.success(likeService.hasLiked(targetId, likeType));
    }

    /**
     * 获取博客统计数据接口
     * @return Response<StatsVO> 响应结果，包含文章数、分类数、总阅读量、总点赞数
     */
    @GetMapping("/stats")
    public Response<StatsVO> getStats() {
        return articleService.getStats();
    }

    /**
     * 获取热门文章列表接口
     * @param limit 返回数量限制，默认5篇
     * @return Response<List<ArticleVO>> 响应结果，包含热门文章列表
     */
    @GetMapping("/hot")
    public Response<List<ArticleVO>> getHotArticles(@RequestParam(defaultValue = "5") Integer limit) {
        return articleService.getHotArticles(limit);
    }

    @GetMapping("/related/{articleId}")
    public Response<List<ArticleVO>> getRelatedArticles(
            @PathVariable Long articleId,
            @RequestParam(defaultValue = "5") Integer limit) {
        return articleService.getRelatedArticles(articleId, limit);
    }

    @GetMapping("/search")
    public Response<Map<String, Object>> search(
        @RequestParam String keyword,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        SearchResponse<ArticleDocument> response = searchService.search(keyword, page, size);
        
        Map<String, Object> result = new HashMap<>();
        result.put("records", searchService.extractSearchResults(response));
        result.put("total", searchService.getTotalHits(response));
        result.put("page", page);
        result.put("size", size);
        
        return Response.success(result);
    }

    @PostMapping("/search/advanced")
    public Response<Map<String, Object>> advancedSearch(@RequestBody ArticleSearchDTO searchDTO) {
        SearchResponse<ArticleDocument> response = searchService.advancedSearch(searchDTO);
        
        Map<String, Object> result = new HashMap<>();
        result.put("records", searchService.extractSearchResults(response));
        result.put("total", searchService.getTotalHits(response));
        result.put("page", searchDTO.getPage());
        result.put("size", searchDTO.getSize());
        
        return Response.success(result);
    }

    @GetMapping("/hot/es")
    public Response<Map<String, Object>> getHotArticlesByES(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        SearchResponse<ArticleDocument> response = hotArticleService.getHotArticles(page, size);
        
        Map<String, Object> result = new HashMap<>();
        result.put("records", hotArticleService.extractResults(response));
        result.put("total", hotArticleService.getTotalHits(response));
        result.put("page", page);
        result.put("size", size);
        
        return Response.success(result);
    }

    @PostMapping("/reindex")
    public Response<Object> reindexAll() {
        return articleService.reindexAllToES();
    }

    @GetMapping("/trending")
    public Response<List<Map<String, Object>>> getTrendingArticles(
        @RequestParam(defaultValue = "24") int hours,
        @RequestParam(defaultValue = "10") int size
    ) {
        SearchResponse<ArticleDocument> response = hotArticleService.getTrendingArticles(hours, size);
        return Response.success(hotArticleService.extractResults(response));
    }

    @GetMapping("/search/relevance")
    public Response<Map<String, Object>> searchWithRelevance(
        @RequestParam String keyword,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        SearchResponse<ArticleDocument> response = hotArticleService.searchWithRelevance(keyword, page, size);
        
        Map<String, Object> result = new HashMap<>();
        result.put("records", hotArticleService.extractResults(response));
        result.put("total", hotArticleService.getTotalHits(response));
        result.put("page", page);
        result.put("size", size);
        
        return Response.success(result);
    }
}
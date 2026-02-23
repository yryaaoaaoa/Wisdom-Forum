package com.yry.blog.myblogarticle.controller;

import com.yry.blog.myblogarticle.dto.AddCommentDTO;
import com.yry.blog.myblogarticle.dto.ArticleQueryDTO;
import com.yry.blog.myblogarticle.dto.ArticleUpdateDTO;
import com.yry.blog.myblogarticle.dto.DeleteCommentDTO;
import com.yry.blog.myblogarticle.service.ArticleService;
import com.yry.blog.myblogarticle.service.CommentService;
import com.yry.blog.myblogarticle.service.LikeService;
import com.yry.blog.myblogarticle.vo.ArticleVO;
import com.yry.blog.myblogarticle.vo.CommentVO;
import com.yry.blog.myblogcommon.entity.article.Article;
import com.yry.blog.myblogcommon.result.PaginationResponse;
import com.yry.blog.myblogcommon.result.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 文章模块控制器
 * 处理文章的发布、更新、删除、查询，以及评论、点赞相关的HTTP请求
 * @author （可补充作者信息）
 * @date （可补充创建日期）
 */
@RestController
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

    /**
     * 发布文章接口
     * 将草稿状态的文章发布为正式文章
     * @param articleId 文章ID
     */
    @PostMapping("/publish")
    public void publishArticle(@RequestBody Long articleId){
        articleService.publishArticle(articleId);
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
    @DeleteMapping("/delete")
    public Response<Object> deleteArticle(@RequestBody Long articleId) {
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
    public Response<Article> getArticleById(Long id) {
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
    public Response<List<CommentVO>> commentArticle(Long articleId) {
        return commentService.getArticleComments(articleId);
    }

    /**
     * 添加文章评论接口
     * 给指定文章新增一条评论
     * @param commentDTO 评论数据传输对象，包含文章ID、评论内容、评论人等信息
     */
    @PostMapping("/comment/add")
    public void addComment(@RequestBody AddCommentDTO commentDTO) {
        commentService.addComment(commentDTO);
    }

    /**
     * 删除文章评论接口
     * 删除指定ID的评论（需校验评论作者权限）
     * @param commentDTO 评论删除数据传输对象，包含要删除的评论ID
     */
    @PostMapping("/comment/delete")
    public void deleteComment(@RequestBody DeleteCommentDTO commentDTO) {
        commentService.deleteComment(commentDTO.getId());
    }

    /**
     * 文章/评论点赞接口
     * 对指定目标（文章或评论）进行点赞/取消点赞操作
     * @param targetId 点赞目标ID（文章ID或评论ID）
     * @param likeType 点赞类型（1-文章点赞，2-评论点赞，可根据业务定义）
     * @return boolean 操作结果：true-点赞成功，false-取消点赞成功
     */
    @PostMapping("/like")
    public boolean like(Long targetId, Integer likeType){
        return likeService.like(targetId, likeType);
    }
}
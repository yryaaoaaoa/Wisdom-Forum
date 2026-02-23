package com.yry.blog.myblogarticle.service.Impl;

import com.yry.blog.myblogadmin.auth.Impl.PermissionCheckerImpl;
import com.yry.blog.myblogarticle.dto.AddCommentDTO;
import com.yry.blog.myblogarticle.mapper.CommentMapper;
import com.yry.blog.myblogarticle.service.CommentService;
import com.yry.blog.myblogarticle.vo.CommentVO;
import com.yry.blog.myblogcommon.entity.article.Comment;
import com.yry.blog.myblogcommon.result.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class CommentServiceImpl implements CommentService {
    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private PermissionCheckerImpl permissionChecker;
    @Override
    public Response<List<CommentVO>> getArticleComments(Long articleId){
        // 1. 获取指定文章的所有评论（包括回复）
        List<CommentVO> flatComments = commentMapper.getComment(articleId);

        // 2. 构建树形结构
        // 2.1 创建 Map 用于快速查找 CommentVO (key: commentId, value: CommentVO)
        Map<Long, CommentVO> commentMap = new HashMap<>();
        for (CommentVO comment : flatComments) {
            // 确保 children 列表已初始化
            if (comment.getChildren() == null) {
                comment.setChildren(new ArrayList<>());
            }
            commentMap.put(comment.getId(), comment);
        }
        // 2.2 构建父子关系，找出顶级评论
        List<CommentVO> rootComments = new ArrayList<>();
        for (CommentVO comment : flatComments) {
            Long parentId = comment.getParentId();
            // parentId 为 null 表示顶级评论
            if (parentId == null) {
                rootComments.add(comment);
            } else {
                // 查找父评论并建立联系
                CommentVO parentComment = commentMap.get(parentId);
                if (parentComment != null) {
                    parentComment.getChildren().add(comment);
                }
            }
        }
        return Response.success(rootComments);
    }
    @Override
    public void addComment(AddCommentDTO commentDTO){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication(); // 用户的安全上下文，里面存了基本的认证信息
        Long userId = permissionChecker.getCurrentUserId();
        Comment comment = Comment.builder()
                .userId(userId)
                .articleId(commentDTO.getArticleId())
                .parentId(commentDTO.getParentId())
                .content(commentDTO.getContent())
                .likeCount(0L)
                .build();
        commentMapper.insert(comment);
    }
    @Override
    public void deleteComment(Long commentId){
        commentMapper.deleteById(commentId);
    }
}

package com.yry.blog.myblogarticle.service;

import com.yry.blog.myblogarticle.dto.AddCommentDTO;
import com.yry.blog.myblogarticle.vo.CommentVO;
import com.yry.blog.myblogcommon.result.Response;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public interface CommentService {
    Response<List<CommentVO>> getArticleComments(Long articleId);
    void addComment(AddCommentDTO commentDTO);
    void deleteComment(Long commentId);
}

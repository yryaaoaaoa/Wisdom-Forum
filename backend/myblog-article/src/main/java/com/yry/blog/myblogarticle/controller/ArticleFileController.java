package com.yry.blog.myblogarticle.controller;


import com.yry.blog.myblogarticle.dto.ArticleUpdateDTO;
import com.yry.blog.myblogarticle.service.ArticleFileService;
import com.yry.blog.myblogarticle.service.ArticleService;
import com.yry.blog.myblogcommon.result.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/articles")
public class ArticleFileController {
    @Autowired
    private ArticleFileService articleFileService;

    @Autowired
    private ArticleService articleService;

    @PostMapping("/minio/content")
    public String uploadArticleContent(String content, Long articleId){
        return articleFileService.uploadArticleContent(content, articleId);
    }
    @GetMapping("/minio/draft")
    public Response<ArticleUpdateDTO> getDraft(){
        return articleService.getDraft();
    }

    @PostMapping("/minio/cover")
    public String uploadArticleCover(MultipartFile file, Long articleId){
        return articleFileService.uploadArticleCover(file,articleId);
    }
    @PostMapping("/minio/img")
    public String uploadArticle(MultipartFile file, Long articleId){
        return articleFileService.uploadArticleContentImage(file,articleId);
    }
}

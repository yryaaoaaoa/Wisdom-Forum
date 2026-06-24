package com.yry.blog.myblogarticle.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yry.blog.myblogadmin.auth.Impl.PermissionCheckerImpl;
import com.yry.blog.myblogarticle.dto.ArticleQueryDTO;
import com.yry.blog.myblogarticle.dto.ArticleUpdateDTO;
import com.yry.blog.myblogarticle.elasticsearch.ArticleDocument;
import com.yry.blog.myblogarticle.elasticsearch.ArticleSearchService;
import com.yry.blog.myblogarticle.elasticsearch.HotArticleService;
import com.yry.blog.myblogarticle.mapper.ArticleCategoryMapper;
import com.yry.blog.myblogarticle.mapper.ArticleMapper;
import com.yry.blog.myblogarticle.mapper.CategoryMapper;
import com.yry.blog.myblogarticle.mapper.CommentMapper;
import com.yry.blog.myblogarticle.mapper.mapstruct.ArticleConvertMapper;
import com.yry.blog.myblogarticle.mq.ArticleSyncMessage;
import com.yry.blog.myblogarticle.mq.DefaultSendCallback;
import com.yry.blog.myblogarticle.service.ArticleService;
import com.yry.blog.myblogarticle.service.ArticleFileService;
import com.yry.blog.myblogarticle.service.LikeService;
import com.yry.blog.myblogarticle.vo.ArticleVO;
import com.yry.blog.myblogarticle.vo.StatsVO;
import com.yry.blog.myblogcommon.cache.constants.CacheConstants;
import com.yry.blog.myblogcommon.cache.enums.CacheTypeEnum;
import com.yry.blog.myblogcommon.cache.util.CacheUtil;
import com.yry.blog.myblogcommon.entity.article.Article;
import com.yry.blog.myblogcommon.entity.article.ArticleCategory;
import com.yry.blog.myblogcommon.entity.article.Category;
import com.yry.blog.myblogcommon.enums.ResponseCodeEnums;
import com.yry.blog.myblogcommon.exception.BusinessException;
import com.yry.blog.myblogcommon.result.PaginationResponse;
import com.yry.blog.myblogcommon.result.Response;
import com.yry.blog.myblogcommon.stat.PostAccessCounter;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService {
    @Autowired
    private PermissionCheckerImpl permissionChecker;
    @Autowired
    private ArticleMapper articleMapper;
    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private ArticleCategoryMapper articleCategoryMapper;
    @Autowired
    private ArticleConvertMapper articleConvertMapper;
    @Autowired
    private PostAccessCounter accessCounter;
    @Autowired
    private HotArticleService hotArticleService;
    @Autowired
    private CacheUtil cacheUtil;
    @Autowired
    private LikeService likeService;
    @Autowired
    private RocketMQTemplate rocketMQTemplate;
    @Autowired
    private ArticleFileService articleFileService;
    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private ArticleSearchService searchService;

    private static final String ARTICLE_SYNC_TOPIC = "article-sync-topic";
    private static final long SEND_TIMEOUT = 30000L;

    @Override
    @Transactional
    public void publishArticle(Long articleId) {
        if (articleId == null) {
            throw new BusinessException(ResponseCodeEnums.BAD_REQUEST, "文章ID不能为空");
        }

        Article article = this.getById(articleId);
        if (article == null) {
            throw new BusinessException(ResponseCodeEnums.NOT_FOUND, "文章不存在");
        }

        if (!permissionChecker.check(article.getAuthId())) {
            throw new BusinessException(ResponseCodeEnums.FORBIDDEN, "无权操作此文章");
        }

        if (article.getStatus() == 1) {
            log.warn("文章 [{}] 已处于发布状态", articleId);
            return;
        }

        if (article.getTitle() == null || article.getTitle().isBlank()) {
            throw new BusinessException(ResponseCodeEnums.BAD_REQUEST, "标题不能为空");
        }
        if (article.getContent() == null || article.getContent().isBlank()) {
            throw new BusinessException(ResponseCodeEnums.BAD_REQUEST, "内容不能为空");
        }

        UpdateWrapper<Article> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", articleId).set("status", 1);
        this.update(updateWrapper);

        evictCache(articleId);
        sendArticleSyncMessage(articleId, "UPDATE");
        log.info("文章 [{}] 发布成功", articleId);
    }

    @Override
    @Transactional
    public Response<ArticleUpdateDTO> updateArticle(Long id, ArticleUpdateDTO articleDTO) {
        if (id == null) {
            return Response.error(ResponseCodeEnums.BAD_REQUEST, "文章ID不能为空");
        }

        Article existingArticle = this.getById(id);
        if (existingArticle == null) {
            return Response.error(ResponseCodeEnums.NOT_FOUND, "文章不存在");
        }

        if (!permissionChecker.check(existingArticle.getAuthId())) {
            return Response.error(ResponseCodeEnums.FORBIDDEN, "无权操作此文章");
        }

        UpdateWrapper<Article> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", id);

        if (articleDTO.getTitle() != null) {
            updateWrapper.set("title", articleDTO.getTitle());
        }
        if (articleDTO.getSummary() != null) {
            updateWrapper.set("summary", articleDTO.getSummary());
        }
        if (articleDTO.getContent() != null) {
            updateWrapper.set("content", articleDTO.getContent());
        }
        if (articleDTO.getCoverUrl() != null) {
            updateWrapper.set("cover_url", articleDTO.getCoverUrl());
        }
        if (articleDTO.getStatus() != null) {
            updateWrapper.set("status", articleDTO.getStatus());
        }

        boolean updateSuccess = this.update(updateWrapper);
        if (!updateSuccess) {
            return Response.error(ResponseCodeEnums.INTERNAL_SERVER_ERROR, "更新失败");
        }

        if (articleDTO.getCategoryId() != null) {
            articleCategoryMapper.deleteByArticleId(id);
            ArticleCategory articleCategory = ArticleCategory.builder()
                    .articleId(id)
                    .categoryId(articleDTO.getCategoryId())
                    .build();
            articleCategoryMapper.insert(articleCategory);
        }

        evictCache(id);
        sendArticleSyncMessage(id, "UPDATE");

        Article updatedArticle = this.getById(id);
        ArticleUpdateDTO resultDTO = articleConvertMapper.convertToUpdateDTO(updatedArticle);
        
        Long categoryId = articleCategoryMapper.selectCategoryIdByArticleId(id);
        resultDTO.setCategoryId(categoryId);
        
        return Response.success(resultDTO);
    }

    @Override
    @Transactional
    public Response<Object> deleteArticle(Long articleId) {
        if (articleId == null) {
            return Response.error(ResponseCodeEnums.BAD_REQUEST, "文章ID不能为空");
        }

        Article article = this.getById(articleId);
        if (article == null) {
            return Response.error(ResponseCodeEnums.NOT_FOUND, "文章不存在");
        }

        if (!permissionChecker.check(article.getAuthId())) {
            return Response.error(ResponseCodeEnums.FORBIDDEN, "无权操作此文章");
        }

        evictCache(articleId);

        articleCategoryMapper.deleteByArticleId(articleId);

        try {
            articleFileService.deleteArticleFiles(articleId);
        } catch (Exception e) {
            log.warn("删除文章关联文件失败，articleId: {}", articleId, e);
        }

        boolean deleteSuccess = this.removeById(articleId);
        if (!deleteSuccess) {
            throw new BusinessException(ResponseCodeEnums.INTERNAL_SERVER_ERROR, "删除文章失败");
        }

        sendArticleSyncMessage(articleId, "DELETE");

        return Response.success(null);
    }

    @Override
    public Response<Article> getArticleById(Long id) {
        if (id == null) {
            return Response.error(ResponseCodeEnums.BAD_REQUEST, "文章ID不能为空");
        }

        boolean isHot = hotArticleService.isHotArticle(id);
        String key = cacheUtil.generateKey(CacheConstants.CACHE_PREFIX_ARTICLE_DETAIL, id);
        Article cacheArticle = cacheUtil.get(key, Article.class, CacheTypeEnum.BOTH, isHot);

        if (cacheArticle != null) {
            accessCounter.recordAccess(String.valueOf(id));
            int increment = accessCounter.getReadCountIncrement(String.valueOf(id));
            Article resultArticle = Article.builder()
                    .id(cacheArticle.getId())
                    .authId(cacheArticle.getAuthId())
                    .title(cacheArticle.getTitle())
                    .summary(cacheArticle.getSummary())
                    .content(cacheArticle.getContent())
                    .coverUrl(cacheArticle.getCoverUrl())
                    .status(cacheArticle.getStatus())
                    .readCount((cacheArticle.getReadCount() == null ? 0 : cacheArticle.getReadCount()) + increment)
                    .likeCount(cacheArticle.getLikeCount())
                    .createdAt(cacheArticle.getCreatedAt())
                    .updatedAt(cacheArticle.getUpdatedAt())
                    .build();
            return Response.success(resultArticle);
        }

        Article article = articleMapper.selectArticleWithAuthorById(id);
        if (article != null) {
            cacheUtil.put(key, article, CacheConstants.ARTICLE_OBJ_EXPIRE, TimeUnit.MINUTES, CacheTypeEnum.BOTH);
            accessCounter.recordAccess(String.valueOf(id));

            int increment = accessCounter.getReadCountIncrement(String.valueOf(id));
            Article resultArticle = Article.builder()
                    .id(article.getId())
                    .authId(article.getAuthId())
                    .title(article.getTitle())
                    .summary(article.getSummary())
                    .content(article.getContent())
                    .coverUrl(article.getCoverUrl())
                    .status(article.getStatus())
                    .readCount((article.getReadCount() == null ? 0 : article.getReadCount()) + increment)
                    .likeCount(article.getLikeCount())
                    .createdAt(article.getCreatedAt())
                    .updatedAt(article.getUpdatedAt())
                    .build();
            return Response.success(resultArticle);
        }

        return Response.success(article);
    }

    @Override
    public Response<PaginationResponse<ArticleVO>> pageArticles(ArticleQueryDTO queryDTO) {
        log.info("分页查询参数: categoryId={}, keyword={}, status={}",
                queryDTO.getCategoryId(), queryDTO.getKeyword(), queryDTO.getStatus());

        Long current = queryDTO.getCurrent();
        Long size = queryDTO.getSize();
        current = current == null || current < 1 ? 1L : current;
        size = size == null || size < 1 ? 10L : size;

        Page<Article> page = new Page<>(current, size);

        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<Article>()
                .select(Article::getId)
                .orderByDesc(Article::getCreatedAt);

        if (queryDTO.getStatus() != null) {
            wrapper.eq(Article::getStatus, queryDTO.getStatus());
        }

        if (queryDTO.getKeyword() != null && !queryDTO.getKeyword().isEmpty()) {
            wrapper.like(Article::getTitle, queryDTO.getKeyword());
        }

        Long categoryId = queryDTO.getCategoryId();
        if (categoryId != null) {
            List<Long> categoryArticleIds = articleCategoryMapper.selectArticleIdsByCategoryId(categoryId);
            log.debug("分类 {} 关联的文章ID列表: {}", categoryId, categoryArticleIds);

            if (categoryArticleIds == null || categoryArticleIds.isEmpty()) {
                log.debug("分类 {} 没有关联的文章，返回空结果", categoryId);
                PaginationResponse<ArticleVO> emptyResponse = new PaginationResponse<>();
                emptyResponse.setCurrent(Math.toIntExact(current));
                emptyResponse.setSize(Math.toIntExact(size));
                emptyResponse.setTotal(0L);
                emptyResponse.setPages(0);
                emptyResponse.setRecords(new ArrayList<>());
                return Response.success(emptyResponse);
            }
            wrapper.in(Article::getId, categoryArticleIds);
        }

        IPage<Article> articlePage = articleMapper.selectPage(page, wrapper);
        log.debug("查询到的文章数量: {}", articlePage.getTotal());

        List<Long> articleIds = articlePage.getRecords().stream()
                .map(Article::getId)
                .filter(Objects::nonNull)
                .toList();

        List<String> cacheKeys = articleIds.stream()
                .map(articleId -> CacheConstants.CACHE_PREFIX_ARTICLE_LIST + ":" + articleId)
                .toList();
        List<Article> cachedArticles = cacheUtil.multiGet(cacheKeys, Article.class);

        Map<Long, Article> hitArticleMap = new HashMap<>();
        List<Long> missIds = new ArrayList<>();
        for (int i = 0; i < articleIds.size(); i++) {
            Long articleId = articleIds.get(i);
            Article cachedArticle = cachedArticles.get(i);
            if (cachedArticle != null) {
                hitArticleMap.put(articleId, cachedArticle);
            } else {
                missIds.add(articleId);
            }
        }

        if (!missIds.isEmpty()) {
            List<Article> missArticles = articleMapper.selectList(
                    new LambdaQueryWrapper<Article>()
                            .in(Article::getId, missIds)
                            .select(Article::getId, Article::getSummary, Article::getReadCount,
                                    Article::getCoverUrl, Article::getStatus, Article::getCreatedAt,
                                    Article::getUpdatedAt, Article::getAuthId, Article::getTitle)
            );
            for (Article article : missArticles) {
                hitArticleMap.put(article.getId(), article);
                String cacheKey = cacheUtil.generateKey(CacheConstants.CACHE_PREFIX_ARTICLE_LIST, article.getId());
                cacheUtil.put(cacheKey, article, CacheConstants.PAGE_ID_EXPIRE, TimeUnit.MINUTES, CacheTypeEnum.BOTH);
            }
        }

        Map<Long, Category> articleCategoryMap = new HashMap<>();
        if (!articleIds.isEmpty()) {
            List<Map<String, Object>> articleCategoryRelations = articleCategoryMapper.selectCategoryIdsByArticleIds(articleIds);
            
            List<Long> categoryIds = articleCategoryRelations.stream()
                    .map(m -> ((Number) m.get("category_id")).longValue())
                    .distinct()
                    .toList();
            
            Map<Long, Category> categoryMap = new HashMap<>();
            if (!categoryIds.isEmpty()) {
                List<Category> categories = categoryMapper.selectBatchIds(categoryIds);
                categoryMap = categories.stream()
                        .collect(Collectors.toMap(Category::getId, c -> c));
            }
            
            for (Map<String, Object> relation : articleCategoryRelations) {
                Long articleId = ((Number) relation.get("article_id")).longValue();
                Long catId = ((Number) relation.get("category_id")).longValue();
                if (categoryMap.containsKey(catId)) {
                    articleCategoryMap.put(articleId, categoryMap.get(catId));
                }
            }
        }

        Map<Long, Long> likeCountMap = likeService.batchGetLikeCount(articleIds, 1);

        List<ArticleVO> articleVOList = new ArrayList<>();
        for (Long articleId : articleIds) {
            Article article = hitArticleMap.get(articleId);
            Long realTimeLikeCount = likeCountMap.getOrDefault(articleId, 0L);
            if (article != null) {
                ArticleVO articleVO = new ArticleVO();
                articleVO.setId(article.getId());
                articleVO.setTitle(article.getTitle());
                articleVO.setSummary(article.getSummary());
                articleVO.setReadCount((article.getReadCount() == null ? 0 : article.getReadCount()) + accessCounter.getReadCountIncrement(String.valueOf(articleId)));
                articleVO.setLikeCount(realTimeLikeCount);
                articleVO.setCoverUrl(article.getCoverUrl());
                articleVO.setStatus(article.getStatus());
                articleVO.setCreatedAt(article.getCreatedAt());
                articleVO.setUpdatedAt(article.getUpdatedAt());
                articleVO.setAuthId(article.getAuthId());

                Category category = articleCategoryMap.get(articleId);
                if (category != null) {
                    articleVO.setCategoryId(category.getId());
                    articleVO.setCategoryName(category.getName());
                }

                articleVOList.add(articleVO);
            }
        }

        PaginationResponse<ArticleVO> paginationResponse = new PaginationResponse<>();
        paginationResponse.setCurrent(Math.toIntExact(current));
        paginationResponse.setSize(Math.toIntExact(size));
        paginationResponse.setTotal(articlePage.getTotal());
        paginationResponse.setPages((int) articlePage.getPages());
        paginationResponse.setRecords(articleVOList);

        return Response.success(paginationResponse);
    }

    @Override
    public Response<ArticleUpdateDTO> getDraft() {
        Long userId = permissionChecker.getCurrentUserId();
        if (userId == null) {
            return Response.error(ResponseCodeEnums.UNAUTHORIZED, "请先登录");
        }

        ArticleUpdateDTO draft = articleMapper.findDraftByAuthId(userId);
        if (draft == null) {
            Article empty = Article.builder()
                    .authId(userId)
                    .content("")
                    .coverUrl("")
                    .title("")
                    .summary("")
                    .likeCount(0L)
                    .readCount(0)
                    .status(0)
                    .build();
            articleMapper.insert(empty);
            draft = articleConvertMapper.convertToUpdateDTO(empty);
        }
        return Response.success(draft);
    }

    private void evictCache(Long articleId) {
        boolean isHot = hotArticleService.isHotArticle(articleId);
        
        String listKey = cacheUtil.generateKey(CacheConstants.CACHE_PREFIX_ARTICLE_LIST, articleId);
        cacheUtil.evict(listKey, CacheTypeEnum.BOTH, isHot);
        
        String detailKey = cacheUtil.generateKey(CacheConstants.CACHE_PREFIX_ARTICLE_DETAIL, articleId);
        cacheUtil.evict(detailKey, CacheTypeEnum.BOTH, isHot);
    }

    @Override
    public Response<StatsVO> getStats() {
        Long articleCount = articleMapper.selectCount(
            new LambdaQueryWrapper<Article>().eq(Article::getStatus, 1)
        );
        
        Long categoryCount = categoryMapper.selectCount(null);
        
        Long totalReadCount = articleMapper.selectSumReadCount();
        
        Long totalLikeCount = likeService.getTotalLikeCount(1);
        
        Long userCount = articleMapper.selectUserCount();

        StatsVO statsVO = StatsVO.builder()
                .articleCount(articleCount != null ? articleCount : 0L)
                .categoryCount(categoryCount != null ? categoryCount : 0L)
                .totalReadCount(totalReadCount != null ? totalReadCount : 0L)
                .totalLikeCount(totalLikeCount != null ? totalLikeCount : 0L)
                .userCount(userCount != null ? userCount : 0L)
                .build();

        return Response.success(statsVO);
    }

    @Override
    public Response<List<ArticleVO>> getHotArticles(Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 5;
        }

        List<Article> hotArticles = articleMapper.selectList(
            new LambdaQueryWrapper<Article>()
                .select(Article::getId, Article::getTitle, Article::getReadCount)
                .eq(Article::getStatus, 1)
                .orderByDesc(Article::getReadCount)
                .last("LIMIT " + limit)
        );

        List<ArticleVO> articleVOList = hotArticles.stream().map(article -> {
            ArticleVO vo = new ArticleVO();
            vo.setId(article.getId());
            vo.setTitle(article.getTitle());
            vo.setReadCount(article.getReadCount() == null ? 0 : article.getReadCount());
            vo.setLikeCount(likeService.getLikeCount(article.getId(), 1));
            return vo;
        }).collect(Collectors.toList());

        return Response.success(articleVOList);
    }

    @Override
    public Response<List<ArticleVO>> getRelatedArticles(Long articleId, Integer limit) {
        if (articleId == null) {
            return Response.success(Collections.emptyList());
        }
        if (limit == null || limit <= 0) {
            limit = 5;
        }

        Long categoryId = articleCategoryMapper.selectCategoryIdByArticleId(articleId);
        if (categoryId == null) {
            return Response.success(Collections.emptyList());
        }

        List<Long> relatedArticleIds = articleCategoryMapper.selectArticleIdsByCategoryId(categoryId);
        
        List<Long> filteredIds = relatedArticleIds.stream()
                .filter(id -> !id.equals(articleId))
                .limit(limit)
                .collect(Collectors.toList());

        if (filteredIds.isEmpty()) {
            return Response.success(Collections.emptyList());
        }

        List<Article> relatedArticles = articleMapper.selectList(
            new LambdaQueryWrapper<Article>()
                .in(Article::getId, filteredIds)
                .eq(Article::getStatus, 1)
                .select(Article::getId, Article::getTitle, Article::getSummary, 
                        Article::getCoverUrl, Article::getReadCount, Article::getCreatedAt)
        );

        List<ArticleVO> articleVOList = relatedArticles.stream().map(article -> {
            ArticleVO vo = new ArticleVO();
            vo.setId(article.getId());
            vo.setTitle(article.getTitle());
            vo.setSummary(article.getSummary());
            vo.setCoverUrl(article.getCoverUrl());
            vo.setReadCount(article.getReadCount() == null ? 0 : article.getReadCount());
            vo.setCreatedAt(article.getCreatedAt());
            vo.setLikeCount(likeService.getLikeCount(article.getId(), 1));
            return vo;
        }).collect(Collectors.toList());

        return Response.success(articleVOList);
    }

    @Override
    public Response<Object> reindexAllToES() {
        log.info("开始重建ES索引...");
        try {
            List<Article> articles = articleMapper.selectList(
                new LambdaQueryWrapper<Article>()
                    .eq(Article::getStatus, 1)
            );

            if (articles.isEmpty()) {
                log.info("没有已发布的文章需要索引");
                return Response.success("没有已发布的文章");
            }

            List<ArticleDocument> documents = articles.stream().map(article -> {
                int commentCount = commentMapper.countByArticleId(article.getId());
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
                    .coverUrl(article.getCoverUrl())
                    .createdAt(article.getCreatedAt())
                    .updatedAt(article.getUpdatedAt())
                    .build();
            }).collect(Collectors.toList());

            searchService.bulkIndex(documents);
            log.info("ES索引重建完成，共索引 {} 篇文章", documents.size());
            return Response.success("重建完成，共索引 " + documents.size() + " 篇文章");
        } catch (Exception e) {
            log.error("ES索引重建失败", e);
            return Response.error(ResponseCodeEnums.INTERNAL_SERVER_ERROR, "索引重建失败: " + e.getMessage());
        }
    }

    private void sendArticleSyncMessage(Long articleId, String operation) {
        ArticleSyncMessage message = ArticleSyncMessage.builder()
            .articleId(articleId)
            .operation(operation)
            .build();
        
        rocketMQTemplate.asyncSend(
            ARTICLE_SYNC_TOPIC,
            message,
            new DefaultSendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    log.info("文章同步消息发送成功 | msgId:{} | articleId:{} | operation:{}",
                            sendResult.getMsgId(), articleId, operation);
                }

                @Override
                public void onException(Throwable e) {
                    log.error("文章同步消息发送失败 | articleId:{} | operation:{}", 
                            articleId, operation, e);
                }
            },
            SEND_TIMEOUT
        );
    }
}

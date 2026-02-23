package com.yry.blog.myblogarticle.service.Impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yry.blog.myblogadmin.auth.Impl.PermissionCheckerImpl;
import com.yry.blog.myblogarticle.HotPost.HotPostJudge;
import com.yry.blog.myblogarticle.dto.ArticleQueryDTO;
import com.yry.blog.myblogarticle.dto.ArticleUpdateDTO;
import com.yry.blog.myblogarticle.mapper.ArticleMapper;
import com.yry.blog.myblogarticle.mapper.mastruct.ArticleConvertMapper;
import com.yry.blog.myblogarticle.service.ArticleService;
import com.yry.blog.myblogarticle.service.LikeService;
import com.yry.blog.myblogarticle.vo.ArticleVO;
import com.yry.blog.myblogcommon.cache.constants.CacheConstants;
import com.yry.blog.myblogcommon.cache.enums.CacheTypeEnum;
import com.yry.blog.myblogcommon.cache.util.CacheUtil;
import com.yry.blog.myblogcommon.entity.article.Article;
import com.yry.blog.myblogcommon.enums.ResponseCodeEnums;
import com.yry.blog.myblogcommon.exception.BusinessException;
import com.yry.blog.myblogcommon.result.PaginationResponse;
import com.yry.blog.myblogcommon.result.Response;
import com.yry.blog.myblogcommon.stat.PostAccessCounter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


/**
 * 文章服务实现类
 * 负责文章的CRUD操作，封面、头像等MinIO相关逻辑后续补充
 */
@Service
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService {
    @Autowired
    private PermissionCheckerImpl permissionChecker;
    @Autowired
    private ArticleMapper articleMapper;
    @Autowired
    private ArticleConvertMapper articleConvertMapper;
    @Autowired
    private PostAccessCounter accessCounter; // 计数组件
    @Autowired
    private HotPostJudge hotPostJudge;
    @Autowired
    private CacheUtil cacheUtil;
    @Autowired
    private LikeService likeService;


    @Transactional // 关键：使用事务保证操作的原子性
    @Override
    public void publishArticle(Long articleId) {
        if (articleId == null) {
            throw new BusinessException(ResponseCodeEnums.BAD_REQUEST, "文章ID不能为空");
        }

        Article draftArticle = this.getById(articleId);
        if (draftArticle == null) {
            throw new BusinessException(ResponseCodeEnums.NOT_FOUND, "文章不存在，无法发布");
        }
        // 可选：检查当前状态是否已经是已发布，避免重复操作
        if (draftArticle.getStatus() == 1) {
            log.warn("文章 [{}] 已处于发布状态，无需重复发布");
            return; // 或抛出 BUSINESS_ERROR，根据业务需求
        }
        draftArticle.setStatus(1); // 1 表示已发布
        this.updateById(draftArticle);
    }
    /**
     * 更新文章内容
     * @param articleDTO 文章更新DTO
     * @return 响应结果
     */
    @Override
    public Response<ArticleUpdateDTO> updateArticle(Long id,ArticleUpdateDTO articleDTO) {
        // 1. 校验articleId必传
        if (id == null) {
            return Response.error(ResponseCodeEnums.BAD_REQUEST);
        }

        // 2. 查询现有文章（校验存在性 + 所有权）
        Article existingArticle = this.getById(id);
        if (existingArticle == null) {
            return Response.error(ResponseCodeEnums.NOT_FOUND);
        }
        if (!permissionChecker.check(existingArticle.getAuthId())) {
            return Response.error(ResponseCodeEnums.FORBIDDEN); // 鉴权
        }

        // 3. 构建更新条件 + 只更新需要修改的字段（关键：局部更新）
        UpdateWrapper<Article> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", id) // 仅更新当前文章
                .set("title", articleDTO.getTitle()) // 标题
                .set("summary", articleDTO.getSummary()) // 摘要
                .set("content", articleDTO.getContent()); // 内容

        // 4. 执行更新并校验结果
        boolean updateSuccess = this.update(updateWrapper);
        if (!updateSuccess) {
            // 更新失败返回服务器错误（可自定义枚举）
            return Response.error(ResponseCodeEnums.INTERNAL_SERVER_ERROR);
        }
        // 5.删除缓存
        boolean isHot = hotPostJudge.isHotPost(String.valueOf(id)); // 判断是否为热门文章
        if (isHot) {
            System.out.println('1');
        }
        else {
            System.out.println('0');
        }
        String key = cacheUtil.generateKey(CacheConstants.CACHE_PREFIX_ARTICLE,id); // 生成对应的键名
        System.out.println(key);
        cacheUtil.evict(key, CacheTypeEnum.BOTH,isHot); // 更新时两级缓存都要删除

        // 6. 返回更新后的实际数据（可选：重新查询或组装DTO）
        Article updatedArticle = this.getById(id);
        // 把更新后的Article转成DTO返回（需自己实现转换逻辑，比如BeanUtils.copyProperties）
        return Response.success(articleConvertMapper.convertToUpdateDTO(updatedArticle));
    }

    /**
     * 删除文章
     *
     * @param articleId 文章ID
     * @return 是否删除成功
     */
    @Override
    public Response<Object> deleteArticle(Long articleId) {
        Long authid = this.getById(articleId).getAuthId();
        if (!permissionChecker.check(authid)){
            return Response.error(ResponseCodeEnums.PERMISSION_DENIED); // 验证权限
        }
        if (articleId == null) {
            return Response.error(ResponseCodeEnums.BAD_REQUEST, "文章不存在！"); //验证传入id是否有效
        }
        // 删除缓存
        boolean isHot = hotPostJudge.isHotPost(String.valueOf(articleId));
        String key = cacheUtil.generateKey(CacheConstants.CACHE_PREFIX_ARTICLE,"detail",articleId);
        cacheUtil.evict(key, CacheTypeEnum.BOTH,isHot);
        return Response.success(this.removeById(articleId));
    }

    /**
     * 根据ID查询文章
     * @param id 文章ID
     * @return 文章详情响应
     */
    @Override
    public Response<Article> getArticleById(Long id) {
        boolean isHot = hotPostJudge.isHotPost(String.valueOf(id));
        String key = cacheUtil.generateKey(CacheConstants.CACHE_PREFIX_ARTICLE,id);
        Article cacheArticle = cacheUtil.get(key,Article.class,CacheTypeEnum.BOTH,isHot);
        if (cacheArticle != null) {
            // 缓存命中：统计访问量（全链路统计，不能漏）
            accessCounter.recordAccess(String.valueOf(id));
            return Response.success(cacheArticle);
        }
        // 缓存未命中，查表，回填缓存
        Article article = this.getById(id);
        cacheUtil.put(
                key,
                article,
                300, // 过期时间（你的CacheUtil里应该有默认值，也可以直接用常量）
                TimeUnit.SECONDS,
                CacheTypeEnum.BOTH
        );
        if (article != null) {
            accessCounter.recordAccess(String.valueOf(id));
        }
        // 可在此处补充阅读量+1等逻辑
        return Response.success(article);
    }

    /**
     * 分页查询文章
     * @param queryDTO 查询条件DTO
     * @return 分页结果
     */
    @Override
    public Response<PaginationResponse<ArticleVO>> pageArticles(ArticleQueryDTO queryDTO) {
        // 1. 获取分页参数并初始化分页对象
        Long current = queryDTO.getCurrent();
        Long size = queryDTO.getSize();
        // 校验分页参数，避免空值或非法值
        current = current == null || current < 1 ? 1L : current;
        size = size == null || size < 1 ? 10L : size;

        Page<Article> page = new Page<>(current, size);

        // 2. 仅查询文章ID（减少数据库查询压力），并按创建时间倒序
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<Article>()
                .select(Article::getId)
                .orderByDesc(Article::getCreatedAt);
        IPage<Article> articlePage = articleMapper.selectPage(page, wrapper);

        // 3. 提取分页查询到的ID列表
        List<Long> articleIds = articlePage.getRecords().stream()
                .map(Article::getId)
                .filter(Objects::nonNull) // 过滤空ID，避免缓存查询异常
                .toList();

        // 4. 批量查询缓存（核心优化点：减少缓存IO次数）
        List<String> cacheKeys = articleIds.stream()
                .map(id -> CacheConstants.CACHE_PREFIX_ARTICLE + id)
                .toList();
        List<Article> cachedArticles = cacheUtil.multiGet(cacheKeys, Article.class);

        // 5. 分离缓存命中和未命中的ID
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

        // 6. 缓存未命中的ID，批量查询数据库并更新缓存
        if (!missIds.isEmpty()) {
            List<Article> missArticles = articleMapper.selectList(
                    new QueryWrapper<Article>()
                            .in("id", missIds)
                            .select("id", "summary", "read_count", "cover_url", "status", "created_at", "updated_at","auth_id","title")
            );
            // 将数据库查询结果存入map，并更新缓存
            for (Article article : missArticles) {
                hitArticleMap.put(article.getId(), article);
                // 缓存未命中的文章，避免下次查询再次查库
                String cacheKey = cacheUtil.generateKey(CacheConstants.CACHE_PREFIX_ARTICLE,article.getId());
                cacheUtil.put(cacheKey, article,CacheConstants.PAGE_ID_EXPIRE,TimeUnit.MINUTES,CacheTypeEnum.BOTH); // 这里可根据需求添加过期时间，如cacheUtil.set(cacheKey, article, 3600L);
            }
        }

        // 7. 按原ID顺序组装ArticleVO（保证分页结果顺序和查询ID顺序一致）
        List<ArticleVO> articleVOList = new ArrayList<>();
        for (Long articleId : articleIds) {
            Article article = hitArticleMap.get(articleId);
            Long realTimeLikeCount = likeService.getLikeCount(articleId, 1); // 1=文章类型
            if (article != null) {
                ArticleVO articleVO = new ArticleVO();
                articleVO.setId(article.getId());
                articleVO.setTitle(article.getTitle());
                articleVO.setSummary(article.getSummary());
                articleVO.setReadCount(article.getReadCount());
                articleVO.setLikeCount(realTimeLikeCount); // 展示部分用redis做展示
                articleVO.setCoverUrl(article.getCoverUrl());
                articleVO.setStatus(article.getStatus());
                articleVO.setCreatedAt(article.getCreatedAt());
                articleVO.setUpdatedAt(article.getUpdatedAt());
                articleVO.setAuthId(article.getAuthId());
                articleVOList.add(articleVO);
            }
        }

        // 8. 组装分页响应结果
        PaginationResponse<ArticleVO> paginationResponse = new PaginationResponse<>();
        paginationResponse.setCurrent(Math.toIntExact(current));
        paginationResponse.setSize(Math.toIntExact(size));
        paginationResponse.setTotal(articlePage.getTotal()); // 总条数
        paginationResponse.setPages((int) articlePage.getPages()); // 总页数
        paginationResponse.setRecords(articleVOList);

        // 9. 返回统一响应体
        return Response.success(paginationResponse);
    }
    public Response<ArticleUpdateDTO> getDraft() {
        Long userId = permissionChecker.getCurrentUserId();
        ArticleUpdateDTO draft = articleMapper.findDraftByAuthId(userId);
        if (draft == null) {
            // 自动创建空草稿（status=0）
            Article empty =  Article.builder()
                    .authId(userId)
                    .content("")
                    .coverUrl("")
                    .title("")
                    .summary("")
                    .likeCount(0)
                    .readCount(0)
                    .build();
            articleMapper.insert(empty);
            draft = articleConvertMapper.convertToUpdateDTO(empty); // 转 DTO
        }
        return Response.success(draft);
    }
}
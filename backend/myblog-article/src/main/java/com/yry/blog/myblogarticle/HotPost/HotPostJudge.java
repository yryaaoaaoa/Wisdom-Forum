package com.yry.blog.myblogarticle.HotPost;

import com.yry.blog.myblogarticle.mapper.ArticleMapper;
import com.yry.blog.myblogcommon.entity.article.Article;
import com.yry.blog.myblogcommon.stat.PostAccessCounter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;

@Component
public class HotPostJudge {
    // 可配置的阈值（改配置文件就行，不用改代码）
    @Value("${forum.hot.post.access-10min:30}")
    private int access10minThreshold;
    @Value("${forum.hot.post.access-1h:1}")
    private int access1hThreshold;

    @Autowired
    private PostAccessCounter accessCounter;
    @Autowired
    private ArticleMapper postMapper;

    // 唯一的判定入口：所有场景都调这个方法
    public boolean isHotPost(String postId) {
        // 1. 基础门槛：已发布、无违规、72小时内发布
        Article post = postMapper.selectById(postId);
        if (post == null || post.getStatus() != 1) {
            return false;
        }
        // 72小时内发布的帖子才判定为热门
        if ((System.currentTimeMillis() - post.getCreatedAt().toEpochSecond(ZoneOffset.of("+8")) * 1000L) > 72 * 3600 * 1000L) {
            return false;
        }

        // 2. 核心指标：10分钟访问≥30次或者一小时内访问≥100次
        int accessCount10min = accessCounter.getAccessCount(postId, "10min");
        int accessCount1h = accessCounter.getAccessCount(postId, "1h");
        if (accessCount10min < access10minThreshold || accessCount1h < access1hThreshold) {
            return false;
        }

        // 3. 辅助指标：防刷量（互动数≥20）
        int interactCount = post.getLikeCount();
        return interactCount >= 20;
    }
}
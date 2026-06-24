package com.yry.blog.myblogarticle.service;

import java.util.List;
import java.util.Map;


public interface LikeService {
    boolean like(Long targetId, Integer likeType);

    Long getLikeCount(Long targetId, Integer likeType);

    boolean hasLiked(Long targetId, Integer likeType);

    Long getTotalLikeCount(Integer likeType);

    Map<Long, Long> batchGetLikeCount(List<Long> targetIds, Integer likeType);
}

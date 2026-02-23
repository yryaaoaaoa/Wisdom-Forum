package com.yry.blog.myblogarticle.service;


public interface LikeService {
    boolean like(Long targetId, Integer likeType);

    Long getLikeCount(Long targetId, Integer likeType);
}

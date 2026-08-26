package com.leisure.postLike.repository;


import com.leisure.postLike.domain.LikedPostSort;
import com.leisure.postLike.dto.result.LikedPostResult;

import java.util.List;

public interface PostLikeCustom {

    List<LikedPostResult> findLikedPosts(Long memberId, LikedPostSort sort, long offset, int size);
}

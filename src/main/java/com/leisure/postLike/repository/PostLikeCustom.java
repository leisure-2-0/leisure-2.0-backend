package com.leisure.postLike.repository;


import com.leisure.postLike.domain.LikedPostSort;
import com.leisure.postLike.dto.response.LikedPostResponse;

import java.util.List;

public interface PostLikeCustom {

    List<LikedPostResponse> findLikedPosts(Long memberId, LikedPostSort sort, long offset, int size);
}

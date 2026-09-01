package com.leisure.postlike.repository;


import com.leisure.postlike.domain.LikedPostSort;
import com.leisure.postlike.dto.result.LikedPostResult;

import java.util.List;

public interface PostLikeRepositoryCustom {

    List<LikedPostResult> findLikedPosts(Long memberId, LikedPostSort sort, long offset, int size);
}

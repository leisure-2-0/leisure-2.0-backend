package com.leisure.post.repository;

import com.leisure.post.domain.MyPostSort;
import com.leisure.post.dto.response.MyPostResponse;
import com.leisure.post.dto.response.PostDetailResponse;

import java.util.List;
import java.util.Optional;

public interface PostCustom {

    List<MyPostResponse> findMyPosts(Long memberId, MyPostSort sort, long offset, int size);

    Optional<PostDetailResponse> findPostDetail(Long memberId, Long postId);
}

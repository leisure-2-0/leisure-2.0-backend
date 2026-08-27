package com.leisure.post.repository;

import com.leisure.post.domain.*;
import com.leisure.post.dto.response.MainFeedPostResponse;
import com.leisure.post.dto.response.MyPostResponse;
import com.leisure.post.dto.response.PostResponse;
import com.leisure.post.dto.result.PostDetailResult;

import java.util.List;
import java.util.Optional;

// 기능 목록 _ 실제 구현은 PostRepositoryImpl에서 수행
public interface PostCustom {

    List<MyPostResponse> findMyPosts(Long memberId, MyPostSort sort, long offset, int size);

    List<PostResponse> findPosts(Long memberId, PostCategory category, PostSort sort, PostCursor cursor, int size);

    List<MainFeedPostResponse> findMainFeedPosts(Long memberId, PostCategory category, PostSort sort, int limit);

    Optional<PostDetailResult> findPostDetail(Long memberId, Long postId);
}

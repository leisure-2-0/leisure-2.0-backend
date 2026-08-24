package com.leisure.post.repository;

import com.leisure.post.domain.MyPostSort;
import com.leisure.post.domain.PostCategory;
import com.leisure.post.domain.PostCursor;
import com.leisure.post.domain.PostSort;
import com.leisure.post.dto.response.MyPostResponse;
import com.leisure.post.dto.response.PostResponse;
import com.leisure.post.dto.result.PostDetailResult;

import java.util.List;
import java.util.Optional;

public interface PostCustom {

    List<MyPostResponse> findMyPosts(Long memberId, MyPostSort sort, long offset, int size);

    List<PostResponse> findPosts(Long memberId, PostCategory category, PostSort sort, PostCursor cursor, int size);

    Optional<PostDetailResult> findPostDetail(Long memberId, Long postId);
}

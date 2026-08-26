package com.leisure.post.repository;

import com.leisure.post.domain.*;
import com.leisure.post.dto.result.MainFeedPostResult;
import com.leisure.post.dto.result.MyPostResult;
import com.leisure.post.dto.result.PostResult;
import com.leisure.post.dto.result.PostDetailResult;

import java.util.List;
import java.util.Optional;

public interface PostCustom {

    List<MyPostResult> findMyPosts(Long memberId, MyPostSort sort, long offset, int size);

    List<PostResult> findPosts(Long memberId, PostCategory category, PostSort sort, PostCursor cursor, int size);

    List<MainFeedPostResult> findMainFeedPosts(Long memberId, PostCategory category, PostSort sort, int limit);

    Optional<PostDetailResult> findPostDetail(Long memberId, Long postId);
}

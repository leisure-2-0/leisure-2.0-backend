package com.leisure.post.repository;

import com.leisure.post.domain.MyPostSort;
import com.leisure.post.dto.response.MyPostResponse;

import java.util.List;

public interface PostCustom {

    List<MyPostResponse> findMyPosts(Long memberId, MyPostSort sort, long offset, int size);
}

package com.leisure.map.dto.response;

import com.leisure.post.domain.PostCategory;

public record PostMapPinResponse(
        Long postId, // 게시글 ID
        String title, // 게시글 제목
        PostCategory category, // 게시글 카테고리
        double latitude, // 게시글 위도
        double longitude // 게시글 경도
) {
}

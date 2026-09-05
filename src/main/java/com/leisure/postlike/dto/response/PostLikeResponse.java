package com.leisure.postlike.dto.response;

public record PostLikeResponse(
        Long memberId,

        Long postId,

        int likeCount,

        boolean isLiked
) {}

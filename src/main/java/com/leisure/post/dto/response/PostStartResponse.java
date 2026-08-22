package com.leisure.post.dto.response;

import com.leisure.post.domain.PostStatus;

public record PostStartResponse(Long postId, PostStatus status) {}

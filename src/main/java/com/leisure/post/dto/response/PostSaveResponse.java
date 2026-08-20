package com.leisure.post.dto.response;

import com.leisure.post.domain.PostStatus;

public record PostSaveResponse(Long postId, PostStatus status) {}

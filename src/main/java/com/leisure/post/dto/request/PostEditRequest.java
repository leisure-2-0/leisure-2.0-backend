package com.leisure.post.dto.request;

import com.leisure.post.domain.PostCategory;
import jakarta.validation.constraints.Size;

public record PostEditRequest(
        @Size(max = 50)
        String title,

        String content,

        PostCategory category
) {}

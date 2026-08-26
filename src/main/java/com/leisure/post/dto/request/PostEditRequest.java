package com.leisure.post.dto.request;

import com.leisure.post.domain.PostCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record PostEditRequest(
        @Size(max = 50)
        String title,

        String content,

        PostCategory category,

        @Size(max = 5)
        Set<String> tags,

        @Valid
        LocationRequest location
) {}

package com.leisure.post.dto.response;

import java.util.List;

public record PostListResponse(

        List<PostResponse> posts,

        String nextCursor,

        boolean hasNext
) {}

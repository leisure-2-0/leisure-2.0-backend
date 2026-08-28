package com.leisure.post.dto.request;

import com.leisure.post.domain.PostCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record PostPublishRequest (

        @Size(max = 50)
        String title,

        String content,

        PostCategory category,

        @Schema(description = "태그 목록(최대 5개). null이면 기존 태그 유지, 빈 배열이면 전체 제거.")
        @Size(max = 5)
        Set<String> tags,

        @Schema(description = "위치 정보. null이면 기존 위치 유지. 값이 오면 통째로 교체되며 내부 필드는 일부 null이어도 그대로 저장된다(전 필드가 null이면 무시되어 유지).")
        @Valid
        LocationRequest location
) {}

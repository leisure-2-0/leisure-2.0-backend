package com.leisure.post.controller;

import com.leisure.global.auth.CurrentMember;
import com.leisure.global.response.ApiResponse;
import com.leisure.post.dto.request.PostEditRequest;
import com.leisure.post.dto.request.PostPublishRequest;
import com.leisure.post.dto.request.PostSaveRequest;
import com.leisure.post.dto.response.*;
import com.leisure.post.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(
        name = "게시글(Post)",
        description = "게시글 컨테이너 생성, 임시 저장, 게시글 게시, 게시글 수정, 게시글 삭제"
)
@RestController
@RequiredArgsConstructor
public class PostController {

    private final PostService service;

    @Operation(
            summary = "게시글 컨테이너 생성",
            description = "클라이언트에서 게시글 작성 버튼을 누르면 게시글 컨테이너가 생성되고 상태는 작성중으로 초기화된다."
    )
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping("/posts")
    public ResponseEntity<ApiResponse<PostStartResponse>> startPosting(@CurrentMember String publicId) {

        PostStartResponse response = service.startPosting(publicId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response.postId() + "번 게시글 작성을 시작했습니다.", response));
    }

    @Operation(
            summary = "게시글 임시 저장",
            description= "임시 저장 버튼이나 자동 저장 시 호출된다. 작성중(WRITING) 상태면 임시 저장(DRAFT)으로 승격된다. "
                    + "무검증(제목·본문·카테고리가 비어도 성공)이며, 각 필드는 null=기존 값 유지 / 빈 문자열=비우기의 부분 갱신이다. "
                    + "tags를 함께 보내면(null이 아니면) 전체 교체되고, 임시 저장 상태 글의 수정은 이 엔드포인트를 재호출하면 된다."
    )
    @SecurityRequirement(name = "BearerAuth")
    @PatchMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<PostSaveResponse>> saveDraft(@CurrentMember String publicId, @PathVariable Long postId, @Valid @RequestBody PostSaveRequest request) {

        PostSaveResponse response = service.saveDraft(publicId, postId, request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(response.postId() + "게시글이 임시 저장되었습니다.", response));
    }

    @Operation(
            summary = "게시글 게시",
            description = "작성중(WRITING)·임시 저장(DRAFT) 상태의 게시글을 게시(PUBLISHED)한다. "
                    + "요청 body의 내용을 반영(applyContent)한 뒤 상태를 전이하므로, 임시 저장을 한 번도 거치지 않아도 바로 게시할 수 있다. "
                    + "제목이 null·빈 문자열·공백이면 게시되지 않는다(POST_TITLE_REQUIRED). tags를 함께 보내면 반영된다."
    )
    @SecurityRequirement(name = "BearerAuth")
    @PatchMapping("/posts/{postId}/publish")
    public ResponseEntity<ApiResponse<PostPublishResponse>> publish(@CurrentMember String publicId, @PathVariable Long postId, @Valid @RequestBody PostPublishRequest request) {

        PostPublishResponse response = service.publish(publicId, postId, request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(response.postId() + "게시글이 게시되었습니다", response));
    }

    @Operation(
            summary = "게시글 수정",
            description = "이미 게시된(PUBLISHED) 글 전용 수정이다. PUBLISHED가 아니면 수정할 수 없고(POST_NOT_EDITABLE), "
                    + "제목을 빈 값으로 만들 수 없다(POST_TITLE_REQUIRED). 상태는 PUBLISHED로 유지된다(재심사 없이 즉시 반영). "
                    + "임시 저장 경로(saveDraft)와 분리돼 있으며, tags를 함께 보내면 반영된다."
    )
    @SecurityRequirement(name = "BearerAuth")
    @PatchMapping("/posts/{postId}/content")
    public ResponseEntity<ApiResponse<PostEditResponse>> editPost(@CurrentMember String publicId, @PathVariable Long postId, @Valid @RequestBody PostEditRequest request) {

        PostEditResponse response = service.editPost(publicId, postId, request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(response.postId() + "게시글이 수정되었습니다.", response));
    }

    @Operation(
            summary = "게시글 삭제",
            description = "본인 게시글을 소프트 삭제한다(deleted_at 기록, 작성 중이거나 임시 저장의 경우는 물리 삭제). "
                    + "삭제된 글은 이후 조회에서 제외된다."
    )
    @SecurityRequirement(name = "BearerAuth")
    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<PostDeleteResponse>> deletePost(@CurrentMember String publicId, @PathVariable Long postId) {

        PostDeleteResponse response = service.deletePost(publicId, postId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(response.postId() + "게시글이 삭제되었습니다.", response));
    }
}

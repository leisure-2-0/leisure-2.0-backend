package com.leisure.post.service;

import com.leisure.global.exception.BusinessException;
import com.leisure.global.exception.ErrorCode;
import com.leisure.member.service.MemberReader;
import com.leisure.post.domain.PostSort;
import com.leisure.post.dto.response.PostListResponse;
import com.leisure.post.dto.response.PostResponse;
import com.leisure.post.repository.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("게시글 피드 조회 (PostQueryService.getPosts, 커서 기반)")
class PostFeedQueryServiceTest {

    @Mock
    private MemberReader reader;

    @Mock
    private PostRepository repository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private PostQueryService postQueryService;

    private PostResponse post(long postId, int likeCount) {
        return new PostResponse(
                postId, "제목", null, 0, likeCount, 0, false, false, "강릉", null,
                new PostResponse.AuthorResponse(1L, "nick", null)
        );
    }

    @Test
    @DisplayName("limit+1개가 조회되면 hasNext=true, 초과분을 잘라내고 nextCursor를 만든다")
    void firstPage_hasNext() {
        // limit=2 → repo에 3개(limit+1) 요청, 3개 반환 → hasNext, 2개로 트림
        given(repository.findPosts(any(), any(), any(), any(), anyInt()))
                .willReturn(List.of(post(30, 50), post(29, 40), post(28, 30)));
        given(objectMapper.writeValueAsString(any())).willReturn("{\"postId\":29}");

        PostListResponse response = postQueryService.getPosts(null, null, PostSort.POPULAR, null, 2);

        assertThat(response.posts()).hasSize(2);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.nextCursor()).isNotNull();
    }

    @Test
    @DisplayName("limit 이하로 조회되면 hasNext=false, nextCursor=null")
    void lastPage_noNext() {
        given(repository.findPosts(any(), any(), any(), any(), anyInt()))
                .willReturn(List.of(post(30, 50), post(29, 40)));  // limit=2, 2개만

        PostListResponse response = postQueryService.getPosts(null, null, PostSort.POPULAR, null, 2);

        assertThat(response.posts()).hasSize(2);
        assertThat(response.hasNext()).isFalse();
        assertThat(response.nextCursor()).isNull();
    }

    @Test
    @DisplayName("커서 형식이 잘못되면 INVALID_CURSOR 예외를 던진다")
    void invalidCursor() {
        assertThatThrownBy(() ->
                postQueryService.getPosts(null, null, PostSort.POPULAR, "!!!not-base64!!!", 2))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_CURSOR);
    }

    @Test
    @DisplayName("limit이 범위(1~30)를 벗어나면 PAGE_SIZE_INVALID 예외를 던진다")
    void invalidLimit() {
        assertThatThrownBy(() ->
                postQueryService.getPosts(null, null, PostSort.POPULAR, null, 31))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PAGE_SIZE_INVALID);
    }
}

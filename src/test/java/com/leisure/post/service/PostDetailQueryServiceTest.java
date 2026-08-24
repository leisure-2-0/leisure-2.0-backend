package com.leisure.post.service;

import com.leisure.global.exception.BusinessException;
import com.leisure.global.exception.ErrorCode;
import com.leisure.member.domain.Member;
import com.leisure.member.service.MemberReader;
import com.leisure.post.domain.PostCategory;
import com.leisure.post.dto.result.PostDetailResult;
import com.leisure.post.repository.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("게시글 상세 조회 (PostQueryService.getPostDetail)")
class PostDetailQueryServiceTest {

    @Mock
    private MemberReader reader;

    @Mock
    private PostRepository repository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private PostQueryService postQueryService;

    private static final String PUBLIC_ID = "public-id";
    private static final Long MEMBER_ID = 1L;
    private static final Long POST_ID = 10L;

    private Member member() {
        Member member = Member.create("user@leisure.com", "ENCODED", "nick", null);
        ReflectionTestUtils.setField(member, "memberId", MEMBER_ID);
        return member;
    }

    private PostDetailResult result() {
        return new PostDetailResult(
                POST_ID, "제목", "본문", PostCategory.RESTAURANT,
                5, 3, 2, true, true, false, null,
                new PostDetailResult.AuthorResult(MEMBER_ID, "nick", null)
        );
    }

    @Test
    @DisplayName("로그인 상태면 memberId로 상세를 조회해 반환한다")
    void success_loggedIn() {
        given(reader.getMemberByPublicId(PUBLIC_ID)).willReturn(member());
        given(repository.findPostDetail(MEMBER_ID, POST_ID)).willReturn(Optional.of(result()));

        PostDetailResult response = postQueryService.getPostDetail(PUBLIC_ID, POST_ID);

        assertThat(response.postId()).isEqualTo(POST_ID);
        assertThat(response.isMine()).isTrue();
    }

    @Test
    @DisplayName("비로그인(publicId=null)이면 memberId=null로 조회하고 회원 조회를 하지 않는다")
    void success_anonymous() {
        given(repository.findPostDetail(null, POST_ID)).willReturn(Optional.of(result()));

        PostDetailResult response = postQueryService.getPostDetail(null, POST_ID);

        assertThat(response.postId()).isEqualTo(POST_ID);
        verify(reader, never()).getMemberByPublicId(any());
    }

    @Test
    @DisplayName("존재하지 않는(또는 비공개/삭제) 글이면 POST_NOT_FOUND 예외를 던진다")
    void notFound() {
        given(repository.findPostDetail(null, POST_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postQueryService.getPostDetail(null, POST_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.POST_NOT_FOUND);
    }
}

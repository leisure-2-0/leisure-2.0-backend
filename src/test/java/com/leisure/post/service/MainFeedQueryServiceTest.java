package com.leisure.post.service;

import com.leisure.member.domain.Member;
import com.leisure.member.service.MemberReader;
import com.leisure.post.domain.PostCategory;
import com.leisure.post.domain.PostSort;
import com.leisure.post.dto.response.MainFeedPostResponse;
import com.leisure.post.repository.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("메인 피드 조회 (PostQueryService.getMainFeedPosts)")
class MainFeedQueryServiceTest {

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

    private Member member() {
        Member member = Member.create("user@leisure.com", "ENCODED", "nick", null);
        ReflectionTestUtils.setField(member, "memberId", MEMBER_ID);
        return member;
    }

    private MainFeedPostResponse post(long postId) {
        return new MainFeedPostResponse(
                postId, "제목", PostCategory.RESTAURANT, 0, 0, 0, false, false, null,
                new MainFeedPostResponse.AuthorResponse(MEMBER_ID, "nick", null)
        );
    }

    @Test
    @DisplayName("로그인 상태면 memberId로 상위 18개를 조회한다")
    void loggedIn() {
        given(reader.getMemberByPublicId(PUBLIC_ID)).willReturn(member());
        given(repository.findMainFeedPosts(MEMBER_ID, PostCategory.HOTEL, PostSort.POPULAR, 18))
                .willReturn(List.of(post(1), post(2)));

        List<MainFeedPostResponse> response =
                postQueryService.getMainFeedPosts(PUBLIC_ID, PostCategory.HOTEL, PostSort.POPULAR);

        assertThat(response).hasSize(2);
        verify(repository).findMainFeedPosts(MEMBER_ID, PostCategory.HOTEL, PostSort.POPULAR, 18);
    }

    @Test
    @DisplayName("비로그인(publicId=null)이면 memberId=null로 조회하고 회원 조회를 하지 않는다")
    void anonymous() {
        given(repository.findMainFeedPosts(null, null, PostSort.LATEST, 18))
                .willReturn(List.of(post(1)));

        List<MainFeedPostResponse> response =
                postQueryService.getMainFeedPosts(null, null, PostSort.LATEST);

        assertThat(response).hasSize(1);
        verify(reader, never()).getMemberByPublicId(any());
        verify(repository).findMainFeedPosts(null, null, PostSort.LATEST, 18);
    }
}

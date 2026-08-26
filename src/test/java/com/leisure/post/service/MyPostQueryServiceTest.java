package com.leisure.post.service;

import com.leisure.global.exception.BusinessException;
import com.leisure.global.exception.ErrorCode;
import com.leisure.member.domain.Member;
import com.leisure.member.service.MemberReader;
import com.leisure.post.assembler.PostResponseAssembler;
import com.leisure.post.domain.MyPostSort;
import com.leisure.post.dto.response.MyPostListResponse;
import com.leisure.post.repository.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("내 게시글 목록 조회 (PostQueryService.getMyPosts)")
class MyPostQueryServiceTest {

    @Mock
    private MemberReader reader;

    @Mock
    private PostRepository repository;

    @Mock
    private PostResponseAssembler assembler;

    @InjectMocks
    private PostQueryService postQueryService;

    private static final String PUBLIC_ID = "public-id";
    private static final Long MEMBER_ID = 1L;

    private Member member() {
        Member member = Member.create("user@leisure.com", "ENCODED", "nick", null);
        ReflectionTestUtils.setField(member, "memberId", MEMBER_ID);
        return member;
    }

    @Test
    @DisplayName("page/size로 offset을 계산해 조회하고 totalPages·hasNext를 산출한다")
    void getMyPosts_success() {
        // 총 25개, size=10, page=1 → offset=10, totalPages=3, hasNext=true
        given(reader.getMemberByPublicId(PUBLIC_ID)).willReturn(member());
        given(repository.findMyPosts(MEMBER_ID, MyPostSort.LATEST, 10L, 10)).willReturn(List.of());
        given(assembler.assembleMyPosts(any())).willReturn(List.of());
        given(repository.countMyPosts(MEMBER_ID)).willReturn(25L);

        MyPostListResponse response = postQueryService.getMyPosts(PUBLIC_ID, MyPostSort.LATEST, 1, 10);

        assertThat(response.page()).isEqualTo(1);
        assertThat(response.totalElements()).isEqualTo(25L);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.hasNext()).isTrue();
    }

    @Test
    @DisplayName("page/size가 null이면 기본값(page=0, size=10)을 적용한다")
    void getMyPosts_defaults() {
        given(reader.getMemberByPublicId(PUBLIC_ID)).willReturn(member());
        given(repository.findMyPosts(MEMBER_ID, MyPostSort.LATEST, 0L, 15)).willReturn(List.of());
        given(assembler.assembleMyPosts(any())).willReturn(List.of());
        given(repository.countMyPosts(MEMBER_ID)).willReturn(0L);

        MyPostListResponse response = postQueryService.getMyPosts(PUBLIC_ID, MyPostSort.LATEST, null, null);

        assertThat(response.page()).isEqualTo(0);
        assertThat(response.size()).isEqualTo(15);
        verify(repository).findMyPosts(MEMBER_ID, MyPostSort.LATEST, 0L, 15);
    }

    @Test
    @DisplayName("page가 음수면 PAGE_INVALID 예외를 던진다")
    void getMyPosts_negativePage() {
        given(reader.getMemberByPublicId(PUBLIC_ID)).willReturn(member());

        assertThatThrownBy(() -> postQueryService.getMyPosts(PUBLIC_ID, MyPostSort.LATEST, -1, 10))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PAGE_INVALID);

        verify(repository, never()).findMyPosts(anyLong(), eq(MyPostSort.LATEST), anyLong(), anyInt());
    }

    @Test
    @DisplayName("size가 범위(1~30)를 벗어나면 PAGE_SIZE_INVALID 예외를 던진다")
    void getMyPosts_invalidSize() {
        given(reader.getMemberByPublicId(PUBLIC_ID)).willReturn(member());

        assertThatThrownBy(() -> postQueryService.getMyPosts(PUBLIC_ID, MyPostSort.LATEST, 0, 100))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PAGE_SIZE_INVALID);

        verify(repository, never()).findMyPosts(anyLong(), eq(MyPostSort.LATEST), anyLong(), anyInt());
    }
}

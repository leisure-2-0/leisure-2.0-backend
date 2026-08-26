package com.leisure.postLike.service;

import com.leisure.global.exception.BusinessException;
import com.leisure.global.exception.ErrorCode;
import com.leisure.member.domain.Member;
import com.leisure.member.service.MemberReader;
import com.leisure.postLike.assembler.LikedPostResponseAssembler;
import com.leisure.postLike.domain.LikedPostSort;
import com.leisure.postLike.dto.response.LikedPostListResponse;
import com.leisure.postLike.repository.PostLikeRepository;
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
class PostLikeQueryServiceTest {

    @Mock
    private MemberReader reader;

    @Mock
    private PostLikeRepository repository;

    @Mock
    private LikedPostResponseAssembler assembler;

    @InjectMocks
    private PostLikeQueryService postLikeQueryService;

    private static final String PUBLIC_ID = "public-id";
    private static final Long MEMBER_ID = 1L;

    private Member member() {
        Member member = Member.create("user@leisure.com", "ENCODED", "nick", null);
        ReflectionTestUtils.setField(member, "memberId", MEMBER_ID);
        return member;
    }

    @Test
    @DisplayName("page/size로 offset을 계산해 조회하고 totalPages·hasNext를 산출한다")
    void getLikedPosts_success() {
        // given: 총 25개, size=10, page=1 → offset=10, totalPages=3, hasNext=true(1+1<3)
        given(reader.getMemberByPublicId(PUBLIC_ID)).willReturn(member());
        given(repository.findLikedPosts(MEMBER_ID, LikedPostSort.LATEST, 10L, 10)).willReturn(List.of());
        given(assembler.assembleLikedPosts(any())).willReturn(List.of());
        given(repository.countLikedPosts(MEMBER_ID)).willReturn(25L);

        // when
        LikedPostListResponse response = postLikeQueryService.getLikedPosts(PUBLIC_ID, LikedPostSort.LATEST, 1, 10);

        // then
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.totalElements()).isEqualTo(25L);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.hasNext()).isTrue();
    }

    @Test
    @DisplayName("마지막 페이지면 hasNext=false")
    void getLikedPosts_lastPage() {
        given(reader.getMemberByPublicId(PUBLIC_ID)).willReturn(member());
        given(repository.findLikedPosts(MEMBER_ID, LikedPostSort.LATEST, 20L, 10)).willReturn(List.of());
        given(assembler.assembleLikedPosts(any())).willReturn(List.of());
        given(repository.countLikedPosts(MEMBER_ID)).willReturn(25L);

        // page=2 → totalPages=3, 2+1<3 == false
        LikedPostListResponse response = postLikeQueryService.getLikedPosts(PUBLIC_ID, LikedPostSort.LATEST, 2, 10);

        assertThat(response.hasNext()).isFalse();
    }

    @Test
    @DisplayName("page/size가 null이면 기본값(page=0, size=10)을 적용한다")
    void getLikedPosts_defaults() {
        given(reader.getMemberByPublicId(PUBLIC_ID)).willReturn(member());
        given(repository.findLikedPosts(MEMBER_ID, LikedPostSort.LATEST, 0L, 10)).willReturn(List.of());
        given(assembler.assembleLikedPosts(any())).willReturn(List.of());
        given(repository.countLikedPosts(MEMBER_ID)).willReturn(0L);

        LikedPostListResponse response = postLikeQueryService.getLikedPosts(PUBLIC_ID, LikedPostSort.LATEST, null, null);

        assertThat(response.page()).isEqualTo(0);
        assertThat(response.size()).isEqualTo(10);
        verify(repository).findLikedPosts(MEMBER_ID, LikedPostSort.LATEST, 0L, 10);
    }

    @Test
    @DisplayName("page가 음수면 PAGE_INVALID 예외를 던진다")
    void getLikedPosts_negativePage() {
        given(reader.getMemberByPublicId(PUBLIC_ID)).willReturn(member());

        assertThatThrownBy(() -> postLikeQueryService.getLikedPosts(PUBLIC_ID, LikedPostSort.LATEST, -1, 10))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PAGE_INVALID);

        verify(repository, never()).findLikedPosts(anyLong(), eq(LikedPostSort.LATEST), anyLong(), anyInt());
    }

    @Test
    @DisplayName("size가 범위(1~30)를 벗어나면 PAGE_SIZE_INVALID 예외를 던진다")
    void getLikedPosts_invalidSize() {
        given(reader.getMemberByPublicId(PUBLIC_ID)).willReturn(member());

        assertThatThrownBy(() -> postLikeQueryService.getLikedPosts(PUBLIC_ID, LikedPostSort.LATEST, 0, 31))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PAGE_SIZE_INVALID);

        verify(repository, never()).findLikedPosts(anyLong(), eq(LikedPostSort.LATEST), anyLong(), anyInt());
    }
}

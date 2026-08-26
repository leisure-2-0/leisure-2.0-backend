package com.leisure.Bookmark.service;

import com.leisure.Bookmark.assembler.BookmarkedPostResponseAssembler;
import com.leisure.Bookmark.domain.BookmarkedPostSort;
import com.leisure.Bookmark.dto.response.BookmarkedPostListResponse;
import com.leisure.Bookmark.repository.BookmarkRepository;
import com.leisure.global.exception.BusinessException;
import com.leisure.global.exception.ErrorCode;
import com.leisure.member.domain.Member;
import com.leisure.member.service.MemberReader;
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
class BookmarkQueryServiceTest {

    @Mock
    private MemberReader reader;

    @Mock
    private BookmarkRepository repository;

    @Mock
    private BookmarkedPostResponseAssembler assembler;

    @InjectMocks
    private BookmarkQueryService bookmarkQueryService;

    private static final String PUBLIC_ID = "public-id";
    private static final Long MEMBER_ID = 1L;

    private Member member() {
        Member member = Member.create("user@leisure.com", "ENCODED", "nick", null);
        ReflectionTestUtils.setField(member, "memberId", MEMBER_ID);
        return member;
    }

    @Test
    @DisplayName("page/size로 offset을 계산해 조회하고 totalPages·hasNext를 산출한다")
    void getBookmarkedPosts_success() {
        // 총 25개, size=10, page=1 → offset=10, totalPages=3, hasNext=true
        given(reader.getMemberByPublicId(PUBLIC_ID)).willReturn(member());
        given(repository.findBookmarkedPosts(MEMBER_ID, BookmarkedPostSort.LATEST, 10L, 10)).willReturn(List.of());
        given(assembler.assembleBookmarkedPosts(any())).willReturn(List.of());
        given(repository.countBookmarkedPosts(MEMBER_ID)).willReturn(25L);

        BookmarkedPostListResponse response =
                bookmarkQueryService.getBookmarkedPosts(PUBLIC_ID, BookmarkedPostSort.LATEST, 1, 10);

        assertThat(response.page()).isEqualTo(1);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.hasNext()).isTrue();
    }

    @Test
    @DisplayName("page/size가 null이면 기본값(page=0, size=10)을 적용한다")
    void getBookmarkedPosts_defaults() {
        given(reader.getMemberByPublicId(PUBLIC_ID)).willReturn(member());
        given(repository.findBookmarkedPosts(MEMBER_ID, BookmarkedPostSort.LATEST, 0L, 10)).willReturn(List.of());
        given(assembler.assembleBookmarkedPosts(any())).willReturn(List.of());
        given(repository.countBookmarkedPosts(MEMBER_ID)).willReturn(0L);

        BookmarkedPostListResponse response =
                bookmarkQueryService.getBookmarkedPosts(PUBLIC_ID, BookmarkedPostSort.LATEST, null, null);

        assertThat(response.page()).isEqualTo(0);
        assertThat(response.size()).isEqualTo(10);
    }

    @Test
    @DisplayName("page가 음수면 PAGE_INVALID 예외를 던진다")
    void getBookmarkedPosts_negativePage() {
        given(reader.getMemberByPublicId(PUBLIC_ID)).willReturn(member());

        assertThatThrownBy(() -> bookmarkQueryService.getBookmarkedPosts(PUBLIC_ID, BookmarkedPostSort.LATEST, -1, 10))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PAGE_INVALID);

        verify(repository, never()).findBookmarkedPosts(anyLong(), eq(BookmarkedPostSort.LATEST), anyLong(), anyInt());
    }

    @Test
    @DisplayName("size가 범위(1~30)를 벗어나면 PAGE_SIZE_INVALID 예외를 던진다")
    void getBookmarkedPosts_invalidSize() {
        given(reader.getMemberByPublicId(PUBLIC_ID)).willReturn(member());

        assertThatThrownBy(() -> bookmarkQueryService.getBookmarkedPosts(PUBLIC_ID, BookmarkedPostSort.LATEST, 0, 0))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PAGE_SIZE_INVALID);

        verify(repository, never()).findBookmarkedPosts(anyLong(), eq(BookmarkedPostSort.LATEST), anyLong(), anyInt());
    }
}

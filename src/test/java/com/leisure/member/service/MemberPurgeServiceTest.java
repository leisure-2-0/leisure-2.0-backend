package com.leisure.member.service;

import com.leisure.bookmark.repository.BookmarkRepository;
import com.leisure.member.repository.MemberRepository;
import com.leisure.pointhistory.repository.PointHistoryRepository;
import com.leisure.post.repository.PostRepository;
import com.leisure.postlike.repository.PostLikeRepository;
import com.leisure.tag.repository.TagRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class MemberPurgeServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private PostLikeRepository postLikeRepository;

    @Mock
    private BookmarkRepository bookmarkRepository;

    @Mock
    private PointHistoryRepository pointHistoryRepository;

    @InjectMocks
    private MemberPurgeService memberPurgeService;

    @Test
    @DisplayName("퍼지 대상 회원이 없으면 아무것도 삭제하지 않고 0을 반환한다")
    void noTarget_deletesNothing() {
        given(memberRepository.findMemberIdsByDeletedAtBefore(any(LocalDateTime.class))).willReturn(List.of());

        int purged = memberPurgeService.purgeWithdrawnMembers();

        assertThat(purged).isZero();
        verifyNoMoreInteractions(memberRepository);
        verifyNoInteractions(postRepository, tagRepository, postLikeRepository, bookmarkRepository, pointHistoryRepository);
    }

    @Test
    @DisplayName("대상 회원과 글이 있으면 자식→글→회원 순서로 삭제하고 회원 수를 반환한다")
    void purge_deletesInOrder() {
        List<Long> memberIds = List.of(1L, 2L);
        List<Long> postIds = List.of(10L, 11L);
        given(memberRepository.findMemberIdsByDeletedAtBefore(any(LocalDateTime.class))).willReturn(memberIds);
        given(postRepository.findPostIdsByMemberIdIn(memberIds)).willReturn(postIds);

        int purged = memberPurgeService.purgeWithdrawnMembers();

        assertThat(purged).isEqualTo(2);

        InOrder inOrder = inOrder(tagRepository, postLikeRepository, bookmarkRepository,
                pointHistoryRepository, postRepository, memberRepository);
        // 글의 자식(태그·좋아요·북마크) 먼저
        inOrder.verify(tagRepository).deleteByPostIdIn(postIds);
        inOrder.verify(postLikeRepository).deleteByPostIdIn(postIds);
        inOrder.verify(bookmarkRepository).deleteByPostIdIn(postIds);
        // 회원이 남에게 누른 것 + 수령 포인트
        inOrder.verify(postLikeRepository).deleteByMemberIdIn(memberIds);
        inOrder.verify(bookmarkRepository).deleteByMemberIdIn(memberIds);
        inOrder.verify(pointHistoryRepository).deleteByMemberIdIn(memberIds);
        // 글 → 회원 (맨 마지막)
        inOrder.verify(postRepository).deleteByMemberIdIn(memberIds);
        inOrder.verify(memberRepository).deleteByMemberIdIn(memberIds);
    }

    @Test
    @DisplayName("대상 회원은 있으나 글이 없으면 글 자식 삭제는 건너뛰고 나머지는 삭제한다")
    void noPosts_skipsPostChildrenDeletes() {
        List<Long> memberIds = List.of(1L);
        given(memberRepository.findMemberIdsByDeletedAtBefore(any(LocalDateTime.class))).willReturn(memberIds);
        given(postRepository.findPostIdsByMemberIdIn(memberIds)).willReturn(List.of());

        memberPurgeService.purgeWithdrawnMembers();

        verify(tagRepository, never()).deleteByPostIdIn(any());
        verify(postLikeRepository, never()).deleteByPostIdIn(any());
        verify(bookmarkRepository, never()).deleteByPostIdIn(any());

        verify(postLikeRepository).deleteByMemberIdIn(memberIds);
        verify(bookmarkRepository).deleteByMemberIdIn(memberIds);
        verify(pointHistoryRepository).deleteByMemberIdIn(memberIds);
        verify(postRepository).deleteByMemberIdIn(memberIds);
        verify(memberRepository).deleteByMemberIdIn(memberIds);
    }
}

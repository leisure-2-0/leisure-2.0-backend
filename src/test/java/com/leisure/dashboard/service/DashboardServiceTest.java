package com.leisure.dashboard.service;

import com.leisure.dashboard.dto.response.DashboardStatsResponse;
import com.leisure.festival.repository.FestivalRepository;
import com.leisure.member.repository.MemberRepository;
import com.leisure.post.domain.PostStatus;
import com.leisure.post.repository.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("대시보드 통계 조회 (DashboardService.getDashboardStats)")
class DashboardServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private FestivalRepository festivalRepository;

    @InjectMocks
    private DashboardService service;

    @Test
    @DisplayName("각 도메인 count를 올바른 필드에 매핑해 조립한다")
    void assemblesStats() {
        given(postRepository.countCertifiedRegions()).willReturn(5L);
        given(postRepository.countByStatusAndDeletedAtIsNull(PostStatus.PUBLISHED)).willReturn(10L);
        given(memberRepository.countJoinedBetween(any(), any())).willReturn(3L);
        given(festivalRepository.countFestivalsInProgress(any(), any())).willReturn(7L);

        DashboardStatsResponse response = service.getDashboardStats();

        assertThat(response.certifiedRegionCount()).isEqualTo(5L);
        assertThat(response.certifiedPostCount()).isEqualTo(10L);
        assertThat(response.monthlyNewMemberCount()).isEqualTo(3L);
        assertThat(response.inProgressFestivalCount()).isEqualTo(7L);
    }

    @Test
    @DisplayName("이번 달 범위를 회원(시각)과 축제(날짜) 형태로 각각 도출해 넘긴다")
    void passesCurrentMonthRange() {
        YearMonth ym = YearMonth.now();

        service.getDashboardStats();

        ArgumentCaptor<LocalDateTime> memberStart = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> memberEnd = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(memberRepository).countJoinedBetween(memberStart.capture(), memberEnd.capture());
        assertThat(memberStart.getValue()).isEqualTo(ym.atDay(1).atStartOfDay());
        assertThat(memberEnd.getValue()).isEqualTo(ym.plusMonths(1).atDay(1).atStartOfDay());

        ArgumentCaptor<LocalDate> festStart = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> festEnd = ArgumentCaptor.forClass(LocalDate.class);
        verify(festivalRepository).countFestivalsInProgress(festStart.capture(), festEnd.capture());
        assertThat(festStart.getValue()).isEqualTo(ym.atDay(1));
        assertThat(festEnd.getValue()).isEqualTo(ym.atEndOfMonth());
    }
}

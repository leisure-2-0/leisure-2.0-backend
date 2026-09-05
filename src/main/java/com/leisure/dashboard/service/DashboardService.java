package com.leisure.dashboard.service;

import com.leisure.dashboard.dto.response.DashboardStatsResponse;
import com.leisure.festival.repository.FestivalRepository;
import com.leisure.member.repository.MemberRepository;
import com.leisure.post.domain.PostStatus;
import com.leisure.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final MemberRepository memberRepository;

    private final PostRepository postRepository;

    private final FestivalRepository festivalRepository;

    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {

        YearMonth now = YearMonth.now();

        LocalDate monthStart = now.atDay(1);
        LocalDate monthEnd = now.atEndOfMonth();

        LocalDateTime start = monthStart.atStartOfDay();
        LocalDateTime end = now.plusMonths(1).atDay(1).atStartOfDay();


        long certifiedRegionCount = postRepository.countCertifiedRegions();

        long certifiedPostCount = postRepository.countByStatusAndDeletedAtIsNull(PostStatus.PUBLISHED);

        long monthlyNewMemberCount = memberRepository.countJoinedBetween(start, end);

        long inProgressFestivalCount = festivalRepository.countFestivalsInProgress(monthStart, monthEnd);

        return new DashboardStatsResponse(
                certifiedRegionCount,
                certifiedPostCount,
                monthlyNewMemberCount,
                inProgressFestivalCount);
    }
}

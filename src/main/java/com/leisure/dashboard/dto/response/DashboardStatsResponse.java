package com.leisure.dashboard.dto.response;

public record DashboardStatsResponse(
        long certifiedRegionCount,    // 인증된 소도시

        long certifiedPostCount,    // 누적 인증 게시글

        long monthlyPostCount, // 이번 달 게시된 글

        long inProgressFestivalCount   // 이번 달 진행 축제
) {}

package com.leisure.dashboard.dto.response;

public record DashboardStatsResponse(
        long certifiedRegionCount,    // 인증된 소도시

        long certifiedPostCount,    // 누적 인증 게시글

        long monthlyNewMemberCount, // 이달 가입 회원

        long inProgressFestivalCount   // 이번 달 진행 축제
) {}

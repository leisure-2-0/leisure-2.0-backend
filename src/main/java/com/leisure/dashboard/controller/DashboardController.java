package com.leisure.dashboard.controller;

import com.leisure.dashboard.dto.response.DashboardStatsResponse;
import com.leisure.dashboard.service.DashboardService;
import com.leisure.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "대시보드(Dashboard)",
        description = "메인 화면 집계 통계 조회"
)
@RestController
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService service;

    @Operation(
            summary = "대시보드 통계 조회",
            description = """
                    메인 화면 집계 통계를 한 번에 반환한다. 비로그인 공개.
                    누적 인증 게시글 수, 이달 가입 회원 수, 이번 달 진행 축제 수, 인증된 소도시 수를 포함한다.
                    '이달/이번 달'은 서버 기준(KST) 현재 월이다.
                    """
    )
    @GetMapping("/dashboards")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getDashboardStats() {

        DashboardStatsResponse response = service.getDashboardStats();

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("대시보드 통계 조회에 성공했습니다.", response));
    }

}

package com.leisure.festival.controller;

import com.leisure.festival.domain.FestivalCategory;
import com.leisure.festival.dto.response.DailyFestivalResponse;
import com.leisure.festival.dto.response.MonthlyFestivalResponse;
import com.leisure.festival.dto.response.UpcomingFestivalResponse;
import com.leisure.festival.service.FestivalQueryService;
import com.leisure.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(
        name = "축제 조회(Festival Query)",
        description = "월별, 일별 축제 캘린더 및 다가오는 축제 조회"
)
@Validated
@RestController
@RequiredArgsConstructor
public class FestivalQueryController {

    private final FestivalQueryService service;

    @Operation(
            summary = "월별 축제 목록 조회",
            description = """
                    지정한 연월과 기간(시작일~종료일)이 겹치는 축제를 이름 가나다순으로 반환한다. 비로그인 공개.
                    한 달을 넘겨 걸치는 축제는 걸치는 모든 달에 노출된다(예: 8/20~9/5 축제는 8월, 9월 조회에 모두 포함).
                    category 미지정 시 전체, 지정 시 해당 분류(FESTIVAL/PERFORMANCE/EVENT)만 조회한다.
                    """
    )
    @GetMapping("/festivals/months")
    public ResponseEntity<ApiResponse<List<MonthlyFestivalResponse>>> getMonthlyFestivals(
            @RequestParam @Min(1900) @Max(2100) int year,
            @RequestParam @Min(1) @Max(12) int month,
            @RequestParam(required = false) FestivalCategory category) {

        List<MonthlyFestivalResponse> responses = service.getMonthlyFestivals(year, month, category);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("월별 축제 목록 조회에 성공했습니다.", responses));

    }


    @Operation(
            summary = "일별 축제 목록 조회",
            description = """
                    지정한 날짜에 진행 중인(축제 기간에 포함되는) 축제를 지역명과 함께 이름 가나다순으로 반환한다. 비로그인 공개.
                    category 미지정 시 전체, 지정 시 해당 분류(FESTIVAL/PERFORMANCE/EVENT)만 조회한다.
                    """
    )
    @GetMapping("/festivals/days")
    public ResponseEntity<ApiResponse<List<DailyFestivalResponse>>> getDailyFestivals(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) FestivalCategory category) {

        List<DailyFestivalResponse> responses = service.getDailyFestivals(date, category);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("일별 축제 목록 조회에 성공했습니다.", responses));

    }

    @Operation(
            summary = "다가오는 축제 Top10 조회",
            description = """
                    내일부터(당일 제외, 서버 기준) 시작하는 축제를 시작일 임박순으로 최대 10개 반환한다. 지역명 포함, 비로그인 공개.
                    오늘 이전에 시작했거나 오늘 시작하는 축제는 제외된다. 파라미터 없는 고정 콘텐츠(홈 노출용).
                    """
    )
    @GetMapping("/festivals/upcoming")
    public ResponseEntity<ApiResponse<List<UpcomingFestivalResponse>>> getUpcomingFestivals() {

        List<UpcomingFestivalResponse> responses = service.getUpcomingFestivals();

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("다가오는 축제 목록 조회에 성공했습니다.", responses));
    }
}

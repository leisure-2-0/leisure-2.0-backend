package com.leisure.festival.service;

import com.leisure.festival.dto.response.UpcomingFestivalResponse;
import com.leisure.festival.dto.result.UpcomingFestivalResult;
import com.leisure.festival.repository.FestivalRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("다가오는 축제 조회 (FestivalQueryService.getUpcomingFestivals)")
class UpcomingFestivalQueryServiceTest {

    @Mock
    private FestivalRepository repository;

    @InjectMocks
    private FestivalQueryService service;

    private UpcomingFestivalResult result(String signguName) {
        return new UpcomingFestivalResult(1L, "축제", signguName, LocalDate.of(2026, 9, 10), "http://img");
    }

    @Test
    @DisplayName("시군구 접미사 제거 규칙: 3글자 이상만 접미사를 떼고, 2글자 구/복합/세종은 특수 처리한다")
    void regionStripRules() {
        given(repository.findUpcomingFestivals(any())).willReturn(List.of(
                result("강릉시"),          // 3글자 → 강릉
                result("남양주시"),        // 4글자 → 남양주
                result("종로구"),          // 3글자 → 종로
                result("남구"),            // 2글자 → 유지
                result("중구"),            // 2글자 → 유지
                result("포항시 남구"),     // 복합 → 그대로
                result("세종특별자치시"),  // 특수 → 세종
                result(null)               // null → null
        ));

        List<String> regions = service.getUpcomingFestivals().stream()
                .map(UpcomingFestivalResponse::region)
                .toList();

        assertThat(regions).containsExactly(
                "강릉", "남양주", "종로", "남구", "중구", "포항시 남구", "세종", null);
    }

    @Test
    @DisplayName("festivalId/name/thumbnailUrl/eventStartDate는 그대로 전달된다")
    void passthrough() {
        given(repository.findUpcomingFestivals(any())).willReturn(List.of(
                new UpcomingFestivalResult(42L, "강릉단오제", "강릉시", LocalDate.of(2026, 9, 10), "http://thumb")));

        UpcomingFestivalResponse r = service.getUpcomingFestivals().get(0);

        assertThat(r.festivalId()).isEqualTo(42L);
        assertThat(r.name()).isEqualTo("강릉단오제");
        assertThat(r.thumbnailUrl()).isEqualTo("http://thumb");
        assertThat(r.eventStartDate()).isEqualTo(LocalDate.of(2026, 9, 10));
        assertThat(r.region()).isEqualTo("강릉");
    }
}

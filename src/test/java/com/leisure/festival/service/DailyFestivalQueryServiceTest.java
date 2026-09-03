package com.leisure.festival.service;

import com.leisure.festival.domain.FestivalCategory;
import com.leisure.festival.dto.response.DailyFestivalResponse;
import com.leisure.festival.dto.result.DailyFestivalResult;
import com.leisure.festival.repository.FestivalRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("일별 축제 조회 (FestivalQueryService.getDailyFestivals)")
class DailyFestivalQueryServiceTest {

    @Mock
    private FestivalRepository repository;

    @InjectMocks
    private FestivalQueryService service;

    @Captor
    private ArgumentCaptor<String> codeCaptor;

    private static final LocalDate DATE = LocalDate.of(2026, 8, 15);

    @Test
    @DisplayName("코드는 enum으로, 시군구는 접미사를 뗀 지역명으로 변환해 응답한다")
    void mapping() {
        given(repository.findDailyFestivals(any(), any())).willReturn(List.of(
                new DailyFestivalResult("EV01", "강릉시", "축제A", "개요", "10:00~18:00", "http://a")));

        List<DailyFestivalResponse> responses = service.getDailyFestivals(DATE, null);

        DailyFestivalResponse r = responses.get(0);
        assertThat(r.category()).isEqualTo(FestivalCategory.FESTIVAL);
        assertThat(r.signguName()).isEqualTo("강릉");
        assertThat(r.name()).isEqualTo("축제A");
        assertThat(r.overview()).isEqualTo("개요");
        assertThat(r.eventTime()).isEqualTo("10:00~18:00");
        assertThat(r.homepageUrl()).isEqualTo("http://a");
    }

    @Test
    @DisplayName("시군구가 null이면 지역명도 null이다(LEFT JOIN 미매칭 방어)")
    void nullRegion() {
        given(repository.findDailyFestivals(any(), any())).willReturn(List.of(
                new DailyFestivalResult("EV03", null, "행사B", null, null, null)));

        DailyFestivalResponse r = service.getDailyFestivals(DATE, null).get(0);

        assertThat(r.signguName()).isNull();
        assertThat(r.category()).isEqualTo(FestivalCategory.EVENT);
    }

    @Test
    @DisplayName("category가 있으면 코드로 변환해 넘기고, 없으면 null을 넘긴다")
    void categoryToCode() {
        given(repository.findDailyFestivals(any(), any())).willReturn(List.of());

        service.getDailyFestivals(DATE, FestivalCategory.PERFORMANCE);
        service.getDailyFestivals(DATE, null);

        verify(repository, times(2)).findDailyFestivals(any(), codeCaptor.capture());
        assertThat(codeCaptor.getAllValues()).containsExactly("EV02", null);
    }
}

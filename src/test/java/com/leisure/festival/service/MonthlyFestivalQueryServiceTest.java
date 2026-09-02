package com.leisure.festival.service;

import com.leisure.festival.domain.FestivalCategory;
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
@DisplayName("월별 축제 조회 (FestivalQueryService.getMonthlyFestivals)")
class MonthlyFestivalQueryServiceTest {

    @Mock
    private FestivalRepository repository;

    @InjectMocks
    private FestivalQueryService service;

    @Captor
    private ArgumentCaptor<LocalDate> monthStartCaptor;

    @Captor
    private ArgumentCaptor<LocalDate> monthEndCaptor;

    @Captor
    private ArgumentCaptor<String> codeCaptor;

    @Test
    @DisplayName("평년 2월은 월초 1일, 월말 28일로 계산해 리포지토리에 넘긴다")
    void monthRange_nonLeap() {
        given(repository.findMonthlyFestivals(any(), any(), any())).willReturn(List.of());

        service.getMonthlyFestivals(2026, 2, null);

        verify(repository).findMonthlyFestivals(monthStartCaptor.capture(), monthEndCaptor.capture(), any());
        assertThat(monthStartCaptor.getValue()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(monthEndCaptor.getValue()).isEqualTo(LocalDate.of(2026, 2, 28));
    }

    @Test
    @DisplayName("윤년 2월은 월말을 29일로 계산한다")
    void monthRange_leap() {
        given(repository.findMonthlyFestivals(any(), any(), any())).willReturn(List.of());

        service.getMonthlyFestivals(2024, 2, null);

        verify(repository).findMonthlyFestivals(monthStartCaptor.capture(), monthEndCaptor.capture(), any());
        assertThat(monthEndCaptor.getValue()).isEqualTo(LocalDate.of(2024, 2, 29));
    }

    @Test
    @DisplayName("category가 있으면 코드로 변환해 넘기고, 없으면 null을 넘긴다")
    void categoryToCode() {
        given(repository.findMonthlyFestivals(any(), any(), any())).willReturn(List.of());

        service.getMonthlyFestivals(2026, 8, FestivalCategory.FESTIVAL);
        service.getMonthlyFestivals(2026, 8, null);

        verify(repository, times(2)).findMonthlyFestivals(any(), any(), codeCaptor.capture());
        assertThat(codeCaptor.getAllValues()).containsExactly("EV01", null);
    }
}

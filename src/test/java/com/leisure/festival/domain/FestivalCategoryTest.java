package com.leisure.festival.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("축제 분류 코드 매핑 (FestivalCategory)")
class FestivalCategoryTest {

    @Test
    @DisplayName("getCode는 enum에 대응하는 TourAPI lclsSystm2 코드를 반환한다")
    void getCode() {
        assertThat(FestivalCategory.FESTIVAL.getCode()).isEqualTo("EV01");
        assertThat(FestivalCategory.PERFORMANCE.getCode()).isEqualTo("EV02");
        assertThat(FestivalCategory.EVENT.getCode()).isEqualTo("EV03");
    }

    @Test
    @DisplayName("fromCode는 코드에 대응하는 enum을 반환한다")
    void fromCode() {
        assertThat(FestivalCategory.fromCode("EV01")).isEqualTo(FestivalCategory.FESTIVAL);
        assertThat(FestivalCategory.fromCode("EV02")).isEqualTo(FestivalCategory.PERFORMANCE);
        assertThat(FestivalCategory.fromCode("EV03")).isEqualTo(FestivalCategory.EVENT);
    }

    @Test
    @DisplayName("fromCode는 null이나 알 수 없는 코드에 대해 null을 반환한다(관대)")
    void fromCode_nullOrUnknown() {
        assertThat(FestivalCategory.fromCode(null)).isNull();
        assertThat(FestivalCategory.fromCode("")).isNull();
        assertThat(FestivalCategory.fromCode("EV99")).isNull();
    }
}

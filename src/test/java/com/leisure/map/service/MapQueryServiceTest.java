package com.leisure.map.service;

import com.leisure.global.exception.BusinessException;
import com.leisure.global.exception.ErrorCode;
import com.leisure.map.repository.MapRepositoryCustom;
import com.leisure.post.domain.PostCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("지도 조회 (MapQueryService)")
class MapQueryServiceTest {

    @Mock
    private MapRepositoryCustom repository;

    @InjectMocks
    private MapQueryService service;

    @Test
    @DisplayName("유효한 bounds면 리포지토리로 조회를 위임한다")
    void validBounds() {
        given(repository.findPinsInBounds(anyDouble(), anyDouble(), anyDouble(), anyDouble(), any()))
                .willReturn(List.of());

        service.getPostPins(37.0, 37.5, 127.0, 127.5, PostCategory.RESTAURANT);

        verify(repository).findPinsInBounds(37.0, 37.5, 127.0, 127.5, PostCategory.RESTAURANT);
    }

    @Test
    @DisplayName("NaN/Infinity 좌표는 거부한다")
    void nonFinite() {
        assertInvalid(() -> service.getPostPins(Double.NaN, 37.5, 127.0, 127.5, null));
        assertInvalid(() -> service.getPostPins(37.0, Double.POSITIVE_INFINITY, 127.0, 127.5, null));
    }

    @Test
    @DisplayName("좌표계 범위를 벗어난 위경도는 거부한다")
    void outOfRange() {
        assertInvalid(() -> service.getPostPins(-91.0, 37.5, 127.0, 127.5, null));  // 위도 < -90
        assertInvalid(() -> service.getPostPins(37.0, 37.5, 127.0, 181.0, null));   // 경도 > 180
    }

    @Test
    @DisplayName("뒤집힌 bounds(min > max)는 거부한다")
    void inverted() {
        assertInvalid(() -> service.getPostPins(37.5, 37.0, 127.0, 127.5, null));   // minLat > maxLat
        assertInvalid(() -> service.getPostPins(37.0, 37.5, 127.5, 127.0, null));   // minLng > maxLng
    }

    @Test
    @DisplayName("허용 범위(0.7도)를 넘는 넓은 화면은 거부한다")
    void spanTooLarge() {
        assertInvalid(() -> service.getPostPins(37.0, 37.9, 127.0, 127.5, null));   // 위도 span 0.9
    }

    @Test
    @DisplayName("지역별 핀 개수는 검증 없이 리포지토리 결과를 그대로 반환한다")
    void regionPinCounts_passthrough() {
        given(repository.findRegionPinCounts(any())).willReturn(List.of());

        assertThat(service.getRegionPinCounts(PostCategory.HOTEL)).isEmpty();
        verify(repository).findRegionPinCounts(PostCategory.HOTEL);
    }

    private void assertInvalid(Runnable call) {
        assertThatThrownBy(call::run)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_REQUEST_PARAMETER);
    }
}

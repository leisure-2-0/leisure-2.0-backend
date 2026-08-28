package com.leisure.map.service;

import com.leisure.global.exception.BusinessException;
import com.leisure.global.exception.ErrorCode;
import com.leisure.map.dto.response.MapPinResponse;
import com.leisure.map.dto.response.RegionPinCountResponse;
import com.leisure.map.repository.MapRepositoryCustom;
import com.leisure.post.domain.PostCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MapQueryService {

    private final MapRepositoryCustom repository;

    private static final double MIN_LATITUDE = -90.0;

    private static final double MAX_LATITUDE = 90.0;

    private static final double MIN_LONGITUDE = -180.0;
    
    private static final double MAX_LONGITUDE = 180.0;

    // TODO: 프론트 줌 정책 확정 후 조정. 현재는 시 단위 화면 정도를 허용한다.
    private static final double MAX_LATITUDE_SPAN = 0.7;

    private static final double MAX_LONGITUDE_SPAN = 0.7;

    // 지역별 게시글 개수 조회 ==============================================================
    @Transactional(readOnly = true) // 트랜잭션을 읽기 전용으로 설정하여 성능 최적화
    public List<RegionPinCountResponse> getRegionPinCounts(PostCategory category) {
        // 쿼리가 바로 응답 DTO로 projection하므로 변환 단계 없이 그대로 반환
        return repository.findRegionPinCounts(category);
    }

    // 지도 범위 내 게시글 핀 조회 ==============================================================
    @Transactional(readOnly = true)
    public List<MapPinResponse> getPostPins(double minLat, double maxLat, double minLng, double maxLng, PostCategory category) {

        validateBounds(minLat, maxLat, minLng, maxLng);

        return repository.findPinsInBounds(minLat, maxLat, minLng, maxLng, category);
    }

    private void validateBounds(double minLat, double maxLat, double minLng, double maxLng) {
        // NaN, Infinity 같은 비정상 숫자는 DB 범위 조건에 넣기 전에 차단한다.
        if (!Double.isFinite(minLat) || !Double.isFinite(maxLat) || !Double.isFinite(minLng) || !Double.isFinite(maxLng)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER);
        }

        // 위도는 좌표계상 -90도에서 90도까지만 유효하다.
        if (minLat < MIN_LATITUDE || minLat > MAX_LATITUDE || maxLat < MIN_LATITUDE || maxLat > MAX_LATITUDE) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER);
        }

        // 경도는 좌표계상 -180도에서 180도까지만 유효하다.
        if (minLng < MIN_LONGITUDE || minLng > MAX_LONGITUDE || maxLng < MIN_LONGITUDE || maxLng > MAX_LONGITUDE) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER);
        }

        // 남쪽/북쪽 또는 서쪽/동쪽 경계가 뒤집힌 bounds는 조회하지 않는다.
        if (minLat > maxLat || minLng > maxLng) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER);
        }

        // 너무 넓은 화면에서 개별 핀 조회가 실행되지 않도록 서버에서 한 번 더 제한한다.
        if (maxLat - minLat > MAX_LATITUDE_SPAN || maxLng - minLng > MAX_LONGITUDE_SPAN) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER);
        }
    }
}

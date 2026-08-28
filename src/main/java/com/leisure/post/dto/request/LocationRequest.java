package com.leisure.post.dto.request;

import com.leisure.post.domain.PostLocation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

@Schema(description = "위치 정보. 각 필드는 선택이며, 카카오맵이 채우지 못한 필드는 null로 보내도 된다(그 정보 없음으로 저장). 단, 전 필드가 null이면 위치로 저장되지 않고 무시된다.")
public record LocationRequest(
        @Schema(description = "지역(예: 강릉). 목록 카드에 노출된다.")
        String region,

        String placeName,

        String address,

        @DecimalMin(value = "-90", inclusive = true)
        @DecimalMax(value = "90", inclusive = true)
        Double latitude,

        @DecimalMin(value = "-180", inclusive = true)
        @DecimalMax(value = "180", inclusive = true)
        Double longitude
) {
    public PostLocation toPostLocation() {
        return PostLocation.of(this.region, this.placeName, this.address, this.latitude, this.longitude);
    }
}
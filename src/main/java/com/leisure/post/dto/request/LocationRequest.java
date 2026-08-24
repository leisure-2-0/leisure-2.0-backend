package com.leisure.post.dto.request;

import com.leisure.post.domain.PostLocation;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

public record LocationRequest(
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
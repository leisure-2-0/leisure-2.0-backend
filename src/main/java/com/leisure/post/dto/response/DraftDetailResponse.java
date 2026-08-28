package com.leisure.post.dto.response;

import com.leisure.post.domain.PostCategory;
import com.leisure.post.dto.result.DraftDetailResult;

import java.time.LocalDateTime;
import java.util.List;

public record DraftDetailResponse(
        Long postId,

        String title,

        String content,

        PostCategory category,

        LocalDateTime updatedAt,

        Location location,

        List<String> tags
) {

    public record Location(
            String region,

            String placeName,

            String address,

            Double latitude,

            Double longitude
    ) {

        // 전 필드가 null이면 위치 없음으로 보고 null 반환
        public static Location of(String region, String placeName, String address, Double latitude, Double longitude) {
            if (region == null && placeName == null && address == null && latitude == null && longitude == null) {
                return null;
            }
            return new Location(region, placeName, address, latitude, longitude);
        }
    }

    public static DraftDetailResponse from(DraftDetailResult r, List<String> tags) {
        Location location = r.location() == null ? null
                : Location.of(
                        r.location().region(),
                        r.location().placeName(),
                        r.location().address(),
                        r.location().latitude(),
                        r.location().longitude());

        return new DraftDetailResponse(
                r.postId(),
                r.title(),
                r.content(),
                r.category(),
                r.updatedAt(),
                location,
                tags
        );
    }
}

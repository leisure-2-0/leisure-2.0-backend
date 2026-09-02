package com.leisure.global.external.tourapi.dto.response;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.leisure.global.external.tourapi.TourApiResponse;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FestivalListResponse(Response response) implements TourApiResponse {

    @Override
    public String resultCode() {
        if (response == null || response.header() == null) {
            return null;
        }
        return response.header().resultCode();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Response(Header header, Body body) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Header(String resultCode, String resultMsg) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Body(Items items, int totalCount) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Items(List<Item> item) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
            @JsonProperty("contentid")
            String contentId,

            String title,

            String addr1,

            String addr2,

            @JsonProperty("eventstartdate") String eventStartDate,

            @JsonProperty("eventenddate") String eventEndDate,

            String mapx,

            String mapy,

            @JsonProperty("lDongRegnCd")
            String ldongRegnCd,

            @JsonProperty("lDongSignguCd")
            String ldongSignguCd,

            @JsonProperty("contenttypeid")
            String contentTypeId,

            String lclsSystm2,

            String lclsSystm3,

            @JsonProperty("modifiedtime")
            String modifiedTime,

            @JsonProperty("firstimage2")
            String firstImage2
    ) {}
}

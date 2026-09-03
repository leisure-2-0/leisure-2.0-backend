package com.leisure.global.external.tourapi.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.leisure.global.external.tourapi.TourApiResponse;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FestivalDetailCommonResponse(Response response) implements TourApiResponse {

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
    public record Body(Items items) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Items(List<Item> item) {}

    public record Item
            (@JsonProperty("contentid") String contentId,

             String overview,

             String homepage) {}
}

package com.leisure.global.external.tourapi.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.leisure.global.external.tourapi.TourApiResponse;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LdongCodeResponse(Response response) implements TourApiResponse {

    // 중첩을 파고들어 resultCode 반환 (null 방어 포함)
    @Override
    public String resultCode() {
        if (response == null || response.header() == null) {
            return null;
        }
        return response.header().resultCode();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Response(Header header, Body body) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Header(String resultCode, String resultMsg) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Body(Items items) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Items(List<Item> item) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(String code, String name) {
    }
}

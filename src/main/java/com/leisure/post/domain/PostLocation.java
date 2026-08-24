package com.leisure.post.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class PostLocation {

    @Column(name = "region")
    private String region;

    @Column(name = "place_name")
    private String placeName;

    @Column(name = "address")
    private String address;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    private PostLocation(String region, String placeName, String address, Double latitude, Double longitude) {
        this.region = region;
        this.placeName = placeName;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public static PostLocation of(String region, String placeName, String address, Double latitude, Double longitude) {
        if (region == null && placeName == null && address == null && latitude == null && longitude == null) {
            return null;
        }
        return new PostLocation(region, placeName, address, latitude, longitude);
    }
 }

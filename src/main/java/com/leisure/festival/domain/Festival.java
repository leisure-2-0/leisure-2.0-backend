package com.leisure.festival.domain;

import com.leisure.festival.dto.command.FestivalData;
import com.leisure.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;


@Entity
@Table(name = "festivals")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Festival extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "festival_id")
    private Long festivalId;

    @Column(name = "tour_content_id", unique = true, nullable = false, updatable = false,
            comment = "TourAPI 콘텐츠 ID, 배치 동기화 기준")
    private String tourContentId;

    @Column(name = "name", nullable = false, comment = "축제/행사명")
    private String name;

    @Column(name = "overview", length = 3000, comment = "축제/행사 소개")
    private String overview;

    @Column(name = "address", comment = "기본 주소, TourAPI addr1")
    private String address;

    @Column(name = "detail_address", comment = "상세 주소, TourAPI addr2")
    private String detailAddress;

    @Column(name = "event_start_date", comment = "행사 시작일")
    private LocalDate eventStartDate;

    @Column(name = "event_end_date", comment = "행사 종료일")
    private LocalDate eventEndDate;

    @Column(name = "latitude", comment = "위도, TourAPI mapy")
    private Double latitude;

    @Column(name = "longitude", comment = "경도, TourAPI mapx")
    private Double longitude;

    @Column(name = "ldong_regn_cd", comment = "법정동 광역 코드 (Region 조인 키)")
    private String ldongRegnCd;

    @Column(name = "ldong_signgu_cd", comment = "법정동 시군구 코드 (Region 조인 키)")
    private String ldongSignguCd;

    @Column(name = "content_type_id", comment = "TourAPI 콘텐츠 타입 ID")
    private String contentTypeId;

    @Column(name = "lcls_systm2", comment = "TourAPI 중분류 코드 lclsSystm2 (축제/공연/행사)")
    private String lclsSystm2;

    @Column(name = "lcls_systm3", comment = "TourAPI 소분류 코드 lclsSystm3 (전시회/박람회/스포츠경기/기타행사)")
    private String lclsSystm3;

    @Column(name = "homepage_url", length = 1000, comment = "공식 홈페이지 URL")
    private String homepageUrl;

    @Column(name = "event_time", comment = "행사 운영 시간")
    private String eventTime;

    @Column(name = "tour_modified_at", comment = "TourAPI 콘텐츠 수정일시, modifiedtime — 상세 보강 배치의 델타 감지 기준")
    private LocalDateTime tourModifiedAt;

    @Column(name = "thumbnail_url", length = 1000, comment = "대표이미지 썸네일 URL, TourAPI firstimage2")
    private String thumbnailUrl;

    private Festival(String tourContentId, String name, String address, String detailAddress,
                     LocalDate eventStartDate, LocalDate eventEndDate, Double latitude, Double longitude,
                     String ldongRegnCd, String ldongSignguCd, String contentTypeId,
                     String lclsSystm2, String lclsSystm3, LocalDateTime tourModifiedAt, String thumbnailUrl) {
        this.tourContentId = tourContentId;
        this.name = name;
        this.address = address;
        this.detailAddress = detailAddress;
        this.eventStartDate = eventStartDate;
        this.eventEndDate = eventEndDate;
        this.latitude = latitude;
        this.longitude = longitude;
        this.ldongRegnCd = ldongRegnCd;
        this.ldongSignguCd = ldongSignguCd;
        this.contentTypeId = contentTypeId;
        this.lclsSystm2 = lclsSystm2;
        this.lclsSystm3 = lclsSystm3;
        this.tourModifiedAt = tourModifiedAt;
        this.thumbnailUrl = thumbnailUrl;
    }

    public static Festival create(FestivalData data) {
        return new Festival(data.tourContentId(), data.name(), data.address(), data.detailAddress(),
                data.eventStartDate(), data.eventEndDate(), data.latitude(), data.longitude(),
                data.ldongRegnCd(), data.ldongSignguCd(), data.contentTypeId(),
                data.lclsSystm2(), data.lclsSystm3(), data.tourModifiedAt(), data.thumbnailUrl());
    }

    public void updateFromList(FestivalData data) {
        this.name = data.name();
        this.address = data.address();
        this.detailAddress = data.detailAddress();
        this.eventStartDate = data.eventStartDate();
        this.eventEndDate = data.eventEndDate();
        this.latitude = data.latitude();
        this.longitude = data.longitude();
        this.ldongRegnCd = data.ldongRegnCd();
        this.ldongSignguCd = data.ldongSignguCd();
        this.contentTypeId = data.contentTypeId();
        this.lclsSystm2 = data.lclsSystm2();
        this.lclsSystm3 = data.lclsSystm3();
        this.thumbnailUrl = data.thumbnailUrl();

        LocalDateTime incoming = data.tourModifiedAt();
        if (incoming != null && (this.tourModifiedAt == null || incoming.isAfter(this.tourModifiedAt))) {
            this.overview = null;
            this.homepageUrl = null;
            this.eventTime = null;
        }

        if (incoming != null) {
            this.tourModifiedAt = incoming;
        }
    }

    public void updateFromDetailCommon(String overview, String homepageUrl) {
        this.overview = overview;
        this.homepageUrl = homepageUrl;
    }

    public void updateFromDetailIntro(String eventTime) {
        this.eventTime = eventTime;
    }
}


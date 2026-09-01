package com.leisure.region.dto.command;

// TourAPI에서 수집한 지역 한 건 — 네트워크(fetch)와 DB(upsert) 사이를 넘기는 중간 그릇.
public record RegionData(
        String ldongRegnCd,   // 법정동 광역 코드

        String ldongSignguCd, // 법정동 시군구 코드

        String regnName,      // 광역명

        String signguName     // 시군구명
) {}

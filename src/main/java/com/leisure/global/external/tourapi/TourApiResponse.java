package com.leisure.global.external.tourapi;

// 모든 TourAPI 응답이 구현 — resultCode로 성공/실패 판정 가능하게
public interface TourApiResponse {
    String resultCode();   // "0000"이면 성공
}

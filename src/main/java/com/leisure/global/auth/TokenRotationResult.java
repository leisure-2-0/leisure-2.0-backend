package com.leisure.global.auth;

public enum TokenRotationResult {
    SUCCESS,             // 성공 — 정상 회전 완료
    NOT_FOUND,           // 실패 — 토큰을 찾을 수 없음 (만료/로그아웃)
    MISMATCHED,          // 실패 — 저장된 토큰과 불일치
    CONCURRENTLY_UPDATED // 실패 — 동시 요청으로 인해 이미 변경됨
}

package com.leisure.chat.domain;

public enum MessageStatus {

    COMPLETED,    // 스트림 정상 완료

    INTERRUPTED,  // 스트림 중단 (브라우저 끊김/네트워크)

    FAILED        // AI 에러/타임아웃
}

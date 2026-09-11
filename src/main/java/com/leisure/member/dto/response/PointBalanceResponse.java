package com.leisure.member.dto.response;

public record PointBalanceResponse(int point) {

    public PointBalanceResponse {
        if (point < 0) {
            throw new IllegalArgumentException("포인트 잔액은 음수일 수 없습니다.");
        }
    }
}

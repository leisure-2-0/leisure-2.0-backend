package com.leisure.pointhistory.domain;

public enum PointType {

    POST_PUBLISH(5),

    LIKE_RECEIVED(2),

    BOOKMARK_RECEIVED(2);

    private final int amount;

    PointType(int amount) {
        this.amount = amount;
    }

    public int getAmount() {
        return amount;
    }
}

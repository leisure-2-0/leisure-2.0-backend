package com.leisure.global.auth;

public record TokenRotationContext(String publicId, String currentRefreshToken, String newRefreshToken, long ttl) {

}

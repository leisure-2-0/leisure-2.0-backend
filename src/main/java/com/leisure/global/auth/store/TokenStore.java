package com.leisure.global.auth.store;

public interface TokenStore {

    default void save(String token, long ttl) {

    }

    default boolean exists(String token) {
        return false;
    }

    default long getInvalidationVersion(String publicId) {
        return 0L;
    }

    default void increaseInvalidationVersion(String publicId) {

    }

}

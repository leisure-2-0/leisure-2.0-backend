package com.leisure.global.auth.store;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RedisTokenStatusStore implements TokenStore {

    private final StringRedisTemplate redisTemplate;

    @Override
    public long getInvalidationVersion(String publicId) {

        String key = buildTokenVersionKey(publicId);
        String value = redisTemplate.opsForValue().get(key);

        if (value == null) {
            return 0L;
        }

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e){
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void increaseInvalidationVersion(String publicId) {
        String key = buildTokenVersionKey(publicId);
        Long version = redisTemplate.opsForValue().increment(key);

        if (version == null) {
            throw new IllegalStateException(key);
        }
    }

    private String buildTokenVersionKey(String publicId) {
        return "token-version:" + publicId;
    }
}

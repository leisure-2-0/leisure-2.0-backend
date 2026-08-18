package com.leisure.global.auth.store;

import com.leisure.global.auth.TokenHasher;
import com.leisure.global.auth.TokenRotationContext;
import com.leisure.global.auth.TokenRotationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class RedisRefreshTokenStore {

    private final StringRedisTemplate redisTemplate;

    public TokenRotationResult rotate(TokenRotationContext context) {
        String key = buildRefreshTokenKey(context.publicId());
        String currentHash = TokenHasher.hash(context.currentRefreshToken());
        String newHash = TokenHasher.hash(context.newRefreshToken());

        return redisTemplate.execute(new SessionCallback<>() {
            @Override
            public TokenRotationResult execute(RedisOperations operations) throws DataAccessException {
                operations.watch(key);

                String storedHash = (String) operations.opsForValue().get(key);

                if (storedHash == null) {
                    operations.unwatch();
                    return TokenRotationResult.NOT_FOUND;
                }

                if (!storedHash.equals(currentHash)) {
                    operations.unwatch();
                    return TokenRotationResult.MISMATCHED;
                }

                operations.multi();
                operations.opsForValue().set(key, newHash, context.ttl(), TimeUnit.MILLISECONDS);
                List<Object> result = operations.exec();

                if (result == null || result.isEmpty()) {
                    return TokenRotationResult.CONCURRENTLY_UPDATED;
                }

                return TokenRotationResult.SUCCESS;
            }
        });
    }


    public void save(String publicId, String refreshToken, long ttl) {
        String key = buildRefreshTokenKey(publicId);
        redisTemplate.opsForValue().set(key, TokenHasher.hash(refreshToken), ttl, TimeUnit.MILLISECONDS);
    }


    public void remove(String publicId) {
        String key = buildRefreshTokenKey(publicId);
        redisTemplate.delete(key);
    }


    private String buildRefreshTokenKey(String publicId) {
        return "refresh:" + publicId;
    }
}

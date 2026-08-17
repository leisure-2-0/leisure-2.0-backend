package com.leisure.global.auth.store;

import com.leisure.global.auth.TokenHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class RedisBlacklistTokenStore implements TokenStore {

    private final StringRedisTemplate redisTemplate;

    @Override
    public void save(String accessToken, long ttl) {

        if (ttl <= 0) {
            return;
        }

        redisTemplate.opsForValue().set(
                buildBlacklistKey(accessToken),
                "true",
                ttl,
                TimeUnit.MILLISECONDS
        );

    }

    @Override
    public boolean exists(String accessToken) {

        Boolean result = redisTemplate.hasKey(buildBlacklistKey(accessToken));

        return result != null && result;
    }

    private String buildBlacklistKey(String token) {
        return "blacklist:" + TokenHasher.hash(token);
    }
}

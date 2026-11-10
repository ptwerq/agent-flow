package com.agentflow.clientservice.ratelimit;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class FixedWindowRateLimiter {
    private final StringRedisTemplate stringRedisTemplate;

    public boolean allow(
            String id,
            int limit,
            Duration windowSize
    ) {
        long windowIndex = System.currentTimeMillis() / windowSize.toMillis();
        String key = String.format("rate:%s:%s", id, windowIndex);
        Long countHits = stringRedisTemplate.opsForValue().increment(key);

        if (countHits != null && countHits == 1L) {
            stringRedisTemplate.expire(key, windowSize);
        }

        return countHits != null && countHits <= limit;
    }
}

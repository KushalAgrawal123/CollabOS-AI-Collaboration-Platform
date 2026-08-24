package com.collabos.backend.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Fixed-window counter in Redis: INCR the key, and on the very first hit in
 * the window (count == 1) attach the TTL that makes it expire. Simple and
 * good enough for a login-brute-force guard — a sliding window would be more
 * precise but isn't worth the extra complexity here.
 */
@Component
public class RateLimiter {

    private final StringRedisTemplate redisTemplate;

    public RateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean tryAcquire(String key, int maxAttempts, Duration window) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == null) {
            return true;
        }
        if (count == 1) {
            redisTemplate.expire(key, window);
        }
        return count <= maxAttempts;
    }
}

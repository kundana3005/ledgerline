package com.ledgerline.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/** Fixed-window counter. Redis in production, in-memory for local use and tests. Fails open if Redis is down. */
@Service
public class RateLimiter {
    private static final Logger log = LoggerFactory.getLogger(RateLimiter.class);
    private final boolean enabled;
    private final boolean useRedis;
    private final ObjectProvider<StringRedisTemplate> redis;
    private final ConcurrentHashMap<String, long[]> memory = new ConcurrentHashMap<>();

    public RateLimiter(@Value("${app.rate-limit.enabled:true}") boolean enabled,
                       @Value("${app.rate-limit.store:memory}") String store,
                       ObjectProvider<StringRedisTemplate> redis) {
        this.enabled = enabled;
        this.useRedis = "redis".equalsIgnoreCase(store);
        this.redis = redis;
    }

    /** Registers one hit and returns the number of hits in the current window (0 when disabled or failed open). */
    public long hit(String key, Duration window) {
        if (!enabled) return 0;
        if (useRedis) {
            try {
                StringRedisTemplate t = redis.getObject();
                t.opsForValue().setIfAbsent(key, "0", window);      // TTL is set when the key is created
                Long n = t.opsForValue().increment(key);
                return n == null ? 0 : n;
            } catch (RuntimeException e) {
                log.warn("Rate limiter unavailable, allowing the request: {}", e.toString());
                return 0;
            }
        }
        long now = System.currentTimeMillis();
        if (memory.size() > 10_000) memory.entrySet().removeIf(en -> en.getValue()[0] <= now);
        return memory.compute(key, (k, old) -> old == null || old[0] <= now
                ? new long[]{now + window.toMillis(), 1}
                : new long[]{old[0], old[1] + 1})[1];
    }
}
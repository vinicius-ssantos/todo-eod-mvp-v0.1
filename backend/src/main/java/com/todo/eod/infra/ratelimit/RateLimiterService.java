package com.todo.eod.infra.ratelimit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimiterService {

    private final StringRedisTemplate redis;
    private final int perOriginPerMinute;

    private final Map<String, Window> localWindows = new ConcurrentHashMap<>();

    public RateLimiterService(
            @Value("${eod.rateLimit.perOriginPerMinute:600}") int perOriginPerMinute,
            org.springframework.beans.factory.ObjectProvider<StringRedisTemplate> redisProvider
    ) {
        this.perOriginPerMinute = perOriginPerMinute;
        this.redis = redisProvider.getIfAvailable();
    }

    public boolean allow(String origin) {
        String minuteKey = currentMinuteKey(origin);
        if (redis != null) {
            try {
                var ops = redis.opsForValue();
                Boolean first = ops.setIfAbsent(minuteKey, "1", java.time.Duration.ofMinutes(1));
                long count;
                if (Boolean.TRUE.equals(first)) {
                    count = 1L;
                } else {
                    count = ops.increment(minuteKey);
                }
                return count <= perOriginPerMinute;
            } catch (Exception e) {
                // fallback to local if redis not reachable
            }
        }
        Window w = localWindows.computeIfAbsent(minuteKey, k -> new Window(System.currentTimeMillis() + 60_000));
        int c = w.counter.incrementAndGet();
        if (System.currentTimeMillis() > w.expiresAt) {
            localWindows.remove(minuteKey);
        }
        return c <= perOriginPerMinute;
    }

    private String currentMinuteKey(String origin) {
        String minute = DateTimeFormatter.ofPattern("yyyyMMddHHmm").withZone(ZoneOffset.UTC).format(Instant.now());
        return "rate:" + origin + ":" + minute;
    }

    private static class Window {
        final long expiresAt;
        final AtomicInteger counter = new AtomicInteger(0);
        Window(long expiresAt) { this.expiresAt = expiresAt; }
    }
}

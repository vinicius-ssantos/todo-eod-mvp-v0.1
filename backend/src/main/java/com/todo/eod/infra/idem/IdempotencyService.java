package com.todo.eod.infra.idem;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class IdempotencyService {

    private final StringRedisTemplate redis;
    private final int ttlSeconds;
    private final Map<String, Long> local = new ConcurrentHashMap<>();

    public IdempotencyService(@Value("${eod.idempotency.ttlSeconds:86400}") int ttlSeconds,
                              org.springframework.beans.factory.ObjectProvider<StringRedisTemplate> redisProvider) {
        this.ttlSeconds = ttlSeconds;
        this.redis = redisProvider.getIfAvailable();
    }

    public boolean isFirstProcessing(String eventId) {
        if (eventId == null || eventId.isBlank()) return true;
        if (redis != null) {
            try {
                Boolean ok = redis.opsForValue().setIfAbsent(key(eventId), "1", Duration.ofSeconds(ttlSeconds));
                if (Boolean.TRUE.equals(ok)) return true;
                return false;
            } catch (Exception e) {
                // fallback to local
            }
        }
        long now = System.currentTimeMillis();
        Long exp = local.putIfAbsent(eventId, now + ttlSeconds * 1000L);
        if (exp == null) return true;
        if (exp < now) { local.put(eventId, now + ttlSeconds * 1000L); return true; }
        return false;
    }

    private String key(String eventId) { return "idem:" + eventId; }
}

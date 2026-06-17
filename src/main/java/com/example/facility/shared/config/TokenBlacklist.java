package com.example.facility.shared.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenBlacklist {

    private final StringRedisTemplate redisTemplate;

    public void revoke(String jti, long ttlMillis) {
        try {
            if (ttlMillis > 0) {
                redisTemplate.opsForValue().set("blacklist:" + jti, "1", ttlMillis, TimeUnit.MILLISECONDS);
            }
        } catch (Exception e) {
            log.error("Failed to revoke token in Redis: {}", e.getMessage());
        }
    }

    public boolean isRevoked(String jti) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + jti));
        } catch (Exception e) {
            log.warn("Redis unavailable during blacklist check, treating token as valid: {}", e.getMessage());
            return false;
        }
    }
}


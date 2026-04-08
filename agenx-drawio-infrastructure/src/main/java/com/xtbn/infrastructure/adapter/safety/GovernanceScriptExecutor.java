package com.xtbn.infrastructure.adapter.safety;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;

@RequiredArgsConstructor
public class GovernanceScriptExecutor {
    private final StringRedisTemplate stringRedisTemplate;
    private final DefaultRedisScript<Long> tokenBucketScript;
    private final DefaultRedisScript<Long> concurrencyAcquireScript;
    private final DefaultRedisScript<Long> concurrencyReleaseScript;

    public long executeTokenBucket(List<String> keys, String nowMillis, String refillRate, String capacity, String requestedTokens, String ttlSeconds) {
        Long result = stringRedisTemplate.execute(tokenBucketScript, keys, nowMillis, refillRate, capacity, requestedTokens, ttlSeconds);
        return result == null ? 0L : result;
    }

    public long executeConcurrencyAcquire(List<String> keys, String limit, String leaseSeconds, String ownerValue) {
        Long result = stringRedisTemplate.execute(concurrencyAcquireScript, keys, limit, leaseSeconds, ownerValue);
        return result == null ? 0L : result;
    }

    public long executeConcurrencyRelease(List<String> keys, String leaseSeconds) {
        Long result = stringRedisTemplate.execute(concurrencyReleaseScript, keys, leaseSeconds);
        return result == null ? 0L : result;
    }
}

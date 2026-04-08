package com.xtbn.infrastructure.adapter.repository;

import com.xtbn.domain.agent.adapter.repository.IGovernanceRepository;
import com.xtbn.domain.agent.model.valobj.properties.PluginGovernanceProperties;
import com.xtbn.infrastructure.adapter.safety.GovernanceScriptExecutor;
import com.xtbn.infrastructure.dao.mapper.GovernanceBlacklistMapper;
import com.xtbn.infrastructure.dao.po.GovernanceBlacklistPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Repository
@RequiredArgsConstructor
public class GovernanceRepository implements IGovernanceRepository {
    private final StringRedisTemplate stringRedisTemplate;
    private final GovernanceScriptExecutor scriptExecutor;
    private final GovernanceBlacklistMapper governanceBlacklistMapper;
    private final PluginGovernanceProperties properties;
    private final ConcurrentMap<String, CacheEntry> blacklistLocalCache = new ConcurrentHashMap<>();

    @Override
    public boolean isBlacklisted(String subjectType, String subjectValue) {
        String cacheKey = subjectType + ":" + subjectValue;
        CacheEntry cacheEntry = blacklistLocalCache.get(cacheKey);
        long now = System.currentTimeMillis();
        if (cacheEntry != null && cacheEntry.expireAt >= now) {
            return cacheEntry.blacklisted;
        }

        String redisKey = blacklistRedisKey(subjectType);
        try {
            Boolean member = stringRedisTemplate.opsForSet().isMember(redisKey, subjectValue);
            boolean blacklisted = Boolean.TRUE.equals(member);
            putBlacklistCache(cacheKey, blacklisted, now);
            return blacklisted;
        } catch (Exception e) {
            log.warn("blacklist redis lookup failed, subjectType={}, subjectValue={}", subjectType, subjectValue, e);
            return false;
        }
    }

    @Override
    public boolean tryAcquireRateLimit(String scope, String identity, int permitsPerSecond) {
        String stateKey = key("rate:" + scope + ":" + identity);
        long now = System.currentTimeMillis();
        long result = scriptExecutor.executeTokenBucket(
                List.of(stateKey),
                String.valueOf(now),
                String.valueOf(permitsPerSecond),
                String.valueOf(permitsPerSecond),
                "1",
                "2"
        );
        return result == 1L;
    }

    @Override
    public boolean tryAcquireDailyQuota(String identity, int quotaLimit) {
        String key = key("quota:" + LocalDate.now().toString() + ":" + identity);
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count == null) {
            return true;
        }
        if (count == 1L) {
            long ttlSeconds = secondsUntilTomorrow();
            stringRedisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);
        }
        return count <= quotaLimit;
    }

    @Override
    public boolean tryAcquireConcurrency(String scope, String identity, String invocationId, int concurrencyLimit, long leaseSeconds) {
        String counterKey = key("concurrency:" + scope + ":" + identity);
        String ownerKey = key("concurrency_owner:" + scope + ":" + invocationId);
        long result = scriptExecutor.executeConcurrencyAcquire(
                List.of(counterKey, ownerKey),
                String.valueOf(concurrencyLimit),
                String.valueOf(leaseSeconds),
                identity
        );
        return result == 1L;
    }

    @Override
    public void releaseConcurrency(String invocationId) {
        releaseConcurrencyScope("global", invocationId, properties.getConcurrencyLeaseSeconds());
        releaseConcurrencyScope("user", invocationId, properties.getConcurrencyLeaseSeconds());
    }

    @Override
    public boolean isCircuitOpen(String identity) {
        String breakerKey = key("breaker:" + identity);
        Object openUntil = stringRedisTemplate.opsForHash().get(breakerKey, "openUntil");
        if (openUntil == null) {
            return false;
        }
        long openUntilValue = Long.parseLong(openUntil.toString());
        return openUntilValue > System.currentTimeMillis();
    }

    @Override
    public void markCircuitSuccess(String identity) {
        String breakerKey = key("breaker:" + identity);
        stringRedisTemplate.delete(breakerKey);
    }

    @Override
    public void markCircuitFailure(String identity, int failureThreshold, int openSeconds) {
        String breakerKey = key("breaker:" + identity);
        Long failures = stringRedisTemplate.opsForHash().increment(breakerKey, "consecutiveFailures", 1L);
        stringRedisTemplate.expire(breakerKey, Duration.ofSeconds(Math.max(openSeconds, 60)));
        if (failures != null && failures >= failureThreshold) {
            long openUntil = System.currentTimeMillis() + openSeconds * 1000L;
            stringRedisTemplate.opsForHash().put(breakerKey, "openUntil", String.valueOf(openUntil));
            stringRedisTemplate.expire(breakerKey, Duration.ofSeconds(openSeconds));
        }
    }

    @Override
    public void preloadBlacklist() {
        long now = System.currentTimeMillis();
        List<GovernanceBlacklistPO> activeRules = governanceBlacklistMapper.queryActiveRules(now);
        blacklistLocalCache.clear();
        stringRedisTemplate.delete(key("blacklist:USER"));
        stringRedisTemplate.delete(key("blacklist:IP"));
        stringRedisTemplate.delete(key("blacklist:TENANT"));
        for (GovernanceBlacklistPO rule : activeRules) {
            refreshBlacklistRule(rule.getSubjectType(), rule.getSubjectValue(), true);
        }
        Set<String> defaults = properties.getBlacklist();
        if (defaults != null) {
            for (String value : defaults) {
                if (value != null && !value.isBlank()) {
                    refreshBlacklistRule("USER", value, true);
                }
            }
        }
        log.info("governance blacklist preloaded, size={}", activeRules.size());
    }

    @Override
    public void refreshBlacklistRule(String subjectType, String subjectValue, boolean blacklisted) {
        if (subjectType == null || subjectType.isBlank() || subjectValue == null || subjectValue.isBlank()) {
            return;
        }
        String normalizedType = subjectType.trim().toUpperCase();
        String normalizedValue = subjectValue.trim();
        String redisKey = blacklistRedisKey(normalizedType);
        String cacheKey = normalizedType + ":" + normalizedValue;
        if (blacklisted) {
            stringRedisTemplate.opsForSet().add(redisKey, normalizedValue);
            putBlacklistCache(cacheKey, true, System.currentTimeMillis());
            return;
        }
        stringRedisTemplate.opsForSet().remove(redisKey, normalizedValue);
        putBlacklistCache(cacheKey, false, System.currentTimeMillis());
    }

    private void releaseConcurrencyScope(String scope, String invocationId, long leaseSeconds) {
        String ownerKey = key("concurrency_owner:" + scope + ":" + invocationId);
        String identity = stringRedisTemplate.opsForValue().get(ownerKey);
        if (identity == null || identity.isBlank()) {
            return;
        }
        String counterKey = key("concurrency:" + scope + ":" + identity);
        scriptExecutor.executeConcurrencyRelease(List.of(counterKey, ownerKey), String.valueOf(leaseSeconds));
    }

    private void putBlacklistCache(String cacheKey, boolean blacklisted, long now) {
        blacklistLocalCache.put(cacheKey, new CacheEntry(blacklisted, now + properties.getBlacklistCacheSeconds() * 1000L));
    }

    private String blacklistRedisKey(String subjectType) {
        return key("blacklist:" + subjectType);
    }

    private String key(String suffix) {
        return properties.getRedisPrefix() + ":" + suffix;
    }

    private long secondsUntilTomorrow() {
        ZoneId zoneId = ZoneId.systemDefault();
        return LocalDate.now(zoneId).plusDays(1).atStartOfDay(zoneId).toEpochSecond() - (System.currentTimeMillis() / 1000L);
    }

    private record CacheEntry(boolean blacklisted, long expireAt) {
    }
}

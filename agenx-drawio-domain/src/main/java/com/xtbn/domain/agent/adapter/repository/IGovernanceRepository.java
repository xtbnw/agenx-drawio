package com.xtbn.domain.agent.adapter.repository;

public interface IGovernanceRepository {

    boolean isBlacklisted(String subjectType, String subjectValue);

    boolean tryAcquireRateLimit(String scope, String identity, int permitsPerSecond);

    boolean tryAcquireDailyQuota(String identity, int quotaLimit);

    boolean tryAcquireConcurrency(String scope, String identity, String invocationId, int concurrencyLimit, long leaseSeconds);

    void releaseConcurrency(String invocationId);

    boolean isCircuitOpen(String identity);

    void markCircuitSuccess(String identity);

    void markCircuitFailure(String identity, int failureThreshold, int openSeconds);

    void preloadBlacklist();

    void refreshBlacklistRule(String subjectType, String subjectValue, boolean blacklisted);
}

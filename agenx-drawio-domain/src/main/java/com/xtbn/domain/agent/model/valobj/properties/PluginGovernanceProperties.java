package com.xtbn.domain.agent.model.valobj.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashSet;
import java.util.Set;

@Data
@ConfigurationProperties(prefix = "agent.plugin.governance")
public class PluginGovernanceProperties {
    private boolean enabled = true;
    private int globalRateLimitPerSecond = 50;
    private int userRateLimitPerSecond = 5;
    private int globalConcurrencyLimit = 20;
    private int userConcurrencyLimit = 3;
    private int defaultUserQuotaPerDay = 1000;
    private String redisPrefix = "governance";
    private long concurrencyLeaseSeconds = 300;
    private long blacklistCacheSeconds = 30;
    private Set<String> blacklist = new LinkedHashSet<>();
    private Breaker breaker = new Breaker();

    @Data
    public static class Breaker {
        private boolean enabled = false;
        private int failureThreshold = 5;
        private int openSeconds = 60;
    }
}

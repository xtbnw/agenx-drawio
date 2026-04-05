package com.xtbn.domain.agent.model.valobj.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "agent.plugin.governance")
public class PluginGovernanceProperties {
    private boolean enabled = true;
    private int userRateLimitPerSecond = 5;
    private int userConcurrencyLimit = 3;
    private int defaultUserQuotaPerDay = 1000;
    private List<String> blacklist = new ArrayList<>();
    private Breaker breaker = new Breaker();

    @Data
    public static class Breaker {
        private boolean enabled = false;
        private int failureThreshold = 5;
        private int openSeconds = 60;
    }
}

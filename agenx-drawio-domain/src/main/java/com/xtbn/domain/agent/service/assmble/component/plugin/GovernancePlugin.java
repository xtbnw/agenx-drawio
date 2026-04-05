package com.xtbn.domain.agent.service.assmble.component.plugin;

import com.google.adk.agents.CallbackContext;
import com.google.adk.agents.InvocationContext;
import com.google.adk.models.LlmRequest;
import com.google.adk.models.LlmResponse;
import com.google.genai.types.Content;
import com.xtbn.domain.agent.model.valobj.properties.PluginGovernanceProperties;
import com.xtbn.domain.agent.service.assmble.component.plugin.support.AbstractAgentPluginSupport;
import com.xtbn.types.enums.ResponseCode;
import com.xtbn.types.exception.AppException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service("governancePlugin")
public class GovernancePlugin extends AbstractAgentPluginSupport {
    private final PluginGovernanceProperties properties;
    private final ConcurrentMap<String, Deque<Long>> userRequestWindows = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, AtomicInteger> userConcurrency = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> invocationOwners = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, UserQuotaState> userQuotaStates = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, CircuitState> circuitStates = new ConcurrentHashMap<>();

    public GovernancePlugin(MeterRegistry meterRegistry, PluginGovernanceProperties properties) {
        super("GovernancePlugin", meterRegistry);
        this.properties = properties;
    }

    @Override
    public Maybe<Content> beforeRunCallback(InvocationContext invocationContext) {
        if (!properties.isEnabled()) {
            return super.beforeRunCallback(invocationContext);
        }
        fillInvocationMdc(invocationContext);
        try {
            String userId = safe(invocationContext.userId());
            rejectIfBlacklisted(invocationContext, userId);
            rejectIfRateLimited(invocationContext, userId);
            rejectIfQuotaExceeded(invocationContext, userId);
            rejectIfConcurrentLimited(invocationContext, userId);
            return super.beforeRunCallback(invocationContext);
        } finally {
            clearMdc();
        }
    }

    @Override
    public Completable afterRunCallback(InvocationContext invocationContext) {
        if (!properties.isEnabled()) {
            return super.afterRunCallback(invocationContext);
        }
        try {
            String userId = invocationOwners.remove(invocationContext.invocationId());
            if (userId != null) {
                AtomicInteger active = userConcurrency.get(userId);
                if (active != null) {
                    active.updateAndGet(value -> Math.max(value - 1, 0));
                }
            }
            return super.afterRunCallback(invocationContext);
        } finally {
            clearMdc();
        }
    }

    @Override
    public Maybe<LlmResponse> beforeModelCallback(CallbackContext callbackContext, LlmRequest.Builder requestBuilder) {
        if (!properties.isEnabled() || !properties.getBreaker().isEnabled()) {
            return super.beforeModelCallback(callbackContext, requestBuilder);
        }
        String userId = safe(callbackContext.userId());
        CircuitState circuitState = circuitStates.computeIfAbsent(userId, key -> new CircuitState());
        long now = System.currentTimeMillis();
        if (circuitState.openUntil > now) {
            markGovernanceRejection(callbackContext.invocationContext(), "circuit_open");
            throw new AppException(ResponseCode.CIRCUIT_BREAKER_OPEN.getCode(), ResponseCode.CIRCUIT_BREAKER_OPEN.getInfo());
        }
        return super.beforeModelCallback(callbackContext, requestBuilder);
    }

    @Override
    public Maybe<LlmResponse> afterModelCallback(CallbackContext callbackContext, LlmResponse llmResponse) {
        if (!properties.isEnabled() || !properties.getBreaker().isEnabled()) {
            return super.afterModelCallback(callbackContext, llmResponse);
        }
        String userId = safe(callbackContext.userId());
        CircuitState circuitState = circuitStates.computeIfAbsent(userId, key -> new CircuitState());
        circuitState.consecutiveFailures = 0;
        circuitState.openUntil = 0L;
        return super.afterModelCallback(callbackContext, llmResponse);
    }

    @Override
    public Maybe<LlmResponse> onModelErrorCallback(CallbackContext callbackContext, LlmRequest.Builder requestBuilder, Throwable throwable) {
        if (!properties.isEnabled() || !properties.getBreaker().isEnabled()) {
            return super.onModelErrorCallback(callbackContext, requestBuilder, throwable);
        }
        String userId = safe(callbackContext.userId());
        CircuitState circuitState = circuitStates.computeIfAbsent(userId, key -> new CircuitState());
        circuitState.consecutiveFailures++;
        if (circuitState.consecutiveFailures >= properties.getBreaker().getFailureThreshold()) {
            circuitState.openUntil = System.currentTimeMillis() + properties.getBreaker().getOpenSeconds() * 1000L;
            log.warn("Circuit breaker opened for userId={}, openUntil={}", userId, circuitState.openUntil);
        }
        return super.onModelErrorCallback(callbackContext, requestBuilder, throwable);
    }

    @Scheduled(fixedDelay = 600000L, initialDelay = 600000L)
    public void cleanupStates() {
        long now = System.currentTimeMillis();
        LocalDate today = LocalDate.now();

        for (Map.Entry<String, Deque<Long>> entry : userRequestWindows.entrySet()) {
            Deque<Long> window = entry.getValue();
            synchronized (window) {
                while (!window.isEmpty() && now - window.peekFirst() >= 1000L) {
                    window.pollFirst();
                }
                if (window.isEmpty()) {
                    userRequestWindows.remove(entry.getKey(), window);
                }
            }
        }

        userConcurrency.entrySet().removeIf(entry -> entry.getValue().get() <= 0 && !invocationOwners.containsValue(entry.getKey()));
        userQuotaStates.entrySet().removeIf(entry -> !today.equals(entry.getValue().date));
        circuitStates.entrySet().removeIf(entry -> entry.getValue().openUntil <= now && entry.getValue().consecutiveFailures == 0);
    }

    private void rejectIfBlacklisted(InvocationContext invocationContext, String userId) {
        if (properties.getBlacklist() != null && properties.getBlacklist().contains(userId)) {
            markGovernanceRejection(invocationContext, "blacklist");
            throw new AppException(ResponseCode.USER_BLACKLISTED.getCode(), ResponseCode.USER_BLACKLISTED.getInfo());
        }
    }

    private void rejectIfRateLimited(InvocationContext invocationContext, String userId) {
        int limit = properties.getUserRateLimitPerSecond();
        if (limit <= 0) {
            return;
        }
        Deque<Long> window = userRequestWindows.computeIfAbsent(userId, key -> new ArrayDeque<>());
        synchronized (window) {
            long now = System.currentTimeMillis();
            while (!window.isEmpty() && now - window.peekFirst() >= 1000L) {
                window.pollFirst();
            }
            if (window.size() >= limit) {
                markGovernanceRejection(invocationContext, "rate_limit");
                throw new AppException(ResponseCode.RATE_LIMITED.getCode(), ResponseCode.RATE_LIMITED.getInfo());
            }
            window.addLast(now);
        }
    }

    private void rejectIfQuotaExceeded(InvocationContext invocationContext, String userId) {
        int quotaLimit = properties.getDefaultUserQuotaPerDay();
        if (quotaLimit <= 0) {
            return;
        }
        UserQuotaState quotaState = userQuotaStates.computeIfAbsent(userId, key -> new UserQuotaState());
        synchronized (quotaState) {
            LocalDate today = LocalDate.now();
            if (!today.equals(quotaState.date)) {
                quotaState.date = today;
                quotaState.count = 0;
            }
            if (quotaState.count >= quotaLimit) {
                markGovernanceRejection(invocationContext, "quota");
                throw new AppException(ResponseCode.USER_QUOTA_EXCEEDED.getCode(), ResponseCode.USER_QUOTA_EXCEEDED.getInfo());
            }
            quotaState.count++;
        }
    }

    private void rejectIfConcurrentLimited(InvocationContext invocationContext, String userId) {
        int concurrencyLimit = properties.getUserConcurrencyLimit();
        if (concurrencyLimit <= 0) {
            return;
        }
        AtomicInteger active = userConcurrency.computeIfAbsent(userId, key -> new AtomicInteger(0));
        while (true) {
            int current = active.get();
            if (current >= concurrencyLimit) {
                markGovernanceRejection(invocationContext, "concurrency");
                throw new AppException(ResponseCode.RATE_LIMITED.getCode(), "USER_CONCURRENCY_LIMITED");
            }
            if (active.compareAndSet(current, current + 1)) {
                invocationOwners.put(invocationContext.invocationId(), userId);
                return;
            }
        }
    }

    private void markGovernanceRejection(InvocationContext invocationContext, String result) {
        Counter.builder("agent_governance_rejections_total")
                .tags(resultTags(invocationContext, result))
                .register(meterRegistry)
                .increment();
    }

    private static class UserQuotaState {
        private LocalDate date = LocalDate.now();
        private int count;
    }

    private static class CircuitState {
        private int consecutiveFailures;
        private long openUntil;
    }
}

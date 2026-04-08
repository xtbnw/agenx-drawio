package com.xtbn.domain.agent.service.assmble.component.plugin;

import com.google.adk.agents.CallbackContext;
import com.google.adk.agents.InvocationContext;
import com.google.adk.models.LlmRequest;
import com.google.adk.models.LlmResponse;
import com.google.genai.types.Content;
import com.xtbn.domain.agent.adapter.repository.IGovernanceRepository;
import com.xtbn.domain.agent.model.valobj.properties.PluginGovernanceProperties;
import com.xtbn.domain.agent.service.assmble.component.plugin.support.AbstractAgentPluginSupport;
import com.xtbn.types.enums.ResponseCode;
import com.xtbn.types.exception.AppException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service("governancePlugin")
public class GovernancePlugin extends AbstractAgentPluginSupport {
    private final PluginGovernanceProperties properties;
    private final IGovernanceRepository governanceRepository;
    private final AtomicInteger governanceRedisErrors = new AtomicInteger();

    public GovernancePlugin(MeterRegistry meterRegistry, PluginGovernanceProperties properties, IGovernanceRepository governanceRepository) {
        super("GovernancePlugin", meterRegistry);
        this.properties = properties;
        this.governanceRepository = governanceRepository;
        Gauge.builder("agent_governance_redis_errors_current", governanceRedisErrors, AtomicInteger::get)
                .register(meterRegistry);
    }

    @Override
    public Maybe<Content> beforeRunCallback(InvocationContext invocationContext) {
        if (!properties.isEnabled()) {
            return super.beforeRunCallback(invocationContext);
        }
        fillInvocationMdc(invocationContext);
        try {
            String identity = resolveIdentity(invocationContext);
            rejectIfBlacklisted(invocationContext, identity);
            rejectIfGlobalRateLimited(invocationContext);
            rejectIfRateLimited(invocationContext, identity);
            rejectIfQuotaExceeded(invocationContext, identity);
            rejectIfGlobalConcurrentLimited(invocationContext);
            rejectIfConcurrentLimited(invocationContext, identity);
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
            safely("release_concurrency", () -> governanceRepository.releaseConcurrency(invocationContext.invocationId()));
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
        String identity = resolveIdentity(callbackContext);
        if (safely("breaker_check", () -> governanceRepository.isCircuitOpen(identity), false)) {
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
        String identity = resolveIdentity(callbackContext);
        safely("breaker_success", () -> governanceRepository.markCircuitSuccess(identity));
        return super.afterModelCallback(callbackContext, llmResponse);
    }

    @Override
    public Maybe<LlmResponse> onModelErrorCallback(CallbackContext callbackContext, LlmRequest.Builder requestBuilder, Throwable throwable) {
        if (!properties.isEnabled() || !properties.getBreaker().isEnabled()) {
            return super.onModelErrorCallback(callbackContext, requestBuilder, throwable);
        }
        String identity = resolveIdentity(callbackContext);
        safely("breaker_failure", () -> governanceRepository.markCircuitFailure(
                identity,
                properties.getBreaker().getFailureThreshold(),
                properties.getBreaker().getOpenSeconds()
        ));
        return super.onModelErrorCallback(callbackContext, requestBuilder, throwable);
    }

    private void rejectIfBlacklisted(InvocationContext invocationContext, String identity) {
        if (safely("blacklist_check", () -> governanceRepository.isBlacklisted("USER", identity), false)) {
            markGovernanceRejection(invocationContext, "blacklist");
            throw new AppException(ResponseCode.USER_BLACKLISTED.getCode(), ResponseCode.USER_BLACKLISTED.getInfo());
        }
    }

    private void rejectIfGlobalRateLimited(InvocationContext invocationContext) {
        int limit = properties.getGlobalRateLimitPerSecond();
        if (limit <= 0) {
            return;
        }
        boolean allowed = safely("global_rate_limit", () -> governanceRepository.tryAcquireRateLimit("global", "global", limit), true);
        if (!allowed) {
            markGovernanceRejection(invocationContext, "global_rate_limit");
            throw new AppException(ResponseCode.GLOBAL_RATE_LIMITED.getCode(), ResponseCode.GLOBAL_RATE_LIMITED.getInfo());
        }
        markGovernanceAccepted("global_rate_limit");
    }

    private void rejectIfRateLimited(InvocationContext invocationContext, String identity) {
        int limit = properties.getUserRateLimitPerSecond();
        if (limit <= 0) {
            return;
        }
        boolean allowed = safely("user_rate_limit", () -> governanceRepository.tryAcquireRateLimit("user", identity, limit), true);
        if (!allowed) {
            markGovernanceRejection(invocationContext, "rate_limit");
            throw new AppException(ResponseCode.RATE_LIMITED.getCode(), ResponseCode.RATE_LIMITED.getInfo());
        }
        markGovernanceAccepted("user_rate_limit");
    }

    private void rejectIfQuotaExceeded(InvocationContext invocationContext, String identity) {
        int quotaLimit = properties.getDefaultUserQuotaPerDay();
        if (quotaLimit <= 0) {
            return;
        }
        boolean allowed = safely("quota_check", () -> governanceRepository.tryAcquireDailyQuota(identity, quotaLimit), true);
        if (!allowed) {
            markGovernanceRejection(invocationContext, "quota");
            throw new AppException(ResponseCode.USER_QUOTA_EXCEEDED.getCode(), ResponseCode.USER_QUOTA_EXCEEDED.getInfo());
        }
        markGovernanceAccepted("quota");
    }

    private void rejectIfGlobalConcurrentLimited(InvocationContext invocationContext) {
        int concurrencyLimit = properties.getGlobalConcurrencyLimit();
        if (concurrencyLimit <= 0) {
            return;
        }
        boolean allowed = safely("global_concurrency", () -> governanceRepository.tryAcquireConcurrency(
                "global",
                "global",
                invocationContext.invocationId(),
                concurrencyLimit,
                properties.getConcurrencyLeaseSeconds()
        ), true);
        if (!allowed) {
            markGovernanceRejection(invocationContext, "global_concurrency");
            throw new AppException(ResponseCode.GLOBAL_CONCURRENCY_LIMITED.getCode(), ResponseCode.GLOBAL_CONCURRENCY_LIMITED.getInfo());
        }
        markGovernanceAccepted("global_concurrency");
    }

    private void rejectIfConcurrentLimited(InvocationContext invocationContext, String identity) {
        int concurrencyLimit = properties.getUserConcurrencyLimit();
        if (concurrencyLimit <= 0) {
            return;
        }
        boolean allowed = safely("user_concurrency", () -> governanceRepository.tryAcquireConcurrency(
                "user",
                identity,
                invocationContext.invocationId(),
                concurrencyLimit,
                properties.getConcurrencyLeaseSeconds()
        ), true);
        if (!allowed) {
            markGovernanceRejection(invocationContext, "concurrency");
            throw new AppException(ResponseCode.USER_CONCURRENCY_LIMITED.getCode(), ResponseCode.USER_CONCURRENCY_LIMITED.getInfo());
        }
        markGovernanceAccepted("user_concurrency");
    }

    private void markGovernanceRejection(InvocationContext invocationContext, String result) {
        Counter.builder("agent_governance_rejections_total")
                .tags(resultTags(invocationContext, result))
                .register(meterRegistry)
                .increment();
    }

    private void markGovernanceAccepted(String action) {
        Counter.builder("agent_governance_requests_total")
                .tag("action", safe(action))
                .tag("result", "accepted")
                .register(meterRegistry)
                .increment();
    }

    private String resolveIdentity(InvocationContext invocationContext) {
        String userId = invocationContext == null ? null : invocationContext.userId();
        if (userId != null && !userId.isBlank()) {
            return userId;
        }
        String sessionId = invocationContext == null || invocationContext.session() == null ? null : invocationContext.session().id();
        if (sessionId != null && !sessionId.isBlank()) {
            return "anonymous:" + sessionId;
        }
        String invocationId = invocationContext == null ? null : invocationContext.invocationId();
        return invocationId == null || invocationId.isBlank() ? "anonymous:unknown" : "anonymous:" + invocationId;
    }

    private String resolveIdentity(CallbackContext callbackContext) {
        return resolveIdentity(callbackContext == null ? null : callbackContext.invocationContext());
    }

    private void safely(String action, Runnable runnable) {
        safely(action, () -> {
            runnable.run();
            return Boolean.TRUE;
        }, Boolean.TRUE);
    }

    private boolean safely(String action, BoolSupplier supplier, boolean defaultValue) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            boolean result = supplier.getAsBoolean();
            sample.stop(Timer.builder("agent_governance_operation_latency")
                    .tag("action", safe(action))
                    .tag("result", "success")
                    .register(meterRegistry));
            return result;
        } catch (Exception e) {
            governanceRedisErrors.incrementAndGet();
            Counter.builder("agent_governance_redis_errors_total")
                    .tag("action", safe(action))
                    .register(meterRegistry)
                    .increment();
            sample.stop(Timer.builder("agent_governance_operation_latency")
                    .tag("action", safe(action))
                    .tag("result", "error")
                    .register(meterRegistry));
            log.warn("governance action failed, action={}", action, e);
            return defaultValue;
        }
    }

    @FunctionalInterface
    private interface BoolSupplier {
        boolean getAsBoolean() throws Exception;
    }
}

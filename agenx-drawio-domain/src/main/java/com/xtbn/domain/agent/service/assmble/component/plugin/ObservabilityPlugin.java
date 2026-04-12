package com.xtbn.domain.agent.service.assmble.component.plugin;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.CallbackContext;
import com.google.adk.agents.InvocationContext;
import com.google.adk.models.LlmRequest;
import com.google.adk.models.LlmResponse;
import com.google.adk.tools.BaseTool;
import com.google.adk.tools.ToolContext;
import com.google.genai.types.Content;
import com.xtbn.domain.agent.model.valobj.properties.PluginObservabilityProperties;
import com.xtbn.domain.agent.service.assmble.component.plugin.support.AbstractAgentPluginSupport;
import com.xtbn.types.common.MetricsConstants;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service("observabilityPlugin")
public class ObservabilityPlugin extends AbstractAgentPluginSupport {
    private final PluginObservabilityProperties properties;
    private final Set<String> failedInvocations = ConcurrentHashMap.newKeySet();

    public ObservabilityPlugin(MeterRegistry meterRegistry, PluginObservabilityProperties properties) {
        super("ObservabilityPlugin", meterRegistry);
        this.properties = properties;
    }

    @Override
    public Maybe<Content> onUserMessageCallback(InvocationContext invocationContext, Content userMessage) {
        if (!properties.isEnabled()) {
            return super.onUserMessageCallback(invocationContext, userMessage);
        }
        fillInvocationMdc(invocationContext);
        try {
            log.info("Received user message: {}", userMessage == null ? "" : userMessage.text());
            return super.onUserMessageCallback(invocationContext, userMessage);
        } finally {
            clearMdc();
        }
    }

    @Override
    public Maybe<Content> beforeRunCallback(InvocationContext invocationContext) {
        if (!properties.isEnabled()) {
            return super.beforeRunCallback(invocationContext);
        }
        fillInvocationMdc(invocationContext);
        startTimer(invocationKey(invocationContext));
        Counter.builder(MetricsConstants.AGENT_REQUESTS_TOTAL)
                .tags(commonTags(invocationContext))
                .register(meterRegistry)
                .increment();
        try {
            log.info("Invocation started");
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
        fillInvocationMdc(invocationContext);
        long elapsedMillis = stopTimerMillis(invocationKey(invocationContext));
        boolean failed = failedInvocations.remove(invocationContext.invocationId());
        Timer.builder(MetricsConstants.AGENT_RUN_DURATION_SECONDS)
                .tags(resultTags(invocationContext, failed ? "failure" : "success").and("phase", "run"))
                .publishPercentileHistogram()
                .minimumExpectedValue(MetricsConstants.METRIC_MIN_DURATION)
                .maximumExpectedValue(MetricsConstants.METRIC_MAX_DURATION)
                .register(meterRegistry)
                .record(Math.max(elapsedMillis, 0L), TimeUnit.MILLISECONDS);
        if (failed) {
            Counter.builder(MetricsConstants.AGENT_REQUEST_FAILURES_TOTAL)
                    .tags(commonTags(invocationContext))
                    .register(meterRegistry)
                    .increment();
        }
        try {
            log.info("Invocation completed in {} ms", elapsedMillis);
            return super.afterRunCallback(invocationContext);
        } finally {
            clearMdc();
        }
    }

    @Override
    public Maybe<Content> beforeAgentCallback(BaseAgent agent, CallbackContext callbackContext) {
        if (!properties.isEnabled()) {
            return super.beforeAgentCallback(agent, callbackContext);
        }
        if (!properties.isAgentStepMetricsEnabled()) {
            return super.beforeAgentCallback(agent, callbackContext);
        }
        fillCallbackMdc(callbackContext);
        startTimer(callbackKey(callbackContext, "agent"));
        try {
            log.info("Agent started: {}", agent == null ? "" : agent.name());
            return super.beforeAgentCallback(agent, callbackContext);
        } finally {
            clearMdc();
        }
    }

    @Override
    public Maybe<Content> afterAgentCallback(BaseAgent agent, CallbackContext callbackContext) {
        if (!properties.isEnabled()) {
            return super.afterAgentCallback(agent, callbackContext);
        }
        if (!properties.isAgentStepMetricsEnabled()) {
            return super.afterAgentCallback(agent, callbackContext);
        }
        fillCallbackMdc(callbackContext);
        long elapsedMillis = stopTimerMillis(callbackKey(callbackContext, "agent"));
        Timer.builder(MetricsConstants.AGENT_RUN_DURATION_SECONDS)
                .tags(commonTags(callbackContext).and("result", "success").and("phase", "agent"))
                .publishPercentileHistogram()
                .minimumExpectedValue(MetricsConstants.METRIC_MIN_DURATION)
                .maximumExpectedValue(MetricsConstants.METRIC_MAX_DURATION)
                .register(meterRegistry)
                .record(Math.max(elapsedMillis, 0L), TimeUnit.MILLISECONDS);
        try {
            log.info("Agent completed: {}, elapsed={} ms", agent == null ? "" : agent.name(), elapsedMillis);
            return super.afterAgentCallback(agent, callbackContext);
        } finally {
            clearMdc();
        }
    }

    @Override
    public Maybe<LlmResponse> beforeModelCallback(CallbackContext callbackContext, LlmRequest.Builder requestBuilder) {
        if (!properties.isEnabled()) {
            return super.beforeModelCallback(callbackContext, requestBuilder);
        }
        if (!properties.isModelMetricsEnabled()) {
            return super.beforeModelCallback(callbackContext, requestBuilder);
        }
        fillCallbackMdc(callbackContext);
        String model = safe(requestBuilder.build().model().orElse("unknown"));
        startTimer(callbackKey(callbackContext, "model"));
        Counter.builder(MetricsConstants.AGENT_MODEL_CALLS_TOTAL)
                .tags(modelTags(callbackContext, model, "started"))
                .register(meterRegistry)
                .increment();
        try {
            log.info("Model started: {}", model);
            return super.beforeModelCallback(callbackContext, requestBuilder);
        } finally {
            clearMdc();
        }
    }

    @Override
    public Maybe<LlmResponse> afterModelCallback(CallbackContext callbackContext, LlmResponse llmResponse) {
        if (!properties.isEnabled()) {
            return super.afterModelCallback(callbackContext, llmResponse);
        }
        if (!properties.isModelMetricsEnabled()) {
            return super.afterModelCallback(callbackContext, llmResponse);
        }
        fillCallbackMdc(callbackContext);
        String model = safe(llmResponse == null ? null : llmResponse.modelVersion().orElse("unknown"));
        long elapsedMillis = stopTimerMillis(callbackKey(callbackContext, "model"));
        Timer.builder(MetricsConstants.AGENT_MODEL_DURATION_SECONDS)
                .tags(modelTags(callbackContext, model, "success"))
                .publishPercentileHistogram()
                .minimumExpectedValue(MetricsConstants.METRIC_MIN_DURATION)
                .maximumExpectedValue(MetricsConstants.METRIC_MAX_DURATION)
                .register(meterRegistry)
                .record(Math.max(elapsedMillis, 0L), TimeUnit.MILLISECONDS);
        try {
            log.info("Model completed: {}, elapsed={} ms", model, elapsedMillis);
            return super.afterModelCallback(callbackContext, llmResponse);
        } finally {
            clearMdc();
        }
    }

    @Override
    public Maybe<LlmResponse> onModelErrorCallback(CallbackContext callbackContext, LlmRequest.Builder requestBuilder, Throwable throwable) {
        if (!properties.isEnabled()) {
            return super.onModelErrorCallback(callbackContext, requestBuilder, throwable);
        }
        if (!properties.isModelMetricsEnabled()) {
            failedInvocations.add(callbackContext.invocationId());
            return super.onModelErrorCallback(callbackContext, requestBuilder, throwable);
        }
        fillCallbackMdc(callbackContext);
        String model = safe(requestBuilder.build().model().orElse("unknown"));
        long elapsedMillis = stopTimerMillis(callbackKey(callbackContext, "model"));
        failedInvocations.add(callbackContext.invocationId());
        Counter.builder(MetricsConstants.AGENT_MODEL_FAILURES_TOTAL)
                .tags(modelTags(callbackContext, model, "failure"))
                .register(meterRegistry)
                .increment();
        Timer.builder(MetricsConstants.AGENT_MODEL_DURATION_SECONDS)
                .tags(modelTags(callbackContext, model, "failure"))
                .publishPercentileHistogram()
                .minimumExpectedValue(MetricsConstants.METRIC_MIN_DURATION)
                .maximumExpectedValue(MetricsConstants.METRIC_MAX_DURATION)
                .register(meterRegistry)
                .record(Math.max(elapsedMillis, 0L), TimeUnit.MILLISECONDS);
        try {
            log.error("Model failed: {}, elapsed={} ms", model, elapsedMillis, throwable);
            return super.onModelErrorCallback(callbackContext, requestBuilder, throwable);
        } finally {
            clearMdc();
        }
    }

    @Override
    public Maybe<Map<String, Object>> beforeToolCallback(BaseTool tool, Map<String, Object> toolArgs, ToolContext toolContext) {
        if (!properties.isEnabled()) {
            return super.beforeToolCallback(tool, toolArgs, toolContext);
        }
        if (!properties.isToolMetricsEnabled()) {
            return super.beforeToolCallback(tool, toolArgs, toolContext);
        }
        fillToolMdc(tool, toolContext);
        startTimer(toolKey(toolContext, tool));
        Counter.builder(MetricsConstants.AGENT_TOOL_CALLS_TOTAL)
                .tags(toolTags(toolContext, tool, "started"))
                .register(meterRegistry)
                .increment();
        try {
            log.info("Tool started: {}, argsKeys={}", tool == null ? "" : tool.name(), toolArgs == null ? "[]" : toolArgs.keySet());
            return super.beforeToolCallback(tool, toolArgs, toolContext);
        } finally {
            clearMdc();
        }
    }

    @Override
    public Maybe<Map<String, Object>> afterToolCallback(BaseTool tool, Map<String, Object> toolArgs, ToolContext toolContext, Map<String, Object> result) {
        if (!properties.isEnabled()) {
            return super.afterToolCallback(tool, toolArgs, toolContext, result);
        }
        if (!properties.isToolMetricsEnabled()) {
            return super.afterToolCallback(tool, toolArgs, toolContext, result);
        }
        fillToolMdc(tool, toolContext);
        long elapsedMillis = stopTimerMillis(toolKey(toolContext, tool));
        Timer.builder(MetricsConstants.AGENT_TOOL_DURATION_SECONDS)
                .tags(toolTags(toolContext, tool, "success"))
                .publishPercentileHistogram()
                .minimumExpectedValue(MetricsConstants.METRIC_MIN_DURATION)
                .maximumExpectedValue(MetricsConstants.METRIC_MAX_DURATION)
                .register(meterRegistry)
                .record(Math.max(elapsedMillis, 0L), TimeUnit.MILLISECONDS);
        try {
            log.info("Tool completed: {}, elapsed={} ms, resultKeys={}", tool == null ? "" : tool.name(), elapsedMillis, result == null ? "[]" : result.keySet());
            return super.afterToolCallback(tool, toolArgs, toolContext, result);
        } finally {
            clearMdc();
        }
    }

    @Override
    public Maybe<Map<String, Object>> onToolErrorCallback(BaseTool tool, Map<String, Object> toolArgs, ToolContext toolContext, Throwable throwable) {
        if (!properties.isEnabled()) {
            return super.onToolErrorCallback(tool, toolArgs, toolContext, throwable);
        }
        if (!properties.isToolMetricsEnabled()) {
            failedInvocations.add(toolContext.invocationId());
            return super.onToolErrorCallback(tool, toolArgs, toolContext, throwable);
        }
        fillToolMdc(tool, toolContext);
        long elapsedMillis = stopTimerMillis(toolKey(toolContext, tool));
        failedInvocations.add(toolContext.invocationId());
        Counter.builder(MetricsConstants.AGENT_TOOL_FAILURES_TOTAL)
                .tags(toolTags(toolContext, tool, "failure"))
                .register(meterRegistry)
                .increment();
        Timer.builder(MetricsConstants.AGENT_TOOL_DURATION_SECONDS)
                .tags(toolTags(toolContext, tool, "failure"))
                .publishPercentileHistogram()
                .minimumExpectedValue(MetricsConstants.METRIC_MIN_DURATION)
                .maximumExpectedValue(MetricsConstants.METRIC_MAX_DURATION)
                .register(meterRegistry)
                .record(Math.max(elapsedMillis, 0L), TimeUnit.MILLISECONDS);
        try {
            log.error("Tool failed: {}, elapsed={} ms", tool == null ? "" : tool.name(), elapsedMillis, throwable);
            return super.onToolErrorCallback(tool, toolArgs, toolContext, throwable);
        } finally {
            clearMdc();
        }
    }
}

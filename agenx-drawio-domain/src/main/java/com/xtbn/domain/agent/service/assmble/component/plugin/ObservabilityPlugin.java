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
        Counter.builder("agent_requests_total")
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
        Timer.builder("agent_run_duration_seconds")
                .tags(resultTags(invocationContext, failed ? "failure" : "success").and("phase", "run"))
                .register(meterRegistry)
                .record(Math.max(elapsedMillis, 0L), TimeUnit.MILLISECONDS);
        if (failed) {
            Counter.builder("agent_request_failures_total")
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
        fillCallbackMdc(callbackContext);
        long elapsedMillis = stopTimerMillis(callbackKey(callbackContext, "agent"));
        Timer.builder("agent_run_duration_seconds")
                .tags(commonTags(callbackContext).and("result", "success").and("phase", "agent"))
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
        fillCallbackMdc(callbackContext);
        String model = safe(requestBuilder.build().model().orElse("unknown"));
        startTimer(callbackKey(callbackContext, "model"));
        Counter.builder("agent_model_calls_total")
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
        fillCallbackMdc(callbackContext);
        String model = safe(llmResponse == null ? null : llmResponse.modelVersion().orElse("unknown"));
        long elapsedMillis = stopTimerMillis(callbackKey(callbackContext, "model"));
        Timer.builder("agent_model_duration_seconds")
                .tags(modelTags(callbackContext, model, "success"))
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
        fillCallbackMdc(callbackContext);
        String model = safe(requestBuilder.build().model().orElse("unknown"));
        long elapsedMillis = stopTimerMillis(callbackKey(callbackContext, "model"));
        failedInvocations.add(callbackContext.invocationId());
        Counter.builder("agent_model_failures_total")
                .tags(modelTags(callbackContext, model, "failure"))
                .register(meterRegistry)
                .increment();
        Timer.builder("agent_model_duration_seconds")
                .tags(modelTags(callbackContext, model, "failure"))
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
        fillToolMdc(tool, toolContext);
        startTimer(toolKey(toolContext, tool));
        Counter.builder("agent_tool_calls_total")
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
        fillToolMdc(tool, toolContext);
        long elapsedMillis = stopTimerMillis(toolKey(toolContext, tool));
        Timer.builder("agent_tool_duration_seconds")
                .tags(toolTags(toolContext, tool, "success"))
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
        fillToolMdc(tool, toolContext);
        long elapsedMillis = stopTimerMillis(toolKey(toolContext, tool));
        failedInvocations.add(toolContext.invocationId());
        Counter.builder("agent_tool_failures_total")
                .tags(toolTags(toolContext, tool, "failure"))
                .register(meterRegistry)
                .increment();
        Timer.builder("agent_tool_duration_seconds")
                .tags(toolTags(toolContext, tool, "failure"))
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

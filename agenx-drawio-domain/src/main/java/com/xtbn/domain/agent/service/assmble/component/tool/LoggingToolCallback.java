package com.xtbn.domain.agent.service.assmble.component.tool;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

import java.util.concurrent.TimeUnit;

@Slf4j
public class LoggingToolCallback implements ToolCallback {

    private static final int PREVIEW_LIMIT = 240;

    private final ToolCallback delegate;
    private final String sourceType;
    private final String sourceName;
    private final String toolName;
    private final MeterRegistry meterRegistry;

    public LoggingToolCallback(ToolCallback delegate, String sourceType, String sourceName, MeterRegistry meterRegistry) {
        this.delegate = delegate;
        this.sourceType = sourceType;
        this.sourceName = sourceName;
        this.toolName = resolveToolName(delegate);
        this.meterRegistry = meterRegistry;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return delegate.getToolMetadata();
    }

    @Override
    public String call(String toolInput) {
        long startNanos = System.nanoTime();
        markStarted();
        log.info("SpringAI Tool started: sourceType={}, sourceName={}, tool={}, inputLength={}, inputPreview={}",
                sourceType, sourceName, toolName, length(toolInput), preview(toolInput));
        try {
            String result = delegate.call(toolInput);
            recordDuration(startNanos, "success");
            log.info("SpringAI Tool completed: sourceType={}, sourceName={}, tool={}, elapsedMs={}, resultLength={}, resultPreview={}",
                    sourceType, sourceName, toolName, elapsedMillis(startNanos), length(result), preview(result));
            return result;
        } catch (Throwable throwable) {
            markFailed();
            recordDuration(startNanos, "failure");
            log.error("SpringAI Tool failed: sourceType={}, sourceName={}, tool={}, elapsedMs={}, inputLength={}",
                    sourceType, sourceName, toolName, elapsedMillis(startNanos), length(toolInput), throwable);
            throw throwable;
        }
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        long startNanos = System.nanoTime();
        markStarted();
        log.info("SpringAI Tool started: sourceType={}, sourceName={}, tool={}, inputLength={}, inputPreview={}, contextPresent={}",
                sourceType, sourceName, toolName, length(toolInput), preview(toolInput), toolContext != null);
        try {
            String result = delegate.call(toolInput, toolContext);
            recordDuration(startNanos, "success");
            log.info("SpringAI Tool completed: sourceType={}, sourceName={}, tool={}, elapsedMs={}, resultLength={}, resultPreview={}",
                    sourceType, sourceName, toolName, elapsedMillis(startNanos), length(result), preview(result));
            return result;
        } catch (Throwable throwable) {
            markFailed();
            recordDuration(startNanos, "failure");
            log.error("SpringAI Tool failed: sourceType={}, sourceName={}, tool={}, elapsedMs={}, inputLength={}",
                    sourceType, sourceName, toolName, elapsedMillis(startNanos), length(toolInput), throwable);
            throw throwable;
        }
    }

    private void markStarted() {
        if (meterRegistry == null) {
            return;
        }
        Counter.builder("agent_tool_calls_total")
                .tag("tool", toolName)
                .tag("sourceType", sourceType)
                .tag("sourceName", sourceName)
                .tag("result", "started")
                .register(meterRegistry)
                .increment();
    }

    private void markFailed() {
        if (meterRegistry == null) {
            return;
        }
        Counter.builder("agent_tool_failures_total")
                .tag("tool", toolName)
                .tag("sourceType", sourceType)
                .tag("sourceName", sourceName)
                .register(meterRegistry)
                .increment();
    }

    private void recordDuration(long startNanos, String result) {
        if (meterRegistry == null) {
            return;
        }
        Timer.builder("agent_tool_duration_seconds")
                .tag("tool", toolName)
                .tag("sourceType", sourceType)
                .tag("sourceName", sourceName)
                .tag("result", result)
                .register(meterRegistry)
                .record(Math.max(elapsedMillis(startNanos), 0L), TimeUnit.MILLISECONDS);
    }

    private static String resolveToolName(ToolCallback toolCallback) {
        try {
            ToolDefinition toolDefinition = toolCallback.getToolDefinition();
            if (toolDefinition != null && toolDefinition.name() != null) {
                return toolDefinition.name();
            }
        } catch (Exception e) {
            log.warn("Failed to resolve tool definition name for callback {}", toolCallback.getClass().getName(), e);
        }
        return toolCallback.getClass().getSimpleName();
    }

    private static int length(String value) {
        return value == null ? 0 : value.length();
    }

    private static long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }

    private static String preview(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.replace('\n', ' ').replace('\r', ' ');
        if (normalized.length() <= PREVIEW_LIMIT) {
            return normalized;
        }
        return normalized.substring(0, PREVIEW_LIMIT) + "...";
    }
}

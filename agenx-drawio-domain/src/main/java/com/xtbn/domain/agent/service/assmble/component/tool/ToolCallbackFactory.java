package com.xtbn.domain.agent.service.assmble.component.tool;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;

import java.util.Arrays;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ToolCallbackFactory {

    public static ToolCallback[] wrapWithLogging(String sourceType, String sourceName, ToolCallback[] toolCallbacks) {
        if (toolCallbacks == null || toolCallbacks.length == 0) {
            log.info("No SpringAI tool callbacks registered: sourceType={}, sourceName={}", sourceType, sourceName);
            return new ToolCallback[0];
        }

        ToolCallback[] wrappedCallbacks = Arrays.stream(toolCallbacks)
                .map(toolCallback -> {
                    String toolName = resolveToolName(toolCallback);
                    log.info("Registered SpringAI tool callback: sourceType={}, sourceName={}, tool={}, callbackClass={}",
                            sourceType, sourceName, toolName, toolCallback.getClass().getName());
                    return (ToolCallback) new LoggingToolCallback(toolCallback, sourceType, sourceName);
                })
                .toArray(ToolCallback[]::new);

        log.info("Registered SpringAI tool callbacks: sourceType={}, sourceName={}, count={}",
                sourceType, sourceName, wrappedCallbacks.length);
        return wrappedCallbacks;
    }

    private static String resolveToolName(ToolCallback toolCallback) {
        try {
            if (toolCallback.getToolDefinition() != null && toolCallback.getToolDefinition().name() != null) {
                return toolCallback.getToolDefinition().name();
            }
        } catch (Exception e) {
            log.warn("Failed to resolve SpringAI tool callback name for {}", toolCallback.getClass().getName(), e);
        }
        return toolCallback.getClass().getSimpleName();
    }
}

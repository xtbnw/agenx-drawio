package com.xtbn.domain.agent.service.assmble.component.plugin.support;

import com.google.adk.agents.CallbackContext;
import com.google.adk.agents.InvocationContext;
import com.google.adk.plugins.BasePlugin;
import com.google.adk.tools.BaseTool;
import com.google.adk.tools.ToolContext;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.slf4j.MDC;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public abstract class AbstractAgentPluginSupport extends BasePlugin {
    protected static final String MDC_TRACE_ID = "traceId";
    protected static final String MDC_USER_ID = "userId";
    protected static final String MDC_SESSION_ID = "sessionId";
    protected static final String MDC_AGENT_NAME = "agentName";
    protected static final String MDC_EVENT_ID = "eventId";
    protected static final String MDC_TOOL_NAME = "toolName";

    private final ConcurrentMap<String, Long> stopwatch = new ConcurrentHashMap<>();
    protected final MeterRegistry meterRegistry;

    protected AbstractAgentPluginSupport(String name, MeterRegistry meterRegistry) {
        super(name);
        this.meterRegistry = meterRegistry;
    }

    protected void fillInvocationMdc(InvocationContext invocationContext) {
        if (invocationContext == null) {
            return;
        }
        putMdc(MDC_TRACE_ID, invocationContext.invocationId());
        putMdc(MDC_USER_ID, invocationContext.userId());
        putMdc(MDC_SESSION_ID, invocationContext.session() == null ? null : invocationContext.session().id());
        putMdc(MDC_AGENT_NAME, invocationContext.agent() == null ? null : invocationContext.agent().name());
        MDC.remove(MDC_EVENT_ID);
        MDC.remove(MDC_TOOL_NAME);
    }

    protected void fillCallbackMdc(CallbackContext callbackContext) {
        if (callbackContext == null) {
            return;
        }
        fillInvocationMdc(callbackContext.invocationContext());
        putMdc(MDC_EVENT_ID, callbackContext.eventId());
    }

    protected void fillToolMdc(BaseTool tool, ToolContext toolContext) {
        fillCallbackMdc(toolContext);
        putMdc(MDC_TOOL_NAME, tool == null ? null : tool.name());
    }

    protected void clearMdc() {
        MDC.remove(MDC_TRACE_ID);
        MDC.remove(MDC_USER_ID);
        MDC.remove(MDC_SESSION_ID);
        MDC.remove(MDC_AGENT_NAME);
        MDC.remove(MDC_EVENT_ID);
        MDC.remove(MDC_TOOL_NAME);
    }

    protected void startTimer(String key) {
        if (key != null) {
            stopwatch.put(key, System.nanoTime());
        }
    }

    protected long stopTimerMillis(String key) {
        if (key == null) {
            return -1L;
        }
        Long start = stopwatch.remove(key);
        if (start == null) {
            return -1L;
        }
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
    }

    protected String invocationKey(InvocationContext invocationContext) {
        return invocationContext == null ? null : invocationContext.invocationId();
    }

    protected String callbackKey(CallbackContext callbackContext, String suffix) {
        if (callbackContext == null) {
            return null;
        }
        return callbackContext.invocationId() + ":" + callbackContext.eventId() + ":" + suffix;
    }

    protected String toolKey(ToolContext toolContext, BaseTool tool) {
        return callbackKey(toolContext, tool == null ? "tool" : safe(tool.name()));
    }

    protected String safe(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }

    protected Tags commonTags(InvocationContext invocationContext) {
        return Tags.of(
                "app", safe(invocationContext == null ? null : invocationContext.appName()),
                "agent", safe(invocationContext == null ? null : invocationContext.agent() == null ? null : invocationContext.agent().name())
        );
    }

    protected Tags commonTags(CallbackContext callbackContext) {
        return Tags.of(
                "app", safe(callbackContext == null ? null : callbackContext.invocationContext().appName()),
                "agent", safe(callbackContext == null ? null : callbackContext.agentName())
        );
    }

    protected Tags resultTags(InvocationContext invocationContext, String result) {
        return commonTags(invocationContext).and("result", safe(result));
    }

    protected Tags modelTags(CallbackContext callbackContext, String model, String result) {
        return commonTags(callbackContext)
                .and("model", safe(model))
                .and("result", safe(result));
    }

    protected Tags toolTags(ToolContext toolContext, BaseTool tool, String result) {
        return commonTags(toolContext)
                .and("tool", safe(tool == null ? null : tool.name()))
                .and("result", safe(result));
    }

    private void putMdc(String key, String value) {
        if (value == null || value.isBlank()) {
            MDC.remove(key);
            return;
        }
        MDC.put(key, value);
    }
}

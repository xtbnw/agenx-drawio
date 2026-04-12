package com.xtbn.domain.agent.service.assmble.component.plugin;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.CallbackContext;
import com.google.adk.agents.InvocationContext;
import com.google.adk.plugins.BasePlugin;
import com.google.adk.tools.BaseTool;
import com.google.adk.tools.ToolContext;
import com.google.genai.types.Content;
import com.xtbn.types.common.RequestTraceConstants;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service("myLogPlugin")
public class MyLogPlugin extends BasePlugin {
    private static final String MDC_TRACE_ID = "traceId";
    private static final String MDC_USER_ID = "userId";
    private static final String MDC_SESSION_ID = "sessionId";
    private static final String MDC_AGENT_NAME = "agentName";
    private static final String MDC_EVENT_ID = "eventId";
    private static final String MDC_TOOL_NAME = "toolName";

    private final ConcurrentMap<String, Long> invocationStartTimes = new ConcurrentHashMap<>();

    public MyLogPlugin(String name) {
        super(name);
    }

    public MyLogPlugin() {
        super("MyLogPlugin");
    }

    @Override
    public Maybe<Content> onUserMessageCallback(InvocationContext invocationContext, Content userMessage) {
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
        fillInvocationMdc(invocationContext);
        invocationStartTimes.put(invocationContext.invocationId(), System.nanoTime());
        try {
            log.info("Invocation started");
            return super.beforeRunCallback(invocationContext);
        } finally {
            clearMdc();
        }
    }

    @Override
    public Maybe<Content> beforeAgentCallback(BaseAgent agent, CallbackContext callbackContext) {
        fillCallbackMdc(callbackContext);
        try {
            log.info("Agent started: {}", agent == null ? "" : agent.name());
            return super.beforeAgentCallback(agent, callbackContext);
        } finally {
            clearMdc();
        }
    }

    @Override
    public Maybe<Map<String, Object>> beforeToolCallback(
            BaseTool tool, Map<String, Object> toolArgs, ToolContext toolContext) {
        fillToolMdc(tool, toolContext);
        try {
            log.info("Tool started: {}, args={}", tool == null ? "" : tool.name(), safeToolArgs(toolArgs));
            return super.beforeToolCallback(tool, toolArgs, toolContext);
        } finally {
            clearMdc();
        }
    }

    @Override
    public Maybe<Map<String, Object>> afterToolCallback(
            BaseTool tool,
            Map<String, Object> toolArgs,
            ToolContext toolContext,
            Map<String, Object> result) {
        fillToolMdc(tool, toolContext);
        try {
            log.info("Tool completed: {}, resultKeys={}", tool == null ? "" : tool.name(), safeMapKeys(result));
            return super.afterToolCallback(tool, toolArgs, toolContext, result);
        } finally {
            clearMdc();
        }
    }

    @Override
    public Maybe<Content> afterAgentCallback(BaseAgent agent, CallbackContext callbackContext) {
        fillCallbackMdc(callbackContext);
        try {
            log.info("Agent completed: {}", agent == null ? "" : agent.name());
            return super.afterAgentCallback(agent, callbackContext);
        } finally {
            clearMdc();
        }
    }

    @Override
    public Completable afterRunCallback(InvocationContext invocationContext) {
        fillInvocationMdc(invocationContext);
        try {
            log.info("Invocation completed in {} ms", elapsedMillis(invocationContext.invocationId()));
            return super.afterRunCallback(invocationContext);
        } finally {
            invocationStartTimes.remove(invocationContext.invocationId());
            clearMdc();
        }
    }

    private void fillInvocationMdc(InvocationContext invocationContext) {
        if (invocationContext == null) {
            return;
        }
        putMdc(MDC_TRACE_ID, resolveRequestId(invocationContext));
        putMdc(MDC_USER_ID, invocationContext.userId());
        putMdc(MDC_AGENT_NAME, invocationContext.agent() == null ? null : invocationContext.agent().name());
        putMdc(MDC_SESSION_ID, invocationContext.session() == null ? null : invocationContext.session().id());
        MDC.remove(MDC_EVENT_ID);
        MDC.remove(MDC_TOOL_NAME);
    }

    private void fillCallbackMdc(CallbackContext callbackContext) {
        if (callbackContext == null) {
            return;
        }
        fillInvocationMdc(callbackContext.invocationContext());
        putMdc(MDC_EVENT_ID, callbackContext.eventId());
    }

    private void fillToolMdc(BaseTool tool, ToolContext toolContext) {
        fillCallbackMdc(toolContext);
        putMdc(MDC_TOOL_NAME, tool == null ? null : tool.name());
    }

    private void putMdc(String key, String value) {
        if (value == null || value.isBlank()) {
            MDC.remove(key);
            return;
        }
        MDC.put(key, value);
    }

    private String resolveRequestId(InvocationContext invocationContext) {
        if (invocationContext == null) {
            return null;
        }
        Object requestId = invocationContext.callbackContextData().get(RequestTraceConstants.CALLBACK_REQUEST_ID);
        if (requestId != null && !String.valueOf(requestId).isBlank()) {
            return String.valueOf(requestId);
        }
        return invocationContext.invocationId();
    }

    private long elapsedMillis(String invocationId) {
        Long startTime = invocationStartTimes.get(invocationId);
        if (startTime == null) {
            return -1L;
        }
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
    }

    private Map<String, Object> safeToolArgs(Map<String, Object> toolArgs) {
        if (toolArgs == null || toolArgs.isEmpty()) {
            return Map.of();
        }
        return new LinkedHashMap<>(toolArgs);
    }

    private String safeMapKeys(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return "[]";
        }
        return map.keySet().toString();
    }

    private void clearMdc() {
        MDC.remove(MDC_TRACE_ID);
        MDC.remove(MDC_USER_ID);
        MDC.remove(MDC_SESSION_ID);
        MDC.remove(MDC_AGENT_NAME);
        MDC.remove(MDC_EVENT_ID);
        MDC.remove(MDC_TOOL_NAME);
    }
}

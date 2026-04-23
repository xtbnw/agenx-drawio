package com.xtbn.domain.agent.service.assmble.component.plugin;

import com.google.adk.agents.CallbackContext;
import com.google.adk.models.LlmResponse;
import com.google.adk.runner.Runner;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import com.xtbn.domain.agent.adapter.port.registry.IBeanRegistry;
import com.xtbn.domain.agent.model.valobj.AgentRegisterVO;
import com.xtbn.domain.agent.service.assmble.component.plugin.support.AbstractAgentPluginSupport;
import com.xtbn.domain.agent.service.assmble.component.plugin.support.DrawioXmlNormalizationSupport;
import com.xtbn.domain.agent.service.assmble.component.plugin.support.DrawioXmlValidationSupport;
import com.xtbn.types.common.Constants;
import com.xtbn.types.common.DrawioXmlGuardConstants;
import com.xtbn.types.common.RequestTraceConstants;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.reactivex.rxjava3.core.Maybe;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
public abstract class AbstractDrawioGuardPlugin extends AbstractAgentPluginSupport {
    private final MeterRegistry meterRegistry;
    private final DrawioXmlNormalizationSupport normalizationSupport;
    private final IBeanRegistry beanRegistry;

    protected AbstractDrawioGuardPlugin(String name,
                                        MeterRegistry meterRegistry,
                                        DrawioXmlNormalizationSupport normalizationSupport,
                                        IBeanRegistry beanRegistry) {
        super(name, meterRegistry);
        this.meterRegistry = meterRegistry;
        this.normalizationSupport = normalizationSupport;
        this.beanRegistry = beanRegistry;
    }

    @Override
    public Maybe<LlmResponse> afterModelCallback(CallbackContext callbackContext, LlmResponse llmResponse) {
        if (!isEnabled() || shouldSkip(callbackContext) || !isGuardedAgent(callbackContext) || llmResponse == null) {
            return super.afterModelCallback(callbackContext, llmResponse);
        }

        if (Boolean.TRUE.equals(llmResponse.partial().orElse(false)) || llmResponse.content().isEmpty()) {
            return super.afterModelCallback(callbackContext, llmResponse);
        }

        fillCallbackMdc(callbackContext);
        try {
            String xml = llmResponse.content().map(Content::text).orElse(null);
            String normalized = normalizationSupport.normalize(xml);
            DrawioXmlValidationSupport.ValidationResult initialResult = validate(normalized);
            if (initialResult.isValid()) {
                log.info("{} passed, mode=direct", pluginLabel());
                return Maybe.just(rewriteContent(llmResponse, normalized));
            }

            log.warn("{} failed, invocationId={}, agentName={}, code={}, message={}, action=retry",
                    pluginLabel(), callbackContext.invocationId(), callbackContext.agentName(),
                    initialResult.getErrorCode().name(), initialResult.getErrorMessage());
            incrementMetric(retryMetricName(), callbackContext);

            String originalUserRequest = callbackContext.invocationContext().userContent().map(Content::text).orElse("");
            String currentRootAgentId = String.valueOf(callbackContext.state().get(DrawioXmlGuardConstants.STATE_AGENT_ID));
            String retryPrompt = buildRetryPrompt(originalUserRequest, normalized, initialResult);
            String retryXml = executeWithTempSession(currentRootAgentId, callbackContext, retryPrompt, DrawioXmlGuardConstants.STAGE_RETRY);

            String normalizedRetryXml = normalizationSupport.normalize(retryXml);
            DrawioXmlValidationSupport.ValidationResult retryResult = validate(normalizedRetryXml);
            if (retryResult.isValid()) {
                log.info("{} passed after retry, mode=direct", pluginLabel());
                return Maybe.just(rewriteContent(llmResponse, normalizedRetryXml));
            }

            log.warn("{} failed after retry, invocationId={}, agentName={}, code={}, message={}, action=degrade",
                    pluginLabel(), callbackContext.invocationId(), callbackContext.agentName(),
                    retryResult.getErrorCode().name(), retryResult.getErrorMessage());
            incrementMetric(degradeMetricName(), callbackContext);

            String degradeXml = executeWithTempSession(degradeAgentId(), callbackContext, originalUserRequest, DrawioXmlGuardConstants.STAGE_DEGRADE);
            String normalizedDegradeXml = normalizationSupport.normalize(degradeXml);
            DrawioXmlValidationSupport.ValidationResult degradeResult = validate(normalizedDegradeXml);
            if (!degradeResult.isValid()) {
                log.error("{} degrade output still invalid, invocationId={}, code={}, message={}",
                        pluginLabel(), callbackContext.invocationId(), degradeResult.getErrorCode().name(), degradeResult.getErrorMessage());
                return super.afterModelCallback(callbackContext, llmResponse);
            }

            log.info("{} degraded successfully, mode=degrade", pluginLabel());
            return Maybe.just(rewriteContent(llmResponse, normalizedDegradeXml));
        } catch (Exception e) {
            log.error("{} failed unexpectedly, invocationId={}", pluginLabel(), callbackContext.invocationId(), e);
            return super.afterModelCallback(callbackContext, llmResponse);
        } finally {
            clearMdc();
        }
    }

    protected boolean shouldSkip(CallbackContext callbackContext) {
        Object skip = callbackContext.invocationContext().callbackContextData().get(DrawioXmlGuardConstants.CTX_SKIP_GUARD);
        return Boolean.TRUE.equals(skip);
    }

    protected LlmResponse rewriteContent(LlmResponse source, String xml) {
        return source.toBuilder().content(Content.fromParts(Part.fromText(xml))).build();
    }

    protected String executeWithTempSession(String targetAgentId, CallbackContext callbackContext, String prompt, String stage) {
        AgentRegisterVO agentRegisterVO = beanRegistry.getBean(targetAgentId, AgentRegisterVO.class);
        if (agentRegisterVO == null) {
            throw new IllegalStateException("Target agent runner not found: " + targetAgentId);
        }

        Runner runner = agentRegisterVO.getRunner();
        String userId = callbackContext.invocationContext().userId();
        String sessionId = "drawio-guard-" + UUID.randomUUID();
        Map<String, Object> state = new LinkedHashMap<>();
        state.put(DrawioXmlGuardConstants.STATE_AGENT_ID, targetAgentId);

        Map<String, Object> callbackData = new LinkedHashMap<>();
        callbackData.put(DrawioXmlGuardConstants.CTX_SKIP_GUARD, Boolean.TRUE);
        callbackData.put(DrawioXmlGuardConstants.CTX_CALL_STAGE, stage);
        String requestId = resolveRequestId(callbackContext.invocationContext());
        if (requestId != null && !requestId.isBlank()) {
            callbackData.put(RequestTraceConstants.CALLBACK_REQUEST_ID, requestId);
        }

        try {
            runner.sessionService()
                    .createSession(Constants.APP_NAME, userId, new java.util.concurrent.ConcurrentHashMap<>(state), sessionId)
                    .blockingGet();

            return collectFinalOutput(runner.runAsync(
                    userId,
                    sessionId,
                    Content.fromParts(Part.fromText(prompt)),
                    callbackContext.invocationContext().runConfig(),
                    callbackData
            ));
        } finally {
            try {
                runner.sessionService().deleteSession(Constants.APP_NAME, userId, sessionId).blockingAwait();
            } catch (Exception e) {
                log.warn("Failed to delete temporary drawio guard session, sessionId={}", sessionId, e);
            }
        }
    }

    protected String collectFinalOutput(io.reactivex.rxjava3.core.Flowable<com.google.adk.events.Event> events) {
        final String[] finalOutput = {null};
        final String[] lastNonEmptyOutput = {""};
        events.blockingForEach(event -> {
            String content = event.stringifyContent();
            if (content != null && !content.trim().isEmpty()) {
                lastNonEmptyOutput[0] = content;
                if (event.finalResponse()) {
                    finalOutput[0] = content;
                }
            }
        });
        return Optional.ofNullable(finalOutput[0]).orElse(lastNonEmptyOutput[0]);
    }

    protected void incrementMetric(String metricName, CallbackContext callbackContext) {
        Counter.builder(metricName).tags(commonTags(callbackContext)).register(meterRegistry).increment();
    }

    protected abstract boolean isEnabled();

    protected abstract boolean isGuardedAgent(CallbackContext callbackContext);

    protected abstract String degradeAgentId();

    protected abstract DrawioXmlValidationSupport.ValidationResult validate(String xml);

    protected abstract String buildRetryPrompt(String originalUserRequest, String invalidXml, DrawioXmlValidationSupport.ValidationResult result);

    protected abstract String pluginLabel();

    protected abstract String retryMetricName();

    protected abstract String degradeMetricName();
}

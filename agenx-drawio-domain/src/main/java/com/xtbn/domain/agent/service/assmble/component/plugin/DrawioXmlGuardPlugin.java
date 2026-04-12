package com.xtbn.domain.agent.service.assmble.component.plugin;

import com.google.adk.agents.CallbackContext;
import com.google.adk.models.LlmResponse;
import com.google.adk.runner.Runner;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import com.xtbn.domain.agent.adapter.port.registry.IBeanRegistry;
import com.xtbn.domain.agent.model.valobj.AgentRegisterVO;
import com.xtbn.domain.agent.model.valobj.properties.PluginDrawioXmlGuardProperties;
import com.xtbn.domain.agent.service.assmble.component.plugin.support.AbstractAgentPluginSupport;
import com.xtbn.domain.agent.service.assmble.component.plugin.support.DrawioXmlNormalizationSupport;
import com.xtbn.domain.agent.service.assmble.component.plugin.support.DrawioXmlRetryPromptSupport;
import com.xtbn.domain.agent.service.assmble.component.plugin.support.DrawioXmlValidationSupport;
import com.xtbn.types.common.Constants;
import com.xtbn.types.common.DrawioXmlGuardConstants;
import com.xtbn.types.common.MetricsConstants;
import com.xtbn.types.common.RequestTraceConstants;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.reactivex.rxjava3.core.Maybe;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service("drawioXmlGuardPlugin")
public class DrawioXmlGuardPlugin extends AbstractAgentPluginSupport {
    private final MeterRegistry meterRegistry;
    private final PluginDrawioXmlGuardProperties properties;
    private final DrawioXmlNormalizationSupport normalizationSupport;
    private final DrawioXmlValidationSupport validationSupport;
    private final DrawioXmlRetryPromptSupport retryPromptSupport;
    private final IBeanRegistry beanRegistry;

    public DrawioXmlGuardPlugin(MeterRegistry meterRegistry,
                                PluginDrawioXmlGuardProperties properties,
                                DrawioXmlNormalizationSupport normalizationSupport,
                                DrawioXmlValidationSupport validationSupport,
                                DrawioXmlRetryPromptSupport retryPromptSupport,
                                IBeanRegistry beanRegistry) {
        super("DrawioXmlGuardPlugin", meterRegistry);
        this.meterRegistry = meterRegistry;
        this.properties = properties;
        this.normalizationSupport = normalizationSupport;
        this.validationSupport = validationSupport;
        this.retryPromptSupport = retryPromptSupport;
        this.beanRegistry = beanRegistry;
    }

    @Override
    public Maybe<LlmResponse> afterModelCallback(CallbackContext callbackContext, LlmResponse llmResponse) {
        if (!properties.isEnabled() || shouldSkip(callbackContext) || !isGuardedAgent(callbackContext) || llmResponse == null) {
            return super.afterModelCallback(callbackContext, llmResponse);
        }

        if (Boolean.TRUE.equals(llmResponse.partial().orElse(false)) || !llmResponse.content().isPresent()) {
            return super.afterModelCallback(callbackContext, llmResponse);
        }

        fillCallbackMdc(callbackContext);
        try {
            String xml = llmResponse.content().map(Content::text).orElse(null);
            String normalized = normalizationSupport.normalize(xml);
            DrawioXmlValidationSupport.ValidationResult initialResult = validationSupport.validate(normalized);
            if (initialResult.isValid()) {
                log.info("Draw.io XML guard passed, mode=direct");
                return Maybe.just(rewriteContent(llmResponse, normalized));
            }

            log.warn("Draw.io XML validation failed, invocationId={}, agentName={}, code={}, message={}, action=retry",
                    callbackContext.invocationId(), callbackContext.agentName(), initialResult.getErrorCode().name(), initialResult.getErrorMessage());
            incrementRetryMetric(callbackContext);

            String originalUserRequest = callbackContext.invocationContext().userContent().map(Content::text).orElse("");
            String currentRootAgentId = String.valueOf(callbackContext.state().get(DrawioXmlGuardConstants.STATE_AGENT_ID));
            String retryPrompt = retryPromptSupport.buildRetryPrompt(originalUserRequest, normalized, initialResult);
            String retryXml = executeWithTempSession(currentRootAgentId, callbackContext, retryPrompt, DrawioXmlGuardConstants.STAGE_RETRY);

            String normalizedRetryXml = normalizationSupport.normalize(retryXml);
            DrawioXmlValidationSupport.ValidationResult retryResult = validationSupport.validate(normalizedRetryXml);
            if (retryResult.isValid()) {
                log.info("Draw.io XML guard passed after retry, mode=direct");
                return Maybe.just(rewriteContent(llmResponse, normalizedRetryXml));
            }

            log.warn("Draw.io XML validation failed after retry, invocationId={}, agentName={}, code={}, message={}, action=degrade",
                    callbackContext.invocationId(), callbackContext.agentName(), retryResult.getErrorCode().name(), retryResult.getErrorMessage());
            incrementDegradeMetric(callbackContext);

            String degradeXml = executeWithTempSession(properties.getDegradeAgentId(), callbackContext, originalUserRequest, DrawioXmlGuardConstants.STAGE_DEGRADE);
            String normalizedDegradeXml = normalizationSupport.normalize(degradeXml);
            DrawioXmlValidationSupport.ValidationResult degradeResult = validationSupport.validate(normalizedDegradeXml);
            if (!degradeResult.isValid()) {
                log.error("Draw.io degrade XML still invalid, invocationId={}, code={}, message={}",
                        callbackContext.invocationId(), degradeResult.getErrorCode().name(), degradeResult.getErrorMessage());
                return super.afterModelCallback(callbackContext, llmResponse);
            }

            log.info("Draw.io XML guard degraded successfully, mode=degrade");
            return Maybe.just(rewriteContent(llmResponse, normalizedDegradeXml));
        } catch (Exception e) {
            log.error("Draw.io XML guard failed unexpectedly, invocationId={}", callbackContext.invocationId(), e);
            return super.afterModelCallback(callbackContext, llmResponse);
        } finally {
            clearMdc();
        }
    }

    private boolean isGuardedAgent(CallbackContext callbackContext) {
        return properties.getGuardedAgentNames().contains(callbackContext.agentName());
    }

    private boolean shouldSkip(CallbackContext callbackContext) {
        Object skip = callbackContext.invocationContext().callbackContextData().get(DrawioXmlGuardConstants.CTX_SKIP_GUARD);
        return Boolean.TRUE.equals(skip);
    }

    private void incrementRetryMetric(CallbackContext callbackContext) {
        Counter.builder(MetricsConstants.DRAWIO_XML_RETRY_COUNT)
                .tags(commonTags(callbackContext))
                .register(meterRegistry)
                .increment();
    }

    private void incrementDegradeMetric(CallbackContext callbackContext) {
        Counter.builder(MetricsConstants.DRAWIO_XML_DEGRADED_TOTAL)
                .tags(commonTags(callbackContext))
                .register(meterRegistry)
                .increment();
    }

    private LlmResponse rewriteContent(LlmResponse source, String xml) {
        return source.toBuilder()
                .content(Content.fromParts(Part.fromText(xml)))
                .build();
    }

    private String executeWithTempSession(String targetAgentId, CallbackContext callbackContext, String prompt, String stage) {
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

    private String collectFinalOutput(io.reactivex.rxjava3.core.Flowable<com.google.adk.events.Event> events) {
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
}

package com.xtbn.domain.agent.service.assmble.component.plugin;

import com.google.adk.agents.CallbackContext;
import com.google.adk.models.LlmRequest;
import com.google.adk.models.LlmResponse;
import com.google.genai.types.GenerateContentResponseUsageMetadata;
import com.xtbn.domain.agent.model.valobj.properties.PluginCostProperties;
import com.xtbn.domain.agent.service.assmble.component.plugin.support.AbstractAgentPluginSupport;
import io.micrometer.core.instrument.MeterRegistry;
import io.reactivex.rxjava3.core.Maybe;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Service("costAccountingPlugin")
public class CostAccountingPlugin extends AbstractAgentPluginSupport {
    private final PluginCostProperties properties;
    private final ConcurrentMap<String, String> requestModels = new ConcurrentHashMap<>();

    public CostAccountingPlugin(MeterRegistry meterRegistry, PluginCostProperties properties) {
        super("CostAccountingPlugin", meterRegistry);
        this.properties = properties;
    }

    @Override
    public Maybe<LlmResponse> beforeModelCallback(CallbackContext callbackContext, LlmRequest.Builder requestBuilder) {
        if (!properties.isEnabled()) {
            return super.beforeModelCallback(callbackContext, requestBuilder);
        }
        requestModels.put(callbackKey(callbackContext, "model"), safe(requestBuilder.build().model().orElse("unknown")));
        return super.beforeModelCallback(callbackContext, requestBuilder);
    }

    @Override
    public Maybe<LlmResponse> afterModelCallback(CallbackContext callbackContext, LlmResponse llmResponse) {
        if (!properties.isEnabled()) {
            return super.afterModelCallback(callbackContext, llmResponse);
        }
        String key = callbackKey(callbackContext, "model");
        String model = safe(llmResponse == null ? null : llmResponse.modelVersion().orElse(requestModels.get(key)));
        GenerateContentResponseUsageMetadata usageMetadata = llmResponse == null ? null : llmResponse.usageMetadata().orElse(null);
        if (usageMetadata == null) {
            requestModels.remove(key);
            log.info("Model usage unavailable, model={}", model);
            return super.afterModelCallback(callbackContext, llmResponse);
        }

        int inputTokens = usageMetadata.promptTokenCount().orElse(0);
        int outputTokens = usageMetadata.candidatesTokenCount().orElse(0);
        int totalTokens = usageMetadata.totalTokenCount().orElse(inputTokens + outputTokens);

        BigDecimal estimatedCost = estimateCost(model, inputTokens, outputTokens);

        requestModels.remove(key);
        log.info("Model usage accounted, model={}, inputTokens={}, outputTokens={}, totalTokens={}, estimatedCost={}",
                model, inputTokens, outputTokens, totalTokens, estimatedCost);
        return super.afterModelCallback(callbackContext, llmResponse);
    }

    @Override
    public Maybe<LlmResponse> onModelErrorCallback(CallbackContext callbackContext, LlmRequest.Builder requestBuilder, Throwable throwable) {
        requestModels.remove(callbackKey(callbackContext, "model"));
        return super.onModelErrorCallback(callbackContext, requestBuilder, throwable);
    }

    private BigDecimal estimateCost(String model, int inputTokens, int outputTokens) {
        PluginCostProperties.ModelPricing pricing = properties.getModelPricing().get(model);
        if (pricing == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal inputCost = pricing.getInputPer1k()
                .multiply(BigDecimal.valueOf(inputTokens))
                .divide(BigDecimal.valueOf(1000L), 8, RoundingMode.HALF_UP);
        BigDecimal outputCost = pricing.getOutputPer1k()
                .multiply(BigDecimal.valueOf(outputTokens))
                .divide(BigDecimal.valueOf(1000L), 8, RoundingMode.HALF_UP);
        return inputCost.add(outputCost);
    }
}

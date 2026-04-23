package com.xtbn.domain.agent.service.assmble.component.plugin;

import com.google.adk.agents.CallbackContext;
import com.xtbn.domain.agent.adapter.port.registry.IBeanRegistry;
import com.xtbn.domain.agent.model.valobj.properties.PluginDrawioLayoutGuardProperties;
import com.xtbn.domain.agent.service.assmble.component.plugin.support.DrawioLayoutValidationSupport;
import com.xtbn.domain.agent.service.assmble.component.plugin.support.DrawioXmlNormalizationSupport;
import com.xtbn.domain.agent.service.assmble.component.plugin.support.DrawioXmlRetryPromptSupport;
import com.xtbn.domain.agent.service.assmble.component.plugin.support.DrawioXmlValidationSupport;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service("drawioLayoutGuardPlugin")
public class DrawioLayoutGuardPlugin extends AbstractDrawioGuardPlugin {
    private static final String DRAWIO_LAYOUT_RETRY_COUNT = "drawio.layout.retry.count";
    private static final String DRAWIO_LAYOUT_DEGRADED_TOTAL = "drawio.layout.degraded.total";

    private final PluginDrawioLayoutGuardProperties properties;
    private final DrawioLayoutValidationSupport validationSupport;
    private final DrawioXmlRetryPromptSupport retryPromptSupport;

    public DrawioLayoutGuardPlugin(MeterRegistry meterRegistry,
                                   PluginDrawioLayoutGuardProperties properties,
                                   DrawioXmlNormalizationSupport normalizationSupport,
                                   DrawioLayoutValidationSupport validationSupport,
                                   DrawioXmlRetryPromptSupport retryPromptSupport,
                                   IBeanRegistry beanRegistry) {
        super("DrawioLayoutGuardPlugin", meterRegistry, normalizationSupport, beanRegistry);
        this.properties = properties;
        this.validationSupport = validationSupport;
        this.retryPromptSupport = retryPromptSupport;
    }

    @Override
    protected boolean isEnabled() {
        return properties.isEnabled();
    }

    @Override
    protected boolean isGuardedAgent(CallbackContext callbackContext) {
        return properties.getGuardedAgentNames().contains(callbackContext.agentName());
    }

    @Override
    protected String degradeAgentId() {
        return properties.getDegradeAgentId();
    }

    @Override
    protected DrawioXmlValidationSupport.ValidationResult validate(String xml) {
        return validationSupport.validate(xml);
    }

    @Override
    protected String buildRetryPrompt(String originalUserRequest, String invalidXml, DrawioXmlValidationSupport.ValidationResult result) {
        return retryPromptSupport.buildLayoutRetryPrompt(originalUserRequest, invalidXml, result);
    }

    @Override
    protected String pluginLabel() {
        return "Draw.io layout guard";
    }

    @Override
    protected String retryMetricName() {
        return DRAWIO_LAYOUT_RETRY_COUNT;
    }

    @Override
    protected String degradeMetricName() {
        return DRAWIO_LAYOUT_DEGRADED_TOTAL;
    }
}

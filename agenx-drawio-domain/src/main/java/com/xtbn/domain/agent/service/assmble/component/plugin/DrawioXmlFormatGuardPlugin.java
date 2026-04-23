package com.xtbn.domain.agent.service.assmble.component.plugin;

import com.google.adk.agents.CallbackContext;
import com.xtbn.domain.agent.adapter.port.registry.IBeanRegistry;
import com.xtbn.domain.agent.model.valobj.properties.PluginDrawioXmlFormatGuardProperties;
import com.xtbn.domain.agent.service.assmble.component.plugin.support.DrawioXmlNormalizationSupport;
import com.xtbn.domain.agent.service.assmble.component.plugin.support.DrawioXmlRetryPromptSupport;
import com.xtbn.domain.agent.service.assmble.component.plugin.support.DrawioXmlValidationSupport;
import com.xtbn.types.common.MetricsConstants;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service("drawioXmlFormatGuardPlugin")
public class DrawioXmlFormatGuardPlugin extends AbstractDrawioGuardPlugin {
    private final PluginDrawioXmlFormatGuardProperties properties;
    private final DrawioXmlValidationSupport validationSupport;
    private final DrawioXmlRetryPromptSupport retryPromptSupport;

    public DrawioXmlFormatGuardPlugin(MeterRegistry meterRegistry,
                                      PluginDrawioXmlFormatGuardProperties properties,
                                      DrawioXmlNormalizationSupport normalizationSupport,
                                      DrawioXmlValidationSupport validationSupport,
                                      DrawioXmlRetryPromptSupport retryPromptSupport,
                                      IBeanRegistry beanRegistry) {
        super("DrawioXmlFormatGuardPlugin", meterRegistry, normalizationSupport, beanRegistry);
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
        return retryPromptSupport.buildFormatRetryPrompt(originalUserRequest, invalidXml, result);
    }

    @Override
    protected String pluginLabel() {
        return "Draw.io XML format guard";
    }

    @Override
    protected String retryMetricName() {
        return MetricsConstants.DRAWIO_XML_RETRY_COUNT;
    }

    @Override
    protected String degradeMetricName() {
        return MetricsConstants.DRAWIO_XML_DEGRADED_TOTAL;
    }
}

package com.xtbn.domain.agent.service.assmble.component.plugin;

import com.google.adk.agents.InvocationContext;
import com.google.genai.types.Content;
import com.xtbn.domain.agent.adapter.port.safety.IPrivacySanitizer;
import com.xtbn.domain.agent.model.valobj.properties.PluginPrivacyProperties;
import com.xtbn.domain.agent.service.assmble.component.plugin.support.AbstractAgentPluginSupport;
import io.micrometer.core.instrument.MeterRegistry;
import io.reactivex.rxjava3.core.Maybe;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service("privacyPlugin")
public class PrivacyPlugin extends AbstractAgentPluginSupport {
    private final PluginPrivacyProperties properties;
    private final IPrivacySanitizer privacySanitizer;

    public PrivacyPlugin(MeterRegistry meterRegistry, PluginPrivacyProperties properties, IPrivacySanitizer privacySanitizer) {
        super("PrivacyPlugin", meterRegistry);
        this.properties = properties;
        this.privacySanitizer = privacySanitizer;
    }

    @Override
    public Maybe<Content> onUserMessageCallback(InvocationContext invocationContext, Content userMessage) {
        if (!properties.isEnabled() || userMessage == null) {
            return super.onUserMessageCallback(invocationContext, userMessage);
        }
        IPrivacySanitizer.SanitizationResult result = privacySanitizer.sanitizeContent(userMessage, properties);
        if (result.matches() <= 0) {
            return super.onUserMessageCallback(invocationContext, userMessage);
        }
        log.info("Sensitive data sanitized, matches={}", result.matches());
        return Maybe.just(result.content());
    }
}

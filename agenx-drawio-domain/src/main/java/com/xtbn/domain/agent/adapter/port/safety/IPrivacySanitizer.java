package com.xtbn.domain.agent.adapter.port.safety;

import com.google.genai.types.Content;
import com.xtbn.domain.agent.model.valobj.properties.PluginPrivacyProperties;

public interface IPrivacySanitizer {

    SanitizationResult sanitizeContent(Content content, PluginPrivacyProperties properties);

    SanitizedText sanitizeText(String text, PluginPrivacyProperties properties);

    record SanitizedText(String text, int matches) {
    }

    record SanitizationResult(Content content, int matches) {
    }
}

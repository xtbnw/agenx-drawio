package com.xtbn.infrastructure.safety;

import com.xtbn.domain.agent.model.valobj.properties.PluginSensitiveWordProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

@Component
public class SensitiveWordDictionaryProvider {
    private static final Logger LOGGER = Logger.getLogger(SensitiveWordDictionaryProvider.class.getName());
    private final PluginSensitiveWordProperties properties;

    public SensitiveWordDictionaryProvider(PluginSensitiveWordProperties properties) {
        this.properties = properties;
    }

    public DictionarySnapshot load() {
        List<String> words = sanitize(properties.getWordList());
        List<String> whitelist = sanitize(properties.getWhitelist());
        if (words.isEmpty()) {
            LOGGER.warning("Sensitive word dictionary is empty.");
        }
        return new DictionarySnapshot(words, whitelist);
    }

    private List<String> sanitize(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .distinct()
                .toList();
    }

    public record DictionarySnapshot(List<String> words, List<String> whitelist) {
    }
}

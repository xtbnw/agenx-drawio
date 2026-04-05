package com.xtbn.domain.agent.service.assmble.component.plugin.support;

import com.google.genai.types.Content;
import com.google.genai.types.Part;
import com.xtbn.domain.agent.model.valobj.properties.PluginPrivacyProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SensitiveDataSanitizer {
    private static final Pattern PHONE_PATTERN = Pattern.compile("(?<!\\d)(1\\d{10})(?!\\d)");
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("(?<![0-9A-Za-z])(\\d{17}[\\dXx]|\\d{15})(?![0-9A-Za-z])");
    private static final Pattern BEARER_PATTERN = Pattern.compile("(?i)(Bearer\\s+)([A-Za-z0-9._\\-+/=]+)");
    private static final Pattern TOKEN_PATTERN = Pattern.compile("(?i)\\b(access[_-]?token|refresh[_-]?token|api[_-]?key|authorization)\\b\\s*[:=]\\s*([A-Za-z0-9._\\-+/=]+)");

    public SanitizationResult sanitizeContent(Content content, PluginPrivacyProperties properties) {
        if (content == null || content.parts().isEmpty()) {
            return new SanitizationResult(content, 0);
        }

        List<Part> sanitizedParts = new ArrayList<>();
        int matches = 0;
        for (Part part : content.parts().orElse(List.of())) {
            if (part == null || part.text().isEmpty()) {
                sanitizedParts.add(part);
                continue;
            }
            SanitizedText sanitizedText = sanitizeText(part.text().orElse(""), properties);
            matches += sanitizedText.matches();
            sanitizedParts.add(part.toBuilder().text(sanitizedText.text()).build());
        }

        if (matches == 0) {
            return new SanitizationResult(content, 0);
        }

        return new SanitizationResult(content.toBuilder().parts(sanitizedParts).build(), matches);
    }

    public SanitizedText sanitizeText(String text, PluginPrivacyProperties properties) {
        if (text == null || text.isBlank() || properties == null || !properties.isEnabled()) {
            return new SanitizedText(text, 0);
        }

        String value = text;
        int matches = 0;
        if (properties.isMaskPhone()) {
            ReplacementResult result = replace(value, PHONE_PATTERN, properties.getReplaceText());
            value = result.text();
            matches += result.matches();
        }
        if (properties.isMaskIdCard()) {
            ReplacementResult result = replace(value, ID_CARD_PATTERN, properties.getReplaceText());
            value = result.text();
            matches += result.matches();
        }
        if (properties.isMaskToken()) {
            ReplacementResult bearerResult = replaceGroup(value, BEARER_PATTERN, 2, properties.getReplaceText());
            value = bearerResult.text();
            matches += bearerResult.matches();

            ReplacementResult tokenResult = replaceGroup(value, TOKEN_PATTERN, 2, properties.getReplaceText());
            value = tokenResult.text();
            matches += tokenResult.matches();
        }
        return new SanitizedText(value, matches);
    }

    private ReplacementResult replace(String text, Pattern pattern, String replaceText) {
        Matcher matcher = pattern.matcher(text);
        int matches = 0;
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            matches++;
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replaceText));
        }
        matcher.appendTail(buffer);
        return new ReplacementResult(buffer.toString(), matches);
    }

    private ReplacementResult replaceGroup(String text, Pattern pattern, int groupIndex, String replaceText) {
        Matcher matcher = pattern.matcher(text);
        int matches = 0;
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            matches++;
            String matched = matcher.group();
            String sensitive = matcher.group(groupIndex);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(matched.replace(sensitive, replaceText)));
        }
        matcher.appendTail(buffer);
        return new ReplacementResult(buffer.toString(), matches);
    }

    private record ReplacementResult(String text, int matches) {
    }

    public record SanitizedText(String text, int matches) {
    }

    public record SanitizationResult(Content content, int matches) {
    }
}

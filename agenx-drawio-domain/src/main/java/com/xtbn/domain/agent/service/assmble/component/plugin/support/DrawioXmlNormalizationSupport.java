package com.xtbn.domain.agent.service.assmble.component.plugin.support;

import org.springframework.stereotype.Component;

@Component
public class DrawioXmlNormalizationSupport {

    public String normalize(String raw) {
        if (raw == null) {
            return null;
        }

        String normalized = raw
                .replace("\uFEFF", "")
                .trim();

        normalized = stripMarkdownFence(normalized).trim();

        int start = normalized.indexOf("<mxfile");
        int end = normalized.lastIndexOf("</mxfile>");
        if (start >= 0 && end >= start) {
            end += "</mxfile>".length();
            normalized = normalized.substring(start, end).trim();
        }

        return normalized;
    }

    private String stripMarkdownFence(String content) {
        if (!content.startsWith("```")) {
            return content;
        }

        int firstLineBreak = content.indexOf('\n');
        if (firstLineBreak < 0) {
            return content;
        }

        String tail = content.substring(firstLineBreak + 1);
        int closing = tail.lastIndexOf("```");
        if (closing < 0) {
            return content;
        }

        return tail.substring(0, closing);
    }
}

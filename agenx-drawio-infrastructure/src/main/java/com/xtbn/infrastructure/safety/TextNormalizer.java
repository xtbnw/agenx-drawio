package com.xtbn.infrastructure.safety;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TextNormalizer {

    public NormalizedText normalize(String text, boolean enabled) {
        if (text == null || text.isEmpty()) {
            return new NormalizedText("", new int[0], new int[0]);
        }
        if (!enabled) {
            int[] starts = new int[text.length()];
            int[] ends = new int[text.length()];
            for (int i = 0; i < text.length(); i++) {
                starts[i] = i;
                ends[i] = i + 1;
            }
            return new NormalizedText(text, starts, ends);
        }

        StringBuilder normalized = new StringBuilder(text.length());
        List<Integer> starts = new ArrayList<>(text.length());
        List<Integer> ends = new ArrayList<>(text.length());

        for (int i = 0; i < text.length(); i++) {
            char current = normalizeChar(text.charAt(i));
            if (!isRetainedChar(current)) {
                continue;
            }
            normalized.append(current);
            starts.add(i);
            ends.add(i + 1);
        }

        return new NormalizedText(normalized.toString(), toIntArray(starts), toIntArray(ends));
    }

    public String normalizeKeyword(String text, boolean enabled) {
        return normalize(text, enabled).text();
    }

    private char normalizeChar(char current) {
        if (current == 12288) {
            current = ' ';
        } else if (current >= 65281 && current <= 65374) {
            current = (char) (current - 65248);
        }
        return Character.toLowerCase(current);
    }

    private boolean isRetainedChar(char current) {
        return Character.isLetterOrDigit(current) || isChinese(current);
    }

    private boolean isChinese(char current) {
        return current >= '\u4e00' && current <= '\u9fff';
    }

    private int[] toIntArray(List<Integer> values) {
        int[] result = new int[values.size()];
        for (int i = 0; i < values.size(); i++) {
            result[i] = values.get(i);
        }
        return result;
    }

    public record NormalizedText(String text, int[] originalStarts, int[] originalEnds) {
    }
}

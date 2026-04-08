package com.xtbn.domain.agent.adapter.port.safety;

import java.util.List;

public interface ISensitiveContentFilter {
    List<MatchResult> findAll(String text);

    boolean contains(String text);

    String replace(String text, String replacement);

    void refresh();

    record MatchResult(String word,
                       int originalStartInclusive,
                       int originalEndExclusive,
                       int normalizedStartInclusive,
                       int normalizedEndExclusive) {
    }
}

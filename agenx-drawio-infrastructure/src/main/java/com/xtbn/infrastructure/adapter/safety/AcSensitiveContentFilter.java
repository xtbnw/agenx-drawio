package com.xtbn.infrastructure.adapter.safety;

import com.xtbn.domain.agent.adapter.port.safety.ISensitiveContentFilter;
import com.xtbn.domain.agent.model.valobj.properties.PluginSensitiveWordProperties;
import com.xtbn.infrastructure.safety.SensitiveWordDictionaryProvider;
import com.xtbn.infrastructure.safety.TextNormalizer;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

@Service
public class AcSensitiveContentFilter implements ISensitiveContentFilter {
    private static final Logger LOGGER = Logger.getLogger(AcSensitiveContentFilter.class.getName());
    private final SensitiveWordDictionaryProvider dictionaryProvider;
    private final PluginSensitiveWordProperties properties;
    private final TextNormalizer textNormalizer;
    private final AtomicReference<MatcherSnapshot> matcherRef = new AtomicReference<>(MatcherSnapshot.empty());

    public AcSensitiveContentFilter(SensitiveWordDictionaryProvider dictionaryProvider,
                                    PluginSensitiveWordProperties properties,
                                    TextNormalizer textNormalizer) {
        this.dictionaryProvider = dictionaryProvider;
        this.properties = properties;
        this.textNormalizer = textNormalizer;
    }


    @PostConstruct
    public void init() {
        refresh();
    }

    @Override
    public void refresh() {
        SensitiveWordDictionaryProvider.DictionarySnapshot dictionarySnapshot = dictionaryProvider.load();
        MatcherSnapshot next = buildSnapshot(dictionarySnapshot);
        matcherRef.set(next);
        LOGGER.info(() -> String.format("Sensitive word matcher refreshed, words=%d, whitelist=%d, hotReloadEnabled=%s",
                next.wordCount(), next.whitelistCount(), properties.isHotReloadEnabled()));
    }

    @Override
    public List<MatchResult> findAll(String text) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }
        MatcherSnapshot snapshot = matcherRef.get();
        if (snapshot.isEmpty()) {
            return List.of();
        }

        TextNormalizer.NormalizedText normalizedText = textNormalizer.normalize(text, properties.isNormalizeEnabled());
        if (normalizedText.text().isEmpty()) {
            return List.of();
        }

        List<AutomatonMatch> whitelistMatches = snapshot.whitelistAutomaton().match(normalizedText.text());
        List<AutomatonMatch> rawMatches = snapshot.sensitiveAutomaton().match(normalizedText.text());
        if (rawMatches.isEmpty()) {
            return List.of();
        }

        List<MatchResult> results = new ArrayList<>();
        for (AutomatonMatch rawMatch : rawMatches) {
            if (isWhitelisted(rawMatch.startInclusive(), rawMatch.endExclusive(), whitelistMatches)) {
                continue;
            }

            int normalizedStart = rawMatch.startInclusive();
            int normalizedEnd = rawMatch.endExclusive();
            int originalStart = normalizedText.originalStarts()[normalizedStart];
            int originalEnd = normalizedText.originalEnds()[normalizedEnd - 1];
            results.add(new MatchResult(rawMatch.word(), originalStart, originalEnd, normalizedStart, normalizedEnd));
        }

        results.sort(Comparator.comparingInt(MatchResult::originalStartInclusive)
                .thenComparingInt(MatchResult::originalEndExclusive));
        return results;
    }

    @Override
    public boolean contains(String text) {
        return !findAll(text).isEmpty();
    }

    @Override
    public String replace(String text, String replacement) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        List<MatchResult> matches = findAll(text);
        if (matches.isEmpty()) {
            return text;
        }

        List<ReplaceRange> ranges = mergeRanges(matches);
        StringBuilder builder = new StringBuilder(text.length());
        int cursor = 0;
        for (ReplaceRange range : ranges) {
            builder.append(text, cursor, range.startInclusive());
            builder.append(replacement);
            cursor = range.endExclusive();
        }
        builder.append(text.substring(cursor));
        return builder.toString();
    }

    private MatcherSnapshot buildSnapshot(SensitiveWordDictionaryProvider.DictionarySnapshot dictionarySnapshot) {
        List<String> normalizedWords = normalizeTerms(dictionarySnapshot.words());
        List<String> normalizedWhitelist = normalizeTerms(dictionarySnapshot.whitelist());
        return new MatcherSnapshot(
                AcAutomaton.build(normalizedWords),
                AcAutomaton.build(normalizedWhitelist),
                normalizedWords.size(),
                normalizedWhitelist.size()
        );
    }

    private List<String> normalizeTerms(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .map(item -> textNormalizer.normalizeKeyword(item, properties.isNormalizeEnabled()))
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .distinct()
                .toList();
    }

    private boolean isWhitelisted(int startInclusive, int endExclusive, List<AutomatonMatch> whitelistMatches) {
        for (AutomatonMatch whitelistMatch : whitelistMatches) {
            if (whitelistMatch.startInclusive() <= startInclusive && whitelistMatch.endExclusive() >= endExclusive) {
                return true;
            }
        }
        return false;
    }

    private List<ReplaceRange> mergeRanges(List<MatchResult> matches) {
        List<ReplaceRange> ranges = new ArrayList<>();
        for (MatchResult match : matches) {
            ReplaceRange current = new ReplaceRange(match.originalStartInclusive(), match.originalEndExclusive());
            if (ranges.isEmpty()) {
                ranges.add(current);
                continue;
            }
            ReplaceRange last = ranges.get(ranges.size() - 1);
            if (current.startInclusive() <= last.endExclusive()) {
                ranges.set(ranges.size() - 1, new ReplaceRange(last.startInclusive(), Math.max(last.endExclusive(), current.endExclusive())));
            } else {
                ranges.add(current);
            }
        }
        return ranges;
    }

    private record ReplaceRange(int startInclusive, int endExclusive) {
    }

    private record MatcherSnapshot(AcAutomaton sensitiveAutomaton,
                                   AcAutomaton whitelistAutomaton,
                                   int wordCount,
                                   int whitelistCount) {
        private static MatcherSnapshot empty() {
            return new MatcherSnapshot(AcAutomaton.empty(), AcAutomaton.empty(), 0, 0);
        }

        private boolean isEmpty() {
            return wordCount <= 0;
        }
    }

    private record AutomatonMatch(String word, int startInclusive, int endExclusive) {
    }

    private static final class AcAutomaton {
        private final Node root = new Node();
        private final boolean empty;

        private AcAutomaton(boolean empty) {
            this.empty = empty;
            this.root.fail = this.root;
        }

        static AcAutomaton build(List<String> words) {
            if (words == null || words.isEmpty()) {
                return empty();
            }
            AcAutomaton automaton = new AcAutomaton(false);
            for (String word : words) {
                automaton.addWord(word);
            }
            automaton.buildFailPointers();
            return automaton;
        }

        static AcAutomaton empty() {
            return new AcAutomaton(true);
        }

        List<AutomatonMatch> match(String text) {
            if (empty || text == null || text.isEmpty()) {
                return List.of();
            }
            List<AutomatonMatch> results = new ArrayList<>();
            Node state = root;
            for (int i = 0; i < text.length(); i++) {
                char current = text.charAt(i);
                while (state != root && !state.children.containsKey(current)) {
                    state = state.fail;
                }
                state = state.children.getOrDefault(current, root);
                if (!state.outputs.isEmpty()) {
                    for (String word : state.outputs) {
                        results.add(new AutomatonMatch(word, i - word.length() + 1, i + 1));
                    }
                }
            }
            return results;
        }

        private void addWord(String word) {
            Node node = root;
            for (int i = 0; i < word.length(); i++) {
                char current = word.charAt(i);
                node = node.children.computeIfAbsent(current, key -> new Node());
            }
            node.outputs.add(word);
        }

        private void buildFailPointers() {
            Deque<Node> queue = new ArrayDeque<>();
            for (Node child : root.children.values()) {
                child.fail = root;
                queue.offer(child);
            }

            while (!queue.isEmpty()) {
                Node current = queue.poll();
                for (Map.Entry<Character, Node> entry : current.children.entrySet()) {
                    char transition = entry.getKey();
                    Node child = entry.getValue();
                    Node failState = current.fail;

                    while (failState != root && !failState.children.containsKey(transition)) {
                        failState = failState.fail;
                    }
                    child.fail = failState.children.getOrDefault(transition, root);
                    if (child.fail == child) {
                        child.fail = root;
                    }
                    child.outputs.addAll(child.fail.outputs);
                    queue.offer(child);
                }
            }
        }

        private static final class Node {
            private final Map<Character, Node> children = new HashMap<>();
            private Node fail;
            private final List<String> outputs = new ArrayList<>();
        }
    }
}

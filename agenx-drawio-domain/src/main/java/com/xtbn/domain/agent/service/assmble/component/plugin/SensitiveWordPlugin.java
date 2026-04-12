package com.xtbn.domain.agent.service.assmble.component.plugin;

import com.alibaba.fastjson.JSON;
import com.google.adk.agents.InvocationContext;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import com.xtbn.domain.agent.adapter.repository.ISensitiveFilterAuditRepository;
import com.xtbn.domain.agent.adapter.port.safety.ISensitiveContentFilter;
import com.xtbn.domain.agent.model.entity.SensitiveFilterAuditEntity;
import com.xtbn.domain.agent.model.entity.SensitiveFilterWordHitEntity;
import com.xtbn.domain.agent.model.valobj.properties.PluginSensitiveWordProperties;
import com.xtbn.domain.agent.service.assmble.component.plugin.support.AbstractAgentPluginSupport;
import com.xtbn.types.enums.ResponseCode;
import com.xtbn.types.exception.AppException;
import io.micrometer.core.instrument.MeterRegistry;
import io.reactivex.rxjava3.core.Maybe;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
@Slf4j
@Service("sensitiveWordPlugin")
public class SensitiveWordPlugin extends AbstractAgentPluginSupport {
    private static final int CONTENT_PREVIEW_LIMIT = 512;

    private final PluginSensitiveWordProperties properties;
    private final ISensitiveContentFilter sensitiveContentFilter;
    private final ISensitiveFilterAuditRepository sensitiveFilterAuditRepository;

    public SensitiveWordPlugin(MeterRegistry meterRegistry,
                               PluginSensitiveWordProperties properties,
                               ISensitiveContentFilter sensitiveContentFilter,
                               ISensitiveFilterAuditRepository sensitiveFilterAuditRepository) {
        super("SensitiveWordPlugin", meterRegistry);
        this.properties = properties;
        this.sensitiveContentFilter = sensitiveContentFilter;
        this.sensitiveFilterAuditRepository = sensitiveFilterAuditRepository;
    }

    @Override
    public Maybe<Content> onUserMessageCallback(InvocationContext invocationContext, Content userMessage) {
        if (!properties.isEnabled() || userMessage == null || userMessage.parts().isEmpty()) {
            return super.onUserMessageCallback(invocationContext, userMessage);
        }

        fillInvocationMdc(invocationContext);
        long startNanos = System.nanoTime();
        try {
            List<Part> updatedParts = new ArrayList<>();
            int totalMatches = 0;
            boolean replaced = false;
            List<String> hitTextSegments = new ArrayList<>();
            Map<String, Integer> wordHitCounts = new LinkedHashMap<>();

            for (Part part : userMessage.parts().orElse(List.of())) {
                if (part == null || part.text().isEmpty()) {
                    updatedParts.add(part);
                    continue;
                }

                String originalText = part.text().orElse("");
                List<ISensitiveContentFilter.MatchResult> matches = sensitiveContentFilter.findAll(originalText);
                if (matches.isEmpty()) {
                    updatedParts.add(part);
                    continue;
                }

                hitTextSegments.add(originalText);
                totalMatches += matches.size();
                collectWordHitCounts(matches, wordHitCounts);
                if (properties.isRejectMode()) {
                    long elapsedMillis = elapsedMillis(startNanos);
                    recordMetrics(invocationContext, totalMatches, elapsedMillis, "hit", "reject");
                    persistAudit(invocationContext, totalMatches, elapsedMillis, "REJECT", wordHitCounts, hitTextSegments);
                    log.warn("Sensitive words detected and rejected, matches={}", totalMatches);
                    throw new AppException(ResponseCode.SENSITIVE_WORD_HIT.getCode(), ResponseCode.SENSITIVE_WORD_HIT.getInfo());
                }

                String replacedText = sensitiveContentFilter.replace(originalText, properties.getReplaceText());
                updatedParts.add(part.toBuilder().text(replacedText).build());
                replaced = true;
            }

            if (totalMatches <= 0) {
                recordMetrics(invocationContext, 0, elapsedMillis(startNanos), "miss", "pass");
                return super.onUserMessageCallback(invocationContext, userMessage);
            }

            long elapsedMillis = elapsedMillis(startNanos);
            recordMetrics(invocationContext, totalMatches, elapsedMillis, "hit", replaced ? "replace" : "pass");
            persistAudit(invocationContext, totalMatches, elapsedMillis, replaced ? "REPLACE" : "PASS", wordHitCounts, hitTextSegments);
            log.info("Sensitive words processed, mode={}, matches={}",
                    properties.getMode().name().toLowerCase(), totalMatches);
            if (!replaced) {
                return super.onUserMessageCallback(invocationContext, userMessage);
            }
            return Maybe.just(userMessage.toBuilder().parts(updatedParts).build());
        } finally {
            clearMdc();
        }
    }

    private void recordMetrics(InvocationContext invocationContext, int totalMatches, long elapsedMillis, String result, String action) {
        // Metrics intentionally omitted. Keep only logs and audit persistence for this plugin.
    }

    private void persistAudit(InvocationContext invocationContext,
                              int totalMatches,
                              long elapsedMillis,
                              String action,
                              Map<String, Integer> wordHitCounts,
                              List<String> hitTextSegments) {
        try {
            long now = System.currentTimeMillis();
            SensitiveFilterAuditEntity audit = new SensitiveFilterAuditEntity();
            audit.setInvocationId(invocationContext == null ? null : invocationContext.invocationId());
            audit.setAppName(invocationContext == null ? null : invocationContext.appName());
            audit.setAgentName(invocationContext == null || invocationContext.agent() == null ? null : invocationContext.agent().name());
            audit.setSessionId(invocationContext == null || invocationContext.session() == null ? null : invocationContext.session().id());
            audit.setUserId(invocationContext == null ? null : invocationContext.userId());
            audit.setMode(properties.getMode().name());
            audit.setAction(action);
            audit.setMatchCount(totalMatches);
            audit.setMatchedWordsJson(JSON.toJSONString(wordHitCounts));
            audit.setContentPreview(truncate(String.join("\n", hitTextSegments), CONTENT_PREVIEW_LIMIT));
            audit.setFilterDurationMs(elapsedMillis);
            audit.setCreatedAt(now);
            audit.setWordHits(buildWordHits(invocationContext == null ? null : invocationContext.invocationId(), wordHitCounts, now));
            sensitiveFilterAuditRepository.appendAudit(audit);
        } catch (Exception e) {
            log.warn("Failed to persist sensitive filter audit: {}", e.getMessage());
        }
    }

    private List<SensitiveFilterWordHitEntity> buildWordHits(String invocationId, Map<String, Integer> wordHitCounts, long createdAt) {
        List<SensitiveFilterWordHitEntity> hits = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : wordHitCounts.entrySet()) {
            SensitiveFilterWordHitEntity hit = new SensitiveFilterWordHitEntity();
            hit.setInvocationId(invocationId);
            hit.setWord(entry.getKey());
            hit.setHitCount(entry.getValue());
            hit.setCreatedAt(createdAt);
            hits.add(hit);
        }
        return hits;
    }

    private void collectWordHitCounts(List<ISensitiveContentFilter.MatchResult> matches, Map<String, Integer> wordHitCounts) {
        for (ISensitiveContentFilter.MatchResult match : matches) {
            wordHitCounts.merge(match.word(), 1, Integer::sum);
        }
    }

    private long elapsedMillis(long startNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }
}

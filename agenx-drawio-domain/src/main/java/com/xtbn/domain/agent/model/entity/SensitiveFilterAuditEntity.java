package com.xtbn.domain.agent.model.entity;

import lombok.Data;

import java.util.List;

@Data
public class SensitiveFilterAuditEntity {
    private Long id;
    private String invocationId;
    private String appName;
    private String agentName;
    private String sessionId;
    private String userId;
    private String mode;
    private String action;
    private Integer matchCount;
    private String matchedWordsJson;
    private String contentPreview;
    private Long filterDurationMs;
    private Long createdAt;
    private List<SensitiveFilterWordHitEntity> wordHits;
}

package com.xtbn.infrastructure.dao.po;

import lombok.Data;

@Data
public class SensitiveFilterAuditPO {
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
}

package com.xtbn.infrastructure.dao.po;

import lombok.Data;

@Data
public class AgentSessionRecordPO {
    private Long id;
    private String appName;
    private String agentId;
    private String userId;
    private String sessionId;
    private String title;
    private String status;
    private Integer messageCount;
    private String lastMessagePreview;
    private Long createdAt;
    private Long updatedAt;
    private Long lastMessageAt;
    private String stateJson;
}

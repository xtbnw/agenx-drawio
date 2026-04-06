package com.xtbn.infrastructure.dao.po;

import lombok.Data;

@Data
public class AgentMemoryRecordPO {
    private Long id;
    private String appName;
    private String userId;
    private String sessionId;
    private String memoryText;
    private String source;
    private Long createdAt;
    private Long updatedAt;
}

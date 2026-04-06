package com.xtbn.domain.agent.model.entity;

import lombok.Data;

@Data
public class StoredMemoryEntity {
    private Long id;
    private String appName;
    private String userId;
    private String sessionId;
    private String memoryText;
    private String source;
    private Long createdAt;
    private Long updatedAt;
}

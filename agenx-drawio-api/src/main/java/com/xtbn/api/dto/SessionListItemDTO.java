package com.xtbn.api.dto;

import lombok.Data;

@Data
public class SessionListItemDTO {
    private String sessionId;
    private String title;
    private String agentId;
    private String agentName;
    private String lastMessagePreview;
    private Integer messageCount;
    private Long createdAt;
    private Long updatedAt;
    private Long lastMessageAt;
}

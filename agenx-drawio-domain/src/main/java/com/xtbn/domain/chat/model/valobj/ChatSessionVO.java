package com.xtbn.domain.chat.model.valobj;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatSessionVO {
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

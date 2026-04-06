package com.xtbn.domain.chat.model.valobj;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ChatSessionDetailVO {
    private String sessionId;
    private String title;
    private String agentId;
    private String agentName;
    private Long createdAt;
    private Long updatedAt;
    private Long lastMessageAt;
    private Integer messageCount;
    private List<ChatHistoryMessageVO> messages;
}

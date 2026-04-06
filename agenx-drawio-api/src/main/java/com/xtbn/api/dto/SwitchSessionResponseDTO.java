package com.xtbn.api.dto;

import lombok.Data;

import java.util.List;

@Data
public class SwitchSessionResponseDTO {
    private String sessionId;
    private String title;
    private String agentId;
    private String agentName;
    private Long createdAt;
    private Long updatedAt;
    private Long lastMessageAt;
    private Integer messageCount;
    private List<HistoryMessageDTO> messages;
}

package com.xtbn.api.dto;

import lombok.Data;

@Data
public class HistoryMessageDTO {
    private String messageId;
    private String role;
    private String content;
    private Long createdAt;
}

package com.xtbn.api.dto;

import lombok.Data;

@Data
public class ChatRequestDTO {
    /**
     * 智能体ID
     */
    private String agentId;
    /**
     * 用户ID
     */
    private String userId;
    /**
     * 会话ID
     */
    private String sessionId;
    /**
     * 消息内容
     */
    private String message;

}

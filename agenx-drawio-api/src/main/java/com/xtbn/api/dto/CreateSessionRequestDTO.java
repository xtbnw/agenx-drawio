package com.xtbn.api.dto;

import lombok.Data;

@Data
public class CreateSessionRequestDTO {
    /**
     * 智能体ID
     */
    private String agentId;

    /**
     * 用户ID
     */
    private String userId;

}

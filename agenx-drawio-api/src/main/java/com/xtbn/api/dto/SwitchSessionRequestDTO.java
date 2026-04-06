package com.xtbn.api.dto;

import lombok.Data;

@Data
public class SwitchSessionRequestDTO {
    private String userId;
    private String sessionId;
}

package com.xtbn.domain.agent.model.entity;

import lombok.Data;

@Data
public class SensitiveFilterWordHitEntity {
    private Long id;
    private Long auditId;
    private String invocationId;
    private String word;
    private Integer hitCount;
    private Long createdAt;
}

package com.xtbn.infrastructure.dao.po;

import lombok.Data;

@Data
public class SensitiveFilterWordHitPO {
    private Long id;
    private Long auditId;
    private String invocationId;
    private String word;
    private Integer hitCount;
    private Long createdAt;
}

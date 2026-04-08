package com.xtbn.infrastructure.dao.po;

import lombok.Data;

@Data
public class GovernanceBlacklistPO {
    private Long id;
    private String subjectType;
    private String subjectValue;
    private String status;
    private String reason;
    private Long expireTime;
    private String createdBy;
    private Long createdAt;
    private String updatedBy;
    private Long updatedAt;
}

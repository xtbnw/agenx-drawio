package com.xtbn.domain.agent.adapter.repository;

import com.xtbn.domain.agent.model.entity.SensitiveFilterAuditEntity;

public interface ISensitiveFilterAuditRepository {

    void appendAudit(SensitiveFilterAuditEntity audit);
}

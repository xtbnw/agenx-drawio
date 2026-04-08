package com.xtbn.infrastructure.adapter.repository;

import com.xtbn.domain.agent.adapter.repository.ISensitiveFilterAuditRepository;
import com.xtbn.domain.agent.model.entity.SensitiveFilterAuditEntity;
import com.xtbn.domain.agent.model.entity.SensitiveFilterWordHitEntity;
import com.xtbn.infrastructure.dao.mapper.SensitiveFilterAuditMapper;
import com.xtbn.infrastructure.dao.po.SensitiveFilterAuditPO;
import com.xtbn.infrastructure.dao.po.SensitiveFilterWordHitPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class SensitiveFilterAuditRepository implements ISensitiveFilterAuditRepository {

    private final SensitiveFilterAuditMapper mapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void appendAudit(SensitiveFilterAuditEntity audit) {
        SensitiveFilterAuditPO auditPO = toPo(audit);
        mapper.insertAudit(auditPO);

        List<SensitiveFilterWordHitEntity> wordHits = audit.getWordHits();
        if (wordHits == null || wordHits.isEmpty()) {
            return;
        }

        List<SensitiveFilterWordHitPO> hitPOs = wordHits.stream()
                .map(item -> toPo(item, auditPO.getId()))
                .toList();
        mapper.batchInsertWordHits(hitPOs);
    }

    private SensitiveFilterAuditPO toPo(SensitiveFilterAuditEntity entity) {
        SensitiveFilterAuditPO po = new SensitiveFilterAuditPO();
        po.setId(entity.getId());
        po.setInvocationId(entity.getInvocationId());
        po.setAppName(entity.getAppName());
        po.setAgentName(entity.getAgentName());
        po.setSessionId(entity.getSessionId());
        po.setUserId(entity.getUserId());
        po.setMode(entity.getMode());
        po.setAction(entity.getAction());
        po.setMatchCount(entity.getMatchCount());
        po.setMatchedWordsJson(entity.getMatchedWordsJson());
        po.setContentPreview(entity.getContentPreview());
        po.setFilterDurationMs(entity.getFilterDurationMs());
        po.setCreatedAt(entity.getCreatedAt());
        return po;
    }

    private SensitiveFilterWordHitPO toPo(SensitiveFilterWordHitEntity entity, Long auditId) {
        SensitiveFilterWordHitPO po = new SensitiveFilterWordHitPO();
        po.setId(entity.getId());
        po.setAuditId(auditId);
        po.setInvocationId(entity.getInvocationId());
        po.setWord(entity.getWord());
        po.setHitCount(entity.getHitCount());
        po.setCreatedAt(entity.getCreatedAt());
        return po;
    }
}

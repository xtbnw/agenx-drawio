package com.xtbn.infrastructure.dao.mapper;

import com.xtbn.infrastructure.dao.po.SensitiveFilterAuditPO;
import com.xtbn.infrastructure.dao.po.SensitiveFilterWordHitPO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SensitiveFilterAuditMapper {

    void insertAudit(SensitiveFilterAuditPO audit);

    void batchInsertWordHits(List<SensitiveFilterWordHitPO> hits);
}

package com.xtbn.infrastructure.dao.mapper;

import com.xtbn.infrastructure.dao.po.GovernanceBlacklistPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GovernanceBlacklistMapper {

    List<GovernanceBlacklistPO> queryActiveRules(@Param("now") long now);

    GovernanceBlacklistPO findActiveRule(@Param("subjectType") String subjectType,
                                         @Param("subjectValue") String subjectValue,
                                         @Param("now") long now);
}

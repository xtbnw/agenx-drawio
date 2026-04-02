package com.xtbn.domain.agent.model.entity;

import com.xtbn.domain.agent.model.valobj.AgentConfigVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AssembleCommandEntity {
    private AgentConfigVO agentConfigVO;
}

package com.xtbn.domain.agent.service;

import com.xtbn.domain.agent.model.valobj.AgentConfigVO;

import java.util.List;

public interface IAssembleService {
    void assembleAgents(List<AgentConfigVO> agentConfigVOS) throws Exception;
}

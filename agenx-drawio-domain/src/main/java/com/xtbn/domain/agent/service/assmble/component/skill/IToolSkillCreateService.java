package com.xtbn.domain.agent.service.assmble.component.skill;

import com.xtbn.domain.agent.model.valobj.AgentConfigVO;
import org.springframework.ai.tool.ToolCallback;

public interface IToolSkillCreateService {
    ToolCallback[] buildToolCallback(AgentConfigVO.AgentRuntime.ChatModel.ToolSkill toolSkill) throws Exception;
}

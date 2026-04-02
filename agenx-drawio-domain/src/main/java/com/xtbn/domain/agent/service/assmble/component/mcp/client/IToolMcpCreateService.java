package com.xtbn.domain.agent.service.assmble.component.mcp.client;

import com.xtbn.domain.agent.model.valobj.AgentConfigVO;
import org.springframework.ai.tool.ToolCallback;

public interface IToolMcpCreateService {
    ToolCallback[] buildToolCallback(AgentConfigVO.AgentRuntime.ChatModel.ToolMcp toolMcp) throws Exception;
}

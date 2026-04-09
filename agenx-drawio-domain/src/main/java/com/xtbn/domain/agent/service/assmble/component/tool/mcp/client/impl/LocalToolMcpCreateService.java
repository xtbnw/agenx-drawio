package com.xtbn.domain.agent.service.assmble.component.tool.mcp.client.impl;

import com.xtbn.domain.agent.model.valobj.AgentConfigVO;
import com.xtbn.domain.agent.service.assmble.component.tool.mcp.client.IToolMcpCreateService;
import com.xtbn.domain.agent.service.assmble.component.tool.ToolCallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Slf4j
@Service
public class LocalToolMcpCreateService implements IToolMcpCreateService {
    @Resource
    private ApplicationContext applicationContext;
    @Resource
    private ToolCallbackFactory toolCallbackFactory;

    @Override
    public ToolCallback[] buildToolCallback(AgentConfigVO.AgentRuntime.ChatModel.ToolMcp toolMcp) throws Exception {
        AgentConfigVO.AgentRuntime.ChatModel.ToolMcp.LocalParameters local = toolMcp.getLocal();
        String name = local.getName();

        ToolCallbackProvider localToolCallbackProvider = (ToolCallbackProvider) applicationContext.getBean(local.getName());
        log.info("tool local mcp initialize {}", name);

        return toolCallbackFactory.wrapWithLogging("mcp-local", name, localToolCallbackProvider.getToolCallbacks());
    }
}

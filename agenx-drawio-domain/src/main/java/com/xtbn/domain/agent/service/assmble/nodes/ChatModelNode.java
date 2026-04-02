package com.xtbn.domain.agent.service.assmble.nodes;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import com.xtbn.domain.agent.model.entity.AssembleCommandEntity;
import com.xtbn.domain.agent.model.valobj.AgentConfigVO;
import com.xtbn.domain.agent.model.valobj.AgentRegisterVO;
import com.xtbn.domain.agent.service.assmble.AbstractSupportNode;
import com.xtbn.domain.agent.service.assmble.component.mcp.client.IToolMcpCreateService;
import com.xtbn.domain.agent.service.assmble.component.mcp.client.factory.DefaultMcpClientFactory;
import com.xtbn.domain.agent.service.assmble.component.skill.IToolSkillCreateService;
import com.xtbn.domain.agent.service.assmble.factory.DefaultAssembleFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ChatModelNode extends AbstractSupportNode {
    @Resource
    private AgentNode agentNode;
    @Resource
    private DefaultMcpClientFactory defaultMcpClientFactory;
    @Resource
    private IToolSkillCreateService toolSkillCreateService;

    @Override
    protected AgentRegisterVO doApply(AssembleCommandEntity requestParameter, DefaultAssembleFactory.DynamicContext dynamicContext) throws Exception {
        log.info("Agent组装-节点：ChatModelNode");
        // 编写 AiApi 构建逻辑
        AgentConfigVO agentConfigVO = requestParameter.getAgentConfigVO();
        AgentConfigVO.AgentRuntime.ChatModel chatModelConfig = agentConfigVO.getRuntime().getChatModel();
        OpenAiApi openAiApi = dynamicContext.getOpenAiApi();
        List<AgentConfigVO.AgentRuntime.ChatModel.ToolMcp> toolMcpList = chatModelConfig.getToolMcpList();
        List<AgentConfigVO.AgentRuntime.ChatModel.ToolSkill> toolSkillList = chatModelConfig.getToolSkillList();
        // 构建mcp服务（工厂）
        List<ToolCallback> toolCallbackList = new ArrayList<>();
        if (null != toolMcpList && !toolMcpList.isEmpty()) {
            for (AgentConfigVO.AgentRuntime.ChatModel.ToolMcp toolMcp : toolMcpList) {
                IToolMcpCreateService toolMcpCreateService = defaultMcpClientFactory.getToolMcpCreateService(toolMcp);
                ToolCallback[] toolCallbacks = toolMcpCreateService.buildToolCallback(toolMcp);
                toolCallbackList.addAll(List.of(toolCallbacks));
            }
        }
        // 构建技能服务（工厂）
        if (null != toolSkillList && !toolSkillList.isEmpty()) {
            for (AgentConfigVO.AgentRuntime.ChatModel.ToolSkill toolSkill : toolSkillList) {
                ToolCallback[] toolCallbacks = toolSkillCreateService.buildToolCallback(toolSkill);
                toolCallbackList.addAll(List.of(toolCallbacks));
            }
        }

        ChatModel chatmodel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(
                        OpenAiChatOptions.builder()
                                .model(chatModelConfig.getModel())
                                .toolCallbacks(toolCallbackList)
                                .build()
                )
                .build();

        dynamicContext.setChatModel(chatmodel);

        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<AssembleCommandEntity, DefaultAssembleFactory.DynamicContext, AgentRegisterVO> get(AssembleCommandEntity requestParameter, DefaultAssembleFactory.DynamicContext dynamicContext) throws Exception {
        return agentNode;
    }

}

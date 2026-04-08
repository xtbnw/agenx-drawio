package com.xtbn.domain.agent.service.assmble.nodes;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import com.google.adk.agents.LlmAgent;
import com.google.adk.models.springai.SpringAI;
import com.google.adk.tools.GoogleSearchTool;
import com.google.common.collect.ImmutableList;
import com.xtbn.domain.agent.model.entity.AssembleCommandEntity;
import com.xtbn.domain.agent.model.valobj.AgentConfigVO;
import com.xtbn.domain.agent.model.valobj.AgentRegisterVO;
import com.xtbn.domain.agent.service.assmble.AbstractSupportNode;
import com.xtbn.domain.agent.service.assmble.factory.DefaultAssembleFactory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.StreamingChatModel;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class AgentNode extends AbstractSupportNode {
    @Resource
    private AgentWorkflowNode agentWorkflowNode;
    @Override
    protected AgentRegisterVO doApply(AssembleCommandEntity requestParameter, DefaultAssembleFactory.DynamicContext dynamicContext) throws Exception {
        log.info("Agent组装-节点：AgentNode");

        AgentConfigVO agentConfigVO = requestParameter.getAgentConfigVO();
        List<AgentConfigVO.AgentRuntime.Agent> agents = agentConfigVO.getRuntime().getAgents();
        ChatModel chatModel = dynamicContext.getChatModel();

        for(AgentConfigVO.AgentRuntime.Agent agentConfig : agents){
            StreamingChatModel streamingChatModel = (StreamingChatModel) chatModel;
            LlmAgent agent=LlmAgent.builder()
                    .name(agentConfig.getName())
                    .description(agentConfig.getDescription())
                    .model(new SpringAI(chatModel, streamingChatModel, agentConfigVO.getRuntime().getChatModel().getModel()))
                    .instruction(agentConfig.getInstruction())
                    .outputKey(agentConfig.getOutputKey())
                    .build();
            dynamicContext.getAgentGroup().put(agentConfig.getName(), agent);
        }

        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<AssembleCommandEntity, DefaultAssembleFactory.DynamicContext, AgentRegisterVO> get(AssembleCommandEntity requestParameter, DefaultAssembleFactory.DynamicContext dynamicContext) throws Exception {
        return agentWorkflowNode;
    }
}


package com.xtbn.domain.agent.service.assmble.nodes;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import com.xtbn.domain.agent.model.entity.AssembleCommandEntity;
import com.xtbn.domain.agent.model.valobj.AgentConfigVO;
import com.xtbn.domain.agent.model.valobj.AgentRegisterVO;
import com.xtbn.domain.agent.service.assmble.AbstractSupportNode;
import com.xtbn.domain.agent.service.assmble.factory.DefaultAssembleFactory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AiApiNode extends AbstractSupportNode {

    @Resource
    private ChatModelNode chatModelNode;

    @Override
    protected AgentRegisterVO doApply(AssembleCommandEntity requestParameter, DefaultAssembleFactory.DynamicContext dynamicContext) throws Exception {
        log.info("Agent组装-节点：AiApiNode");
        // 编写 AiApi 构建逻辑
        AgentConfigVO agentConfigVO = requestParameter.getAgentConfigVO();
        AgentConfigVO.AgentRuntime.AiApi aiApiConfig=agentConfigVO.getRuntime().getAiApi();

        OpenAiApi openAiApi=OpenAiApi.builder()
                .baseUrl(aiApiConfig.getBaseUrl())
                .apiKey(aiApiConfig.getApiKey())
                .completionsPath(aiApiConfig.getCompletionsPath())
                .build();

        dynamicContext.setOpenAiApi(openAiApi);

        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<AssembleCommandEntity, DefaultAssembleFactory.DynamicContext, AgentRegisterVO> get(AssembleCommandEntity requestParameter, DefaultAssembleFactory.DynamicContext dynamicContext) throws Exception {
        return chatModelNode;
    }

}


package com.xtbn.domain.agent.service.assmble.factory;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import com.google.adk.agents.BaseAgent;
import com.xtbn.domain.agent.model.entity.AssembleCommandEntity;
import com.xtbn.domain.agent.model.valobj.AgentConfigVO;
import com.xtbn.domain.agent.model.valobj.AgentRegisterVO;
import com.xtbn.domain.agent.service.assmble.nodes.RootNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class DefaultAssembleFactory {
    @Resource
    private RootNode rootNode;

    public StrategyHandler<AssembleCommandEntity, DynamicContext, AgentRegisterVO> getStrategyHandler() {
        return rootNode;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DynamicContext {
        /**
         * LLM API
         */
        private OpenAiApi openAiApi;
        /**
         * chatModel
         */
        private ChatModel chatModel;
        /**
         * Agents
         */
        private Map<String, BaseAgent> agentGroup = new HashMap<>();
        /**
         * Current Workflow Step Index
         */
        private AtomicInteger currentStepIndex = new AtomicInteger(0);
        /**
         * Current Workflow
         */
        private AgentConfigVO.AgentRuntime.AgentWorkflow currentAgentWorkflow;

        public List<BaseAgent> queryAgentList(List<String> agentNames) {
            if (agentNames == null || agentNames.isEmpty() || agentGroup == null) {
                return Collections.emptyList();
            }

            List<BaseAgent> agents = new ArrayList<>();
            for (String name : agentNames) {
                BaseAgent agent = agentGroup.get(name);
                if (agent!=null){
                    agents.add(agent);
                }
            }

            return agents;
        }

        public void addCurrentStepIndex(){
            currentStepIndex.incrementAndGet();
        }

        public int getCurrentStepIndex(){
            return currentStepIndex.get();
        }

    }
}

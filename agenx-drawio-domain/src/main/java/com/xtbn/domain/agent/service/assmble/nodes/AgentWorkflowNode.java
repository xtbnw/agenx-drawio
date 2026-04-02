package com.xtbn.domain.agent.service.assmble.nodes;


import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import com.xtbn.domain.agent.adapter.port.registry.IBeanRegistry;
import com.xtbn.domain.agent.model.entity.AssembleCommandEntity;
import com.xtbn.domain.agent.model.valobj.AgentConfigVO;
import com.xtbn.domain.agent.model.valobj.AgentRegisterVO;
import com.xtbn.domain.agent.model.valobj.enums.WorkFlowTypeEnum;
import com.xtbn.domain.agent.service.assmble.AbstractSupportNode;
import com.xtbn.domain.agent.service.assmble.factory.DefaultAssembleFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@Service
public class AgentWorkflowNode extends AbstractSupportNode {
    @Resource
    private RunnerNode runnerNode;
    @Resource
    private IBeanRegistry beanRegistry;

    @Override
    protected AgentRegisterVO doApply(AssembleCommandEntity requestParameter, DefaultAssembleFactory.DynamicContext dynamicContext) throws Exception {
        log.info("Agent组装-节点：AgentWorkflowNode");

        AgentConfigVO agentConfigVO = requestParameter.getAgentConfigVO();
        List<AgentConfigVO.AgentRuntime.AgentWorkflow> agentWorkflows = agentConfigVO.getRuntime().getAgentWorkflows();

        if (null == agentWorkflows || agentWorkflows.isEmpty() || dynamicContext.getCurrentStepIndex() >= agentWorkflows.size()) {
            // 设置结果值
            dynamicContext.setCurrentAgentWorkflow(null);
            // 路由下节点
            return router(requestParameter, dynamicContext);
        }
        dynamicContext.setCurrentAgentWorkflow(agentWorkflows.get(dynamicContext.getCurrentStepIndex()));
        dynamicContext.addCurrentStepIndex();

        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<AssembleCommandEntity, DefaultAssembleFactory.DynamicContext, AgentRegisterVO> get(AssembleCommandEntity requestParameter, DefaultAssembleFactory.DynamicContext dynamicContext) throws Exception {
        AgentConfigVO.AgentRuntime.AgentWorkflow currentAgentWorkflow=dynamicContext.getCurrentAgentWorkflow();
        if (null == currentAgentWorkflow) {
            return runnerNode;
        }
        String workflowType=currentAgentWorkflow.getType();
        WorkFlowTypeEnum workFlowTypeEnum=WorkFlowTypeEnum.getByType(workflowType);
        if(null==workFlowTypeEnum){
            throw new RuntimeException("Agent Workflow Type is not support");
        }
        return beanRegistry.getBean(workFlowTypeEnum.getNodeClass());
    }
}

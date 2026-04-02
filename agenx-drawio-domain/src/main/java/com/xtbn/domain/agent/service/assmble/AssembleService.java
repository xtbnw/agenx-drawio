package com.xtbn.domain.agent.service.assmble;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import com.xtbn.domain.agent.adapter.port.registry.IBeanRegistry;
import com.xtbn.domain.agent.model.entity.AssembleCommandEntity;
import com.xtbn.domain.agent.model.valobj.AgentConfigVO;
import com.xtbn.domain.agent.model.valobj.AgentRegisterVO;
import com.xtbn.domain.agent.service.IAssembleService;
import com.xtbn.domain.agent.service.assmble.factory.DefaultAssembleFactory;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AssembleService implements IAssembleService {
    @Resource
    private DefaultAssembleFactory defaultAssembleFactory;
    @Resource
    private IBeanRegistry beanRegistry;
    @Override
    public void assembleAgents(List<AgentConfigVO> agentConfigVOS) throws Exception {
        StrategyHandler<AssembleCommandEntity, DefaultAssembleFactory.DynamicContext, AgentRegisterVO> handler = defaultAssembleFactory.getStrategyHandler();
        for (AgentConfigVO agentConfigVO : agentConfigVOS) {
            AgentRegisterVO agentRegisterVO = handler.apply(
                    AssembleCommandEntity.builder()
                            .agentConfigVO(agentConfigVO)
                            .build(),
                    new DefaultAssembleFactory.DynamicContext()
            );
            beanRegistry.registerBean(agentConfigVO.getRootAgent().getRootAgentId(), AgentRegisterVO.class, agentRegisterVO);
        }
    }
}

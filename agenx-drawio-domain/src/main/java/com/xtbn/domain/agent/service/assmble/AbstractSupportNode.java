package com.xtbn.domain.agent.service.assmble;

import cn.bugstack.wrench.design.framework.tree.AbstractMultiThreadStrategyRouter;
import com.xtbn.domain.agent.adapter.port.registry.IBeanRegistry;
import com.xtbn.domain.agent.model.entity.AssembleCommandEntity;
import com.xtbn.domain.agent.model.valobj.AgentRegisterVO;
import com.xtbn.domain.agent.service.assmble.factory.DefaultAssembleFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import javax.annotation.Resource;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public abstract class AbstractSupportNode extends AbstractMultiThreadStrategyRouter<AssembleCommandEntity, DefaultAssembleFactory.DynamicContext, AgentRegisterVO> {
    protected final Logger log = LoggerFactory.getLogger(AbstractSupportNode.class);

    @Resource
    protected IBeanRegistry beanRegistry;

    @Override
    protected void multiThread(AssembleCommandEntity requestParameter, DefaultAssembleFactory.DynamicContext dynamicContext) throws ExecutionException, InterruptedException, TimeoutException {
    }
}

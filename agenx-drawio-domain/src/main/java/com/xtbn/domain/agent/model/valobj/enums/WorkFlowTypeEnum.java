package com.xtbn.domain.agent.model.valobj.enums;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import com.xtbn.domain.agent.model.entity.AssembleCommandEntity;
import com.xtbn.domain.agent.model.valobj.AgentRegisterVO;
import com.xtbn.domain.agent.service.assmble.factory.DefaultAssembleFactory;
import com.xtbn.domain.agent.service.assmble.nodes.workflow.LoopAgentNode;
import com.xtbn.domain.agent.service.assmble.nodes.workflow.ParallelAgentNode;
import com.xtbn.domain.agent.service.assmble.nodes.workflow.SequentialAgentNode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum WorkFlowTypeEnum {

    Loop("循环执行", "loop", LoopAgentNode.class),
    Parallel("并行执行", "parallel", ParallelAgentNode.class),
    Sequential("串行执行", "sequential", SequentialAgentNode.class),

    ;

    private String name;
    private String type;
    private Class<? extends StrategyHandler<AssembleCommandEntity, DefaultAssembleFactory.DynamicContext, AgentRegisterVO>> nodeClass;

    public static WorkFlowTypeEnum getByType(String type) {
        if (type == null) {
            return null;
        }
        for (WorkFlowTypeEnum value : values()) {
            if (value.getType().equalsIgnoreCase(type)) {
                return value;
            }
        }
        return null;
    }
}

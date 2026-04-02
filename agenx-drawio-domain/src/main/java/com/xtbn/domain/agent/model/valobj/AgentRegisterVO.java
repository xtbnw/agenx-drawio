package com.xtbn.domain.agent.model.valobj;

import com.google.adk.runner.InMemoryRunner;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AgentRegisterVO {
    /**
     * 智能体名称
     */
    private String appName;

    /**
     * 智能体ID
     */
    private String rootAgentId;

    /**
     * 智能体名称
     */
    private String rootAgentName;

    /**
     * 智能体描述
     */
    private String rootAgentDesc;

    /**
     * 智能体执行对象
     */
    private InMemoryRunner runner;
}

package com.xtbn.domain.agent.service.assmble.nodes;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import com.google.adk.agents.BaseAgent;
import com.google.adk.plugins.BasePlugin;
import com.google.adk.runner.InMemoryRunner;
import com.google.common.collect.ImmutableList;
import com.xtbn.domain.agent.adapter.port.registry.IBeanRegistry;
import com.xtbn.domain.agent.model.entity.AssembleCommandEntity;
import com.xtbn.domain.agent.model.valobj.AgentConfigVO;
import com.xtbn.domain.agent.model.valobj.AgentRegisterVO;
import com.xtbn.domain.agent.service.assmble.AbstractSupportNode;
import com.xtbn.domain.agent.service.assmble.factory.DefaultAssembleFactory;
import com.xtbn.types.enums.ResponseCode;
import com.xtbn.types.exception.AppException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class RunnerNode extends AbstractSupportNode {
    @Resource
    private IBeanRegistry beanRegistry;
    @Override
    protected AgentRegisterVO doApply(AssembleCommandEntity requestParameter, DefaultAssembleFactory.DynamicContext dynamicContext) throws Exception {
        log.info("Agent组装-节点：RunnerNode");

        AgentConfigVO agentConfigVO = requestParameter.getAgentConfigVO();
        String appName = agentConfigVO.getAppName();
        String rootAgentId = agentConfigVO.getRootAgent().getRootAgentId();
        String rootAgentName = agentConfigVO.getRootAgent().getRootAgentName();
        String rootAgentDesc = agentConfigVO.getRootAgent().getRootAgentDesc();


        InMemoryRunner runner = getRunner(dynamicContext, agentConfigVO, appName);

        AgentRegisterVO agentRegisterVO = AgentRegisterVO.builder()
                .appName(appName)
                .rootAgentId(rootAgentId)
                .rootAgentName(rootAgentName)
                .rootAgentDesc(rootAgentDesc)
                .runner(runner)
                .build();
        return agentRegisterVO;
    }

    @Override
    public StrategyHandler<AssembleCommandEntity, DefaultAssembleFactory.DynamicContext, AgentRegisterVO> get(AssembleCommandEntity requestParameter, DefaultAssembleFactory.DynamicContext dynamicContext) throws Exception {
        return defaultStrategyHandler;
    }

    private InMemoryRunner getRunner(DefaultAssembleFactory.DynamicContext dynamicContext, AgentConfigVO agentConfigVO, String appName) {
        AgentConfigVO.AgentRuntime.Runner runnerConfig = agentConfigVO.getRuntime().getRunner();

        String agentName = runnerConfig.getAgentName();
        if (StringUtils.isBlank(agentName)) {
            log.error("runner.agentName is null");
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
        }

        BaseAgent baseAgent = dynamicContext.getAgentGroup().get(agentName);

        List<BasePlugin> plugins;
        List<String> pluginNameList = runnerConfig.getPluginNameList();
        if (null != pluginNameList && !pluginNameList.isEmpty()) {
            plugins = new ArrayList<>();
            for (String pluginName : pluginNameList) {
                BasePlugin plugin = beanRegistry.getBean(pluginName, BasePlugin.class);
                plugins.add(plugin);
            }
        } else {
            plugins = ImmutableList.of();
        }

        return new InMemoryRunner(baseAgent, appName, plugins);
    }
}

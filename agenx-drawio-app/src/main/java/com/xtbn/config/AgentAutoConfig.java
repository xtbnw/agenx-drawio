package com.xtbn.config;

import com.xtbn.domain.agent.model.valobj.properties.AgentAutoConfigProperties;
import com.xtbn.domain.agent.service.IAssembleService;
import com.xtbn.domain.agent.service.assmble.component.mcp.server.DrawioXmlToolService;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import java.util.ArrayList;

@Slf4j
@Configuration
@EnableConfigurationProperties(AgentAutoConfigProperties.class)
public class AgentAutoConfig implements ApplicationListener<ApplicationReadyEvent> {
    @Resource
    private AgentAutoConfigProperties agentAutoConfigProperties;
    @Resource
    private IAssembleService assembleService;

    @Override
    public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
        try {
//            log.info("Agent 配置 {}", JSON.toJSONString(agentAutoConfigProperties.getTables().values()));
            assembleService.assembleAgents(new ArrayList<>(agentAutoConfigProperties.getTables().values()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Bean("drawioXmlToolCallbackProvider")
    public ToolCallbackProvider drawioXmlTools(DrawioXmlToolService drawioXmlToolService) {
        return MethodToolCallbackProvider.builder().toolObjects(drawioXmlToolService).build();
    }
}
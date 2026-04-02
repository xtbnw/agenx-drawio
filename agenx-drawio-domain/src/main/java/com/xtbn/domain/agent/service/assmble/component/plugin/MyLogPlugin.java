package com.xtbn.domain.agent.service.assmble.component.plugin;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.CallbackContext;
import com.google.adk.agents.InvocationContext;
import com.google.adk.plugins.BasePlugin;
import com.google.adk.tools.BaseTool;
import com.google.adk.tools.ToolContext;
import com.google.genai.types.Content;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service("myLogPlugin")
public class MyLogPlugin extends BasePlugin {
    public MyLogPlugin(String name) {
        super(name);
    }
    public MyLogPlugin() {
        super("MyLogPlugin");
    }

    @Override
    public Maybe<Content> onUserMessageCallback(InvocationContext invocationContext, Content userMessage) {
        log.info("用户输入信息:{}", userMessage.text());
        return super.onUserMessageCallback(invocationContext, userMessage);
    }
    @Override
    public Maybe<Content> beforeRunCallback(InvocationContext invocationContext){
        log.info("开始处理");
        return super.beforeRunCallback(invocationContext);
    }

    @Override
    public Maybe<Content> beforeAgentCallback(BaseAgent agent, CallbackContext callbackContext) {
        String agentName = agent.name();
        log.info("Agent调用开始：{}", agentName);
        return super.beforeAgentCallback(agent, callbackContext);
    }

    @Override
    public Maybe<Map<String, Object>> beforeToolCallback(
            BaseTool tool, Map<String, Object> toolArgs, ToolContext toolContext){
        log.info("调用工具：{}", tool.name());
        return super.beforeToolCallback(tool, toolArgs, toolContext);
    }

    @Override
    public Maybe<Map<String, Object>> afterToolCallback(
            BaseTool tool,
            Map<String, Object> toolArgs,
            ToolContext toolContext,
            Map<String, Object> result){
        log.info("工具调用完成：{}", tool.name());
        return super.afterToolCallback(tool, toolArgs, toolContext, result);
    }

    @Override
    public Maybe<Content> afterAgentCallback(BaseAgent agent, CallbackContext callbackContext) {
        String agentName = agent.name();
        log.info("Agent调用完成：{}", agentName);
        return super.afterAgentCallback(agent, callbackContext);
    }

    @Override
    public Completable afterRunCallback(InvocationContext invocationContext){
        log.info("处理完成");
        return super.afterRunCallback(invocationContext);
    }
}

package com.xtbn.domain.agent.service.assmble.component.mcp.client.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xtbn.domain.agent.model.valobj.AgentConfigVO;
import com.xtbn.domain.agent.service.assmble.component.mcp.client.IToolMcpCreateService;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;

import java.time.Duration;
@Slf4j
@Service
public class StdioToolMcpCreateService implements IToolMcpCreateService {
    @Override
    public ToolCallback[] buildToolCallback(AgentConfigVO.AgentRuntime.ChatModel.ToolMcp toolMcp) throws Exception {
        AgentConfigVO.AgentRuntime.ChatModel.ToolMcp.StdioServerParameters stdioConfig = toolMcp.getStdio();
        AgentConfigVO.AgentRuntime.ChatModel.ToolMcp.StdioServerParameters.ServerParameters serverParameters = stdioConfig.getServerParameters();
        // https://github.com/modelcontextprotocol/servers/tree/main/src/filesystem
        var stdioParams = ServerParameters.builder(serverParameters.getCommand())
                .args(serverParameters.getArgs())
                .env(serverParameters.getEnv())
                .build();

        McpSyncClient mcpSyncClient = McpClient.sync(new StdioClientTransport(stdioParams, new JacksonMcpJsonMapper(new ObjectMapper())))
                .requestTimeout(Duration.ofSeconds(stdioConfig.getRequestTimeout())).build();
        var init_stdio = mcpSyncClient.initialize();

        log.info("Tool Stdio MCP Initialized {}", init_stdio);
        return SyncMcpToolCallbackProvider.builder().mcpClients(mcpSyncClient).build()
                .getToolCallbacks();
    }
}

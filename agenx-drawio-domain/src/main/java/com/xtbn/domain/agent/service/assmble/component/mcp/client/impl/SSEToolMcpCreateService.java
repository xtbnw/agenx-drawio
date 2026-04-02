package com.xtbn.domain.agent.service.assmble.component.mcp.client.impl;

import com.xtbn.domain.agent.model.valobj.AgentConfigVO;
import com.xtbn.domain.agent.service.assmble.component.mcp.client.IToolMcpCreateService;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
public class SSEToolMcpCreateService implements IToolMcpCreateService {
    @Override
    public ToolCallback[] buildToolCallback(AgentConfigVO.AgentRuntime.ChatModel.ToolMcp toolMcp) throws Exception {
        AgentConfigVO.AgentRuntime.ChatModel.ToolMcp.SSEServerParameters sseConfig = toolMcp.getSse();
        String originalBaseUri = sseConfig.getBaseUri();
        String baseUri;
        String sseEndpoint;

        int queryParamStartIndex = originalBaseUri.indexOf("sse");
        if (queryParamStartIndex != -1) {
            baseUri = originalBaseUri.substring(0, queryParamStartIndex - 1);
            sseEndpoint = originalBaseUri.substring(queryParamStartIndex - 1);
        } else {
            baseUri = originalBaseUri;
            sseEndpoint = sseConfig.getSseEndpoint();
        }

        sseEndpoint = StringUtils.isBlank(sseEndpoint) ? "/sse" : sseEndpoint;

        HttpClientSseClientTransport sseClientTransport = HttpClientSseClientTransport
                .builder(baseUri) // 使用截取后的 baseUri
                .sseEndpoint(sseEndpoint) // 使用截取或默认的 sseEndpoint
                .build();

        McpSyncClient mcpSyncClient = McpClient.sync(sseClientTransport).requestTimeout(Duration.ofSeconds(sseConfig.getRequestTimeout())).build();
        var init_sse = mcpSyncClient.initialize();

        log.info("Tool SSE MCP Initialized {}", init_sse);
        return SyncMcpToolCallbackProvider.builder()
                .mcpClients(mcpSyncClient).build()
                .getToolCallbacks();
    }
}

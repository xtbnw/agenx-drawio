package com.xtbn.domain.agent.service.assmble.component.tool.mcp.client.impl;

import com.xtbn.domain.agent.model.valobj.AgentConfigVO;
import com.xtbn.domain.agent.service.assmble.component.tool.mcp.client.IToolMcpCreateService;
import com.xtbn.domain.agent.service.assmble.component.tool.ToolCallbackFactory;
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
    @javax.annotation.Resource
    private ToolCallbackFactory toolCallbackFactory;

    @Override
    public ToolCallback[] buildToolCallback(AgentConfigVO.AgentRuntime.ChatModel.ToolMcp toolMcp) throws Exception {
        AgentConfigVO.AgentRuntime.ChatModel.ToolMcp.SSEServerParameters sseConfig = toolMcp.getSse();
        if (sseConfig == null || StringUtils.isBlank(sseConfig.getBaseUri())) {
            log.warn("skip sse mcp init because baseUri is blank");
            return new ToolCallback[0];
        }
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

        try {
            McpSyncClient mcpSyncClient = McpClient.sync(sseClientTransport).requestTimeout(Duration.ofSeconds(sseConfig.getRequestTimeout())).build();
            var initSse = mcpSyncClient.initialize();
            log.info("Tool SSE MCP Initialized {}", initSse);
            ToolCallback[] callbacks = SyncMcpToolCallbackProvider.builder()
                    .mcpClients(mcpSyncClient).build()
                    .getToolCallbacks();
            return toolCallbackFactory.wrapWithLogging("mcp-sse", sseConfig.getName(), callbacks);
        } catch (Exception e) {
            log.warn("skip sse mcp init for {} because initialization failed: {}", sseConfig.getName(), e.getMessage());
            return new ToolCallback[0];
        }
    }
}

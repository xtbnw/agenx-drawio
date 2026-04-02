package com.xtbn.domain.agent.service.assmble.component.mcp.client.factory;

import com.xtbn.domain.agent.model.valobj.AgentConfigVO;
import com.xtbn.domain.agent.service.assmble.component.mcp.client.IToolMcpCreateService;
import com.xtbn.domain.agent.service.assmble.component.mcp.client.impl.LocalToolMcpCreateService;
import com.xtbn.domain.agent.service.assmble.component.mcp.client.impl.SSEToolMcpCreateService;
import com.xtbn.domain.agent.service.assmble.component.mcp.client.impl.StdioToolMcpCreateService;
import com.xtbn.types.enums.ResponseCode;
import com.xtbn.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DefaultMcpClientFactory {
    private final LocalToolMcpCreateService localToolMcpCreateService;
    private final SSEToolMcpCreateService sseToolMcpCreateService;
    private final StdioToolMcpCreateService stdioToolMcpCreateService;
    public DefaultMcpClientFactory(LocalToolMcpCreateService localToolMcpCreateService, SSEToolMcpCreateService sseToolMcpCreateService, StdioToolMcpCreateService stdioToolMcpCreateService) {
        this.localToolMcpCreateService = localToolMcpCreateService;
        this.sseToolMcpCreateService = sseToolMcpCreateService;
        this.stdioToolMcpCreateService = stdioToolMcpCreateService;
    }

    public IToolMcpCreateService getToolMcpCreateService(AgentConfigVO.AgentRuntime.ChatModel.ToolMcp toolMcp) {
        if (null != toolMcp.getLocal()) return localToolMcpCreateService;
        if (null != toolMcp.getSse()) return sseToolMcpCreateService;
        if (null != toolMcp.getStdio()) return stdioToolMcpCreateService;
        throw new AppException(ResponseCode.NOT_FOUND_METHOD.getCode(), ResponseCode.NOT_FOUND_METHOD.getInfo());
    }
}

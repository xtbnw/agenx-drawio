package com.xtbn.domain.chat.service;

import com.google.adk.events.Event;
import com.xtbn.domain.chat.model.entity.ChatCommandEntity;
import com.xtbn.domain.agent.model.valobj.AgentConfigVO;
import reactor.core.publisher.Flux;

import java.util.List;

public interface IChatService {
    List<AgentConfigVO.RootAgent> queryAgentConfigList();

    String createSession(String agentId, String userId);

    String handleFinalMessage(String agentId, String userId, String message);

    String handleFinalMessage(String agentId, String userId, String sessionId, String message);

    List<String> handleMessage(String agentId, String userId, String message);

    List<String> handleMessage(String agentId, String userId, String sessionId, String message);

    Flux<Event> handleMessageStream(String agentId, String userId, String sessionId, String message);

    List<String> handleMessage(ChatCommandEntity chatCommandEntity);
}

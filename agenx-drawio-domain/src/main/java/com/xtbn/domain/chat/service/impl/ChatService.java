package com.xtbn.domain.chat.service.impl;

import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.Session;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import com.xtbn.domain.agent.adapter.port.registry.IBeanRegistry;
import com.xtbn.domain.agent.model.valobj.AgentConfigVO;
import com.xtbn.domain.agent.model.valobj.AgentRegisterVO;
import com.xtbn.domain.agent.model.valobj.properties.AgentAutoConfigProperties;
import com.xtbn.domain.chat.model.entity.ChatCommandEntity;
import com.xtbn.domain.chat.service.IChatService;
import com.xtbn.types.enums.ResponseCode;
import com.xtbn.types.exception.AppException;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.disposables.Disposable;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
public class ChatService implements IChatService {

    @Resource
    private IBeanRegistry beanRegistry;
    @Resource
    private AgentAutoConfigProperties agentAutoConfigProperties;

    private final Map<String, String> userSessions = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Object> state = new ConcurrentHashMap<>();

    @Override
    public List<AgentConfigVO.RootAgent> queryAgentConfigList() {
        Map<String, AgentConfigVO> tables = agentAutoConfigProperties.getTables();

        List<AgentConfigVO.RootAgent> agentList = new ArrayList<>();
        if (null != tables) {
            for (AgentConfigVO vo : tables.values()) {
                if (null != vo.getRootAgent()) {
                    agentList.add(vo.getRootAgent());
                }
            }
        }
        return agentList;
    }

    @Override
    public String createSession(String agentId, String userId) {
        return createSession(agentId, userId, false);
    }

    @Override
    public String createSession(String agentId, String userId, boolean refresh) {
        AgentRegisterVO preferredAgent = getAgentRegister(agentId);
        String sessionKey = userId;

        if (refresh) {
            String sessionId = createSharedSession(userId, preferredAgent);
            userSessions.put(sessionKey, sessionId);
            return sessionId;
        }

        return userSessions.computeIfAbsent(sessionKey, key -> createSharedSession(userId, preferredAgent));
    }

    @Override
    public List<String> handleMessage(String agentId, String userId, String message) {
        String sessionId = createSession(agentId, userId);
        return handleMessage(agentId, userId, sessionId, message);
    }

    @Override
    public String handleFinalMessage(String agentId, String userId, String message) {
        String sessionId = createSession(agentId, userId);
        return handleFinalMessage(agentId, userId, sessionId, message);
    }

    @Override
    public List<String> handleMessage(String agentId, String userId, String sessionId, String message) {
        AgentRegisterVO agentRegisterVO = getAgentRegister(agentId);
        ensureSessionExists(agentRegisterVO, userId, sessionId);

        InMemoryRunner runner = agentRegisterVO.getRunner();
        Content userMsg = Content.fromParts(Part.fromText(message));
        Flowable<Event> events = runner.runAsync(userId, sessionId, userMsg);

        List<String> outputs = new ArrayList<>();
        events.blockingForEach(event -> outputs.add(event.stringifyContent()));

        return outputs;
    }

    @Override
    public String handleFinalMessage(String agentId, String userId, String sessionId, String message) {
        AgentRegisterVO agentRegisterVO = getAgentRegister(agentId);
        ensureSessionExists(agentRegisterVO, userId, sessionId);

        InMemoryRunner runner = agentRegisterVO.getRunner();
        Content userMsg = Content.fromParts(Part.fromText(message));
        Flowable<Event> events = runner.runAsync(userId, sessionId, userMsg);

        AtomicReference<String> finalOutput = new AtomicReference<>();
        AtomicReference<String> lastNonEmptyOutput = new AtomicReference<>();

        events.blockingForEach(event -> {
            String content = event.stringifyContent();
            if (null != content && !content.trim().isEmpty()) {
                lastNonEmptyOutput.set(content);
                if (event.finalResponse()) {
                    finalOutput.set(content);
                }
            }
        });

        if (null != finalOutput.get()) {
            return finalOutput.get();
        }

        return null != lastNonEmptyOutput.get() ? lastNonEmptyOutput.get() : "";
    }

    @Override
    public Flux<Event> handleMessageStream(String agentId, String userId, String sessionId, String message) {
        AgentRegisterVO agentRegisterVO = getAgentRegister(agentId);

        if (null == sessionId || sessionId.isEmpty()) {
            sessionId = createSession(agentId, userId);
        }

        ensureSessionExists(agentRegisterVO, userId, sessionId);

        InMemoryRunner runner = agentRegisterVO.getRunner();
        Content userMsg = Content.fromParts(Part.fromText(message));
        Flowable<Event> events = runner.runAsync(userId, sessionId, userMsg);

        return Flux.create(sink -> {
            Disposable disposable = events.subscribe(
                    sink::next,
                    sink::error,
                    sink::complete
            );
            sink.onCancel(disposable::dispose);
            sink.onDispose(disposable::dispose);
        });
    }

    private AgentRegisterVO getAgentRegister(String agentId) {
        AgentRegisterVO agentRegisterVO = beanRegistry.getBean(agentId, AgentRegisterVO.class);
        if (null == agentRegisterVO) {
            throw new AppException(ResponseCode.E0001.getCode());
        }
        return agentRegisterVO;
    }

    private String createSharedSession(String userId, AgentRegisterVO preferredAgent) {
        String preferredAppName = preferredAgent.getAppName();
        String sessionId = UUID.randomUUID().toString();

        syncSession(preferredAgent, userId, sessionId);

        Map<String, AgentConfigVO> tables = agentAutoConfigProperties.getTables();
        if (null != tables) {
            for (AgentConfigVO agentConfig : tables.values()) {
                if (null == agentConfig.getRootAgent()) {
                    continue;
                }

                String agentId = agentConfig.getRootAgent().getRootAgentId();
                AgentRegisterVO agentRegisterVO = beanRegistry.getBean(agentId, AgentRegisterVO.class);
                if (null == agentRegisterVO) {
                    continue;
                }

                if (preferredAppName.equals(agentRegisterVO.getAppName())) {
                    continue;
                }

                syncSession(agentRegisterVO, userId, sessionId);
            }
        }

        return sessionId;
    }

    private void ensureSessionExists(AgentRegisterVO agentRegisterVO, String userId, String sessionId) {
        Session session = agentRegisterVO.getRunner()
                .sessionService()
                .getSession(agentRegisterVO.getAppName(), userId, sessionId, Optional.empty())
                .blockingGet();

        if (null == session) {
            syncSession(agentRegisterVO, userId, sessionId);
        }
    }

    private void syncSession(AgentRegisterVO agentRegisterVO, String userId, String sessionId) {
        agentRegisterVO.getRunner()
                .sessionService()
                .createSession(agentRegisterVO.getAppName(), userId, state, sessionId)
                .blockingGet();
    }

    @Override
    public List<String> handleMessage(ChatCommandEntity chatCommandEntity) {
        AgentRegisterVO agentRegisterVO = getAgentRegister(chatCommandEntity.getAgentId());
        ensureSessionExists(agentRegisterVO, chatCommandEntity.getUserId(), chatCommandEntity.getSessionId());

        List<Part> parts = new ArrayList<>();

        List<ChatCommandEntity.Content.Text> texts = chatCommandEntity.getTexts();
        if (null != texts && !texts.isEmpty()) {
            for (ChatCommandEntity.Content.Text text : texts) {
                parts.add(Part.fromText(text.getMessage()));
            }
        }

        List<ChatCommandEntity.Content.File> files = chatCommandEntity.getFiles();
        if (null != files && !files.isEmpty()) {
            for (ChatCommandEntity.Content.File file : files) {
                parts.add(Part.fromUri(file.getFileUri(), file.getMimeType()));
            }
        }

        List<ChatCommandEntity.Content.InlineData> inlineDatas = chatCommandEntity.getInlineDatas();
        if (null != inlineDatas && !inlineDatas.isEmpty()) {
            for (ChatCommandEntity.Content.InlineData inlineData : inlineDatas) {
                parts.add(Part.fromBytes(inlineData.getBytes(), inlineData.getMimeType()));
            }
        }

        Content content = Content.builder().role("user").parts(parts).build();
        InMemoryRunner runner = agentRegisterVO.getRunner();
        Flowable<Event> events = runner.runAsync(chatCommandEntity.getUserId(), chatCommandEntity.getSessionId(), content);

        List<String> outputs = new ArrayList<>();
        events.blockingForEach(event -> outputs.add(event.stringifyContent()));

        return outputs;
    }
}

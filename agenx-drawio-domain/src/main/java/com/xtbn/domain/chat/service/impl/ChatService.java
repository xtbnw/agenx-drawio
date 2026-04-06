package com.xtbn.domain.chat.service.impl;

import com.google.adk.events.Event;
import com.google.adk.runner.Runner;
import com.google.adk.sessions.Session;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import com.xtbn.domain.agent.adapter.port.registry.IBeanRegistry;
import com.xtbn.domain.agent.adapter.repository.ISharedRunnerComponentRepository;
import com.xtbn.domain.agent.model.valobj.AgentConfigVO;
import com.xtbn.domain.agent.model.valobj.AgentRegisterVO;
import com.xtbn.domain.agent.model.valobj.properties.AgentAutoConfigProperties;
import com.xtbn.domain.chat.model.entity.ChatCommandEntity;
import com.xtbn.domain.chat.model.valobj.ChatHistoryMessageVO;
import com.xtbn.domain.chat.model.valobj.ChatSessionDetailVO;
import com.xtbn.domain.chat.model.valobj.ChatSessionVO;
import com.xtbn.domain.chat.service.IChatService;
import com.xtbn.types.enums.ResponseCode;
import com.xtbn.types.exception.AppException;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.disposables.Disposable;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.xtbn.types.common.Constants.APP_NAME;

@Slf4j
@Service
public class ChatService implements IChatService {

    private static final String STATE_AGENT_ID = "agentId";
    private static final String STATE_TITLE = "title";
    private static final String STATE_LAST_MESSAGE_PREVIEW = "lastMessagePreview";
    private static final String STATE_MESSAGE_COUNT = "messageCount";
    private static final String STATE_CREATED_AT = "createdAt";
    private static final String STATE_UPDATED_AT = "updatedAt";
    private static final String STATE_LAST_MESSAGE_AT = "lastMessageAt";

    @Resource
    private IBeanRegistry beanRegistry;
    @Resource
    private AgentAutoConfigProperties agentAutoConfigProperties;
    @Resource
    private ISharedRunnerComponentRepository sharedRunnerComponentRepository;

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
        AgentRegisterVO agentRegisterVO = getAgentRegister(agentId);
        String sessionId = UUID.randomUUID().toString();
        agentRegisterVO.getRunner()
                .sessionService()
                .createSession(APP_NAME, userId, buildSessionState(agentId), sessionId)
                .blockingGet();
        return sessionId;
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

        Runner runner = agentRegisterVO.getRunner();
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

        Runner runner = agentRegisterVO.getRunner();
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

        Runner runner = agentRegisterVO.getRunner();
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

    @Override
    public List<ChatSessionVO> querySessionList(String userId) {
        return sharedRunnerComponentRepository.getSharedSessionService()
                .listSessions(APP_NAME, userId)
                .map(response -> response.sessions().stream()
                        .map(this::toSessionVO)
                        .sorted(Comparator.comparing(ChatSessionVO::getLastMessageAt, Comparator.nullsLast(Comparator.reverseOrder())))
                        .collect(Collectors.toList()))
                .blockingGet();
    }

    @Override
    public ChatSessionDetailVO querySessionDetail(String userId, String sessionId) {
        Session session = sharedRunnerComponentRepository.getSharedSessionService()
                .getSession(APP_NAME, userId, sessionId, Optional.empty())
                .blockingGet();

        if (null == session) {
            throw new AppException("SESSION_NOT_FOUND");
        }

        return toSessionDetailVO(session);
    }

    private AgentRegisterVO getAgentRegister(String agentId) {
        AgentRegisterVO agentRegisterVO = beanRegistry.getBean(agentId, AgentRegisterVO.class);
        if (null == agentRegisterVO) {
            throw new AppException(ResponseCode.E0001.getCode());
        }
        return agentRegisterVO;
    }

    private void ensureSessionExists(AgentRegisterVO agentRegisterVO, String userId, String sessionId) {
        Session session = agentRegisterVO.getRunner()
                .sessionService()
                .getSession(APP_NAME, userId, sessionId, Optional.empty())
                .blockingGet();

        if (null == session) {
            throw new AppException("SESSION_NOT_FOUND");
        }
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
        Runner runner = agentRegisterVO.getRunner();
        Flowable<Event> events = runner.runAsync(chatCommandEntity.getUserId(), chatCommandEntity.getSessionId(), content);

        List<String> outputs = new ArrayList<>();
        events.blockingForEach(event -> outputs.add(event.stringifyContent()));

        return outputs;
    }

    private ConcurrentHashMap<String, Object> buildSessionState(String agentId) {
        long now = Instant.now().toEpochMilli();
        ConcurrentHashMap<String, Object> state = new ConcurrentHashMap<>();
        state.put(STATE_AGENT_ID, agentId);
        state.put(STATE_CREATED_AT, now);
        state.put(STATE_UPDATED_AT, now);
        state.put(STATE_LAST_MESSAGE_AT, now);
        state.put(STATE_MESSAGE_COUNT, 0);
        return state;
    }

    private ChatSessionVO toSessionVO(Session session) {
        Map<String, Object> state = session.state();
        String agentId = readString(state, STATE_AGENT_ID);
        return ChatSessionVO.builder()
                .sessionId(session.id())
                .title(firstNonBlank(readString(state, STATE_TITLE), "新会话"))
                .agentId(agentId)
                .agentName(resolveAgentName(agentId))
                .lastMessagePreview(readString(state, STATE_LAST_MESSAGE_PREVIEW))
                .messageCount(readInteger(state, STATE_MESSAGE_COUNT))
                .createdAt(readLong(state, STATE_CREATED_AT))
                .updatedAt(readLong(state, STATE_UPDATED_AT))
                .lastMessageAt(readLong(state, STATE_LAST_MESSAGE_AT))
                .build();
    }

    private ChatSessionDetailVO toSessionDetailVO(Session session) {
        Map<String, Object> state = session.state();
        String agentId = readString(state, STATE_AGENT_ID);
        List<ChatHistoryMessageVO> messages = session.events().stream()
                .map(this::toHistoryMessage)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return ChatSessionDetailVO.builder()
                .sessionId(session.id())
                .title(firstNonBlank(readString(state, STATE_TITLE), "新会话"))
                .agentId(agentId)
                .agentName(resolveAgentName(agentId))
                .createdAt(readLong(state, STATE_CREATED_AT))
                .updatedAt(readLong(state, STATE_UPDATED_AT))
                .lastMessageAt(readLong(state, STATE_LAST_MESSAGE_AT))
                .messageCount(readInteger(state, STATE_MESSAGE_COUNT))
                .messages(messages)
                .build();
    }

    private ChatHistoryMessageVO toHistoryMessage(Event event) {
        String content = event.stringifyContent();
        if (content == null || content.trim().isEmpty()) {
            return null;
        }

        boolean userMessage = "user".equalsIgnoreCase(event.author());
        if (!userMessage && !event.finalResponse()) {
            return null;
        }

        return ChatHistoryMessageVO.builder()
                .messageId(event.id())
                .role(userMessage ? "user" : "assistant")
                .content(content)
                .createdAt(event.timestamp())
                .build();
    }

    private String resolveAgentName(String agentId) {
        if (agentId == null || agentId.isBlank()) {
            return null;
        }
        Map<String, AgentConfigVO> tables = agentAutoConfigProperties.getTables();
        if (tables == null) {
            return agentId;
        }
        for (AgentConfigVO configVO : tables.values()) {
            if (configVO.getRootAgent() != null && agentId.equals(configVO.getRootAgent().getRootAgentId())) {
                return configVO.getRootAgent().getRootAgentName();
            }
        }
        return agentId;
    }

    private String readString(Map<String, Object> state, String key) {
        Object value = state.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private Long readLong(Map<String, Object> state, String key) {
        Object value = state.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ignore) {
            return null;
        }
    }

    private Integer readInteger(Map<String, Object> state, String key) {
        Object value = state.get(key);
        if (value == null) {
            return 0;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignore) {
            return 0;
        }
    }

    private String firstNonBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}

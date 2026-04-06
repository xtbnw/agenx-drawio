package com.xtbn.domain.agent.service.assmble.component.session;

import com.alibaba.fastjson.JSON;
import com.google.adk.events.Event;
import com.google.adk.sessions.BaseSessionService;
import com.google.adk.sessions.GetSessionConfig;
import com.google.adk.sessions.ListEventsResponse;
import com.google.adk.sessions.ListSessionsResponse;
import com.google.adk.sessions.Session;
import com.xtbn.domain.agent.adapter.repository.ISessionRepository;
import com.xtbn.domain.agent.model.entity.StoredEventEntity;
import com.xtbn.domain.agent.model.entity.StoredSessionEntity;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SessionService implements BaseSessionService {

    public static final String STATE_AGENT_ID = "agentId";
    public static final String STATE_TITLE = "title";
    public static final String STATE_LAST_MESSAGE_PREVIEW = "lastMessagePreview";
    public static final String STATE_MESSAGE_COUNT = "messageCount";
    public static final String STATE_CREATED_AT = "createdAt";
    public static final String STATE_UPDATED_AT = "updatedAt";
    public static final String STATE_LAST_MESSAGE_AT = "lastMessageAt";

    private final ISessionRepository repository;

    @Override
    @Transactional
    public Single<Session> createSession(String appName, String userId, ConcurrentMap<String, Object> state, String sessionId) {
        return Single.fromCallable(() -> {
            long now = Instant.now().toEpochMilli();
            Map<String, Object> nextState = new LinkedHashMap<>();
            if (state != null) {
                nextState.putAll(state);
            }
            nextState.putIfAbsent(STATE_CREATED_AT, now);
            nextState.put(STATE_UPDATED_AT, now);
            nextState.putIfAbsent(STATE_LAST_MESSAGE_AT, now);
            nextState.putIfAbsent(STATE_MESSAGE_COUNT, 0);

            String agentId = valueAsString(nextState.get(STATE_AGENT_ID));
            repository.insertSession(appName, agentId == null ? "" : agentId, userId, sessionId, JSON.toJSONString(nextState), now);
            return buildSession(appName, userId, sessionId, nextState, List.of(), now);
        });
    }

    @Override
    public Maybe<Session> getSession(String appName, String userId, String sessionId, Optional<GetSessionConfig> config) {
        return Maybe.fromCallable(() -> repository.findSession(userId, sessionId)
                .filter(record -> appName.equals(record.getAppName()))
                .map(record -> toSession(record, config))
                .orElse(null));
    }

    @Override
    public Single<ListSessionsResponse> listSessions(String appName, String userId) {
        return Single.fromCallable(() -> ListSessionsResponse.builder()
                .sessions(repository.listSessionsByAppAndUserId(appName, userId).stream()
                        .map(record -> toSession(record, Optional.empty(), false))
                        .collect(Collectors.toList()))
                .build());
    }

    @Override
    @Transactional
    public Completable deleteSession(String appName, String userId, String sessionId) {
        return Completable.fromAction(() -> repository.findSession(userId, sessionId)
                .filter(record -> appName.equals(record.getAppName()))
                .ifPresent(record -> repository.markDeleted(sessionId)));
    }

    @Override
    public Single<ListEventsResponse> listEvents(String appName, String userId, String sessionId) {
        return Single.fromCallable(() -> {
            StoredSessionEntity record = repository.findSession(userId, sessionId)
                    .filter(item -> appName.equals(item.getAppName()))
                    .orElse(null);
            if (record == null) {
                return ListEventsResponse.builder().events(List.of()).build();
            }
            return ListEventsResponse.builder()
                    .events(loadEvents(record, Optional.empty()))
                    .build();
        });
    }

    @Override
    @Transactional
    public Single<Event> appendEvent(Session session, Event event) {
        return Single.fromCallable(() -> {
            long now = Instant.now().toEpochMilli();
            StoredEventEntity record = new StoredEventEntity();
            record.setSessionId(session.id());
            record.setEventId(event.id());
            record.setInvocationId(event.invocationId());
            record.setAuthor(event.author());
            record.setContentText(event.stringifyContent());
            record.setContentJson(event.content().map(ContentLike::toStringValue).orElse(null));
            record.setEventJson(event.toString());
            record.setPartialFlag(event.partial().orElse(false));
            record.setTurnCompleteFlag(event.turnComplete().orElse(false));
            record.setFinalResponseFlag(event.finalResponse());
            record.setErrorMessage(event.errorMessage().orElse(null));
            record.setTimestampMs(event.timestamp());
            record.setCreatedAt(now);
            repository.appendEvent(record);

            StoredSessionEntity sessionRecord = repository.findSession(session.id()).orElse(null);
            if (sessionRecord != null) {
                Map<String, Object> state = parseState(sessionRecord.getStateJson());
                updateSessionMetadata(sessionRecord, state, event, now);
                repository.updateSessionState(session.id(), JSON.toJSONString(state), now);
                repository.updateSessionSummary(
                        session.id(),
                        sessionRecord.getMessageCount(),
                        sessionRecord.getTitle(),
                        sessionRecord.getLastMessagePreview(),
                        now,
                        sessionRecord.getLastMessageAt()
                );
            }
            return event;
        });
    }

    public Session toSession(StoredSessionEntity record, Optional<GetSessionConfig> config) {
        return toSession(record, config, true);
    }

    public Session toSession(StoredSessionEntity record, Optional<GetSessionConfig> config, boolean includeEvents) {
        List<Event> events = includeEvents ? loadEvents(record, config) : List.of();
        Map<String, Object> state = enrichState(record);
        long updatedAt = record.getUpdatedAt() == null ? record.getLastMessageAt() : record.getUpdatedAt();
        return buildSession(record.getAppName(), record.getUserId(), record.getSessionId(), state, events, updatedAt);
    }

    private List<Event> loadEvents(StoredSessionEntity record, Optional<GetSessionConfig> config) {
        List<StoredEventEntity> eventRecords = repository.listEvents(record.getSessionId());
        List<Event> events = eventRecords.stream()
                .map(item -> Event.fromJson(item.getEventJson()))
                .sorted(Comparator.comparingLong(Event::timestamp))
                .collect(Collectors.toList());

        if (config.isPresent()) {
            GetSessionConfig getSessionConfig = config.get();
            if (getSessionConfig.afterTimestamp().isPresent()) {
                long afterTimestamp = getSessionConfig.afterTimestamp().get().toEpochMilli();
                events = events.stream()
                        .filter(event -> event.timestamp() > afterTimestamp)
                        .collect(Collectors.toList());
            }
            if (getSessionConfig.numRecentEvents().isPresent()) {
                int count = getSessionConfig.numRecentEvents().get();
                if (count >= 0 && events.size() > count) {
                    events = new ArrayList<>(events.subList(events.size() - count, events.size()));
                }
            }
        }

        return events;
    }

    private Session buildSession(String appName, String userId, String sessionId, Map<String, Object> state, List<Event> events, long updatedAt) {
        ConcurrentHashMap<String, Object> safeState = new ConcurrentHashMap<>();
        if (state != null) {
            state.forEach((key, value) -> {
                if (key != null && value != null) {
                    safeState.put(key, value);
                }
            });
        }
        return Session.builder(sessionId)
                .appName(appName)
                .userId(userId)
                .state(safeState)
                .events(events)
                .lastUpdateTime(Instant.ofEpochMilli(updatedAt))
                .build();
    }

    private Map<String, Object> enrichState(StoredSessionEntity record) {
        Map<String, Object> state = parseState(record.getStateJson());
        state.put(STATE_AGENT_ID, record.getAgentId());
        state.put(STATE_TITLE, record.getTitle());
        state.put(STATE_LAST_MESSAGE_PREVIEW, record.getLastMessagePreview());
        state.put(STATE_MESSAGE_COUNT, record.getMessageCount());
        state.put(STATE_CREATED_AT, record.getCreatedAt());
        state.put(STATE_UPDATED_AT, record.getUpdatedAt());
        state.put(STATE_LAST_MESSAGE_AT, record.getLastMessageAt());
        return state;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseState(String stateJson) {
        if (stateJson == null || stateJson.isBlank()) {
            return new LinkedHashMap<>();
        }
        Object parsed = JSON.parse(stateJson);
        if (parsed instanceof Map<?, ?> map) {
            return new LinkedHashMap<>((Map<String, Object>) map);
        }
        return new LinkedHashMap<>();
    }

    private void updateSessionMetadata(StoredSessionEntity sessionRecord, Map<String, Object> state, Event event, long now) {
        String displayText = normalizeContent(event.stringifyContent());
        boolean visible = isVisibleHistoryEvent(event, displayText);

        if (visible) {
            int nextCount = (sessionRecord.getMessageCount() == null ? 0 : sessionRecord.getMessageCount()) + 1;
            sessionRecord.setMessageCount(nextCount);
            sessionRecord.setLastMessagePreview(truncate(displayText, 180));
            sessionRecord.setLastMessageAt(event.timestamp() > 0 ? event.timestamp() : now);
            state.put(STATE_MESSAGE_COUNT, nextCount);
            state.put(STATE_LAST_MESSAGE_PREVIEW, sessionRecord.getLastMessagePreview());
            state.put(STATE_LAST_MESSAGE_AT, sessionRecord.getLastMessageAt());

            if ((sessionRecord.getTitle() == null || sessionRecord.getTitle().isBlank()) && "user".equalsIgnoreCase(event.author())) {
                String title = truncate(displayText, 30);
                sessionRecord.setTitle(title);
                state.put(STATE_TITLE, title);
            }
        }

        sessionRecord.setUpdatedAt(now);
        state.put(STATE_UPDATED_AT, now);
    }

    private boolean isVisibleHistoryEvent(Event event, String content) {
        if (content == null || content.isBlank()) {
            return false;
        }
        if ("user".equalsIgnoreCase(event.author())) {
            return true;
        }
        return event.finalResponse();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String normalizeContent(String content) {
        if (content == null) {
            return null;
        }
        return content.trim().replaceAll("\\s+", " ");
    }

    private String valueAsString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private interface ContentLike {
        static String toStringValue(Object value) {
            return value == null ? null : value.toString();
        }
    }
}

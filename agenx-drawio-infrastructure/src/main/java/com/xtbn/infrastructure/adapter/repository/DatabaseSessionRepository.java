package com.xtbn.infrastructure.adapter.repository;

import com.xtbn.domain.agent.adapter.repository.ISessionRepository;
import com.xtbn.domain.agent.model.entity.StoredEventEntity;
import com.xtbn.domain.agent.model.entity.StoredSessionEntity;
import com.xtbn.infrastructure.dao.mapper.AgentSessionStoreMapper;
import com.xtbn.infrastructure.dao.po.AgentEventRecordPO;
import com.xtbn.infrastructure.dao.po.AgentSessionRecordPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DatabaseSessionRepository implements ISessionRepository {

    private final AgentSessionStoreMapper mapper;

    @Override
    public void insertSession(String appName, String agentId, String userId, String sessionId, String stateJson, long now) {
        mapper.insertSession(appName, agentId, userId, sessionId, now);
        mapper.insertSessionState(sessionId, stateJson, now);
    }

    @Override
    public Optional<StoredSessionEntity> findSession(String sessionId) {
        return Optional.ofNullable(toEntity(mapper.findSessionBySessionId(sessionId)));
    }

    @Override
    public Optional<StoredSessionEntity> findSession(String userId, String sessionId) {
        return Optional.ofNullable(toEntity(mapper.findSessionByUserIdAndSessionId(userId, sessionId)));
    }

    @Override
    public List<StoredSessionEntity> listSessionsByUserId(String userId) {
        return mapper.listSessionsByUserId(userId).stream().map(this::toEntity).toList();
    }

    @Override
    public List<StoredSessionEntity> listSessionsByAppAndUserId(String appName, String userId) {
        return mapper.listSessionsByAppAndUserId(appName, userId).stream().map(this::toEntity).toList();
    }

    @Override
    public void updateSessionState(String sessionId, String stateJson, long now) {
        mapper.updateSessionState(sessionId, stateJson, now);
    }

    @Override
    public void appendEvent(StoredEventEntity event) {
        mapper.insertEvent(toPo(event));
    }

    @Override
    public List<StoredEventEntity> listEvents(String sessionId) {
        return mapper.listEvents(sessionId).stream().map(this::toEntity).toList();
    }

    @Override
    public void updateSessionSummary(String sessionId, Integer messageCount, String title, String lastMessagePreview, long now, long lastMessageAt) {
        mapper.updateSessionSummary(sessionId, messageCount, title, lastMessagePreview, now, lastMessageAt);
    }

    @Override
    public void markDeleted(String sessionId) {
        mapper.markDeleted(sessionId);
    }

    private StoredSessionEntity toEntity(AgentSessionRecordPO po) {
        if (po == null) {
            return null;
        }
        StoredSessionEntity entity = new StoredSessionEntity();
        entity.setId(po.getId());
        entity.setAppName(po.getAppName());
        entity.setAgentId(po.getAgentId());
        entity.setUserId(po.getUserId());
        entity.setSessionId(po.getSessionId());
        entity.setTitle(po.getTitle());
        entity.setStatus(po.getStatus());
        entity.setMessageCount(po.getMessageCount());
        entity.setLastMessagePreview(po.getLastMessagePreview());
        entity.setCreatedAt(po.getCreatedAt());
        entity.setUpdatedAt(po.getUpdatedAt());
        entity.setLastMessageAt(po.getLastMessageAt());
        entity.setStateJson(po.getStateJson());
        return entity;
    }

    private StoredEventEntity toEntity(AgentEventRecordPO po) {
        if (po == null) {
            return null;
        }
        StoredEventEntity entity = new StoredEventEntity();
        entity.setId(po.getId());
        entity.setSessionId(po.getSessionId());
        entity.setEventId(po.getEventId());
        entity.setInvocationId(po.getInvocationId());
        entity.setAuthor(po.getAuthor());
        entity.setContentText(po.getContentText());
        entity.setContentJson(po.getContentJson());
        entity.setEventJson(po.getEventJson());
        entity.setPartialFlag(po.getPartialFlag());
        entity.setTurnCompleteFlag(po.getTurnCompleteFlag());
        entity.setFinalResponseFlag(po.getFinalResponseFlag());
        entity.setErrorMessage(po.getErrorMessage());
        entity.setTimestampMs(po.getTimestampMs());
        entity.setCreatedAt(po.getCreatedAt());
        return entity;
    }

    private AgentEventRecordPO toPo(StoredEventEntity entity) {
        AgentEventRecordPO po = new AgentEventRecordPO();
        po.setId(entity.getId());
        po.setSessionId(entity.getSessionId());
        po.setEventId(entity.getEventId());
        po.setInvocationId(entity.getInvocationId());
        po.setAuthor(entity.getAuthor());
        po.setContentText(entity.getContentText());
        po.setContentJson(entity.getContentJson());
        po.setEventJson(entity.getEventJson());
        po.setPartialFlag(entity.getPartialFlag());
        po.setTurnCompleteFlag(entity.getTurnCompleteFlag());
        po.setFinalResponseFlag(entity.getFinalResponseFlag());
        po.setErrorMessage(entity.getErrorMessage());
        po.setTimestampMs(entity.getTimestampMs());
        po.setCreatedAt(entity.getCreatedAt());
        return po;
    }
}

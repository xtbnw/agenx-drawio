package com.xtbn.domain.agent.adapter.repository;

import com.xtbn.domain.agent.model.entity.StoredEventEntity;
import com.xtbn.domain.agent.model.entity.StoredSessionEntity;

import java.util.List;
import java.util.Optional;

public interface ISessionRepository {

    void insertSession(String appName, String agentId, String userId, String sessionId, String stateJson, long now);

    Optional<StoredSessionEntity> findSession(String sessionId);

    Optional<StoredSessionEntity> findSession(String userId, String sessionId);

    List<StoredSessionEntity> listSessionsByUserId(String userId);

    List<StoredSessionEntity> listSessionsByAppAndUserId(String appName, String userId);

    void updateSessionState(String sessionId, String stateJson, long now);

    void appendEvent(StoredEventEntity event);

    List<StoredEventEntity> listEvents(String sessionId);

    void updateSessionSummary(String sessionId, Integer messageCount, String title, String lastMessagePreview, long now, long lastMessageAt);

    void markDeleted(String sessionId);
}

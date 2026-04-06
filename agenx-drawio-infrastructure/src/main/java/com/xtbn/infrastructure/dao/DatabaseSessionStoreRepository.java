package com.xtbn.infrastructure.dao;

import com.xtbn.infrastructure.dao.mapper.AgentSessionStoreMapper;
import com.xtbn.infrastructure.dao.po.AgentEventRecordPO;
import com.xtbn.infrastructure.dao.po.AgentMemoryRecordPO;
import com.xtbn.infrastructure.dao.po.AgentSessionRecordPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DatabaseSessionStoreRepository {

    private final AgentSessionStoreMapper mapper;

    public void insertSession(String appName, String agentId, String userId, String sessionId, String stateJson, long now) {
        mapper.insertSession(appName, agentId, userId, sessionId, now);
        mapper.insertSessionState(sessionId, stateJson, now);
    }

    public Optional<AgentSessionRecordPO> findSession(String sessionId) {
        return Optional.ofNullable(mapper.findSessionBySessionId(sessionId));
    }

    public Optional<AgentSessionRecordPO> findSession(String userId, String sessionId) {
        return Optional.ofNullable(mapper.findSessionByUserIdAndSessionId(userId, sessionId));
    }

    public List<AgentSessionRecordPO> listSessionsByUserId(String userId) {
        return mapper.listSessionsByUserId(userId);
    }

    public List<AgentSessionRecordPO> listSessionsByAppAndUserId(String appName, String userId) {
        return mapper.listSessionsByAppAndUserId(appName, userId);
    }

    public void updateSessionState(String sessionId, String stateJson, long now) {
        mapper.updateSessionState(sessionId, stateJson, now);
    }

    public void appendEvent(AgentEventRecordPO record) {
        mapper.insertEvent(record);
    }

    public List<AgentEventRecordPO> listEvents(String sessionId) {
        return mapper.listEvents(sessionId);
    }

    public void updateSessionSummary(String sessionId, Integer messageCount, String title, String lastMessagePreview, long now, long lastMessageAt) {
        mapper.updateSessionSummary(sessionId, messageCount, title, lastMessagePreview, now, lastMessageAt);
    }

    public void upsertMemory(AgentMemoryRecordPO record) {
        Integer exists = mapper.countMemoryBySessionId(record.getSessionId());
        if (exists != null && exists > 0) {
            mapper.updateMemory(record);
            return;
        }
        mapper.insertMemory(record);
    }

    public List<AgentMemoryRecordPO> searchMemories(String appName, String userId, String query) {
        String keyword = query == null ? "" : query.trim();
        if (keyword.isEmpty()) {
            return mapper.searchMemoriesWithoutQuery(appName, userId);
        }
        return mapper.searchMemoriesWithQuery(appName, userId, keyword);
    }

    public void markDeleted(String sessionId) {
        mapper.markDeleted(sessionId);
    }
}

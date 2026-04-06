package com.xtbn.infrastructure.adapter.repository;

import com.xtbn.domain.agent.adapter.repository.IMemoryRepository;
import com.xtbn.domain.agent.model.entity.StoredMemoryEntity;
import com.xtbn.infrastructure.dao.mapper.AgentSessionStoreMapper;
import com.xtbn.infrastructure.dao.po.AgentMemoryRecordPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class DatabaseMemoryRepository implements IMemoryRepository {

    private final AgentSessionStoreMapper mapper;

    @Override
    public void upsertMemory(StoredMemoryEntity memory) {
        Integer exists = mapper.countMemoryBySessionId(memory.getSessionId());
        if (exists != null && exists > 0) {
            mapper.updateMemory(toPo(memory));
            return;
        }
        mapper.insertMemory(toPo(memory));
    }

    @Override
    public List<StoredMemoryEntity> searchMemories(String appName, String userId, String query) {
        String keyword = query == null ? "" : query.trim();
        if (keyword.isEmpty()) {
            return mapper.searchMemoriesWithoutQuery(appName, userId).stream().map(this::toEntity).toList();
        }
        return mapper.searchMemoriesWithQuery(appName, userId, keyword).stream().map(this::toEntity).toList();
    }

    private StoredMemoryEntity toEntity(AgentMemoryRecordPO po) {
        if (po == null) {
            return null;
        }
        StoredMemoryEntity entity = new StoredMemoryEntity();
        entity.setId(po.getId());
        entity.setAppName(po.getAppName());
        entity.setUserId(po.getUserId());
        entity.setSessionId(po.getSessionId());
        entity.setMemoryText(po.getMemoryText());
        entity.setSource(po.getSource());
        entity.setCreatedAt(po.getCreatedAt());
        entity.setUpdatedAt(po.getUpdatedAt());
        return entity;
    }

    private AgentMemoryRecordPO toPo(StoredMemoryEntity entity) {
        AgentMemoryRecordPO po = new AgentMemoryRecordPO();
        po.setId(entity.getId());
        po.setAppName(entity.getAppName());
        po.setUserId(entity.getUserId());
        po.setSessionId(entity.getSessionId());
        po.setMemoryText(entity.getMemoryText());
        po.setSource(entity.getSource());
        po.setCreatedAt(entity.getCreatedAt());
        po.setUpdatedAt(entity.getUpdatedAt());
        return po;
    }
}

package com.xtbn.domain.agent.adapter.repository;

import com.xtbn.domain.agent.model.entity.StoredMemoryEntity;

import java.util.List;

public interface IMemoryRepository {

    void upsertMemory(StoredMemoryEntity memory);

    List<StoredMemoryEntity> searchMemories(String appName, String userId, String query);
}

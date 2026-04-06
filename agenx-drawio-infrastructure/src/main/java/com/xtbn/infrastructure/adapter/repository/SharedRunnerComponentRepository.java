package com.xtbn.infrastructure.adapter.repository;

import com.google.adk.memory.BaseMemoryService;
import com.google.adk.sessions.BaseSessionService;
import com.xtbn.domain.agent.adapter.repository.ISharedRunnerComponentRepository;
import com.xtbn.infrastructure.dao.DatabaseMemoryService;
import com.xtbn.infrastructure.dao.DatabaseSessionService;
import org.springframework.stereotype.Service;

@Service
public class SharedRunnerComponentRepository implements ISharedRunnerComponentRepository {
    private final BaseSessionService sharedSessionService;
    private final BaseMemoryService sharedMemoryService;

    public SharedRunnerComponentRepository(DatabaseSessionService databaseSessionService, DatabaseMemoryService databaseMemoryService) {
        this.sharedSessionService = databaseSessionService;
        this.sharedMemoryService = databaseMemoryService;
    }

    @Override
    public BaseSessionService getSharedSessionService() {
        return sharedSessionService;
    }

    @Override
    public BaseMemoryService getSharedMemoryService() {
        return sharedMemoryService;
    }
}

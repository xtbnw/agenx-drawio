package com.xtbn.infrastructure.adapter.repository;

import com.google.adk.memory.BaseMemoryService;
import com.google.adk.memory.InMemoryMemoryService;
import com.google.adk.sessions.BaseSessionService;
import com.google.adk.sessions.InMemorySessionService;
import com.xtbn.domain.agent.adapter.repository.ISharedRunnerComponentRepository;
import lombok.Data;
import org.springframework.stereotype.Service;

@Service
@Data
public class SharedRunnerComponentRepository implements ISharedRunnerComponentRepository {
    private final BaseSessionService sharedSessionService;
    private final BaseMemoryService sharedMemoryService;

    public SharedRunnerComponentRepository() {
        this.sharedSessionService = new InMemorySessionService();
        this.sharedMemoryService = new InMemoryMemoryService();
    }
}

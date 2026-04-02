package com.xtbn.domain.agent.adapter.repository;

import com.google.adk.memory.BaseMemoryService;
import com.google.adk.sessions.BaseSessionService;

public interface ISharedRunnerComponentRepository {
    BaseSessionService getSharedSessionService();
    BaseMemoryService getSharedMemoryService();
}

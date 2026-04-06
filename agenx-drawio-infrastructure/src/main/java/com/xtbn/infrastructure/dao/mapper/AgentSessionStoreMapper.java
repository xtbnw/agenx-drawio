package com.xtbn.infrastructure.dao.mapper;

import com.xtbn.infrastructure.dao.po.AgentEventRecordPO;
import com.xtbn.infrastructure.dao.po.AgentMemoryRecordPO;
import com.xtbn.infrastructure.dao.po.AgentSessionRecordPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AgentSessionStoreMapper {

    void insertSession(@Param("appName") String appName,
                       @Param("agentId") String agentId,
                       @Param("userId") String userId,
                       @Param("sessionId") String sessionId,
                       @Param("now") long now);

    void insertSessionState(@Param("sessionId") String sessionId,
                            @Param("stateJson") String stateJson,
                            @Param("updatedAt") long updatedAt);

    AgentSessionRecordPO findSessionBySessionId(@Param("sessionId") String sessionId);

    AgentSessionRecordPO findSessionByUserIdAndSessionId(@Param("userId") String userId,
                                                         @Param("sessionId") String sessionId);

    List<AgentSessionRecordPO> listSessionsByUserId(@Param("userId") String userId);

    List<AgentSessionRecordPO> listSessionsByAppAndUserId(@Param("appName") String appName,
                                                          @Param("userId") String userId);

    void updateSessionState(@Param("sessionId") String sessionId,
                            @Param("stateJson") String stateJson,
                            @Param("updatedAt") long updatedAt);

    void insertEvent(AgentEventRecordPO record);

    List<AgentEventRecordPO> listEvents(@Param("sessionId") String sessionId);

    void updateSessionSummary(@Param("sessionId") String sessionId,
                              @Param("messageCount") Integer messageCount,
                              @Param("title") String title,
                              @Param("lastMessagePreview") String lastMessagePreview,
                              @Param("updatedAt") long updatedAt,
                              @Param("lastMessageAt") long lastMessageAt);

    Integer countMemoryBySessionId(@Param("sessionId") String sessionId);

    void updateMemory(AgentMemoryRecordPO record);

    void insertMemory(AgentMemoryRecordPO record);

    List<AgentMemoryRecordPO> searchMemoriesWithoutQuery(@Param("appName") String appName,
                                                         @Param("userId") String userId);

    List<AgentMemoryRecordPO> searchMemoriesWithQuery(@Param("appName") String appName,
                                                      @Param("userId") String userId,
                                                      @Param("query") String query);

    void markDeleted(@Param("sessionId") String sessionId);
}

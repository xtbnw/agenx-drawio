package com.xtbn.infrastructure.dao.po;

import lombok.Data;

@Data
public class AgentEventRecordPO {
    private Long id;
    private String sessionId;
    private String eventId;
    private String invocationId;
    private String author;
    private String contentText;
    private String contentJson;
    private String eventJson;
    private Boolean partialFlag;
    private Boolean turnCompleteFlag;
    private Boolean finalResponseFlag;
    private String errorMessage;
    private Long timestampMs;
    private Long createdAt;
}

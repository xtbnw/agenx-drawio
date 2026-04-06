CREATE TABLE IF NOT EXISTS agent_session (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    app_name VARCHAR(64) NOT NULL,
    agent_id VARCHAR(128) NOT NULL,
    user_id VARCHAR(128) NOT NULL,
    session_id VARCHAR(128) NOT NULL,
    title VARCHAR(255) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    message_count INT NOT NULL DEFAULT 0,
    last_message_preview VARCHAR(1000) NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    last_message_at BIGINT NOT NULL,
    CONSTRAINT uk_agent_session_session_id UNIQUE (session_id),
    INDEX idx_agent_session_user_last_message (user_id, last_message_at),
    INDEX idx_agent_session_app_user (app_name, user_id),
    INDEX idx_agent_session_user_agent (user_id, agent_id)
);

CREATE TABLE IF NOT EXISTS agent_session_state (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(128) NOT NULL,
    state_json LONGTEXT NULL,
    updated_at BIGINT NOT NULL,
    CONSTRAINT uk_agent_session_state_session_id UNIQUE (session_id),
    CONSTRAINT fk_agent_session_state_session_id FOREIGN KEY (session_id) REFERENCES agent_session (session_id)
);

CREATE TABLE IF NOT EXISTS agent_event (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(128) NOT NULL,
    event_id VARCHAR(128) NULL,
    invocation_id VARCHAR(128) NULL,
    author VARCHAR(255) NULL,
    content_text LONGTEXT NULL,
    content_json LONGTEXT NULL,
    event_json LONGTEXT NOT NULL,
    partial_flag TINYINT(1) NOT NULL DEFAULT 0,
    turn_complete_flag TINYINT(1) NOT NULL DEFAULT 0,
    final_response_flag TINYINT(1) NOT NULL DEFAULT 0,
    error_message VARCHAR(1000) NULL,
    timestamp_ms BIGINT NOT NULL,
    created_at BIGINT NOT NULL,
    INDEX idx_agent_event_session_time (session_id, timestamp_ms, id),
    INDEX idx_agent_event_session_final_time (session_id, final_response_flag, timestamp_ms),
    CONSTRAINT fk_agent_event_session_id FOREIGN KEY (session_id) REFERENCES agent_session (session_id)
);

CREATE TABLE IF NOT EXISTS agent_memory (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    app_name VARCHAR(64) NOT NULL,
    user_id VARCHAR(128) NOT NULL,
    session_id VARCHAR(128) NOT NULL,
    memory_text LONGTEXT NULL,
    source VARCHAR(64) NOT NULL DEFAULT 'session_summary',
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    CONSTRAINT uk_agent_memory_session_id UNIQUE (session_id),
    INDEX idx_agent_memory_app_user_updated (app_name, user_id, updated_at)
);

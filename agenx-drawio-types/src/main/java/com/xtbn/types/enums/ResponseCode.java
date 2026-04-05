package com.xtbn.types.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public enum ResponseCode {

    SUCCESS("0000", "SUCCESS"),
    UN_ERROR("0001", "UNKNOWN_ERROR"),
    ILLEGAL_PARAMETER("0002", "ILLEGAL_PARAMETER"),
    NOT_FOUND_METHOD("0003", "NOT_FOUND_METHOD"),
    RATE_LIMITED("G0001", "RATE_LIMITED"),
    USER_BLACKLISTED("G0002", "USER_BLACKLISTED"),
    USER_QUOTA_EXCEEDED("G0003", "USER_QUOTA_EXCEEDED"),
    CIRCUIT_BREAKER_OPEN("G0004", "CIRCUIT_BREAKER_OPEN"),

    E0001("E0001", "AGENT_NOT_FOUND"),
    E0002("E0002", "MCP_CONFIG_NOT_SUPPORTED");

    private String code;
    private String info;
}

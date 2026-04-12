package com.xtbn.types.common;

import java.time.Duration;

public final class MetricsConstants {

    private MetricsConstants() {
    }

    public static final String AGENT_REQUESTS_TOTAL = "agent_requests_total";
    public static final String AGENT_REQUEST_FAILURES_TOTAL = "agent_request_failures_total";
    public static final String AGENT_RUN_DURATION_SECONDS = "agent_run_duration_seconds";

    public static final String AGENT_MODEL_CALLS_TOTAL = "agent_model_calls_total";
    public static final String AGENT_MODEL_FAILURES_TOTAL = "agent_model_failures_total";
    public static final String AGENT_MODEL_DURATION_SECONDS = "agent_model_duration_seconds";

    public static final String AGENT_TOOL_CALLS_TOTAL = "agent_tool_calls_total";
    public static final String AGENT_TOOL_FAILURES_TOTAL = "agent_tool_failures_total";
    public static final String AGENT_TOOL_DURATION_SECONDS = "agent_tool_duration_seconds";

    public static final String AGENT_GOVERNANCE_REDIS_ERRORS_CURRENT = "agent_governance_redis_errors_current";
    public static final String AGENT_GOVERNANCE_REJECTIONS_TOTAL = "agent_governance_rejections_total";
    public static final String AGENT_GOVERNANCE_REQUESTS_TOTAL = "agent_governance_requests_total";
    public static final String AGENT_GOVERNANCE_OPERATION_LATENCY = "agent_governance_operation_latency";
    public static final String AGENT_GOVERNANCE_REDIS_ERRORS_TOTAL = "agent_governance_redis_errors_total";

    public static final String AGENT_SENSITIVE_MATCHES_TOTAL = "agent_sensitive_matches_total";
    public static final String AGENT_SENSITIVE_FILTER_REQUESTS_TOTAL = "agent_sensitive_filter_requests_total";
    public static final String AGENT_SENSITIVE_FILTER_DURATION_SECONDS = "agent_sensitive_filter_duration_seconds";
    public static final String AGENT_SENSITIVE_FILTER_MATCHES_TOTAL = "agent_sensitive_filter_matches_total";

    public static final String AGENT_INPUT_TOKENS_TOTAL = "agent_input_tokens_total";
    public static final String AGENT_OUTPUT_TOKENS_TOTAL = "agent_output_tokens_total";
    public static final String AGENT_ESTIMATED_COST_TOTAL = "agent_estimated_cost_total";

    public static final String DRAWIO_XML_RETRY_COUNT = "drawio_xml_retry_count";
    public static final String DRAWIO_XML_DEGRADED_TOTAL = "drawio_xml_degraded_total";

    public static final Duration METRIC_MIN_DURATION = Duration.ofMillis(1);
    public static final Duration METRIC_MAX_DURATION = Duration.ofSeconds(30);
}

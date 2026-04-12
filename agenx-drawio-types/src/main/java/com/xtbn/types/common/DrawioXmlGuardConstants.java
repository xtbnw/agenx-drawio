package com.xtbn.types.common;

public final class DrawioXmlGuardConstants {

    private DrawioXmlGuardConstants() {
    }

    public static final String CTX_SKIP_GUARD = "drawioXmlGuardSkip";
    public static final String CTX_CALL_STAGE = "drawioXmlGuardCallStage";

    public static final String STAGE_RETRY = "retry";
    public static final String STAGE_DEGRADE = "degrade";

    public static final String AGENT_FAST_XML = "DrawioDirectXmlAgent";
    public static final String AGENT_BALANCED_XML = "DiagramXmlAgent";
    public static final String AGENT_QUALITY_XML = "XmlRevisionAgent";

    public static final String DEFAULT_INTERNAL_AGENT_PREFIX = "internal-";
    public static final String DEFAULT_DEGRADE_AGENT_ID = "internal-drawio-degrade";

    public static final String STATE_AGENT_ID = "agentId";
}

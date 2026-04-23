package com.xtbn.types.common;

public final class DrawioXmlGuardConstants {

    private DrawioXmlGuardConstants() {
    }

    public static final String CTX_SKIP_GUARD = "drawioXmlGuardSkip";
    public static final String CTX_CALL_STAGE = "drawioXmlGuardCallStage";

    public static final String STAGE_RETRY = "retry";
    public static final String STAGE_DEGRADE = "degrade";

    public static final String AGENT_FAST_XML = "DrawioDirectXmlAgent";
    public static final String AGENT_SPEC_XML = "DiagramXmlRenderAgent";
    public static final String AGENT_POLISH_XML = "XmlPolishAgent";
    public static final String AGENT_MAX_RENDER_XML = "MaxXmlRenderAgent";
    public static final String AGENT_MAX_REFINE_XML = "MaxXmlRefineAgent";

    public static final String DEFAULT_INTERNAL_AGENT_PREFIX = "internal-";
    public static final String DEFAULT_DEGRADE_AGENT_ID = "internal-drawio-degrade";

    public static final String STATE_AGENT_ID = "agentId";
}

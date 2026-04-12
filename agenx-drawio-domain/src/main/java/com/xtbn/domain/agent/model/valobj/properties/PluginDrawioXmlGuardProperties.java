package com.xtbn.domain.agent.model.valobj.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import com.xtbn.types.common.DrawioXmlGuardConstants;

import java.util.LinkedHashSet;
import java.util.Set;

@Data
@ConfigurationProperties(prefix = "agent.plugin.drawio-xml-guard")
public class PluginDrawioXmlGuardProperties {
    private boolean enabled = true;
    private String internalAgentPrefix = DrawioXmlGuardConstants.DEFAULT_INTERNAL_AGENT_PREFIX;
    private String degradeAgentId = DrawioXmlGuardConstants.DEFAULT_DEGRADE_AGENT_ID;
    private Set<String> guardedAgentNames = new LinkedHashSet<>(Set.of(
            DrawioXmlGuardConstants.AGENT_FAST_XML,
            DrawioXmlGuardConstants.AGENT_BALANCED_XML,
            DrawioXmlGuardConstants.AGENT_QUALITY_XML
    ));
}

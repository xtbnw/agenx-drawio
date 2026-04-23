package com.xtbn.domain.agent.model.valobj.properties;

import com.xtbn.types.common.DrawioXmlGuardConstants;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashSet;
import java.util.Set;

@Data
@ConfigurationProperties(prefix = "agent.plugin.drawio-layout-guard")
public class PluginDrawioLayoutGuardProperties {
    private boolean enabled = true;
    private String degradeAgentId = DrawioXmlGuardConstants.DEFAULT_DEGRADE_AGENT_ID;
    private Set<String> guardedAgentNames = new LinkedHashSet<>(Set.of(
            DrawioXmlGuardConstants.AGENT_MAX_RENDER_XML,
            DrawioXmlGuardConstants.AGENT_MAX_REFINE_XML
    ));
}

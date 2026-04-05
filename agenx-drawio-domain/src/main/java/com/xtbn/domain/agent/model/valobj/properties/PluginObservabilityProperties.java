package com.xtbn.domain.agent.model.valobj.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "agent.plugin.observability")
public class PluginObservabilityProperties {
    private boolean enabled = true;
}

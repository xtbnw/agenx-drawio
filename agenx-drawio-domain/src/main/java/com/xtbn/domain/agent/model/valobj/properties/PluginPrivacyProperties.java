package com.xtbn.domain.agent.model.valobj.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "agent.plugin.privacy")
public class PluginPrivacyProperties {
    private boolean enabled = true;
    private boolean maskPhone = true;
    private boolean maskIdCard = true;
    private boolean maskToken = true;
    private String replaceText = "***";
}

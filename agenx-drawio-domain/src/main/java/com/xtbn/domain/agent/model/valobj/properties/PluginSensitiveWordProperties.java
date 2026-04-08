package com.xtbn.domain.agent.model.valobj.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "agent.plugin.sensitive-word")
public class PluginSensitiveWordProperties {
    private boolean enabled = true;
    private Mode mode = Mode.REJECT;
    private String replaceText = "***";
    private boolean normalizeEnabled = true;
    private List<String> wordList = new ArrayList<>();
    private List<String> whitelist = new ArrayList<>();
    private boolean hotReloadEnabled = false;

    public boolean isRejectMode() {
        return mode == Mode.REJECT;
    }

    public enum Mode {
        REJECT,
        REPLACE
    }
}

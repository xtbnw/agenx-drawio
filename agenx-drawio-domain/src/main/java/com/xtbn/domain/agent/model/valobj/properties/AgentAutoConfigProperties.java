package com.xtbn.domain.agent.model.valobj.properties;

import com.xtbn.domain.agent.model.valobj.AgentConfigVO;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "ai.agent.config", ignoreInvalidFields = true)
public class AgentAutoConfigProperties {
    /**
     * 是否启用AI Agent自动装配
     */
    private boolean enabled = false;

    private Map<String, AgentConfigVO> tables;
}

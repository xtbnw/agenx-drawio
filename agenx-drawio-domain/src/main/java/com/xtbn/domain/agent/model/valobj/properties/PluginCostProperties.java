package com.xtbn.domain.agent.model.valobj.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "agent.plugin.cost")
public class PluginCostProperties {
    private boolean enabled = true;
    private Map<String, ModelPricing> modelPricing = new HashMap<>();

    @Data
    public static class ModelPricing {
        private BigDecimal inputPer1k = BigDecimal.ZERO;
        private BigDecimal outputPer1k = BigDecimal.ZERO;
    }
}

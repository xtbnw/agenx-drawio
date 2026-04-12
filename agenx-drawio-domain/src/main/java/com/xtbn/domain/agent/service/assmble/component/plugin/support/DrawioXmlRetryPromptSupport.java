package com.xtbn.domain.agent.service.assmble.component.plugin.support;

import org.springframework.stereotype.Component;

@Component
public class DrawioXmlRetryPromptSupport {

    public String buildRetryPrompt(String originalUserRequest, String invalidXml, DrawioXmlValidationSupport.ValidationResult result) {
        StringBuilder builder = new StringBuilder(512);
        builder.append("The previous draw.io XML output was invalid.\n");
        builder.append("Original user request:\n");
        builder.append(nullToEmpty(originalUserRequest).trim()).append("\n\n");
        builder.append("Validation failure:\n");
        builder.append("- Code: ").append(result.getErrorCode().name()).append("\n");
        builder.append("- Message: ").append(result.getErrorMessage()).append("\n\n");
        builder.append("Previous invalid XML:\n");
        builder.append(nullToEmpty(invalidXml).trim()).append("\n\n");
        builder.append("Retry requirements:\n");
        builder.append("1. Return XML only.\n");
        builder.append("2. Keep the original diagram semantics; fix structure only.\n");
        builder.append("3. Output a complete draw.io mxfile that can be imported directly.\n");
        builder.append("4. Do not add explanations, markdown fences, or extra text.");
        return builder.toString();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
